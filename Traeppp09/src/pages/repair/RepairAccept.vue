<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Check } from '@element-plus/icons-vue'
import { acceptRepair, getRepairDetail } from '@/api/repair'

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

/** 当前步骤 */
const activeStep = ref(0)

/** 验收表单 */
const acceptResult = ref('')
const acceptOpinion = ref('')
const acceptPhotos = ref<any[]>([])
const submitting = ref(false)

/** 加载详情 */
async function loadDetail() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const res = await getRepairDetail(id)
    if (res) {
      detail.value = res
      activeStep.value = statusStepMap[res.status]
    }
  } finally {
    loading.value = false
  }
}

/** 提交验收 */
async function handleSubmit() {
  if (!acceptResult.value) {
    ElMessage.warning('请选择验收结果')
    return
  }
  if (!acceptOpinion.value.trim()) {
    ElMessage.warning('请填写验收意见')
    return
  }
  submitting.value = true
  try {
    await acceptRepair(detail.value!.id, { repairStatus: acceptResult.value, acceptOpinion: acceptOpinion.value })
    ElMessage.success('验收提交成功')
    router.push('/repair/list')
  } finally {
    submitting.value = false
  }
}

/** 格式化金额 */
function formatCost(val: number) {
  return val ? `¥${val.toLocaleString()}` : '-'
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="page-container repair-accept" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><Check /></el-icon>
        </div>
        <h2 class="page-title">维修验收</h2>
      </div>
      <p class="page-desc">验收维修实施结果，填写验收意见</p>
    </div>

    <!-- 流程步骤 -->
    <div class="steps-card">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step v-for="(step, idx) in steps" :key="idx" :title="step" />
      </el-steps>
    </div>

    <template v-if="detail">
      <!-- 实施信息卡片 -->
      <div class="impl-card">
        <h3 class="card-title">实施信息</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">烤房名称</span>
            <span class="value">{{ detail.barnName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">维修类型</span>
            <span class="value">{{ detail.repairType }}</span>
          </div>
          <div class="detail-item">
            <span class="label">实施队伍</span>
            <span class="value">曲靖市烤房维修服务队</span>
          </div>
          <div class="detail-item">
            <span class="label">实施时间</span>
            <span class="value">{{ detail.auditTime || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">预估费用</span>
            <span class="value highlight">{{ formatCost(detail.estimatedCost) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">实际费用</span>
            <span class="value highlight">{{ formatCost(detail.actualCost) }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">损坏描述</span>
            <span class="value">{{ detail.applyDesc }}</span>
          </div>
        </div>

        <!-- 完工照片 -->
        <div class="photo-section">
          <span class="label">完工照片</span>
          <div class="photo-grid">
            <div class="photo-placeholder">暂无照片</div>
          </div>
        </div>
      </div>

      <!-- 验收表单 -->
      <div class="accept-card" v-if="detail.status === '待验收'">
        <h3 class="card-title">验收操作</h3>
        <el-form label-width="100px" style="max-width: 600px">
          <el-form-item label="验收结果" required>
            <el-radio-group v-model="acceptResult">
              <el-radio value="合格">合格</el-radio>
              <el-radio value="不合格">不合格</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="验收意见" required>
            <el-input
              v-model="acceptOpinion"
              type="textarea"
              :rows="4"
              placeholder="请填写验收意见"
            />
          </el-form-item>
          <el-form-item label="验收照片">
            <el-upload
              action="#"
              :auto-upload="false"
              :file-list="acceptPhotos"
              :on-change="(file: any, list: any) => acceptPhotos = list"
              list-type="picture-card"
              accept="image/*"
            >
              <el-icon><UploadFilled /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交验收</el-button>
            <el-button @click="router.push('/repair/list')">返回</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ========== 页面容器 ========== */
.repair-accept {
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

.impl-card,
.accept-card {
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

.impl-card:hover,
.accept-card:hover {
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

/* ========== 轻拟态按钮样式 ========== */
.accept-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 8px rgba(10, 77, 62, 0.25),
    0 1px 2px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.accept-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #107055);
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.35),
    0 2px 4px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.accept-card :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(10, 77, 62, 0.2),
    inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 普通按钮轻拟态 */
.accept-card :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff, #f5f5f5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transition: all 0.2s ease;
}

.accept-card :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(145deg, #ffffff, #fafafa);
  transform: translateY(-1px);
  box-shadow:
    0 4px 10px rgba(0, 0, 0, 0.1),
    0 2px 3px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.accept-card :deep(.el-button:not(.el-button--primary):active) {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.1),
    inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* ========== 上传区域样式 ========== */
.accept-card :deep(.el-upload--picture-card) {
  border: 2px dashed rgba(10, 77, 62, 0.3);
  border-radius: 8px;
  transition: all 0.3s ease;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(245, 245, 245, 0.8));
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.accept-card :deep(.el-upload--picture-card:hover) {
  border-color: #0D5D46;
  background: linear-gradient(145deg, rgba(255, 255, 255, 1), rgba(250, 250, 250, 0.9));
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .accept-card :deep(.el-form-item__label) {
    float: none;
    text-align: left;
    padding-bottom: 8px;
  }

  .accept-card :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
