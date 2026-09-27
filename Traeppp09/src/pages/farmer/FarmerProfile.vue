<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import VChart from 'vue-echarts'
import {
  Award, Users, TrendingUp, AlertTriangle, School, Target, ClipboardCheck,
  MapPinned, Gauge, Sparkles, DatabaseZap, ChevronRight, Star, Radar, ListChecks
} from 'lucide-vue-next'
import {
  getOverview, getVillageGap, getProfiles, getTrainPlan, genDemo, computeProfiles,
  DIMS, DEFAULT_PAPER
} from '@/api/farmerAlgo'

// ==================== 总览 KPI ====================
const overview = reactive({ profileCount: 0, avgP: 0, weakVillages: 0, needVisit: 0, levelDist: [] as any[] })

const levelColors: Record<string, string> = { 职业: '#2E8B6A', 普通: '#D4944A', 新手: '#D4604A' }

const levelDistOption = computed(() => {
  const data = overview.levelDist.map((l) => ({ name: l.level, value: l.cnt }))
  return {
    color: Object.values(levelColors),
    tooltip: { trigger: 'item', formatter: '{b}: {c} 户 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#3A6A5A', fontSize: 12 } },
    series: [{
      name: '分级分布', type: 'pie', radius: ['58%', '78%'],
      center: ['50%', '44%'], avoidLabelOverlap: true,
      label: { show: true, position: 'center', formatter: () => '', },
      labelLine: { show: false },
      emphasis: { label: { show: false } },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data
    }]
  }
})

const levelTotal = computed(() => overview.levelDist.reduce((s, l) => s + (l.cnt || 0), 0))

// ==================== 村×五维薄弱矩阵 ====================
const matrix = reactive({ baseline: {} as Record<string, number>, villages: [] as any[], themes: {} as Record<string, string> })

const statusStyle = (s: string) => {
  const map: Record<string, string> = {
    red: 'background:#FDECEA;color:#C0392B;',
    yellow: 'background:#FDF3E3;color:#B9770E;',
    green: 'background:#E4F4EC;color:#1E804F;',
    gray: 'background:#EEF2F1;color:#8A9A94;'
  }
  return map[s] || map.gray
}
const statusLabel = (s: string) => ({ red: '薄弱', yellow: '偏弱', green: '达标', gray: '无数据' }[s] || '')

// ==================== 培育计划 ====================
const plan = reactive({ plan: [] as any[], themes: {} as Record<string, string> })

const dimAlgoColor = (d: string) => {
  const map: Record<string, string> = { 栽培: '#2E8B6A', 植保: '#5FB292', 采烤: '#D4944A', 烘烤: '#8A63B8', 综合: '#3A6A5A' }
  return map[d] || '#2E8B6A'
}

// ==================== 个人画像表 ====================
const profiles = ref<any[]>([])
const loading = ref(false)
const paperId = ref(DEFAULT_PAPER)
const profileQuery = reactive({ township: '', village: '', level: '', keyword: '' })
const pager = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const selected = ref<any>(null)
const radarOption = computed(() => {
  const r = selected.value
  if (!r) return null
  return {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: DIMS.map((d) => ({ name: d, max: 100 })),
      radius: '66%', center: ['50%', '52%'],
      splitNumber: 4,
      axisName: { color: '#3A6A5A', fontSize: 12 },
      splitArea: { areaStyle: { color: ['rgba(46,139,106,0.03)', 'rgba(46,139,106,0.06)'] } },
      splitLine: { lineStyle: { color: 'rgba(26,107,79,0.15)' } },
      axisLine: { lineStyle: { color: 'rgba(26,107,79,0.2)' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [r.k_cult, r.k_pp, r.k_hv, r.k_cur, r.k_syn],
        name: r.farmerName,
        symbol: 'circle', symbolSize: 5,
        areaStyle: { color: 'rgba(46,139,106,0.25)' },
        lineStyle: { color: '#2E8B6A', width: 2 },
        itemStyle: { color: '#2E8B6A' }
      }]
    }]
  }
})

// ==================== 加载 ====================
async function loadOverview() {
  Object.assign(overview, await getOverview(paperId.value))
}
async function loadMatrix() {
  Object.assign(matrix, await getVillageGap(paperId.value))
}
async function loadPlan() {
  Object.assign(plan, await getTrainPlan(paperId.value))
}
async function loadProfiles() {
  loading.value = true
  try {
    const res = await getProfiles({ pageNum: pager.pageNum, pageSize: pager.pageSize, ...profileQuery, paperId: paperId.value })
    profiles.value = res.rows || []
    pager.total = res.total || 0
  } finally { loading.value = false }
}
function handleSearch() { pager.pageNum = 1; loadProfiles() }
function handleReset() { profileQuery.township = ''; profileQuery.village = ''; profileQuery.level = ''; profileQuery.keyword = ''; handleSearch() }

function selectRow(row: any) { selected.value = row }

// 乡镇/村级选项
const townshipOptions = computed(() => [...new Set(matrix.villages.map((v) => v.township))])
const villageOptions = computed(() => (profileQuery.township ? matrix.villages.filter((v) => v.township === profileQuery.township).map((v) => v.village) : []))

// ==================== 操作 ====================
async function handleDemo() {
  const res = await genDemo(paperId.value)
  ElMessage.success(res.message || '已生成演示数据')
  await loadOverview(); await loadMatrix(); await loadPlan(); await loadProfiles()
}
async function handleRecompute() {
  const res = await computeProfiles(paperId.value)
  ElMessage.success(res.message || '画像重建完成')
  await loadOverview(); await loadMatrix(); await loadPlan(); await loadProfiles()
}
function handlePaperChange() {
  loadOverview(); loadMatrix(); loadPlan(); loadProfiles()
}

const avgByDim = computed(() => {
  const cols = ['k_cult', 'k_pp', 'k_hv', 'k_cur', 'k_syn']
  return DIMS.map((d, i) => ({
    name: d, value: matrix.baseline[d] != null ? Number(matrix.baseline[d]) : 0
  }))
})

const baselineBarOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 30, right: 10, top: 10, bottom: 24 },
  xAxis: { type: 'category', data: DIMS, axisLabel: { color: '#3A6A5A', fontSize: 11 }, axisLine: { lineStyle: { color: 'rgba(26,107,79,0.2)' } } },
  yAxis: { type: 'value', max: 100, axisLabel: { color: '#8A9A94', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(26,107,79,0.06)' } } },
  series: [{
    type: 'bar', barWidth: 26, data: avgByDim.value.map((d) => d.value),
    itemStyle: {
      borderRadius: [4, 4, 0, 0],
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#2E8B6A' }, { offset: 1, color: '#9ACDB9' }
      ])
    },
    label: { show: true, position: 'top', color: '#0D3D30', fontSize: 11, fontWeight: 600 }
  }]
}))

onMounted(() => {
  loadOverview(); loadMatrix(); loadPlan(); loadProfiles()
})

const fmt = (v: any, n = 1) => (Number.isFinite(Number(v)) ? Number(Number(v).toFixed(n)) : 0)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><Radar :size="20" /></div>
        <div>
          <h2 class="header-title">烟农画像</h2>
          <p class="header-sub">基于五维知识问卷的烟农画像 · 薄弱定位 · 培育计划</p>
        </div>
      </div>
      <div class="header-actions">
        <el-select v-model="paperId" size="default" style="width:150px" placeholder="批次" @change="handlePaperChange">
          <el-option label="问卷批次 2026" value="2026" />
          <el-option label="问卷批次 2025" value="2025" />
        </el-select>
        <el-button type="primary" :icon="Sparkles" @click="handleDemo">生成演示数据</el-button>
        <el-button :icon="DatabaseZap" @click="handleRecompute">重建画像</el-button>
      </div>
    </div>

    <!-- KPI -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon ic1"><Users :size="26" color="#2E8B6A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.profileCount }}</div>
          <div class="kpi-label">画像覆盖烟农</div>
        </div>
        <div class="kpi-trend">已建档</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon ic2"><Gauge :size="26" color="#3A6A5A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ fmt(overview.avgP) }}</div>
          <div class="kpi-label">平均画像分 P</div>
        </div>
        <div class="kpi-trend">满分 100</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon ic3"><AlertTriangle :size="26" color="#D4944A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.weakVillages }}</div>
          <div class="kpi-label">薄弱环节村</div>
        </div>
        <div class="kpi-trend">存在 ≤60 / 缺口&gt;15</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon ic4"><ClipboardCheck :size="26" color="#D4604A" /></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.needVisit }}</div>
          <div class="kpi-label">需上门辅导</div>
        </div>
        <div class="kpi-trend">个人维度薄弱</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="card-title">
          <span class="t-ic" style="background:rgba(46,139,106,.1)"><Award :size="14" color="#2E8B6A" /></span>
          <span>烟农分级分布</span>
        </div>
        <div class="donut-wrap">
          <VChart :option="levelDistOption" autoresize style="height:170px" />
          <div class="donut-center">
            <div class="dc-val">{{ levelTotal }}</div>
            <div class="dc-label">画像烟农数</div>
          </div>
        </div>
        <div class="level-legend">
          <div v-for="l in overview.levelDist" :key="l.level" class="lv-item">
            <span class="lv-dot" :style="{ background: levelColors[l.level] }"></span>
            <span class="lv-name">{{ l.level }}</span>
            <span class="lv-cnt">{{ l.cnt }} 户</span>
            <span class="lv-pct">{{ levelTotal ? ((l.cnt / levelTotal) * 100).toFixed(1) : 0 }}%</span>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <div class="card-title">
          <span class="t-ic" style="background:rgba(138,99,184,.1)"><TrendingUp :size="14" color="#8A63B8" /></span>
          <span>全县五维基线</span>
        </div>
        <VChart :option="baselineBarOption" autoresize style="height:180px" />
        <div class="baseline-note">基线 = 各维度全县烟农平均分，用于对比村域差距</div>
      </div>

      <div class="chart-card radar-card">
        <div class="card-title">
          <span class="t-ic" style="background:rgba(212,148,74,.12)"><Star :size="14" color="#D4944A" /></span>
          <span>单户画像雷达</span>
          <span v-if="selected" class="sel-name">{{ selected.farmerName }} · {{ selected.village }}</span>
        </div>
        <VChart v-if="radarOption" :option="radarOption" autoresize style="height:180px" />
        <div v-else class="radar-empty">
          <MapPinned :size="26" color="#8A9A94" />
          <p>点击下方某位烟农查看其五维画像雷达</p>
        </div>
      </div>
    </div>

    <!-- 村×五维薄弱矩阵 -->
    <div class="section-card">
      <div class="section-head">
        <div class="section-title">
          <span class="title-flag"></span>
          <h3>村 × 五维薄弱矩阵</h3>
        </div>
        <div class="legend-row">
          <span class="lg"><i style="background:#C0392B"></i>薄弱(≤60 或缺口&gt;15)</span>
          <span class="lg"><i style="background:#B9770E"></i>偏弱(低于基线)</span>
          <span class="lg"><i style="background:#1E804F"></i>达标(高于基线)</span>
          <span class="lg"><i style="background:#8A9A94"></i>无数据</span>
        </div>
      </div>
      <div class="matrix-wrap">
        <table class="matrix-table">
          <thead>
            <tr>
              <th rowspan="1">乡镇 · 村</th>
              <th>户数</th>
              <th>画像均分</th>
              <th v-for="d in DIMS" :key="d">{{ d }}</th>
              <th>最弱维</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in matrix.villages" :key="v.village">
              <td class="m-region">
                <div class="mr-main">{{ v.village }}</div>
                <div class="mr-sub">{{ v.township }}</div>
              </td>
              <td class="m-num">{{ v.farmers }}</td>
              <td class="m-num strong">{{ fmt(v.avgP) }}</td>
              <td v-for="dd in v.dims" :key="dd.dim" class="m-cell">
                <span class="cell-pill" :style="statusStyle(dd.status)">
                  <span class="cp-val">{{ fmt(dd.value) }}</span>
                  <span class="cp-gap">{{ dd.gap > 0 ? '-' + Math.abs(dd.gap) : '+' + Math.abs(dd.gap) }}</span>
                </span>
              </td>
              <td class="m-weak">
                <span class="weak-tag" :style="{ background: dimAlgoColor(v.weakDim) + '22', color: dimAlgoColor(v.weakDim) }">
                  {{ v.weakDim || '—' }}
                </span>
              </td>
            </tr>
            <tr v-if="matrix.villages.length === 0">
              <td colspan="9" class="cell-empty">暂无画像数据，请先在「答题情况」导入或生成演示数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 培育计划 -->
    <div class="section-card">
      <div class="section-head">
        <div class="section-title">
          <span class="title-flag"></span>
          <h3>培育计划建议</h3>
        </div>
        <span class="section-hint">按薄弱缺口自动排定优先级，联动现场会 + 上门辅导</span>
      </div>
      <div class="plan-grid">
        <div v-for="(p, i) in plan.plan" :key="i" class="plan-card">
          <div class="plan-top">
            <div class="plan-rank">No.{{ i + 1 }}</div>
            <div class="plan-region">
              <div class="pr-name">{{ p.village }}</div>
              <div class="pr-sub">{{ p.township }} · {{ p.dim }}维得分 {{ fmt(p.value) }}</div>
            </div>
            <div class="plan-pri">
              <span class="pri-val">{{ fmt(p.priority) }}</span>
              <span class="pri-label">优先级</span>
            </div>
          </div>
          <div class="plan-gap">
            <span>与县级基线缺口</span>
            <strong>{{ Math.abs(p.gap) }}</strong>
          </div>
          <div class="plan-theme" :style="{ borderLeft: '3px solid ' + dimAlgoColor(p.dim) }">
            <School :size="14" :color="dimAlgoColor(p.dim)" />
            <span><b>{{ p.theme }}</b> · {{ p.action }}</span>
          </div>
          <div class="plan-targets">
            <span class="pt-label"><Target :size="12" /> 上门名单</span>
            <div class="pt-names">
              <el-tag v-for="(t, ti) in p.targets" :key="ti" size="small" effect="plain" round>{{ t.farmerName }} {{ fmt(t.score) }}</el-tag>
              <span v-if="p.targets && p.targets.length === 0" class="pt-none">暂无低分个体</span>
            </div>
          </div>
        </div>
        <div v-if="plan.plan.length === 0" class="plan-empty">
          <ListChecks :size="28" color="#8A9A94" />
          <p>暂无薄弱环节，各片区知识水平达标</p>
        </div>
      </div>
    </div>

    <!-- 个人画像明细 -->
    <div class="section-card nomargin">
      <div class="section-head mb8">
        <div class="section-title">
          <span class="title-flag"></span>
          <h3>个人画像明细</h3>
        </div>
      </div>
      <div class="filter-container">
        <div class="filter-left">
          <el-select v-model="profileQuery.township" placeholder="全部乡镇" clearable style="width:130px" @change="profileQuery.village=''; handleSearch()">
            <el-option v-for="t in townshipOptions" :key="t" :label="t" :value="t" />
          </el-select>
          <el-select v-model="profileQuery.village" placeholder="全部村" clearable style="width:130px" :disabled="!profileQuery.township" @change="handleSearch">
            <el-option v-for="v in villageOptions" :key="v" :label="v" :value="v" />
          </el-select>
          <el-select v-model="profileQuery.level" placeholder="全部分级" clearable style="width:120px" @change="handleSearch">
            <el-option label="职业烟农" value="职业" />
            <el-option label="普通烟农" value="普通" />
            <el-option label="新手烟农" value="新手" />
          </el-select>
          <el-input v-model="profileQuery.keyword" placeholder="姓名 / 手机号" clearable style="width:160px" @keyup.enter="handleSearch" />
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="profiles" v-loading="loading" size="small" highlight-current-row @row-click="selectRow" :header-cell-style="{ background: '#F2F8F5', color: '#0D3D30', fontWeight: 600 }">
          <el-table-column label="烟农" width="150">
            <template #default="{ row }">
              <div class="cell-main">{{ row.farmerName }}</div>
              <div class="cell-sub">{{ row.farmerPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="区域" min-width="150">
            <template #default="{ row }">{{ [row.township, row.village, row.poundGroup].filter(Boolean).join(' · ') }}</template>
          </el-table-column>
          <el-table-column label="等级" width="90" align="center">
            <template #default="{ row }">
              <span class="lv-pill" :style="{ background: (levelColors[row.level] || '#5A8A7A') + '1f', color: (levelColors[row.level] || '#5A8A7A') }">
                {{ row.level }}烟农
              </span>
            </template>
          </el-table-column>
          <el-table-column label="画像分" width="70" align="center">
            <template #default="{ row }"><span class="score-num">{{ fmt(row.pScore) }}</span></template>
          </el-table-column>
          <el-table-column label="最弱维度" width="90" align="center">
            <template #default="{ row }">
              <span class="weak-tag" :style="{ background: dimAlgoColor(row.weakDim) + '22', color: dimAlgoColor(row.weakDim) }">{{ row.weakDim || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="80">
            <template #default="{ row }">
              <div class="pri-bar">
                <div class="pri-track"><div class="pri-fill" :style="{ width: Math.min(row.priority, 100) + '%' }"></div></div>
                <span class="pri-num">{{ fmt(row.priority) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" text :icon="Radar" @click.stop="selectRow(row)">雷达</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="pager.pageNum"
            v-model:page-size="pager.pageSize"
            :total="pager.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadProfiles"
            @current-change="loadProfiles"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 12px; padding: 12px; }
.page-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #0D5D46, #2E8B6A); color: #fff; box-shadow: 0 4px 12px rgba(13, 93, 70, .3); }
.header-title { margin: 0; font-size: 18px; font-weight: 700; color: #0D3D30; }
.header-sub { margin: 2px 0 0; font-size: 12px; color: var(--text-muted); }
.header-actions { display: flex; gap: 8px; }

.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.kpi-card { background: var(--surface-color); border-radius: var(--radius-xl); padding: 14px 16px; display: flex; gap: 12px; align-items: center; border: 1px solid var(--border-light); box-shadow: var(--shadow-card); position: relative; overflow: hidden; }
.kpi-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: linear-gradient(180deg, #0D5D46, #5FB292); }
.kpi-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ic1 { background: rgba(46,139,106,.12); } .ic2 { background: rgba(90,138,122,.12); } .ic3 { background: rgba(212,148,74,.14); } .ic4 { background: rgba(212,96,74,.12); }
.kpi-value { font-size: 24px; font-weight: 800; color: #0D3D30; line-height: 1.1; }
.kpi-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.kpi-trend { position: absolute; right: 12px; top: 12px; font-size: 11px; color: var(--text-placeholder); }

.chart-row { display: grid; grid-template-columns: 1fr 1.1fr 1.1fr; gap: 12px; }
.chart-card { background: var(--surface-color); border-radius: var(--radius-xl); border: 1px solid var(--border-light); box-shadow: var(--shadow-md); padding: 12px 14px; }
.card-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #0D3D30; }
.t-ic { width: 22px; height: 22px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.sel-name { margin-left: auto; font-size: 11px; color: var(--text-muted); font-weight: 500; }
.donut-wrap { position: relative; }
.donut-center { position: absolute; top: 55px; left: 0; right: 0; text-align: center; pointer-events: none; }
.dc-val { font-size: 22px; font-weight: 800; color: #0D3D30; }
.dc-label { font-size: 11px; color: var(--text-muted); }
.level-legend { display: grid; grid-template-columns: repeat(auto-fill, minmax(105px, 1fr)); gap: 6px 8px; padding-top: 4px; }
.lv-item { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #3A6A5A; }
.lv-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.lv-cnt { font-weight: 600; color: #0D3D30; }
.lv-pct { color: var(--text-placeholder); }
.baseline-note { font-size: 11px; color: var(--text-muted); text-align: center; }
.radar-empty { height: 180px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; color: var(--text-placeholder); }
.radar-empty p { margin: 0; font-size: 12px; }

.section-card { background: var(--surface-color); border-radius: var(--radius-xl); border: 1px solid var(--border-light); box-shadow: var(--shadow-md); padding: 14px 16px; }
.section-card.nomargin { margin: 0; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.section-title { display: flex; align-items: center; gap: 8px; }
.title-flag { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, #2E8B6A, #0D5D46); }
.section-title h3 { margin: 0; font-size: 15px; color: #0D3D30; }
.section-hint { font-size: 12px; color: var(--text-muted); }
.mb8 { margin-bottom: 4px; }
.legend-row { display: flex; gap: 12px; font-size: 11px; color: #3A6A5A; }
.legend-row .lg { display: flex; align-items: center; gap: 4px; }
.legend-row i { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }

.matrix-wrap { overflow: auto; border: 1px solid var(--border-light); border-radius: 8px; }
.matrix-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.matrix-table th { background: #F2F8F5; color: #0D3D30; font-weight: 600; padding: 8px 10px; white-space: nowrap; border-bottom: 1px solid #e2ece6; }
.matrix-table td { padding: 7px 10px; text-align: center; border-bottom: 1px solid #EFF5F1; }
.matrix-table tr:last-child td { border-bottom: none; }
.matrix-table tbody tr:hover { background: #F4FAF7; }
.m-region { text-align: left !important; }
.mr-main { font-weight: 600; color: #0D3D30; }
.mr-sub { font-size: 11px; color: var(--text-muted); }
.m-num { color: #3A6A5A; }
.m-num.strong { font-weight: 700; color: #0D3D30; }
.cell-pill { display: inline-flex; align-items: baseline; gap: 4px; padding: 2px 8px; border-radius: 5px; min-width: 58px; justify-content: center; }
.cp-val { font-size: 12px; font-weight: 700; }
.cp-gap { font-size: 10px; opacity: .75; }
.m-weak .weak-tag { font-weight: 600; }
.weak-tag { display: inline-flex; padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.cell-empty { color: var(--text-placeholder); padding: 24px !important; }

.plan-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; }
.plan-card { border: 1px solid var(--border-light); border-radius: 10px; padding: 12px 14px; background: #FBFDFC; transition: box-shadow .2s; }
.plan-card:hover { box-shadow: var(--shadow-md); }
.plan-top { display: flex; align-items: center; gap: 10px; }
.plan-rank { width: 32px; height: 32px; border-radius: 8px; background: linear-gradient(135deg, #0D5D46, #2E8B6A); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.plan-region { flex: 1; }
.pr-name { font-weight: 700; color: #0D3D30; }
.pr-sub { font-size: 11px; color: var(--text-muted); }
.plan-pri { text-align: center; }
.pri-val { font-size: 18px; font-weight: 800; color: #D4944A; display: block; line-height: 1.1; }
.pri-label { font-size: 10px; color: var(--text-placeholder); }
.plan-gap { display: flex; justify-content: space-between; align-items: center; margin: 10px 0 8px; padding: 6px 10px; background: #FDF3E3; border-radius: 6px; font-size: 12px; color: #B9770E; }
.plan-gap strong { font-size: 15px; }
.plan-theme { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #3A6A5A; padding: 4px 0; }
.plan-theme span { line-height: 1.4; }
.plan-targets { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #E0EBE5; }
.pt-label { font-size: 11px; color: var(--text-muted); display: inline-flex; align-items: center; gap: 4px; margin-bottom: 5px; }
.pt-names { display: flex; flex-wrap: wrap; gap: 4px; }
.pt-none { font-size: 11px; color: var(--text-placeholder); }
.plan-empty { grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px; color: var(--text-placeholder); gap: 6px; }
.plan-empty p { margin: 0; font-size: 13px; }

.filter-container { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.filter-left { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.table-container { padding: 10px 12px; }
.cell-main { font-weight: 600; color: #0D3D30; }
.cell-sub { font-size: 11px; color: var(--text-muted); }
.lv-pill { display: inline-flex; padding: 2px 9px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.score-num { font-size: 15px; font-weight: 800; color: #0D5D46; }
.pri-bar { display: flex; align-items: center; gap: 6px; }
.pri-track { flex: 1; height: 6px; background: #EEF4F1; border-radius: 3px; overflow: hidden; }
.pri-fill { height: 100%; background: linear-gradient(90deg, #2E8B6A, #D4944A); border-radius: 3px; }
.pri-num { font-size: 11px; font-weight: 600; color: #D4944A; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 8px 0 0; }
.data-table :deep(.el-table__cell) { padding: 4px 6px; font-size: 12px; white-space: nowrap; }
</style>