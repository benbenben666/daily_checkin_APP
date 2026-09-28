<template>
	<view class="page">
		<!-- 个人信息卡片：头像 + 昵称/手机号 + 系统角色标签 -->
		<view class="card profile-card">
			<!-- 头像：取昵称/手机号首字 -->
			<view class="avatar">{{ avatarText }}</view>
			<view class="flex1">
				<text class="name">{{ user.nickname || user.phone }}</text>
				<text class="muted">{{ user.phone }}</text>
				<text class="tag" :class="user.systemRole === 'ADMIN' ? 'tag-red' : 'tag-blue'">
					{{ user.systemRole === 'ADMIN' ? '系统管理员' : '普通用户' }}
				</text>
			</view>
		</view>

		<!-- 修改昵称卡片：输入新昵称后保存 -->
		<view class="card">
			<text class="label">修改昵称</text>
			<view class="row">
				<input class="input flex1" v-model="nickname" placeholder="新昵称" />
				<button class="btn-primary mini" :loading="saving" @click="saveNickname">保存</button>
			</view>
		</view>

		<!-- 功能菜单：系统管理（仅 ADMIN 可见）/ 我的公司 / 退出登录 -->
		<view class="card menu">
			<view v-if="user.systemRole === 'ADMIN'" class="menu-item" @click="go('/pages/admin/index')">系统管理</view>
			<view class="menu-item" @click="go('/pages/index/index')">我的公司</view>
			<view class="menu-item danger" @click="showLogoutModal = true">退出登录</view>
		</view>

		<!-- 退出登录确认弹窗 -->
		<view v-if="showLogoutModal" class="modal-mask" @click="showLogoutModal = false">
			<view class="modal-box" @click.stop>
				<text class="modal-title">退出登录</text>
				<text class="modal-content">确定要退出当前账号吗？退出后需要重新登录</text>
				<view class="modal-btns">
					<view class="modal-btn cancel" @click="showLogoutModal = false">取消</view>
					<view class="modal-btn confirm" @click="logout">退出</view>
				</view>
			</view>
		</view>
		<!-- 退出成功提示 -->
		<view v-if="showLogoutToast" class="toast-box">
			<text class="toast-text">已退出登录</text>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 个人中心页：用户资料展示 / 修改昵称 / 系统管理入口 / 退出登录
// ============================================================
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { authApi } from '../../api/index.js'
import { clearToken, getToken } from '../../api/request.js'

// 当前用户信息（昵称 / 手机号 / 系统角色）
const user = ref({})
// 昵称输入框内容
const nickname = ref('')
// 保存昵称请求中标志：防重复提交
const saving = ref(false)
// 退出登录确认弹窗显隐
const showLogoutModal = ref(false)
// 退出成功大号提示弹窗显隐
const showLogoutToast = ref(false)

// 头像文字：取昵称（无则手机号）首字，兜底 '?'
const avatarText = computed(() => {
	const n = user.value.nickname || user.value.phone || '?'
	return n.substring(0, 1)
})

// 每次进入页面：校验登录态并刷新用户信息（保存昵称后返回也会同步）
onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	user.value = await authApi.me()
	nickname.value = user.value.nickname || ''
})

/** 保存昵称：非空校验 -> 提交 -> 重新拉取最新用户信息 */
async function saveNickname() {
	if (saving.value) return
	if (!nickname.value.trim()) {
		uni.showToast({ title: '昵称不能为空', icon: 'none' })
		return
	}
	saving.value = true
	try {
		await authApi.updateMe({ nickname: nickname.value.trim() })
		uni.showToast({ title: '已保存', icon: 'none' })
		user.value = await authApi.me()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		saving.value = false
	}
}

/** 菜单跳转封装 */
function go(url) {
	// 「我的公司」是首页，可能已在页面栈内，用 reLaunch 避免重复页面实例
	if (url === '/pages/index/index') {
		uni.reLaunch({ url })
	} else {
		uni.navigateTo({ url })
	}
}

/**
 * 退出登录：关确认弹窗 -> 清 token 与用户缓存 ->
 * 显示大号提示（替代原生 toast）-> 1 秒后跳转登录页
 */
function logout() {
	showLogoutModal.value = false
	clearToken()
	uni.removeStorageSync('user_info')
	// 大号提示弹窗替代原生 toast
	showLogoutToast.value = true
	// 延迟跳转，保证提示可见
	setTimeout(() => {
		uni.reLaunch({ url: '/pages/login/login' })
	}, 1000)
}
</script>

<style scoped>
/* 页面根节点：底部留白 */
.page {
	padding-bottom: 60rpx;
}

/* 个人信息卡片：头像与文字信息横向排列 */
.profile-card {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 24rpx;
}

/* 头像圆块：蓝底白字，展示昵称/手机号首字 */
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

/* 用户昵称：加粗大字 */
.name {
	font-size: 36rpx;
	font-weight: bold;
	display: block;
}

/* 表单区块标题 */
.label {
	font-size: 28rpx;
	color: #606266;
	margin-bottom: 16rpx;
	display: block;
}

/* 保存按钮：小号尺寸，与输入框同行 */
.mini {
	font-size: 26rpx;
	padding: 0 40rpx;
	line-height: 64rpx;
	height: 64rpx;
	margin-left: 16rpx;
}

/* 菜单项：底部分隔线 */
.menu-item {
	padding: 28rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
	font-size: 30rpx;
}

/* 末项菜单去掉分隔线 */
.menu-item:last-child {
	border-bottom: none;
}

/* 危险菜单项：红色（退出登录） */
.menu-item.danger {
	color: #fa3534;
}

/* ---------- 退出登录确认弹窗 ---------- */

/* 全屏半透明遮罩：点击空白处关闭 */
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

/* 弹窗白色卡片：居中固定宽度 */
.modal-box {
	width: 560rpx;
	background-color: #fff;
	border-radius: 24rpx;
	padding: 48rpx 40rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
}

/* 弹窗标题 */
.modal-title {
	font-size: 34rpx;
	font-weight: bold;
	color: #303133;
}

/* 弹窗正文提示 */
.modal-content {
	font-size: 28rpx;
	color: #909399;
	margin-top: 24rpx;
	line-height: 1.6;
	text-align: center;
}

/* 弹窗按钮行：取消 / 退出 平分 */
.modal-btns {
	display: flex;
	flex-direction: row;
	width: 100%;
	margin-top: 48rpx;
	gap: 24rpx;
}

/* 弹窗按钮基础：胶囊形 */
.modal-btn {
	flex: 1;
	height: 80rpx;
	line-height: 80rpx;
	text-align: center;
	font-size: 30rpx;
	border-radius: 40rpx;
}

/* 取消按钮：灰底 */
.modal-btn.cancel {
	background-color: #f5f7fa;
	color: #606266;
}

/* 退出按钮：红底白字（危险操作色） */
.modal-btn.confirm {
	background-color: #fa3534;
	color: #fff;
}

/* ---------- 退出成功大号提示 ---------- */

/* 居中黑色半透明提示卡片 */
.toast-box {
	position: fixed;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	background-color: rgba(0, 0, 0, 0.75);
	border-radius: 24rpx;
	padding: 48rpx 80rpx;
	z-index: 1000;
}

/* 提示文案：白色加粗 */
.toast-text {
	color: #fff;
	font-size: 36rpx;
	font-weight: bold;
}
</style>
