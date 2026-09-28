<script>
// ============================================================
// 应用根组件：应用级生命周期 + 全局公共样式
// 注意：这里只能监听应用级生命周期（启动 / 前后台切换），
// 页面级逻辑请写在各页面自己的 onLoad / onShow 中
// ============================================================
export default {
	// 应用首次启动时触发（全局仅执行一次），适合做一次性初始化
	onLaunch() {
		console.log('App Launch')
	},
	// 应用从后台切回前台时触发
	onShow() {
		console.log('App Show')
	},
	// 应用从前台切到后台时触发
	onHide() {
		console.log('App Hide')
	}
}
</script>

<style>
/* ============================================================
   全局公共样式（无 scoped，所有页面共用）
   页面私有样式请写在各自 <style scoped> 中
   ============================================================ */

/* 页面根节点：统一灰底、默认字号与文字色 */
page {
	background-color: #f5f6f8;
	font-size: 28rpx;
	color: #333;
}

/* 白色圆角卡片容器：各页面的基础布局单元 */
.card {
	background-color: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin: 20rpx 24rpx;
}

/* 主操作按钮：蓝底白字 */
.btn-primary {
	background-color: #2979ff;
	color: #fff;
	border-radius: 12rpx;
	font-size: 30rpx;
}

/* 次操作按钮：白底蓝边 */
.btn-plain {
	background-color: #fff;
	color: #2979ff;
	border: 1rpx solid #2979ff;
	border-radius: 12rpx;
	font-size: 30rpx;
}

/* 危险操作按钮：白底红边（删除 / 解散等） */
.btn-danger {
	background-color: #fff;
	color: #fa3534;
	border: 1rpx solid #fa3534;
	border-radius: 12rpx;
	font-size: 30rpx;
}

/* 表单输入框：灰底圆角 */
.input {
	background-color: #f5f6f8;
	border-radius: 12rpx;
	padding: 20rpx 24rpx;
	font-size: 28rpx;
}

/* 标签基础样式：小号圆角色块，配合下方语义色使用 */
.tag {
	display: inline-block;
	padding: 4rpx 16rpx;
	border-radius: 8rpx;
	font-size: 22rpx;
}

/* 蓝色标签：进行中 / 信息类 */
.tag-blue {
	background-color: #e8f1ff;
	color: #2979ff;
}

/* 绿色标签：已完成 / 成功类 */
.tag-green {
	background-color: #e8f8ee;
	color: #19be6b;
}

/* 橙色标签：待处理 / 超时类 */
.tag-orange {
	background-color: #fff3e6;
	color: #ff9900;
}

/* 红色标签：错误 / 危险类 */
.tag-red {
	background-color: #ffecec;
	color: #fa3534;
}

/* 灰色标签：已取消 / 中性类 */
.tag-gray {
	background-color: #f0f0f0;
	color: #909399;
}

/* 横向布局行：常与 .flex1 配合实现左右分布 */
.row {
	display: flex;
	flex-direction: row;
	align-items: center;
}

/* 弹性伸缩：占满同行剩余空间 */
.flex1 {
	flex: 1;
}

/* 弱化文字：次要信息展示 */
.muted {
	color: #909399;
	font-size: 24rpx;
}
</style>
