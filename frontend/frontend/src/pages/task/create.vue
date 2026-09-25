<template>
	<view class="page">
		<view class="card">
			<text class="label">任务标题 *</text>
			<input class="input" v-model="form.title" placeholder="请输入任务标题" maxlength="200" />

			<text class="label">任务详情</text>
			<textarea class="textarea" v-model="form.description" placeholder="任务详情说明（可选）" />

			<text class="label">任务类型 *</text>
			<view class="row seg">
				<view class="seg-item flex1" :class="{ active: form.taskType === 'GLOBAL' }" @click="form.taskType = 'GLOBAL'">全局抢单</view>
				<view class="seg-item flex1" :class="{ active: form.taskType === 'ASSIGNED' }" @click="form.taskType = 'ASSIGNED'">指定成员</view>
			</view>

			<text class="label">限时时长（5 分钟 ~ 30 天）*</text>
			<view class="row limit-row">
				<input class="input flex1" type="number" v-model="limitValue" placeholder="数值" />
				<view class="row unit-seg">
					<view class="unit" :class="{ active: limitUnit === 'min' }" @click="limitUnit = 'min'">分钟</view>
					<view class="unit" :class="{ active: limitUnit === 'hour' }" @click="limitUnit = 'hour'">小时</view>
					<view class="unit" :class="{ active: limitUnit === 'day' }" @click="limitUnit = 'day'">天</view>
				</view>
			</view>

			<view class="row switch-row">
				<text class="flex1">允许超时补交</text>
				<switch :checked="allowLate" @change="allowLate = $event.detail.value" color="#2979ff" />
			</view>

			<text class="label">可见范围 *</text>
			<view class="row seg">
				<view class="seg-item flex1" :class="{ active: form.visibility === 'PUBLIC' }" @click="form.visibility = 'PUBLIC'">全员可见</view>
				<view class="seg-item flex1" :class="{ active: form.visibility === 'RESTRICTED' }" @click="form.visibility = 'RESTRICTED'">指定人可见</view>
			</view>

			<!-- 指派人选择 -->
			<view v-if="form.taskType === 'ASSIGNED'">
				<text class="label">指派给 *（可多选）</text>
				<view class="member-pick">
					<view v-for="m in members" :key="m.userId" class="pick-item"
						:class="{ checked: form.assigneeIds.includes(m.userId) }" @click="toggleAssignee(m.userId)">
						{{ m.nickname || m.phone }}
					</view>
				</view>
			</view>

			<!-- 可见人选择 -->
			<view v-if="form.visibility === 'RESTRICTED'">
				<text class="label">可见人 *（可多选）</text>
				<view class="member-pick">
					<view v-for="m in members" :key="m.userId" class="pick-item"
						:class="{ checked: form.viewerIds.includes(m.userId) }" @click="toggleViewer(m.userId)">
						{{ m.nickname || m.phone }}
					</view>
				</view>
			</view>

			<button class="btn-primary" :loading="loading" @click="submit">发布任务</button>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { companyApi, taskApi } from '../../api/index.js'

const companyId = ref(0)
const members = ref([])
const loading = ref(false)
const allowLate = ref(true)
const limitValue = ref('30')
const limitUnit = ref('min')

const form = reactive({
	title: '',
	description: '',
	taskType: 'GLOBAL',
	visibility: 'PUBLIC',
	assigneeIds: [],
	viewerIds: []
})

onLoad(async (options) => {
	companyId.value = Number(options.companyId)
	members.value = await companyApi.members(companyId.value)
})

function toggleAssignee(id) {
	const i = form.assigneeIds.indexOf(id)
	if (i >= 0) form.assigneeIds.splice(i, 1)
	else form.assigneeIds.push(id)
}

function toggleViewer(id) {
	const i = form.viewerIds.indexOf(id)
	if (i >= 0) form.viewerIds.splice(i, 1)
	else form.viewerIds.push(id)
}

function toMinutes() {
	const v = Number(limitValue.value)
	if (!v || v <= 0) return 0
	if (limitUnit.value === 'min') return v
	if (limitUnit.value === 'hour') return v * 60
	return v * 60 * 24
}

async function submit() {
	if (!form.title.trim()) {
		uni.showToast({ title: '请输入任务标题', icon: 'none' })
		return
	}
	const minutes = toMinutes()
	if (minutes < 5 || minutes > 43200) {
		uni.showToast({ title: '限时需在 5 分钟 ~ 30 天之间', icon: 'none' })
		return
	}
	if (form.taskType === 'ASSIGNED' && form.assigneeIds.length === 0) {
		uni.showToast({ title: '请选择被指派人', icon: 'none' })
		return
	}
	if (form.visibility === 'RESTRICTED' && form.viewerIds.length === 0) {
		uni.showToast({ title: '请选择可见人', icon: 'none' })
		return
	}
	loading.value = true
	try {
		await taskApi.create(companyId.value, {
			title: form.title.trim(),
			description: form.description,
			taskType: form.taskType,
			visibility: form.visibility,
			timeLimitMinutes: minutes,
			allowLateSubmit: allowLate.value ? 1 : 0,
			assigneeIds: form.taskType === 'ASSIGNED' ? form.assigneeIds : [],
			viewerIds: form.visibility === 'RESTRICTED' ? form.viewerIds : []
		})
		uni.showToast({ title: '发布成功', icon: 'success' })
		setTimeout(() => uni.navigateBack(), 800)
	} catch (e) {
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.page {
	padding-bottom: 60rpx;
}

.label {
	font-size: 28rpx;
	color: #606266;
	margin: 24rpx 0 12rpx;
	display: block;
}

.textarea {
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx 24rpx;
	font-size: 28rpx;
	width: 100%;
	box-sizing: border-box;
	min-height: 160rpx;
}

.seg {
	gap: 20rpx;
}

.seg-item {
	text-align: center;
	padding: 20rpx 0;
	background-color: #f5f6f8;
	border-radius: 12rpx;
	border: 2rpx solid transparent;
}

.seg-item.active {
	border-color: #2979ff;
	color: #2979ff;
	background-color: #e8f1ff;
}

.limit-row {
	gap: 16rpx;
}

.unit-seg {
	gap: 8rpx;
}

.unit {
	padding: 16rpx 24rpx;
	background-color: #f5f6f8;
	border-radius: 12rpx;
	font-size: 26rpx;
}

.unit.active {
	background-color: #2979ff;
	color: #fff;
}

.switch-row {
	padding: 20rpx 0;
	border-bottom: 1rpx solid #f0f0f0;
}

.member-pick {
	display: flex;
	flex-direction: row;
	flex-wrap: wrap;
	gap: 16rpx;
}

.pick-item {
	padding: 12rpx 28rpx;
	background-color: #f5f6f8;
	border-radius: 32rpx;
	font-size: 26rpx;
	border: 2rpx solid transparent;
}

.pick-item.checked {
	background-color: #e8f1ff;
	border-color: #2979ff;
	color: #2979ff;
}

.btn-primary {
	margin-top: 40rpx;
}
</style>
