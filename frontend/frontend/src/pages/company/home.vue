<template>
	<view class="page">
		<!-- 顶部信息 -->
		<view class="card header">
			<view class="row">
				<text class="company-name flex1">{{ companyName }}</text>
				<text class="tag" :class="roleTagClass(myRole)">{{ roleText(myRole) }}</text>
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
			<view v-if="!isManager" class="row view-switch">
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
						<text class="link" @click="removeMember(m)">移除</text>
						<text class="link danger" @click="blacklistMember(m)">拉黑</text>
					</view>
				</view>
			</view>
			<view v-if="isFounder" class="card">
				<button class="btn-danger" @click="dissolveCompany">解散公司</button>
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
					<button class="btn-primary mini" @click="approve(a)">通过</button>
					<button class="btn-danger mini" @click="reject(a)">拒绝</button>
				</view>
				<text v-if="a.status === 'REJECTED' && a.rejectReason" class="muted">拒绝理由：{{ a.rejectReason }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { companyApi, taskApi, applicationApi } from '../../api/index.js'

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

const isManager = computed(() => myRole.value === 'FOUNDER' || myRole.value === 'MANAGER')
const isFounder = computed(() => myRole.value === 'FOUNDER')
const pendingCount = computed(() => applications.value.filter(a => a.status === 'PENDING').length)

onLoad((options) => {
	companyId.value = Number(options.id)
	companyName.value = decodeURIComponent(options.name || '')
	myRole.value = options.role || ''
})

onShow(async () => {
	try {
		const me = JSON.parse(uni.getStorageSync('user_info') || '{}')
		myUserId.value = me.userId || 0
	} catch (e) {}
	await Promise.all([loadDetail(), loadTasks()])
	if (tab.value === 'member') await loadMembers()
	if (tab.value === 'approve') await loadApplications()
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

function switchTab(t) {
	tab.value = t
	if (t === 'member') loadMembers()
	if (t === 'approve') loadApplications()
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
	uni.showModal({
		title: '移除成员',
		content: `确定移除 ${m.nickname || m.phone} 吗？`,
		success: async (res) => {
			if (res.confirm) {
				await companyApi.removeMember(companyId.value, m.userId)
				uni.showToast({ title: '已移除', icon: 'none' })
				await loadMembers()
			}
		}
	})
}

function blacklistMember(m) {
	// #ifdef H5
	const reason = window.prompt('拉黑原因（可留空）', '')
	doBlacklist(m, reason)
	// #endif
	// #ifndef H5
	doBlacklist(m, '')
	// #endif
}

async function doBlacklist(m, reason) {
	await companyApi.blacklist(companyId.value, { userId: m.userId, reason })
	uni.showToast({ title: '已拉黑', icon: 'none' })
	await loadMembers()
}

function dissolveCompany() {
	uni.showModal({
		title: '解散公司',
		content: '解散后不可恢复，确定解散吗？',
		success: async (res) => {
			if (res.confirm) {
				await companyApi.dissolve(companyId.value)
				uni.showToast({ title: '已解散', icon: 'none' })
				setTimeout(() => uni.navigateBack(), 800)
			}
		}
	})
}

async function approve(a) {
	await applicationApi.approve(a.id)
	uni.showToast({ title: '已通过', icon: 'none' })
	await loadApplications()
}

function reject(a) {
	// #ifdef H5
	const reason = window.prompt('拒绝理由（可留空）', '')
	if (reason !== null) doReject(a, reason)
	// #endif
	// #ifndef H5
	doReject(a, '')
	// #endif
}

async function doReject(a, reason) {
	await applicationApi.reject(a.id, reason)
	uni.showToast({ title: '已拒绝', icon: 'none' })
	await loadApplications()
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

function formatTime(t) {
	return t ? t.replace('T', ' ').substring(0, 16) : ''
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
</style>
