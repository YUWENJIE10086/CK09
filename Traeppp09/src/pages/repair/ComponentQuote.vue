<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, PriceTag } from '@element-plus/icons-vue'
import { getQuoteList, addQuote, updateQuote, deleteQuote } from '@/api/component-quote'

/** 筛选表单 */
const filterForm = reactive({
  itemName: '',
})

/** 表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

/** 加载列表数据 */
async function loadData() {
  loading.value = true
  try {
    const res: any = await getQuoteList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      itemName: filterForm.itemName || undefined,
    })
    tableData.value = res?.rows || []
    total.value = res?.total ?? 0
  } catch (error) {
    console.error('加载部件报价列表失败:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

/** 重置 */
function handleReset() {
  filterForm.itemName = ''
  pagination.pageNum = 1
  loadData()
}

/** 分页变化 */
function handlePageChange() {
  loadData()
}

/** 格式化单价 */
function formatPrice(val: any): string {
  if (val == null || val === '') return '-'
  const num = Number(val)
  if (isNaN(num)) return '-'
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/** 新增/编辑弹窗 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增部件报价')
const submitLoading = ref(false)
const form = reactive({
  id: null as number | null,
  itemName: '',
  specModel: '',
  unit: '',
  unitPrice: null as number | null,
  remark: '',
})

/** 单位选项 */
const unitOptions = ['个', '件', '套', '台', '根', '块', '米', '组', '批', '公斤']

/** 重置表单 */
function resetForm() {
  form.id = null
  form.itemName = ''
  form.specModel = ''
  form.unit = ''
  form.unitPrice = null
  form.remark = ''
}

/** 打开新增弹窗 */
function handleAdd() {
  resetForm()
  dialogTitle.value = '新增部件报价'
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function handleEdit(row: any) {
  resetForm()
  form.id = row.id
  form.itemName = row.itemName || ''
  form.specModel = row.specModel || ''
  form.unit = row.unit || ''
  form.unitPrice = row.unitPrice != null ? Number(row.unitPrice) : null
  form.remark = row.remark || ''
  dialogTitle.value = '编辑部件报价'
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  if (!form.itemName.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  submitLoading.value = true
  try {
    const payload: any = {
      itemName: form.itemName.trim(),
      specModel: form.specModel.trim(),
      unit: form.unit,
      unitPrice: form.unitPrice,
      remark: form.remark.trim(),
    }
    if (form.id != null) {
      payload.id = form.id
      await updateQuote(payload)
      ElMessage.success('修改成功')
    } else {
      await addQuote(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存部件报价失败:', error)
  } finally {
    submitLoading.value = false
  }
}

/** 删除 */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除报价"${row.itemName}"吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteQuote(row.id)
    ElMessage.success('删除成功')
    // 如果删除后当前页空了，回退一页
    if (tableData.value.length === 1 && pagination.pageNum > 1) {
      pagination.pageNum--
    }
    loadData()
  } catch (error) {
    // 用户取消删除时不提示
    if (error !== 'cancel') {
      console.error('删除部件报价失败:', error)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><PriceTag /></el-icon>
        </div>
        <div class="page-title-text">
          <h2 class="page-title">部件报价</h2>
          <p class="page-desc">管理烤房维修部件的报价信息，支持按名称检索</p>
        </div>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="项目名称">
          <el-input
            v-model="filterForm.itemName"
            placeholder="请输入项目名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮区 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增报价</el-button>
    </div>

    <!-- 表格区 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="itemName" label="项目名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="specModel" label="规格型号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.specModel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.unit || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unitPrice" label="单价" width="140" align="center">
          <template #default="{ row }">
            <span class="price-text">{{ formatPrice(row.unitPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close append-to-body align-center>
      <el-form :model="form" label-width="90px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.itemName" placeholder="请输入项目名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="form.specModel" placeholder="请输入规格型号" maxlength="100" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位">
              <el-select v-model="form.unit" placeholder="请选择单位" clearable filterable allow-create style="width: 100%">
                <el-option v-for="u in unitOptions" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价(元)">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="1" placeholder="请输入单价" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="说明">
          <el-input type="textarea" v-model="form.remark" :rows="3" placeholder="请输入说明" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100%;
  padding: 11px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ===== 页面标题 ===== */
.page-header {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.75) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 8px 11px;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
}

.page-title-wrap {
  display: flex;
  align-items: flex-start;
  gap: 7px;
}

.page-title-icon {
  width: clamp(36px, 3vw, 44px);
  height: clamp(36px, 3vw, 44px);
  border-radius: 50%;
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 50%, #1A6B4F 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 3px 8px rgba(10, 77, 62, 0.35);
  flex-shrink: 0;
}

.page-title-text {
  flex: 1;
}

.page-title {
  font-size: clamp(18px, 1.8vw, 24px);
  font-weight: 700;
  color: #0D3D30;
  margin: 0;
}

.page-desc {
  font-size: clamp(12px, 1vw, 14px);
  color: #5A8A7A;
  margin: 4px 0 0;
}

/* ===== 筛选区 ===== */
.filter-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 7px 8px 0;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
}

.filter-card :deep(.el-form-item) {
  margin-right: 14px;
  margin-bottom: 7px;
}

/* ===== 操作按钮区 ===== */
.action-bar {
  display: flex;
  gap: 7px;
}

/* ===== 表格区 ===== */
.table-card {
  flex: 1;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 7px;
  box-shadow: 0 4px 14px rgba(26, 107, 79, 0.12);
  display: flex;
  flex-direction: column;
}

.table-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(26, 107, 79, 0.06);
}

.table-card :deep(.el-table__row:hover > td) {
  background: rgba(26, 107, 79, 0.04) !important;
}

/* 单价文字高亮 */
.price-text {
  color: #0D5D46;
  font-weight: 600;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}
</style>
