<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import HealthGauge from '@/components/HealthGauge.vue'
import BarnCode from '@/components/BarnCode.vue'
import { getBarnInfo, getBarnComponentList } from '@/api/barn'
import { getReservationList } from '@/api/reservation'
import { getRepairList } from '@/api/repair'

const route = useRoute()
const router = useRouter()

/** 烤房状态中文→StatusTag key映射 */
const barnStatusKeyMap: Record<string, string> = {
  '优良': 'excellent',
  '正常': 'normal',
  '急需修复': 'urgent',
  '有望修复': 'hopeful',
}

/** 部位名称映射 */
const partTypeNameMap: Record<string, string> = {
  JR: '加热系统',
  SR: '散热系统',
  ZK: '装炕系统',
  ZT: '自控系统',
  FS: '风机系统',
}

/** 部位图标映射 */
const partTypeIconMap: Record<string, string> = {
  JR: '🔥',
  SR: '💨',
  ZK: '🏗️',
  ZT: '🎛️',
  FS: '🔄',
}

/** 详情数据 */
const detail = ref<any>(null)
const loading = ref(false)
const activeTab = ref('basic')
const activeCollapse = ref(['JR', 'SR', 'ZK', 'ZT', 'FS'])

/** 部件列表 */
const components = ref<any[]>([])

/** 维修记录（按barnId过滤） */
const repairRecords = ref<any[]>([])

/** 预约记录（按barnId过滤） */
const reservations = ref<any[]>([])

/** 评价记录 */
const evaluations = ref<any[]>([])

/** 健康评分颜色 */
const healthColor = computed(() => {
  const score = Number(detail.value?.healthScore || 0)
  if (score >= 85) return 'var(--success-color)'
  if (score >= 70) return 'var(--warning-color)'
  return 'var(--danger-color)'
})

/** 按部位分组的部件列表 */
const groupedComponents = computed(() => {
  const groups: { partType: string; name: string; icon: string; components: any[] }[] = []
  const typeOrder = ['JR', 'SR', 'ZK', 'ZT', 'FS']
  typeOrder.forEach(pt => {
    const comps = components.value.filter((c: any) => (c.componentKey || c.partType) === pt)
    if (comps.length > 0) {
      groups.push({
        partType: pt,
        name: partTypeNameMap[pt],
        icon: partTypeIconMap[pt],
        components: comps,
      })
    }
  })
  return groups
})

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

/** 获取详情数据 */
async function fetchDetail() {
  const id = String(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const data = await getBarnInfo(id)
    if (!data) {
      ElMessage.error('未找到该烤房信息')
      router.back()
      return
    }
    detail.value = data
    // 加载部件列表
    try {
      const compList: any = await getBarnComponentList(id)
      components.value = (compList?.rows || compList || []) as any[]
    } catch {
      components.value = []
    }
    // 加载维修记录
    try {
      const repairs: any = await getRepairList({ ovenId: id, pageSize: 50 })
      repairRecords.value = (repairs?.rows || []) as any[]
    } catch {
      repairRecords.value = []
    }
    // 加载预约记录
    try {
      const resvs: any = await getReservationList({ ovenId: id, pageSize: 50 })
      reservations.value = (resvs?.rows || []) as any[]
    } catch {
      reservations.value = []
    }
  } finally {
    loading.value = false
  }
}

/** 返回 */
function handleBack() {
  router.back()
}

/** 评分颜色 */
function scoreTagType(score: number): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (score >= 85) return 'success'
  if (score >= 70) return 'warning'
  if (score >= 45) return 'danger'
  return 'info'
}

/** 部件状态标签类型 */
function componentStatusTagType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === '正常') return 'success'
  if (status === '轻微损坏') return 'warning'
  if (status === '中度损坏' || status === '严重损坏') return 'danger'
  if (status === '缺失' || status === '报废') return 'info'
  return ''
}

/** 维修记录时间线颜色 */
function repairTimelineType(status: string): '' | 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    '待审核': 'warning',
    '实施中': 'primary',
    '已归档': 'info',
  }
  return map[status] || 'primary'
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="barn-detail-page" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-icon">
        <el-icon><View /></el-icon>
      </div>
      <h1 class="page-title-text">烤房详情</h1>
    </div>

    <!-- 顶部 -->
    <div class="header-card glass-card">
      <div class="detail-header">
        <div class="header-left">
          <button class="back-btn neumorphic-btn" @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </button>
          <BarnCode :code="detail?.id || detail?.barnCode || ''" />
          <h2 class="barn-name">{{ detail?.barnName }}</h2>
          <StatusTag
            v-if="detail"
            :status="barnStatusKeyMap[detail.auditStatus || detail.useStatus] || 'normal'"
            type="barn"
          />
        </div>
      </div>
    </div>

    <!-- Tab页签 -->
    <div class="content-card glass-card" v-if="detail">
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <div class="basic-layout">
            <div class="basic-info">
              <el-descriptions :column="2" border class="glass-inner">
                <el-descriptions-item label="烤房编号">
                  <BarnCode :code="detail.id || detail.barnCode" />
                </el-descriptions-item>
                <el-descriptions-item label="烤房名称">{{ detail.barnName }}</el-descriptions-item>
                <el-descriptions-item label="项目类型">{{ detail.projectType || detail.barnType }}</el-descriptions-item>
                <el-descriptions-item label="建设方式">{{ detail.buildMethod || '-' }}</el-descriptions-item>
                <el-descriptions-item label="健康等级">
                  <span class="level-tag" :class="'level-tag--' + (detail.healthLevel === '优良' ? 'excellent' : detail.healthLevel === '需维护' ? 'maintenance' : detail.healthLevel === '急需修复' ? 'urgent' : 'retired')"><span>{{ detail.healthLevel || '未评' }}</span></span>
                </el-descriptions-item>
                <el-descriptions-item label="健康评分">
                  <span class="health-score" :style="{ color: healthColor }">
                    {{ Number(detail.healthScore || 0).toFixed(0) }}分
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="使用状态">
                  <span class="status-pill" :class="'status-pill--' + (detail.useStatus === '在用' ? 'inUse' : detail.useStatus === '闲置' ? 'idle' : detail.useStatus === '转用' ? 'transferred' : 'damaged')">{{ detail.useStatus }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="建成时间">{{ detail.completeDate || detail.buildYear || '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属县区">{{ detail.county || detail.countyCode }}</el-descriptions-item>
                <el-descriptions-item label="所属乡镇">{{ detail.township || detail.townCode }}</el-descriptions-item>
                <el-descriptions-item label="详细地址" :span="2">{{ detail.address }}</el-descriptions-item>
                <el-descriptions-item label="经度">{{ detail.longitude }}</el-descriptions-item>
                <el-descriptions-item label="纬度">{{ detail.latitude }}</el-descriptions-item>
                <el-descriptions-item label="建设费用" :span="2">¥{{ Number(detail.projectCost || 0).toLocaleString() }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="health-gauge-wrapper glass-inner">
              <HealthGauge :score="Number(detail.healthScore || 0)" />
            </div>
          </div>
        </el-tab-pane>

        <!-- 部件状态 -->
        <el-tab-pane :label="`部件状态 (${components.length})`" name="components">
          <el-collapse v-model="activeCollapse" class="glass-collapse">
            <el-collapse-item
              v-for="group in groupedComponents"
              :key="group.partType"
              :name="group.partType"
            >
              <template #title>
                <div class="collapse-title">
                  <span class="part-icon">{{ group.icon }}</span>
                  <span class="part-name">{{ group.name }}</span>
                  <el-tag size="small" type="info" class="part-count">{{ group.components.length }}个部件</el-tag>
                </div>
              </template>
              <el-table :data="group.components" border stripe size="small" class="glass-table">
                <el-table-column prop="componentName" label="部件名称" min-width="140" />
                <el-table-column prop="componentKey" label="类型" width="80" align="center" />
                <el-table-column prop="status" label="状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="componentStatusTagType(row.status)" size="small" effect="light">
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="damageLevel" label="损坏程度" width="100" align="center" />
                <el-table-column prop="score" label="评分" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="scoreTagType(Number(row.score || 0))" size="small">{{ row.score || '-' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
          <el-empty v-if="!groupedComponents.length" description="暂无部件信息" />
        </el-tab-pane>

        <!-- 维修记录 -->
        <el-tab-pane :label="`维修记录 (${repairRecords.length})`" name="repair">
          <div class="timeline-wrapper" v-if="repairRecords.length">
            <el-timeline>
              <el-timeline-item
                v-for="record in repairRecords"
                :key="record.id"
                :timestamp="parseTime(record.applyTime)"
                :type="repairTimelineType(record.repairStatus)"
                placement="top"
              >
                <div class="timeline-card glass-inner">
                  <div class="repair-header">
                    <span class="repair-type">{{ record.repairType }}</span>
                    <el-tag size="small">{{ record.repairStatus }}</el-tag>
                  </div>
                  <p class="repair-desc">{{ record.applyDesc }}</p>
                  <div class="repair-cost">
                    <span>预估费用：<b>¥{{ Number(record.estimatedCost || 0).toLocaleString() }}</b></span>
                    <span v-if="record.actualCost">实际费用：<b>¥{{ Number(record.actualCost).toLocaleString() }}</b></span>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
          <el-empty v-else description="暂无维修记录" />
        </el-tab-pane>

        <!-- 预约记录 -->
        <el-tab-pane :label="`预约记录 (${reservations.length})`" name="reservation">
          <el-table :data="reservations" border stripe v-if="reservations.length" class="glass-table">
            <el-table-column prop="id" label="预约编号" width="100" />
            <el-table-column prop="userName" label="预约人" width="100" />
            <el-table-column label="开始时间" width="160">
              <template #default="{ row }">{{ parseTime(row.reserveStart).slice(0, 16) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" width="160">
              <template #default="{ row }">{{ parseTime(row.reserveEnd).slice(0, 16) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110" align="center" />
          </el-table>
          <el-empty v-else description="暂无预约记录" />
        </el-tab-pane>

        <!-- 评价记录 -->
        <el-tab-pane label="评价记录" name="evaluation">
          <el-empty description="暂无评价记录" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script lang="ts">
import { View } from '@element-plus/icons-vue'
</script>

<style scoped>
/* 毛玻璃设计系统 - 烤房详情页 */
.barn-detail-page {
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 1.5vw, 20px);
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  gap: clamp(12px, 1.5vw, 16px);
  margin-bottom: clamp(4px, 0.5vw, 8px);
}

.page-title-icon {
  width: clamp(40px, 4vw, 48px);
  height: clamp(40px, 4vw, 48px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.3);
}

.page-title-icon .el-icon {
  font-size: clamp(20px, 2vw, 24px);
  color: #fff;
}

.page-title-text {
  font-size: clamp(22px, 2.5vw, 28px);
  font-weight: 700;
  color: #1A1F36;
  margin: 0;
  letter-spacing: -0.02em;
}

/* 毛玻璃卡片 */
.glass-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  box-shadow: 
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glass-card:hover {
  transform: translateY(-3px);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.1),
    0 2px 4px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* 内层毛玻璃 */
.glass-inner {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 8px;
}

/* 顶部卡片 */
.header-card {
  padding: clamp(16px, 2vw, 24px);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: clamp(12px, 1.5vw, 16px);
}

/* 轻拟态按钮 */
.neumorphic-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: clamp(8px, 1vw, 10px) clamp(14px, 1.8vw, 18px);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.75) 100%);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  color: #4A5568;
  font-size: clamp(13px, 1vw, 14px);
  font-weight: 500;
  cursor: pointer;
  box-shadow: 
    -2px -2px 4px rgba(255, 255, 255, 1),
    2px 2px 4px rgba(138, 168, 160, 0.5);
  transition: all 0.2s ease;
}

.neumorphic-btn:hover {
  transform: translateY(-1px);
  box-shadow: 
    -3px -3px 6px rgba(255, 255, 255, 1),
    3px 3px 6px rgba(138, 168, 160, 0.6);
}

.neumorphic-btn:active {
  box-shadow: 
    inset 2px 2px 4px rgba(255, 255, 255, 0.9),
    inset -2px -2px 4px rgba(138, 168, 160, 0.45);
}

.back-btn {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  color: #fff;
  border: none;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.35);
}

.back-btn:hover {
  background: linear-gradient(135deg, #0D5D46, #0F6D52);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.45);
}

.barn-name {
  font-size: clamp(18px, 2vw, 20px);
  font-weight: 700;
  color: #1A1F36;
  margin: 0;
}

/* 内容卡片 */
.content-card {
  padding: clamp(16px, 2vw, 24px);
}

/* 基本信息布局 */
.basic-layout {
  display: flex;
  gap: clamp(20px, 3vw, 32px);
  align-items: flex-start;
  flex-wrap: wrap;
}

.basic-info {
  flex: 1;
  min-width: clamp(280px, 40vw, 500px);
}

/* 描述列表样式 */
.basic-info :deep(.el-descriptions) {
  --el-descriptions-table-border: 1px solid rgba(138, 168, 160, 0.2);
}

.basic-info :deep(.el-descriptions__label) {
  color: #697386 !important;
  font-weight: 500;
  font-size: clamp(12px, 1vw, 13px);
  background: rgba(248, 250, 252, 0.8) !important;
  padding: clamp(10px, 1.2vw, 12px) clamp(12px, 1.5vw, 16px) !important;
  width: 120px;
}

.basic-info :deep(.el-descriptions__content) {
  color: #1A1F36 !important;
  font-size: clamp(13px, 1vw, 14px);
  padding: clamp(10px, 1.2vw, 12px) clamp(12px, 1.5vw, 16px) !important;
}

/* 健康评分 */
.health-score {
  font-weight: 700;
  font-size: clamp(14px, 1.2vw, 16px);
  letter-spacing: 0.02em;
}

.health-gauge-wrapper {
  width: clamp(200px, 25vw, 240px);
  min-width: 200px;
  height: clamp(200px, 25vw, 240px);
  padding: clamp(12px, 1.5vw, 16px);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 使用状态 */
.status-pill--inUse { background: #2E8B6A; color: #FFFFFF; }
.status-pill--idle { background: #3498DB; color: #FFFFFF; }
.status-pill--transferred { background: #E67E22; color: #FFFFFF; }
.status-pill--damaged { background: #E74C3C; color: #FFFFFF; }

/* 健康等级 */
.level-tag--excellent { background: transparent; color: #2E8B6A; border: 2px solid #2E8B6A; }
.level-tag--maintenance { background: transparent; color: #F39C12; border: 2px solid #F39C12; }
.level-tag--urgent { background: transparent; color: #E74C3C; border: 2px solid #E74C3C; }
.level-tag--retired { background: transparent; color: #94A3B8; border: 2px dashed #94A3B8; }

.status-pill {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 6px;
  font-size: clamp(12px, 1vw, 14px);
  font-weight: 700;
  white-space: nowrap;
}

.level-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: clamp(12px, 1vw, 14px);
  font-weight: 700;
  white-space: nowrap;
  transform: skewX(-5deg);
}

.level-tag > span {
  display: inline-block;
  transform: skewX(5deg);
}

/* 折叠面板 */
.glass-collapse :deep(.el-collapse-item__header) {
  background: rgba(255, 255, 255, 0.5);
  border-color: rgba(138, 168, 160, 0.2);
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.glass-collapse :deep(.el-collapse-item__header:hover) {
  background: rgba(255, 255, 255, 0.7);
}

.glass-collapse :deep(.el-collapse-item__wrap) {
  border: none;
}

.glass-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 8px;
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.part-icon {
  font-size: clamp(16px, 1.5vw, 18px);
}

.part-name {
  font-weight: 600;
  font-size: clamp(14px, 1.1vw, 15px);
  color: #1A1F36;
}

.part-count {
  margin-left: 4px;
}

/* 表格样式 */
.glass-table :deep(.el-table) {
  --el-table-border-color: rgba(138, 168, 160, 0.2);
  --el-table-header-bg-color: rgba(248, 250, 252, 0.8);
  --el-table-header-text-color: #4A5568;
  --el-table-row-hover-bg-color: rgba(248, 250, 252, 0.6);
  --el-table-bg-color: transparent;
  border-radius: 8px;
}

.glass-table :deep(.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: clamp(12px, 0.9vw, 13px);
  color: #4A5568;
  background: rgba(248, 250, 252, 0.9);
}

.glass-table :deep(.el-table td.el-table__cell) {
  color: #1A1F36;
  font-size: clamp(12px, 0.9vw, 13px);
}

/* 时间线 */
.timeline-wrapper {
  padding: 8px 0;
}

.timeline-card {
  padding: clamp(12px, 1.5vw, 16px);
  transition: box-shadow 0.2s ease;
}

.timeline-card:hover {
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.repair-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.repair-type {
  font-weight: 600;
  font-size: clamp(14px, 1.1vw, 15px);
  color: #1A1F36;
}

.repair-desc {
  color: #4A5568;
  font-size: clamp(12px, 1vw, 13px);
  margin: 4px 0 8px;
}

.repair-cost {
  display: flex;
  gap: 24px;
  font-size: clamp(12px, 1vw, 13px);
  color: #697386;
}

.repair-cost b {
  color: #1A1F36;
}

/* Tab 样式 */
:deep(.el-tabs__item) {
  font-weight: 500;
  font-size: clamp(13px, 1vw, 14px);
  color: #697386;
}

:deep(.el-tabs__item.is-active) {
  color: #0A4D3E;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  height: 3px;
  border-radius: 2px;
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(138, 168, 160, 0.2);
}

/* 响应式 */
@media (max-width: 768px) {
  .basic-layout {
    flex-direction: column;
  }
  
  .health-gauge-wrapper {
    width: 100%;
    min-width: unset;
    height: clamp(180px, 30vw, 220px);
  }
  
  .header-left {
    flex-wrap: wrap;
  }
}
</style>