<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ChatDotSquare, Search, Refresh } from '@element-plus/icons-vue'
import { getEvaluationList } from '@/api/evaluation'
import { listCounty } from '@/api/dict'
import BarnCode from '@/components/BarnCode.vue'

/** 筛选表单 */
const filterForm = reactive({
  evaluatorPhone: '' as string,
  ovenId: '' as string,
  county: '' as string,
  minRating: undefined as number | undefined,
  maxRating: undefined as number | undefined,
  dateRange: [] as string[],
})

/** 分页参数 */
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

/** 表格数据 */
const tableData = ref<any[]>([])
const loading = ref(false)

/** 区县下拉 */
const countyOptions = ref<{ value: string; label: string }[]>([])

/** 展开行 */
const expandedRows = ref<string[]>([])

/** 将评分归一化为 0-5 星（兼容 1-5 直接星级与 0-100 百分制） */
function toStars(val: any): number {
  const n = Number(val ?? 0)
  if (isNaN(n)) return 0
  if (n > 5) return Math.round(n / 20)
  return n
}

/** 解析图片字段（兼容 JSON 数组字符串、逗号分隔字符串、数组） */
function parseImages(val: any): string[] {
  if (!val) return []
  if (Array.isArray(val)) return val.filter(Boolean)
  const s = String(val).trim()
  if (!s) return []
  if (s.startsWith('[')) {
    try {
      const arr = JSON.parse(s)
      return Array.isArray(arr) ? arr.filter(Boolean) : []
    } catch { /* ignore */ }
  }
  return s.split(',').map(u => u.trim()).filter(Boolean)
}

/** 格式化时间（兼容字符串与 Jackson 的 LocalDateTime 数组序列化） */
function formatTime(val: any): string {
  if (!val) return ''
  if (typeof val === 'string') return val.replace('T', ' ').substring(0, 19)
  if (Array.isArray(val)) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = val
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${y}-${pad(mo)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }
  return String(val)
}

/** 获取列表数据 */
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }
    if (filterForm.evaluatorPhone) params.evaluatorPhone = filterForm.evaluatorPhone
    if (filterForm.ovenId) params.ovenId = filterForm.ovenId
    if (filterForm.county) params.county = filterForm.county
    if (filterForm.minRating != null) params.minRating = filterForm.minRating
    if (filterForm.maxRating != null) params.maxRating = filterForm.maxRating
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }

    const res: any = await getEvaluationList(params)
    tableData.value = res?.rows || res?.data || []
    pagination.total = res?.total ?? tableData.value.length
  } catch (e) {
    console.error('加载评价列表失败:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  fetchData()
}

/** 重置 */
function handleReset() {
  filterForm.evaluatorPhone = ''
  filterForm.ovenId = ''
  filterForm.county = ''
  filterForm.minRating = undefined
  filterForm.maxRating = undefined
  filterForm.dateRange = []
  pagination.pageNum = 1
  fetchData()
}

/** 分页变化 */
function handlePageChange(page: number) {
  pagination.pageNum = page
  fetchData()
}

/** 每页条数变化 */
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchData()
}

/** 展开行变化 */
function handleExpandChange(row: any, expandedRowsList: any[]) {
  expandedRows.value = expandedRowsList.map(r => r.id)
}

onMounted(() => {
  listCounty().then((res: any) => {
    countyOptions.value = (res || []).map((c: any) => ({ value: c.countyName, label: c.countyName }))
  }).catch(() => {})
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-wrap">
        <div class="page-title-icon">
          <el-icon :size="18"><ChatDotSquare /></el-icon>
        </div>
        <h2 class="page-title">评价管理</h2>
      </div>
      <p class="page-desc">烤房与烘烤师评价记录查询</p>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-container">
      <el-form :model="filterForm" inline>
        <el-form-item label="烟农手机号">
          <el-input v-model="filterForm.evaluatorPhone" placeholder="请输入手机号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="烤房编号">
          <el-input v-model="filterForm.ovenId" placeholder="请输入烤房编号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="filterForm.county" placeholder="全部区县" clearable style="width: 140px">
            <el-option v-for="c in countyOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分范围">
          <el-input-number v-model="filterForm.minRating" :min="1" :max="5" :controls="false" placeholder="最低分" style="width: 90px" />
          <span style="margin: 0 6px">-</span>
          <el-input-number v-model="filterForm.maxRating" :min="1" :max="5" :controls="false" placeholder="最高分" style="width: 90px" />
        </el-form-item>
        <el-form-item label="评价时间">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        style="width: 100%"
        @expand-change="handleExpandChange"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="烤师评价内容">
                  {{ row.bakerComment || '暂无评价' }}
                </el-descriptions-item>
                <el-descriptions-item label="烤房标签">
                  <span v-if="row.ovenTags">{{ row.ovenTags }}</span>
                  <span v-else>暂无标签</span>
                </el-descriptions-item>
                <el-descriptions-item label="评价图片">
                  <div v-if="parseImages(row.images).length" class="eval-images">
                    <el-image
                      v-for="(img, i) in parseImages(row.images)"
                      :key="i"
                      :src="img"
                      :preview-src-list="parseImages(row.images)"
                      :preview-teleported="true"
                      fit="cover"
                      class="eval-img"
                    />
                  </div>
                  <span v-else>暂无图片</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价时间" width="180" align="center">
          <template #default="{ row }"><span class="nowrap">{{ formatTime(row.evaluateTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="evaluatorName" label="烟农姓名" width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="evaluatorPhone" label="手机号" width="135" align="center" />
        <el-table-column label="烤房编号" width="130" align="center">
          <template #default="{ row }">
            <BarnCode :code="row.ovenId" />
          </template>
        </el-table-column>
        <el-table-column prop="county" label="区县" width="70" align="center" />
        <el-table-column prop="township" label="乡镇" width="70" align="center" />
        <el-table-column label="总体评分" width="110" align="center">
          <template #default="{ row }">
            <el-rate :model-value="toStars(row.overallRating)" disabled :max="5" show-score score-template="{value}" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="烤师评分" width="110" align="center">
          <template #default="{ row }">
            <el-rate :model-value="toStars(row.bakerRating)" disabled :max="5" show-score score-template="{value}" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="温控评分" width="110" align="center">
          <template #default="{ row }">
            <el-rate :model-value="toStars(row.tempControlRating)" disabled :max="5" show-score score-template="{value}" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="设备评分" width="110" align="center">
          <template #default="{ row }">
            <el-rate :model-value="toStars(row.equipmentRating)" disabled :max="5" show-score score-template="{value}" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="150" align="left">
          <template #default="{ row }">
            <el-popover v-if="row.comment" trigger="click" placement="top" :width="280" popper-class="comment-popper">
              <template #reference>
                <span class="comment-trigger">{{ row.comment }}</span>
              </template>
              <div class="comment-full">{{ row.comment }}</div>
            </el-popover>
            <span v-else class="comment-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.source === '小程序'" type="success" size="small" effect="light">{{ row.source }}</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">{{ row.source || 'Web端' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 毛玻璃质感设计系统 ==================== */
.page-container {
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
  letter-spacing: 0.02em;
}

.page-desc {
  font-size: clamp(12px, 1.4vw, 14px);
  color: var(--text-muted);
  margin: 0;
  padding-left: clamp(40px, 5vw, 52px);
}

/* 毛玻璃筛选区 */
.filter-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  padding: clamp(14px, 2vw, 20px) clamp(18px, 2.5vw, 28px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  margin-bottom: clamp(16px, 2vw, 24px);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;
}

.filter-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(10, 77, 62, 0.2), transparent);
}

.filter-container:hover {
  box-shadow:
    0 6px 24px rgba(10, 77, 62, 0.08),
    0 12px 40px rgba(10, 77, 62, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
}

.filter-container :deep(.el-form-item) {
  margin-right: clamp(16px, 2vw, 28px);
  margin-bottom: 12px;
}

/* 轻拟态按钮 */
.filter-container :deep(.el-button--primary) {
  background: linear-gradient(135deg, #0A4D3E, #0D5D46);
  border: none;
  box-shadow:
    0 2px 4px rgba(10, 77, 62, 0.2),
    0 4px 8px rgba(10, 77, 62, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
}

.filter-container :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #0D5D46, #1A6B4F);
  box-shadow:
    0 4px 8px rgba(10, 77, 62, 0.25),
    0 8px 16px rgba(10, 77, 62, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.filter-container :deep(.el-button--primary:active) {
  transform: translateY(0);
  box-shadow:
    0 1px 2px rgba(10, 77, 62, 0.2),
    inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 毛玻璃表格区 */
.table-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.72) 100%);
  backdrop-filter: blur(16px) saturate(1.3);
  -webkit-backdrop-filter: blur(16px) saturate(1.3);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 4px 16px rgba(10, 77, 62, 0.06),
    0 8px 32px rgba(10, 77, 62, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  padding: clamp(14px, 2vw, 20px);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.table-container:hover {
  box-shadow:
    0 8px 24px rgba(10, 77, 62, 0.1),
    0 16px 48px rgba(10, 77, 62, 0.08),
    0 0 0 1px rgba(10, 77, 62, 0.1);
  transform: translateY(-3px);
}

.expand-content {
  padding: clamp(10px, 1.5vw, 14px) clamp(16px, 2vw, 24px);
  background: rgba(10, 77, 62, 0.03);
}

.nowrap {
  white-space: nowrap;
}

/* ===== 评分星星缩小间距 ===== */
.table-container :deep(.el-rate--small) {
  display: inline-flex;
  align-items: center;
  height: auto;
}
.table-container :deep(.el-rate--small .el-rate__icon) {
  margin-right: 1px;
  font-size: 12px;
}
.table-container :deep(.el-rate--small .el-rate__text) {
  font-size: 12px;
  margin-left: 2px;
}

/* ===== 评价内容折叠 ===== */
.comment-trigger {
  display: inline-block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  color: #5A8A7A;
  font-size: 13px;
  vertical-align: middle;
}
.comment-trigger:hover {
  color: #0D5D46;
  text-decoration: underline;
}
.comment-empty {
  color: #C0C4CC;
  font-size: 14px;
}

/* ===== 烤房编号去方框 ===== */
.table-container :deep(.barn-code) {
  background: none;
  border: none;
  padding: 0;
  border-radius: 0;
}

.eval-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.eval-img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  border: 1px solid rgba(10, 77, 62, 0.15);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: clamp(12px, 1.5vw, 18px);
}
</style>
