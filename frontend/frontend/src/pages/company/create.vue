<template>
	<view class="page">
		<view class="card">
			<text class="label">公司名称</text>
			<input class="input" v-model="name" placeholder="请输入公司名称（允许重名）" />
			<button class="btn-primary" :loading="loading" @click="submit">创建</button>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { companyApi } from '../../api/index.js'

const name = ref('')
const loading = ref(false)

async function submit() {
	if (!name.value.trim()) {
		uni.showToast({ title: '请输入公司名称', icon: 'none' })
		return
	}
	loading.value = true
	try {
		await companyApi.create({ name: name.value.trim() })
		uni.showToast({ title: '创建成功', icon: 'success' })
		setTimeout(() => uni.navigateBack(), 800)
	} catch (e) {
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.page {
	padding-top: 20rpx;
}

.label {
	font-size: 28rpx;
	color: #606266;
	margin-bottom: 16rpx;
	display: block;
}

.btn-primary {
	margin-top: 40rpx;
}
</style>
