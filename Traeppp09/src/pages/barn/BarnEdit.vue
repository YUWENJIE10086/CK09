<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBarnInfo, updateBarn } from '@/api/barn'
import { listCounty, listTownship } from '@/api/dict'

const route = useRoute()
const router = useRouter()

/** 当前步骤 */
const currentStep = ref(0)

/** 5大部位14个部件定义 */
const partGroups = [
  {
    partType: 'JR',
    partName: '加热系统',
    icon: '🔥',
    components: [
      { key: 'jr_heater', name: '加热炉' },
    ],
  },
  {
    partType: 'SR',
    partName: '散热系统',
    icon: '💨',
    components: [
      { key: 'sr_pipe', name: '散热管' },
      { key: 'sr_return', name: '回风管' },
      { key: 'sr_inlet', name: '进风口' },
      { key: 'sr_outlet', name: '排湿口' },
    ],
  },
  {
    partType: 'ZK',
    partName: '装炕系统',
    icon: '🏗️',
    components: [
      { key: 'zk_rack', name: '烟架' },
      { key: 'zk_beam', name: '挂烟梁' },
      { key: 'zk_door', name: '装烟门' },
    ],
  },
  {
    partType: 'ZT',
    partName: '自控系统',
    icon: '🎛️',
    components: [
      { key: 'zt_sensor', name: '温湿度传感器' },
      { key: 'zt_controller', name: '自控仪' },
      { key: 'zt_actuator', name: '执行器' },
    ],
  },
  {
    partType: 'FS',
    partName: '风机系统',
    icon: '🔄',
    components: [
      { key: 'fs_circulation', name: '循环风机' },
      { key: 'fs_exhaust', name: '排湿风机' },
      { key: 'fs_combustion', name: '助燃风机' },
    ],
  },
]

/** 部件初始状态选项 */
const componentStatusOptions = [
  { value: '正常', label: '正常' },
  { value: '损坏', label: '损坏' },
  { value: '缺失', label: '缺失' },
]

/** Step1 基本信息表单 */
const basicForm = reactive({
  barnName: '',
  id: '',
  barnType: '',
  capacity: null as number | null,
  benefitArea: null as number | null,
  buildYear: null as number | null,
  buildCost: null as number | null,
  countyCode: '',
  townCode: '',
  stationName: '',
  cooperativeName: '',
})

/** Step2 位置信息表单 */
const locationForm = reactive({
  address: '',
  longitude: null as number | null,
  latitude: null as number | null,
})

/** Step3 部件信息 */
const componentForm = reactive<Record<string, string>>({})

// 初始化部件状态
partGroups.forEach(group => {
  group.components.forEach(comp => {
    componentForm[comp.key] = '正常'
  })
})

/** 县区选项（从后端字典拉取） */
const countyOptions = ref<{ value: string; label: string }[]>([])
/** 乡镇选项 */
const townOptions = ref<{ value: string; label: string }[]>([])

async function loadCounties() {
  try {
    const list: any[] = await listCounty()
    countyOptions.value = (list || []).map((c: any) => ({ value: c.countyCode, label: c.countyName }))
  } catch {
    countyOptions.value = []
  }
}

async function loadTowns(countyCode: string) {
  if (!countyCode) { townOptions.value = []; return }
  try {
    const list: any[] = await listTownship(countyCode)
    townOptions.value = (list || []).map((t: any) => ({ value: t.townshipCode, label: t.townshipName }))
  } catch {
    townOptions.value = []
  }
}

watch(() => basicForm.countyCode, (v) => {
  basicForm.townCode = ''
  loadTowns(v)
})

/** 烤房类型选项 */
const barnTypeOptions = [
  { value: '气流下降式', label: '密集式（气流下降式）' },
  { value: '气流上升式', label: '普通（气流上升式）' },
]

/** Step1校验规则 */
const basicRules = {
  barnName: [{ required: true, message: '请输入烤房名称', trigger: 'blur' }],
  id: [{ required: true, message: '请输入烤房编号', trigger: 'blur' }],
  barnType: [{ required: true, message: '请选择烤房类型', trigger: 'change' }],
  countyCode: [{ required: true, message: '请选择所属县区', trigger: 'change' }],
}

/** 步骤是否可前进 */
const step1Valid = computed(() => {
  return basicForm.barnName && basicForm.id && basicForm.barnType && basicForm.countyCode
})

const step2Valid = computed(() => {
  return locationForm.address
})

/** 加载状态 */
const loading = ref(false)

/** 部件状态映射（从mock部件状态到表单状态） */
const mockStatusToFormStatus: Record<string, string> = {
  '正常': '正常',
  '轻微损坏': '损坏',
  '中度损坏': '损坏',
  '严重损坏': '损坏',
  '报废': '缺失',
}

/** 加载烤房数据 */
async function loadBarnData() {
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
    // 填充基本信息
    Object.assign(basicForm, {
      barnName: data.barnName,
      id: data.id,
      barnType: data.projectType || data.barnType,
      capacity: data.lengthM || data.capacity,
      benefitArea: data.benefitArea,
      buildYear: data.completeDate ? Number(String(data.completeDate).slice(0, 4)) : data.buildYear,
      countyCode: data.countyCode,
      townCode: data.townCode,
    })
    // 填充位置信息
    Object.assign(locationForm, {
      address: data.address || data.gpsAddress,
      longitude: data.longitude || data.gpsLongitude,
      latitude: data.latitude || data.gpsLatitude,
    })
    // 填充部件信息
    if (data.components) {
      const componentNameToKey: Record<string, string> = {}
      partGroups.forEach(group => {
        group.components.forEach(comp => {
          componentNameToKey[comp.name] = comp.key
        })
      })
      data.components.forEach((comp: any) => {
        const key = componentNameToKey[comp.componentName]
        if (key) {
          componentForm[key] = mockStatusToFormStatus[comp.status] || '正常'
        }
      })
    }
  } finally {
    loading.value = false
  }
}

/** 下一步 */
function handleNext() {
  if (currentStep.value === 0) {
    if (!step1Valid.value) {
      ElMessage.warning('请填写必填项')
      return
    }
  } else if (currentStep.value === 1) {
    if (!step2Valid.value) {
      ElMessage.warning('请填写地址信息')
      return
    }
  }
  currentStep.value++
}

/** 上一步 */
function handlePrev() {
  currentStep.value--
}

/** 保存修改 - 同步到 MySQL */
async function handleSave() {
  try {
    const county = countyOptions.value.find(c => c.value === basicForm.countyCode)
    const town = townOptions.value.find(t => t.value === basicForm.townCode)
    await updateBarn({
      id: String(route.params.id),
      barnName: basicForm.barnName,
      projectType: basicForm.barnType,
      lengthM: basicForm.capacity || 0,
      projectCost: basicForm.buildCost || 0,
      completeDate: basicForm.buildYear ? `${basicForm.buildYear}-12-31` : null,
      countyCode: basicForm.countyCode,
      county: county?.label,
      townCode: basicForm.townCode,
      township: town?.label,
      address: locationForm.address,
      longitude: locationForm.longitude || 0,
      latitude: locationForm.latitude || 0,
    })
    ElMessage.success('修改成功，已写入 MySQL')
    router.push({ name: 'BarnList' })
  } catch (e: any) {
    ElMessage.error('修改失败：' + (e?.message || '请重试'))
  }
}

onMounted(async () => {
  await loadCounties()
  loadBarnData()
})
</script>

<template>
  <div class="barn-edit-page" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-icon">
        <el-icon><Edit /></el-icon>
      </div>
      <h1 class="page-title-text">编辑烤房</h1>
    </div>

    <div class="edit-card glass-card">
      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" align-center class="step-bar">
        <el-step title="基本信息" />
        <el-step title="位置信息" />
        <el-step title="部件信息" />
        <el-step title="确认修改" />
      </el-steps>

      <!-- Step1 基本信息 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-form :model="basicForm" :rules="basicRules" label-width="110px" class="step-form">
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="烤房名称" prop="barnName">
                <el-input v-model="basicForm.barnName" placeholder="请输入烤房名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="烤房编号" prop="id">
                <el-input v-model="basicForm.id" placeholder="请输入烤房编号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="烤房类型" prop="barnType">
                <el-select v-model="basicForm.barnType" placeholder="请选择类型" style="width: 100%">
                  <el-option v-for="opt in barnTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="容量（竿）">
                <el-input-number v-model="basicForm.capacity" :min="0" :max="1000" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="受益面积（亩）">
                <el-input-number v-model="basicForm.benefitArea" :min="0" :max="500" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="建设年度">
                <el-date-picker v-model="basicForm.buildYear" type="year" placeholder="请选择年度" style="width: 100%" value-format="YYYY" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="建设费用（元）">
                <el-input-number v-model="basicForm.buildCost" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属县区" prop="countyCode">
                <el-select v-model="basicForm.countyCode" placeholder="请选择县区" style="width: 100%">
                  <el-option v-for="opt in countyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="乡镇">
                <el-select v-model="basicForm.townCode" placeholder="请选择乡镇" style="width: 100%" :disabled="!basicForm.countyCode">
                  <el-option v-for="opt in townOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="烟站">
                <el-input v-model="basicForm.stationName" placeholder="请输入烟站名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合作社">
                <el-input v-model="basicForm.cooperativeName" placeholder="请输入合作社名称" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- Step2 位置信息 -->
      <div v-show="currentStep === 1" class="step-content">
        <el-form :model="locationForm" label-width="110px" class="step-form">
          <el-row :gutter="24">
            <el-col :span="24">
              <el-form-item label="详细地址">
                <el-input v-model="locationForm.address" placeholder="请输入详细地址" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="经度">
                <el-input-number v-model="locationForm.longitude" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="纬度">
                <el-input-number v-model="locationForm.latitude" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="地图选点">
                <div class="map-placeholder glass-inner">
                  <el-icon :size="32" color="#C0C4CC"><MapLocation /></el-icon>
                  <p>地图选点功能需配置API Key</p>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- Step3 部件信息 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="component-groups">
          <div v-for="group in partGroups" :key="group.partType" class="component-group glass-inner">
            <div class="group-header">
              <span class="group-icon">{{ group.icon }}</span>
              <span class="group-name">{{ group.partName }}</span>
            </div>
            <el-row :gutter="16">
              <el-col :span="8" v-for="comp in group.components" :key="comp.key">
                <div class="component-item">
                  <span class="comp-label">{{ comp.name }}</span>
                  <el-select v-model="componentForm[comp.key]" size="small" style="width: clamp(100px, 12vw, 120px)">
                    <el-option v-for="opt in componentStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>

      <!-- Step4 确认修改 -->
      <div v-show="currentStep === 3" class="step-content">
        <el-descriptions title="基本信息" :column="2" border class="confirm-section glass-inner">
          <el-descriptions-item label="烤房名称">{{ basicForm.barnName }}</el-descriptions-item>
          <el-descriptions-item label="烤房编号">{{ basicForm.id }}</el-descriptions-item>
          <el-descriptions-item label="烤房类型">{{ basicForm.barnType }}</el-descriptions-item>
          <el-descriptions-item label="容量">{{ basicForm.capacity }}竿</el-descriptions-item>
          <el-descriptions-item label="受益面积">{{ basicForm.benefitArea }}亩</el-descriptions-item>
          <el-descriptions-item label="建设年度">{{ basicForm.buildYear }}</el-descriptions-item>
          <el-descriptions-item label="所属县区">{{ countyOptions.find(c => c.value === basicForm.countyCode)?.label }}</el-descriptions-item>
          <el-descriptions-item label="乡镇">{{ townOptions.find(t => t.value === basicForm.townCode)?.label || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="位置信息" :column="2" border class="confirm-section glass-inner">
          <el-descriptions-item label="详细地址" :span="2">{{ locationForm.address }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ locationForm.longitude }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ locationForm.latitude }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="部件信息" :column="3" border class="confirm-section glass-inner">
          <template v-for="group in partGroups" :key="group.partType">
            <el-descriptions-item v-for="comp in group.components" :key="comp.key" :label="comp.name">
              <el-tag :type="componentForm[comp.key] === '正常' ? 'success' : componentForm[comp.key] === '损坏' ? 'danger' : 'warning'" size="small">
                {{ componentForm[comp.key] }}
              </el-tag>
            </el-descriptions-item>
          </template>
        </el-descriptions>
      </div>

      <!-- 底部按钮 -->
      <div class="step-actions">
        <button v-if="currentStep > 0" class="neumorphic-btn" @click="handlePrev">
          上一步
        </button>
        <button v-if="currentStep < 3" class="primary-gradient-btn" @click="handleNext">
          下一步
        </button>
        <button v-if="currentStep === 3" class="primary-gradient-btn submit-btn" @click="handleSave">
          保存修改
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { MapLocation, Edit } from '@element-plus/icons-vue'
</script>

<style scoped>
/* 毛玻璃设计系统 - 编辑烤房页 */
.barn-edit-page {
  max-width: clamp(800px, 85vw, 960px);
  margin: 0 auto;
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  gap: clamp(12px, 1.5vw, 16px);
  margin-bottom: clamp(16px, 2vw, 24px);
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

.edit-card {
  padding: clamp(24px, 3vw, 36px) clamp(20px, 3vw, 40px);
}

/* 步骤条 */
.step-bar {
  margin-bottom: clamp(24px, 3vw, 36px);
  padding: 0 clamp(20px, 3vw, 40px);
}

.step-bar :deep(.el-step__head.is-finish) {
  color: #0A4D3E;
  border-color: #0A4D3E;
}

.step-bar :deep(.el-step__head.is-finish .el-step__icon) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  color: #fff;
  border-color: transparent;
}

.step-bar :deep(.el-step__head.is-process .el-step__icon) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.35);
}

.step-bar :deep(.el-step__title.is-finish) {
  color: #0A4D3E;
  font-weight: 600;
}

.step-bar :deep(.el-step__title.is-process) {
  color: #0A4D3E;
  font-weight: 600;
}

.step-content {
  min-height: clamp(300px, 40vw, 400px);
  padding: 0 clamp(12px, 2vw, 20px);
}

.step-form {
  max-width: 800px;
  margin: 0 auto;
}

/* 表单样式 */
.step-form :deep(.el-form-item__label) {
  color: #4A5568;
  font-weight: 500;
  padding-bottom: 8px;
}

.step-form :deep(.el-input__wrapper),
.step-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: inset 1px 1px 2px rgba(0, 0, 0, 0.05);
}

.step-form :deep(.el-input__wrapper:hover),
.step-form :deep(.el-select .el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.8);
}

/* 地图占位 */
.map-placeholder {
  width: 100%;
  height: clamp(180px, 25vw, 240px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(138, 168, 160, 0.4);
  color: #697386;
}

.map-placeholder p {
  margin-top: 8px;
  font-size: clamp(12px, 1vw, 13px);
}

/* 部件组 */
.component-groups {
  max-width: 800px;
  margin: 0 auto;
}

.component-group {
  margin-bottom: clamp(12px, 1.5vw, 20px);
  padding: clamp(12px, 1.5vw, 20px);
  transition: box-shadow 0.2s ease;
}

.component-group:hover {
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: clamp(12px, 1.5vw, 16px);
}

.group-icon {
  font-size: clamp(18px, 2vw, 20px);
}

.group-name {
  font-weight: 600;
  font-size: clamp(14px, 1.1vw, 15px);
  color: #1A1F36;
}

.component-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.comp-label {
  font-size: clamp(13px, 1vw, 14px);
  color: #4A5568;
}

/* 确认页描述列表 */
.confirm-section {
  margin-bottom: clamp(16px, 2vw, 24px);
}

.confirm-section :deep(.el-descriptions__label) {
  color: #697386 !important;
  font-weight: 500;
  font-size: clamp(12px, 1vw, 13px);
  background: rgba(248, 250, 252, 0.8) !important;
  padding: clamp(10px, 1.2vw, 12px) clamp(12px, 1.5vw, 16px) !important;
}

.confirm-section :deep(.el-descriptions__content) {
  color: #1A1F36 !important;
  padding: clamp(10px, 1.2vw, 12px) clamp(12px, 1.5vw, 16px) !important;
}

.confirm-section :deep(.el-descriptions__title) {
  font-size: clamp(14px, 1.1vw, 15px);
  font-weight: 600;
  color: #1A1F36;
}

.confirm-section :deep(.el-descriptions__body) {
  background: transparent;
}

.confirm-section :deep(.el-descriptions__table) {
  border-color: rgba(138, 168, 160, 0.2);
}

/* 轻拟态按钮 */
.neumorphic-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: clamp(10px, 1.2vw, 12px) clamp(20px, 2.5vw, 28px);
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

/* 主按钮深绿渐变 */
.primary-gradient-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: clamp(10px, 1.2vw, 12px) clamp(24px, 3vw, 32px);
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: clamp(13px, 1vw, 14px);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.35);
  transition: all 0.2s ease;
}

.primary-gradient-btn:hover {
  background: linear-gradient(135deg, #0D5D46, #0F6D52);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(10, 77, 62, 0.45);
}

.primary-gradient-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.35);
}

.submit-btn {
  padding: clamp(12px, 1.5vw, 14px) clamp(32px, 4vw, 40px);
}

/* 底部按钮 */
.step-actions {
  display: flex;
  justify-content: center;
  gap: clamp(10px, 1.2vw, 12px);
  margin-top: clamp(24px, 3vw, 36px);
  padding-top: clamp(16px, 2vw, 24px);
  border-top: 1px solid rgba(138, 168, 160, 0.2);
}

/* 响应式 */
@media (max-width: 768px) {
  .step-content {
    padding: 0 8px;
  }
  
  .step-bar {
    padding: 0 8px;
  }
  
  .step-form :deep(.el-col) {
    margin-bottom: 8px;
  }
}
</style>