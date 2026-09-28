// ============================================================
// 应用入口文件（uni-app 规范入口）
// uni-app 要求导出 createApp 工厂函数而非直接实例化应用，
// 以便框架兼容 SSR 及各小程序平台（H5 / 微信 / 支付宝等）
// ============================================================
import { createSSRApp } from 'vue'
import App from './App.vue'

// 创建 Vue 应用实例，返回值由 uni-app 框架接管并完成挂载
export function createApp() {
	const app = createSSRApp(App)
	return {
		app
	}
}
