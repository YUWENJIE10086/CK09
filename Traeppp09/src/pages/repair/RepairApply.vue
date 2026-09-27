<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Edit } from '@element-plus/icons-vue'
import { applyRepair } from '@/api/repair'
import { getBarnOptions } from '@/api/barn'

const router = useRouter()

/** 烤房下拉数据 */
const barnOptions = ref<any[]>([])

/** 14个部件 */
const componentOptions = [
  '加热炉', '散热管', '回风管', '进风口', '排湿口',
  '烟架', '挂烟梁', '装烟门',
  '温湿度传感器', '自控仪', '执行器',
  '循环风机', '排湿风机', '助燃风机',
]

/** 维修类型 */
const repairTypeOptions = [
  { value: '常规维护', label: '常规维护' },
  { value: '专项修复', label: '专项修复' },
  { value: '再利用', label: '再利用' },
  { value: '退出', label: '退出' },
]

/** 紧迫性 */
const urgencyOptions = [
  { value: '紧急建议', label: '紧急建议' },
  { value: '暂缓', label: '暂缓' },
  { value: '正常', label: '正常' },
]

/** 表单数据 */
const form = reactive({
  ovenId: null as string | null,
  repairType: '常规维护',
  urgency: '正常',
  damageDesc: '',
  photos: [] as any[],
  estimatedCost: 0,
  relatedComponents: [] as string[],
  applicant: '',
})

/** 表单引用 */
const formRef = ref()

/** 表单规则 */
const rules = {
  ovenId: [{ required: true, message: '请选择烤房', trigger: 'change' }],
  repairType: [{ required: true, message: '请选择维修类型', trigger: 'change' }],
  urgency: [{ required: true, message: '请选择紧迫性', trigger: 'change' }],
  damageDesc: [{ required: true, message: '请填写损坏描述', trigger: 'blur' }],
}

/** 提交中 */
const submitting = ref(false)

/** 加载烤房选项 */
async function loadBarns() {
  try {
    barnOptions.value = await getBarnOptions()
  } catch {
    barnOptions.value = []
  }
}

/** 提交 - 同步写入 MySQL */
async function handleSubmit() {
  try {
    await formRef.value.validate()
    submitting.value = true
    const barn = barnOptions.value.find(b => b.id === form.ovenId)
    await applyRepair({
      ovenId: form.ovenId as string,
      repairType: form.repairType,
      urgency: form.urgency,
      applyDesc: form.damageDesc,
      estimatedCost: form.estimatedCost,
      applicant: form.applicant || '烟农',
      remark: form.relatedComponents.length ? `关联部件：${form.relatedComponents.join('、')}` : '',
    })
    ElMessage.success('维修提报成功，已写入数据库')
    router.push('/repair/list')
  } catch (e: any) {
    if (e?.message) ElMessage.error('提报失败：' + e.message)
  } finally {
    submitting.value = false
  }
}

/** 取消 */
function handleCancel() {
  router.push('/repair/list')
}

onMounted(() => {
  loadBarns()
})
</script>

<template>
  <div class="page-container repair-apply">
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><Edit /></el-icon>
        </div>
        <h2 class="page-title">维修提报</h2>
      </div>
      <p class="page-desc">填写维修申请信息，提交后将自动写入 MySQL 数据库并进入审核流程</p>
    </div>

    <div class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 720px">
        <el-form-item label="选择烤房" prop="ovenId">
          <el-select v-model="form.ovenId" placeholder="请选择烤房" filterable clearable style="width: 100%">
            <el-option
              v-for="barn in barnOptions"
              :key="barn.id"
              :label="`${barn.barnName} (${barn.id || ''})`"
              :value="barn.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="维修类型" prop="repairType">
          <el-radio-group v-model="form.repairType">
            <el-radio v-for="opt in repairTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="紧迫性" prop="urgency">
          <el-radio-group v-model="form.urgency">
            <el-radio v-for="opt in urgencyOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="损坏描述" prop="damageDesc">
          <el-input
            v-model="form.damageDesc"
            type="textarea"
            :rows="4"
            placeholder="请详细描述损坏情况、影响范围等"
          />
        </el-form-item>

        <el-form-item label="损坏照片">
          <el-upload
            action="#"
            :auto-upload="false"
            :file-list="form.photos"
            :on-change="(file: any, list: any) => form.photos = list"
            list-type="picture-card"
            accept="image/*"
          >
            <el-icon><UploadFilled /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="预估费用">
          <el-input-number
            v-model="form.estimatedCost"
            :min="0"
            :step="500"
            :precision="0"
            controls-position="right"
            style="width: 220px"
          />
          <span style="margin-left: 8px; color: var(--text-muted)">元</span>
        </el-form-item>

        <el-form-item label="关联部件">
          <el-checkbox-group v-model="form.relatedComponents">
            <el-checkbox
              v-for="comp in componentOptions"
              :key="comp"
              :value="comp"
            >{{ comp }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="申请人">
          <el-input v-model="form.applicant" placeholder="选填，默认烟农" style="width: 220px" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
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

/* ========== 毛玻璃表单卡片样式 ========== */
.form-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: clamp(20px, 3vw, 32px);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.form-card:hover {
  transform: translateY(-3px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* 表单项间距优化 */
.form-card :deep(.el-form-item) {
  margin-bottom: clamp(18px, 2vw, 24px);
}

/* ========== 轻拟态按钮样式 ========== */
.form-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 8px rgba(10, 77, 62, 0.25),
    0 1px 2px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.form-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #107055);
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.35),
    0 2px 4px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.form-card :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(10, 77, 62, 0.2),
    inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 普通按钮轻拟态 */
.form-card :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff, #f5f5f5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.08),
    0 1px 2px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transition: all 0.2s ease;
}

.form-card :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(145deg, #ffffff, #fafafa);
  transform: translateY(-1px);
  box-shadow:
    0 4px 10px rgba(0, 0, 0, 0.1),
    0 2px 3px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.form-card :deep(.el-button:not(.el-button--primary):active) {
  transform: translateY(0);
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.1),
    inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* ========== 上传区域样式 ========== */
.form-card :deep(.el-upload--picture-card) {
  border: 2px dashed rgba(10, 77, 62, 0.3);
  border-radius: 8px;
  transition: all 0.3s ease;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(245, 245, 245, 0.8));
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.form-card :deep(.el-upload--picture-card:hover) {
  border-color: #0D5D46;
  background: linear-gradient(145deg, rgba(255, 255, 255, 1), rgba(250, 250, 250, 0.9));
  transform: translateY(-2px);
  box-shadow:
    0 4px 12px rgba(10, 77, 62, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

/* ========== 单选框组样式 ========== */
.form-card :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.form-card :deep(.el-radio) {
  margin-right: 0;
}

/* ========== 输入框聚焦样式 ========== */
.form-card :deep(.el-input__wrapper:focus-within),
.form-card :deep(.el-textarea__inner:focus) {
  box-shadow:
    0 0 0 2px rgba(10, 77, 62, 0.2),
    0 2px 6px rgba(0, 0, 0, 0.05);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .form-card {
    padding: clamp(16px, 2vw, 20px);
  }

  .form-card :deep(.el-form-item__label) {
    float: none;
    text-align: left;
    padding-bottom: 8px;
  }

  .form-card :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
