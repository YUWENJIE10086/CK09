import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getReservationCalendar, addReservation, approveReservation, deleteReservation } from '@/api/reservation'

/**
 * 预约数据共享状态
 * 确保预约审核列表和预约日历数据同步
 */
export const useReservationStore = defineStore('reservation', () => {
  /** 所有预约数据 */
  const allReservations = ref<any[]>([])

  /** 数据版本号，每次修改递增 */
  const version = ref(0)

  /** 是否正在加载 */
  const loading = ref(false)

  /** 从数据库加载所有预约数据 */
  async function fetchAll() {
    loading.value = true
    try {
      const res: any = await getReservationCalendar()
      const rows = res?.rows || res?.data?.rows || (Array.isArray(res) ? res : [])
      console.log('[ReservationStore] fetchAll response:', res, 'rows count:', rows.length)
      if (rows.length > 0) {
        console.log('[ReservationStore] sample row:', JSON.stringify(rows[0]))
      }
      allReservations.value = rows
    } catch (e: any) {
      console.error('[ReservationStore] fetchAll failed:', e?.message || e)
      allReservations.value = []
    } finally {
      loading.value = false
    }
  }

  /** 新增预约 → 写入数据库 → 刷新缓存 */
  async function createReservation(data: any) {
    await addReservation(data)
    await fetchAll()
    version.value++
  }

  /** 审核预约 → 写入数据库 → 刷新缓存 */
  async function approve(id: string, status: string, opinion: string) {
    await approveReservation(id, status, opinion)
    await fetchAll()
    version.value++
  }

  /** 删除预约 → 写入数据库 → 刷新缓存 */
  async function remove(id: number) {
    await deleteReservation(id)
    await fetchAll()
    version.value++
  }

  return {
    allReservations,
    version,
    loading,
    fetchAll,
    createReservation,
    approve,
    remove,
  }
})
