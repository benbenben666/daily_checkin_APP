<template>
	<view class="page">
		<view class="card">
			<text class="label">邀请码</text>
			<input class="input" v-model="inviteCode" placeholder="请输入 8 位邀请码" maxlength="8" />

			<text class="label">申请身份</text>
			<view class="row roles">
				<view class="role-item flex1" :class="{ active: applyRole === 'EMPLOYEE' }" @click="applyRole = 'EMPLOYEE'">员工</view>
				<view class="role-item flex1" :class="{ active: applyRole === 'MANAGER' }" @click="applyRole = 'MANAGER'">管理者</view>
			</view>
			<text class="muted">管理者申请需创始人审批；每天最多申请 3 次</text>

			<button class="btn-primary" :loading="loading" @click="submit">提交申请</button>
		</view>

		<view class="card">
			<text class="section-title">我的申请记录</text>
			<view v-if="myApps.length === 0" class="muted">暂无申请记录</view>
			<view v-for="a in myApps" :key="a.id" class="app-item">
				<view class="row">
					<text class="flex1">{{ a.companyName }}</text>
					<text class="tag" :class="statusTagClass(a.status)">{{ statusText(a.status) }}</text>
				</view>
				<view class="row">
					<text class="muted flex1">申请{{ a.applyRole === 'MANAGER' ? '管理者' : '员工' }} · {{ formatTime(a.createdAt) }}</text>
					<text v-if="a.status === 'PENDING'" class="link" @click="cancelApp(a)">撤回</text>
				</view>
				<text v-if="a.status === 'REJECTED' && a.rejectReason" class="muted">拒绝理由：{{ a.rejectReason }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { companyApi, applicationApi } from '../../api/index.js'

const inviteCode = ref('')
const applyRole = ref('EMPLOYEE')
const loading = ref(false)
const myApps = ref([])

onShow(loadApps)

async function loadApps() {
	myApps.value = await applicationApi.mine()
}

async function submit() {
	if (!inviteCode.value.trim()) {
		uni.showToast({ title: '请输入邀请码', icon: 'none' })
		return
	}
	loading.value = true
	try {
		await companyApi.join({ inviteCode: inviteCode.value.trim(), applyRole: applyRole.value })
		uni.showToast({ title: '申请已提交，等待审批', icon: 'none' })
		inviteCode.value = ''
		await loadApps()
	} catch (e) {
	} finally {
		loading.value = false
	}
}

async function cancelApp(a) {
	await applicationApi.cancel(a.id)
	uni.showToast({ title: '已撤回', icon: 'none' })
	await loadApps()
}

function statusText(s) {
	return s === 'PENDING' ? '待审批' : s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已拒绝' : '已撤回'
}

function statusTagClass(s) {
	return s === 'PENDING' ? 'tag-orange' : s === 'APPROVED' ? 'tag-green' : s === 'REJECTED' ? 'tag-red' : 'tag-gray'
}

function formatTime(t) {
	return t ? t.replace('T', ' ').substring(0, 16) : ''
}
</script>

<style scoped>
.page {
	padding-bottom: 40rpx;
}

.label {
	font-size: 28rpx;
	color: #606266;
	margin: 16rpx 0;
	display: block;
}

.roles {
	gap: 20rpx;
	margin-bottom: 16rpx;
}

.role-item {
	text-align: center;
	padding: 20rpx 0;
	background-color: #f5f6f8;
	border-radius: 12rpx;
	border: 2rpx solid transparent;
}

.role-item.active {
	border-color: #2979ff;
	color: #2979ff;
	background-color: #e8f1ff;
}

.btn-primary {
	margin-top: 40rpx;
}

.section-title {
	font-size: 30rpx;
	font-weight: bold;
	display: block;
	margin-bottom: 20rpx;
}

.app-item {
	padding: 20rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
}

.link {
	color: #2979ff;
	font-size: 26rpx;
}
</style>
