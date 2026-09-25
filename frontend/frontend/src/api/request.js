// 后端接口基地址。开发环境为本地后端；联调前请确认后端已开启 CORS（已配置）。
export const BASE_URL = 'http://localhost:8080'

const TOKEN_KEY = 'auth_token'

export function getToken() {
	return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
	uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
	uni.removeStorageSync(TOKEN_KEY)
}

/**
 * 统一请求封装。
 * - 自动携带 Authorization: Bearer <token>
 * - 后端返回 { code, message, data }，code=0 时 resolve(data)，否则 reject(message)
 * - 401 时清除令牌并跳转登录页
 */
export function request(options) {
	return new Promise((resolve, reject) => {
		const token = getToken()
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
					resolve(body.data)
				} else {
					const msg = (body && body.message) || '请求失败'
					if (body && body.code === 401) {
						clearToken()
						uni.reLaunch({ url: '/pages/login/login' })
					} else {
						uni.showToast({ title: msg, icon: 'none' })
					}
					reject(new Error(msg))
				}
			},
			fail: (err) => {
				uni.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
				reject(err)
			}
		})
	})
}

export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })
