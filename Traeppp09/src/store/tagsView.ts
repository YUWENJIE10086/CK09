import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

/** 标签页项 */
export interface TagView {
  /** 路由路径 */
  path: string
  /** 路由名称 */
  name: string
  /** 标签标题 */
  title: string
  /** 路由查询参数 */
  query?: Record<string, string>
}

/**
 * 标签页状态管理
 */
export const useTagsViewStore = defineStore('tagsView', () => {
  /** 已打开的标签页列表 */
  const visitedViews = ref<TagView[]>([])

  /** 添加标签页 */
  const addView = (route: RouteLocationNormalized) => {
    if (visitedViews.value.some(v => v.path === route.path)) return
    const title = (route.meta?.title as string) || route.name?.toString() || route.path
    visitedViews.value.push({
      path: route.path,
      name: route.name?.toString() || '',
      title,
      query: route.query as Record<string, string>,
    })
  }

  /** 删除标签页 */
  const removeView = (path: string) => {
    const idx = visitedViews.value.findIndex(v => v.path === path)
    if (idx > -1) {
      visitedViews.value.splice(idx, 1)
    }
  }

  /** 关闭其他标签页 */
  const removeOtherViews = (path: string) => {
    visitedViews.value = visitedViews.value.filter(v => v.path === path)
  }

  /** 关闭所有标签页 */
  const removeAllViews = () => {
    visitedViews.value = []
  }

  return {
    visitedViews,
    addView,
    removeView,
    removeOtherViews,
    removeAllViews,
  }
})
