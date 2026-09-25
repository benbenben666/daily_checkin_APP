<script>
export default {
	onLaunch() {
		console.log('App Launch')
	},
	onShow() {
		console.log('App Show')
	},
	onHide() {
		console.log('App Hide')
	}
}
</script>

<style>
/* 全局公共样式 */
page {
	background-color: #f5f6f8;
	font-size: 28rpx;
	color: #333;
}

.card {
	background-color: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin: 20rpx 24rpx;
}

.btn-primary {
	background-color: #2979ff;
	color: #fff;
	border-radius: 12rpx;
	font-size: 30rpx;
}

.btn-plain {
	background-color: #fff;
	color: #2979ff;
	border: 1rpx solid #2979ff;
	border-radius: 12rpx;
	font-size: 30rpx;
}

.btn-danger {
	background-color: #fff;
	color: #fa3534;
	border: 1rpx solid #fa3534;
	border-radius: 12rpx;
	font-size: 30rpx;
}

.input {
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx 24rpx;
	font-size: 28rpx;
}

.tag {
	display: inline-block;
	padding: 4rpx 16rpx;
	border-radius: 8rpx;
	font-size: 22rpx;
}

.tag-blue {
	background-color: #e8f1ff;
	color: #2979ff;
}

.tag-green {
	background-color: #e8f8ee;
	color: #19be6b;
}

.tag-orange {
	background-color: #fff3e6;
	color: #ff9900;
}

.tag-red {
	background-color: #ffecec;
	color: #fa3534;
}

.tag-gray {
	background-color: #f0f0f0;
	color: #909399;
}

.row {
	display: flex;
	flex-direction: row;
	align-items: center;
}

.flex1 {
	flex: 1;
}

.muted {
	color: #909399;
	font-size: 24rpx;
}
</style>
