// ============================================================
// 网络请求层：封装 uni.request，全项目 HTTP 出口
// 职责：基地址拼接、token 自动携带、统一解析后端 Result
// 结构（{ code, message, data }）、401 全局跳登录、错误 toast
// ============================================================

// 后端接口基地址。开发环境为本地后端；联调前请确认后端已开启 CORS（已配置）。
export const BASE_URL = 'http://localhost:8080'

// token 在本地存储中的 key
const TOKEN_KEY = 'auth_token'

// 401 跳转去重：并发请求同时 401 时只 reLaunch 一次登录页
let redirectingToLogin = false

/** 读取本地保存的登录令牌（无 token 返回空串） */
export function getToken() {
	return uni.getStorageSync(TOKEN_KEY) || ''
}

/** 登录成功后保存令牌 */
export function setToken(token) {
	uni.setStorageSync(TOKEN_KEY, token)
}

/** 退出登录 / 令牌失效时清除令牌 */
export function clearToken() {
	uni.removeStorageSync(TOKEN_KEY)
}

/** 登录成功后复位 401 跳转标志 */
export function resetAuthRedirect() {
	redirectingToLogin = false
}

/**
 * 统一请求封装（返回 Promise，直接 await 即可拿到 data）。
 * - 自动携带 Authorization: Bearer <token>
 * - 后端返回 { code, message, data }，code=0 时 resolve(data)，否则 reject(message)
 * - 401 时清除令牌并跳转登录页
 * @param {Object} options { url, method, data, header }
 */
export function request(options) {
	return new Promise((resolve, reject) => {
		const token = getToken()
		// 默认 JSON 请求头，允许调用方通过 options.header 覆盖扩展
		const header = { 'Content-Type': 'application/json', ...(options.header || {}) }
		if (token) {
			header['Authorization'] = 'Bearer ' + token
		}
		uni.request({
			url: BASE_URL + options.url,
			method: options.method || 'GET',
			data: options.data,
			header,
			success: (res) => {
				const body = res.data
				if (body && body.code === 0) {
					// 业务成功：只把 data 交给调用方
					resolve(body.data)
				} else {
					const msg = (body && body.message) || '请求失败'
					if (body && body.code === 401) {
						// 令牌无效/过期：清 token 后统一跳登录页（去重）
						clearToken()
						if (!redirectingToLogin) {
							redirectingToLogin = true
							uni.reLaunch({ url: '/pages/login/login' })
						}
					} else {
						// 其余业务错误：统一 toast 提示
						uni.showToast({ title: msg, icon: 'none' })
					}
					reject(new Error(msg))
				}
			},
			fail: (err) => {
				// 网络层失败（断网 / 超时 / 后端未启动）
				uni.showToast({ title: '网络异常，请检查网络连接状态或联系系统管理员', icon: 'none' })
				reject(err)
			}
		})
	})
}

// ---------------- 四个动词快捷方法 ----------------
export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })
