<template>
	<view class="page">
		<view v-if="!loading && companies.length === 0" class="empty card">
			<text class="muted">你还没有加入任何公司</text>
			<text class="muted">创建一个公司，或凭邀请码加入</text>
		</view>

		<view v-for="c in companies" :key="c.companyId" class="card company-item" @click="enterCompany(c)">
			<view class="row">
				<text class="company-name flex1">{{ c.companyName }}</text>
				<text class="tag" :class="roleTagClass(c.companyRole)">{{ roleText(c.companyRole) }}</text>
			</view>
			<text class="muted">加入时间：{{ formatTime(c.joinedAt) }}</text>
		</view>

		<view class="btns">
			<button class="btn-primary" @click="go('/pages/company/create')">创建公司</button>
			<button class="btn-plain" @click="go('/pages/company/join')">凭邀请码加入</button>
			<button class="btn-plain" @click="go('/pages/profile/index')">个人中心</button>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { companyApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

const companies = ref([])
const loading = ref(true)

onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	await load()
})

async function load() {
	loading.value = true
	try {
		companies.value = await companyApi.mine()
	} finally {
		loading.value = false
	}
}

function roleText(role) {
	return role === 'FOUNDER' ? '创始人' : role === 'MANAGER' ? '管理者' : '员工'
}

function roleTagClass(role) {
	return role === 'FOUNDER' ? 'tag-orange' : role === 'MANAGER' ? 'tag-blue' : 'tag-green'
}

function formatTime(t) {
	return t ? t.replace('T', ' ').substring(0, 16) : ''
}

function enterCompany(c) {
	uni.navigateTo({ url: `/pages/company/home?id=${c.companyId}&name=${encodeURIComponent(c.companyName)}&role=${c.companyRole}` })
}

function go(url) {
	uni.navigateTo({ url })
}
</script>

<style scoped>
.page {
	padding-bottom: 40rpx;
}

.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 80rpx 24rpx;
	gap: 12rpx;
}

.company-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.company-name {
	font-size: 34rpx;
	font-weight: bold;
}

.btns {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
	padding: 40rpx 24rpx;
}
</style>
