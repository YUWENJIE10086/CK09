import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 设备类型 */
export type DeviceType = 'desktop' | 'mobile'

/**
 * 应用全局状态管理
 * 管理侧边栏折叠状态、设备类型等应用级状态
 */
export const useAppStore = defineStore('app', () => {
  // ========== 状态 ==========
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref<boolean>(false)
  /** 当前设备类型 */
  const device = ref<DeviceType>('desktop')

  // ========== 动作 ==========

  /**
   * 切换侧边栏折叠状态
   */
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /**
   * 设置侧边栏折叠状态
   * @param collapsed 是否折叠
   */
  function setSidebarCollapsed(collapsed: boolean): void {
    sidebarCollapsed.value = collapsed
  }

  /**
   * 设置设备类型
   * @param deviceType 设备类型
   */
  function setDevice(deviceType: DeviceType): void {
    device.value = deviceType
    // 移动端自动折叠侧边栏
    if (deviceType === 'mobile') {
      sidebarCollapsed.value = true
    }
  }

  /**
   * 关闭侧边栏（移动端使用）
   */
  function closeSidebar(): void {
    sidebarCollapsed.value = true
  }

  return {
    // 状态
    sidebarCollapsed,
    device,
    // 动作
    toggleSidebar,
    setSidebarCollapsed,
    setDevice,
    closeSidebar,
  }
})
