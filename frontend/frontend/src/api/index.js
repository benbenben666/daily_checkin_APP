import { get, post, put, del } from './request'

// 认证
export const authApi = {
	register: (data) => post('/api/auth/register', data),
	login: (data) => post('/api/auth/login', data),
	me: () => get('/api/auth/me'),
	updateMe: (data) => put('/api/auth/me', data)
}

// 公司
export const companyApi = {
	create: (data) => post('/api/companies', data),
	mine: () => get('/api/companies/mine'),
	detail: (id) => get(`/api/companies/${id}`),
	join: (data) => post('/api/companies/join', data),
	members: (id) => get(`/api/companies/${id}/members`),
	removeMember: (id, userId) => del(`/api/companies/${id}/members/${userId}`),
	blacklist: (id, data) => post(`/api/companies/${id}/blacklist`, data),
	dissolve: (id) => post(`/api/companies/${id}/dissolve`)
}

// 申请与审批
export const applicationApi = {
	pending: (companyId) => get(`/api/companies/${companyId}/applications`),
	history: (companyId) => get(`/api/companies/${companyId}/applications/history`),
	mine: () => get('/api/applications/mine'),
	approve: (id) => post(`/api/applications/${id}/approve`),
	reject: (id, rejectReason) => post(`/api/applications/${id}/reject`, { rejectReason }),
	cancel: (id) => post(`/api/applications/${id}/cancel`)
}

// 任务
export const taskApi = {
	create: (companyId, data) => post(`/api/companies/${companyId}/tasks`, data),
	list: (companyId, view) => get(`/api/companies/${companyId}/tasks`, view ? { view } : {}),
	detail: (id) => get(`/api/tasks/${id}`),
	update: (id, data) => put(`/api/tasks/${id}`, data),
	complete: (id, data) => post(`/api/tasks/${id}/complete`, data),
	cancel: (id, cancelReason) => post(`/api/tasks/${id}/cancel`, { cancelReason })
}

// 附件
export const fileApi = {
	policy: (bizType) => post(`/api/files/policy?bizType=${bizType}`)
}

// 系统管理（仅 ADMIN）
export const adminApi = {
	users: (keyword) => get('/api/admin/users', keyword ? { keyword } : {}),
	updateUser: (id, data) => put(`/api/admin/users/${id}`, data),
	deleteUser: (id) => del(`/api/admin/users/${id}`),
	updateRole: (id, systemRole) => put(`/api/admin/users/${id}/role`, { systemRole }),
	companies: (keyword) => get('/api/admin/companies', keyword ? { keyword } : {}),
	dissolveCompany: (id) => post(`/api/admin/companies/${id}/dissolve`)
}
