<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View, Setting } from '@element-plus/icons-vue'
import { Building2, Zap, Wind, CircuitBoard, Warehouse, Package } from 'lucide-vue-next'
import BarnCode from '@/components/BarnCode.vue'
import StatusTag from '@/components/StatusTag.vue'
import { getComponentList, getBarnFullProfile } from '@/api/barn'
import { listCounty, listTownship } from '@/api/dict'
import { thumbUrl } from '@/utils/image'

/** 部位分类定义 */
const categories = [
  { key: '', label: '全部' },
  { key: 'JR', label: 'JR加热设备' },
  { key: 'SR', label: 'SR散热器' },
  { key: 'ZK', label: 'ZK自控设备' },
  { key: 'ZT', label: 'ZT烤房主体' },
  { key: 'FS', label: 'FS附属设施' },
]

/** 状态选项（对应数据库evaluation字段值） */
const statusOptions = [
  { value: '优良', label: '优良' },
  { value: '较好', label: '较好' },
  { value: '较差', label: '较差' },
  { value: '差', label: '差' },
]

/** 统计卡片配置 */
const statCards = [
  { key: 'total', title: '总烤房数', icon: Building2 },
  { key: 'JR', title: 'JR加热设备', icon: Zap },
  { key: 'SR', title: 'SR散热器', icon: Wind },
  { key: 'ZK', title: 'ZK自控设备', icon: CircuitBoard },
  { key: 'ZT', title: 'ZT烤房主体', icon: Warehouse },
  { key: 'FS', title: 'FS附属设施', icon: Package },
] as const

const stats = reactive({
  total: 0,
  JR: 0,
  SR: 0,
  ZK: 0,
  ZT: 0,
  FS: 0,
})

/** 筛选表单 */
const filterForm = reactive({
  category: '',
  projectId: '',
  county: '',
  township: '',
  status: '',
  facilityStatus: '',
})

const countyOptions = ref<{ value: string; label: string }[]>([])
const townOptions = ref<{ value: string; label: string }[]>([])

/** 表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({ pageNum: 1, pageSize: 10 })

/** 详情弹窗 */
const detailDialog = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>(null)
const activeCategory = ref('JR')

/** 状态 → el-tag 类型 */
function getStatusTagType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  return statusTagType(status)
}

/** 加载县区 */
async function loadCounties() {
  try {
    const res: any = await listCounty()
    countyOptions.value = (res || []).map((c: any) => ({ value: c.countyName, label: c.countyName }))
  } catch { /* ignore */ }
}

/** 县区变化联动乡镇 */
function handleCountyChange(countyName: string) {
  filterForm.township = ''
  loadTowns(countyName)
}

/** 加载乡镇 */
async function loadTowns(countyName: string) {
  if (!countyName) { townOptions.value = []; return }
  try {
    const res: any = await listTownship(countyName)
    townOptions.value = (res || []).map((t: any) => ({
      value: t.townshipName || t.townName,
      label: t.townshipName || t.townName,
    }))
  } catch { townOptions.value = [] }
}

/** 当前是否选了具体部位 */
const isCategoryMode = computed(() => !!filterForm.category)

/** 加载列表数据 */
async function loadData() {
  loading.value = true
  try {
    const res: any = await getComponentList({
      category: filterForm.category || undefined,
      projectId: filterForm.projectId || undefined,
      county: filterForm.county || undefined,
      township: filterForm.township || undefined,
      status: filterForm.status || undefined,
      facilityStatus: filterForm.facilityStatus || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    })
    const rows = res?.rows || []
    tableData.value = rows
    total.value = res?.total ?? rows.length
  } catch (error) {
    console.error('加载部件列表失败:', error)
    ElMessage.error('加载部件列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 加载统计数据 */
async function loadStats() {
  try {
    const res: any = await getComponentList({
      pageNum: 1,
      pageSize: 9999,
      county: filterForm.county || undefined,
      township: filterForm.township || undefined,
      projectId: filterForm.projectId || undefined,
      facilityStatus: filterForm.facilityStatus || undefined,
    })
    const rows = res?.rows || []
    stats.total = rows.length
    // 全部模式下每行有jrCode/srCode等字段，有编码就算有该部件
    stats.JR = rows.filter((r: any) => r.jrCode).length
    stats.SR = rows.filter((r: any) => r.srCode).length
    stats.ZK = rows.filter((r: any) => r.zkCode).length
    stats.ZT = rows.filter((r: any) => r.ztCode).length
    stats.FS = rows.filter((r: any) => r.fsCode).length
  } catch { /* ignore */ }
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadData()
  loadStats()
}

/** 重置 */
function handleReset() {
  Object.assign(filterForm, { category: '', projectId: '', county: '', township: '', status: '', facilityStatus: '' })
  townOptions.value = []
  pagination.pageNum = 1
  loadData()
  loadStats()
}

/** 分类Tab切换 */
function handleCategoryChange(key: string) {
  filterForm.category = key
  pagination.pageNum = 1
  loadData()
}

/** 分页变化 */
function handlePageChange() {
  loadData()
}

/** 查看详情 */
async function handleViewDetail(row: any) {
  detailDialog.value = true
  detailLoading.value = true
  detailData.value = null
  activeCategory.value = 'JR'
  try {
    const res: any = await getBarnFullProfile(row.projectId || row.id)
    detailData.value = res || row
  } catch (error) {
    console.error('加载部件详情失败:', error)
    ElMessage.error('加载部件详情失败')
    detailData.value = row
  } finally {
    detailLoading.value = false
  }
}

/** 获取部位数据（后端返回的是原始数据库行，含project_id/evaluation/编码等字段） */
function getCategoryData(catKey: string): any {
  if (!detailData.value) return null
  const data = detailData.value
  // 后端返回结构: { baseInfo: {...}, JR: { project_id, heater_code, evaluation, ... }, SR: {...}, ... }
  if (data[catKey] && typeof data[catKey] === 'object') return data[catKey]
  return null
}

/**
 * 各部位基本信息字段配置（只展示有意义的数据字段，排除project_id/created_at/updated_at等内部字段）
 * 与烤房列表(BarnList.vue)详情弹窗保持一致的中文标签
 */
const CATEGORY_INFO_FIELDS: Record<string, { key: string; label: string }[]> = {
  JR: [
    { key: 'heater_code', label: '加热设备编码' },
    { key: 'heater_type', label: '加热设备类型' },
    { key: 'burner_code', label: '燃烧机编码' },
    { key: 'burner_status', label: '燃烧机状态' },
    { key: 'burner_repair_year', label: '燃烧机修复年度' },
    { key: 'build_year', label: '年度' },
    { key: 'project_cost', label: '工程造价(元)' },
    { key: 'subsidy_total', label: '补贴总额(元)' },
    { key: 'subsidy_national', label: '国家局补贴(元)' },
    { key: 'subsidy_region', label: '产区补贴(元)' },
    { key: 'construction_unit', label: '施工(供应)单位' },
    { key: 'data_year', label: '数据采集年度' },
    { key: 'evaluation', label: '评价' },
    { key: 'input_form', label: '录入形式' },
  ],
  SR: [
    { key: 'radiator_code', label: '散热器编码' },
    { key: 'data_year', label: '数据采集年度' },
    { key: 'evaluation', label: '评价' },
    { key: 'input_form', label: '录入形式' },
  ],
  ZK: [
    { key: 'controller_code', label: '自控设备编码' },
    { key: 'data_year', label: '数据采集年度' },
    { key: 'evaluation', label: '评价' },
    { key: 'input_form', label: '录入形式' },
  ],
  ZT: [
    { key: 'body_code', label: '主体编码' },
    { key: 'data_year', label: '数据采集年度' },
    { key: 'evaluation', label: '评价' },
    { key: 'input_form', label: '录入形式' },
  ],
  FS: [
    { key: 'annex_code', label: '附属编码' },
    { key: 'evaluation', label: '评价' },
    { key: 'clip_used', label: '烟夹使用情况' },
    { key: 'clip_status', label: '烟夹状态' },
    { key: 'clip_pending_count', label: '待补充数量' },
  ],
}

/** 各部位子部件配置（用于表格展示状态/修复年度/照片） */
const CATEGORY_SUB_PARTS: Record<string, { name: string; prefix: string; hasNormal?: boolean }[]> = {
  JR: [], // JR只有燃烧机一个部件，直接在基本信息中展示
  SR: [
    { name: '散热管', prefix: 'pipe' },
    { name: '炉膛', prefix: 'furnace' },
    { name: '清灰门', prefix: 'ash_door' },
    { name: '烟囱', prefix: 'chimney' },
  ],
  ZK: [
    { name: '冷风门', prefix: 'cold_door' },
    { name: '冷风门电动机', prefix: 'cold_door_motor' },
    { name: '排湿窗', prefix: 'exhaust_window' },
    { name: '循环风机', prefix: 'circulation_fan' },
    { name: '助燃鼓风机', prefix: 'combustion_fan' },
    { name: '操作箱', prefix: 'control_box', hasNormal: true },
    { name: '水壶', prefix: 'water_pot' },
    { name: '探头', prefix: 'probe' },
  ],
  ZT: [
    { name: '墙体屋顶', prefix: 'wall_roof', hasNormal: true },
    { name: '挂烟梁柱', prefix: 'beam', hasNormal: true },
    { name: '装烟室门', prefix: 'door', hasNormal: true },
  ],
  FS: [
    { name: '烟棚', prefix: 'shed', hasNormal: true },
    { name: '电路电器', prefix: 'electrical', hasNormal: true },
  ],
}

/** JR部位燃烧机照片配置 */
const JR_PHOTOS = [
  { key: 'burner_photo_normal', label: '正常照片' },
  { key: 'burner_photo_damaged', label: '损坏照片' },
  { key: 'burner_photo_detail', label: '瑕疵细节图' },
  { key: 'burner_photo_missing', label: '缺失照片' },
]

/** 获取部位基本信息列表（排除空值字段） */
function getCategoryInfoFields(catKey: string): { label: string; value: string }[] {
  const cat = getCategoryData(catKey)
  if (!cat) return []
  const fields = CATEGORY_INFO_FIELDS[catKey] || []
  return fields.map(f => ({
    label: f.label,
    value: cat[f.key] != null && String(cat[f.key]).trim() !== '' ? String(cat[f.key]) : '-',
  }))
}

/** 获取部位子部件表格数据 */
function getCategorySubParts(catKey: string): any[] {
  const cat = getCategoryData(catKey)
  if (!cat) return []
  const parts = CATEGORY_SUB_PARTS[catKey] || []
  return parts.map(p => ({
    name: p.name,
    status: cat[`${p.prefix}_status`] || '',
    year: cat[`${p.prefix}_repair_year`] || '',
    normal: p.hasNormal ? cat[`${p.prefix}_photo_normal`] : null,
    damaged: cat[`${p.prefix}_photo_damaged`],
    detail: cat[`${p.prefix}_photo_detail`],
    missing: cat[`${p.prefix}_photo_missing`],
  }))
}

/** JR部位照片列表 */
function getJRPhotos(): { label: string; url: string }[] {
  const cat = getCategoryData('JR')
  if (!cat) return []
  return JR_PHOTOS.map(p => ({
    label: p.label,
    url: cat[p.key] || '',
  })).filter(p => p.url)
}

/** 收集所有照片URL用于预览 */
function photoList(...urls: (string | null | undefined)[]): string[] {
  return urls.filter(u => u && String(u).trim()) as string[]
}

/** 状态标签类型 */
function statusTagType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === '正常' || status === '优良') return 'success'
  if (status === '较好') return 'success'
  if (status === '较差') return 'warning'
  if (status === '差' || status === '损坏' || status === '缺失') return 'danger'
  return 'info'
}

/** 规范化使用状态值（后端可能返回中文或英文，统一映射为 StatusTag 所需的英文 key）
 *  baking / inUse / 在烤 / 使用中 -> baking（显示"在烤"）
 *  idle / 空闲 -> idle（显示"空闲"）
 */
function normalizeUseStatus(status: string): string {
  if (!status) return 'idle'
  const s = String(status).toLowerCase()
  if (s === 'baking' || s === 'inuse' || s === '在烤' || s === '使用中') return 'baking'
  if (s === 'idle' || s === '空闲') return 'idle'
  return status
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
          <el-icon :size="18"><Setting /></el-icon>
        </div>
        <div class="page-title-text">
          <h2 class="page-title">烤房部件</h2>
          <p class="page-desc">管理烤房5大部位（JR加热设备/SR散热器/ZK自控设备/ZT烤房主体/FS附属设施）的状态与档案</p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="card in statCards" :key="card.key" class="stat-card" :class="'stat-card--' + card.key">
        <div class="stat-icon">
          <component :is="card.icon" :size="22" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats[card.key] }}</div>
          <div class="stat-label">{{ card.title }}</div>
        </div>
      </div>
    </div>

    <!-- 部位分类Tab -->
    <div class="category-tabs">
      <div
        v-for="cat in categories"
        :key="cat.key || 'all'"
        class="category-tab"
        :class="{ active: filterForm.category === cat.key }"
        @click="handleCategoryChange(cat.key)"
      >
        {{ cat.label }}
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="项目编号">
          <el-input v-model="filterForm.projectId" placeholder="模糊匹配" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="filterForm.county" placeholder="全部" clearable style="width: 120px" @change="handleCountyChange">
            <el-option v-for="c in countyOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="乡镇">
          <el-select v-model="filterForm.township" placeholder="请先选区县" clearable style="width: 120px" :disabled="!filterForm.county">
            <el-option v-for="t in townOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 100px">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
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
      <el-table :data="tableData" v-loading="loading" style="width: 100%" size="small">
        <!-- 烤房项目编号 -->
        <el-table-column label="烤房编码" width="130" align="center" show-overflow-tooltip>
          <template #default="{ row }"><BarnCode :code="row.projectId || row.id" /></template>
        </el-table-column>
        <el-table-column prop="county" label="区县" width="55" align="center" />
        <el-table-column prop="township" label="乡镇" width="55" align="center" />
        <el-table-column prop="village" label="村" width="55" align="center" />
        <el-table-column label="使用状态" width="70" align="center">
          <template #default="{ row }">
            <StatusTag :status="normalizeUseStatus(row.useStatus || row.status || 'idle')" type="useStatus" />
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
        <el-table-column label="健康分" width="55" align="center">
          <template #default="{ row }">
            <span v-if="row.healthScore != null" :style="{ color: getHealthScoreColor(row.healthScore), fontWeight: 600 }">
              {{ Number(row.healthScore).toFixed(1) }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余寿命(年)" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.lifeResidual != null" :style="{ color: getLifeColor(row.lifeResidual), fontWeight: 600 }">
              {{ Number(row.lifeResidual).toFixed(1) }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="detailAddress" label="详细地址" min-width="70" show-overflow-tooltip />

        <!-- 选了具体部位时：显示该部件编码列 -->
        <el-table-column v-if="isCategoryMode" prop="componentCode" label="部件编码" width="120" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="component-code">{{ row.componentCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isCategoryMode" prop="dataYear" label="数据年度" width="75" align="center" />
        <el-table-column v-if="isCategoryMode" prop="categoryName" label="部位类型" width="85" align="center" />

        <!-- 全部模式：显示5大部位编码 -->
        <template v-else>
          <el-table-column prop="jrCode" label="JR编码" width="75" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.jrCode" class="component-code">{{ row.jrCode }}</span>
              <span v-else class="status-dash">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="srCode" label="SR编码" width="75" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.srCode" class="component-code">{{ row.srCode }}</span>
              <span v-else class="status-dash">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="zkCode" label="ZK编码" width="75" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.zkCode" class="component-code">{{ row.zkCode }}</span>
              <span v-else class="status-dash">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="ztCode" label="ZT编码" width="75" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.ztCode" class="component-code">{{ row.ztCode }}</span>
              <span v-else class="status-dash">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="fsCode" label="FS编码" width="75" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.fsCode" class="component-code">{{ row.fsCode }}</span>
              <span v-else class="status-dash">-</span>
            </template>
          </el-table-column>
        </template>

        <el-table-column label="操作" width="65" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="View" @click="handleViewDetail(row)">详情</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialog" title="烤房部件档案" width="900px" destroy-on-close class="detail-dialog" append-to-body align-center>
      <div v-loading="detailLoading" class="detail-scroll-wrap">
        <div v-if="detailData" class="detail-content">
          <!-- 基本信息 -->
          <div class="detail-header">
            <BarnCode :code="detailData.baseInfo?.project_id || detailData.projectId || detailData.id" />
            <span class="detail-loc" v-if="detailData.baseInfo?.county || detailData.county">
              {{ detailData.baseInfo?.county || detailData.county || '' }} {{ detailData.baseInfo?.township || detailData.township || '' }} {{ detailData.baseInfo?.village || detailData.village || '' }}
            </span>
            <span class="detail-addr" v-if="detailData.baseInfo?.detail_address || detailData.detailAddress">
              {{ detailData.baseInfo?.detail_address || detailData.detailAddress }}
            </span>
          </div>

          <!-- 5大部位 Tab -->
          <el-tabs v-model="activeCategory" class="detail-tabs">
            <el-tab-pane
              v-for="cat in categories.slice(1)"
              :key="cat.key"
              :label="cat.label"
              :name="cat.key"
            >
              <div v-if="getCategoryData(cat.key)" class="category-detail">
                <!-- 基本信息 -->
                <el-descriptions :column="3" border class="cat-desc">
                  <el-descriptions-item
                    v-for="field in getCategoryInfoFields(cat.key)"
                    :key="field.label"
                    :label="field.label"
                  >
                    <el-tag v-if="field.label === '评价'" :type="statusTagType(field.value)" size="small">{{ field.value }}</el-tag>
                    <el-tag v-else-if="field.label.includes('状态') && field.value !== '-'" :type="statusTagType(field.value)" size="small">{{ field.value }}</el-tag>
                    <span v-else>{{ field.value }}</span>
                  </el-descriptions-item>
                </el-descriptions>

                <!-- JR 燃烧机照片 -->
                <template v-if="cat.key === 'JR' && getJRPhotos().length">
                  <el-divider content-position="left">燃烧机照片</el-divider>
                  <div class="photo-grid">
                    <div v-for="photo in getJRPhotos()" :key="photo.label" class="photo-cell">
                      <div class="photo-cell-label">{{ photo.label }}</div>
                      <el-image :src="thumbUrl(photo.url)" :preview-src-list="getJRPhotos().map(p => p.url)" :initial-index="getJRPhotos().findIndex(p => p.url === photo.url)" :preview-teleported="true" fit="cover" class="photo-thumb" />
                    </div>
                  </div>
                </template>

                <!-- 子部件明细表格（SR/ZK/ZT/FS） -->
                <template v-if="getCategorySubParts(cat.key).length">
                  <el-divider content-position="left">部件明细</el-divider>
                  <el-table :data="getCategorySubParts(cat.key)" border style="width: 100%" size="small">
                    <el-table-column prop="name" label="部件名称" width="120" align="center" />
                    <el-table-column label="状态" width="90" align="center">
                      <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '无数据' }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="year" label="修复年度" width="100" align="center" />
                    <el-table-column label="正常照片" width="110" align="center" v-if="(CATEGORY_SUB_PARTS[cat.key] || []).some(p => p.hasNormal)">
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
              </div>
              <el-empty v-else :description="`无${cat.label}数据`" />
            </el-tab-pane>
          </el-tabs>
        </div>
        <el-empty v-else-if="!detailLoading" description="暂无数据" />
      </div>
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
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

/* ===== 统计卡片（琥珀色系 - 左侧大号图标 + 右侧数据） ===== */
.stats-row {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 140px;
  display: flex;
  align-items: center;
  gap: 0;
  padding: 8px;
  background: #ffffff;
  border: 1px solid #FEF3C7;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.08);
  transition: all 0.25s;
  max-height: 70px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 16px rgba(217, 119, 6, 0.15);
}

.stat-icon {
  flex: 0 0 33%;
  height: 100%;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.stat-card--total .stat-icon { background: #FEF3C7; color: #D97706; }
.stat-card--JR .stat-icon { background: #FEF3C7; color: #F59E0B; }
.stat-card--SR .stat-icon { background: #FEF3C7; color: #FCD34D; }
.stat-card--ZK .stat-icon { background: #FEF3C7; color: #D97706; }
.stat-card--ZT .stat-icon { background: #FEF3C7; color: #F59E0B; }
.stat-card--FS .stat-icon { background: #FEF3C7; color: #FCD34D; }

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-left: 10px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #92400E;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #92837E;
}

/* ===== 部位分类Tab ===== */
.category-tabs {
  display: flex;
  gap: 7px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
  flex-wrap: wrap;
}

.category-tab {
  padding: 7px 16px;
  border-radius: 8px;
  font-size: clamp(12px, 1vw, 14px);
  font-weight: 500;
  color: #5A8A7A;
  cursor: pointer;
  transition: all 0.25s;
  user-select: none;
}

.category-tab:hover {
  background: rgba(46, 139, 106, 0.08);
  color: #2E8B6A;
}

.category-tab.active {
  background: linear-gradient(135deg, #0A4D3E 0%, #2E8B6A 100%);
  color: #fff;
  box-shadow: 0 3px 8px rgba(10, 77, 62, 0.3);
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

/* 表格内 BarnCode 紧凑样式 */
.table-card :deep(.barn-code) {
  padding: 2px 6px;
  font-size: 12px;
  line-height: 1.4;
}

/* 表格内 StatusTag 紧凑样式 */
.table-card :deep(.solid-status-block) {
  padding: 2px 8px;
  font-size: 12px;
  line-height: 18px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.status-dash {
  color: #C0C4CC;
  font-size: 12px;
}

.component-code {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 12px;
  font-weight: 600;
  color: #0A4D3E;
  background: rgba(46, 139, 106, 0.08);
  padding: 1px 4px;
  border-radius: 4px;
  white-space: nowrap;
}

/* 设施现状文字+圆点样式（与使用状态色块区分） */
.facility-status-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  font-size: 12px;
  white-space: nowrap;
}

.facility-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  display: inline-block;
}

.code-value {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  color: #0A4D3E;
  font-size: 15px;
}

/* ===== 详情弹窗 ===== */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0 20px;
  max-height: 75vh;
  overflow: hidden;
}

.detail-scroll-wrap {
  max-height: 65vh;
  overflow-y: auto;
  padding: 0 4px;
}

.detail-content {
  padding: 7px 0;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 14px;
  background: rgba(26, 107, 79, 0.06);
  border-radius: 8px;
  margin-bottom: 7px;
  flex-wrap: wrap;
}

.detail-loc {
  font-size: 14px;
  font-weight: 600;
  color: #0D5D46;
}

.detail-addr {
  font-size: 13px;
  color: #5A8A7A;
}

.detail-tabs {
  margin-top: 7px;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: #0A4D3E;
}

.detail-tabs :deep(.el-tabs__active-bar) {
  background-color: #2E8B6A;
}

.detail-tabs :deep(.el-tabs__item:hover) {
  color: #2E8B6A;
}

/* 描述列表样式 */
.cat-desc {
  margin-bottom: 7px;
}

.cat-desc :deep(.el-descriptions__label) {
  width: 130px;
  font-weight: 600;
  color: #5A8A7A;
  background: #F4F8F6;
}

.cat-desc :deep(.el-descriptions__content) {
  color: #0D3D30;
}

/* 照片网格 */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 11px;
  margin-bottom: 7px;
}

.photo-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.photo-cell-label {
  font-size: 12px;
  color: #5A8A7A;
  text-align: center;
  font-weight: 500;
}

.photo-thumb {
  width: 100%;
  height: 90px;
  border-radius: 6px;
  border: 1px solid #E1E8E5;
  cursor: pointer;
}

.photo-empty-text {
  font-size: 13px;
  color: #C0C4CC;
}

/* 表格内照片缩略图 */
:deep(.el-table .photo-thumb) {
  width: 80px;
  height: 60px;
}

.no-data {
  text-align: center;
  padding: 27px;
  color: #9CA3AF;
  font-size: 14px;
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .stats-row {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
  }
}

@media (max-width: 900px) {
  .stat-card {
    flex: 1 1 45%;
  }
  .category-tabs {
    flex-direction: column;
  }
  .category-tab {
    text-align: center;
  }
}

@media (max-width: 600px) {
  .stat-card {
    flex: 1 1 100%;
  }
  .category-overview {
    flex-direction: column;
    gap: 7px;
  }
}
</style>
