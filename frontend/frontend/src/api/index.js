// ============================================================
// 后端接口定义层：按业务模块集中声明所有 HTTP 接口
// 页面统一 import { xxxApi } 调用，不在页面里手拼 URL，
// 好处是后端路径变动时只改这一处
// ============================================================
import { get, post, put, del } from './request'

// ---------------- 认证模块 ----------------
export const authApi = {
	/** 注册：传手机号、密码、昵称（可选） */
	register: (data) => post('/api/auth/register', data),
	/** 登录：成功返回 { token, userId, systemRole, ... } */
	login: (data) => post('/api/auth/login', data),
	/** 获取当前登录用户信息 */
	me: () => get('/api/auth/me'),
	/** 更新当前用户资料（目前仅支持改昵称） */
	updateMe: (data) => put('/api/auth/me', data)
}

// ---------------- 公司模块 ----------------
export const companyApi = {
	/** 创建公司（创建者自动成为创始人 FOUNDER） */
	create: (data) => post('/api/companies', data),
	/** 我加入的公司列表 */
	mine: () => get('/api/companies/mine'),
	/** 公司详情（含 inviteCode 邀请码、myRole 我在该公司内的角色） */
	detail: (id) => get(`/api/companies/${id}`),
	/** 凭邀请码申请加入公司（管理者身份需创始人审批） */
	join: (data) => post('/api/companies/join', data),
	/** 公司成员列表 */
	members: (id) => get(`/api/companies/${id}/members`),
	/** 移除成员（创始人/管理者可用） */
	removeMember: (id, userId) => del(`/api/companies/${id}/members/${userId}`),
	/** 拉黑成员：移出公司并进黑名单，之后无法再申请加入 */
	blacklist: (id, data) => post(`/api/companies/${id}/blacklist`, data),
	/** 主动退出公司（创始人不可退出，只能解散） */
	leave: (id) => post(`/api/companies/${id}/leave`),
	/** 解散公司（仅创始人） */
	dissolve: (id) => post(`/api/companies/${id}/dissolve`)
}

// ---------------- 申请与审批模块 ----------------
export const applicationApi = {
	/** 待审批申请列表（管理者视角） */
	pending: (companyId) => get(`/api/companies/${companyId}/applications`),
	/** 全部历史申请（管理者视角，含已通过/拒绝/撤回） */
	history: (companyId) => get(`/api/companies/${companyId}/applications/history`),
	/** 我提交过的加入申请 */
	mine: () => get('/api/applications/mine'),
	/** 通过申请 */
	approve: (id) => post(`/api/applications/${id}/approve`),
	/** 拒绝申请（可附拒绝理由） */
	reject: (id, rejectReason) => post(`/api/applications/${id}/reject`, { rejectReason }),
	/** 撤回我的申请（仅待审批状态可撤回） */
	cancel: (id) => post(`/api/applications/${id}/cancel`)
}

// ---------------- 任务模块 ----------------
export const taskApi = {
	/** 发布任务（ companyId 公司内） */
	create: (companyId, data) => post(`/api/companies/${companyId}/tasks`, data),
	/**
	 * 任务列表。
	 * view 为空 = 管理者全量列表；'grab' = 员工待办；'history' = 员工历史
	 */
	list: (companyId, view) => get(`/api/companies/${companyId}/tasks`, view ? { view } : {}),
	/** 任务详情（含指派人、提交内容等） */
	detail: (id) => get(`/api/tasks/${id}`),
	/** 修改任务 */
	update: (id, data) => put(`/api/tasks/${id}`, data),
	/** 完成任务 / 超时补交（ submitContent 为提交说明，可空） */
	complete: (id, data) => post(`/api/tasks/${id}/complete`, data),
	/** 取消任务（可附取消原因） */
	cancel: (id, cancelReason) => post(`/api/tasks/${id}/cancel`, { cancelReason })
}

// ---------------- 附件模块 ----------------
export const fileApi = {
	/** 获取文件直传策略（ bizType 为业务类型） */
	policy: (bizType) => post(`/api/files/policy?bizType=${bizType}`)
}

// ---------------- 系统管理模块（仅 ADMIN 可调用） ----------------
export const adminApi = {
	/** 用户列表（ keyword 昵称/手机号模糊搜索） */
	users: (keyword) => get('/api/admin/users', keyword ? { keyword } : {}),
	/** 更新用户信息（如启用/禁用） */
	updateUser: (id, data) => put(`/api/admin/users/${id}`, data),
	/** 删除用户（软删除，历史数据保留） */
	deleteUser: (id) => del(`/api/admin/users/${id}`),
	/** 修改用户系统角色（ USER / ADMIN） */
	updateRole: (id, systemRole) => put(`/api/admin/users/${id}/role`, { systemRole }),
	/** 公司列表（ keyword 搜索） */
	companies: (keyword) => get('/api/admin/companies', keyword ? { keyword } : {}),
	/** 强制解散公司 */
	dissolveCompany: (id) => post(`/api/admin/companies/${id}/dissolve`)
}
