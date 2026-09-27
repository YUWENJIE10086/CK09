<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, View, Search, RefreshRight } from '@element-plus/icons-vue'
import DataCard from '@/components/DataCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import BarnCode from '@/components/BarnCode.vue'
import { getAnalysisOverview, getHealthAnalysis } from '@/api/ai'
import { getBarnAssignList } from '@/api/barn'

// ========== 总览数据 ==========
const overview = ref<any>({})
const overviewLoading = ref(false)

// ========== 烤房列表 ==========
const tableData = ref<any[]>([])
const loading = ref(false)
const pagination = ref({ pageNum: 1, pageSize: 10, total: 0 })
const filterKeyword = ref('')

// ========== 算法名称映射 ==========
const algoLabelMap: Record<string, string> = {
  bmhi: 'BMHI',
  fche: 'FCHE',
  topsis: 'TOPSIS',
  cdci: 'CDCI',
  crhe: 'CRHE',
  edrl: 'EDRL',
  wrrl: 'WRRL',
  bdrl: 'BDRL',
  gple: 'GPLE',
}

// ========== 统计卡片 ==========
const statCards = computed(() => {
  const hs = overview.value.healthStats || {}
  const ls = overview.value.lifeStats || {}
  return [
    { title: '烤房总数', value: hs.total || 0, icon: 'OfficeBuilding', color: '#1A6B4F' },
    { title: '已计算健康分', value: hs.calculated || 0, icon: 'CircleCheck', color: '#2E8B6A' },
    { title: '平均健康分', value: hs.avg_score ?? '-', icon: 'TrendCharts', color: '#0D5D46' },
    { title: '平均剩余寿命(年)', value: ls.avg_life ?? '-', icon: 'Timer', color: '#5A8A7A' },
  ]
})

// ========== 健康分布 ==========
const healthDistribution = computed(() => overview.value.healthDistribution || [])
const lifeDistribution = computed(() => overview.value.lifeDistribution || [])

// ========== 算法对比 ==========
const algoComparison = computed(() => overview.value.algoComparison || {})

// ========== 加载总览 ==========
async function loadOverview() {
  overviewLoading.value = true
  try {
    overview.value = await getAnalysisOverview()
  } catch {
    // 错误已在拦截器处理
  } finally {
    overviewLoading.value = false
  }
}

// ========== 加载烤房列表 ==========
async function loadData() {
  loading.value = true
  try {
    const res: any = await getBarnAssignList({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      keyword: filterKeyword.value || undefined,
    })
    const rows = res?.rows || []
    tableData.value = rows.map((r: any) => ({
      ...r,
      barnName: r.barnName || r.id,
    }))
    pagination.value.total = res?.total ?? rows.length
  } catch {
    ElMessage.error('加载列表失败')
    tableData.value = []
    pagination.value.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.pageNum = 1
  loadData()
}

function handlePageChange(page: number) {
  pagination.value.pageNum = page
  loadData()
}

// ========== 健康分颜色 ==========
function getScoreColor(score: any): string {
  const s = Number(score)
  if (isNaN(s)) return '#94A3B8'
  if (s >= 85) return '#2E8B6A'
  if (s >= 70) return '#D4944A'
  if (s >= 50) return '#D4604A'
  return '#94A3B8'
}

function getLifeColor(life: any): string {
  const l = Number(life)
  if (isNaN(l)) return '#94A3B8'
  if (l <= 1) return '#D4604A'
  if (l <= 3) return '#D4944A'
  if (l <= 5) return '#5A8A7A'
  return '#2E8B6A'
}

function calculateHealthLevel(score: any): string {
  const s = Number(score)
  if (isNaN(s)) return 'retired'
  if (s >= 90) return 'excellent'
  if (s >= 75) return 'maintenance'
  if (s >= 60) return 'urgent'
  return 'retired'
}

// ========== 详情弹窗 ==========
const detailDialog = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>(null)

async function handleViewDetail(row: any) {
  detailDialog.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getHealthAnalysis(row.projectId || row.id)
    detailData.value = res
  } catch {
    ElMessage.error('加载健康分析详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ========== 详情数据计算 ==========
const detailBase = computed(() => detailData.value?.base || {})
const detailHealthScores = computed(() => {
  const scores = detailData.value?.healthScores || {}
  return Object.entries(scores).map(([key, val]: [string, any]) => ({
    algo: key,
    label: val?.label || algoLabelMap[key] || key,
    score: val?.score,
    isCurrent: detailBase.value.current_health_algo === key,
  }))
})
const detailLifeScores = computed(() => {
  const scores = detailData.value?.lifeScores || {}
  return Object.entries(scores).map(([key, val]: [string, any]) => ({
    algo: key,
    label: val?.label || algoLabelMap[key] || key,
    residual: val?.residual,
    isCurrent: detailBase.value.current_life_algo === key,
  }))
})
const detailComponents = computed(() => detailData.value?.components || {})
const detailBmhiBreakdown = computed(() => detailData.value?.bmhiBreakdown || null)

// 部件状态颜色
function statusColor(status: string): string {
  if (status === '正常') return '#2E8B6A'
  if (status === '缺失' || status === '损坏') return '#D4604A'
  return '#D4944A'
}

// 初始化
onMounted(() => {
  loadOverview()
  loadData()
})
</script>

<template>
  <div class="page-container health-analysis-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><DataAnalysis /></el-icon>
        </div>
        <h2 class="page-title">健康分析</h2>
      </div>
      <p class="page-desc">烤房健康分总览、算法对比与部件级健康诊断分析</p>
    </div>

    <!-- 顶部统计卡片 -->
    <div class="stat-row" v-loading="overviewLoading">
      <DataCard
        v-for="card in statCards"
        :key="card.title"
        :title="card.title"
        :value="card.value"
        :icon="card.icon"
        :color="card.color"
      />
    </div>

    <!-- 当前算法 + 分布统计 -->
    <div class="overview-row">
      <!-- 健康分布 -->
      <div class="glass-card dist-card">
        <div class="card-header">
          <h3 class="card-title">健康等级分布</h3>
          <el-tag size="small" type="success">
            当前算法: {{ algoLabelMap[overview.currentHealthAlgo] || overview.currentHealthAlgo || 'BMHI' }}
          </el-tag>
        </div>
        <div class="dist-list">
          <div v-for="item in healthDistribution" :key="item.level" class="dist-item">
            <span class="dist-label">{{ item.level }}</span>
            <div class="dist-bar-wrap">
              <div class="dist-bar" :style="{
                width: (overview.healthStats?.total ? (item.cnt / overview.healthStats.total) * 100 : 0) + '%',
                background: item.level === '优良' ? '#2E8B6A' : item.level === '良好' ? '#5A8A7A' : item.level === '一般' ? '#D4944A' : item.level === '较差' ? '#D4604A' : '#C0C4CC'
              }"></div>
            </div>
            <span class="dist-count">{{ item.cnt }}</span>
          </div>
        </div>
      </div>

      <!-- 寿命分布 -->
      <div class="glass-card dist-card">
        <div class="card-header">
          <h3 class="card-title">寿命预警分布</h3>
          <el-tag size="small" type="warning">
            当前算法: {{ algoLabelMap[overview.currentLifeAlgo] || overview.currentLifeAlgo || 'BDRL' }}
          </el-tag>
        </div>
        <div class="dist-list">
          <div v-for="item in lifeDistribution" :key="item.level" class="dist-item">
            <span class="dist-label">{{ item.level }}</span>
            <div class="dist-bar-wrap">
              <div class="dist-bar" :style="{
                width: (overview.lifeStats?.calculated ? (item.cnt / overview.lifeStats.calculated) * 100 : 0) + '%',
                background: item.level === '正常' ? '#2E8B6A' : item.level === '需关注' ? '#5A8A7A' : item.level === '需维修' ? '#D4944A' : item.level === '紧急维修' ? '#D4604A' : '#C0C4CC'
              }"></div>
            </div>
            <span class="dist-count">{{ item.cnt }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 算法对比表 -->
    <div class="glass-card">
      <div class="card-header">
        <h3 class="card-title">五种健康分算法对比</h3>
      </div>
      <el-table :data="Object.entries(algoComparison).map(([k, v]: [string, any]) => ({ algo: k, ...v }))" size="small" style="width: 100%">
        <el-table-column label="算法" width="120" align="center">
          <template #default="{ row }">
            <span style="font-weight: 600; color: #0D5D46">{{ algoLabelMap[row.algo] || row.algo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avg" label="平均分" width="100" align="center" />
        <el-table-column prop="min" label="最低分" width="100" align="center" />
        <el-table-column prop="max" label="最高分" width="100" align="center" />
        <el-table-column label="分值范围" align="center">
          <template #default="{ row }">
            <span style="color: #909399">{{ row.min }} ~ {{ row.max }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 烤房列表 -->
    <div class="glass-card">
      <div class="card-header">
        <h3 class="card-title">烤房健康分列表</h3>
        <div class="header-actions">
          <el-input
            v-model="filterKeyword"
            placeholder="搜索烤房编号/名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="() => { filterKeyword = ''; pagination.pageNum = 1; loadData() }">重置</el-button>
        </div>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column label="烤房编号" width="160" align="center">
          <template #default="{ row }"><BarnCode :code="row.id" /></template>
        </el-table-column>
        <el-table-column prop="county" label="县区" width="90" align="center" />
        <el-table-column prop="township" label="乡镇" width="90" align="center" />
        <el-table-column label="健康等级" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.currentHealthScore ? calculateHealthLevel(row.currentHealthScore) : 'retired'" type="health" />
          </template>
        </el-table-column>
        <el-table-column label="健康分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.currentHealthScore != null" :style="{ color: getScoreColor(row.currentHealthScore), fontWeight: 700, fontSize: '15px' }">
              {{ row.currentHealthScore }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余寿命(年)" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.currentLifeResidual != null" :style="{ color: getLifeColor(row.currentLifeResidual), fontWeight: 700 }">
              {{ row.currentLifeResidual }}
            </span>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="健康算法" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="success" v-if="row.currentHealthAlgo">{{ algoLabelMap[row.currentHealthAlgo] || row.currentHealthAlgo }}</el-tag>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="寿命算法" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="warning" v-if="row.currentLifeAlgo">{{ algoLabelMap[row.currentLifeAlgo] || row.currentLifeAlgo }}</el-tag>
            <span v-else style="color: #C0C4CC">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="View" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailDialog"
      title="健康分析详情"
      width="900px"
      destroy-on-close
      append-to-body
      align-center
      class="detail-dialog"
    >
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <!-- 基础信息 -->
          <el-descriptions :column="3" border size="small" class="detail-desc">
            <el-descriptions-item label="烤房编号">{{ detailBase.project_id }}</el-descriptions-item>
            <el-descriptions-item label="县区">{{ detailBase.county }}</el-descriptions-item>
            <el-descriptions-item label="乡镇">{{ detailBase.township }}</el-descriptions-item>
            <el-descriptions-item label="使用状态">{{ detailBase.use_status }}</el-descriptions-item>
            <el-descriptions-item label="竣工日期">{{ detailBase.finish_date }}</el-descriptions-item>
            <el-descriptions-item label="启用日期">{{ detailBase.start_date }}</el-descriptions-item>
            <el-descriptions-item label="当前健康分">
              <span :style="{ color: getScoreColor(detailBase.current_health_score), fontWeight: 700, fontSize: '16px' }">
                {{ detailBase.current_health_score ?? '-' }}
              </span>
              <el-tag size="small" type="success" style="margin-left: 8px" v-if="detailBase.current_health_algo">
                {{ algoLabelMap[detailBase.current_health_algo] || detailBase.current_health_algo }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="剩余寿命(年)">
              <span :style="{ color: getLifeColor(detailBase.current_life_residual), fontWeight: 700, fontSize: '16px' }">
                {{ detailBase.current_life_residual ?? '-' }}
              </span>
              <el-tag size="small" type="warning" style="margin-left: 8px" v-if="detailBase.current_life_algo">
                {{ algoLabelMap[detailBase.current_life_algo] || detailBase.current_life_algo }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 健康分算法对比 -->
          <h4 class="section-title">健康分算法对比</h4>
          <el-table :data="detailHealthScores" size="small" style="width: 100%">
            <el-table-column label="算法" min-width="200">
              <template #default="{ row }">
                <span style="font-weight: 600">{{ row.label }}</span>
                <el-tag v-if="row.isCurrent" size="small" type="success" style="margin-left: 8px">当前</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="健康分" width="120" align="center">
              <template #default="{ row }">
                <span v-if="row.score != null" :style="{ color: getScoreColor(row.score), fontWeight: 700, fontSize: '15px' }">{{ row.score }}</span>
                <span v-else style="color: #C0C4CC">未计算</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 寿命预测对比 -->
          <h4 class="section-title">寿命预测算法对比</h4>
          <el-table :data="detailLifeScores" size="small" style="width: 100%">
            <el-table-column label="算法" min-width="200">
              <template #default="{ row }">
                <span style="font-weight: 600">{{ row.label }}</span>
                <el-tag v-if="row.isCurrent" size="small" type="warning" style="margin-left: 8px">当前</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="剩余寿命(年)" width="120" align="center">
              <template #default="{ row }">
                <span v-if="row.residual != null" :style="{ color: getLifeColor(row.residual), fontWeight: 700, fontSize: '15px' }">{{ row.residual }}</span>
                <span v-else style="color: #C0C4CC">未计算</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- BMHI 计算分解 -->
          <template v-if="detailBmhiBreakdown">
            <h4 class="section-title">BMHI 健康分计算分解</h4>
            <div class="bmhi-breakdown">
              <div class="breakdown-formula">
                <el-icon color="#0D5D46" :size="16"><DataAnalysis /></el-icon>
                <span>{{ detailBmhiBreakdown.formula }}</span>
              </div>
              <div class="breakdown-calc">{{ detailBmhiBreakdown.calculation }}</div>
              <div class="breakdown-items">
                <div class="breakdown-item" v-for="key in ['JR_加热设备', 'SR_散热器', 'ZK_自控设备', 'ZT_烤房主体', 'FS_附属设施']" :key="key">
                  <span class="bk-label">{{ key }}</span>
                  <span class="bk-value" :style="{ color: getScoreColor(detailBmhiBreakdown[key]) }">{{ detailBmhiBreakdown[key] }}</span>
                </div>
              </div>
            </div>
          </template>

          <!-- 部件状态详情 -->
          <h4 class="section-title">部件状态详情</h4>
          <div class="component-grid">
            <!-- JR 加热设备 -->
            <div class="comp-card" v-if="detailComponents.JR">
              <div class="comp-title">JR - 加热设备</div>
              <div class="comp-body">
                <div class="comp-row">
                  <span class="comp-label">燃烧器状态:</span>
                  <span :style="{ color: statusColor(detailComponents.JR.burner_status), fontWeight: 600 }">{{ detailComponents.JR.burner_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">修复年份:</span>
                  <span>{{ detailComponents.JR.burner_repair_year || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">评价:</span>
                  <span>{{ detailComponents.JR.evaluation || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- SR 散热器 -->
            <div class="comp-card" v-if="detailComponents.SR">
              <div class="comp-title">SR - 散热器</div>
              <div class="comp-body">
                <div class="comp-row">
                  <span class="comp-label">散热管:</span>
                  <span :style="{ color: statusColor(detailComponents.SR.pipe_status), fontWeight: 600 }">{{ detailComponents.SR.pipe_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">炉膛:</span>
                  <span :style="{ color: statusColor(detailComponents.SR.furnace_status), fontWeight: 600 }">{{ detailComponents.SR.furnace_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">清灰门:</span>
                  <span :style="{ color: statusColor(detailComponents.SR.ash_door_status), fontWeight: 600 }">{{ detailComponents.SR.ash_door_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">烟囱:</span>
                  <span :style="{ color: statusColor(detailComponents.SR.chimney_status), fontWeight: 600 }">{{ detailComponents.SR.chimney_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">评价:</span>
                  <span>{{ detailComponents.SR.evaluation || '-' }}</span>
                </div>
              </div>
            </div>

            <!-- ZK 自控设备 -->
            <div class="comp-card" v-if="detailComponents.ZK">
              <div class="comp-title">ZK - 自控设备</div>
              <div class="comp-body comp-body-grid">
                <div class="comp-row"><span class="comp-label">冷风门:</span><span :style="{ color: statusColor(detailComponents.ZK.cold_door_status) }">{{ detailComponents.ZK.cold_door_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">冷风门电机:</span><span :style="{ color: statusColor(detailComponents.ZK.cold_door_motor_status) }">{{ detailComponents.ZK.cold_door_motor_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">排湿窗:</span><span :style="{ color: statusColor(detailComponents.ZK.exhaust_window_status) }">{{ detailComponents.ZK.exhaust_window_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">循环风机:</span><span :style="{ color: statusColor(detailComponents.ZK.circulation_fan_status) }">{{ detailComponents.ZK.circulation_fan_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">燃烧风机:</span><span :style="{ color: statusColor(detailComponents.ZK.combustion_fan_status) }">{{ detailComponents.ZK.combustion_fan_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">操作箱:</span><span :style="{ color: statusColor(detailComponents.ZK.control_box_status) }">{{ detailComponents.ZK.control_box_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">水壶:</span><span :style="{ color: statusColor(detailComponents.ZK.water_pot_status) }">{{ detailComponents.ZK.water_pot_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">探头:</span><span :style="{ color: statusColor(detailComponents.ZK.probe_status) }">{{ detailComponents.ZK.probe_status || '-' }}</span></div>
                <div class="comp-row"><span class="comp-label">评价:</span><span>{{ detailComponents.ZK.evaluation || '-' }}</span></div>
              </div>
            </div>

            <!-- ZT 烤房主体 -->
            <div class="comp-card" v-if="detailComponents.ZT">
              <div class="comp-title">ZT - 烤房主体</div>
              <div class="comp-body">
                <div class="comp-row">
                  <span class="comp-label">墙体屋顶:</span>
                  <span :style="{ color: statusColor(detailComponents.ZT.wall_roof_status), fontWeight: 600 }">{{ detailComponents.ZT.wall_roof_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">挂烟梁柱:</span>
                  <span :style="{ color: statusColor(detailComponents.ZT.beam_status), fontWeight: 600 }">{{ detailComponents.ZT.beam_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">装烟室门:</span>
                  <span :style="{ color: statusColor(detailComponents.ZT.door_status), fontWeight: 600 }">{{ detailComponents.ZT.door_status || '-' }}</span>
                </div>
                <div class="comp-row">
                  <span class="comp-label">评价:</span>
                  <span>{{ detailComponents.ZT.evaluation || '-' }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.health-analysis-page {
  padding: 12px;
  min-height: 100%;
}

/* 页面标题 */
.page-header {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: clamp(10px, 1.5vw, 14px);
  margin-bottom: 6px;
}

.page-title-icon {
  width: clamp(32px, 4vw, 40px);
  height: clamp(32px, 4vw, 40px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.3);
}

.page-title-icon .el-icon {
  color: #FFFFFF;
}

.page-title {
  font-size: clamp(18px, 2.2vw, 22px);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.4vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(40px, 5vw, 52px);
}

/* 统计卡片行 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: clamp(12px, 1.5vw, 18px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

/* 毛玻璃卡片 */
.glass-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  margin-bottom: clamp(16px, 2vw, 24px);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.glass-card:hover {
  box-shadow:
    0 8px 24px rgba(10, 77, 62, 0.1),
    0 16px 48px rgba(10, 77, 62, 0.08),
    0 0 0 1px rgba(10, 77, 62, 0.1);
  transform: translateY(-3px);
}

/* 概览行 */
.overview-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: clamp(12px, 1.5vw, 18px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

/* 分布卡片 */
.dist-card {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: clamp(14px, 1.5vw, 18px) clamp(16px, 2vw, 24px) 0;
}

.card-title {
  font-size: clamp(14px, 1.6vw, 16px);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: clamp(10px, 1.2vw, 14px);
  border-left: 3px solid #0A4D3E;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 分布列表 */
.dist-list {
  padding: clamp(12px, 1.5vw, 18px) clamp(16px, 2vw, 24px);
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.dist-label {
  width: 70px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.dist-bar-wrap {
  flex: 1;
  height: 20px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: 8px;
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.dist-count {
  width: 30px;
  text-align: right;
  font-weight: 700;
  font-size: 14px;
  color: var(--text-primary);
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: clamp(12px, 1.5vw, 18px) clamp(16px, 2vw, 24px);
}

/* 详情弹窗 */
.detail-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-desc {
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #0D5D46;
  margin: 20px 0 10px;
  padding-left: 10px;
  border-left: 3px solid #0D5D46;
}

/* BMHI 分解 */
.bmhi-breakdown {
  background: linear-gradient(135deg, rgba(13, 93, 70, 0.05) 0%, rgba(26, 107, 79, 0.03) 100%);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid rgba(13, 93, 70, 0.1);
}

.breakdown-formula {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0D5D46;
  margin-bottom: 8px;
}

.breakdown-calc {
  font-size: 13px;
  color: var(--text-secondary);
  font-family: 'Courier New', monospace;
  background: rgba(255, 255, 255, 0.6);
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.breakdown-items {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.breakdown-item {
  text-align: center;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 10px 6px;
}

.bk-label {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.bk-value {
  display: block;
  font-size: 18px;
  font-weight: 800;
}

/* 部件卡片网格 */
.component-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.comp-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0.6) 100%);
  border: 1px solid rgba(13, 93, 70, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.comp-title {
  background: linear-gradient(135deg, rgba(13, 93, 70, 0.08) 0%, rgba(26, 107, 79, 0.05) 100%);
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 700;
  color: #0D5D46;
  border-bottom: 1px solid rgba(13, 93, 70, 0.08);
}

.comp-body {
  padding: 10px 12px;
}

.comp-body-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
}

.comp-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 3px 0;
}

.comp-label {
  color: var(--text-muted);
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .overview-row {
    grid-template-columns: 1fr;
  }
  .breakdown-items {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr;
  }
  .component-grid {
    grid-template-columns: 1fr;
  }
  .breakdown-items {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
