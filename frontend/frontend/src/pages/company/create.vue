<template>
	<view class="page">
		<!-- 创建公司表单卡片 -->
		<view class="card">
			<text class="label">公司名称</text>
			<input class="input" v-model="name" placeholder="请输入需要创建的公司名称" />
			<!-- 创建按钮：loading 防重复提交 -->
			<button class="btn-primary" :loading="loading" @click="submit">创建</button>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 创建公司页：输入公司名创建，创建者自动成为创始人
// ============================================================
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { companyApi } from '../../api/index.js'
import { getToken } from '../../api/request.js'

// 公司名称输入
const name = ref('')
// 创建请求中标志
const loading = ref(false)

// 进入页面校验登录态（未登录统一踢回登录页）
onShow(() => {
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
	}
})

/** 提交创建：非空校验 -> 调接口 -> 提示成功后返回上一页 */
async function submit() {
	if (!name.value.trim()) {
		uni.showToast({ title: '请输入公司名称', icon: 'none' })
		return
	}
	loading.value = true
	try {
		await companyApi.create({ name: name.value.trim() })
		uni.showToast({ title: '创建成功', icon: 'success' })
		// 延迟返回，保证成功提示可见
		setTimeout(() => uni.navigateBack(), 800)
	} catch (e) {
		// 错误提示已在 request 层统一处理
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
/* 页面根节点：顶部留白 */
.page {
	padding-top: 20rpx;
}

/* 表单区块标题 */
.label {
	font-size: 28rpx;
	color: #606266;
	margin-bottom: 16rpx;
	display: block;
}

/* 创建按钮与输入框留出间距 */
.btn-primary {
	margin-top: 40rpx;
}
</style>
