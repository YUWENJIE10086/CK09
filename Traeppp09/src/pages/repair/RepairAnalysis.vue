<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { DataAnalysis, Tools, Wallet, Coin, Calendar } from '@element-plus/icons-vue'
import { getRepairAnalysis, getRepairList } from '@/api/repair'
import { repairSyncHealthLife } from '@/api/ai'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'

// ========== 分析数据 ==========
const analysisData = ref<any>(null)
const loading = ref(false)

// ========== 预警窗口：已提报维修申请 ==========
const repairAlertClosed = ref(false)
const repairApplyCount = ref(0)

// ========== 智能分析数据（后端计算） ==========
const roiList = computed(() => analysisData.value?.roiList || [])
const priorityList = computed(() => analysisData.value?.priorityList || [])
const maintenancePlan = computed(() => analysisData.value?.maintenancePlan || [])

// ========== 一键同步维修记录 ==========
const syncing = ref(false)
async function runRepairSync() {
  syncing.value = true
  try {
    await repairSyncHealthLife()
    await loadAnalysis()
    await loadExtraData()
  } catch {
    // 忽略
  } finally {
    syncing.value = false
  }
}

// ========== BI看板：首页仅展示核心，余下进“更多”弹窗（可筛选/分页） ==========

// ROI排行
const roiDialogVisible = ref(false)
const roiBarnFilter = ref('')
const roiPage = ref(1)
const roiPageSize = ref(8)
const roiFiltered = computed(() => {
  const k = roiBarnFilter.value.trim().toLowerCase()
  return roiList.value.filter((r: any) => !k || String(r.barnId).toLowerCase().includes(k) || String(r.component || '').toLowerCase().includes(k))
})
const roiPaged = computed(() => {
  const s = (roiPage.value - 1) * roiPageSize.value
  return roiFiltered.value.slice(s, s + roiPageSize.value)
})
function openRoiDialog() {
  roiPage.value = 1
  roiDialogVisible.value = true
}

// 维修优先级与寿命预警（全宽主表，BI看板展示前 8 条）
const priorityTop = computed(() => priorityList.value.slice(0, 6))
const priorityDialogVisible = ref(false)
const priWarnFilter = ref('')
const priBarnFilter = ref('')
const priPage = ref(1)
const priPageSize = ref(8)
const priWarnOptions = computed(() =>
  ['正常', '关注', '健康预警', '寿命预警'].filter(t => priorityList.value.some((r: any) => r.warning === t)),
)
const priFiltered = computed(() => {
  const k = priBarnFilter.value.trim().toLowerCase()
  const w = priWarnFilter.value
  return priorityList.value.filter((r: any) =>
    (!k || String(r.barnId).toLowerCase().includes(k)) && (!w || r.warning === w),
  )
})
const priPaged = computed(() => {
  const s = (priPage.value - 1) * priPageSize.value
  return priFiltered.value.slice(s, s + priPageSize.value)
})
function openPriorityDialog() {
  priPage.value = 1
  priorityDialogVisible.value = true
}

// 维修方案推荐
const planDialogVisible = ref(false)
const planBarnFilter = ref('')
const planPage = ref(1)
const planPageSize = ref(8)
const planFiltered = computed(() => {
  const k = planBarnFilter.value.trim().toLowerCase()
  return maintenancePlan.value.filter((r: any) => !k || String(r.barnId).toLowerCase().includes(k))
})
const planPaged = computed(() => {
  const s = (planPage.value - 1) * planPageSize.value
  return planFiltered.value.slice(s, s + planPageSize.value)
})
function openPlanDialog() {
  planPage.value = 1
  planDialogVisible.value = true
}

watch(() => roiBarnFilter.value, () => { roiPage.value = 1 })
watch(() => [priWarnFilter.value, priBarnFilter.value], () => { priPage.value = 1 })
watch(() => planBarnFilter.value, () => { planPage.value = 1 })

/** 主题色 */
const THEME = {
  primary: '#0D5D46',
  green: '#2E8B6A',
  lightGreen: '#5FB292',
  blue: '#1565C0',
  orange: '#D4944A',
  purple: '#7B1FA2',
}

/** 更换类型颜色规则 */
const replacementTypeColorMap: Record<string, { bg: string; color: string }> = {
  '新采购': { bg: '#E8F5EE', color: '#2E8B6A' },
  '改造升级': { bg: '#E3F2FD', color: '#1565C0' },
  '维护修复': { bg: '#FEF3E7', color: '#D4944A' },
  '防护加固': { bg: '#F3E5F5', color: '#7B1FA2' },
}

/** 获取更换类型样式 */
function getReplacementTypeStyle(type: string) {
  return replacementTypeColorMap[type] || { bg: '#F0F0F0', color: '#606266' }
}

/** 加载分析数据 */
async function loadAnalysis() {
  loading.value = true
  try {
    const res: any = await getRepairAnalysis()
    analysisData.value = res || {}
  } catch {
    analysisData.value = {}
  } finally {
    loading.value = false
  }
}

/** 加载待审核维修数量 */
async function loadExtraData() {
  try {
    const repairRes = await getRepairList({ pageNum: 1, pageSize: 9999 })
    const rows = repairRes?.rows || []
    repairApplyCount.value = rows.filter((r: any) => r.repairStatus === '待审核').length
  } catch {
    // ignore
  }
}

/** 总览指标 */
const overview = computed(() => analysisData.value?.overview || {})

/** 总维修次数 */
const totalRecords = computed(() => overview.value.total_records ?? '-')

/** 总投入金额（工程造价 + 行业投入） */
const totalInvest = computed(() => {
  const cost = Number(overview.value.total_cost || 0)
  const industry = Number(overview.value.total_industry || 0)
  const sum = cost + industry
  return sum > 0 ? `¥${sum.toLocaleString()}` : '-'
})

/** 平均单次维修成本 */
const avgCost = computed(() => {
  const records = Number(overview.value.total_records || 0)
  const cost = Number(overview.value.total_cost || 0)
  if (records > 0 && cost > 0) {
    return `¥${Math.round(cost / records).toLocaleString()}`
  }
  return '-'
})

/** 本年度维修次数 */
const currentYearRecords = computed(() => {
  const stats: any[] = analysisData.value?.yearStats || []
  const currentYear = String(new Date().getFullYear())
  const item = stats.find(s => String(s.repair_year) === currentYear)
  return item ? (item.cnt ?? '-') : '-'
})

// ========== 图表配置 ==========

/** 1. 年度维修资金趋势（面积折线图） */
const yearTrendOption = computed(() => {
  const stats: any[] = analysisData.value?.yearStats || []
  const years = stats.map(s => s.repair_year)
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eee',
      textStyle: { color: '#333', fontSize: 12 },
      valueFormatter: (v: any) => `¥${Number(v || 0).toLocaleString()}`,
    },
    legend: { top: 0, right: 10, textStyle: { color: '#606266', fontSize: 11 } },
    grid: { left: 50, right: 16, top: 36, bottom: 28 },
    xAxis: {
      type: 'category',
      data: years,
      axisLine: { lineStyle: { color: '#C0D6CC' } },
      axisLabel: { color: '#606266', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#606266',
        fontSize: 11,
        formatter: (v: number) => v >= 10000 ? `${(v / 10000).toFixed(0)}万` : `${v}`,
      },
      splitLine: { lineStyle: { color: '#EEF5F2' } },
    },
    series: [
      {
        name: '工程造价',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: stats.map(s => s.total_cost || 0),
        itemStyle: { color: THEME.green },
        lineStyle: { color: THEME.green, width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(46,139,106,0.35)' },
            { offset: 1, color: 'rgba(46,139,106,0.02)' },
          ]),
        },
      },
      {
        name: '行业投入',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: stats.map(s => s.total_industry || 0),
        itemStyle: { color: THEME.primary },
        lineStyle: { color: THEME.primary, width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(13,93,70,0.30)' },
            { offset: 1, color: 'rgba(13,93,70,0.02)' },
          ]),
        },
      },
    ],
  }
})

/** 2. 更换类型占比（环形图） */
const typePieOption = computed(() => {
  const stats: any[] = analysisData.value?.typeStats || []
  const data = stats.map(s => {
    const c = getReplacementTypeStyle(s.replacement_type)
    return {
      name: s.replacement_type,
      value: s.cnt || 0,
      itemStyle: { color: c.color },
    }
  })
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eee',
      textStyle: { color: '#333', fontSize: 12 },
      formatter: '{b}: {c} 次 ({d}%)',
    },
    legend: { orient: 'vertical', right: 6, top: 'center', textStyle: { color: '#606266', fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%', color: '#2A5A4E', fontSize: 11 },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data,
      },
    ],
  }
})

/** 3. 部件维修次数TOP（横向条形图） */
const componentBarOption = computed(() => {
  const stats: any[] = [...(analysisData.value?.componentStats || [])]
    .sort((a, b) => (b.cnt || 0) - (a.cnt || 0))
    .slice(0, 8)
    .reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eee',
      textStyle: { color: '#333', fontSize: 12 },
      formatter: (p: any) => `${p[0].name}: ${p[0].value} 次`,
    },
    grid: { left: 70, right: 24, top: 20, bottom: 24 },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#606266', fontSize: 11 },
      splitLine: { lineStyle: { color: '#EEF5F2' } },
    },
    yAxis: {
      type: 'category',
      data: stats.map(s => s.component_name),
      axisLine: { lineStyle: { color: '#C0D6CC' } },
      axisLabel: { color: '#606266', fontSize: 11 },
    },
    series: [
      {
        name: '维修次数',
        type: 'bar',
        barWidth: 14,
        data: stats.map(s => s.cnt || 0),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: 'rgba(13,93,70,0.45)' },
            { offset: 1, color: '#0D5D46' },
          ]),
          borderRadius: [0, 6, 6, 0],
        },
        label: { show: true, position: 'right', color: '#2A5A4E', fontSize: 11 },
      },
    ],
  }
})

/** 格式化金额 */
function formatMoney(val: number) {
  return `¥${val.toLocaleString()}`
}

/** 健康等级Tag类型 */
function healthLevelTagType(level: string) {
  if (level === '优良') return 'success'
  if (level === '良好') return 'success'
  if (level === '一般') return 'warning'
  if (level === '预警') return 'danger'
  if (level === '危险') return 'danger'
  return 'info'
}

/** 寿命预警Tag类型 */
function warnTagType(level: string) {
  if (level === '紧急维修') return 'danger'
  if (level === '建议维修') return 'warning'
  if (level === '需关注') return 'info'
  return 'success'
}

/** 维修优先级Tag类型 */
function priorityTagType(priority: string) {
  if (priority === '高') return 'danger'
  if (priority === '中') return 'warning'
  return 'info'
}

onMounted(() => {
  loadAnalysis()
  loadExtraData()
})
</script>

<template>
  <div class="page-container repair-analysis" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><DataAnalysis /></el-icon>
        </div>
        <h2 class="page-title">维修分析</h2>
      </div>
      <p class="page-desc">维修资金趋势、类型分布、ROI排行、维修优先级与维修方案</p>
      <div class="header-actions">
        <el-button
          type="primary"
          :loading="syncing"
          :disabled="loading"
          size="small"
          @click="runRepairSync"
        >
          <el-icon v-if="!syncing" style="margin-right: 4px"><Tools /></el-icon>
          一键同步维修记录（续寿命/加健康分）
        </el-button>
      </div>
    </div>

    <!-- 预警窗口：已提报维修申请 -->
    <el-alert
      v-if="!repairAlertClosed && repairApplyCount > 0"
      class="warning-alert"
      title="预警提醒"
      type="warning"
      show-icon
      :closable="true"
      @close="repairAlertClosed = true"
    >
      <div class="warning-alert-msg">
        <span><span class="warning-num">{{ repairApplyCount }}</span>人已提报维修申请，请及时处理。</span>
      </div>
    </el-alert>

    <!-- 总览指标卡片 -->
    <div class="overview-row">
      <div class="overview-card overview-card--blue">
        <div class="overview-icon overview-icon--blue">
          <el-icon :size="20"><Tools /></el-icon>
        </div>
        <div class="overview-text">
          <div class="overview-value">{{ totalRecords }}</div>
          <div class="overview-label">总维修次数</div>
        </div>
      </div>
      <div class="overview-card overview-card--green">
        <div class="overview-icon overview-icon--green">
          <el-icon :size="20"><Wallet /></el-icon>
        </div>
        <div class="overview-text">
          <div class="overview-value">{{ totalInvest }}</div>
          <div class="overview-label">总投入金额</div>
        </div>
      </div>
      <div class="overview-card overview-card--orange">
        <div class="overview-icon overview-icon--orange">
          <el-icon :size="20"><Coin /></el-icon>
        </div>
        <div class="overview-text">
          <div class="overview-value">{{ avgCost }}</div>
          <div class="overview-label">平均单次维修成本</div>
        </div>
      </div>
      <div class="overview-card overview-card--purple">
        <div class="overview-icon overview-icon--purple">
          <el-icon :size="20"><Calendar /></el-icon>
        </div>
        <div class="overview-text">
          <div class="overview-value">{{ currentYearRecords }}</div>
          <div class="overview-label">本年度维修次数</div>
        </div>
      </div>
    </div>

    <!-- 图表网格（BI看板：一行三图） -->
    <div class="chart-grid">
      <div class="chart-card">
        <h4 class="chart-title">年度维修资金趋势</h4>
        <VChart :option="yearTrendOption" autoresize style="height: 200px" />
      </div>
      <div class="chart-card">
        <h4 class="chart-title">维修类型分布</h4>
        <VChart :option="typePieOption" autoresize style="height: 200px" />
      </div>
      <div class="chart-card">
        <h4 class="chart-title">部件维修TOP</h4>
        <VChart :option="componentBarOption" autoresize style="height: 200px" />
      </div>
    </div>

    <!-- 底部主表：维修优先级与寿命预警（全宽、字段对齐） -->
    <div class="table-card full">
      <div class="card-head">
        <h3 class="card-title">维修优先级与寿命预警</h3>
        <div class="head-actions">
          <button class="more-chip" type="button" @click="openRoiDialog">ROI 排行 · {{ roiList.length }}</button>
          <button class="more-chip" type="button" @click="openPlanDialog">维修方案 · {{ maintenancePlan.length }}</button>
          <span class="more-link" @click="openPriorityDialog">全部（{{ priorityList.length }} 条）›</span>
        </div>
      </div>
      <el-table
        :data="priorityTop"
        style="width: 100%; border-top: 1px solid #EEF4F1"
        size="default"
        :cell-style="{ height: '42px', padding: '0', color: '#3D4A45' }"
        :header-cell-style="{ color: '#4A5B54', fontWeight: 600, background: '#F4FAF8' }"
      >
        <el-table-column type="index" label="排序" width="58" align="center" />
        <el-table-column label="烤房编号" width="142" align="center">
          <template #default="{ row }"><span class="barn-ref">{{ row.barnId }}</span></template>
        </el-table-column>
        <el-table-column label="区域" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ [row.county, row.township, row.village].filter(Boolean).join(' ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="healthScore" label="健康分" width="86" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.healthScore >= 75 ? '#2E8B6A' : row.healthScore >= 60 ? '#D4944A' : '#D4604A', fontWeight: 600 }">{{ row.healthScore || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="健康等级" width="98" align="center">
          <template #default="{ row }">
            <el-tag :type="healthLevelTagType(row.healthLevel)" size="small" effect="light">{{ row.healthLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lifeResidual" label="剩余寿命(年)" width="106" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.lifeResidual <= 4 ? '#D4604A' : row.lifeResidual <= 7 ? '#D4944A' : '#2E8B6A', fontWeight: 600 }">
              {{ row.lifeResidual == null ? '-' : row.lifeResidual }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small" effect="light">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priorityScore" label="优先级评分" width="100" align="center" />
        <el-table-column prop="warning" label="预警类型" width="106" align="center" />
      </el-table>
      <el-empty v-if="!priorityList.length" description="暂无预警烤房" :image-size="72" />
    </div>

    <!-- ROI排行 全部弹窗 -->
    <el-dialog v-model="roiDialogVisible" title="维修项目ROI排行（全部）" width="88%" top="6vh" append-to-body>
      <div class="dialog-filter">
        <el-input
          v-model="roiBarnFilter"
          placeholder="按烤房编号 / 部件筛选"
          clearable
          prefix-icon="Search"
          style="width: 260px"
        />
        <span class="filter-total">共 {{ roiFiltered.length }} 条</span>
      </div>
      <el-table :data="roiPaged" style="width: 100%" size="default" max-height="52vh">
        <el-table-column type="index" label="排名" width="56" align="center" />
        <el-table-column label="烤房编号" width="150" align="center">
          <template #default="{ row }"><span class="barn-ref">{{ row.barnId }}</span></template>
        </el-table-column>
        <el-table-column label="区域" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ [row.county, row.township, row.village].filter(Boolean).join(' ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="component" label="主要维修部件" min-width="130" show-overflow-tooltip />
        <el-table-column prop="repairTimes" label="维修次数" width="84" align="center" />
        <el-table-column prop="invest" label="投入金额" width="110" align="center">
          <template #default="{ row }">{{ row.invest ? formatMoney(row.invest) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="lifeGain" label="寿命续期(年)" width="104" align="center">
          <template #default="{ row }"><span style="color:#2E8B6A;font-weight:600">+{{ row.lifeGain }}</span></template>
        </el-table-column>
        <el-table-column prop="healthGain" label="健康加分" width="94" align="center">
          <template #default="{ row }"><span style="color:#1565C0;font-weight:600">+{{ row.healthGain }}</span></template>
        </el-table-column>
        <el-table-column prop="roi" label="ROI" width="84" align="center">
          <template #default="{ row }">
            <span style="color:#0D5D46;font-weight:700"><template v-if="row.roi > 0">{{ row.roi }}x</template><template v-else>-</template></span>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="roiFiltered.length"
          :page-size="roiPageSize"
          :current-page="roiPage"
          @current-change="roiPage = $event"
        />
      </div>
    </el-dialog>

    <!-- 维修优先级与寿命预警 全部弹窗 -->
    <el-dialog v-model="priorityDialogVisible" title="维修优先级与寿命预警（全部）" width="88%" top="6vh" append-to-body>
      <div class="dialog-filter">
        <el-select v-model="priWarnFilter" placeholder="预警类型" clearable style="width: 160px">
          <el-option v-for="t in priWarnOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-input
          v-model="priBarnFilter"
          placeholder="按烤房编号筛选"
          clearable
          prefix-icon="Search"
          style="width: 240px"
        />
        <span class="filter-total">共 {{ priFiltered.length }} 条</span>
      </div>
      <el-table :data="priPaged" style="width: 100%" size="default" max-height="52vh">
        <el-table-column label="烤房编号" width="150" align="center">
          <template #default="{ row }"><span class="barn-ref">{{ row.barnId }}</span></template>
        </el-table-column>
        <el-table-column label="区域" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ [row.county, row.township, row.village].filter(Boolean).join(' ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="healthScore" label="健康分" width="84" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.healthScore >= 75 ? '#2E8B6A' : row.healthScore >= 60 ? '#D4944A' : '#D4604A', fontWeight: 600 }">{{ row.healthScore || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="健康等级" width="84" align="center">
          <template #default="{ row }">
            <el-tag :type="healthLevelTagType(row.healthLevel)" size="small" effect="light">{{ row.healthLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lifeResidual" label="剩余寿命(年)" width="104" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.lifeResidual <= 4 ? '#D4604A' : row.lifeResidual <= 7 ? '#D4944A' : '#2E8B6A', fontWeight: 600 }">{{ row.lifeResidual == null ? '-' : row.lifeResidual }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warnLevel" label="寿命预警" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="warnTagType(row.warnLevel)" size="small" effect="light">{{ row.warnLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="repairTimes" label="维修次数" width="84" align="center" />
        <el-table-column prop="priority" label="优先级" width="84" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small" effect="light">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warning" label="预警类型" width="104" align="center" />
        <el-table-column prop="priorityScore" label="优先级评分" width="96" align="center" />
      </el-table>
      <div class="dialog-pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="priFiltered.length"
          :page-size="priPageSize"
          :current-page="priPage"
          @current-change="priPage = $event"
        />
      </div>
    </el-dialog>

    <!-- 维修方案推荐 全部弹窗 -->
    <el-dialog v-model="planDialogVisible" title="维修方案推荐（全部）" width="80%" top="8vh" append-to-body>
      <div class="dialog-filter">
        <el-input
          v-model="planBarnFilter"
          placeholder="按烤房编号筛选"
          clearable
          prefix-icon="Search"
          style="width: 240px"
        />
        <span class="filter-total">共 {{ planFiltered.length }} 条</span>
      </div>
      <el-table :data="planPaged" style="width: 100%" size="default" max-height="52vh">
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small" effect="light">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="烤房编号" width="150" align="center">
          <template #default="{ row }"><span class="barn-ref">{{ row.barnId }}</span></template>
        </el-table-column>
        <el-table-column label="区域" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ [row.county, row.township, row.village].filter(Boolean).join(' ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="component" label="待维修部件" min-width="120" show-overflow-tooltip />
        <el-table-column prop="lifeResidual" label="剩余寿命(年)" width="100" align="center">
          <template #default="{ row }">{{ row.lifeResidual == null ? '-' : row.lifeResidual }}</template>
        </el-table-column>
        <el-table-column prop="warning" label="预警类型" width="100" align="center" />
        <el-table-column prop="recommendation" label="维修方案建议" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span
              :style="{
                color: row.recommendation.startsWith('【紧急】') ? '#D4604A' : row.recommendation.startsWith('【建议】') ? '#D4944A' : row.recommendation.startsWith('【关注】') ? '#7B1FA2' : '#2E8B6A'
              }"
            >{{ row.recommendation }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="planFiltered.length"
          :page-size="planPageSize"
          :current-page="planPage"
          @current-change="planPage = $event"
        />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ========== 页面标题样式 ========== */
.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(10px, 2vw, 14px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(36px, 3vw, 44px);
  height: clamp(36px, 3vw, 44px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
  font-size: clamp(16px, 1.5vw, 20px);
}

.page-title {
  font-size: clamp(18px, 2vw, 22px);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.2vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(44px, 4vw, 56px);
}

.page-header {
  position: relative;
}

.header-actions {
  position: absolute;
  top: 6px;
  right: 10px;
}

.header-actions .el-button {
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(13, 93, 70, 0.2);
}

.barn-ref {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: #0D5D46;
}

.barn-ref:hover {
  text-decoration: underline;
}

/* BI看板：卡片头部 + 更多入口 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.more-link {
  font-size: 12px;
  color: #0D5D46;
  cursor: pointer;
  user-select: none;
  padding: 2px 10px;
  border: 1px solid rgba(13, 93, 70, 0.35);
  border-radius: 14px;
  transition: all 0.15s;
  line-height: 20px;
  white-space: nowrap;
}

.more-link:hover {
  background: rgba(13, 93, 70, 0.08);
  border-color: #0D5D46;
}

/* 弹窗筛选栏 */
.dialog-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-total {
  font-size: 12px;
  color: var(--text-muted, #8492a6);
}

/* 弹窗底部分页 */
.dialog-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* ========== 预警窗口 ========== */
.warning-alert {
  border-radius: 8px;
  margin-bottom: 16px;
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

/* ========== 总览指标卡片 ========== */
.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.overview-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.25s ease;
}

.overview-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.overview-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.overview-icon--blue {
  background: linear-gradient(135deg, #1565C0, #1976D2);
}
.overview-icon--blue .el-icon {
  color: #FFFFFF;
}

.overview-icon--green {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
}
.overview-icon--green .el-icon {
  color: #FFFFFF;
}

.overview-icon--orange {
  background: linear-gradient(135deg, #D4944A, #E8A95E);
}
.overview-icon--orange .el-icon {
  color: #FFFFFF;
}

.overview-icon--purple {
  background: linear-gradient(135deg, #7B1FA2, #9C27B0);
}
.overview-icon--purple .el-icon {
  color: #FFFFFF;
}

.overview-text {
  flex: 1;
  min-width: 0;
}

.overview-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  word-break: break-all;
}

.overview-card--blue .overview-value { color: #1565C0; }
.overview-card--green .overview-value { color: #0D5D46; }
.overview-card--orange .overview-value { color: #D4944A; }
.overview-card--purple .overview-value { color: #7B1FA2; }

.overview-label {
  font-size: 12px;
  color: #6B7280;
  margin-top: 2px;
}

/* ========== 图表网格（BI看板：一行三图） ========== */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.chart-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 12px 14px 8px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.25s ease;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.chart-title {
  font-size: 13px;
  font-weight: 600;
  color: #0D5D46;
  margin: 0 0 6px;
}

/* ========== 底部主表 ========== */
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.more-chip {
  font-size: 12px;
  color: #0D5D46;
  background: rgba(13, 93, 70, 0.07);
  border: 1px solid rgba(13, 93, 70, 0.28);
  border-radius: 14px;
  padding: 2px 12px;
  cursor: pointer;
  transition: all 0.15s;
  line-height: 20px;
  white-space: nowrap;
}

.more-chip:hover {
  background: rgba(13, 93, 70, 0.14);
  border-color: #0D5D46;
}

/* ========== 表格卡片 ========== */
.table-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 0;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.25s ease;
}

.table-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 10px;
  padding-left: 10px;
  border-left: 3px solid #0A4D3E;
}

/* ========== 表格紧凑样式 ========== */
:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table .el-table__cell) {
  padding: 6px 0;
}

/* 表格内取消烤房编号方框样式 */
.table-card :deep(.barn-code) {
  background: none;
  border: none;
  padding: 0;
  border-radius: 0;
}

/* ========== 响应式适配 ========== */
@media (max-width: 1200px) {
  .overview-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-row {
    grid-template-columns: 1fr;
  }
}
</style>
