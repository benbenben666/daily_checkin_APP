<template>
	<view class="page">
		<view class="logo-area">
			<text class="logo-title">任务平台</text>
			<text class="logo-sub">公司内部任务发布与执行平台</text>
		</view>

		<view class="card">
			<view class="row tabs">
				<view class="tab flex1" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</view>
				<view class="tab flex1" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</view>
			</view>

			<input class="input" type="number" maxlength="11" v-model="form.phone" placeholder="手机号" />
			<input class="input" password v-model="form.password" placeholder="密码（至少 6 位）" />
			<input v-if="mode === 'register'" class="input" v-model="form.nickname" placeholder="昵称（可选）" />

			<button class="btn-primary" :loading="loading" @click="submit">
				{{ mode === 'login' ? '登录' : '注册' }}
			</button>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { authApi } from '../../api/index.js'
import { setToken } from '../../api/request.js'

const mode = ref('login')
const loading = ref(false)
const form = reactive({ phone: '', password: '', nickname: '' })

function afterLogin(data) {
	setToken(data.token)
	uni.setStorageSync('user_info', JSON.stringify(data))
	if (data.systemRole === 'ADMIN') {
		uni.reLaunch({ url: '/pages/admin/index' })
	} else {
		uni.reLaunch({ url: '/pages/index/index' })
	}
}

async function submit() {
	if (!/^1\d{10}$/.test(form.phone)) {
		uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
		return
	}
	if (form.password.length < 6) {
		uni.showToast({ title: '密码至少 6 位', icon: 'none' })
		return
	}
	loading.value = true
	try {
		if (mode.value === 'register') {
			await authApi.register({ phone: form.phone, password: form.password, nickname: form.nickname })
			uni.showToast({ title: '注册成功，请登录', icon: 'none' })
			mode.value = 'login'
		} else {
			const data = await authApi.login({ phone: form.phone, password: form.password })
			afterLogin(data)
		}
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	padding: 0 24rpx;
}

.logo-area {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 120rpx 0 60rpx;
}

.logo-title {
	font-size: 56rpx;
	font-weight: bold;
	color: #2979ff;
}

.logo-sub {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: #909399;
}

.tabs {
	margin-bottom: 30rpx;
}

.tab {
	text-align: center;
	padding: 16rpx 0;
	font-size: 32rpx;
	color: #909399;
	border-bottom: 4rpx solid transparent;
}

.tab.active {
	color: #2979ff;
	font-weight: bold;
	border-bottom-color: #2979ff;
}

.input {
	margin-bottom: 24rpx;
}

.btn-primary {
	margin-top: 12rpx;
}
</style>
