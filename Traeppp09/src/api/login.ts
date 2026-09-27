/**
 * 登录认证API - 对接RuoYi后端
 */
import { post } from './request'

export interface LoginBody {
  username: string
  password: string
}

export function login(data: LoginBody): Promise<any> {
  return post('/api/login', data) as any
}

export function logout(): Promise<any> {
  return post('/api/logout')
}

export function getInfo(): Promise<any> {
  return post('/api/getInfo') as any
}
