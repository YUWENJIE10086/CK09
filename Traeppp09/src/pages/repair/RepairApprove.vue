<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { approveRepair, getRepairDetail } from '@/api/repair'

const route = useRoute()
const router = useRouter()

/** 维修流程步骤 */
const steps = ['申请', '审核', '公示', '审批', '实施', '验收', '归档']

type RepairStatus = '待审核' | '已审核' | '实施中' | '待验收' | '已验收' | '已归档'

/** 状态到步骤索引映射 */
const statusStepMap: Record<RepairStatus, number> = {
  '待审核': 0,
  '已审核': 3,
  '实施中': 4,
  '待验收': 5,
  '已验收': 6,
  '已归档': 6,
}

/** 维修详情 */
const detail = ref<any | null>(null)
const loading = ref(false)

/** 审批表单 */
const auditOpinion = ref('')
const approving = ref(false)

/** 审批记录时间轴 */
const auditTimeline = ref<{ time: string; content: string; type?: string }[]>([])

/** 加载详情 */
async function loadDetail() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const res = await getRepairDetail(id)
    if (res) {
      detail.value = res
      buildTimeline(res)
    }
  } finally {
    loading.value = false
  }
}

/** 构建审批时间轴 */
function buildTimeline(record: any) {
  const list: { time: string; content: string; type?: string }[] = []
  list.push({
    time: record.applyTime,
    content: `${record.applicantName} 提交了维修申请`,
    type: 'primary',
  })
  if (record.auditorName && record.auditTime) {
    list.push({
      time: record.auditTime,
      content: `${record.auditorName} 完成了审核`,
      type: 'success',
    })
  }
  auditTimeline.value = list
}

/** 当前步骤 */
const activeStep = ref(0)

/** 审批通过 */
async function handleApprove() {
  if (!auditOpinion.value.trim()) {
    ElMessage.warning('请填写审批意见')
    return
  }
  approving.value = true
  try {
    await approveRepair(detail.value!.id, { repairStatus: '已审核', auditOpinion: auditOpinion.value })
    ElMessage.success('审批通过')
    router.push('/repair/list')
  } finally {
    approving.value = false
  }
}

/** 审批驳回 */
async function handleReject() {
  if (!auditOpinion.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  approving.value = true
  try {
    await approveRepair(detail.value!.id, { repairStatus: 'rejected', auditOpinion: auditOpinion.value })
    ElMessage.success('已驳回')
    router.push('/repair/list')
  } finally {
    approving.value = false
  }
}

/** 格式化金额 */
function formatCost(val: number) {
  return val ? `¥${val.toLocaleString()}` : '-'
}

onMounted(() => {
  loadDetail().then(() => {
    if (detail.value) {
      activeStep.value = statusStepMap[detail.value.status]
    }
  })
})
</script>

<template>
  <div class="page-container repair-approve" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><Check /></el-icon>
        </div>
        <h2 class="page-title">维修审批</h2>
      </div>
      <p class="page-desc">审核维修申请，填写审批意见</p>
    </div>

    <!-- 流程步骤 -->
    <div class="steps-card">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="(step, idx) in steps" :key="idx" :title="step" />
      </el-steps>
    </div>

    <template v-if="detail">
      <!-- 申请详情卡片 -->
      <div class="detail-card">
        <h3 class="card-title">申请详情</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">烤房名称</span>
            <span class="value">{{ detail.barnName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">申请编号</span>
            <span class="value">{{ detail.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">维修类型</span>
            <span class="value">{{ detail.repairType }}</span>
          </div>
          <div class="detail-item">
            <span class="label">紧迫性</span>
            <span class="value">
              <el-tag
                :type="detail.urgency === '紧急建议' ? 'danger' : detail.urgency === '暂缓' ? 'info' : 'success'"
                size="small"
              >{{ detail.urgency }}</el-tag>
            </span>
          </div>
          <div class="detail-item">
            <span class="label">申请人</span>
            <span class="value">{{ detail.applicantName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">申请时间</span>
            <span class="value">{{ detail.applyTime }}</span>
          </div>
          <div class="detail-item">
            <span class="label">预估费用</span>
            <span class="value highlight">{{ formatCost(detail.estimatedCost) }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">损坏描述</span>
            <span class="value">{{ detail.applyDesc }}</span>
          </div>
        </div>

        <!-- 损坏照片占位 -->
        <div class="photo-section">
          <span class="label">损坏照片</span>
          <div class="photo-grid">
            <div class="photo-placeholder">暂无照片</div>
          </div>
        </div>
      </div>

      <!-- 审批表单 -->
      <div class="approve-card" v-if="detail.status === '待审核' || detail.status === '已审核'">
        <h3 class="card-title">审批操作</h3>
        <el-input
          v-model="auditOpinion"
          type="textarea"
          :rows="4"
          placeholder="请输入审批意见"
          style="margin-bottom: 16px"
        />
        <div class="approve-actions">
          <el-button type="success" :loading="approving" @click="handleApprove">审批通过</el-button>
          <el-button type="danger" :loading="approving" @click="handleReject">驳回</el-button>
        </div>
      </div>

      <!-- 审批记录时间轴 -->
      <div class="timeline-card">
        <h3 class="card-title">审批记录</h3>
        <el-timeline>
          <el-timeline-item
            v-for="(item, idx) in auditTimeline"
            :key="idx"
            :timestamp="item.time"
            :type="item.type as any"
            placement="top"
          >
            {{ item.content }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ========== 页面容器 ========== */
.repair-approve {
  min-height: 100%;
}

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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.page-title-icon:hover {
  transform: scale(1.05);
  box-shadow:
    0 6px 16px rgba(10, 77, 62, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
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

/* ========== 毛玻璃卡片样式 ========== */
.steps-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(20px, 3vw, 32px);
  margin-bottom: clamp(16px, 2vw, 24px);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.steps-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.detail-card,
.approve-card,
.timeline-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(20px, 3vw, 32px);
  margin-bottom: clamp(16px, 2vw, 24px);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.detail-card:hover,
.approve-card:hover,
.timeline-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* ========== 卡片标题 ========== */
.card-title {
  font-size: clamp(15px, 1.5vw, 17px);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 clamp(16px, 2vw, 24px);
  padding-bottom: clamp(10px, 1.2vw, 14px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  position: relative;
  padding-left: clamp(12px, 1.5vw, 16px);
}

.card-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: clamp(16px, 2vw, 22px);
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border-radius: 2px;
}

/* ========== 详情网格 ========== */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: clamp(12px, 1.5vw, 18px);
}

.detail-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  transition: background 0.2s ease;
}

.detail-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-item .label {
  font-size: clamp(12px, 1.2vw, 14px);
  color: var(--text-muted);
  white-space: nowrap;
  min-width: 70px;
  flex-shrink: 0;
}

.detail-item .value {
  font-size: clamp(13px, 1.3vw, 15px);
  color: var(--text-primary);
  word-break: break-all;
}

.detail-item .value.highlight {
  color: #D4944A;
  font-weight: 600;
}

/* ========== 照片区域 ========== */
.photo-section {
  margin-top: clamp(16px, 2vw, 24px);
}

.photo-section .label {
  font-size: clamp(12px, 1.2vw, 14px);
  color: var(--text-muted);
  display: block;
  margin-bottom: 8px;
}

.photo-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.photo-placeholder {
  width: clamp(80px, 8vw, 100px);
  height: clamp(80px, 8vw, 100px);
  border: 2px dashed rgba(10, 77, 62, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(11px, 1.1vw, 13px);
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
}

.photo-placeholder:hover {
  border-color: rgba(10, 77, 62, 0.4);
  background: rgba(0, 0, 0, 0.04);
}

/* ========== 审批操作 ========== */
.approve-actions {
  display: flex;
  gap: 12px;
}

/* 轻拟态按钮 */
.approve-card :deep(.el-button--success) {
  background: linear-gradient(135deg, #2E8B6A, #35A078);
  border: none;
  box-shadow:
    0 2px 8px rgba(46, 139, 106, 0.3),
    0 1px 2px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
}

.approve-card :deep(.el-button--success:hover) {
  background: linear-gradient(135deg, #35A078, #3CB584);
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(46, 139, 106, 0.4),
    0 2px 4px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.approve-card :deep(.el-button--success:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(46, 139, 106, 0.3),
    inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.approve-card :deep(.el-button--danger) {
  background: linear-gradient(135deg, #D4604A, #E07058);
  border: none;
  box-shadow:
    0 2px 8px rgba(212, 96, 74, 0.3),
    0 1px 2px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
}

.approve-card :deep(.el-button--danger:hover) {
  background: linear-gradient(135deg, #E07058, #EC8066);
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(212, 96, 74, 0.4),
    0 2px 4px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.approve-card :deep(.el-button--danger:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(212, 96, 74, 0.3),
    inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* ========== 时间轴样式 ========== */
.timeline-card :deep(.el-timeline-item__node) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
}

.timeline-card :deep(.el-timeline-item__tail) {
  border-left-color: rgba(10, 77, 62, 0.2);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .approve-actions {
    flex-direction: column;
  }

  .approve-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
