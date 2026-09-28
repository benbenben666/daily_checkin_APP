<template>
	<view class="page">
		<!-- 任务详情卡片：标题 + 状态 + 标签 + 基础信息 + 详情/指派人/提交内容 -->
		<view v-if="task" class="card">
			<!-- 标题行：任务标题 + 状态标签 -->
			<view class="row">
				<text class="title flex1">{{ task.title }}</text>
				<text class="tag" :class="statusTag">{{ statusText }}</text>
			</view>
			<!-- 属性标签行：任务类型 / 超时完成 / 可补交 -->
			<view class="row tags-row">
				<text class="tag" :class="task.taskType === 'GLOBAL' ? 'tag-blue' : 'tag-orange'">
					{{ task.taskType === 'GLOBAL' ? '全局抢单' : '指定任务' }}
				</text>
				<text v-if="task.isLate === 1" class="tag tag-red">超时完成</text>
				<text v-if="task.allowLateSubmit === 1" class="tag tag-green">可补交</text>
			</view>

			<!-- 基础信息区：发布人 / 时间 / 完成人等按状态条件展示 -->
			<view class="info-block">
				<view class="info-row"><text class="muted">发布人</text><text>{{ task.publisherNickname || task.publisherId }}</text></view>
				<view class="info-row"><text class="muted">发布时间</text><text>{{ formatTime(task.createdAt) }}</text></view>
				<view class="info-row"><text class="muted">完成截止</text><text>{{ formatTime(task.deadlineAt) }}</text></view>
				<view v-if="task.allowLateSubmit === 1" class="info-row"><text class="muted">补交截止</text><text>{{ formatTime(task.lateDeadlineAt) }}</text></view>
				<view v-if="task.status === 'COMPLETED'" class="info-row"><text class="muted">完成人</text><text>{{ task.completedByNickname || task.completedBy }}</text></view>
				<view v-if="task.status === 'COMPLETED'" class="info-row"><text class="muted">完成时间</text><text>{{ formatTime(task.completedAt) }}</text></view>
				<view v-if="task.status === 'CANCELLED'" class="info-row"><text class="muted">取消原因</text><text>{{ task.cancelReason || '无' }}</text></view>
			</view>

			<!-- 任务详情描述：有内容才显示 -->
			<view v-if="task.description" class="desc-block">
				<text class="section-title">任务详情</text>
				<text class="desc">{{ task.description }}</text>
			</view>

			<!-- 被指派人列表：仅指定任务有 -->
			<view v-if="task.assignees && task.assignees.length > 0" class="desc-block">
				<text class="section-title">被指派人</text>
				<view class="row member-tags">
					<text v-for="a in task.assignees" :key="a.userId" class="tag tag-blue">{{ a.nickname || a.phone }}</text>
				</view>
			</view>

			<!-- 提交内容：任务完成后展示完成人填写的说明 -->
			<view v-if="task.submitContent" class="desc-block">
				<text class="section-title">提交内容</text>
				<text class="desc">{{ task.submitContent }}</text>
			</view>
		</view>

		<!-- 操作区：主按钮不可完成时置灰占位（避免留白），取消按钮按权限显示 -->
		<view v-if="task" class="card actions">
			<!-- 主操作：进行中/补交期内可点击提交，其余状态置灰展示状态说明 -->
			<button class="btn-primary" :class="{ 'btn-disabled': !canComplete }" :disabled="!canComplete" @click="showComplete = true">
				{{ completeBtnText }}
			</button>
			<button v-if="canCancel" class="btn-danger" :loading="canceling" @click="cancelTask">取消任务</button>
		</view>

		<!-- 完成提交弹窗：填写提交说明后确认 -->
		<view v-if="showComplete" class="modal-mask" @click="showComplete = false">
			<view class="modal" @click.stop>
				<text class="section-title">{{ task.status === 'EXPIRED' ? '超时补交' : '提交任务' }}</text>
				<textarea class="textarea" v-model="submitContent" placeholder="提交说明（可不填，直接提交）" />
				<view class="row modal-btns">
					<button class="btn-plain flex1" @click="showComplete = false">取消</button>
					<button class="btn-primary flex1" :loading="submitting" @click="doComplete">确认提交</button>
				</view>
			</view>
		</view>

		<!-- 取消任务确认弹窗：统一样式组件 -->
		<confirm-dialog
			v-model:visible="showCancelModal"
			title="取消任务"
			content="确定取消该任务吗？"
			@confirm="doCancel"
		/>
	</view>
</template>

<script setup>
// ============================================================
// 任务详情页：查看任务信息、完成任务 / 超时补交、取消任务
// 权限规则：
// - 完成：进行中可完成；超时仅在补交期内可补交；指定任务仅被指派人
// - 取消：公司创始人 / 管理者，或系统管理员
// ============================================================
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { companyApi, taskApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'
import { formatTime } from '../../utils/format.js'
// 统一确认弹窗组件（样式与退出登录弹窗一致）
import ConfirmDialog from '../../components/confirm-dialog/confirm-dialog.vue'

// 任务 id（路由参数传入）
const taskId = ref(0)
// 任务详情数据
const task = ref(null)
// 完成提交弹窗显隐
const showComplete = ref(false)
// 完成任务的提交说明（可空）
const submitContent = ref('')
// 提交完成请求中标志
const submitting = ref(false)
// 取消任务请求中标志
const canceling = ref(false)
// 取消任务确认弹窗显隐
const showCancelModal = ref(false)
// 当前用户 id（从缓存解析，用于判断是否被指派人）
const myUserId = ref(0)
// 我在该任务所属公司内的角色（FOUNDER / MANAGER / EMPLOYEE）
const companyRole = ref('')
// 我的系统角色（ADMIN / USER）
const systemRole = ref('')

// 是否被指派（ASSIGNED 任务只有被指派人能完成，否则后端必然拒绝）
const isAssignee = computed(() => {
	if (!task.value || !task.value.assignees) return false
	return task.value.assignees.some(a => Number(a.userId) === Number(myUserId.value))
})

// 是否可完成任务（false 时主按钮置灰不可点击）
const canComplete = computed(() => {
	if (!task.value) return false
	if (task.value.taskType === 'ASSIGNED' && !isAssignee.value) return false
	if (task.value.status === 'PENDING') return true
	// 已超时：需允许补交且当前时间仍在补交截止时间之前
	return task.value.status === 'EXPIRED'
		&& task.value.allowLateSubmit === 1
		&& isWithinLateDeadline(task.value.lateDeadlineAt)
})

// 是否显示「取消任务」按钮
const canCancel = computed(() => {
	if (!task.value) return false
	const s = task.value.status
	if (s !== 'PENDING' && s !== 'EXPIRED') return false
	// 与后端一致：该公司创始人/管理者，或系统管理员
	return systemRole.value === 'ADMIN' || companyRole.value === 'FOUNDER' || companyRole.value === 'MANAGER'
})

// 主操作按钮文案：不可完成时展示对应状态说明
const completeBtnText = computed(() => {
	if (!task.value) return ''
	const s = task.value.status
	if (s === 'COMPLETED') return '任务已完成'
	if (s === 'CANCELLED') return '任务已取消'
	if (s === 'EXPIRED') {
		if (task.value.allowLateSubmit !== 1) return '已超时'
		if (!isWithinLateDeadline(task.value.lateDeadlineAt)) return '补交已截止'
	}
	// 进行中或补交期内：指定任务非被指派人不可提交
	if (task.value.taskType === 'ASSIGNED' && !isAssignee.value) return '仅被指派人可提交'
	return s === 'EXPIRED' ? '超时补交' : '提交任务'
})

/** 判断当前时间是否仍在补交截止时间之前（异常时间串按不可补交处理） */
function isWithinLateDeadline(t) {
	if (!t) return false
	const deadline = new Date(String(t).replace(' ', 'T'))
	if (isNaN(deadline.getTime())) return false
	return Date.now() <= deadline.getTime()
}

// 任务状态 -> 中文文案
const statusText = computed(() => {
	if (!task.value) return ''
	const s = task.value.status
	return s === 'PENDING' ? '进行中' : s === 'EXPIRED' ? '已超时' : s === 'COMPLETED' ? '已完成' : '已取消'
})

// 任务状态 -> 标签颜色（进行中蓝 / 超时橙 / 完成绿 / 取消灰）
const statusTag = computed(() => {
	if (!task.value) return 'tag-gray'
	const s = task.value.status
	return s === 'PENDING' ? 'tag-blue' : s === 'EXPIRED' ? 'tag-orange' : s === 'COMPLETED' ? 'tag-green' : 'tag-gray'
})

/** 页面加载（仅一次）：校验任务 id，并从缓存解析我的用户信息 */
onLoad((options) => {
	const id = Number(options && options.id)
	// 任务 id 非法时提示并退回上一页
	if (!Number.isFinite(id) || id <= 0) {
		uni.showToast({ title: '任务参数有误', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 800)
		return
	}
	taskId.value = id
	// 从本地缓存解析当前用户 id 与系统角色
	try {
		const me = JSON.parse(uni.getStorageSync('user_info') || '{}')
		myUserId.value = me.userId || 0
		systemRole.value = me.systemRole || 'USER'
	} catch (e) {}
})

// 每次页面显示：校验登录态后刷新任务详情（完成/取消后返回也会刷新）
onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	if (!taskId.value) return
	await load()
})

/** 拉取任务详情，并顺带加载我在该公司内的角色 */
async function load() {
	task.value = await taskApi.detail(taskId.value)
	if (task.value && task.value.companyId) {
		await loadCompanyRole()
	}
}

// 取消权限以接口返回的 myRole 为准（不能用路由参数）
async function loadCompanyRole() {
	try {
		const detail = await companyApi.detail(task.value.companyId)
		companyRole.value = (detail && detail.myRole) || ''
	} catch (e) {
		companyRole.value = ''
	}
}

/** 提交完成任务：可带提交说明，成功后关弹窗并刷新详情 */
async function doComplete() {
	submitting.value = true
	try {
		await taskApi.complete(taskId.value, { submitContent: submitContent.value })
		uni.showToast({ title: '提交成功', icon: 'success' })
		showComplete.value = false
		await load()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		submitting.value = false
	}
}

/** 打开取消任务确认弹窗（统一样式，见 confirm-dialog 组件） */
function cancelTask() {
	showCancelModal.value = true
}

/** 确认取消任务：调用取消接口，成功后刷新详情 */
async function doCancel() {
	if (canceling.value) return
	canceling.value = true
	try {
		await taskApi.cancel(taskId.value, '')
		uni.showToast({ title: '已取消', icon: 'none' })
		await load()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		canceling.value = false
	}
}
</script>

<style scoped>
/* 页面根节点：底部留白 */
.page {
	padding-bottom: 60rpx;
}

/* 任务标题：大字加粗 */
.title {
	font-size: 38rpx;
	font-weight: bold;
}

/* 属性标签行：允许换行 */
.tags-row {
	gap: 12rpx;
	margin-top: 16rpx;
	flex-wrap: wrap;
}

/* 基础信息区：顶部分隔线 */
.info-block {
	margin-top: 24rpx;
	border-top: 1rpx solid #f0f0f0;
	padding-top: 16rpx;
}

/* 单行信息：标签左、内容右 */
.info-row {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	padding: 10rpx 0;
	font-size: 28rpx;
}

/* 详情描述区块 */
.desc-block {
	margin-top: 24rpx;
}

/* 区块标题 */
.section-title {
	font-size: 30rpx;
	font-weight: bold;
	display: block;
	margin-bottom: 12rpx;
}

/* 描述正文 */
.desc {
	font-size: 28rpx;
	color: #606266;
	line-height: 1.6;
}

/* 被指派人标签组：允许换行 */
.member-tags {
	gap: 12rpx;
	flex-wrap: wrap;
}

/* 操作区按钮：纵向排列 */
.actions {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

/* 主按钮不可完成态：置灰占位且不可点击 */
.btn-disabled {
	background-color: #c0c4cc;
	color: #fff;
}

/* 完成提交弹窗：全屏半透明遮罩 */
.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 999;
}

/* 弹窗白色卡片：与统一确认弹窗外观保持一致 */
.modal {
	width: 560rpx;
	background-color: #fff;
	border-radius: 24rpx;
	padding: 40rpx;
}

/* 提交说明多行文本框 */
.textarea {
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx;
	font-size: 28rpx;
	width: 100%;
	box-sizing: border-box;
	min-height: 160rpx;
	margin: 20rpx 0;
}

/* 弹窗按钮行 */
.modal-btns {
	gap: 20rpx;
}
</style>
