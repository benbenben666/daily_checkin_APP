<template>
	<view class="page">
		<!-- 空状态：未加入任何公司时引导创建 / 加入（v-if 排除首次加载中） -->
		<view v-if="!loading && companies.length === 0" class="empty card">
			<text class="muted">你还没有加入任何公司</text>
			<text class="muted">创建一个公司，或凭邀请码加入</text>
		</view>

		<!-- 我的公司列表：点击卡片进入公司主页 -->
		<view v-for="c in companies" :key="c.companyId" class="card company-item" @click="enterCompany(c)">
			<view class="row">
				<text class="company-name flex1">{{ c.companyName }}</text>
				<text class="tag" :class="roleTagClass(c.companyRole)">{{ roleText(c.companyRole) }}</text>
			</view>
			<text class="muted">加入时间：{{ formatTime(c.joinedAt) }}</text>
		</view>

		<!-- 底部操作区：创建公司 / 凭邀请码加入 / 个人中心 -->
		<view class="btns">
			<button class="btn-primary" @click="go('/pages/company/create')">创建公司</button>
			<button class="btn-plain" @click="go('/pages/company/join')">凭邀请码加入</button>
			<button class="btn-plain" @click="go('/pages/profile/index')">个人中心</button>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 我的公司列表页（普通用户登录后的首页）
// 展示已加入的公司，并作为创建 / 加入公司的入口
// ============================================================
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { companyApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'
import { formatTime } from '../../utils/format.js'

// 我加入的公司列表
const companies = ref([])
// 首次加载中标志：区分「加载中」与「空列表」，避免加载期间误显示空状态
const loading = ref(true)

// 每次页面显示：先校验登录态，再刷新公司列表
// （从公司页 / 其他页面返回时也会触发，保证数据最新）
onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	await load()
})

/** 拉取我加入的公司列表（finally 保证 loading 一定复位） */
async function load() {
	loading.value = true
	try {
		companies.value = await companyApi.mine()
	} finally {
		loading.value = false
	}
}

/** 公司角色枚举 -> 中文文案 */
function roleText(role) {
	return role === 'FOUNDER' ? '创始人' : role === 'MANAGER' ? '管理者' : '员工'
}

/** 公司角色 -> 标签颜色（创始人橙 / 管理者蓝 / 员工绿） */
function roleTagClass(role) {
	return role === 'FOUNDER' ? 'tag-orange' : role === 'MANAGER' ? 'tag-blue' : 'tag-green'
}

/**
 * 点击公司卡片进入公司主页。
 * 公司名 encodeURIComponent 编码后拼进 URL；
 * role 参数仅作展示用途，主页权限以接口返回的 myRole 为准
 */
function enterCompany(c) {
	uni.navigateTo({ url: `/pages/company/home?id=${c.companyId}&name=${encodeURIComponent(c.companyName)}&role=${c.companyRole}` })
}

/** 普通页面跳转封装 */
function go(url) {
	uni.navigateTo({ url })
}
</script>

<style scoped>
/* 页面根节点：底部留白避免内容贴边 */
.page {
	padding-bottom: 40rpx;
}

/* 空状态卡片：两行提示垂直居中 */
.empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 80rpx 24rpx;
	gap: 12rpx;
}

/* 公司卡片：公司名 + 角色标签 + 加入时间 */
.company-item {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

/* 公司名称：加粗突出 */
.company-name {
	font-size: 34rpx;
	font-weight: bold;
}

/* 底部操作按钮组：纵向排列 */
.btns {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
	padding: 40rpx 24rpx;
}

/* 按钮占满整行 */
.btns button {
	width: 100%;
}
</style>
