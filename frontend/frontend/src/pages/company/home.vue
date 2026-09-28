<template>
	<view class="page">
		<!-- 顶部信息 -->
		<view class="card header">
			<view class="row">
				<text class="company-name flex1">{{ companyName }}</text>
				<text v-if="myRole" class="tag" :class="roleTagClass(myRole)">{{ roleText(myRole) }}</text>
			</view>
			<view v-if="detail && detail.inviteCode" class="row invite-row">
				<text class="muted">邀请码：{{ detail.inviteCode }}</text>
				<text class="link" @click="copyInvite">复制</text>
			</view>
		</view>

		<!-- 主 Tab 切换：任务 / 成员 / 审批（审批仅管理者可见，带待办角标） -->
		<view class="row tabs">
			<view class="tab flex1" :class="{ active: tab === 'task' }" @click="switchTab('task')">任务</view>
			<view class="tab flex1" :class="{ active: tab === 'member' }" @click="switchTab('member')">成员</view>
			<view v-if="isManager" class="tab flex1" :class="{ active: tab === 'approve' }" @click="switchTab('approve')">
				审批<text v-if="pendingCount > 0" class="badge">{{ pendingCount }}</text>
			</view>
		</view>

		<!-- 任务列表 Tab -->
		<view v-if="tab === 'task'">
			<!-- 员工视角切换：待办 / 历史（管理者看全量列表，无需切换） -->
			<view v-if="myRole && !isManager" class="row view-switch">
				<view class="tag" :class="taskView === 'grab' ? 'tag-blue' : 'tag-gray'" @click="changeView('grab')">待办</view>
				<view class="tag" :class="taskView === 'history' ? 'tag-blue' : 'tag-gray'" @click="changeView('history')">历史</view>
			</view>
			<view v-if="tasks.length === 0" class="empty muted">暂无任务</view>
			<!-- 单条任务卡片：标题 + 状态 + 类型/可见性/超时标签 + 截止时间 -->
			<view v-for="t in tasks" :key="t.id" class="card task-item" @click="openTask(t)">
				<view class="row">
					<text class="task-title flex1">{{ t.title }}</text>
					<text class="tag" :class="taskStatusTag(t)">{{ taskStatusText(t) }}</text>
				</view>
				<view class="row">
					<text class="tag" :class="t.taskType === 'GLOBAL' ? 'tag-blue' : 'tag-orange'">
						{{ t.taskType === 'GLOBAL' ? '全局抢单' : '指定任务' }}
					</text>
					<text v-if="t.isLate === 1" class="tag tag-red">超时完成</text>
				</view>
				<text class="muted">截止：{{ formatTime(t.deadlineAt) }}</text>
			</view>
			<!-- 发布任务入口：仅创始人/管理者可见 -->
			<button v-if="isManager" class="btn-primary publish-btn" @click="goCreateTask">发布任务</button>
		</view>

		<!-- 成员列表 Tab -->
		<view v-if="tab === 'member'">
			<!-- 单个成员卡片：昵称 + 角色 + 联系方式/加入时间 + 管理操作 -->
			<view v-for="m in members" :key="m.userId" class="card member-item">
				<view class="row">
					<text class="member-name flex1">{{ m.nickname || m.phone }}</text>
					<text class="tag" :class="roleTagClass(m.companyRole)">{{ roleText(m.companyRole) }}</text>
				</view>
				<view class="row">
					<text class="muted flex1">{{ m.phone }} · 加入于 {{ formatTime(m.joinedAt) }}</text>
					<!-- 移除/拉黑：满足 canManageMember 权限规则才显示 -->
					<view v-if="canManageMember(m)" class="row">
						<text class="link" :class="{ busy: memberActingId === m.userId }" @click="removeMember(m)">移除</text>
						<text class="link danger" :class="{ busy: memberActingId === m.userId }" @click="blacklistMember(m)">拉黑</text>
					</view>
				</view>
			</view>
			<!-- 解散公司：仅创始人可见，危险操作 -->
			<view v-if="isFounder" class="card">
				<button class="btn-danger" :loading="dissolving" @click="dissolveCompany">解散公司</button>
			</view>
		</view>

		<!-- 审批 Tab：仅创始人/管理者可见 -->
		<view v-if="tab === 'approve' && isManager">
			<!-- 审批视角切换：待审批 / 全部历史 -->
			<view class="row view-switch">
				<view class="tag" :class="approveView === 'pending' ? 'tag-blue' : 'tag-gray'" @click="changeApproveView('pending')">待审批</view>
				<view class="tag" :class="approveView === 'history' ? 'tag-blue' : 'tag-gray'" @click="changeApproveView('history')">全部历史</view>
			</view>
			<view v-if="applications.length === 0" class="empty muted">暂无申请</view>
			<!-- 单条申请卡片：申请人 + 申请身份 + 状态 + 通过/拒绝操作 -->
			<view v-for="a in applications" :key="a.id" class="card app-item">
				<view class="row">
					<text class="flex1">{{ a.userNickname || a.userPhone }} 申请{{ a.applyRole === 'MANAGER' ? '管理者' : '员工' }}</text>
					<text class="tag" :class="appStatusTag(a.status)">{{ appStatusText(a.status) }}</text>
				</view>
				<text class="muted">{{ a.userPhone }} · {{ formatTime(a.createdAt) }}</text>
				<view v-if="a.status === 'PENDING'" class="row approve-btns">
					<button class="btn-primary mini" :loading="appActingId === a.id" @click="approve(a)">通过</button>
					<button class="btn-danger mini" :loading="appActingId === a.id" @click="reject(a)">拒绝</button>
				</view>
				<text v-if="a.status === 'REJECTED' && a.rejectReason" class="muted">拒绝理由：{{ a.rejectReason }}</text>
			</view>
		</view>

		<!-- 退出公司（非创始人可见） -->
		<view v-if="canLeave" class="card leave-card">
			<button class="btn-danger" :loading="leaving" @click="leaveCompany">退出公司</button>
		</view>
		<!-- 统一确认弹窗：移除/拉黑/解散/拒绝/退出公司 共用 -->
		<confirm-dialog
			v-model:visible="confirmDialog.visible"
			:title="confirmDialog.title"
			:content="confirmDialog.content"
			:show-input="confirmDialog.showInput"
			:placeholder="confirmDialog.placeholder"
			@confirm="onDialogConfirm"
		/>
	</view>
</template>

<script setup>
// ============================================================
// 公司主页：单个公司内部的管理中枢
// 三个 Tab：任务（列表/发布）、成员（移除/拉黑/解散）、审批（加入申请）
// 我的角色 myRole 由 companyApi.detail 接口返回为准：
// FOUNDER 创始人 / MANAGER 管理者 / EMPLOYEE 员工
// ============================================================
import { ref, computed, reactive } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { companyApi, taskApi, applicationApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'
import { formatTime } from '../../utils/format.js'
// 统一确认弹窗组件（样式与退出登录弹窗一致）
import ConfirmDialog from '../../components/confirm-dialog/confirm-dialog.vue'

// 公司 id（路由参数传入）
const companyId = ref(0)
// 公司名称（仅用于展示，来自路由参数）
const companyName = ref('')
// 我在该公司内的角色（以接口返回为准，不用路由参数）
const myRole = ref('')
// 公司详情（含邀请码 inviteCode、我的角色 myRole）
const detail = ref(null)
// 当前主 Tab：task 任务 / member 成员 / approve 审批
const tab = ref('task')
// 任务列表
const tasks = ref([])
// 员工任务视角：grab 待办 / history 历史
const taskView = ref('grab')
// 成员列表
const members = ref([])
// 审批申请列表
const applications = ref([])
// 审批视角：pending 待审批 / history 全部历史
const approveView = ref('pending')
// 当前用户 id（从本地缓存解析，用于避免对自己操作）
const myUserId = ref(0)
// 退出公司请求中标志
const leaving = ref(false)
// 解散公司请求中标志
const dissolving = ref(false)
// 正在操作的成员 id（移除/拉黑防重复，0 表示空闲）
const memberActingId = ref(0)
// 正在操作的申请 id（通过/拒绝防重复，0 表示空闲）
const appActingId = ref(0)

// 是否管理者（创始人或管理者：可发布任务、审批、管理成员）
const isManager = computed(() => myRole.value === 'FOUNDER' || myRole.value === 'MANAGER')
// 是否创始人（可解散公司、可操作管理者）
const isFounder = computed(() => myRole.value === 'FOUNDER')
// 创始人不能退出公司（需先解散），其余在职角色可主动离职
const canLeave = computed(() => !!myRole.value && myRole.value !== 'FOUNDER')
// 待审批数量：用于审批 Tab 的红色角标
const pendingCount = computed(() => applications.value.filter(a => a.status === 'PENDING').length)

/** 页面加载（仅一次）：从路由参数取公司 id 与名称 */
onLoad((options) => {
	companyId.value = Number(options.id)
	// uni-app H5 路由已解码过一次，这里不能再 decodeURIComponent（含裸 % 的公司名会抛 URIError）
	companyName.value = options.name || ''
	// 角色不取路由参数，统一等 companyApi.detail 返回的 myRole，避免按参数短暂渲染出错误权限
})

// 每次页面显示：校验登录态 -> 取我的用户 id -> 按序加载数据
onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	// 从本地缓存解析当前用户 id（解析失败则忽略，保持 0）
	try {
		const me = JSON.parse(uni.getStorageSync('user_info') || '{}')
		myUserId.value = me.userId || 0
	} catch (e) {}
	// 先拿到权威角色，再按角色取任务列表（员工视图与管理者全量列表不同）
	await loadDetail()
	await loadTasks()
	// 角标依赖待审批列表，进入页面时就要加载（不能只在切到审批 Tab 时才加载）
	if (isManager.value) await loadApplications()
	if (tab.value === 'member') await loadMembers()
})

/** 拉取公司详情，并同步我的权威角色 myRole */
async function loadDetail() {
	detail.value = await companyApi.detail(companyId.value)
	if (detail.value && detail.value.myRole) myRole.value = detail.value.myRole
}

/** 拉取任务列表：管理者传空 view 取全量，员工按待办/历史视角 */
async function loadTasks() {
	tasks.value = await taskApi.list(companyId.value, isManager.value ? '' : taskView.value)
}

/** 拉取成员列表 */
async function loadMembers() {
	members.value = await companyApi.members(companyId.value)
}

/** 拉取审批列表：按当前审批视角（待审批 / 全部历史） */
async function loadApplications() {
	applications.value = approveView.value === 'pending'
		? await applicationApi.pending(companyId.value)
		: await applicationApi.history(companyId.value)
}

/** 切换主 Tab，并按需懒加载对应数据 */
async function switchTab(t) {
	tab.value = t
	try {
		if (t === 'member') await loadMembers()
		if (t === 'approve') await loadApplications()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	}
}

/** 切换员工任务视角（待办/历史）并刷新列表 */
async function changeView(v) {
	taskView.value = v
	await loadTasks()
}

/** 切换审批视角（待审批/全部历史）并刷新列表 */
async function changeApproveView(v) {
	approveView.value = v
	await loadApplications()
}

/** 打开任务详情页 */
function openTask(t) {
	uni.navigateTo({ url: `/pages/task/detail?id=${t.id}` })
}

/** 跳转发布任务页（携带公司 id） */
function goCreateTask() {
	uni.navigateTo({ url: `/pages/task/create?companyId=${companyId.value}` })
}

/** 复制邀请码到剪贴板 */
function copyInvite() {
	uni.setClipboardData({ data: detail.value.inviteCode })
}

/**
 * 判断能否对成员 m 执行移除/拉黑：
 * 1. 操作者须为管理者 2. 创始人不可被操作
 * 3. 不能操作自己 4. 管理者仅创始人可操作
 */
function canManageMember(m) {
	if (!isManager.value) return false
	if (m.companyRole === 'FOUNDER') return false
	if (m.userId === myUserId.value) return false
	// 管理者不能移除/拉黑其他管理者以上的角色由后端兜底，这里仅创始人可操作管理者
	if (m.companyRole === 'MANAGER' && !isFounder.value) return false
	return true
}

// ---------- 统一确认弹窗（替代原生 uni.showModal，样式与退出登录弹窗一致） ----------

// 弹窗状态：显隐 + 标题/内容/输入框配置 + 确认后要执行的动作
const confirmDialog = reactive({
	visible: false,
	title: '',
	content: '',
	showInput: false,
	placeholder: '',
	action: null
})

/** 打开统一确认弹窗：opts 传 title / content / showInput / placeholder / action */
function showConfirm(opts) {
	Object.assign(confirmDialog, { showInput: false, placeholder: '', action: null }, opts, { visible: true })
}

/** 弹窗确认回调：执行对应动作（value 为输入框内容，无输入框时为空串） */
async function onDialogConfirm(value) {
	const action = confirmDialog.action
	if (action) await action(value)
}

/** 移除成员：弹窗二次确认，成功后刷新成员列表 */
function removeMember(m) {
	if (memberActingId.value) return
	showConfirm({
		title: '移除成员',
		content: `确定移除 ${m.nickname || m.phone} 吗？`,
		action: async () => {
			memberActingId.value = m.userId
			try {
				await companyApi.removeMember(companyId.value, m.userId)
				uni.showToast({ title: '已移除', icon: 'none' })
				await loadMembers()
			} catch (e) {
				// 错误提示已在 request 层统一处理
			} finally {
				memberActingId.value = 0
			}
		}
	})
}

/** 拉黑成员：弹窗内可填原因，拉黑后成员被移出且无法再申请加入 */
function blacklistMember(m) {
	if (memberActingId.value) return
	showConfirm({
		title: '拉黑成员',
		content: `确定拉黑 ${m.nickname || m.phone} 吗？拉黑后该成员将被移出公司`,
		showInput: true,
		placeholder: '拉黑原因（可选）',
		action: async (reason) => doBlacklist(m, reason || '')
	})
}

/** 执行拉黑请求（memberActingId 防重复提交） */
async function doBlacklist(m, reason) {
	if (memberActingId.value) return
	memberActingId.value = m.userId
	try {
		await companyApi.blacklist(companyId.value, { userId: m.userId, reason })
		uni.showToast({ title: '已拉黑', icon: 'none' })
		await loadMembers()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		memberActingId.value = 0
	}
}

/** 解散公司：仅创始人，二次确认后解散并返回上一页 */
function dissolveCompany() {
	if (dissolving.value) return
	showConfirm({
		title: '解散公司',
		content: '解散后不可恢复，确定解散吗？',
		action: async () => {
			dissolving.value = true
			try {
				await companyApi.dissolve(companyId.value)
				uni.showToast({ title: '已解散', icon: 'none' })
				setTimeout(() => uni.navigateBack(), 800)
			} catch (e) {
				// 错误提示已在 request 层统一处理
			} finally {
				dissolving.value = false
			}
		}
	})
}

/** 通过申请（appActingId 防重复），成功后刷新审批列表 */
async function approve(a) {
	if (appActingId.value) return
	appActingId.value = a.id
	try {
		await applicationApi.approve(a.id)
		uni.showToast({ title: '已通过', icon: 'none' })
		await loadApplications()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		appActingId.value = 0
	}
}

/** 拒绝申请：弹窗内可填拒绝理由 */
function reject(a) {
	if (appActingId.value) return
	showConfirm({
		title: '拒绝申请',
		content: `确定拒绝 ${a.userNickname || a.userPhone} 的申请吗？`,
		showInput: true,
		placeholder: '拒绝理由（可选）',
		action: async (reason) => doReject(a, reason || '')
	})
}

/** 执行拒绝请求（appActingId 防重复提交） */
async function doReject(a, reason) {
	if (appActingId.value) return
	appActingId.value = a.id
	try {
		await applicationApi.reject(a.id, reason)
		uni.showToast({ title: '已拒绝', icon: 'none' })
		await loadApplications()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		appActingId.value = 0
	}
}

/** 退出公司：非创始人，确认后退出并回到「我的公司」列表页 */
function leaveCompany() {
	if (leaving.value) return
	showConfirm({
		title: '退出公司',
		content: `确定退出「${companyName.value}」吗？退出后需重新申请加入`,
		action: async () => {
			leaving.value = true
			try {
				await companyApi.leave(companyId.value)
				uni.showToast({ title: '已退出公司', icon: 'none' })
				setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 800)
			} catch (e) {
				// 错误提示已在 request 层统一处理
			} finally {
				leaving.value = false
			}
		}
	})
}

/** 公司角色枚举 -> 中文文案 */
function roleText(role) {
	return role === 'FOUNDER' ? '创始人' : role === 'MANAGER' ? '管理者' : '员工'
}

/** 公司角色 -> 标签颜色（创始人橙 / 管理者蓝 / 员工绿） */
function roleTagClass(role) {
	return role === 'FOUNDER' ? 'tag-orange' : role === 'MANAGER' ? 'tag-blue' : 'tag-green'
}

/** 任务状态枚举 -> 中文文案 */
function taskStatusText(t) {
	return t.status === 'PENDING' ? '进行中' : t.status === 'EXPIRED' ? '已超时' : t.status === 'COMPLETED' ? '已完成' : '已取消'
}

/** 任务状态 -> 标签颜色（进行中蓝 / 超时橙 / 完成绿 / 取消灰） */
function taskStatusTag(t) {
	return t.status === 'PENDING' ? 'tag-blue' : t.status === 'EXPIRED' ? 'tag-orange' : t.status === 'COMPLETED' ? 'tag-green' : 'tag-gray'
}

/** 申请状态枚举 -> 中文文案 */
function appStatusText(s) {
	return s === 'PENDING' ? '待审批' : s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已拒绝' : '已撤回'
}

/** 申请状态 -> 标签颜色（待审批橙 / 通过绿 / 拒绝红 / 撤回灰） */
function appStatusTag(s) {
	return s === 'PENDING' ? 'tag-orange' : s === 'APPROVED' ? 'tag-green' : s === 'REJECTED' ? 'tag-red' : 'tag-gray'
}
</script>

<style scoped>
/* 页面根节点：底部留白 */
.page {
	padding-bottom: 60rpx;
}

/* 公司信息头部卡片：纵向排列 */
.header {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

/* 公司名称：大字加粗 */
.company-name {
	font-size: 36rpx;
	font-weight: bold;
}

/* 邀请码行：文案 + 复制按钮 */
.invite-row {
	gap: 16rpx;
}

/* 主 Tab 栏：白色圆角容器 */
.tabs {
	background-color: #fff;
	margin: 0 24rpx;
	border-radius: 16rpx;
	padding: 8rpx;
}

/* Tab 项：默认灰，选中蓝加粗 */
.tab {
	text-align: center;
	padding: 20rpx 0;
	color: #909399;
	font-size: 30rpx;
	position: relative;
}

.tab.active {
	color: #2979ff;
	font-weight: bold;
}

/* 审批 Tab 待办数量角标：右上角红色胶囊 */
.badge {
	position: absolute;
	top: 8rpx;
	right: 24rpx;
	background-color: #fa3534;
	color: #fff;
	font-size: 20rpx;
	border-radius: 20rpx;
	padding: 2rpx 10rpx;
}

/* 列表内视角切换（待办/历史等） */
.view-switch {
	gap: 16rpx;
	padding: 20rpx 24rpx 0;
}

/* 空列表占位文案 */
.empty {
	text-align: center;
	padding: 60rpx 0;
	display: block;
}

/* 任务卡片：纵向排列 */
.task-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

/* 任务标题：加粗 */
.task-title {
	font-size: 32rpx;
	font-weight: bold;
}

/* 发布任务按钮：左右留出边距 */
.publish-btn {
	margin: 30rpx 24rpx;
}

/* 成员卡片：纵向排列 */
.member-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

/* 成员昵称 */
.member-name {
	font-size: 32rpx;
}

/* 可点击文字操作（复制/移除等） */
.link {
	color: #2979ff;
	font-size: 26rpx;
	margin-left: 24rpx;
}

/* 危险文字操作（拉黑） */
.link.danger {
	color: #fa3534;
}

/* 操作中禁用态：置灰 */
.link.busy {
	color: #c0c4cc;
}

/* 审批申请卡片：纵向排列 */
.app-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

/* 通过/拒绝按钮行 */
.approve-btns {
	gap: 20rpx;
}

/* 小号按钮：审批操作用 */
.mini {
	font-size: 26rpx;
	padding: 0 40rpx;
	line-height: 64rpx;
	height: 64rpx;
}

/* 退出公司卡片：与上方内容拉开距离 */
.leave-card {
	margin-top: 40rpx;
}
</style>
