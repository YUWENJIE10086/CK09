<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Check, View } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

/** 当前步骤 */
const currentStep = ref(0)

/** 上传相关 */
const uploadRef = ref()
const fileList = ref<UploadFile[]>([])
const hasUploaded = ref(false)

/** 校验结果 */
interface ValidationResult {
  name: string
  status: 'pass' | 'fail'
  detail: string
}

const validationResults = ref<ValidationResult[]>([])

/** 导入进度 */
const importProgress = ref(0)
const importing = ref(false)
const importDone = ref(false)

/** 导入统计 */
const importStats = reactive({
  total: 0,
  success: 0,
  failed: 0,
})

/** 历史更新记录 */
interface HistoryRecord {
  id: number
  updateTime: string
  operator: string
  recordCount: number
  status: string
}

const historyRecords = ref<HistoryRecord[]>([
  { id: 1, updateTime: '2026-05-20 14:30:00', operator: '管理员', recordCount: 1280, status: '已完成' },
  { id: 2, updateTime: '2025-12-15 10:15:00', operator: '张伟', recordCount: 1156, status: '已完成' },
  { id: 3, updateTime: '2025-06-10 09:00:00', operator: '管理员', recordCount: 1020, status: '已完成' },
  { id: 4, updateTime: '2024-12-20 16:45:00', operator: '李强', recordCount: 980, status: '已完成' },
  { id: 5, updateTime: '2024-06-15 11:30:00', operator: '管理员', recordCount: 850, status: '已完成' },
])

/** 步骤1：下载模板 */
function handleDownloadTemplate() {
  ElMessage.success('模板下载成功')
}

/** 步骤2：上传文件 */
function handleUploadSuccess() {
  hasUploaded.value = true
  currentStep.value = 2
  // 模拟校验结果
  validationResults.value = [
    { name: '必填项检查', status: 'pass', detail: '所有必填字段均已填写，共检查1,280条记录' },
    { name: '格式检查', status: 'pass', detail: '日期格式、编号格式、数值范围均符合规范' },
    { name: '逻辑一致性检查', status: 'fail', detail: '发现3条记录的建成年份晚于首次使用年份，请核实' },
    { name: '重复数据检查', status: 'pass', detail: '未发现重复的烤房编号' },
  ]
}

function handleUploadError() {
  ElMessage.error('上传失败，请重试')
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件，请先删除已上传文件')
}

function handleRemoveFile() {
  hasUploaded.value = false
  fileList.value = []
  currentStep.value = 1
}

/** 步骤3：确认校验完成，进入步骤4 */
function handleValidationConfirm() {
  currentStep.value = 3
}

/** 步骤4：确认导入 */
function handleImport() {
  importing.value = true
  importProgress.value = 0
  importDone.value = false

  const timer = setInterval(() => {
    importProgress.value += Math.random() * 15
    if (importProgress.value >= 100) {
      importProgress.value = 100
      clearInterval(timer)
      importing.value = false
      importDone.value = true
      importStats.total = 1280
      importStats.success = 1277
      importStats.failed = 3
      ElMessage.success('数据导入完成')
    }
  }, 300)
}

/** 重新开始 */
function handleReset() {
  currentStep.value = 0
  hasUploaded.value = false
  fileList.value = []
  validationResults.value = []
  importProgress.value = 0
  importing.value = false
  importDone.value = false
}

/** 查看历史详情 */
function handleViewDetail(row: HistoryRecord) {
  ElMessage.info(`查看 ${row.updateTime} 的更新详情`)
}
</script>

<template>
  <div class="annual-update-page">
    <!-- 说明卡片 -->
    <el-alert
      title="年度数据更新说明"
      type="info"
      :closable="false"
      show-icon
      description="年度数据更新用于每年对烤房基础信息进行批量更新。请按照以下步骤操作：下载模板 → 填写数据 → 上传文件 → 数据校验 → 确认导入。校验通过后数据将正式入库，请确保数据准确。"
    />

    <!-- 步骤条 -->
    <el-card class="step-card" shadow="never">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="下载模板" description="下载Excel模板" />
        <el-step title="上传数据" description="上传填写好的文件" />
        <el-step title="数据校验" description="系统自动校验" />
        <el-step title="确认导入" description="确认并导入数据" />
      </el-steps>
    </el-card>

    <!-- 步骤内容 -->
    <el-card class="content-card" shadow="never">
      <!-- 步骤1：下载模板 -->
      <div v-if="currentStep === 0" class="step-content">
        <div class="step-icon-wrapper">
          <el-icon :size="48" color="#1A3C6E"><Download /></el-icon>
        </div>
        <h3 class="step-title">下载导入模板</h3>
        <p class="step-desc">请先下载标准Excel模板，按照模板格式填写烤房年度数据</p>
        <el-button type="primary" :icon="Download" size="large" @click="handleDownloadTemplate(); currentStep = 1">
          下载模板
        </el-button>
      </div>

      <!-- 步骤2：上传数据 -->
      <div v-if="currentStep === 1" class="step-content">
        <div class="step-icon-wrapper">
          <el-icon :size="48" color="#1A3C6E"><Upload /></el-icon>
        </div>
        <h3 class="step-title">上传数据文件</h3>
        <p class="step-desc">请上传填写好的Excel文件（支持 .xlsx / .xls 格式）</p>
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleUploadSuccess"
          :on-error="handleUploadError"
          :on-exceed="handleExceed"
          v-model:file-list="fileList"
          class="upload-area"
        >
          <el-icon :size="40" color="#C0C4CC"><Upload /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 .xlsx / .xls 格式，文件大小不超过10MB</div>
          </template>
        </el-upload>
        <div v-if="hasUploaded" class="upload-actions">
          <el-button type="primary" @click="currentStep = 2">下一步：数据校验</el-button>
          <el-button @click="handleRemoveFile">重新上传</el-button>
        </div>
      </div>

      <!-- 步骤3：数据校验 -->
      <div v-if="currentStep === 2" class="step-content">
        <div class="step-icon-wrapper">
          <el-icon :size="48" color="#1A3C6E"><Check /></el-icon>
        </div>
        <h3 class="step-title">数据校验结果</h3>
        <p class="step-desc">系统已自动完成数据校验，请查看校验结果</p>
        <el-table :data="validationResults" stripe border style="width: 100%; margin-top: 20px;">
          <el-table-column prop="name" label="校验项" width="180" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.status === 'pass'" class="status-pass">✓ 通过</span>
              <span v-else class="status-fail">✗ 失败</span>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="300" />
        </el-table>
        <div class="step-actions">
          <el-button @click="handleReset">返回重传</el-button>
          <el-button type="primary" @click="handleValidationConfirm">下一步：确认导入</el-button>
        </div>
      </div>

      <!-- 步骤4：确认导入 -->
      <div v-if="currentStep === 3" class="step-content">
        <template v-if="!importDone">
          <div class="step-icon-wrapper">
            <el-icon :size="48" color="#1A3C6E"><Check /></el-icon>
          </div>
          <h3 class="step-title">确认导入数据</h3>
          <p class="step-desc">校验已完成，点击确认按钮开始导入数据</p>
          <div v-if="importing" class="import-progress">
            <el-progress :percentage="Math.floor(importProgress)" :stroke-width="20" :text-inside="true" status="success" />
            <p class="progress-text">正在导入数据，请稍候...</p>
          </div>
          <div v-else class="step-actions">
            <el-button @click="handleReset">返回重传</el-button>
            <el-button type="primary" size="large" @click="handleImport">确认导入</el-button>
          </div>
        </template>
        <template v-else>
          <div class="import-result">
            <el-icon :size="64" color="#67C23A"><Check /></el-icon>
            <h3 class="step-title">导入完成</h3>
            <div class="result-stats">
              <div class="result-item">
                <span class="result-label">总记录数</span>
                <span class="result-value">{{ importStats.total }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">成功导入</span>
                <span class="result-value" style="color:#67C23A">{{ importStats.success }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">导入失败</span>
                <span class="result-value" style="color:#F56C6C">{{ importStats.failed }}</span>
              </div>
            </div>
            <el-button type="primary" @click="handleReset">继续更新</el-button>
          </div>
        </template>
      </div>
    </el-card>

    <!-- 历史更新记录 -->
    <el-card class="history-card" shadow="never">
      <template #header>
        <span class="card-title">历史更新记录</span>
      </template>
      <el-table :data="historyRecords" stripe border style="width: 100%">
        <el-table-column prop="updateTime" label="更新时间" width="200" />
        <el-table-column prop="operator" label="操作人" width="120" align="center" />
        <el-table-column prop="recordCount" label="更新记录数" width="120" align="center" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="success" size="default">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>查看详情
            </button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
/* ==================== 毛玻璃质感设计系统 ==================== */
.annual-update-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 1.5vw, 18px);
}

/* 提示框增强 */
.annual-update-page :deep(.el-alert) {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.08) 0%, rgba(64, 158, 255, 0.04) 100%);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 8px;
  border: 1px solid rgba(64, 158, 255, 0.15);
}

/* 毛玻璃步骤卡片 */
.step-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.step-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.step-card :deep(.el-card__body) {
  padding: clamp(18px, 2.2vw, 28px) clamp(28px, 3.5vw, 48px);
}

/* 毛玻璃内容卡片 */
.content-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.content-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.content-card:hover {
  box-shadow:
    0 6px 24px rgba(10, 77, 62, 0.08),
    0 12px 40px rgba(10, 77, 62, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.content-card :deep(.el-card__body) {
  padding: clamp(24px, 3vw, 38px);
}

/* 步骤内容 */
.step-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: clamp(14px, 1.8vw, 24px) 0;
}

.step-icon-wrapper {
  width: clamp(64px, 8vw, 88px);
  height: clamp(64px, 8vw, 88px);
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(10, 77, 62, 0.08) 0%, rgba(10, 77, 62, 0.04) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: clamp(12px, 1.5vw, 18px);
}

.step-title {
  font-size: clamp(16px, 1.8vw, 20px);
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.step-desc {
  font-size: clamp(13px, 1.5vw, 15px);
  color: #909399;
  margin: 0 0 clamp(18px, 2.2vw, 28px);
}

/* 轻拟态按钮 */
.content-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 4px rgba(10, 77, 62, 0.2),
    0 4px 8px rgba(10, 77, 62, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.content-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  box-shadow:
    0 4px 8px rgba(10, 77, 62, 0.25),
    0 8px 16px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.content-card :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 2px rgba(10, 77, 62, 0.2),
    inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.upload-area {
  width: clamp(320px, 40vw, 520px);
}

.step-actions {
  display: flex;
  gap: clamp(10px, 1.2vw, 14px);
  margin-top: clamp(18px, 2.2vw, 28px);
}

.upload-actions {
  display: flex;
  gap: clamp(10px, 1.2vw, 14px);
  margin-top: clamp(14px, 1.8vw, 20px);
}

.status-pass {
  color: #2E8B6A;
  font-weight: 600;
}

.status-fail {
  color: #D4604A;
  font-weight: 600;
}

.import-progress {
  width: clamp(320px, 40vw, 420px);
  margin-top: clamp(18px, 2.2vw, 28px);
}

.progress-text {
  text-align: center;
  color: #909399;
  font-size: clamp(12px, 1.4vw, 14px);
  margin-top: clamp(10px, 1.2vw, 14px);
}

.import-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: clamp(14px, 1.8vw, 24px) 0;
}

.result-stats {
  display: flex;
  gap: clamp(32px, 4vw, 56px);
  margin: clamp(18px, 2.2vw, 28px) 0;
}

.result-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.result-label {
  font-size: clamp(12px, 1.4vw, 14px);
  color: #909399;
}

.result-value {
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 700;
  color: #303133;
}

/* 毛玻璃历史卡片 */
.history-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.history-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.history-card:hover {
  box-shadow:
    0 6px 24px rgba(10, 77, 62, 0.08),
    0 12px 40px rgba(10, 77, 62, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.card-title {
  font-size: clamp(14px, 1.6vw, 16px);
  font-weight: 600;
  color: #303133;
}

/* 表格操作按钮 */
.table-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.table-action-btn--primary {
  color: #0A4D3E;
}

.table-action-btn--primary:hover {
  color: #0D5D46;
  background: rgba(10, 77, 62, 0.06);
  border-radius: 4px;
}
</style>
