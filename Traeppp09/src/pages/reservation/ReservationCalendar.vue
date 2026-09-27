<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, Calendar } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { getReservationCalendar } from '@/api/reservation'
import { getBarnOptions } from '@/api/barn'
import { useReservationStore } from '@/store/reservation'
import StatusTag from '@/components/StatusTag.vue'
import BarnCode from '@/components/BarnCode.vue'

/** 共享预约数据 store */
const reservationStore = useReservationStore()
const { allReservations, loading } = storeToRefs(reservationStore)

/** 中文状态 → StatusTag key映射 */
const statusKeyMap: Record<string, string> = {
  '待审核': 'pending',
  '已确认': 'confirmed',
  '使用中': 'baking',
  '烘烤中': 'baking',
  '已完成': 'finished',
  '已取消': 'rejected',
  '已驳回': 'rejected',
  '超时': 'timeout',
}

/** 状态对应色块颜色 - 统一颜色规范，已确认和已完成用不同颜色区分 */
const statusColorMap: Record<string, string> = {
  '待审核': '#F39C12',      // 黄色
  '已确认': '#3498DB',      // 蓝色 - 区分已完成
  '使用中': '#2980B9',      // 深蓝色
  '烘烤中': '#2980B9',      // 深蓝色
  '已完成': '#2E8B6A',      // 绿色 - 区分已确认
  '已取消': '#94A3B8',      // 灰色
  '已驳回': '#94A3B8',      // 灰色
  '超时': '#E74C3C',        // 红色
}

/** 选中的烤房ID */
const selectedBarnId = ref<string | null>(null)

/** 烤房选项（从后端拿） */
const barnOptions = ref<any[]>([])

/** 日历当前日期 */
const calendarDate = ref(new Date())

/** 当前日历显示的年份和月份文本 */
const calendarTitle = computed(() => {
  const d = calendarDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

/** 跳转到上个月 */
function goToPrevMonth() {
  const d = calendarDate.value
  calendarDate.value = new Date(d.getFullYear(), d.getMonth() - 1, 1)
}

/** 跳转到下个月 */
function goToNextMonth() {
  const d = calendarDate.value
  calendarDate.value = new Date(d.getFullYear(), d.getMonth() + 1, 1)
}

/** 回到今天 */
function goToToday() {
  calendarDate.value = new Date()
}

/** 详情对话框 */
const detailDialog = ref(false)
const detailReservations = ref<any[]>([])
const detailDate = ref('')

/** 当前显示的预约数据（根据选中烤房过滤） */
const filteredReservations = computed(() => {
  const list = allReservations.value
  if (!selectedBarnId.value) return list
  return list.filter(r => r.ovenId === selectedBarnId.value)
})

/** 格式化日期为 YYYY-MM-DD */
function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 解析后端的时间字段（LocalDateTime 序列化为数组或字符串） */
function parseTime(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v.replace('T', ' ')
  if (Array.isArray(v) && v.length >= 3) {
    const [y, m, d, hh = 0, mm = 0, ss = 0] = v
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
  }
  return String(v)
}

/** 解析日期字段（LocalDate 序列化为数组或字符串） */
function parseDate(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v.split(' ')[0].split('T')[0]
  if (Array.isArray(v) && v.length >= 3) {
    const [y, m, d] = v
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return String(v)
}

/** 缓存日期→预约列表的映射，避免重复计算 */
const reservationsByDateMap = computed(() => {
  const map = new Map<string, any[]>()
  const list = filteredReservations.value
  for (const r of list) {
    // 确定该预约覆盖的日期范围
    const startStr = parseDate(r.reserveStart || r.reserveDate)
    const endStr = parseDate(r.reserveEnd || r.reserveStart || r.reserveDate)
    if (!startStr) continue

    const startDate = new Date(startStr + 'T00:00:00')
    const endDate = endStr ? new Date(endStr + 'T00:00:00') : startDate

    // 遍历日期范围内的每一天
    const current = new Date(startDate)
    while (current <= endDate) {
      const key = formatDate(current)
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(r)
      current.setDate(current.getDate() + 1)
    }
  }
  return map
})

/** 获取某日期的预约列表（使用缓存） */
function getReservationsByDate(date: Date): any[] {
  const dateStr = formatDate(date)
  return reservationsByDateMap.value.get(dateStr) || []
}

/** 点击日期格子 */
function handleDateClick(date: Date) {
  const reservations = getReservationsByDate(date)
  if (reservations.length === 0) return
  detailDate.value = formatDate(date)
  detailReservations.value = reservations
  detailDialog.value = true
}

/** 选择烤房 */
function handleSelectBarn(barnId: number | null) {
  selectedBarnId.value = barnId
}

/** 加载数据 — 通过 store 共享 */
async function fetchData() {
  await reservationStore.fetchAll()
  console.log('[ReservationCalendar] loaded:', allReservations.value.length, 'records')
  if (allReservations.value.length > 0) {
    console.log('[ReservationCalendar] sample:', JSON.stringify(allReservations.value[0]))
  }
}

/** 加载烤房列表 */
async function loadBarns() {
  try {
    const res: any = await getBarnOptions()
    barnOptions.value = Array.isArray(res) ? res : (res?.rows || res?.data || [])
  } catch {
    barnOptions.value = []
  }
}

onMounted(() => {
  fetchData()
  loadBarns()
})

/** 监控数据变化 */
watch(allReservations, (val) => {
  console.log('[ReservationCalendar] allReservations changed:', val.length, 'records')
  if (val.length > 0) {
    const sample = val[0]
    console.log('[ReservationCalendar] sample keys:', Object.keys(sample))
    console.log('[ReservationCalendar] sample reserveDate:', sample.reserveDate, 'reserveStart:', sample.reserveStart, 'reserveEnd:', sample.reserveEnd, 'status:', sample.status)
  }
}, { immediate: true })
</script>

<template>
  <div class="page-container calendar-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><Calendar /></el-icon>
        </div>
        <h2 class="page-title">预约日历</h2>
      </div>
      <p class="page-desc">
        按日历视图查看烤房预约安排
        <span v-if="loading" class="data-status loading">加载中...</span>
        <span v-else-if="allReservations.length > 0" class="data-status loaded">共 {{ allReservations.length }} 条预约</span>
        <span v-else class="data-status empty">暂无数据，请确认已登录</span>
      </p>
    </div>

    <div class="calendar-layout">
      <!-- 左侧烤房列表 -->
      <div class="barn-sidebar">
        <div class="barn-sidebar-header">
          <el-icon><OfficeBuilding /></el-icon>
          <span>烤房列表 ({{ barnOptions.length }})</span>
        </div>
        <div class="barn-list">
          <div
            class="barn-item"
            :class="{ active: selectedBarnId === null }"
            @click="handleSelectBarn(null)"
          >
            <el-icon><Grid /></el-icon>
            <span>全部烤房</span>
          </div>
          <div
            v-for="barn in barnOptions"
            :key="barn.id"
            class="barn-item"
            :class="{ active: selectedBarnId === barn.id }"
            @click="handleSelectBarn(barn.id)"
          >
            <el-icon><HomeFilled /></el-icon>
            <span>{{ barn.barnName }}</span>
          </div>
        </div>
        <!-- 图例 -->
        <div class="legend">
          <div class="legend-title">状态图例</div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #F39C12"></span>待审核
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #3498DB"></span>已确认
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #2980B9"></span>使用中
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #2E8B6A"></span>已完成
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #94A3B8"></span>已取消
          </div>
          <div class="legend-item">
            <span class="legend-dot" style="background: #E74C3C"></span>超时
          </div>
        </div>
      </div>

      <!-- 右侧日历 -->
      <div class="calendar-main">
        <!-- 月份导航栏 -->
        <div class="calendar-nav">
          <el-button :icon="ArrowLeft" @click="goToPrevMonth" size="small">上个月</el-button>
          <span class="calendar-nav-title">{{ calendarTitle }}</span>
          <el-button :icon="ArrowRight" @click="goToNextMonth" size="small" class="nav-next-btn">下个月</el-button>
          <el-button type="primary" size="small" @click="goToToday" class="nav-today-btn">今天</el-button>
        </div>
        <!-- 调试信息（临时） -->
        <div v-if="allReservations.length === 0 && !loading" class="debug-info">
          <el-empty description="暂无预约数据">
            <template #description>
              <p>暂无预约数据</p>
              <p style="font-size: 12px; color: #999;">请确认已登录后刷新页面</p>
            </template>
            <el-button type="primary" @click="fetchData">重新加载</el-button>
          </el-empty>
        </div>
        <el-calendar v-else v-model="calendarDate">
          <template #date-cell="{ data }">
            <div class="calendar-cell" @click="handleDateClick(new Date(data.day))">
              <div class="calendar-day">{{ data.day.split('-')[2] }}</div>
              <div class="calendar-tags">
                <template v-for="r in getReservationsByDate(new Date(data.day))" :key="r.id">
                  <div
                    class="calendar-tag"
                    :style="{ backgroundColor: (statusColorMap[r.status] || '#94A3B8') + '18', borderLeftColor: statusColorMap[r.status] || '#94A3B8' }"
                    :title="`${r.userName || ''} - ${r.status || ''}`"
                  >
                    <span class="tag-name">{{ r.userName || r.barnName }}</span>
                    <span class="tag-status" :style="{ color: statusColorMap[r.status] || '#94A3B8', backgroundColor: (statusColorMap[r.status] || '#94A3B8') + '20' }">{{ r.status }}</span>
                  </div>
                </template>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>
    </div>

    <!-- 预约详情对话框 -->
    <el-dialog v-model="detailDialog" :title="`${detailDate} 预约详情`" width="700px" destroy-on-close class="stripe-dialog" append-to-body align-center>
      <el-table :data="detailReservations" style="width: 100%">
        <el-table-column prop="id" label="预约编号" width="100" align="center" />
        <el-table-column prop="userName" label="烟农姓名" width="100" align="center" />
        <el-table-column label="烤房编号" width="160" align="center">
          <template #default="{ row }">
            <BarnCode :code="row.ovenId" />
          </template>
        </el-table-column>
        <el-table-column prop="barnName" label="烤房名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="开始时间" width="160" align="center">
          <template #default="{ row }">{{ parseTime(row.reserveStart).slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160" align="center">
          <template #default="{ row }">{{ parseTime(row.reserveEnd).slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <StatusTag :status="statusKeyMap[row.status]" type="reservation" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ========================================
   毛玻璃质感设计系统 - 日历页面
   ======================================== */

/* 页面容器 */
.calendar-page {
  height: calc(100vh - var(--navbar-height) - 40px);
  overflow: hidden;
  padding: clamp(16px, 2vw, 24px);
  background: linear-gradient(180deg, #F8FAFB 0%, #EEF2F6 100%);
}

/* 页面标题 */
.page-header {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(8px, 1vw, 12px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(32px, 4vw, 40px);
  height: clamp(32px, 4vw, 40px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
}

.page-title {
  font-size: clamp(18px, 2.5vw, 24px);
  font-weight: 700;
  color: #1A2A3A;
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.5vw, 14px);
  color: #6B7C8D;
  margin: 0;
  padding-left: clamp(40px, 5vw, 52px);
}

/* 数据状态标签 */
.data-status {
  margin-left: clamp(8px, 1vw, 12px);
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.data-status.loading {
  background: rgba(37, 99, 235, 0.12);
  color: #2563EB;
  backdrop-filter: blur(8px);
}

.data-status.loaded {
  background: rgba(5, 150, 105, 0.12);
  color: #059669;
  backdrop-filter: blur(8px);
}

.data-status.empty {
  background: rgba(220, 38, 38, 0.1);
  color: #DC2626;
  backdrop-filter: blur(8px);
}

/* 日历布局 */
.calendar-layout {
  display: flex;
  gap: clamp(12px, 1.5vw, 20px);
  height: calc(100% - 70px);
}

/* 毛玻璃侧边栏 */
.barn-sidebar {
  width: clamp(200px, 18vw, 280px);
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.barn-sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: clamp(12px, 1.5vw, 16px);
  font-size: clamp(13px, 1.5vw, 15px);
  font-weight: 600;
  color: #0A4D3E;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.barn-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.barn-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: clamp(8px, 1vw, 10px) clamp(10px, 1.2vw, 12px);
  border-radius: 8px;
  cursor: pointer;
  font-size: clamp(12px, 1.3vw, 13px);
  color: #4B5563;
  transition: all 0.2s ease;
  margin-bottom: 2px;
  background: transparent;
}

.barn-item:hover {
  background: rgba(10, 77, 62, 0.06);
  color: #0A4D3E;
  transform: translateX(2px);
}

.barn-item.active {
  background: linear-gradient(135deg, rgba(10, 77, 62, 0.12) 0%, rgba(10, 77, 62, 0.08) 100%);
  color: #0A4D3E;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(10, 77, 62, 0.15);
}

.barn-item .el-icon {
  flex-shrink: 0;
}

.barn-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 图例 */
.legend {
  padding: clamp(10px, 1.2vw, 14px) clamp(12px, 1.5vw, 16px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.legend-title {
  font-size: 11px;
  color: #6B7C8D;
  margin-bottom: 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #4B5563;
  margin-bottom: 4px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 毛玻璃日历主区域 */
.calendar-main {
  flex: 1;
  min-width: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* 月份导航栏 */
.calendar-nav {
  display: flex;
  align-items: center;
  gap: clamp(8px, 1vw, 12px);
  padding: clamp(10px, 1.2vw, 14px) clamp(16px, 2vw, 24px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.calendar-nav-title {
  font-size: clamp(14px, 1.8vw, 18px);
  font-weight: 700;
  color: #1A2A3A;
  flex: 1;
  text-align: center;
  user-select: none;
}

/* 导航按钮样式 */
.calendar-nav :deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.calendar-nav :deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #EEF2F6 0%, #E5E9EF 100%);
}

.calendar-nav :deep(.el-button:active) {
  transform: translateY(0);
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.nav-next-btn .el-icon {
  margin-left: 4px;
}

/* 今天按钮 - 深绿渐变 */
.nav-today-btn {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
}

.nav-today-btn:hover {
  background: linear-gradient(135deg, #0D5D46 0%, #0A4D3E 100%) !important;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.25) !important;
  transform: translateY(-2px) !important;
}

.nav-today-btn:active {
  transform: translateY(0) !important;
  box-shadow: 0 1px 4px rgba(10, 77, 62, 0.3), inset 0 1px 2px rgba(0, 0, 0, 0.1) !important;
}

.debug-info {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}

/* 日历组件样式 */
.calendar-main :deep(.el-calendar) {
  height: 100%;
  --el-calendar-border: none;
}

.calendar-main :deep(.el-calendar__header) {
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.calendar-main :deep(.el-calendar__body) {
  padding: 8px;
}

.calendar-main :deep(.el-calendar-table .el-calendar-day) {
  height: auto;
  min-height: 100px;
  padding: 4px;
}

.calendar-cell {
  cursor: pointer;
  height: 100%;
  transition: all 0.2s ease;
  border-radius: 8px;
  padding: 4px;
}

.calendar-cell:hover {
  background: rgba(10, 77, 62, 0.06);
}

.calendar-day {
  font-size: clamp(12px, 1.4vw, 14px);
  font-weight: 600;
  color: #1A2A3A;
  margin-bottom: 4px;
}

.calendar-tags {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 日历标签样式 */
.calendar-tag {
  font-size: clamp(10px, 1.1vw, 11px);
  color: #1A2A3A;
  padding: 3px 8px;
  border-radius: 6px;
  border-left: 3px solid;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 18px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(4px);
  transition: transform 0.15s ease;
}

.calendar-tag:hover {
  transform: translateX(2px);
}

.tag-name {
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.tag-status {
  font-size: 9px;
  font-weight: 700;
  flex-shrink: 0;
  padding: 1px 5px;
  border-radius: 4px;
  line-height: 14px;
}

/* 对话框毛玻璃样式 */
.stripe-dialog :deep(.el-dialog) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.85) 100%);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stripe-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 20px 24px !important;
  background: transparent;
}

.stripe-dialog :deep(.el-dialog__body) {
  padding: 24px !important;
  background: transparent;
}

.stripe-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px !important;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

.stripe-dialog :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.3);
}

.stripe-dialog :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #0A4D3E 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.4);
}

.stripe-dialog :deep(.el-button:not(.el-button--primary)) {
  border-radius: 8px;
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

/* 表格样式 */
.stripe-dialog :deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

.stripe-dialog :deep(.el-table th) {
  background: linear-gradient(135deg, #F8FAFB 0%, #EEF2F6 100%);
  color: #374151;
  font-weight: 600;
}

.stripe-dialog :deep(.el-table tr:hover > td) {
  background: rgba(10, 77, 62, 0.04) !important;
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .calendar-layout {
    flex-direction: column;
    height: auto;
    min-height: calc(100% - 70px);
  }
  
  .barn-sidebar {
    width: 100%;
    max-height: 200px;
  }
  
  .barn-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px;
  }
  
  .barn-item {
    flex: 0 0 auto;
    min-width: 100px;
  }
  
  .legend {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    border-top: none;
    border-left: 1px solid rgba(0, 0, 0, 0.06);
  }
  
  .legend-item {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .calendar-page {
    padding: 12px;
  }
  
  .calendar-nav {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .calendar-nav-title {
    order: -1;
    width: 100%;
    margin-bottom: 8px;
  }
  
  .calendar-main :deep(.el-calendar-table .el-calendar-day) {
    min-height: 60px;
  }
}
</style>
