<template>
	<view class="page">
		<!-- 加入公司表单卡片 -->
		<view class="card">
			<text class="label">邀请码</text>
			<input class="input" v-model="inviteCode" placeholder="请输入 8 位邀请码" maxlength="8" />

			<!-- 申请身份切换：员工 / 管理者 -->
			<text class="label">申请身份</text>
			<view class="row roles">
				<view class="role-item flex1" :class="{ active: applyRole === 'EMPLOYEE' }" @click="applyRole = 'EMPLOYEE'">员工</view>
				<view class="role-item flex1" :class="{ active: applyRole === 'MANAGER' }" @click="applyRole = 'MANAGER'">管理者</view>
			</view>
			<!-- 业务规则提示 -->
			<text class="muted">管理者申请需创始人审批；每天最多申请 3 次</text>

			<button class="btn-primary" :loading="loading" @click="submit">提交申请</button>
		</view>

		<!-- 我的申请记录列表 -->
		<view class="card">
			<text class="section-title">我的申请记录</text>
			<!-- 空状态 -->
			<view v-if="myApps.length === 0" class="muted">暂无申请记录</view>
			<!-- 单条申请：公司名 + 状态标签 + 申请信息 + 操作 -->
			<view v-for="a in myApps" :key="a.id" class="app-item">
				<view class="row">
					<text class="flex1">{{ a.companyName }}</text>
					<text class="tag" :class="statusTagClass(a.status)">{{ statusText(a.status) }}</text>
				</view>
				<view class="row">
					<text class="muted flex1">申请{{ a.applyRole === 'MANAGER' ? '管理者' : '员工' }} · {{ formatTime(a.createdAt) }}</text>
					<!-- 待审批状态可撤回 -->
					<text v-if="a.status === 'PENDING'" class="link" @click="cancelApp(a)">撤回</text>
				</view>
				<!-- 被拒绝时展示理由 -->
				<text v-if="a.status === 'REJECTED' && a.rejectReason" class="muted">拒绝理由：{{ a.rejectReason }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 加入公司页：凭 8 位邀请码提交加入申请（员工 / 管理者身份），
// 并展示我的历史申请（含状态、拒绝理由、撤回操作）
// ============================================================
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { companyApi, applicationApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'
import { formatTime } from '../../utils/format.js'

// 邀请码输入
const inviteCode = ref('')
// 申请身份：EMPLOYEE 员工 / MANAGER 管理者
const applyRole = ref('EMPLOYEE')
// 提交申请请求中标志
const loading = ref(false)
// 正在撤回的申请 id（防重复点击，0 表示空闲）
const cancelingId = ref(0)
// 我的申请记录列表
const myApps = ref([])

// 进入页面：校验登录态后刷新申请记录
onShow(async () => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	await loadApps()
})

/** 拉取我的加入申请列表 */
async function loadApps() {
	myApps.value = await applicationApi.mine()
}

/** 提交加入申请：非空校验 -> 提交 -> 清空输入并刷新记录 */
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
		// 错误提示已在 request 层统一处理（如黑名单、超过每日次数限制）
	} finally {
		loading.value = false
	}
}

/** 撤回待审批的申请（cancelingId 防重复提交） */
async function cancelApp(a) {
	if (cancelingId.value) return
	cancelingId.value = a.id
	try {
		await applicationApi.cancel(a.id)
		uni.showToast({ title: '已撤回', icon: 'none' })
		await loadApps()
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		cancelingId.value = 0
	}
}

/** 申请状态枚举 -> 中文文案 */
function statusText(s) {
	return s === 'PENDING' ? '待审批' : s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已拒绝' : '已撤回'
}

/** 申请状态 -> 标签颜色（待审批橙 / 通过绿 / 拒绝红 / 撤回灰） */
function statusTagClass(s) {
	return s === 'PENDING' ? 'tag-orange' : s === 'APPROVED' ? 'tag-green' : s === 'REJECTED' ? 'tag-red' : 'tag-gray'
}
</script>

<style scoped>
/* 页面根节点：底部留白 */
.page {
	padding-bottom: 40rpx;
}

/* 表单区块标题 */
.label {
	font-size: 28rpx;
	color: #606266;
	margin: 16rpx 0;
	display: block;
}

/* 身份切换行 */
.roles {
	gap: 20rpx;
	margin-bottom: 16rpx;
}

/* 身份选项：默认灰底，选中蓝边高亮 */
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

/* 提交按钮与上方内容留出间距 */
.btn-primary {
	margin-top: 40rpx;
}

/* 申请记录区块标题 */
.section-title {
	font-size: 30rpx;
	font-weight: bold;
	display: block;
	margin-bottom: 20rpx;
}

/* 单条申请记录：底部分隔线 */
.app-item {
	padding: 20rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
}

/* 可点击文字操作（撤回） */
.link {
	color: #2979ff;
	font-size: 26rpx;
}
</style>
