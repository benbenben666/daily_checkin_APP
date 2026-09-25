<template>
	<view class="page">
		<view v-if="task" class="card">
			<view class="row">
				<text class="title flex1">{{ task.title }}</text>
				<text class="tag" :class="statusTag">{{ statusText }}</text>
			</view>
			<view class="row tags-row">
				<text class="tag" :class="task.taskType === 'GLOBAL' ? 'tag-blue' : 'tag-orange'">
					{{ task.taskType === 'GLOBAL' ? '全局抢单' : '指定任务' }}
				</text>
				<text v-if="task.visibility === 'RESTRICTED'" class="tag tag-gray">部分可见</text>
				<text v-if="task.isLate === 1" class="tag tag-red">超时完成</text>
				<text v-if="task.allowLateSubmit === 1" class="tag tag-green">可补交</text>
			</view>

			<view class="info-block">
				<view class="info-row"><text class="muted">发布人</text><text>{{ task.publisherNickname || task.publisherId }}</text></view>
				<view class="info-row"><text class="muted">发布时间</text><text>{{ formatTime(task.createdAt) }}</text></view>
				<view class="info-row"><text class="muted">完成截止</text><text>{{ formatTime(task.deadlineAt) }}</text></view>
				<view v-if="task.allowLateSubmit === 1" class="info-row"><text class="muted">补交截止</text><text>{{ formatTime(task.lateDeadlineAt) }}</text></view>
				<view v-if="task.status === 'COMPLETED'" class="info-row"><text class="muted">完成人</text><text>{{ task.completedByNickname || task.completedBy }}</text></view>
				<view v-if="task.status === 'COMPLETED'" class="info-row"><text class="muted">完成时间</text><text>{{ formatTime(task.completedAt) }}</text></view>
				<view v-if="task.status === 'CANCELLED'" class="info-row"><text class="muted">取消原因</text><text>{{ task.cancelReason || '无' }}</text></view>
			</view>

			<view v-if="task.description" class="desc-block">
				<text class="section-title">任务详情</text>
				<text class="desc">{{ task.description }}</text>
			</view>

			<view v-if="task.assignees && task.assignees.length > 0" class="desc-block">
				<text class="section-title">被指派人</text>
				<view class="row member-tags">
					<text v-for="a in task.assignees" :key="a.userId" class="tag tag-blue">{{ a.nickname || a.phone }}</text>
				</view>
			</view>

			<view v-if="task.submitContent" class="desc-block">
				<text class="section-title">提交内容</text>
				<text class="desc">{{ task.submitContent }}</text>
			</view>
		</view>

		<!-- 操作区 -->
		<view v-if="task" class="card actions">
			<!-- 可完成：进行中，或已超时但允许补交 -->
			<button v-if="canComplete" class="btn-primary" @click="showComplete = true">
				{{ task.status === 'EXPIRED' ? '超时补交' : '完成任务' }}
			</button>
			<button v-if="canCancel" class="btn-danger" @click="cancelTask">取消任务</button>
		</view>

		<!-- 完成提交弹窗 -->
		<view v-if="showComplete" class="modal-mask" @click="showComplete = false">
			<view class="modal" @click.stop>
				<text class="section-title">{{ task.status === 'EXPIRED' ? '超时补交' : '完成任务' }}</text>
				<textarea class="textarea" v-model="submitContent" placeholder="提交说明（可不填，直接提交）" />
				<view class="row modal-btns">
					<button class="btn-plain flex1" @click="showComplete = false">取消</button>
					<button class="btn-primary flex1" :loading="submitting" @click="doComplete">确认提交</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { taskApi } from '../../api/index.js'

const taskId = ref(0)
const task = ref(null)
const showComplete = ref(false)
const submitContent = ref('')
const submitting = ref(false)
const myUserId = ref(0)

const canComplete = computed(() => {
	if (!task.value) return false
	if (task.value.status === 'PENDING') return true
	// 已超时：允许补交且未超补交窗口时仍可提交（后端会二次校验）
	return task.value.status === 'EXPIRED' && task.value.allowLateSubmit === 1
})

const canCancel = computed(() => {
	if (!task.value) return false
	return task.value.status === 'PENDING' || task.value.status === 'EXPIRED'
})

const statusText = computed(() => {
	if (!task.value) return ''
	const s = task.value.status
	return s === 'PENDING' ? '进行中' : s === 'EXPIRED' ? '已超时' : s === 'COMPLETED' ? '已完成' : '已取消'
})

const statusTag = computed(() => {
	if (!task.value) return 'tag-gray'
	const s = task.value.status
	return s === 'PENDING' ? 'tag-blue' : s === 'EXPIRED' ? 'tag-orange' : s === 'COMPLETED' ? 'tag-green' : 'tag-gray'
})

onLoad((options) => {
	taskId.value = Number(options.id)
	try {
		myUserId.value = JSON.parse(uni.getStorageSync('user_info') || '{}').userId || 0
	} catch (e) {}
})

onShow(load)

async function load() {
	task.value = await taskApi.detail(taskId.value)
}

async function doComplete() {
	submitting.value = true
	try {
		await taskApi.complete(taskId.value, { submitContent: submitContent.value })
		uni.showToast({ title: '提交成功', icon: 'success' })
		showComplete.value = false
		await load()
	} catch (e) {
	} finally {
		submitting.value = false
	}
}

function cancelTask() {
	uni.showModal({
		title: '取消任务',
		content: '确定取消该任务吗？',
		editable: true,
		placeholderText: '取消原因（可选）',
		success: async (res) => {
			if (res.confirm) {
				await taskApi.cancel(taskId.value, res.content || '')
				uni.showToast({ title: '已取消', icon: 'none' })
				await load()
			}
		}
	})
}

function formatTime(t) {
	return t ? t.replace('T', ' ').substring(0, 16) : ''
}
</script>

<style scoped>
.page {
	padding-bottom: 60rpx;
}

.title {
	font-size: 38rpx;
	font-weight: bold;
}

.tags-row {
	gap: 12rpx;
	margin-top: 16rpx;
	flex-wrap: wrap;
}

.info-block {
	margin-top: 24rpx;
	border-top: 1rpx solid #f0f0f0;
	padding-top: 16rpx;
}

.info-row {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	padding: 10rpx 0;
	font-size: 28rpx;
}

.desc-block {
	margin-top: 24rpx;
}

.section-title {
	font-size: 30rpx;
	font-weight: bold;
	display: block;
	margin-bottom: 12rpx;
}

.desc {
	font-size: 28rpx;
	color: #606266;
	line-height: 1.6;
}

.member-tags {
	gap: 12rpx;
	flex-wrap: wrap;
}

.actions {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

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

.modal {
	width: 80%;
	background-color: #fff;
	border-radius: 16rpx;
	padding: 32rpx;
}

.textarea {
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx;
	font-size: 28rpx;
	width: 100%;
	box-sizing: border-box;
	min-height: 160rpx;
	margin: 20rpx 0;
}

.modal-btns {
	gap: 20rpx;
}
</style>
