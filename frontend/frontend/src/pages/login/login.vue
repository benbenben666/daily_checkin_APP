<template>
	<view class="page">
		<!-- 品牌区：Logo 与产品定位语 -->
		<view class="logo-area">
			<text class="logo-title">任易通</text>
			<text class="logo-sub">适用于任务数据持久化的管理平台</text>
		</view>

		<!-- 登录 / 注册表单卡片 -->
		<view class="card">
			<!-- 模式切换 Tab：登录 <-> 注册 -->
			<view class="row tabs">
				<view class="tab flex1" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</view>
				<view class="tab flex1" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</view>
			</view>

			<!-- 手机号：数字键盘，限 11 位 -->
			<input class="input" type="number" maxlength="11" v-model="form.phone" placeholder="手机号" />
			<!-- 密码：password 属性自动掩码显示 -->
			<input class="input" password v-model="form.password" placeholder="密码（至少 6 位）" />
			<!-- 昵称：仅注册模式显示，可不填 -->
			<input v-if="mode === 'register'" class="input" v-model="form.nickname" placeholder="昵称（可选）" />

			<!-- 提交按钮：loading 期间防重复点击，文案随模式切换 -->
			<button class="btn-primary" :loading="loading" @click="submit">
				{{ mode === 'login' ? '登录' : '注册' }}
			</button>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 登录 / 注册页（应用启动页）
// 登录成功后按系统角色分流：ADMIN -> 系统管理，USER -> 我的公司
// ============================================================
import { reactive, ref } from 'vue'
import { authApi } from '../../api/index.js'
import { setToken, resetAuthRedirect } from '../../api/request.js'

// 当前表单模式：'login' 登录 / 'register' 注册
const mode = ref('login')
// 提交中标志：控制按钮 loading 态，防止重复提交
const loading = ref(false)
// 表单数据：手机号 + 密码 + 昵称（昵称仅注册时使用）
const form = reactive({ phone: '', password: '', nickname: '' })

/**
 * 登录成功后的统一处理：
 * 1. 复位 401 跳转标志 2. 保存 token 3. 缓存用户信息
 * 4. 提示“登录成功”后按角色跳转（延迟 600ms 让提示可见）
 * @param {Object} data 后端返回的 { token, systemRole, ... }
 */
function afterLogin(data) {
	resetAuthRedirect()
	setToken(data.token)
	uni.setStorageSync('user_info', JSON.stringify(data))
	uni.showToast({ title: '登录成功', icon: 'success' })
	// 延迟跳转，避免页面切换覆盖 toast 提示
	setTimeout(() => {
		if (data.systemRole === 'ADMIN') {
			uni.reLaunch({ url: '/pages/admin/index' })
		} else {
			uni.reLaunch({ url: '/pages/index/index' })
		}
	}, 600)
}

/**
 * 提交表单：先本地校验，再按模式发起注册或登录。
 * 注册成功后自动切回登录模式；登录成功走 afterLogin。
 */
async function submit() {
	// 本地校验：手机号必须是 1 开头的 11 位数字
	if (!/^1\d{10}$/.test(form.phone)) {
		uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
		return
	}
	// 本地校验：密码至少 6 位
	if (form.password.length < 6) {
		uni.showToast({ title: '密码至少 6 位', icon: 'none' })
		return
	}
	loading.value = true
	try {
		if (mode.value === 'register') {
			// 注册成功不自动登录，切回登录 Tab 让用户自行登录
			await authApi.register({ phone: form.phone, password: form.password, nickname: form.nickname })
			uni.showToast({ title: '注册成功，请登录', icon: 'none' })
			mode.value = 'login'
		} else {
			const data = await authApi.login({ phone: form.phone, password: form.password })
			afterLogin(data)
		}
	} catch (e) {
		// 错误提示已在 request 层统一处理（含 401 自动跳登录页）
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
/* 页面根节点：撑满全屏（表单居中的基础） */
.page {
	min-height: 100vh;
	padding: 0 24rpx;
}

/* 品牌区：Logo + 副标题垂直居中排列 */
.logo-area {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 120rpx 0 60rpx;
}

/* Logo 主标题：品牌大字 */
.logo-title {
	font-size: 56rpx;
	font-weight: bold;
	color: #2979ff;
}

/* Logo 副标题：产品定位语 */
.logo-sub {
	margin-top: 16rpx;
	font-size: 26rpx;
	color: #909399;
}

/* 登录/注册 Tab 行 */
.tabs {
	margin-bottom: 30rpx;
}

/* Tab 项：默认灰色，选中变蓝加粗 */
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

/* 表单输入框间距（基础样式在 App.vue 全局定义） */
.input {
	margin-bottom: 24rpx;
}

/* 提交按钮与输入区留出间距 */
.btn-primary {
	margin-top: 12rpx;
}
</style>
