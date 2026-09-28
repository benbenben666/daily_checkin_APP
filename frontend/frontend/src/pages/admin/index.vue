<template>
	<view class="page">
		<!-- 顶部 Tab：用户管理 / 公司管理 / 我的（个人中心） -->
		<view class="row tabs">
			<view class="tab flex1" :class="{ active: tab === 'users' }" @click="switchTab('users')">用户管理</view>
			<view class="tab flex1" :class="{ active: tab === 'companies' }" @click="switchTab('companies')">公司管理</view>
			<view class="tab flex1" @click="goProfile">我的</view>
		</view>

		<!-- 搜索行：回车或点击按钮触发搜索 -->
		<view class="card search-row">
			<input class="input flex1" v-model="keyword" placeholder="搜索" @confirm="load" />
			<button class="btn-primary mini" @click="load">搜索</button>
		</view>

		<!-- 用户管理 Tab：昵称/手机号搜索，角色/状态/删除操作 -->
		<view v-if="tab === 'users'">
			<!-- 单个用户卡片：昵称 + 角色/状态标签 + 操作链接（自己不可操作） -->
			<view v-for="u in users" :key="u.id" class="card user-item">
				<view class="row">
					<text class="flex1">{{ u.nickname || u.phone }}</text>
					<text class="tag" :class="u.systemRole === 'ADMIN' ? 'tag-red' : 'tag-gray'">
						{{ u.systemRole === 'ADMIN' ? '管理员' : '普通用户' }}
					</text>
					<text class="tag" :class="u.status === 1 ? 'tag-green' : 'tag-gray'">
						{{ u.status === 1 ? '正常' : '已禁用' }}
					</text>
				</view>
				<view class="row">
					<text class="muted flex1">{{ u.phone }}</text>
					<!-- 以下操作均不对自己显示 -->
					<text v-if="u.id !== myUserId" class="link" @click="toggleAdmin(u)">
						{{ u.systemRole === 'ADMIN' ? '取消管理员' : '设为管理员' }}
					</text>
					<text v-if="u.id !== myUserId" class="link" @click="toggleStatus(u)">
						{{ u.status === 1 ? '禁用' : '启用' }}
					</text>
					<text v-if="u.id !== myUserId" class="link danger" @click="deleteUser(u)">删除</text>
				</view>
			</view>
		</view>

		<!-- 公司管理 Tab：强制解散公司 -->
		<view v-if="tab === 'companies'">
			<!-- 单个公司卡片：名称 + 状态 + 邀请码/成员数 + 解散操作 -->
			<view v-for="c in companies" :key="c.id" class="card user-item">
				<view class="row">
					<text class="flex1">{{ c.name }}</text>
					<text class="tag" :class="c.status === 1 ? 'tag-green' : 'tag-gray'">
						{{ c.status === 1 ? '正常' : '已解散' }}
					</text>
				</view>
				<view class="row">
					<text class="muted flex1">邀请码 {{ c.inviteCode }} · {{ c.memberCount }} 名成员</text>
					<text v-if="c.status === 1" class="link danger" @click="dissolveCompany(c)">解散</text>
				</view>
			</view>
		</view>
		<!-- 统一确认弹窗：删除用户/解散公司 共用 -->
		<confirm-dialog
			v-model:visible="confirmDialog.visible"
			:title="confirmDialog.title"
			:content="confirmDialog.content"
			@confirm="onDialogConfirm"
		/>
	</view>
</template>

<script setup>
// ============================================================
// 系统管理页（仅 ADMIN 可进入）
// 两个 Tab：用户管理（角色/禁用/删除）、公司管理（强制解散）
// 搜索关键词对用户为昵称/手机号，对公司为公司名
// ============================================================
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { adminApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'
// 统一确认弹窗组件（样式与退出登录弹窗一致）
import ConfirmDialog from '../../components/confirm-dialog/confirm-dialog.vue'

// 当前 Tab：users 用户管理 / companies 公司管理
const tab = ref('users')
// 搜索关键词
const keyword = ref('')
// 用户列表
const users = ref([])
// 公司列表
const companies = ref([])
// 当前管理员自己的用户 id（自己不可被操作）
const myUserId = ref(0)
// 操作请求中标志（本页所有写操作共用一个防重复标志）
const submitting = ref(false)

// 每次页面显示：校验登录态 -> 取我的用户 id -> 加载当前 Tab 数据
onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	// 从本地缓存解析当前用户 id
	try {
		myUserId.value = JSON.parse(uni.getStorageSync('user_info') || '{}').userId || 0
	} catch (e) {}
	await load()
})

/** 按当前 Tab 与关键词加载数据 */
async function load() {
	if (tab.value === 'users') {
		users.value = await adminApi.users(keyword.value)
	} else {
		companies.value = await adminApi.companies(keyword.value)
	}
}

/** 切换 Tab：清空关键词后加载新 Tab 数据 */
async function switchTab(t) {
	tab.value = t
	keyword.value = ''
	try {
		await load()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	}
}

function goProfile() {
	// 「我的」不在当前页面栈内，用 reLaunch 避免重复页面实例
	uni.reLaunch({ url: '/pages/profile/index' })
}

/** 切换用户系统角色：管理员 <-> 普通用户 */
async function toggleAdmin(u) {
	if (submitting.value) return
	submitting.value = true
	try {
		const target = u.systemRole === 'ADMIN' ? 'USER' : 'ADMIN'
		await adminApi.updateRole(u.id, target)
		uni.showToast({ title: '已更新', icon: 'none' })
		await load()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		submitting.value = false
	}
}

/** 切换用户状态：启用 <-> 禁用（禁用后该用户无法登录） */
async function toggleStatus(u) {
	if (submitting.value) return
	submitting.value = true
	try {
		await adminApi.updateUser(u.id, { status: u.status === 1 ? 0 : 1 })
		uni.showToast({ title: '已更新', icon: 'none' })
		await load()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		submitting.value = false
	}
}

// ---------- 统一确认弹窗（替代原生 uni.showModal，样式与退出登录弹窗一致） ----------

// 弹窗状态：显隐 + 标题/内容 + 确认后要执行的动作
const confirmDialog = reactive({
	visible: false,
	title: '',
	content: '',
	action: null
})

/** 打开统一确认弹窗：opts 传 title / content / action */
function showConfirm(opts) {
	Object.assign(confirmDialog, { action: null }, opts, { visible: true })
}

/** 弹窗确认回调：执行对应动作 */
async function onDialogConfirm() {
	const action = confirmDialog.action
	if (action) await action()
}

/** 删除用户：软删除（历史数据保留），二次确认后执行 */
function deleteUser(u) {
	showConfirm({
		title: '删除用户',
		content: `确定删除 ${u.nickname || u.phone} 吗？（软删除，历史数据保留）`,
		action: async () => {
			if (submitting.value) return
			submitting.value = true
			try {
				await adminApi.deleteUser(u.id)
				uni.showToast({ title: '已删除', icon: 'none' })
				await load()
			} catch (e) {
				// 错误提示已在 request 层统一处理
			} finally {
				submitting.value = false
			}
		}
	})
}

/** 强制解散公司：二次确认后执行 */
function dissolveCompany(c) {
	showConfirm({
		title: '解散公司',
		content: `确定解散「${c.name}」吗？`,
		action: async () => {
			if (submitting.value) return
			submitting.value = true
			try {
				await adminApi.dissolveCompany(c.id)
				uni.showToast({ title: '已解散', icon: 'none' })
				await load()
			} catch (e) {
				// 错误提示已在 request 层统一处理
			} finally {
				submitting.value = false
			}
		}
	})
}
</script>

<style scoped>
/* 页面根节点：底部留白 */
.page {
	padding-bottom: 60rpx;
}

/* 顶部 Tab 栏：白色圆角容器 */
.tabs {
	background-color: #fff;
	margin: 20rpx 24rpx;
	border-radius: 16rpx;
	padding: 8rpx;
}

/* Tab 项：默认灰，选中蓝加粗 */
.tab {
	text-align: center;
	padding: 20rpx 0;
	color: #909399;
	font-size: 30rpx;
}

.tab.active {
	color: #2979ff;
	font-weight: bold;
}

/* 搜索行：输入框 + 搜索按钮 */
.search-row {
	gap: 16rpx;
}

/* 小号搜索按钮 */
.mini {
	font-size: 26rpx;
	padding: 0 40rpx;
	line-height: 64rpx;
	height: 64rpx;
}

/* 用户/公司卡片：纵向排列 */
.user-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

/* 可点击文字操作 */
.link {
	color: #2979ff;
	font-size: 26rpx;
	margin-left: 24rpx;
}

/* 危险文字操作（删除/解散） */
.link.danger {
	color: #fa3534;
}
</style>
