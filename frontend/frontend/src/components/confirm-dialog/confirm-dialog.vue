<template>
	<!-- 通用确认弹窗：全屏半透明遮罩，点击空白处关闭 -->
	<view v-if="visible" class="modal-mask" @click="close">
		<view class="modal-box" @click.stop>
			<text class="modal-title">{{ title }}</text>
			<text class="modal-content">{{ content }}</text>
			<!-- 可选输入框：用于填写原因/理由（可不填） -->
			<input
				v-if="showInput"
				v-model="inputValue"
				class="modal-input"
				:placeholder="placeholder"
			/>
			<view class="modal-btns">
				<view class="modal-btn cancel" @click="close">取消</view>
				<view class="modal-btn confirm" @click="onConfirm">{{ confirmText }}</view>
			</view>
		</view>
	</view>
</template>

<script setup>
// ============================================================
// 通用确认弹窗组件：与「退出登录」弹窗同款样式
// 结构：标题 + 正文提示 + 可选输入框 + 取消/确认胶囊按钮
// 用法示例（纯确认）：
//   <confirm-dialog
//     v-model:visible="show"
//     title="取消任务"
//     content="确定取消该任务吗？"
//     @confirm="doCancel"
//   />
// 需要填写原因时加：show-input placeholder="原因（可选）"，
// 确认事件会带回输入内容：@confirm="(val) => ..."
// ============================================================
import { ref, watch } from 'vue'

// 组件属性
const props = defineProps({
	visible: { type: Boolean, default: false }, // 显隐（v-model:visible）
	title: { type: String, default: '提示' }, // 弹窗标题
	content: { type: String, default: '' }, // 正文提示
	confirmText: { type: String, default: '确定' }, // 确认按钮文案
	showInput: { type: Boolean, default: false }, // 是否显示可选输入框
	placeholder: { type: String, default: '' } // 输入框占位文案
})

// 对外事件：update:visible 双向绑定显隐；confirm 点击确认（带回输入值）
const emit = defineEmits(['update:visible', 'confirm'])

// 输入框内容（仅 showInput 时用到）
const inputValue = ref('')

// 每次打开弹窗时清空上一次输入
watch(
	() => props.visible,
	(v) => {
		if (v) inputValue.value = ''
	}
)

/** 关闭弹窗：点击遮罩或「取消」按钮 */
function close() {
	emit('update:visible', false)
}

/** 点击确认：带回输入内容并关闭弹窗 */
function onConfirm() {
	emit('confirm', inputValue.value)
	emit('update:visible', false)
}
</script>

<style scoped>
/* ---------- 以下样式与「退出登录」确认弹窗保持一致 ---------- */

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

/* 可选输入框：浅灰底圆角 */
.modal-input {
	width: 100%;
	box-sizing: border-box;
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx;
	font-size: 28rpx;
	margin-top: 24rpx;
}

/* 弹窗按钮行：取消 / 确认 平分 */
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

/* 确认按钮：红底白字（危险操作色） */
.modal-btn.confirm {
	background-color: #fa3534;
	color: #fff;
}
</style>
