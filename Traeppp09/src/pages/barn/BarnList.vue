<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, Delete, Connection, RefreshLeft, CircleClose, Plus, Download, Location, Edit } from '@element-plus/icons-vue'
import { Factory, Flame, PauseCircle, HeartPulse, AlertTriangle, Building2 } from 'lucide-vue-next'
import StatusTag from '@/components/StatusTag.vue'
import BarnCode from '@/components/BarnCode.vue'
import { getBarnAssignList, getBarnFullProfile, getBarnProjectStats, updateBarnProject } from '@/api/barn'
import { getRepairList } from '@/api/repair'
import { listCounty, listTownship } from '@/api/dict'
import { thumbUrl } from '@/utils/image'

/** 烤房使用状态 → StatusTag key */
const useStatusKeyMap: Record<string, string> = {
  '在烤': 'baking',
  '空闲': 'idle',
}
const healthLevelKeyMap: Record<string, string> = {
  '优良': 'excellent',
  '良好': 'good',
  '一般': 'fair',
  '较差': 'poor',
  '需维护': 'maintenance',
  '急需修复': 'urgent',
  '退出': 'retired',
}

/** 使用状态选项（英文值） */
const useStatusOptions = [
  { value: 'baking', label: '在烤' },
  { value: 'idle', label: '空闲' },
]

/** 健康等级选项（英文值） */
const healthOptions = [
  { value: 'excellent', label: '优良' },
  { value: 'good', label: '良好' },
  { value: 'fair', label: '一般' },
  { value: 'poor', label: '较差' },
  { value: 'maintenance', label: '需维护' },
  { value: 'urgent', label: '急需修复' },
  { value: 'retired', label: '退出' },
]

/** 统计卡片配置 */
const statCards = [
  { key: 'total', title: '总烤房数', icon: Building2, color: '#0D5D46', bg: '#E0F2EC' },
  { key: 'baking', title: '在烤', icon: Flame, color: '#2E8B6A', bg: '#E8F5EE' },
  { key: 'idle', title: '空闲', icon: PauseCircle, color: '#D4944A', bg: '#FEF3E7' },
  { key: 'excellent', title: '优良', icon: HeartPulse, color: '#1565C0', bg: '#E3F2FD' },
  { key: 'poor', title: '较差', icon: AlertTriangle, color: '#D4604A', bg: '#FDECEA' },
  { key: 'damaged', title: '损毁', icon: Factory, color: '#8B5CF6', bg: '#F3EEFE' },
] as const

const stats = reactive({ total: 0, baking: 0, idle: 0, excellent: 0, poor: 0, damaged: 0, lifeZero: 0 })

/** 筛选表单 */
const filterForm = reactive({
  barnName: '',
  id: '',
  countyCode: '',
  townCode: '',
  useStatus: '',
  healthLevel: '',
  facilityStatus: '',
})

const countyOptions = ref<{ value: string; label: string }[]>([])
const townOptions = ref<{ value: string; label: string }[]>([])

/** 烤房表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({ pageNum: 1, pageSize: 10 })

/** 预警窗口关闭状态 */
const alertClosed = ref(false)

/** 加载县区 */
async function loadCounties() {
  try {
    const res: any = await listCounty()
    // value 用 countyCode（用于联动乡镇），label 用 countyName（用于显示）
    countyOptions.value = (res || []).map((c: any) => ({ value: c.countyCode, label: c.countyName }))
  } catch { /* ignore */ }
}

/** 县区变化时联动加载乡镇 */
function handleCountyChange(countyCode: string) {
  filterForm.townCode = ''
  if (countyCode) {
    loadTowns(countyCode)
  } else {
    townOptions.value = []
  }
}

/** 加载乡镇 */
async function loadTowns(countyCode: string) {
  if (!countyCode) { townOptions.value = []; return }
  try {
    const res: any = await listTownship(countyCode)
    // value 用 townshipName（后端 kf_basedata 表 township 字段存的是乡镇名）
    townOptions.value = (res || []).map((t: any) => ({ value: t.townshipName, label: t.townshipName }))
  } catch { townOptions.value = [] }
}

/** 根据 countyCode 获取 countyName（列表筛选需要县名） */
function getCountyName(countyCode: string): string {
  const c = countyOptions.value.find(o => o.value === countyCode)
  return c ? c.label : ''
}

/** 加载统计数据 - 基于当前筛选条件动态计算指标卡 */
async function loadStats() {
  try {
    // 使用与列表相同的筛选条件获取全部匹配数据，计算指标卡
    const res: any = await getBarnAssignList({
      pageNum: 1,
      pageSize: 9999,
      county: getCountyName(filterForm.countyCode) || undefined,
      township: filterForm.townCode || undefined,
      keyword: filterForm.barnName || filterForm.id || undefined,
      status: filterForm.useStatus || undefined,
      healthLevel: filterForm.healthLevel || undefined,
      facilityStatus: filterForm.facilityStatus || undefined,
    })
    const rows = res?.rows || []
    stats.total = rows.length
    stats.baking = rows.filter((r: any) => r.status === 'baking').length
    stats.idle = rows.filter((r: any) => r.status !== 'baking').length
    // 健康等级统计
    stats.excellent = rows.filter((r: any) => {
      const score = Number(r.currentHealthScore ?? r.healthScore ?? 0)
      return score >= 85
    }).length
    stats.poor = rows.filter((r: any) => {
      const score = Number(r.currentHealthScore ?? r.healthScore ?? 0)
      return score > 0 && score < 50
    }).length
    // 损毁统计
    stats.damaged = rows.filter((r: any) => r.facilityStatus === '损坏').length
    // 剩余寿命为0的烤房
    stats.lifeZero = rows.filter((r: any) => {
      const life = r.currentLifeResidual
      return life != null && Number(life) <= 0
    }).length
  } catch { /* ignore */ }
}

/** 加载烤房列表 */
async function loadData() {
  loading.value = true
  try {
    const res: any = await getBarnAssignList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      county: getCountyName(filterForm.countyCode) || undefined,
      township: filterForm.townCode || undefined,
      keyword: filterForm.barnName || filterForm.id || undefined,
      status: filterForm.useStatus || undefined,
      healthLevel: filterForm.healthLevel || undefined,
      facilityStatus: filterForm.facilityStatus || undefined,
    })
    const rows = res?.rows || []
    tableData.value = rows.map((r: any) => ({
      ...r,
      // 添加中文使用状态用于显示
      useStatus: r.status === 'baking' ? '在烤' : '空闲',
      barnName: r.barnName || r.id,
    }))
    total.value = res?.total ?? rows.length
  } catch (error) {
    console.error('加载烤房列表失败:', error)
    ElMessage.error('加载烤房列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.pageNum = 1; loadData(); loadStats(); }

function handleReset() {
  Object.assign(filterForm, { barnName: '', id: '', countyCode: '', townCode: '', useStatus: '', healthLevel: '', facilityStatus: '' })
  townOptions.value = []
  pagination.pageNum = 1
  loadData()
  loadStats()
}

function handlePageChange(page: number) { pagination.pageNum = page; loadData(); }

/** ===== 维修记录（详情面板中使用） ===== */
const detailRepairList = ref<any[]>([])
const detailRepairLoading = ref(false)

/** 加载某烤房的维修记录（详情面板中使用） */
async function loadDetailRepairRecords(ovenId: string) {
  detailRepairLoading.value = true
  try {
    const res: any = await getRepairList({ ovenId, pageNum: 1, pageSize: 9999 })
    detailRepairList.value = res?.rows || []
  } catch {
    detailRepairList.value = []
  } finally {
    detailRepairLoading.value = false
  }
}

/** 格式化金额（与维修列表保持一致） */
function formatRepairCost(val: any): string {
  return val != null && val !== '' ? `¥${Number(val).toLocaleString()}` : '-'
}

/** 根据健康分数计算健康等级 */
function calculateHealthLevel(score: number): string {
  if (score >= 85) return 'excellent'
  if (score >= 70) return 'good'
  if (score >= 50) return 'fair'
  if (score > 0) return 'poor'
  return 'retired'
}

/** 设施现状颜色 */
function getFacilityStatusColor(status: string): string {
  if (status === '损坏') return '#D4604A'
  if (status === '闲置') return '#D4944A'
  if (status === '另作他用') return '#8B5CF6'
  return '#2E8B6A' // 正常
}

/** 健康分颜色 */
function getHealthScoreColor(score: number): string {
  if (score >= 85) return '#2E8B6A'
  if (score >= 70) return '#D4944A'
  if (score >= 50) return '#D4604A'
  return '#94A3B8'
}

/** 剩余寿命颜色 */
function getLifeColor(life: number): string {
  if (life <= 1) return '#D4604A'
  if (life <= 3) return '#D4944A'
  if (life <= 5) return '#5A8A7A'
  return '#2E8B6A'
}

/** 新建烤房 */
function handleAddBarn() {
  ElMessage.info('新建烤房功能开发中...')
}

/** 导出数据 */
function handleExport() {
  ElMessage.info('导出数据功能开发中...')
}

/** 查看地图 */
function handleMapView() {
  ElMessage.info('地图列表功能开发中...')
}

/** 健康等级统计 */
function handleHealthStats() {
  ElMessage.info('健康等级统计功能开发中...')
}

/** 县区分布 */
function handleCountyStats() {
  ElMessage.info('县区分布功能开发中...')
}

/** 项目类型分布 */
function handleProjectTypeStats() {
  ElMessage.info('项目类型分布功能开发中...')
}

/** 查看详情 */
const detailDialog = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>({})
const detailActiveTab = ref('base')

async function handleViewDetail(row: any) {
  detailDialog.value = true
  detailLoading.value = true
  detailActiveTab.value = 'base'
  detailRepairList.value = []
  try {
    const res: any = await getBarnFullProfile(row.projectId || row.id)
    detailData.value = res || {}
    // 同时加载维修记录
    loadDetailRepairRecords(row.projectId || row.id)
  } catch (e) {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

/** 状态标签类型 */
function statusTagType(status: string): string {
  if (status === '正常') return 'success'
  if (status === '缺失') return 'danger'
  if (status === '损坏') return 'warning'
  return 'info'
}

/** 收集非空照片URL，用于 el-image 预览列表 */
function photoList(...urls: (string | null | undefined)[]): string[] {
  return urls.filter((v): v is string => !!v && String(v).trim() !== '')
}

/** SR 散热器部件表格数据（4个部件：散热管/炉膛/清灰门/烟囱） */
const srTableData = computed(() => {
  const sr = detailData.value.SR
  if (!sr) return []
  return [
    { name: '散热管', status: sr.pipe_status, year: sr.pipe_repair_year, damaged: sr.pipe_photo_damaged, detail: sr.pipe_photo_detail, missing: sr.pipe_photo_missing },
    { name: '炉膛', status: sr.furnace_status, year: sr.furnace_repair_year, damaged: sr.furnace_photo_damaged, detail: sr.furnace_photo_detail, missing: sr.furnace_photo_missing },
    { name: '清灰门', status: sr.ash_door_status, year: sr.ash_door_repair_year, damaged: sr.ash_door_photo_damaged, detail: sr.ash_door_photo_detail, missing: sr.ash_door_photo_missing },
    { name: '烟囱', status: sr.chimney_status, year: sr.chimney_repair_year, damaged: sr.chimney_photo_damaged, detail: sr.chimney_photo_detail, missing: sr.chimney_photo_missing },
  ]
})

/** ZK 自控设备部件表格数据（8个子部件） */
const zkTableData = computed(() => {
  const zk = detailData.value.ZK
  if (!zk) return []
  return [
    { name: '冷风门', status: zk.cold_door_status, year: zk.cold_door_repair_year, normal: zk.cold_door_photo_normal, damaged: zk.cold_door_photo_damaged, detail: zk.cold_door_photo_detail, missing: zk.cold_door_photo_missing },
    { name: '冷风门电动机', status: zk.cold_door_motor_status, year: zk.cold_door_motor_repair_year, normal: zk.cold_door_motor_photo_normal, damaged: zk.cold_door_motor_photo_damaged, detail: zk.cold_door_motor_photo_detail, missing: zk.cold_door_motor_photo_missing },
    { name: '排湿窗', status: zk.exhaust_window_status, year: zk.exhaust_window_repair_year, normal: zk.exhaust_window_photo_normal, damaged: zk.exhaust_window_photo_damaged, detail: zk.exhaust_window_photo_detail, missing: zk.exhaust_window_photo_missing },
    { name: '循环风机', status: zk.circulation_fan_status, year: zk.circulation_fan_repair_year, normal: zk.circulation_fan_photo_normal, damaged: zk.circulation_fan_photo_damaged, detail: zk.circulation_fan_photo_detail, missing: zk.circulation_fan_photo_missing },
    { name: '助燃鼓风机', status: zk.combustion_fan_status, year: zk.combustion_fan_repair_year, normal: zk.combustion_fan_photo_normal, damaged: zk.combustion_fan_photo_damaged, detail: zk.combustion_fan_photo_detail, missing: zk.combustion_fan_photo_missing },
    { name: '操作箱', status: zk.control_box_status, year: zk.control_box_repair_year, normal: zk.control_box_photo_normal, damaged: zk.control_box_photo_damaged, detail: zk.control_box_photo_detail, missing: zk.control_box_photo_missing },
    { name: '水壶', status: zk.water_pot_status, year: zk.water_pot_repair_year, normal: zk.water_pot_photo_normal, damaged: zk.water_pot_photo_damaged, detail: zk.water_pot_photo_detail, missing: zk.water_pot_photo_missing },
    { name: '探头', status: zk.probe_status, year: zk.probe_repair_year, normal: zk.probe_photo_normal, damaged: zk.probe_photo_damaged, detail: zk.probe_photo_detail, missing: zk.probe_photo_missing },
  ]
})

/** ZT 烤房主体部件表格数据（3个部件：墙体屋顶/挂烟梁柱/装烟室门） */
const ztTableData = computed(() => {
  const zt = detailData.value.ZT
  if (!zt) return []
  return [
    { name: '墙体屋顶', status: zt.wall_roof_status, year: zt.wall_roof_repair_year, normal: zt.wall_roof_photo_normal, damaged: zt.wall_roof_photo_damaged, detail: zt.wall_roof_photo_detail, missing: zt.wall_roof_photo_missing },
    { name: '挂烟梁柱', status: zt.beam_status, year: zt.beam_repair_year, normal: zt.beam_photo_normal, damaged: zt.beam_photo_damaged, detail: zt.beam_photo_detail, missing: zt.beam_photo_missing },
    { name: '装烟室门', status: zt.door_status, year: zt.door_repair_year, normal: zt.door_photo_normal, damaged: zt.door_photo_damaged, detail: zt.door_photo_detail, missing: zt.door_photo_missing },
  ]
})

/** FS 附属设施部件表格数据（3个部件：烟夹/烟棚/电路电器） */
const fsTableData = computed(() => {
  const fs = detailData.value.FS
  if (!fs) return []
  return [
    { name: '烟夹', status: fs.clip_status, year: null, normal: null, damaged: null, detail: null, missing: null },
    { name: '烟棚', status: fs.shed_status, year: fs.shed_repair_year, normal: fs.shed_photo_normal, damaged: fs.shed_photo_damaged, detail: fs.shed_photo_detail, missing: fs.shed_photo_missing },
    { name: '电路电器', status: fs.electrical_status, year: fs.electrical_repair_year, normal: fs.electrical_photo_normal, damaged: fs.electrical_photo_damaged, detail: fs.electrical_photo_detail, missing: fs.electrical_photo_missing },
  ]
})

/** 编辑烤房 */
const editDialog = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: '', barnName: '', projectType: '', buildMethod: '', useStatus: '',
  address: '', latitude: '', longitude: '', altitude: '',
  technician: '', projectCost: '', healthScore: 85, healthLevel: '',
  projectOwner: '', constructionUnit: '', description: '',
  facilityStatus: '',
})

function handleEdit(row: any) {
  editForm.id = row.id || row.projectId || ''
  editForm.barnName = row.barnName || ''
  editForm.projectType = row.projectType || ''
  editForm.buildMethod = row.buildMethod || ''
  editForm.useStatus = row.useStatus || row.useStatus || ''
  editForm.address = row.address || row.detailAddress || ''
  editForm.latitude = row.latitude || ''
  editForm.longitude = row.longitude || ''
  editForm.altitude = row.altitude || ''
  editForm.technician = row.technician || ''
  editForm.projectCost = row.projectCost || ''
  editForm.healthScore = row.healthScore || 85
  editForm.healthLevel = row.healthLevel || ''
  editForm.projectOwner = row.projectOwner || ''
  editForm.constructionUnit = row.constructionUnit || ''
  editForm.description = row.description || ''
  editForm.facilityStatus = row.facilityStatus || '正常'
  editDialog.value = true
}

async function handleEditSubmit() {
  editLoading.value = true
  try {
    await updateBarnProject(editForm)
    ElMessage.success('更新成功')
    editDialog.value = false
    loadData()
    loadStats()
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    editLoading.value = false
  }
}

/** 详情状态保存 */
const detailSaveLoading = ref(false)

async function handleSaveDetailStatus() {
  if (!detailData.value?.baseInfo?.project_id) return
  detailSaveLoading.value = true
  try {
    await updateBarnProject({
      id: detailData.value.baseInfo.project_id,
      useStatus: detailData.value.baseInfo.use_status,
      facilityStatus: detailData.value.baseInfo.facility_status || '正常',
    })
    ElMessage.success('状态更新成功')
    loadData()
    loadStats()
  } catch (e) {
    ElMessage.error('状态更新失败')
  } finally {
    detailSaveLoading.value = false
  }
}

function handleDetailStatusChange() { /* 自动保存由按钮触发 */ }
function handleDetailFacilityChange() { /* 自动保存由按钮触发 */ }

onMounted(() => {
  loadCounties()
  loadData()
  loadStats()
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
          <h2 class="page-title">烤房列表</h2>
          <p class="page-desc">查看和管理所有烤房，筛选状态和健康等级，导出数据</p>
        </div>
      </div>
    </div>

    <!-- 预警窗口：健康等级较差 / 剩余寿命为0 -->
    <el-alert
      v-if="!alertClosed && (stats.poor > 0 || stats.lifeZero > 0)"
      class="warning-alert"
      title="预警提醒"
      type="warning"
      show-icon
      :closable="true"
      @close="alertClosed = true"
    >
      <div class="warning-alert-msg">
        <span>有<span class="warning-num">{{ stats.poor }}</span>个烤房健康等级为较差，</span>
        <span>有<span class="warning-num">{{ stats.lifeZero }}</span>个烤房剩余寿命为0，请及时处理。</span>
      </div>
    </el-alert>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-bar" :style="{ background: card.color }"></div>
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <component :is="card.icon" :size="18" />
        </div>
        <div class="stat-info">
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
          <el-select v-model="filterForm.townCode" :placeholder="filterForm.countyCode ? '全部' : '请先选县区'" clearable style="width: 120px" :disabled="!filterForm.countyCode">
            <el-option v-for="t in townOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用状态">
          <el-select v-model="filterForm.useStatus" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="opt in useStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="健康等级">
          <el-select v-model="filterForm.healthLevel" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="opt in healthOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
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

    <!-- 操作按钮区 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="handleAddBarn">新建烤房</el-button>
      <el-button type="success" :icon="Download" @click="handleExport">导出数据</el-button>
      <el-button type="warning" :icon="Location" @click="handleMapView">地图列表</el-button>
    </div>

    <!-- 表格区 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" size="small" row-key="id">
        <el-table-column label="项目编号" width="140" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <BarnCode :code="row.id" />
          </template>
        </el-table-column>
        <el-table-column prop="county" label="县区" width="60" align="center" />
        <el-table-column prop="township" label="乡镇" width="60" align="center" />
        <el-table-column label="使用状态" width="70" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status || 'idle'" type="useStatus" />
          </template>
        </el-table-column>
        <el-table-column label="设施现状" width="75" align="center">
          <template #default="{ row }">
            <span class="facility-status-text">
              <span class="facility-dot" :style="{ background: getFacilityStatusColor(row.facilityStatus) }"></span>
              <span :style="{ color: getFacilityStatusColor(row.facilityStatus) }">{{ row.facilityStatus || '正常' }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="健康等级" width="75" align="center">
          <template #default="{ row }">
            <StatusTag :status="calculateHealthLevel(Number(row.currentHealthScore ?? row.healthScore ?? 0))" type="health" />
          </template>
        </el-table-column>
        <el-table-column label="健康分" width="60" align="center">
          <template #default="{ row }">
            <span v-if="row.currentHealthScore != null" :style="{ color: getHealthScoreColor(row.currentHealthScore), fontWeight: 600 }">
              {{ row.currentHealthScore }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余寿命" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.currentLifeResidual != null" :style="{ color: getLifeColor(row.currentLifeResidual), fontWeight: 600 }">
              {{ row.currentLifeResidual }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="120" show-overflow-tooltip />
        <el-table-column prop="projectType" label="项目类型" width="90" align="center" show-overflow-tooltip />
        <el-table-column prop="technician" label="技术员" width="60" align="center" />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button type="warning" size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
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
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 编辑烤房弹窗 -->
    <el-dialog v-model="editDialog" title="编辑烤房信息" width="700px" destroy-on-close append-to-body align-center>
      <el-form :model="editForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="烤房编号"><el-input v-model="editForm.id" disabled /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="烤房名称"><el-input v-model="editForm.barnName" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目类型"><el-input v-model="editForm.projectType" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建设方式"><el-input v-model="editForm.buildMethod" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用状态">
              <el-select v-model="editForm.useStatus" placeholder="请选择">
                <el-option label="在烤" value="在烤" />
                <el-option label="空闲" value="空闲" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设施现状">
              <el-select v-model="editForm.facilityStatus" placeholder="请选择">
                <el-option label="正常" value="正常" />
                <el-option label="闲置" value="闲置" />
                <el-option label="损坏" value="损坏" />
                <el-option label="另作他用" value="另作他用" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="健康分数"><el-input-number v-model="editForm.healthScore" :min="0" :max="100" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址"><el-input v-model="editForm.address" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="经度"><el-input v-model="editForm.longitude" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="纬度"><el-input v-model="editForm.latitude" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="海拔"><el-input v-model="editForm.altitude" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目业主"><el-input v-model="editForm.projectOwner" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="施工单位"><el-input v-model="editForm.constructionUnit" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input type="textarea" v-model="editForm.description" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 烤房详情弹窗 -->
    <el-dialog v-model="detailDialog" title="烤房完整档案" width="90%" destroy-on-close class="detail-dialog" append-to-body align-center>
      <div v-loading="detailLoading" class="detail-body">
        <el-tabs v-model="detailActiveTab" class="detail-tabs">
          <!-- 基础信息 -->
          <el-tab-pane label="基础信息" name="base">
            <el-descriptions :column="3" border v-if="detailData.baseInfo">
              <el-descriptions-item label="项目编号">{{ detailData.baseInfo.project_id }}</el-descriptions-item>
              <el-descriptions-item label="使用状态">{{ detailData.baseInfo.use_status }}</el-descriptions-item>
              <el-descriptions-item label="设施现状">{{ detailData.baseInfo.facility_status || '正常' }}</el-descriptions-item>
              <el-descriptions-item label="建设方式">{{ detailData.baseInfo.build_method }}</el-descriptions-item>
              <el-descriptions-item label="项目类型">{{ detailData.baseInfo.project_type }}</el-descriptions-item>
              <el-descriptions-item label="长×宽×高">{{ detailData.baseInfo.oven_length }}×{{ detailData.baseInfo.oven_width }}×{{ detailData.baseInfo.oven_height }}米</el-descriptions-item>
              <el-descriptions-item label="工程造价">{{ detailData.baseInfo.project_cost }}元</el-descriptions-item>
              <el-descriptions-item label="国家局补贴">{{ detailData.baseInfo.natl_subsidy }}元</el-descriptions-item>
              <el-descriptions-item label="产区补贴">{{ detailData.baseInfo.local_subsidy }}元</el-descriptions-item>
              <el-descriptions-item label="补贴总额">{{ detailData.baseInfo.total_subsidy }}元</el-descriptions-item>
              <el-descriptions-item label="开工时间">{{ detailData.baseInfo.start_date }}</el-descriptions-item>
              <el-descriptions-item label="竣工时间">{{ detailData.baseInfo.finish_date }}</el-descriptions-item>
              <el-descriptions-item label="海拔">{{ detailData.baseInfo.altitude }}米</el-descriptions-item>
              <el-descriptions-item label="经度">{{ detailData.baseInfo.longitude }}</el-descriptions-item>
              <el-descriptions-item label="纬度">{{ detailData.baseInfo.latitude }}</el-descriptions-item>
              <el-descriptions-item label="技术员">{{ detailData.baseInfo.technician_name }}</el-descriptions-item>
              <el-descriptions-item label="市">{{ detailData.baseInfo.city }}</el-descriptions-item>
              <el-descriptions-item label="县(市、区)">{{ detailData.baseInfo.county }}</el-descriptions-item>
              <el-descriptions-item label="乡(镇)">{{ detailData.baseInfo.township }}</el-descriptions-item>
              <el-descriptions-item label="村">{{ detailData.baseInfo.village }}</el-descriptions-item>
              <el-descriptions-item label="详细地址" :span="3">{{ detailData.baseInfo.detail_address }}</el-descriptions-item>
              <el-descriptions-item label="项目业主">{{ detailData.baseInfo.project_owner }}</el-descriptions-item>
              <el-descriptions-item label="施工单位">{{ detailData.baseInfo.build_company }}</el-descriptions-item>
            </el-descriptions>
            <template v-if="detailData.baseInfo">
              <el-divider content-position="left">状态管理（可修改）</el-divider>
              <div style="display: flex; gap: 16px; align-items: center; margin-bottom: 16px;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="font-weight: 600; white-space: nowrap;">使用状态:</span>
                  <el-select v-model="detailData.baseInfo.use_status" style="width: 120px" @change="handleDetailStatusChange">
                    <el-option label="在烤" value="baking" />
                    <el-option label="空闲" value="idle" />
                  </el-select>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="font-weight: 600; white-space: nowrap;">设施现状:</span>
                  <el-select v-model="detailData.baseInfo.facility_status" style="width: 140px" @change="handleDetailFacilityChange">
                    <el-option label="正常" value="正常" />
                    <el-option label="闲置" value="闲置" />
                    <el-option label="损坏" value="损坏" />
                    <el-option label="另作他用" value="另作他用" />
                  </el-select>
                </div>
                <el-button type="success" size="small" :loading="detailSaveLoading" @click="handleSaveDetailStatus">保存</el-button>
              </div>
            </template>
          </el-tab-pane>

          <!-- JR 加热设备 -->
          <el-tab-pane label="JR 加热设备" name="JR">
            <template v-if="detailData.JR">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="加热设备编码">{{ detailData.JR.heater_code }}</el-descriptions-item>
                <el-descriptions-item label="加热设备类型">{{ detailData.JR.heater_type }}</el-descriptions-item>
                <el-descriptions-item label="燃烧机编码">{{ detailData.JR.burner_code }}</el-descriptions-item>
                <el-descriptions-item label="燃烧机状态">
                  <el-tag :type="statusTagType(detailData.JR.burner_status)">{{ detailData.JR.burner_status || '无数据' }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="修复年度">{{ detailData.JR.burner_repair_year }}</el-descriptions-item>
                <el-descriptions-item label="年度">{{ detailData.JR.build_year }}</el-descriptions-item>
                <el-descriptions-item label="工程造价">{{ detailData.JR.project_cost }}元</el-descriptions-item>
                <el-descriptions-item label="补贴总额">{{ detailData.JR.subsidy_total }}元</el-descriptions-item>
                <el-descriptions-item label="国家局补贴">{{ detailData.JR.subsidy_national }}元</el-descriptions-item>
                <el-descriptions-item label="产区补贴">{{ detailData.JR.subsidy_region }}元</el-descriptions-item>
                <el-descriptions-item label="施工单位">{{ detailData.JR.construction_unit }}</el-descriptions-item>
                <el-descriptions-item label="数据采集年度">{{ detailData.JR.data_year }}</el-descriptions-item>
                <el-descriptions-item label="评价">{{ detailData.JR.evaluation }}</el-descriptions-item>
                <el-descriptions-item label="录入形式">{{ detailData.JR.input_form }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">燃烧机照片</el-divider>
              <div class="photo-grid">
                <div class="photo-cell">
                  <div class="photo-cell-label">正常照片</div>
                  <el-image v-if="detailData.JR.burner_photo_normal" :src="thumbUrl(detailData.JR.burner_photo_normal)" :preview-src-list="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing)" :initial-index="0" :preview-teleported="true" fit="cover" class="photo-thumb" />
                  <div v-else class="photo-empty">无</div>
                </div>
                <div class="photo-cell">
                  <div class="photo-cell-label">损坏照片</div>
                  <el-image v-if="detailData.JR.burner_photo_damaged" :src="thumbUrl(detailData.JR.burner_photo_damaged)" :preview-src-list="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing)" :initial-index="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing).indexOf(detailData.JR.burner_photo_damaged)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                  <div v-else class="photo-empty">无</div>
                </div>
                <div class="photo-cell">
                  <div class="photo-cell-label">瑕疵细节图</div>
                  <el-image v-if="detailData.JR.burner_photo_detail" :src="thumbUrl(detailData.JR.burner_photo_detail)" :preview-src-list="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing)" :initial-index="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing).indexOf(detailData.JR.burner_photo_detail)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                  <div v-else class="photo-empty">无</div>
                </div>
                <div class="photo-cell">
                  <div class="photo-cell-label">缺失照片</div>
                  <el-image v-if="detailData.JR.burner_photo_missing" :src="thumbUrl(detailData.JR.burner_photo_missing)" :preview-src-list="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing)" :initial-index="photoList(detailData.JR.burner_photo_normal, detailData.JR.burner_photo_damaged, detailData.JR.burner_photo_detail, detailData.JR.burner_photo_missing).indexOf(detailData.JR.burner_photo_missing)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                  <div v-else class="photo-empty">无</div>
                </div>
              </div>
            </template>
            <el-empty v-else description="无加热设备数据" />
          </el-tab-pane>

          <!-- SR 散热器 -->
          <el-tab-pane label="SR 散热器" name="SR">
            <template v-if="detailData.SR">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="散热器编码">{{ detailData.SR.radiator_code }}</el-descriptions-item>
                <el-descriptions-item label="评价">{{ detailData.SR.evaluation }}</el-descriptions-item>
                <el-descriptions-item label="录入形式">{{ detailData.SR.input_form }}</el-descriptions-item>
                <el-descriptions-item label="数据采集年度">{{ detailData.SR.data_year }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">部件明细</el-divider>
              <el-table :data="srTableData" border style="width: 100%">
                <el-table-column prop="name" label="部件名称" width="100" align="center" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '无数据' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="year" label="修复年度" width="100" align="center" />
                <el-table-column label="损坏照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.damaged" :src="thumbUrl(row.damaged)" :preview-src-list="photoList(row.damaged)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="瑕疵细节图" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.detail" :src="thumbUrl(row.detail)" :preview-src-list="photoList(row.detail)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="缺失照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.missing" :src="thumbUrl(row.missing)" :preview-src-list="photoList(row.missing)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="无散热器数据" />
          </el-tab-pane>

          <!-- ZK 自控设备 -->
          <el-tab-pane label="ZK 自控设备" name="ZK">
            <template v-if="detailData.ZK">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="自控设备编码">{{ detailData.ZK.controller_code }}</el-descriptions-item>
                <el-descriptions-item label="评价">{{ detailData.ZK.evaluation }}</el-descriptions-item>
                <el-descriptions-item label="录入形式">{{ detailData.ZK.input_form }}</el-descriptions-item>
                <el-descriptions-item label="数据采集年度">{{ detailData.ZK.data_year }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">部件明细</el-divider>
              <el-table :data="zkTableData" border style="width: 100%">
                <el-table-column prop="name" label="部件名称" width="120" align="center" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '无数据' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="year" label="修复年度" width="100" align="center" />
                <el-table-column label="正常照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.normal" :src="thumbUrl(row.normal)" :preview-src-list="photoList(row.normal)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="损坏照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.damaged" :src="thumbUrl(row.damaged)" :preview-src-list="photoList(row.damaged)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="瑕疵细节图" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.detail" :src="thumbUrl(row.detail)" :preview-src-list="photoList(row.detail)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="缺失照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.missing" :src="thumbUrl(row.missing)" :preview-src-list="photoList(row.missing)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="无自控设备数据" />
          </el-tab-pane>

          <!-- ZT 烤房主体 -->
          <el-tab-pane label="ZT 烤房主体" name="ZT">
            <template v-if="detailData.ZT">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="主体编码">{{ detailData.ZT.body_code }}</el-descriptions-item>
                <el-descriptions-item label="评价">{{ detailData.ZT.evaluation }}</el-descriptions-item>
                <el-descriptions-item label="录入形式">{{ detailData.ZT.input_form }}</el-descriptions-item>
                <el-descriptions-item label="数据采集年度">{{ detailData.ZT.data_year }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">部件明细</el-divider>
              <el-table :data="ztTableData" border style="width: 100%">
                <el-table-column prop="name" label="部件名称" width="100" align="center" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '无数据' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="year" label="修复年度" width="100" align="center" />
                <el-table-column label="正常照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.normal" :src="thumbUrl(row.normal)" :preview-src-list="photoList(row.normal)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="损坏照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.damaged" :src="thumbUrl(row.damaged)" :preview-src-list="photoList(row.damaged)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="瑕疵细节图" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.detail" :src="thumbUrl(row.detail)" :preview-src-list="photoList(row.detail)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="缺失照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.missing" :src="thumbUrl(row.missing)" :preview-src-list="photoList(row.missing)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="无烤房主体数据" />
          </el-tab-pane>

          <!-- FS 附属设施 -->
          <el-tab-pane label="FS 附属设施" name="FS">
            <template v-if="detailData.FS">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="附属编码">{{ detailData.FS.annex_code }}</el-descriptions-item>
                <el-descriptions-item label="评价">{{ detailData.FS.evaluation }}</el-descriptions-item>
                <el-descriptions-item label="烟夹使用情况">{{ detailData.FS.clip_used }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">部件明细</el-divider>
              <el-table :data="fsTableData" border style="width: 100%">
                <el-table-column prop="name" label="部件名称" width="100" align="center" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '无数据' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="修复年度" width="100" align="center">
                  <template #default="{ row }">{{ row.year || '-' }}</template>
                </el-table-column>
                <el-table-column label="正常照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.normal" :src="thumbUrl(row.normal)" :preview-src-list="photoList(row.normal)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="损坏照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.damaged" :src="thumbUrl(row.damaged)" :preview-src-list="photoList(row.damaged)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="瑕疵细节图" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.detail" :src="thumbUrl(row.detail)" :preview-src-list="photoList(row.detail)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
                <el-table-column label="缺失照片" width="110" align="center">
                  <template #default="{ row }">
                    <el-image v-if="row.missing" :src="thumbUrl(row.missing)" :preview-src-list="photoList(row.missing)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    <span v-else class="photo-empty-text">无</span>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="无附属设施数据" />
          </el-tab-pane>

          <!-- 维修记录 -->
          <el-tab-pane label="维修记录" name="repair">
            <div v-loading="detailRepairLoading">
              <template v-if="detailRepairList.length">
                <div style="margin-bottom: 10px; color: #606266; font-size: 13px;">
                  共 {{ detailRepairList.length }} 条维修记录
                </div>
                <el-table :data="detailRepairList" border style="width: 100%" size="small">
                  <el-table-column prop="repairYear" label="维修年度" width="90" align="center">
                    <template #default="{ row: r }">{{ r.repairYear || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="componentName" label="部件名称" min-width="100" align="center" show-overflow-tooltip>
                    <template #default="{ row: r }">{{ r.componentName || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="repairAction" label="维修动作" min-width="90" align="center" show-overflow-tooltip>
                    <template #default="{ row: r }">{{ r.repairAction || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="replacementType" label="更换类型" width="100" align="center">
                    <template #default="{ row: r }">{{ r.replacementType || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="estimatedCost" label="工程造价" width="100" align="center">
                    <template #default="{ row: r }">{{ formatRepairCost(r.estimatedCost) }}</template>
                  </el-table-column>
                  <el-table-column prop="industryAmount" label="行业投入金额" width="110" align="center">
                    <template #default="{ row: r }">{{ formatRepairCost(r.industryAmount) }}</template>
                  </el-table-column>
                </el-table>
              </template>
              <el-empty v-else description="暂无维修记录" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
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

/* ===== 统计卡片（多色系 - 左侧竖条+圆角图标风格） ===== */
.stats-row {
  display: flex;
  gap: 7px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.25s;
  max-height: 70px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 16px rgba(0, 0, 0, 0.1);
}

.stat-bar {
  width: 4px;
  align-self: stretch;
  border-radius: 2px;
  flex-shrink: 0;
}

.stat-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
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

/* ===== 操作按钮区 ===== */
.action-bar {
  display: flex;
  gap: 7px;
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

/* 表格内取消项目编号的方框样式 */
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

/* ===== 详情弹窗 ===== */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0 20px 14px;
  max-height: 80vh;
  overflow: hidden;
}

.detail-body {
  max-height: 78vh;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Tab 区域确保标题不被遮挡，左侧留足 padding */
.detail-tabs :deep(.el-tabs__header) {
  padding-left: 14px;
  margin-bottom: 7px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: #FFFFFF;
  padding-top: 7px;
  backdrop-filter: blur(8px);
}

.detail-tabs :deep(.el-tabs__content) {
  padding: 0 7px;
  overflow: visible;
}

.detail-tabs :deep(.el-tab-pane) {
  padding: 7px 0;
}

/* el-descriptions 样式微调 */
.detail-tabs :deep(.el-descriptions) {
  margin-bottom: 7px;
}

.detail-tabs :deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 600;
  color: #5A8A7A;
}

/* el-divider 标题样式 */
.detail-tabs :deep(.el-divider__text) {
  font-size: 14px;
  font-weight: 600;
  color: #1A6B4F;
  background: #f5f9f7;
}

/* el-table 照片列样式 */
.detail-tabs :deep(.el-table .cell) {
  text-align: center;
}

/* ===== 照片网格（JR 加热设备用） ===== */
.photo-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 7px 0;
}

.photo-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.photo-cell-label {
  font-size: 12px;
  color: #5A8A7A;
  font-weight: 500;
}

/* ===== 照片缩略图（80x80） ===== */
.photo-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid rgba(26, 107, 79, 0.2);
  cursor: pointer;
  transition: all 0.2s;
  object-fit: cover;
}

.photo-thumb:hover {
  border-color: #1A6B4F;
  box-shadow: 0 2px 8px rgba(26, 107, 79, 0.25);
  transform: scale(1.05);
}

/* 空照片占位（网格用） */
.photo-empty {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px dashed rgba(138, 168, 160, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #A0B5A8;
  background: rgba(240, 247, 243, 0.5);
}

/* 空照片文字（表格内用） */
.photo-empty-text {
  font-size: 12px;
  color: #A0B5A8;
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
</style>

<!-- 全局样式：图片预览查看器置顶 + 关闭按钮增强（teleported 到 body，不能用 scoped） -->
<style>
.el-image-viewer__wrapper {
  z-index: 9999 !important;
}

.el-image-viewer__btn.el-image-viewer__close {
  width: 52px !important;
  height: 52px !important;
  font-size: 36px !important;
  background-color: rgba(0, 0, 0, 0.6) !important;
  border-radius: 50% !important;
  top: 24px !important;
  right: 24px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;
  transition: all 0.2s !important;
}

.el-image-viewer__btn.el-image-viewer__close:hover {
  background-color: rgba(220, 53, 69, 0.85) !important;
  transform: scale(1.1) !important;
}
</style>
