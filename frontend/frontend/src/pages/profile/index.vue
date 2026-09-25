<template>
	<view class="page">
		<view class="card profile-card">
			<view class="avatar">{{ avatarText }}</view>
			<view class="flex1">
				<text class="name">{{ user.nickname || user.phone }}</text>
				<text class="muted">{{ user.phone }}</text>
				<text class="tag" :class="user.systemRole === 'ADMIN' ? 'tag-red' : 'tag-blue'">
					{{ user.systemRole === 'ADMIN' ? '系统管理员' : '普通用户' }}
				</text>
			</view>
		</view>

		<view class="card">
			<text class="label">修改昵称</text>
			<view class="row">
				<input class="input flex1" v-model="nickname" placeholder="新昵称" />
				<button class="btn-primary mini" @click="saveNickname">保存</button>
			</view>
		</view>

		<view class="card menu">
			<view v-if="user.systemRole === 'ADMIN'" class="menu-item" @click="go('/pages/admin/index')">系统管理</view>
			<view class="menu-item" @click="go('/pages/index/index')">我的公司</view>
			<view class="menu-item danger" @click="logout">退出登录</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { authApi } from '../../api/index.js'
import { clearToken, getToken } from '../../api/request.js'

const user = ref({})
const nickname = ref('')

const avatarText = computed(() => {
	const n = user.value.nickname || user.value.phone || '?'
	return n.substring(0, 1)
})

onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	user.value = await authApi.me()
	nickname.value = user.value.nickname || ''
})

async function saveNickname() {
	if (!nickname.value.trim()) {
		uni.showToast({ title: '昵称不能为空', icon: 'none' })
		return
	}
	await authApi.updateMe({ nickname: nickname.value.trim() })
	uni.showToast({ title: '已保存', icon: 'none' })
	user.value = await authApi.me()
}

function go(url) {
	uni.navigateTo({ url })
}

function logout() {
	uni.showModal({
		title: '退出登录',
		content: '确定退出吗？',
		success: (res) => {
			if (res.confirm) {
				clearToken()
				uni.removeStorageSync('user_info')
				uni.reLaunch({ url: '/pages/login/login' })
			}
		}
	})
}
</script>

<style scoped>
.page {
	padding-bottom: 60rpx;
}

.profile-card {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 24rpx;
}

.avatar {
	width: 120rpx;
	height: 120rpx;
	border-radius: 60rpx;
	background-color: #2979ff;
	color: #fff;
	font-size: 48rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.name {
	font-size: 36rpx;
	font-weight: bold;
	display: block;
}

.label {
	font-size: 28rpx;
	color: #606266;
	margin-bottom: 16rpx;
	display: block;
}

.mini {
	font-size: 26rpx;
	padding: 0 40rpx;
	line-height: 64rpx;
	height: 64rpx;
	margin-left: 16rpx;
}

.menu-item {
	padding: 28rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
	font-size: 30rpx;
}

.menu-item:last-child {
	border-bottom: none;
}

.menu-item.danger {
	color: #fa3534;
}
</style>
