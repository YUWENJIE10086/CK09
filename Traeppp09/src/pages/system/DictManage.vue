<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, InfoFilled } from '@element-plus/icons-vue'

/** 字典类型数据 */
interface DictTypeRecord {
  dictId: number
  dictName: string
  dictType: string
  status: '0' | '1'
  remark: string
  createTime: string
}

/** 字典数据项 */
interface DictDataRecord {
  dictCode: number
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: '0' | '1'
  remark: string
}

/** Mock字典类型数据 */
const mockDictTypes: DictTypeRecord[] = [
  { dictId: 1, dictName: '烤房状态', dictType: 'barn_status', status: '0', remark: '烤房状态列表', createTime: '2025-01-01 10:00:00' },
  { dictId: 2, dictName: '健康等级', dictType: 'health_level', status: '0', remark: '烤房健康等级', createTime: '2025-01-01 10:30:00' },
  { dictId: 3, dictName: '维修类型', dictType: 'repair_type', status: '0', remark: '维修类型列表', createTime: '2025-02-01 09:00:00' },
  { dictId: 4, dictName: '维修状态', dictType: 'repair_status', status: '0', remark: '维修状态列表', createTime: '2025-02-01 09:30:00' },
  { dictId: 5, dictName: '预约状态', dictType: 'reservation_status', status: '0', remark: '预约状态列表', createTime: '2025-03-01 14:00:00' },
  { dictId: 6, dictName: '用户状态', dictType: 'sys_normal_disable', status: '0', remark: '系统用户状态', createTime: '2025-01-01 08:00:00' },
  { dictId: 7, dictName: '性别', dictType: 'sys_user_sex', status: '0', remark: '用户性别列表', createTime: '2025-01-01 08:30:00' },
  { dictId: 8, dictName: '操作类型', dictType: 'sys_oper_type', status: '0', remark: '操作类型列表', createTime: '2025-01-01 09:00:00' },
  { dictId: 9, dictName: '通知类型', dictType: 'sys_notice_type', status: '1', remark: '通知类型列表', createTime: '2025-04-01 10:00:00' },
]

/** Mock字典数据 */
const mockDictDataMap: Record<string, DictDataRecord[]> = {
  barn_status: [
    { dictCode: 101, dictType: 'barn_status', dictLabel: '优良', dictValue: 'excellent', sort: 1, status: '0', remark: '' },
    { dictCode: 102, dictType: 'barn_status', dictLabel: '正常', dictValue: 'normal', sort: 2, status: '0', remark: '' },
    { dictCode: 103, dictType: 'barn_status', dictLabel: '急需修复', dictValue: 'urgent', sort: 3, status: '0', remark: '' },
    { dictCode: 104, dictType: 'barn_status', dictLabel: '有望修复', dictValue: 'hopeful', sort: 4, status: '0', remark: '' },
  ],
  health_level: [
    { dictCode: 201, dictType: 'health_level', dictLabel: '优良', dictValue: 'excellent', sort: 1, status: '0', remark: '' },
    { dictCode: 202, dictType: 'health_level', dictLabel: '需维护', dictValue: 'maintenance', sort: 2, status: '0', remark: '' },
    { dictCode: 203, dictType: 'health_level', dictLabel: '急需修复', dictValue: 'urgent', sort: 3, status: '0', remark: '' },
    { dictCode: 204, dictType: 'health_level', dictLabel: '退出', dictValue: 'retired', sort: 4, status: '0', remark: '' },
  ],
  repair_type: [
    { dictCode: 301, dictType: 'repair_type', dictLabel: '常规维护', dictValue: '1', sort: 1, status: '0', remark: '' },
    { dictCode: 302, dictType: 'repair_type', dictLabel: '专项修复', dictValue: '2', sort: 2, status: '0', remark: '' },
    { dictCode: 303, dictType: 'repair_type', dictLabel: '再利用', dictValue: '3', sort: 3, status: '0', remark: '' },
    { dictCode: 304, dictType: 'repair_type', dictLabel: '退出', dictValue: '4', sort: 4, status: '0', remark: '' },
  ],
  repair_status: [
    { dictCode: 401, dictType: 'repair_status', dictLabel: '待审核', dictValue: '1', sort: 1, status: '0', remark: '' },
    { dictCode: 402, dictType: 'repair_status', dictLabel: '已审核', dictValue: '2', sort: 2, status: '0', remark: '' },
    { dictCode: 403, dictType: 'repair_status', dictLabel: '实施中', dictValue: '3', sort: 3, status: '0', remark: '' },
    { dictCode: 404, dictType: 'repair_status', dictLabel: '待验收', dictValue: '4', sort: 4, status: '0', remark: '' },
    { dictCode: 405, dictType: 'repair_status', dictLabel: '已验收', dictValue: '5', sort: 5, status: '0', remark: '' },
    { dictCode: 406, dictType: 'repair_status', dictLabel: '已归档', dictValue: '6', sort: 6, status: '0', remark: '' },
  ],
  reservation_status: [
    { dictCode: 501, dictType: 'reservation_status', dictLabel: '待审核', dictValue: '1', sort: 1, status: '0', remark: '' },
    { dictCode: 502, dictType: 'reservation_status', dictLabel: '已确认', dictValue: '2', sort: 2, status: '0', remark: '' },
    { dictCode: 503, dictType: 'reservation_status', dictLabel: '烘烤中', dictValue: '3', sort: 3, status: '0', remark: '' },
    { dictCode: 504, dictType: 'reservation_status', dictLabel: '已完成', dictValue: '4', sort: 4, status: '0', remark: '' },
    { dictCode: 505, dictType: 'reservation_status', dictLabel: '超时', dictValue: '5', sort: 5, status: '0', remark: '' },
  ],
  sys_normal_disable: [
    { dictCode: 601, dictType: 'sys_normal_disable', dictLabel: '正常', dictValue: '0', sort: 1, status: '0', remark: '' },
    { dictCode: 602, dictType: 'sys_normal_disable', dictLabel: '停用', dictValue: '1', sort: 2, status: '0', remark: '' },
  ],
  sys_user_sex: [
    { dictCode: 701, dictType: 'sys_user_sex', dictLabel: '男', dictValue: '0', sort: 1, status: '0', remark: '' },
    { dictCode: 702, dictType: 'sys_user_sex', dictLabel: '女', dictValue: '1', sort: 2, status: '0', remark: '' },
    { dictCode: 703, dictType: 'sys_user_sex', dictLabel: '未知', dictValue: '2', sort: 3, status: '0', remark: '' },
  ],
  sys_oper_type: [
    { dictCode: 801, dictType: 'sys_oper_type', dictLabel: '查询', dictValue: '1', sort: 1, status: '0', remark: '' },
    { dictCode: 802, dictType: 'sys_oper_type', dictLabel: '新增', dictValue: '2', sort: 2, status: '0', remark: '' },
    { dictCode: 803, dictType: 'sys_oper_type', dictLabel: '修改', dictValue: '3', sort: 3, status: '0', remark: '' },
    { dictCode: 804, dictType: 'sys_oper_type', dictLabel: '删除', dictValue: '4', sort: 4, status: '0', remark: '' },
    { dictCode: 805, dictType: 'sys_oper_type', dictLabel: '导出', dictValue: '5', sort: 5, status: '0', remark: '' },
  ],
  sys_notice_type: [
    { dictCode: 901, dictType: 'sys_notice_type', dictLabel: '通知', dictValue: '1', sort: 1, status: '0', remark: '' },
    { dictCode: 902, dictType: 'sys_notice_type', dictLabel: '公告', dictValue: '2', sort: 2, status: '0', remark: '' },
  ],
}

/** 当前Tab：dict / data */
const activeTab = ref('dict')

/** ===== 字典类型Tab ===== */
const filterForm = reactive({
  dictName: '',
  dictType: '',
})
const dictTypeData = ref<DictTypeRecord[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({ pageNum: 1, pageSize: 10 })

/** 字典类型对话框 */
const dictTypeDialogVisible = ref(false)
const dictTypeDialogTitle = ref('新增字典类型')
const dictTypeForm = reactive({
  dictId: 0,
  dictName: '',
  dictType: '',
  status: '0' as '0' | '1',
  remark: '',
})
const dictTypeFormRef = ref()
const isEdit = ref(false)

const dictTypeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
}

/** ===== 字典数据Tab ===== */
const currentDictType = ref('')
const currentDictName = ref('')
const dictDataList = ref<DictDataRecord[]>([])
const dictDataLoading = ref(false)
const dictDataTotal = ref(0)
const dictDataPagination = reactive({ pageNum: 1, pageSize: 10 })

/** 字典数据对话框 */
const dictDataDialogVisible = ref(false)
const dictDataDialogTitle = ref('新增字典数据')
const dictDataForm = reactive({
  dictCode: 0,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  status: '0' as '0' | '1',
  remark: '',
})
const dictDataFormRef = ref()
const isDataEdit = ref(false)

const dictDataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
}

/** 加载字典类型列表 */
function loadDictTypes() {
  loading.value = true
  setTimeout(() => {
    let filtered = [...mockDictTypes]
    if (filterForm.dictName) {
      filtered = filtered.filter(d => d.dictName.includes(filterForm.dictName))
    }
    if (filterForm.dictType) {
      filtered = filtered.filter(d => d.dictType.includes(filterForm.dictType))
    }
    total.value = filtered.length
    const start = (pagination.pageNum - 1) * pagination.pageSize
    dictTypeData.value = filtered.slice(start, start + pagination.pageSize)
    loading.value = false
  }, 300)
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadDictTypes()
}

/** 重置 */
function handleReset() {
  Object.assign(filterForm, { dictName: '', dictType: '' })
  pagination.pageNum = 1
  loadDictTypes()
}

/** 分页 */
function handlePageChange(page: number) {
  pagination.pageNum = page
  loadDictTypes()
}
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadDictTypes()
}

/** 新增字典类型 */
function handleAddType() {
  isEdit.value = false
  dictTypeDialogTitle.value = '新增字典类型'
  Object.assign(dictTypeForm, { dictId: 0, dictName: '', dictType: '', status: '0', remark: '' })
  dictTypeDialogVisible.value = true
}

/** 编辑字典类型 */
function handleEditType(row: DictTypeRecord) {
  isEdit.value = true
  dictTypeDialogTitle.value = '编辑字典类型'
  Object.assign(dictTypeForm, {
    dictId: row.dictId,
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status,
    remark: row.remark,
  })
  dictTypeDialogVisible.value = true
}

/** 提交字典类型 */
async function handleSubmitType() {
  const valid = await dictTypeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    const idx = mockDictTypes.findIndex(d => d.dictId === dictTypeForm.dictId)
    if (idx !== -1) {
      mockDictTypes[idx] = { ...mockDictTypes[idx], ...dictTypeForm }
    }
    ElMessage.success('修改成功')
  } else {
    mockDictTypes.push({
      dictId: Date.now(),
      dictName: dictTypeForm.dictName,
      dictType: dictTypeForm.dictType,
      status: dictTypeForm.status,
      remark: dictTypeForm.remark,
      createTime: new Date().toLocaleString(),
    })
    ElMessage.success('新增成功')
  }
  dictTypeDialogVisible.value = false
  loadDictTypes()
}

/** 删除字典类型 */
async function handleDeleteType(row: DictTypeRecord) {
  await ElMessageBox.confirm(`确定删除字典类型「${row.dictName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  const idx = mockDictTypes.findIndex(d => d.dictId === row.dictId)
  if (idx !== -1) mockDictTypes.splice(idx, 1)
  ElMessage.success('删除成功')
  loadDictTypes()
}

/** 查看字典数据 */
function handleDictData(row: DictTypeRecord) {
  currentDictType.value = row.dictType
  currentDictName.value = row.dictName
  activeTab.value = 'data'
  loadDictData()
}

/** ===== 字典数据Tab方法 ===== */

/** 加载字典数据 */
function loadDictData() {
  if (!currentDictType.value) return
  dictDataLoading.value = true
  setTimeout(() => {
    const data = mockDictDataMap[currentDictType.value] || []
    dictDataTotal.value = data.length
    const start = (dictDataPagination.pageNum - 1) * dictDataPagination.pageSize
    dictDataList.value = data.slice(start, start + dictDataPagination.pageSize)
    dictDataLoading.value = false
  }, 300)
}

/** 字典数据分页 */
function handleDataPageChange(page: number) {
  dictDataPagination.pageNum = page
  loadDictData()
}
function handleDataSizeChange(size: number) {
  dictDataPagination.pageSize = size
  dictDataPagination.pageNum = 1
  loadDictData()
}

/** 新增字典数据 */
function handleAddData() {
  isDataEdit.value = false
  dictDataDialogTitle.value = '新增字典数据'
  Object.assign(dictDataForm, { dictCode: 0, dictType: currentDictType.value, dictLabel: '', dictValue: '', sort: 0, status: '0', remark: '' })
  dictDataDialogVisible.value = true
}

/** 编辑字典数据 */
function handleEditData(row: DictDataRecord) {
  isDataEdit.value = true
  dictDataDialogTitle.value = '编辑字典数据'
  Object.assign(dictDataForm, {
    dictCode: row.dictCode,
    dictType: row.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    sort: row.sort,
    status: row.status,
    remark: row.remark,
  })
  dictDataDialogVisible.value = true
}

/** 提交字典数据 */
async function handleSubmitData() {
  const valid = await dictDataFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (isDataEdit.value) {
    const list = mockDictDataMap[dictDataForm.dictType]
    if (list) {
      const idx = list.findIndex(d => d.dictCode === dictDataForm.dictCode)
      if (idx !== -1) list[idx] = { ...list[idx], ...dictDataForm }
    }
    ElMessage.success('修改成功')
  } else {
    if (!mockDictDataMap[dictDataForm.dictType]) {
      mockDictDataMap[dictDataForm.dictType] = []
    }
    mockDictDataMap[dictDataForm.dictType].push({
      dictCode: Date.now(),
      dictType: dictDataForm.dictType,
      dictLabel: dictDataForm.dictLabel,
      dictValue: dictDataForm.dictValue,
      sort: dictDataForm.sort,
      status: dictDataForm.status,
      remark: dictDataForm.remark,
    })
    ElMessage.success('新增成功')
  }
  dictDataDialogVisible.value = false
  loadDictData()
}

/** 删除字典数据 */
async function handleDeleteData(row: DictDataRecord) {
  await ElMessageBox.confirm(`确定删除字典数据「${row.dictLabel}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  const list = mockDictDataMap[row.dictType]
  if (list) {
    const idx = list.findIndex(d => d.dictCode === row.dictCode)
    if (idx !== -1) list.splice(idx, 1)
  }
  ElMessage.success('删除成功')
  loadDictData()
}

onMounted(() => {
  loadDictTypes()
})
</script>

<template>
  <div class="dict-manage">
    <el-tabs v-model="activeTab">
      <!-- 字典类型Tab -->
      <el-tab-pane label="字典类型" name="dict">
        <!-- 筛选区 -->
        <div class="filter-card">
          <el-form :model="filterForm" inline>
            <el-form-item label="字典名称">
              <el-input v-model="filterForm.dictName" placeholder="请输入字典名称" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="字典类型">
              <el-input v-model="filterForm.dictType" placeholder="请输入字典类型" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
              <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-button type="primary" :icon="Plus" @click="handleAddType">新增</el-button>
        </div>

        <!-- 表格 -->
        <div class="table-card">
          <el-table :data="dictTypeData" v-loading="loading" stripe border style="width: 100%">
            <el-table-column prop="dictId" label="字典编号" width="100" align="center" />
            <el-table-column prop="dictName" label="字典名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="dictType" label="字典类型" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" @click="handleDictData(row)">{{ row.dictType }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <button class="table-action-btn table-action-btn--primary" @click="handleEditType(row)">
                  <el-icon><Edit /></el-icon>编辑
                </button>
                <span class="action-divider"></span>
                <button class="table-action-btn table-action-btn--danger" @click="handleDeleteType(row)">
                  <el-icon><Delete /></el-icon>删除
                </button>
                <button class="table-action-btn table-action-btn--success" @click="handleDictData(row)">
                  <el-icon><InfoFilled /></el-icon>字典数据
                </button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="pagination.pageNum"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 字典数据Tab -->
      <el-tab-pane label="字典数据" name="data" :disabled="!currentDictType">
        <template #label>
          <span>字典数据{{ currentDictName ? ` - ${currentDictName}` : '' }}</span>
        </template>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-button type="primary" :icon="Plus" @click="handleAddData">新增</el-button>
          <el-button @click="activeTab = 'dict'">返回字典类型</el-button>
        </div>

        <!-- 表格 -->
        <div class="table-card">
          <el-table :data="dictDataList" v-loading="dictDataLoading" stripe border style="width: 100%">
            <el-table-column prop="dictCode" label="字典编码" width="100" align="center" />
            <el-table-column prop="dictLabel" label="字典标签" min-width="150" />
            <el-table-column prop="dictValue" label="字典键值" min-width="120" />
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <button class="table-action-btn table-action-btn--primary" @click="handleEditData(row)">
                  <el-icon><Edit /></el-icon>编辑
                </button>
                <span class="action-divider"></span>
                <button class="table-action-btn table-action-btn--danger" @click="handleDeleteData(row)">
                  <el-icon><Delete /></el-icon>删除
                </button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="dictDataPagination.pageNum"
              v-model:page-size="dictDataPagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="dictDataTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="handleDataSizeChange"
              @current-change="handleDataPageChange"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 字典类型对话框 -->
    <el-dialog v-model="dictTypeDialogVisible" :title="dictTypeDialogTitle" width="500px" destroy-on-close append-to-body align-center>
      <el-form ref="dictTypeFormRef" :model="dictTypeForm" :rules="dictTypeRules" label-width="80px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="dictTypeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="dictTypeForm.dictType" placeholder="请输入字典类型" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dictTypeForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dictTypeForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictTypeDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmitType">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据对话框 -->
    <el-dialog v-model="dictDataDialogVisible" :title="dictDataDialogTitle" width="500px" destroy-on-close append-to-body align-center>
      <el-form ref="dictDataFormRef" :model="dictDataForm" :rules="dictDataRules" label-width="80px">
        <el-form-item label="字典类型">
          <el-input v-model="dictDataForm.dictType" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dictDataForm.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dictDataForm.dictValue" placeholder="请输入字典键值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dictDataForm.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dictDataForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dictDataForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDataDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmitData">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.dict-manage {
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
}

/* ===== Tab样式 ===== */
.dict-manage :deep(.el-tabs) {
  background: transparent;
}

.dict-manage :deep(.el-tabs__header) {
  margin-bottom: clamp(12px, 1.5vw, 20px);
}

.dict-manage :deep(.el-tabs__item) {
  font-weight: 500;
}

.dict-manage :deep(.el-tabs__item.is-active) {
  color: #0A4D3E;
}

/* ===== 毛玻璃卡片 ===== */
.filter-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  padding-bottom: clamp(8px, 1vw, 12px);
  margin-bottom: clamp(12px, 1.5vw, 20px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.filter-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(10, 77, 62, 0.12), inset 0 1px 1px rgba(255, 255, 255, 0.8);
}

/* ===== 操作栏 ===== */
.action-bar {
  margin-bottom: clamp(12px, 1.5vw, 20px);
  display: flex;
  gap: clamp(6px, 0.8vw, 10px);
}

/* 轻拟态主按钮 */
.action-bar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.25), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #126B52 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.35), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
  transform: translateY(-2px);
}

.action-bar :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(10, 77, 62, 0.2), inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 轻拟态默认按钮 */
.action-bar :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff 0%, #f7f8f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button:not(.el-button--primary):hover) {
  background: linear-gradient(145deg, #f7f8f9 0%, #ffffff 100%);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transform: translateY(-2px);
}

.action-bar :deep(.el-button:not(.el-button--primary):active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08), inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* ===== 毛玻璃表格卡片 ===== */
.table-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  padding: clamp(16px, 2vw, 24px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(255, 255, 255, 0.8);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.table-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(10, 77, 62, 0.12), inset 0 1px 1px rgba(255, 255, 255, 0.8);
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: clamp(12px, 1.5vw, 20px);
}

/* ===== 筛选表单按钮 ===== */
.filter-card :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E 0%, #0D5D46 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(10, 77, 62, 0.25), 0 1px 2px rgba(255, 255, 255, 0.5) inset;
}

.filter-card :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46 0%, #126B52 100%);
  box-shadow: 0 4px 12px rgba(10, 77, 62, 0.35);
  transform: translateY(-2px);
}

.filter-card :deep(.el-button:not(.el-button--primary)) {
  background: linear-gradient(145deg, #ffffff 0%, #f7f8f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.filter-card :deep(.el-button:not(.el-button--primary):hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

/* ===== 对话框样式 ===== */
:deep(.el-dialog) {
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

/* ===== 响应式调整 ===== */
@media (max-width: 768px) {
  .dict-manage {
    padding: 12px;
  }

  .filter-card,
  .table-card {
    padding: 12px;
    border-radius: 8px;
  }
}
</style>
