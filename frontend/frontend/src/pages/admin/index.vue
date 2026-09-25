<template>
	<view class="page">
		<view class="row tabs">
			<view class="tab flex1" :class="{ active: tab === 'users' }" @click="switchTab('users')">用户管理</view>
			<view class="tab flex1" :class="{ active: tab === 'companies' }" @click="switchTab('companies')">公司管理</view>
			<view class="tab flex1" :class="{ active: tab === 'profile' }" @click="goProfile">我的</view>
		</view>

		<view class="card search-row">
			<input class="input flex1" v-model="keyword" placeholder="搜索" @confirm="load" />
			<button class="btn-primary mini" @click="load">搜索</button>
		</view>

		<!-- 用户管理 -->
		<view v-if="tab === 'users'">
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

		<!-- 公司管理 -->
		<view v-if="tab === 'companies'">
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
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { adminApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

const tab = ref('users')
const keyword = ref('')
const users = ref([])
const companies = ref([])
const myUserId = ref(0)

onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	try {
		myUserId.value = JSON.parse(uni.getStorageSync('user_info') || '{}').userId || 0
	} catch (e) {}
	await load()
})

async function load() {
	if (tab.value === 'users') {
		users.value = await adminApi.users(keyword.value)
	} else {
		companies.value = await adminApi.companies(keyword.value)
	}
}

function switchTab(t) {
	tab.value = t
	keyword.value = ''
	load()
}

function goProfile() {
	uni.navigateTo({ url: '/pages/profile/index' })
}

async function toggleAdmin(u) {
	const target = u.systemRole === 'ADMIN' ? 'USER' : 'ADMIN'
	await adminApi.updateRole(u.id, target)
	uni.showToast({ title: '已更新', icon: 'none' })
	await load()
}

async function toggleStatus(u) {
	await adminApi.updateUser(u.id, { status: u.status === 1 ? 0 : 1 })
	uni.showToast({ title: '已更新', icon: 'none' })
	await load()
}

function deleteUser(u) {
	uni.showModal({
		title: '删除用户',
		content: `确定删除 ${u.nickname || u.phone} 吗？（软删除，历史数据保留）`,
		success: async (res) => {
			if (res.confirm) {
				await adminApi.deleteUser(u.id)
				uni.showToast({ title: '已删除', icon: 'none' })
				await load()
			}
		}
	})
}

function dissolveCompany(c) {
	uni.showModal({
		title: '解散公司',
		content: `确定解散「${c.name}」吗？`,
		success: async (res) => {
			if (res.confirm) {
				await adminApi.dissolveCompany(c.id)
				uni.showToast({ title: '已解散', icon: 'none' })
				await load()
			}
		}
	})
}
</script>

<style scoped>
.page {
	padding-bottom: 60rpx;
}

.tabs {
	background-color: #fff;
	margin: 20rpx 24rpx;
	border-radius: 16rpx;
	padding: 8rpx;
}

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

.search-row {
	gap: 16rpx;
}

.mini {
	font-size: 26rpx;
	padding: 0 40rpx;
	line-height: 64rpx;
	height: 64rpx;
}

.user-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.link {
	color: #2979ff;
	font-size: 26rpx;
	margin-left: 24rpx;
}

.link.danger {
	color: #fa3534;
}
</style>
