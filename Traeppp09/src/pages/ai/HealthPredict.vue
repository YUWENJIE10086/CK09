<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, Search, WarningFilled, Tools, DataLine } from '@element-plus/icons-vue'
import { getHealthAnalysis } from '@/api/ai'
import { getBarnAssignList } from '@/api/barn'

// ========== 烤房选择 ==========
const barnOptions = ref<any[]>([])
const selectedBarnId = ref<string>('')
const loading = ref(false)
const analysisData = ref<any>(null)

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

// ========== 加载烤房选项 ==========
async function loadBarnOptions() {
  try {
    const res: any = await getBarnAssignList({ pageNum: 1, pageSize: 500 })
    barnOptions.value = (res?.rows || []).map((r: any) => ({
      id: r.projectId || r.id,
      county: r.county,
      township: r.township,
      currentHealthScore: r.currentHealthScore,
      currentLifeResidual: r.currentLifeResidual,
      currentHealthAlgo: r.currentHealthAlgo,
      currentLifeAlgo: r.currentLifeAlgo,
    }))
  } catch {
    barnOptions.value = []
  }
}

// ========== 开始分析 ==========
async function handleAnalyze() {
  if (!selectedBarnId.value) {
    ElMessage.warning('请先选择烤房')
    return
  }
  loading.value = true
  analysisData.value = null
  try {
    analysisData.value = await getHealthAnalysis(selectedBarnId.value)
  } catch {
    ElMessage.error('加载预测数据失败')
  } finally {
    loading.value = false
  }
}

// ========== 数据计算 ==========
const baseInfo = computed(() => analysisData.value?.base || {})
const healthScores = computed(() => {
  const scores = analysisData.value?.healthScores || {}
  return Object.entries(scores).map(([key, val]: [string, any]) => ({
    algo: key,
    label: val?.label || algoLabelMap[key] || key,
    score: val?.score,
    isCurrent: baseInfo.value.current_health_algo === key,
  })).filter((s: any) => s.score != null)
})
const lifeScores = computed(() => {
  const scores = analysisData.value?.lifeScores || {}
  return Object.entries(scores).map(([key, val]: [string, any]) => ({
    algo: key,
    label: val?.label || algoLabelMap[key] || key,
    residual: val?.residual,
    isCurrent: baseInfo.value.current_life_algo === key,
  })).filter((s: any) => s.residual != null)
})
const components = computed(() => analysisData.value?.components || {})
const bmhiBreakdown = computed(() => analysisData.value?.bmhiBreakdown || null)

// ========== 预测指标 ==========
const currentHealthScore = computed(() => baseInfo.value.current_health_score)
const currentLifeResidual = computed(() => baseInfo.value.current_life_residual)
const currentHealthAlgo = computed(() => baseInfo.value.current_health_algo)
const currentLifeAlgo = computed(() => baseInfo.value.current_life_algo)

// 风险等级判定
const riskLevel = computed(() => {
  const score = Number(currentHealthScore.value)
  const life = Number(currentLifeResidual.value)
  if (isNaN(score) && isNaN(life)) return { level: '未知', color: '#94A3B8', desc: '暂无足够数据进行分析' }
  if ((!isNaN(life) && life <= 1) || (!isNaN(score) && score < 50)) {
    return { level: '极高风险', color: '#D4604A', desc: '烤房健康状况严重恶化，需立即维修或考虑退役' }
  }
  if ((!isNaN(life) && life <= 3) || (!isNaN(score) && score < 70)) {
    return { level: '高风险', color: '#D4944A', desc: '烤房健康状况明显退化，建议尽快安排维修' }
  }
  if ((!isNaN(life) && life <= 5) || (!isNaN(score) && score < 85)) {
    return { level: '中风险', color: '#5A8A7A', desc: '烤房健康状况有所下降，建议关注并制定维护计划' }
  }
  return { level: '低风险', color: '#2E8B6A', desc: '烤房健康状况良好，保持常规巡检即可' }
})

// 维修建议
const maintenanceAdvice = computed(() => {
  const advice: { priority: string; component: string; action: string; urgency: string }[] = []
  const comp = components.value

  // 检查各部件状态
  if (comp.JR?.burner_status && comp.JR.burner_status !== '正常') {
    advice.push({ priority: '1', component: 'JR-燃烧器', action: `状态为"${comp.JR.burner_status}"，需检查或更换`, urgency: comp.JR.burner_status === '损坏' ? '紧急' : '尽快' })
  }
  if (comp.SR) {
    const srIssues = [
      comp.SR.pipe_status && comp.SR.pipe_status !== '正常' ? '散热管' : null,
      comp.SR.furnace_status && comp.SR.furnace_status !== '正常' ? '炉膛' : null,
      comp.SR.ash_door_status && comp.SR.ash_door_status !== '正常' ? '清灰门' : null,
      comp.SR.chimney_status && comp.SR.chimney_status !== '正常' ? '烟囱' : null,
    ].filter(Boolean)
    if (srIssues.length > 0) {
      advice.push({ priority: '2', component: `SR-散热器(${srIssues.join('/')})`, action: `存在异常部件，需维修`, urgency: srIssues.length > 2 ? '紧急' : '尽快' })
    }
  }
  if (comp.ZK) {
    const zkIssues: string[] = []
    if (comp.ZK.cold_door_status && comp.ZK.cold_door_status !== '正常') zkIssues.push('冷风门')
    if (comp.ZK.cold_door_motor_status && comp.ZK.cold_door_motor_status !== '正常') zkIssues.push('冷风门电机')
    if (comp.ZK.exhaust_window_status && comp.ZK.exhaust_window_status !== '正常') zkIssues.push('排湿窗')
    if (comp.ZK.circulation_fan_status && comp.ZK.circulation_fan_status !== '正常') zkIssues.push('循环风机')
    if (comp.ZK.combustion_fan_status && comp.ZK.combustion_fan_status !== '正常') zkIssues.push('燃烧风机')
    if (comp.ZK.control_box_status && comp.ZK.control_box_status !== '正常') zkIssues.push('操作箱')
    if (comp.ZK.water_pot_status && comp.ZK.water_pot_status !== '正常') zkIssues.push('水壶')
    if (comp.ZK.probe_status && comp.ZK.probe_status !== '正常') zkIssues.push('探头')
    if (zkIssues.length > 0) {
      advice.push({ priority: '3', component: `ZK-自控设备(${zkIssues.join('/')})`, action: `${zkIssues.length}个部件异常，需维修`, urgency: zkIssues.length > 3 ? '紧急' : '尽快' })
    }
  }
  if (comp.ZT) {
    const ztIssues: string[] = []
    if (comp.ZT.wall_roof_status && comp.ZT.wall_roof_status !== '正常') ztIssues.push('墙体屋顶')
    if (comp.ZT.beam_status && comp.ZT.beam_status !== '正常') ztIssues.push('挂烟梁柱')
    if (comp.ZT.door_status && comp.ZT.door_status !== '正常') ztIssues.push('装烟室门')
    if (ztIssues.length > 0) {
      advice.push({ priority: '4', component: `ZT-烤房主体(${ztIssues.join('/')})`, action: '主体结构异常，影响安全', urgency: '紧急' })
    }
  }

  // 寿命预警
  const life = Number(currentLifeResidual.value)
  if (!isNaN(life) && life <= 3) {
    advice.unshift({ priority: '0', component: '整体寿命', action: `剩余寿命仅${life}年，建议制定退役/大修计划`, urgency: life <= 1 ? '紧急' : '尽快' })
  }

  return advice.length > 0 ? advice : [{ priority: '-', component: '全部部件', action: '各部件状态正常，保持常规巡检', urgency: '常规' }]
})

// 部件风险排名
const componentRiskRanking = computed(() => {
  const ranking: { component: string; status: string; riskScore: number; level: string }[] = []
  const comp = components.value

  const calcRisk = (status: string): number => {
    if (status === '损坏' || status === '缺失') return 100
    if (status === '异常') return 70
    if (status === '正常') return 10
    return 30
  }

  if (comp.JR?.burner_status) {
    const r = calcRisk(comp.JR.burner_status)
    ranking.push({ component: 'JR-燃烧器', status: comp.JR.burner_status, riskScore: r, level: r >= 70 ? '高风险' : r >= 30 ? '中风险' : '低风险' })
  }
  if (comp.SR) {
    const parts = [
      { name: 'SR-散热管', status: comp.SR.pipe_status },
      { name: 'SR-炉膛', status: comp.SR.furnace_status },
      { name: 'SR-清灰门', status: comp.SR.ash_door_status },
      { name: 'SR-烟囱', status: comp.SR.chimney_status },
    ]
    parts.forEach(p => {
      if (p.status) {
        const r = calcRisk(p.status)
        ranking.push({ component: p.name, status: p.status, riskScore: r, level: r >= 70 ? '高风险' : r >= 30 ? '中风险' : '低风险' })
      }
    })
  }
  if (comp.ZK) {
    const parts = [
      { name: 'ZK-冷风门', status: comp.ZK.cold_door_status },
      { name: 'ZK-冷风门电机', status: comp.ZK.cold_door_motor_status },
      { name: 'ZK-排湿窗', status: comp.ZK.exhaust_window_status },
      { name: 'ZK-循环风机', status: comp.ZK.circulation_fan_status },
      { name: 'ZK-燃烧风机', status: comp.ZK.combustion_fan_status },
      { name: 'ZK-操作箱', status: comp.ZK.control_box_status },
      { name: 'ZK-水壶', status: comp.ZK.water_pot_status },
      { name: 'ZK-探头', status: comp.ZK.probe_status },
    ]
    parts.forEach(p => {
      if (p.status) {
        const r = calcRisk(p.status)
        ranking.push({ component: p.name, status: p.status, riskScore: r, level: r >= 70 ? '高风险' : r >= 30 ? '中风险' : '低风险' })
      }
    })
  }
  if (comp.ZT) {
    const parts = [
      { name: 'ZT-墙体屋顶', status: comp.ZT.wall_roof_status },
      { name: 'ZT-挂烟梁柱', status: comp.ZT.beam_status },
      { name: 'ZT-装烟室门', status: comp.ZT.door_status },
    ]
    parts.forEach(p => {
      if (p.status) {
        const r = calcRisk(p.status)
        ranking.push({ component: p.name, status: p.status, riskScore: r, level: r >= 70 ? '高风险' : r >= 30 ? '中风险' : '低风险' })
      }
    })
  }

  return ranking.sort((a, b) => b.riskScore - a.riskScore).slice(0, 10)
})

// 颜色函数
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
function getRiskColor(level: string): string {
  if (level === '高风险') return '#D4604A'
  if (level === '中风险') return '#D4944A'
  return '#2E8B6A'
}

onMounted(() => {
  loadBarnOptions()
})
</script>

<template>
  <div class="page-container health-predict-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><TrendCharts /></el-icon>
        </div>
        <h2 class="page-title">健康状态预测</h2>
      </div>
      <p class="page-desc">基于多算法融合的健康分与寿命预测，进行风险等级评估与维修建议</p>
    </div>

    <!-- 选择区 -->
    <div class="glass-card select-card">
      <el-form inline>
        <el-form-item label="选择烤房">
          <el-select
            v-model="selectedBarnId"
            placeholder="请选择烤房"
            filterable
            style="width: 360px"
          >
            <el-option
              v-for="b in barnOptions"
              :key="b.id"
              :label="`${b.id} (${b.county || ''} ${b.township || ''})`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :icon="Search" @click="handleAnalyze">开始预测分析</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 预测结果 -->
    <div v-if="analysisData" v-loading="loading">
      <!-- 核心指标 -->
      <div class="metric-row">
        <div class="metric-card" :style="{ borderTopColor: getScoreColor(currentHealthScore) }">
          <div class="metric-icon" :style="{ background: getScoreColor(currentHealthScore) + '20' }">
            <el-icon :size="24" :color="getScoreColor(currentHealthScore)"><DataLine /></el-icon>
          </div>
          <div class="metric-info">
            <div class="metric-value" :style="{ color: getScoreColor(currentHealthScore) }">
              {{ currentHealthScore ?? '-' }}
            </div>
            <div class="metric-label">当前健康分</div>
            <el-tag size="small" type="success" v-if="currentHealthAlgo" style="margin-top: 4px">
              {{ algoLabelMap[currentHealthAlgo] || currentHealthAlgo }}
            </el-tag>
          </div>
        </div>

        <div class="metric-card" :style="{ borderTopColor: getLifeColor(currentLifeResidual) }">
          <div class="metric-icon" :style="{ background: getLifeColor(currentLifeResidual) + '20' }">
            <el-icon :size="24" :color="getLifeColor(currentLifeResidual)"><Timer /></el-icon>
          </div>
          <div class="metric-info">
            <div class="metric-value" :style="{ color: getLifeColor(currentLifeResidual) }">
              {{ currentLifeResidual ?? '-' }}<span class="metric-unit">年</span>
            </div>
            <div class="metric-label">剩余寿命</div>
            <el-tag size="small" type="warning" v-if="currentLifeAlgo" style="margin-top: 4px">
              {{ algoLabelMap[currentLifeAlgo] || currentLifeAlgo }}
            </el-tag>
          </div>
        </div>

        <div class="metric-card" :style="{ borderTopColor: riskLevel.color }">
          <div class="metric-icon" :style="{ background: riskLevel.color + '20' }">
            <el-icon :size="24" :color="riskLevel.color"><WarningFilled /></el-icon>
          </div>
          <div class="metric-info">
            <div class="metric-value" :style="{ color: riskLevel.color }">
              {{ riskLevel.level }}
            </div>
            <div class="metric-label">风险等级</div>
          </div>
        </div>
      </div>

      <!-- 风险评估描述 -->
      <div class="glass-card risk-desc-card">
        <div class="card-header">
          <h3 class="card-title">风险评估</h3>
        </div>
        <div class="risk-desc-content">
          <el-icon :size="20" :color="riskLevel.color"><WarningFilled /></el-icon>
          <span>{{ riskLevel.desc }}</span>
        </div>
      </div>

      <!-- 健康分多算法对比 -->
      <div class="glass-card" v-if="healthScores.length > 0">
        <div class="card-header">
          <h3 class="card-title">健康分多算法预测对比</h3>
        </div>
        <el-table :data="healthScores" size="small" style="width: 100%">
          <el-table-column label="算法" min-width="200">
            <template #default="{ row }">
              <span style="font-weight: 600">{{ row.label }}</span>
              <el-tag v-if="row.isCurrent" size="small" type="success" style="margin-left: 8px">当前</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="预测健康分" width="150" align="center">
            <template #default="{ row }">
              <span :style="{ color: getScoreColor(row.score), fontWeight: 700, fontSize: '16px' }">{{ row.score }}</span>
            </template>
          </el-table-column>
          <el-table-column label="健康等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.score >= 85 ? 'success' : row.score >= 70 ? 'warning' : 'danger'" size="small">
                {{ row.score >= 85 ? '优良' : row.score >= 70 ? '良好' : row.score >= 50 ? '一般' : '较差' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 寿命预测多算法对比 -->
      <div class="glass-card" v-if="lifeScores.length > 0">
        <div class="card-header">
          <h3 class="card-title">寿命预测多算法对比</h3>
        </div>
        <el-table :data="lifeScores" size="small" style="width: 100%">
          <el-table-column label="算法" min-width="200">
            <template #default="{ row }">
              <span style="font-weight: 600">{{ row.label }}</span>
              <el-tag v-if="row.isCurrent" size="small" type="warning" style="margin-left: 8px">当前</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="剩余寿命(年)" width="150" align="center">
            <template #default="{ row }">
              <span :style="{ color: getLifeColor(row.residual), fontWeight: 700, fontSize: '16px' }">{{ row.residual }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预警级别" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.residual <= 1 ? 'danger' : row.residual <= 3 ? 'warning' : row.residual <= 5 ? 'info' : 'success'" size="small">
                {{ row.residual <= 1 ? '紧急维修' : row.residual <= 3 ? '需维修' : row.residual <= 5 ? '需关注' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- BMHI 计算分解 -->
      <div class="glass-card" v-if="bmhiBreakdown">
        <div class="card-header">
          <h3 class="card-title">健康分计算分解 (BMHI)</h3>
        </div>
        <div class="bmhi-breakdown">
          <div class="breakdown-formula">
            <el-icon color="#0D5D46" :size="16"><DataLine /></el-icon>
            <span>{{ bmhiBreakdown.formula }}</span>
          </div>
          <div class="breakdown-calc">{{ bmhiBreakdown.calculation }}</div>
          <div class="breakdown-items">
            <div class="breakdown-item" v-for="key in ['JR_加热设备', 'SR_散热器', 'ZK_自控设备', 'ZT_烤房主体', 'FS_附属设施']" :key="key">
              <span class="bk-label">{{ key }}</span>
              <span class="bk-value" :style="{ color: getScoreColor(bmhiBreakdown[key]) }">{{ bmhiBreakdown[key] }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 部件风险排名 -->
      <div class="glass-card" v-if="componentRiskRanking.length > 0">
        <div class="card-header">
          <h3 class="card-title">部件风险排名 TOP10</h3>
        </div>
        <el-table :data="componentRiskRanking" size="small" style="width: 100%">
          <el-table-column type="index" label="排名" width="70" align="center" />
          <el-table-column prop="component" label="部件" min-width="150" />
          <el-table-column prop="status" label="当前状态" width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.status === '正常' ? '#2E8B6A' : row.status === '损坏' || row.status === '缺失' ? '#D4604A' : '#D4944A', fontWeight: 600 }">
                {{ row.status }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="riskScore" label="风险分" width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: getRiskColor(row.level), fontWeight: 700 }">{{ row.riskScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="风险等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.level === '高风险' ? 'danger' : row.level === '中风险' ? 'warning' : 'success'" size="small">
                {{ row.level }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 维修建议 -->
      <div class="glass-card">
        <div class="card-header">
          <h3 class="card-title">
            <el-icon :size="18" color="#0D5D46"><Tools /></el-icon>
            维修建议
          </h3>
        </div>
        <div class="advice-list">
          <div v-for="(item, idx) in maintenanceAdvice" :key="idx" class="advice-item">
            <div class="advice-priority" :style="{
              background: item.urgency === '紧急' ? '#D4604A' : item.urgency === '尽快' ? '#D4944A' : '#2E8B6A'
            }">
              {{ item.priority }}
            </div>
            <div class="advice-content">
              <div class="advice-component">{{ item.component }}</div>
              <div class="advice-action">{{ item.action }}</div>
            </div>
            <el-tag
              :type="item.urgency === '紧急' ? 'danger' : item.urgency === '尽快' ? 'warning' : 'success'"
              size="small"
            >{{ item.urgency }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="glass-card empty-state">
      <el-icon :size="48" color="#C0D6CC"><TrendCharts /></el-icon>
      <p>请选择烤房并点击"开始预测分析"查看健康状态预测</p>
    </div>

    <!-- 算法说明 -->
    <div class="glass-card algo-card">
      <el-collapse>
        <el-collapse-item title="预测算法说明" name="algo">
          <div class="algo-content">
            <p><strong>健康分算法 (5种)：</strong>BMHI(AHP层次加权) / FCHE(模糊综合评价) / TOPSIS(熵权排序) / CDCI(退化曲线积分) / CRHE(组合赋权融合)</p>
            <p><strong>寿命预测算法 (4种)：</strong>EDRL(指数衰减) / WRRL(威布尔可靠性) / BDRL(双段退化,推荐) / GPLE(灰色预测)</p>
            <p><strong>风险等级判定：</strong>综合当前健康分和剩余寿命，分为低/中/高/极高四级风险</p>
            <p><strong>部件风险评分：</strong>正常=10分, 异常=70分, 损坏/缺失=100分，按降序排列展示TOP10</p>
            <p><strong>维修建议生成：</strong>根据各部件状态自动生成优先级排序的维修建议，紧急/尽快/常规三级</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<style scoped>
.health-predict-page {
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
}

.page-desc {
  font-size: clamp(12px, 1.4vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(40px, 5vw, 52px);
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

.select-card {
  padding: clamp(14px, 2vw, 20px) clamp(18px, 2.5vw, 28px);
  margin-bottom: clamp(16px, 2vw, 24px);
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

/* 核心指标行 */
.metric-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: clamp(12px, 1.5vw, 18px);
  margin-bottom: clamp(16px, 2vw, 24px);
}

.metric-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  display: flex;
  align-items: center;
  gap: clamp(12px, 1.5vw, 18px);
  border-top: 3px solid #0D5D46;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(10, 77, 62, 0.12), 0 0 24px rgba(13, 93, 70, 0.08);
}

.metric-icon {
  width: clamp(48px, 5vw, 56px);
  height: clamp(48px, 5vw, 56px);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-info {
  flex: 1;
}

.metric-value {
  font-size: clamp(28px, 3.2vw, 36px);
  font-weight: 800;
  line-height: 1.2;
}

.metric-unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 4px;
}

.metric-label {
  font-size: clamp(11px, 1.2vw, 14px);
  color: var(--text-muted);
  margin-top: 4px;
}

/* 风险描述 */
.risk-desc-card {
  padding-bottom: clamp(14px, 1.5vw, 18px);
}

.risk-desc-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: clamp(12px, 1.5vw, 18px) clamp(16px, 2vw, 24px);
  font-size: clamp(13px, 1.4vw, 15px);
  color: var(--text-secondary);
  line-height: 1.6;
}

/* BMHI 分解 */
.bmhi-breakdown {
  padding: clamp(12px, 1.5vw, 18px) clamp(16px, 2vw, 24px);
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

/* 维修建议 */
.advice-list {
  padding: clamp(12px, 1.5vw, 18px) clamp(16px, 2vw, 24px);
}

.advice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.advice-item:last-child {
  border-bottom: none;
}

.advice-priority {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 14px;
  color: #FFFFFF;
  flex-shrink: 0;
}

.advice-content {
  flex: 1;
}

.advice-component {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.advice-action {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: clamp(40px, 5vw, 64px) clamp(16px, 2vw, 28px);
}

.empty-state p {
  margin-top: 16px;
  font-size: clamp(13px, 1.4vw, 15px);
  color: var(--text-muted);
}

/* 算法说明 */
.algo-card {
  padding: 0 4px;
}

.algo-content {
  font-size: clamp(12px, 1.3vw, 14px);
  color: var(--text-secondary);
  line-height: 1.8;
  padding: 0 clamp(16px, 2vw, 24px) clamp(12px, 1.5vw, 18px);
}

.algo-content p {
  margin: 4px 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .metric-row {
    grid-template-columns: 1fr;
  }
  .breakdown-items {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .breakdown-items {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
