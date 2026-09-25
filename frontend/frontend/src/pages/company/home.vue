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

		<!-- Tab 切换 -->
		<view class="row tabs">
			<view class="tab flex1" :class="{ active: tab === 'task' }" @click="switchTab('task')">任务</view>
			<view class="tab flex1" :class="{ active: tab === 'member' }" @click="switchTab('member')">成员</view>
			<view v-if="isManager" class="tab flex1" :class="{ active: tab === 'approve' }" @click="switchTab('approve')">
				审批<text v-if="pendingCount > 0" class="badge">{{ pendingCount }}</text>
			</view>
		</view>

		<!-- 任务列表 -->
		<view v-if="tab === 'task'">
			<view v-if="myRole && !isManager" class="row view-switch">
				<view class="tag" :class="taskView === 'grab' ? 'tag-blue' : 'tag-gray'" @click="changeView('grab')">待办</view>
				<view class="tag" :class="taskView === 'history' ? 'tag-blue' : 'tag-gray'" @click="changeView('history')">历史</view>
			</view>
			<view v-if="tasks.length === 0" class="empty muted">暂无任务</view>
			<view v-for="t in tasks" :key="t.id" class="card task-item" @click="openTask(t)">
				<view class="row">
					<text class="task-title flex1">{{ t.title }}</text>
					<text class="tag" :class="taskStatusTag(t)">{{ taskStatusText(t) }}</text>
				</view>
				<view class="row">
					<text class="tag" :class="t.taskType === 'GLOBAL' ? 'tag-blue' : 'tag-orange'">
						{{ t.taskType === 'GLOBAL' ? '全局抢单' : '指定任务' }}
					</text>
					<text v-if="t.visibility === 'RESTRICTED'" class="tag tag-gray">部分可见</text>
					<text v-if="t.isLate === 1" class="tag tag-red">超时完成</text>
				</view>
				<text class="muted">截止：{{ formatTime(t.deadlineAt) }}</text>
			</view>
			<button v-if="isManager" class="btn-primary publish-btn" @click="goCreateTask">发布任务</button>
		</view>

		<!-- 成员列表 -->
		<view v-if="tab === 'member'">
			<view v-for="m in members" :key="m.userId" class="card member-item">
				<view class="row">
					<text class="member-name flex1">{{ m.nickname || m.phone }}</text>
					<text class="tag" :class="roleTagClass(m.companyRole)">{{ roleText(m.companyRole) }}</text>
				</view>
				<view class="row">
					<text class="muted flex1">{{ m.phone }} · 加入于 {{ formatTime(m.joinedAt) }}</text>
					<view v-if="canManageMember(m)" class="row">
						<text class="link" :class="{ busy: memberActingId === m.userId }" @click="removeMember(m)">移除</text>
						<text class="link danger" :class="{ busy: memberActingId === m.userId }" @click="blacklistMember(m)">拉黑</text>
					</view>
				</view>
			</view>
			<view v-if="isFounder" class="card">
				<button class="btn-danger" :loading="dissolving" @click="dissolveCompany">解散公司</button>
			</view>
		</view>

		<!-- 审批 -->
		<view v-if="tab === 'approve' && isManager">
			<view class="row view-switch">
				<view class="tag" :class="approveView === 'pending' ? 'tag-blue' : 'tag-gray'" @click="changeApproveView('pending')">待审批</view>
				<view class="tag" :class="approveView === 'history' ? 'tag-blue' : 'tag-gray'" @click="changeApproveView('history')">全部历史</view>
			</view>
			<view v-if="applications.length === 0" class="empty muted">暂无申请</view>
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
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { companyApi, taskApi, applicationApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'
import { formatTime } from '../../utils/format.js'

const companyId = ref(0)
const companyName = ref('')
const myRole = ref('')
const detail = ref(null)
const tab = ref('task')
const tasks = ref([])
const taskView = ref('grab')
const members = ref([])
const applications = ref([])
const approveView = ref('pending')
const myUserId = ref(0)
const leaving = ref(false)
const dissolving = ref(false)
const memberActingId = ref(0)
const appActingId = ref(0)

const isManager = computed(() => myRole.value === 'FOUNDER' || myRole.value === 'MANAGER')
const isFounder = computed(() => myRole.value === 'FOUNDER')
// 创始人不能退出公司（需先解散），其余在职角色可主动离职
const canLeave = computed(() => !!myRole.value && myRole.value !== 'FOUNDER')
const pendingCount = computed(() => applications.value.filter(a => a.status === 'PENDING').length)

onLoad((options) => {
	companyId.value = Number(options.id)
	// uni-app H5 路由已解码过一次，这里不能再 decodeURIComponent（含裸 % 的公司名会抛 URIError）
	companyName.value = options.name || ''
	// 角色不取路由参数，统一等 companyApi.detail 返回的 myRole，避免按参数短暂渲染出错误权限
})

onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
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

async function loadDetail() {
	detail.value = await companyApi.detail(companyId.value)
	if (detail.value && detail.value.myRole) myRole.value = detail.value.myRole
}

async function loadTasks() {
	tasks.value = await taskApi.list(companyId.value, isManager.value ? '' : taskView.value)
}

async function loadMembers() {
	members.value = await companyApi.members(companyId.value)
}

async function loadApplications() {
	applications.value = approveView.value === 'pending'
		? await applicationApi.pending(companyId.value)
		: await applicationApi.history(companyId.value)
}

async function switchTab(t) {
	tab.value = t
	try {
		if (t === 'member') await loadMembers()
		if (t === 'approve') await loadApplications()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	}
}

async function changeView(v) {
	taskView.value = v
	await loadTasks()
}

async function changeApproveView(v) {
	approveView.value = v
	await loadApplications()
}

function openTask(t) {
	uni.navigateTo({ url: `/pages/task/detail?id=${t.id}` })
}

function goCreateTask() {
	uni.navigateTo({ url: `/pages/task/create?companyId=${companyId.value}` })
}

function copyInvite() {
	uni.setClipboardData({ data: detail.value.inviteCode })
}

function canManageMember(m) {
	if (!isManager.value) return false
	if (m.companyRole === 'FOUNDER') return false
	if (m.userId === myUserId.value) return false
	// 管理者不能移除/拉黑其他管理者以上的角色由后端兜底，这里仅创始人可操作管理者
	if (m.companyRole === 'MANAGER' && !isFounder.value) return false
	return true
}

async function removeMember(m) {
	if (memberActingId.value) return
	uni.showModal({
		title: '移除成员',
		content: `确定移除 ${m.nickname || m.phone} 吗？`,
		success: async (res) => {
			if (!res.confirm || memberActingId.value) return
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

function blacklistMember(m) {
	if (memberActingId.value) return
	uni.showModal({
		title: '拉黑成员',
		content: `确定拉黑 ${m.nickname || m.phone} 吗？拉黑后该成员将被移出公司`,
		editable: true,
		placeholderText: '拉黑原因（可选）',
		success: async (res) => {
			if (res.confirm) {
				await doBlacklist(m, res.content || '')
			}
		}
	})
}

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

function dissolveCompany() {
	if (dissolving.value) return
	uni.showModal({
		title: '解散公司',
		content: '解散后不可恢复，确定解散吗？',
		success: async (res) => {
			if (!res.confirm || dissolving.value) return
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

function reject(a) {
	if (appActingId.value) return
	uni.showModal({
		title: '拒绝申请',
		content: `确定拒绝 ${a.userNickname || a.userPhone} 的申请吗？`,
		editable: true,
		placeholderText: '拒绝理由（可选）',
		success: async (res) => {
			if (res.confirm) {
				await doReject(a, res.content || '')
			}
		}
	})
}

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

function leaveCompany() {
	if (leaving.value) return
	uni.showModal({
		title: '退出公司',
		content: `确定退出「${companyName.value}」吗？退出后需重新申请加入`,
		success: async (res) => {
			if (!res.confirm || leaving.value) return
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

function roleText(role) {
	return role === 'FOUNDER' ? '创始人' : role === 'MANAGER' ? '管理者' : '员工'
}

function roleTagClass(role) {
	return role === 'FOUNDER' ? 'tag-orange' : role === 'MANAGER' ? 'tag-blue' : 'tag-green'
}

function taskStatusText(t) {
	return t.status === 'PENDING' ? '进行中' : t.status === 'EXPIRED' ? '已超时' : t.status === 'COMPLETED' ? '已完成' : '已取消'
}

function taskStatusTag(t) {
	return t.status === 'PENDING' ? 'tag-blue' : t.status === 'EXPIRED' ? 'tag-orange' : t.status === 'COMPLETED' ? 'tag-green' : 'tag-gray'
}

function appStatusText(s) {
	return s === 'PENDING' ? '待审批' : s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已拒绝' : '已撤回'
}

function appStatusTag(s) {
	return s === 'PENDING' ? 'tag-orange' : s === 'APPROVED' ? 'tag-green' : s === 'REJECTED' ? 'tag-red' : 'tag-gray'
}
</script>

<style scoped>
.page {
	padding-bottom: 60rpx;
}

.header {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.company-name {
	font-size: 36rpx;
	font-weight: bold;
}

.invite-row {
	gap: 16rpx;
}

.tabs {
	background-color: #fff;
	margin: 0 24rpx;
	border-radius: 16rpx;
	padding: 8rpx;
}

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

.view-switch {
	gap: 16rpx;
	padding: 20rpx 24rpx 0;
}

.empty {
	text-align: center;
	padding: 60rpx 0;
	display: block;
}

.task-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.task-title {
	font-size: 32rpx;
	font-weight: bold;
}

.publish-btn {
	margin: 30rpx 24rpx;
}

.member-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.member-name {
	font-size: 32rpx;
}

.link {
	color: #2979ff;
	font-size: 26rpx;
	margin-left: 24rpx;
}

.link.danger {
	color: #fa3534;
}

.link.busy {
	color: #c0c4cc;
}

.app-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.approve-btns {
	gap: 20rpx;
}

.mini {
	font-size: 26rpx;
	padding: 0 40rpx;
	line-height: 64rpx;
	height: 64rpx;
}

.leave-card {
	margin-top: 40rpx;
}
</style>
