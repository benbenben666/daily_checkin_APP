<template>
	<view class="page">
		<!-- 发布任务表单卡片 -->
		<view class="card">
			<!-- 任务标题：必填 -->
			<text class="label">任务标题 *</text>
			<input class="input" v-model="form.title" placeholder="请输入任务标题" maxlength="200" />

			<!-- 任务详情：选填的长文本说明 -->
			<text class="label">任务详情</text>
			<textarea class="textarea" v-model="form.description" placeholder="任务详情说明（可选）" />

			<!-- 任务类型：全局抢单（全员可抢）/ 指定成员（仅指派人可完成） -->
			<text class="label">任务类型 *</text>
			<view class="row seg">
				<view class="seg-item flex1" :class="{ active: form.taskType === 'GLOBAL' }" @click="form.taskType = 'GLOBAL'">全局抢单</view>
				<view class="seg-item flex1" :class="{ active: form.taskType === 'ASSIGNED' }" @click="form.taskType = 'ASSIGNED'">指定成员</view>
			</view>

			<!-- 限时时长：数值 + 单位（分钟/小时/天），5 分钟 ~ 30 天 -->
			<text class="label">限时时长（5 分钟 ~ 30 天）*</text>
			<view class="row limit-row">
				<input class="input flex1" type="number" v-model="limitValue" placeholder="数值" />
				<view class="row unit-seg">
					<view class="unit" :class="{ active: limitUnit === 'min' }" @click="limitUnit = 'min'">分钟</view>
					<view class="unit" :class="{ active: limitUnit === 'hour' }" @click="limitUnit = 'hour'">小时</view>
					<view class="unit" :class="{ active: limitUnit === 'day' }" @click="limitUnit = 'day'">天</view>
				</view>
			</view>

			<!-- 允许超时补交：开启后超时任务仍可在补交截止前提交 -->
			<view class="row switch-row">
				<text class="flex1">允许超时补交</text>
				<switch :checked="allowLate" @change="allowLate = $event.detail.value" color="#2979ff" />
			</view>

			<!-- 指派人选择：仅任务类型为「指定成员」时显示 -->
			<view v-if="form.taskType === 'ASSIGNED'">
				<text class="label">指派给 *（可多选，仅被指派人可见并可完成；公司创始人/管理员始终可见）</text>
				<view class="member-pick">
					<view v-for="m in members" :key="m.userId" class="pick-item"
						:class="{ checked: form.assigneeIds.includes(m.userId) }" @click="toggleAssignee(m.userId)">
						{{ m.nickname || m.phone }}
					</view>
				</view>
			</view>

			<!-- 发布按钮：loading 防重复提交 -->
			<button class="btn-primary" :loading="loading" @click="submit">发布任务</button>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 发布任务页：公司管理者发布限时任务
// 支持：任务类型（全局抢单/指定成员）、限时时长、超时补交、指派人多选
// 可见性由任务类型决定：全局任务全员可见；指定任务仅被指派人可见
// （公司创始人/管理员始终可见，但只有被指派人可完成）
// ============================================================
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { companyApi, taskApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

// 公司 id（路由参数传入）
const companyId = ref(0)
// 公司成员列表（用于指派人多选）
const members = ref([])
// 发布请求中标志
const loading = ref(false)
// 是否允许超时补交（默认允许）
const allowLate = ref(true)
// 限时时长数值（配合单位换算成分钟）
const limitValue = ref('30')
// 限时时长单位：min 分钟 / hour 小时 / day 天
const limitUnit = ref('min')

// 任务表单数据（与后端 CreateTaskRequest 对应）
const form = reactive({
	title: '',            // 任务标题
	description: '',      // 任务详情说明
	taskType: 'GLOBAL',   // GLOBAL 全局抢单 / ASSIGNED 指定成员
	assigneeIds: []       // 被指派人 id 集合（ASSIGNED 时必填）
})

// 页面加载：校验登录态与公司参数，并拉取成员列表供选择
onLoad(async (options) => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	const id = Number(options && options.companyId)
	// 公司 id 非法时提示并退回上一页
	if (!Number.isFinite(id) || id <= 0) {
		uni.showToast({ title: '公司参数有误', icon: 'none' })
		setTimeout(() => uni.navigateBack(), 800)
		return
	}
	companyId.value = id
	members.value = await companyApi.members(companyId.value)
})

/** 切换指派人选中状态（多选互不影响） */
function toggleAssignee(id) {
	const i = form.assigneeIds.indexOf(id)
	if (i >= 0) form.assigneeIds.splice(i, 1)
	else form.assigneeIds.push(id)
}

/** 把「数值 + 单位」换算成分钟；非法输入返回 0 */
function toMinutes() {
	const v = Number(limitValue.value)
	if (!v || v <= 0) return 0
	if (limitUnit.value === 'min') return v
	if (limitUnit.value === 'hour') return v * 60
	return v * 60 * 24
}

/**
 * 发布任务：本地校验（标题、时长范围、指派人）
 * -> 换算时长 -> 提交 -> 成功后返回公司主页
 */
async function submit() {
	if (!form.title.trim()) {
		uni.showToast({ title: '请输入任务标题', icon: 'none' })
		return
	}
	const minutes = toMinutes()
	// 时长限制：5 分钟 ~ 30 天（43200 分钟）
	if (minutes < 5 || minutes > 43200) {
		uni.showToast({ title: '限时需在 5 分钟 ~ 30 天之间', icon: 'none' })
		return
	}
	// 指定成员任务必须选择被指派人
	if (form.taskType === 'ASSIGNED' && form.assigneeIds.length === 0) {
		uni.showToast({ title: '请选择被指派人', icon: 'none' })
		return
	}
	loading.value = true
	try {
		await taskApi.create(companyId.value, {
			title: form.title.trim(),
			description: form.description,
			taskType: form.taskType,
			timeLimitMinutes: minutes,
			allowLateSubmit: allowLate.value ? 1 : 0,
			// 与任务类型无关的数组传空，避免提交脏数据
			assigneeIds: form.taskType === 'ASSIGNED' ? form.assigneeIds : []
		})
		uni.showToast({ title: '发布成功', icon: 'success' })
		// 延迟返回，保证成功提示可见
		setTimeout(() => uni.navigateBack(), 800)
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
/* 页面根节点：底部留白 */
.page {
	padding-bottom: 60rpx;
}

/* 表单区块标题 */
.label {
	font-size: 28rpx;
	color: #606266;
	margin: 24rpx 0 12rpx;
	display: block;
}

/* 任务详情多行文本框 */
.textarea {
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx 24rpx;
	font-size: 28rpx;
	width: 100%;
	box-sizing: border-box;
	min-height: 160rpx;
}

/* 分段选择器行（任务类型） */
.seg {
	gap: 20rpx;
}

/* 分段选项：默认灰底，选中蓝边高亮 */
.seg-item {
	text-align: center;
	padding: 20rpx 0;
	background-color: #f5f6f8;
	border-radius: 12rpx;
	border: 2rpx solid transparent;
}

.seg-item.active {
	border-color: #2979ff;
	color: #2979ff;
	background-color: #e8f1ff;
}

/* 限时时长行：数值输入 + 单位选择 */
.limit-row {
	gap: 16rpx;
}

/* 单位选择组 */
.unit-seg {
	gap: 8rpx;
}

/* 单位选项：默认灰底，选中蓝底白字 */
.unit {
	padding: 16rpx 24rpx;
	background-color: #f5f6f8;
	border-radius: 12rpx;
	font-size: 26rpx;
}

.unit.active {
	background-color: #2979ff;
	color: #fff;
}

/* 超时补交开关行：底部分隔线 */
.switch-row {
	padding: 20rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
}

/* 成员多选区：横向流式排列 */
.member-pick {
	display: flex;
	flex-direction: row;
	flex-wrap: wrap;
	gap: 16rpx;
}

/* 成员选择项：胶囊形，选中蓝边高亮 */
.pick-item {
	padding: 12rpx 28rpx;
	background-color: #f5f6f8;
	border-radius: 32rpx;
	font-size: 26rpx;
	border: 2rpx solid transparent;
}

.pick-item.checked {
	background-color: #e8f1ff;
	border-color: #2979ff;
	color: #2979ff;
}

/* 发布按钮与上方内容留出间距 */
.btn-primary {
	margin-top: 40rpx;
}
</style>
