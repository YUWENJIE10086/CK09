import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getInfo as getInfoApi } from '@/api/login'

// 用户信息接口
export interface UserInfo {
  userId: number
  userName: string
  nickName: string
  avatar: string
  email: string
  phone: string
  userType: string
}

// 登录参数接口
export interface LoginParams {
  username: string
  password: string
  code?: string
  uuid?: string
}

/**
 * 用户状态管理 - 对接RuoYi后端
 */
export const useUserStore = defineStore('user', () => {
  // ========== 状态 ==========
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>({
    userId: 0,
    userName: '',
    nickName: '',
    avatar: '',
    email: '',
    phone: '',
    userType: '',
  })
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  // ========== 动作 ==========
  async function login(loginData: LoginParams): Promise<void> {
    try {
      // 后端返回 R<{token, userInfo}>，已被 request.ts 自动解开 → res 即 data
      const res: any = await loginApi({
        username: loginData.username,
        password: loginData.password,
      })
      const tk = res?.token || res?.data?.token
      if (!tk) throw new Error('登录返回token为空')
      token.value = tk
      localStorage.setItem('token', tk)
    } catch (error: any) {
      throw new Error(error?.msg || error?.message || '登录失败，请检查用户名和密码')
    }
  }

  async function getUserInfo(): Promise<void> {
    try {
      const res: any = await getInfoApi()
      // 已被拦截器解开 R 包装，res 可能是 {user, roles, permissions} 或 {userInfo, roles}
      const u = res?.user || res?.userInfo || res?.data?.user || res?.data?.userInfo || res
      if (u) {
        userInfo.value = {
          userId: u.userId || 0,
          userName: u.userName || '',
          nickName: u.nickName || u.userName || '',
          avatar: u.avatar || '',
          email: u.email || '',
          phone: u.phone || '',
          userType: u.userType || '00',
        }
      }
      // 根据 userType 映射角色
      const map: Record<string, string> = {
        '00': 'admin',
        '01': 'coop',
        '02': 'tech',
        '03': 'farmer',
        '04': 'station',
        '05': 'county',
        '06': 'city',
      }
      const role = map[userInfo.value.userType] || 'admin'
      roles.value = [role]
      permissions.value = ['*:*:*']
    } catch (error: any) {
      throw new Error(error?.msg || error?.message || '获取用户信息失败')
    }
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 即使接口失败也要清除本地
    } finally {
      resetState()
    }
  }

  function resetState(): void {
    token.value = ''
    userInfo.value = {
      userId: 0,
      userName: '',
      nickName: '',
      avatar: '',
      email: '',
      phone: '',
      userType: '',
    }
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    login,
    getUserInfo,
    logout,
    resetState,
  }
})
