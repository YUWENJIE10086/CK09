/**
 * 用户管理API - 对接RuoYi后端
 */
import { get, post, put, del } from './request'

export interface UserListParams {
  pageNum?: number
  pageSize?: number
  userName?: string
  phone?: string
  status?: string
  userType?: string
}

/** 获取用户列表（分页） */
export function getUserList(params: UserListParams = {}): Promise<any> {
  return get('/system/user/list', params)
}

/** 获取用户详情 */
export function getUserInfo(userId: number): Promise<any> {
  return get(`/system/user/${userId}`)
}

/** 新增用户 */
export function addUser(data: any): Promise<any> {
  return post('/system/user', data)
}

/** 修改用户 */
export function updateUser(data: any): Promise<any> {
  return put('/system/user', data)
}

/** 删除用户 */
export function deleteUser(userId: number): Promise<any> {
  return del(`/system/user/${userId}`)
}

/** 重置密码（管理员操作） */
export function resetUserPwd(userId: number, newPassword: string): Promise<any> {
  return put(`/system/user/resetPwd/${userId}`, { newPassword })
}

/** 修改密码（用户自己修改） */
export function updateUserPwd(userId: number, oldPassword: string, newPassword: string): Promise<any> {
  return put('/system/user/updatePwd', { userId, oldPassword, newPassword })
}

/** 修改用户状态 */
export function changeUserStatus(userId: number, status: string): Promise<any> {
  return put('/system/user/changeStatus', { userId, status })
}
