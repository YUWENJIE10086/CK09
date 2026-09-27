import { defineStore } from 'pinia'
import { ref } from 'vue'
import { asyncRoutes, constantRoutes } from '@/router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 判断用户是否有权限访问该路由
 * @param roles 用户角色列表
 * @param route 路由配置
 */
function hasPermission(roles: string[], route: RouteRecordRaw): boolean {
  if (route.meta?.roles) {
    return roles.some((role) => (route.meta!.roles as string[]).includes(role))
  }
  // 没有配置roles则默认有权限
  return true
}

/**
 * 递归过滤异步路由
 * 根据用户角色过滤出有权限访问的路由
 * @param routes 待过滤的路由列表
 * @param roles 用户角色列表
 */
function filterAsyncRoutes(routes: RouteRecordRaw[], roles: string[]): RouteRecordRaw[] {
  const filtered: RouteRecordRaw[] = []

  routes.forEach((route) => {
    const temp = { ...route } as RouteRecordRaw
    if (hasPermission(roles, temp)) {
      if (temp.children) {
        // 递归过滤子路由
        temp.children = filterAsyncRoutes(temp.children, roles)
      }
      filtered.push(temp)
    }
  })

  return filtered
}

/**
 * 权限状态管理
 * 管理动态路由的生成和过滤
 */
export const usePermissionStore = defineStore('permission', () => {
  // ========== 状态 ==========
  /** 动态路由列表（根据角色过滤后的路由） */
  const routes = ref<RouteRecordRaw[]>([])
  /** 已添加的动态路由（用于判断是否已生成） */
  const addedRoutes = ref<RouteRecordRaw[]>([])

  // ========== 动作 ==========

  /**
   * 根据角色生成可访问的路由
   * @param roles 用户角色列表
   * @returns 过滤后的动态路由列表
   */
  function generateRoutes(roles: string[]): RouteRecordRaw[] {
    let accessedRoutes: RouteRecordRaw[]

    // admin角色拥有所有路由权限
    if (roles.includes('admin')) {
      accessedRoutes = asyncRoutes
    } else {
      // 根据角色过滤路由
      accessedRoutes = filterAsyncRoutes(asyncRoutes, roles)
    }

    routes.value = accessedRoutes
    addedRoutes.value = accessedRoutes
    return accessedRoutes
  }

  /**
   * 重置权限路由
   * 退出登录时调用
   */
  function resetRoutes(): void {
    routes.value = []
    addedRoutes.value = []
  }

  return {
    // 状态
    routes,
    addedRoutes,
    // 动作
    generateRoutes,
    resetRoutes,
  }
})
