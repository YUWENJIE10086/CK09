<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, Delete, Connection, RefreshLeft, CircleClose, VideoPlay } from '@element-plus/icons-vue'
import { Layers, Thermometer, CircleDot, ClipboardCheck } from 'lucide-vue-next'
import StatusTag from '@/components/StatusTag.vue'
import BarnCode from '@/components/BarnCode.vue'
import {
  getAssignmentList,
  addAssignment,
  updateAssignment,
  deleteAssignment,
  getAssignmentStats,
  getAssignmentsByBarn,
  searchFarmer,
  autoReleaseAssignment,
} from '@/api/assignment'
import { getBarnAssignList } from '@/api/barn'
import { getReservationList } from '@/api/reservation'
import { listCounty, listTownship } from '@/api/dict'
import { Phone } from '@element-plus/icons-vue'

const router = useRouter()

/** 烤房使用状态 → StatusTag key */
const useStatusKeyMap: Record<string, string> = {
  '在烤': 'baking',
  '空闲': 'idle',
}
const healthLevelKeyMap: Record<string, string> = {
  '优良': 'excellent',
  '需维护': 'maintenance',
  '急需修复': 'urgent',
  '退出': 'retired',
}

/** 分配状态中文 → StatusTag key */
const assignStatusKeyMap: Record<string, string> = {
  '已分配': 'confirmed',
  '使用中': 'baking',
  '已归还': 'archived',
}

/** 统计卡片配置（每张卡片独立配色 + 图标） */
const statCards = [
  { key: 'total', title: '总烤房数', icon: Layers, color: '#0D5D46', bg: '#E8F5EE' },
  { key: 'baking', title: '在烤', icon: Thermometer, color: '#2E8B6A', bg: '#E0F2EA' },
  { key: 'assigned', title: '已分配', icon: ClipboardCheck, color: '#1565C0', bg: '#E3F2FD' },
  { key: 'archived', title: '空闲', icon: CircleDot, color: '#D4944A', bg: '#FBEEDB' },
] as const

const stats = reactive({ total: 0, baking: 0, assigned: 0, archived: 0 })

/** 预警窗口：待审核预约申请数 */
const reservationAlertClosed = ref(false)
const reservationApplyCount = ref(0)

async function loadReservationCount() {
  try {
    const res: any = await getReservationList({ pageNum: 1, pageSize: 9999 })
    reservationApplyCount.value = (res?.rows || []).filter((r: any) => r.status === '待审核').length
  } catch { reservationApplyCount.value = 0 }
}

/** 筛选表单 */
const filterForm = reactive({
  barnName: '',
  id: '',
  countyCode: '',
  townCode: '',
  useStatus: '',
  facilityStatus: '',
})

const countyOptions = ref<{ value: string; label: string }[]>([])
const townOptions = ref<{ value: string; label: string }[]>([])

/** 烤房表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({ pageNum: 1, pageSize: 10 })

/** 每个烤房的最新分配信息映射 */
const barnAssignmentMap = ref<Record<string, any>>({})

/** 分配对话框 */
const assignDialog = ref(false)
const assignLoading = ref(false)
const assignForm = reactive({
  ovenId: undefined as string | undefined,
  barnName: '',
  id: '',
  farmerId: undefined as string | undefined,
  farmerName: '',
  farmerPhone: '',
  /** 分配时间段 [startTime, endTime]，格式 YYYY-MM-DD HH:mm:ss */
  timeRange: [] as string[],
  remark: '',
})

/** 选中烟农的详情信息（用于在对话框下方显示填充） */
const selectedFarmerInfo = ref<any>(null)

/** 烟农远程搜索 */
const farmerOptions = ref<any[]>([])
const farmerLoading = ref(false)

/** 分配历史对话框 */
const historyDialog = ref(false)
const historyBarnName = ref('')
const historyList = ref<any[]>([])
const historyLoading = ref(false)

/** 时间解析 */
function parseTime(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v
  if (Array.isArray(v) && v.length >= 3) {
    const [y, m, d, hh = 0, mm = 0, ss = 0] = v
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
  }
  return String(v)
}

/** 判断分配状态 */
function getAssignStatus(barnId: string): string {
  const assign = barnAssignmentMap.value[barnId]
  if (!assign) return 'none' // 无分配记录 = 未分配
  // 后端返回的是中文状态：已分配/使用中/已归还/已撤销
  // 已分配和使用中都算"已分配"状态，显示 使用+归还+历史
  if (assign.status === '已分配' || assign.status === '使用中') return 'assigned'
  // 已归还和已撤销都算"未分配"状态，显示 分配+历史
  return 'none'
}

/** 加载统计数据 */
async function loadStats() {
  // 基于barnAssignmentMap统计分配状态
  const total = Object.keys(barnAssignmentMap.value).length
  const assigned = Object.values(barnAssignmentMap.value).filter((a: any) => a.status === '已分配' || a.status === '使用中').length
  const completed = Object.values(barnAssignmentMap.value).filter((a: any) => a.status === '已归还' || a.status === '已撤销').length

  stats.assigned = assigned
  stats.archived = completed
  // total 和 baking 由 loadData 基于完整列表计算，这里不覆盖
  if (total > stats.total) stats.total = total
}

/** 加载县区 */
async function loadCounties() {
  try {
    const res: any = await listCounty()
    countyOptions.value = (res || []).map((c: any) => ({ value: c.countyName, label: c.countyName }))
  } catch { /* ignore */ }
}

/** 县区变化时联动加载乡镇 */
function handleCountyChange(countyName: string) {
  filterForm.townCode = ''
  loadTowns(countyName)
}

/** 加载乡镇 */
async function loadTowns(countyName: string) {
  if (!countyName) { townOptions.value = []; return }
  try {
    const res: any = await listTownship({ countyCode: countyName })
    townOptions.value = (res || []).map((t: any) => ({ value: t.townshipName, label: t.townshipName }))
  } catch { townOptions.value = [] }
}

/** 加载烤房列表 + 统计烤房状态 */
async function loadData() {
  loading.value = true
  try {
    // 使用状态中文->英文映射
    const useStatusMap: Record<string, string> = {
      'baking': 'baking',
      'idle': 'idle',
    }
    const dbStatus = filterForm.useStatus ? useStatusMap[filterForm.useStatus] : undefined

    const res: any = await getBarnAssignList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      county: filterForm.countyCode || undefined,
      township: filterForm.townCode || undefined,
      keyword: filterForm.barnName || filterForm.id || undefined,
      status: dbStatus,
      facilityStatus: filterForm.facilityStatus || undefined,
    })
    const rows = res?.rows || []

    // 获取所有烤房数据用于统计（使用与列表相同的筛选条件，包含关键词和状态）
    const statsRes: any = await getBarnAssignList({
      pageNum: 1,
      pageSize: 9999,
      county: filterForm.countyCode || undefined,
      township: filterForm.townCode || undefined,
      keyword: filterForm.barnName || filterForm.id || undefined,
      status: dbStatus,
      facilityStatus: filterForm.facilityStatus || undefined,
    })
    const statsRows = statsRes?.rows || []

    tableData.value = rows.map((r: any) => ({
      ...r,
      // 添加中文使用状态用于显示
      useStatus: r.status === 'baking' ? '在烤' : '空闲',
      barnName: r.barnName || r.id,
    }))
    total.value = res?.total ?? rows.length

    // 实时计算各状态的烤房数量
    // 1. 总烤房数
    stats.total = statsRows.length

    // 2. 使用中
    stats.baking = statsRows.filter((r: any) => r.status === 'baking').length

    // 3. 闲置中
    stats.archived = statsRows.filter((r: any) => r.status === 'idle').length

    // 为每个烤房查询当前有效分配
    const assignRes: any = await getAssignmentList({ pageNum: 1, pageSize: 9999 })
    const allAssigns = assignRes?.rows || []

    // 构建barnAssignmentMap（只包含当前筛选结果中的烤房）
    const barnIds = rows.map((r: any) => r.id).filter(Boolean)
    barnAssignmentMap.value = {}
    for (const bId of barnIds) {
      const active = allAssigns
        .filter((a: any) => (a.ovenId || a.oven_id) === bId)
        .sort((a: any, b: any) => new Date(b.assignedAt || b.assigned_at || b.createTime || b.created_at).getTime() - new Date(a.assignedAt || a.assigned_at || a.createTime || a.created_at).getTime())[0]
      if (active) barnAssignmentMap.value[bId] = active
    }

    // 4. 已分配（有活跃分配记录的烤房数，可能与在烤不同）
    stats.assigned = Object.values(barnAssignmentMap.value).filter(
      (a: any) => a.status === '已分配' || a.status === '使用中'
    ).length
  } catch (error) {
    console.error('加载烤房列表失败:', error)
    ElMessage.error('加载烤房列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }

/** 根据健康分数计算健康等级 */
function calculateHealthLevel(score: number): string {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'maintenance'
  if (score >= 60) return 'urgent'
  return 'retired'
}

/** 设施现状颜色 */
function getFacilityStatusColor(status: string): string {
  if (status === '损坏') return '#D4604A'
  if (status === '闲置') return '#D4944A'
  if (status === '另作他用') return '#8B5CF6'
  return '#2E8B6A'
}

/** 健康分颜色 */
function getHealthScoreColor(score: number | null): string {
  if (score === null || score === undefined) return '#C0C4CC'
  if (score >= 90) return '#2E8B6A'
  if (score >= 75) return '#5FB292'
  if (score >= 60) return '#D4944A'
  if (score >= 40) return '#D4604A'
  return '#B71C1C'
}

/** 剩余寿命颜色 */
function getLifeColor(years: number | null): string {
  if (years === null || years === undefined) return '#C0C4CC'
  if (years <= 1) return '#D4604A'
  if (years <= 3) return '#D4944A'
  if (years <= 5) return '#5FB292'
  return '#2E8B6A'
}

function handleReset() {
  Object.assign(filterForm, { barnName: '', id: '', countyCode: '', townCode: '', useStatus: '', facilityStatus: '' })
  townOptions.value = []
  pagination.pageNum = 1
  loadData()
}
function handlePageChange(page: number) { pagination.pageNum = page; loadData() }
function handleSizeChange(size: number) { pagination.pageSize = size; pagination.pageNum = 1; loadData() }

/** 远程搜索烟农 */
async function remoteSearchFarmer(keyword: string) {
  if (!keyword) { farmerOptions.value = []; return }
  farmerLoading.value = true
  try {
    const res: any = await searchFarmer(keyword)
    farmerOptions.value = Array.isArray(res) ? res : (res?.rows || [])
  } catch { farmerOptions.value = [] } finally { farmerLoading.value = false }
}

/** 选中烟农 */
function handleFarmerSelect(value: any) {
  const farmer = farmerOptions.value.find((f: any) => (f.userId || f.farmerId) === value)
  if (farmer) {
    assignForm.farmerName = farmer.nickName || farmer.userName || farmer.farmerName || ''
    assignForm.farmerPhone = farmer.phone || farmer.farmerPhone || ''
    // 保存详情，用于下方显示填充
    selectedFarmerInfo.value = {
      farmerId: farmer.userId || farmer.farmerId,
      farmerName: assignForm.farmerName,
      farmerPhone: assignForm.farmerPhone,
      creditLevel: farmer.creditLevel || '未评',
      creditScore: farmer.creditScore ?? '-',
      area: farmer.area || '-',
      plantingArea: farmer.plantingArea ?? '-',
      plantingYears: farmer.plantingYears ?? '-',
      totalBakes: farmer.totalBakes ?? 0,
    }
  } else {
    selectedFarmerInfo.value = null
  }
}

/** 判断分配是否已到期 */
function isAssignExpired(assign: any): boolean {
  if (!assign) return false
  const endTime = assign.endTime || assign.end_time
  if (!endTime) return false
  return new Date(endTime.replace(' ', 'T')).getTime() < Date.now()
}

/** 打开分配对话框 */
function openAssignDialog(row: any) {
  Object.assign(assignForm, {
    ovenId: row.id,
    barnName: row.barnName,
    id: row.id,
    farmerId: undefined,
    farmerName: '',
    farmerPhone: '',
    timeRange: [],
    remark: '',
  })
  farmerOptions.value = []
  selectedFarmerInfo.value = null
  assignDialog.value = true
}

function formatDatetime(d: Date | string): string {
  if (!d) return ''
  if (typeof d === 'string') return d.replace('T', ' ')
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 提交分配 */
async function submitAssign() {
  if (!assignForm.farmerId) { ElMessage.warning('请搜索并选择烟农'); return }
  if (!assignForm.timeRange || assignForm.timeRange.length !== 2 || !assignForm.timeRange[0] || !assignForm.timeRange[1]) {
    ElMessage.warning('请选择分配时间'); return
  }
  assignLoading.value = true
  try {
    await addAssignment({
      ovenId: assignForm.ovenId,
      barnName: assignForm.barnName,
      id: assignForm.id,
      farmerId: assignForm.farmerId,
      farmerName: assignForm.farmerName,
      farmerPhone: assignForm.farmerPhone,
      startTime: formatDatetime(assignForm.timeRange[0]),
      endTime: formatDatetime(assignForm.timeRange[1]),
      status: '已分配',
      remark: assignForm.remark,
    })
    ElMessage.success(`烤房「${assignForm.barnName}」已成功分配给「${assignForm.farmerName}」`)
    assignDialog.value = false
    loadData()
    loadStats()
  } finally { assignLoading.value = false }
}

/** 标记为使用中 */
async function handleStartUsing(row: any) {
  const assign = barnAssignmentMap.value[row.id]
  if (!assign) return
  try {
    await ElMessageBox.confirm(`确认烤房「${row.barnName}」已开始使用？`, '开始使用', { type: 'info' })
    await updateAssignment({ id: assign.id, status: '使用中' })
    ElMessage.success('状态已更新为使用中')
    loadData()
    loadStats()
  } catch { /* cancel */ }
}

/** 归还 */
async function handleReturn(row: any) {
  const assign = barnAssignmentMap.value[row.id]
  if (!assign) return
  try {
    await ElMessageBox.confirm(`确认将烤房「${row.barnName}」标记为已归还吗？`, '烤房归还', { type: 'warning' })
    await updateAssignment({ id: assign.id, status: '已归还', returnDate: formatDatetime(new Date()) })
    ElMessage.success('归还成功')
    // 重新加载烤房列表，更新状态和操作按钮
    loadData()
    loadStats()
  } catch { /* cancel */ }
}

/** 查看分配历史 */
async function handleHistory(row: any) {
  historyBarnName.value = row.barnName
  historyDialog.value = true
  historyLoading.value = true
  try {
    const res: any = await getAssignmentsByBarn(row.id)
    historyList.value = Array.isArray(res) ? res : (res?.rows || [])
  } catch { historyList.value = [] } finally { historyLoading.value = false }
}

/** 删除分配记录 */
async function handleDeleteHistory(item: any) {
  try {
    await ElMessageBox.confirm(`确定删除该分配记录吗？`, '提示', { type: 'warning' })
    await deleteAssignment(item.id)
    ElMessage.success('删除成功')
    if (assignForm.ovenId) {
      const res: any = await getAssignmentsByBarn(assignForm.ovenId)
      historyList.value = Array.isArray(res) ? res : (res?.rows || [])
    }
    loadData()
    loadStats()
  } catch { /* cancel */ }
}

// ==================== 烟农详情对话框 ====================

const farmerDialogVisible = ref(false)
const farmerDialogLoading = ref(false)
const farmerDialogTitle = ref('')
const farmerDetail = ref<any>(null)

async function handleViewFarmerDetail(assign: any) {
  farmerDialogTitle.value = `分配详情 - ${assign.farmerName}`
  farmerDialogVisible.value = true
  farmerDialogLoading.value = true
  farmerDetail.value = null
  try {
    // 调用搜索烟农接口获取完整信息（按手机号搜索）
    const res: any = await searchFarmer(assign.farmerPhone)
    const farmerList = Array.isArray(res) ? res : (res?.rows || res?.data || [])
    const farmer = farmerList.find((f: any) => (f.phone || f.farmerPhone) === assign.farmerPhone) || farmerList[0]
    // 映射字段，兼容两种命名
    if (farmer) {
      farmerDetail.value = {
        name: farmer.name || farmer.farmerName || farmer.nickName || '',
        phone: farmer.phone || farmer.farmerPhone || '',
        creditLevel: farmer.creditLevel || '未评定',
        creditScore: farmer.creditScore || 0,
        area: farmer.area || '-',
        plantArea: farmer.plantArea || farmer.plantingArea || 0,
        plantYears: farmer.plantYears || farmer.plantingYears || 0,
        bakingCount: farmer.bakingCount || farmer.totalBakes || 0,
      }
    }
  } catch (error) {
    console.error('查询烟农详情失败:', error)
    ElMessage.error('查询烟农详情失败')
  } finally {
    farmerDialogLoading.value = false
  }
}

/** 编辑烟农信息 */
function handleEditFarmer(farmer: any) {
  farmerDialogVisible.value = false
  // 跳转到烟农管理页面并滚动到该烟农
  router.push({ path: '/system/farmer-manage' })
  // 这里可以添加自动滚动到该烟农的代码
  ElMessage.info('请使用烟农管理页面的搜索功能定位该烟农')
}

/** 自动释放到期分配（静默调用） */
async function callAutoRelease() {
  try {
    await autoReleaseAssignment()
  } catch {
    // 静默失败，不影响页面加载
  }
}

let autoReleaseTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  // 页面加载时先自动释放到期分配
  callAutoRelease()
  loadReservationCount()
  loadStats()
  loadCounties()
  loadData()
  // 每5分钟自动调用一次释放到期分配
  autoReleaseTimer = setInterval(() => {
    callAutoRelease()
  }, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (autoReleaseTimer) {
    clearInterval(autoReleaseTimer)
    autoReleaseTimer = null
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><Connection /></el-icon>
        </div>
        <div class="page-title-text">
          <h2 class="page-title">烤房分配</h2>
          <p class="page-desc">基于烤房列表，定向分配烤房给烟农，管理分配状态与归还</p>
        </div>
      </div>
    </div>

    <!-- 预警窗口：已提交预约申请 -->
    <el-alert
      v-if="!reservationAlertClosed && reservationApplyCount > 0"
      class="warning-alert"
      title="预警提醒"
      type="warning"
      show-icon
      :closable="true"
      @close="reservationAlertClosed = true"
    >
      <div class="warning-alert-msg">
        <span><span class="warning-num">{{ reservationApplyCount }}</span>人已提交预约申请，请及时处理。</span>
      </div>
    </el-alert>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-bar" :style="{ background: card.color }"></div>
        <div class="stat-icon" :style="{ background: card.bg }">
          <component :is="card.icon" :size="18" :color="card.color" />
        </div>
        <div class="stat-body">
          <div class="stat-value" :style="{ color: card.color }">{{ stats[card.key] }}</div>
          <div class="stat-label">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="烤房名称">
          <el-input v-model="filterForm.barnName" placeholder="请输入" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="烤房编号">
          <el-input v-model="filterForm.id" placeholder="请输入" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="县区">
          <el-select v-model="filterForm.countyCode" placeholder="全部" clearable style="width: 120px" @change="handleCountyChange">
            <el-option v-for="c in countyOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="乡镇">
          <el-select v-model="filterForm.townCode" placeholder="请先选县区" clearable style="width: 120px" :disabled="!filterForm.countyCode">
            <el-option v-for="t in townOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用状态">
          <el-select v-model="filterForm.useStatus" placeholder="全部" clearable style="width: 100px">
            <el-option label="在烤" value="baking" />
            <el-option label="空闲" value="idle" />
          </el-select>
        </el-form-item>
        <el-form-item label="设施现状">
          <el-select v-model="filterForm.facilityStatus" placeholder="全部" clearable style="width: 110px">
            <el-option label="正常" value="正常" />
            <el-option label="闲置" value="闲置" />
            <el-option label="损坏" value="损坏" />
            <el-option label="另作他用" value="另作他用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" :row-class-name="({ row }) => row && barnAssignmentMap[row.id] ? 'row-assigned' : ''">
        <el-table-column label="项目编号" width="160" align="center">
          <template #default="{ row }"><BarnCode :code="row.id" /></template>
        </el-table-column>
        <el-table-column prop="county" label="县区" width="90" align="center" />
        <el-table-column prop="township" label="乡镇" width="90" align="center" />
        <el-table-column label="使用状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status || 'idle'" type="useStatus" />
          </template>
        </el-table-column>
        <el-table-column label="设施现状" width="110" align="center">
          <template #default="{ row }">
            <span class="facility-status-text">
              <span class="facility-dot" :style="{ background: getFacilityStatusColor(row.facilityStatus) }"></span>
              <span :style="{ color: getFacilityStatusColor(row.facilityStatus) }">{{ row.facilityStatus || '正常' }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="健康等级" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.healthScore ? calculateHealthLevel(row.healthScore) : 'retired'" type="health" />
          </template>
        </el-table-column>
        <el-table-column label="健康分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.currentHealthScore != null" :style="{ color: getHealthScoreColor(row.currentHealthScore), fontWeight: 600 }">
              {{ Number(row.currentHealthScore).toFixed(1) }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余寿命(年)" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.currentLifeResidual != null" :style="{ color: getLifeColor(row.currentLifeResidual), fontWeight: 600 }">
              {{ Number(row.currentLifeResidual).toFixed(1) }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="当前分配" min-width="200">
          <template #default="{ row }">
            <template v-if="barnAssignmentMap[row.id]">
              <div class="assign-info" @click="handleViewFarmerDetail(barnAssignmentMap[row.id])">
                <div class="assign-info-row">
                  <span class="assign-info-name">{{ barnAssignmentMap[row.id].farmerName }}</span>
                  <StatusTag :status="assignStatusKeyMap[barnAssignmentMap[row.id].status] || ''" type="assignment" />
                  <span v-if="isAssignExpired(barnAssignmentMap[row.id])" class="assign-info-tag tag-expired">到期</span>
                  <span v-if="barnAssignmentMap[row.id].evaluated" class="assign-info-tag tag-evaluated">已评</span>
                </div>
                <div class="assign-info-meta">
                  <span v-if="barnAssignmentMap[row.id].farmerPhone" class="assign-info-phone">
                    <el-icon><Phone /></el-icon>{{ barnAssignmentMap[row.id].farmerPhone.slice(-4) }}
                  </span>
                  <span v-if="barnAssignmentMap[row.id].startTime" class="assign-info-time">
                    {{ (barnAssignmentMap[row.id].startTime || '').slice(5, 10) }}~{{ (barnAssignmentMap[row.id].endTime || '').slice(5, 10) }}
                  </span>
                </div>
              </div>
            </template>
            <span v-else class="assign-info-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btn-group">
              <template v-if="getAssignStatus(row.id) === 'none'">
                <button class="table-action-btn table-action-btn--primary" @click="openAssignDialog(row)">
                  <el-icon><Connection /></el-icon>分配
                </button>
              </template>
              <template v-else>
                <button class="table-action-btn table-action-btn--success" @click="handleStartUsing(row)">
                  <el-icon><VideoPlay /></el-icon>使用
                </button>
                <button class="table-action-btn table-action-btn--warning" @click="handleReturn(row)">
                  <el-icon><RefreshLeft /></el-icon>归还
                </button>
              </template>
              <button class="table-action-btn table-action-btn--info" @click="handleHistory(row)">
                <el-icon><View /></el-icon>历史
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 分配对话框 -->
    <el-dialog v-model="assignDialog" title="分配烤房" width="520px" destroy-on-close append-to-body align-center>
      <div class="dialog-barn-info">
        <span class="label">烤房：</span>
        <span class="value">{{ assignForm.barnName }}</span>
        <BarnCode :code="assignForm.id" />
      </div>
      <el-form :model="assignForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="搜索烟农" required>
          <el-select
            v-model="assignForm.farmerId"
            placeholder="请输入烟农姓名或电话搜索"
            filterable
            remote
            reserve-keyword
            :remote-method="remoteSearchFarmer"
            :loading="farmerLoading"
            style="width: 100%"
            @change="handleFarmerSelect"
          >
            <el-option
              v-for="item in farmerOptions"
              :key="item.userId || item.farmerId"
              :label="`${item.nickName || item.farmerName || item.userName}${item.phone || item.farmerPhone ? ' (' + (item.phone || item.farmerPhone) + ')' : ''}`"
              :value="item.userId || item.farmerId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="烟农姓名">
          <el-input v-model="assignForm.farmerName" placeholder="选中烟农后自动填充" disabled />
        </el-form-item>
        <el-form-item label="烟农电话">
          <el-input v-model="assignForm.farmerPhone" placeholder="选中烟农后自动填充" disabled />
        </el-form-item>
        <!-- 烟农详情填充显示 -->
        <div v-if="selectedFarmerInfo" class="farmer-detail-box">
          <div class="farmer-detail-title">烟农档案信息</div>
          <div class="farmer-detail-grid">
            <div class="farmer-detail-item">
              <span class="farmer-detail-label">信用等级</span>
              <span class="farmer-detail-value level-badge" :class="'level-' + (selectedFarmerInfo.creditLevel || 'D')">{{ selectedFarmerInfo.creditLevel }}</span>
            </div>
            <div class="farmer-detail-item">
              <span class="farmer-detail-label">信用分数</span>
              <span class="farmer-detail-value">{{ selectedFarmerInfo.creditScore }}</span>
            </div>
            <div class="farmer-detail-item">
              <span class="farmer-detail-label">所属区域</span>
              <span class="farmer-detail-value">{{ selectedFarmerInfo.area }}</span>
            </div>
            <div class="farmer-detail-item">
              <span class="farmer-detail-label">种植面积</span>
              <span class="farmer-detail-value">{{ selectedFarmerInfo.plantingArea }} 亩</span>
            </div>
            <div class="farmer-detail-item">
              <span class="farmer-detail-label">种植年限</span>
              <span class="farmer-detail-value">{{ selectedFarmerInfo.plantingYears }} 年</span>
            </div>
            <div class="farmer-detail-item">
              <span class="farmer-detail-label">总烘烤次数</span>
              <span class="farmer-detail-value">{{ selectedFarmerInfo.totalBakes }} 次</span>
            </div>
          </div>
        </div>
        <el-form-item label="分配时间" required>
          <el-date-picker
            v-model="assignForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 烟农详情对话框 -->
    <el-dialog v-model="farmerDialogVisible" :title="farmerDialogTitle" width="600px" v-loading="farmerDialogLoading" append-to-body align-center>
      <div v-if="farmerDetail" class="farmer-detail-content">
        <div class="farmer-detail-header">
          <div class="farmer-name">{{ farmerDetail.name }}</div>
          <div class="farmer-phone"><el-icon><Phone /></el-icon>{{ farmerDetail.phone }}</div>
        </div>
        <div class="farmer-detail-grid">
          <div class="farmer-detail-item">
            <span class="farmer-label">信用等级：</span>
            <span class="farmer-value">{{ farmerDetail.creditLevel || '未评定' }}</span>
          </div>
          <div class="farmer-detail-item">
            <span class="farmer-label">信用分数：</span>
            <span class="farmer-value">{{ farmerDetail.creditScore || 0 }}</span>
          </div>
          <div class="farmer-detail-item">
            <span class="farmer-label">所属区域：</span>
            <span class="farmer-value">{{ farmerDetail.area || '-' }}</span>
          </div>
          <div class="farmer-detail-item">
            <span class="farmer-label">种植面积：</span>
            <span class="farmer-value">{{ farmerDetail.plantArea || 0 }}亩</span>
          </div>
          <div class="farmer-detail-item">
            <span class="farmer-label">种植年限：</span>
            <span class="farmer-value">{{ farmerDetail.plantYears || 0 }}年</span>
          </div>
          <div class="farmer-detail-item">
            <span class="farmer-label">总烘烤次数：</span>
            <span class="farmer-value">{{ farmerDetail.bakingCount || 0 }}次</span>
          </div>
        </div>
        <div class="farmer-detail-footer">
          <el-button type="primary" @click="handleEditFarmer(farmerDetail)">编辑烟农信息</el-button>
        </div>
      </div>
      <div v-else class="farmer-empty">暂无烟农信息</div>
    </el-dialog>

    <!-- 历史对话框 -->
    <el-dialog v-model="historyDialog" :title="`分配历史 - ${historyBarnName}`" width="720px" destroy-on-close append-to-body align-center>
      <el-table :data="historyList" v-loading="historyLoading" style="width: 100%" max-height="360">
        <el-table-column prop="farmerName" label="烟农" width="90" align="center" />
        <el-table-column prop="farmerPhone" label="电话" width="120" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <StatusTag :status="assignStatusKeyMap[row.status] || ''" type="assignment" />
          </template>
        </el-table-column>
        <el-table-column label="分配日期" width="150" align="center">
          <template #default="{ row }">{{ parseTime(row.assignDate).slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="分配时间段" width="280" align="center">
          <template #default="{ row }">
            <span v-if="row.startTime || row.endTime">
              {{ parseTime(row.startTime).slice(0, 16) }} ~ {{ parseTime(row.endTime).slice(0, 16) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="归还日期" width="150" align="center">
          <template #default="{ row }">{{ row.returnDate ? parseTime(row.returnDate).slice(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column label="评价" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.evaluated" type="success" size="small" effect="light">已评价</el-tag>
            <el-tag v-else-if="row.status === '已归还'" type="warning" size="small" effect="plain">待评价</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDeleteHistory(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="historyDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100%;
  padding: 11px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ===== 页面标题 ===== */
.page-header {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.75) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 8px 11px;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
}

.page-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 7px;
}

.page-title-icon {
  width: clamp(36px, 3vw, 44px);
  height: clamp(36px, 3vw, 44px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 50%, #1A6B4F 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 3px 8px rgba(10, 77, 62, 0.35);
  flex-shrink: 0;
}

.page-title-text {
  flex: 1;
}

.page-title {
  font-size: clamp(18px, 1.8vw, 24px);
  font-weight: 700;
  color: #0D3D30;
  margin: 0;
}

.page-desc {
  font-size: clamp(12px, 1vw, 14px);
  color: #5A8A7A;
  margin: 4px 0 0;
}

/* ===== 预警窗口 ===== */
.warning-alert {
  border-radius: 8px;
  box-shadow: 0 3px 12px rgba(212, 100, 74, 0.15);
}

.warning-alert-msg {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 13px;
  color: #6B7280;
}

.warning-alert-msg .warning-num {
  font-weight: 700;
  color: #D4604A;
  padding: 0 2px;
}

/* ===== 统计卡片（图标圆角方块 + 左侧竖条 + 多色区分） ===== */
.stats-row {
  display: flex;
  gap: 7px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 10px 14px;
  background: #ffffff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.25s;
  overflow: hidden;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 16px rgba(0, 0, 0, 0.1);
}

.stat-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  flex-shrink: 0;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
  margin-top: 2px;
}

/* ===== 筛选区 ===== */
.filter-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 7px 8px 0;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
}

.filter-card :deep(.el-form-item) {
  margin-right: 14px;
  margin-bottom: 7px;
}

/* ===== 表格区 ===== */
.table-card {
  flex: 1;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 7px;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
  display: flex;
  flex-direction: column;
}

.table-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(26, 107, 79, 0.06);
}

.table-card :deep(.el-table__row:hover > td) {
  background: rgba(26, 107, 79, 0.04) !important;
}

.table-card :deep(.row-assigned) {
  background: rgba(46, 139, 106, 0.03);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

/* ===== 状态标签 ===== */
.status-pill {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 8px;
  font-size: clamp(11px, 0.9vw, 13px);
  font-weight: 500;
}

.status--inUse { background: #E8F5EE; color: #2E8B6A; }
.status--idle { background: #F0F7F3; color: #5A8A7A; }
.status--transferred { background: #FEF3E7; color: #D4944A; }
.status--damaged { background: #FEF2F2; color: #D4604A; }

.level-tag {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 8px;
  font-size: clamp(11px, 0.9vw, 13px);
  font-weight: 500;
}

.level--excellent { background: #E8F5EE; color: #2E8B6A; }
.level--maintenance { background: #FEF3E7; color: #D4944A; }
.level--urgent { background: #FEF2F2; color: #D4604A; }

/* ===== 对话框 ===== */
.dialog-barn-info {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 14px;
  background: rgba(26, 107, 79, 0.06);
  border-radius: 8px;
}

.dialog-barn-info .label {
  font-size: clamp(12px, 1vw, 14px);
  color: #5A8A7A;
}

.dialog-barn-info .value {
  font-size: clamp(14px, 1.2vw, 16px);
  font-weight: 700;
  color: #0D5D46;
}

/* ===== 烟农详情填充显示 ===== */
.farmer-detail-box {
  margin: 8px 0 16px 0;
  padding: 14px 16px;
  background: linear-gradient(135deg, #f0faf6 0%, #e8f5ef 100%);
  border: 1px solid #c8e6d5;
  border-radius: 8px;
}
.farmer-detail-title {
  font-size: 13px;
  font-weight: 700;
  color: #0D5D46;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #b0d9c4;
}
.farmer-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px 16px;
}
.farmer-detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.farmer-detail-label {
  font-size: 12px;
  color: #6b9b8a;
}
.farmer-detail-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f3a32;
}
.level-badge {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 700;
  width: fit-content;
}
.level-A {
  background: #e6f7ed;
  color: #16a34a;
}
.level-B {
  background: #fff7e6;
  color: #d97706;
}
.level-C {
  background: #ffe6e6;
  color: #dc2626;
}
.level-D {
  background: #f3e8ff;
  color: #7c3aed;
}
@media (max-width: 768px) {
  .farmer-detail-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .stats-row {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
  }
}

@media (max-width: 600px) {
  .stat-card {
    flex: 1 1 100%;
  }
}

/* ===== 分配信息（简洁内联布局） ===== */
.assign-info {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 5px;
  transition: background 0.2s ease;
}

.assign-info:hover {
  background: rgba(26, 107, 79, 0.05);
}

.assign-info-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.assign-info-name {
  font-size: 13px;
  font-weight: 700;
  color: #1A6B4F;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
}

.assign-info-tag {
  display: inline-block;
  padding: 0 5px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  white-space: nowrap;
}

.tag-expired {
  background: #FEF2F2;
  color: #D4604A;
}

.tag-evaluated {
  background: #F0FDF4;
  color: #1A6B4F;
}

.assign-info-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 2px;
}

.assign-info-phone {
  font-size: 11px;
  color: #6B7280;
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.assign-info-time {
  font-size: 11px;
  color: #94A3B8;
  white-space: nowrap;
}

.assign-info-empty {
  color: #C0C4CC;
  font-size: 14px;
}

/* ===== 烟农详情对话框样式 ===== */
.farmer-detail-content {
  padding: 10px 0;
}

.farmer-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #E1E8E5;
}

.farmer-name {
  font-size: 18px;
  font-weight: 700;
  color: #1A6B4F;
}

.farmer-phone {
  font-size: 14px;
  color: #6B7280;
  display: flex;
  align-items: center;
  gap: 4px;
}

.farmer-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.farmer-detail-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  line-height: 1.6;
}

.farmer-label {
  color: #6B7280;
  font-weight: 500;
  min-width: 85px;
}

.farmer-value {
  color: #1F2937;
  font-weight: 600;
  flex: 1;
}

.farmer-detail-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 15px;
  border-top: 1px solid #E1E8E5;
}

.farmer-empty {
  text-align: center;
  padding: 40px 20px;
  color: #9CA3AF;
  font-size: 14px;
}

/* ===== 表格操作按钮（与维修列表统一风格） ===== */
.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.table-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 5px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  background: linear-gradient(145deg, #ffffff, #f0f0f0);
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  white-space: nowrap;
}

.table-action-btn:hover {
  transform: translateY(-1px);
  box-shadow:
    0 4px 10px rgba(0, 0, 0, 0.1),
    0 2px 3px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.table-action-btn:active {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.1),
    inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.table-action-btn--primary { color: #0D5D46; }
.table-action-btn--success { color: #2E8B6A; }
.table-action-btn--warning { color: #D4944A; }
.table-action-btn--danger  { color: #D4604A; }
.table-action-btn--info    { color: #6B7280; }

/* 表格内取消烤房编号的方框样式 */
.table-card :deep(.barn-code) {
  background: none;
  border: none;
  padding: 0;
  border-radius: 0;
}

/* 设施现状文字+圆点样式（与使用状态色块区分） */
.facility-status-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
}

.facility-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  display: inline-block;
}
</style>