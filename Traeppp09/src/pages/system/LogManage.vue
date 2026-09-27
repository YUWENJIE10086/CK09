<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, View } from '@element-plus/icons-vue'

/** 操作日志数据类型 */
interface LogRecord {
  operId: number
  title: string
  operType: string
  operName: string
  operUrl: string
  operMethod: string
  operParam: string
  operIp: string
  operLocation: string
  operStatus: '0' | '1' // 0成功 1失败
  operTime: string
  costTime: number
}

/** Mock操作日志数据 */
const mockLogs: LogRecord[] = [
  { operId: 1, title: '用户管理', operType: '查询', operName: 'admin', operUrl: '/system/user/list', operMethod: 'GET', operParam: '{}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '0', operTime: '2025-06-10 10:30:00', costTime: 45 },
  { operId: 2, title: '用户管理', operType: '新增', operName: 'admin', operUrl: '/system/user', operMethod: 'POST', operParam: '{"username":"test01","nickName":"测试用户"}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '0', operTime: '2025-06-10 10:32:15', costTime: 120 },
  { operId: 3, title: '角色管理', operType: '修改', operName: 'admin', operUrl: '/system/role', operMethod: 'PUT', operParam: '{"roleId":2,"roleName":"烤房管理员"}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '0', operTime: '2025-06-10 11:00:00', costTime: 88 },
  { operId: 4, title: '烤房管理', operType: '查询', operName: 'zhangsan', operUrl: '/barn/list', operMethod: 'GET', operParam: '{}', operIp: '192.168.1.101', operLocation: '云南省宣威市', operStatus: '0', operTime: '2025-06-10 11:15:30', costTime: 200 },
  { operId: 5, title: '维修管理', operType: '新增', operName: 'lisi', operUrl: '/repair/submit', operMethod: 'POST', operParam: '{"barnId":"KF001","repairType":"常规维护"}', operIp: '192.168.1.102', operLocation: '云南省沾益区', operStatus: '0', operTime: '2025-06-10 13:20:00', costTime: 156 },
  { operId: 6, title: '预约管理', operType: '查询', operName: 'zhaoliu', operUrl: '/reservation/audit', operMethod: 'GET', operParam: '{}', operIp: '192.168.1.103', operLocation: '云南省师宗县', operStatus: '1', operTime: '2025-06-10 14:00:00', costTime: 3500 },
  { operId: 7, title: '字典管理', operType: '删除', operName: 'admin', operUrl: '/system/dict/9', operMethod: 'DELETE', operParam: '{}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '0', operTime: '2025-06-10 14:30:00', costTime: 65 },
  { operId: 8, title: '用户管理', operType: '修改', operName: 'admin', operUrl: '/system/user/resetPwd', operMethod: 'PUT', operParam: '{"userId":4}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '0', operTime: '2025-06-10 15:00:00', costTime: 90 },
  { operId: 9, title: '菜单管理', operType: '查询', operName: 'admin', operUrl: '/system/menu/list', operMethod: 'GET', operParam: '{}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '0', operTime: '2025-06-10 15:20:00', costTime: 55 },
  { operId: 10, title: '烤房管理', operType: '导出', operName: 'zhangsan', operUrl: '/barn/export', operMethod: 'POST', operParam: '{}', operIp: '192.168.1.101', operLocation: '云南省宣威市', operStatus: '0', operTime: '2025-06-10 16:00:00', costTime: 2800 },
  { operId: 11, title: '部门管理', operType: '新增', operName: 'admin', operUrl: '/system/dept', operMethod: 'POST', operParam: '{"deptName":"测试部门"}', operIp: '192.168.1.100', operLocation: '云南省曲靖市', operStatus: '1', operTime: '2025-06-10 16:30:00', costTime: 5000 },
  { operId: 12, title: '维修管理', operType: '修改', operName: 'lisi', operUrl: '/repair/approve/3', operMethod: 'PUT', operParam: '{"status":"已审核"}', operIp: '192.168.1.102', operLocation: '云南省沾益区', operStatus: '0', operTime: '2025-06-10 17:00:00', costTime: 110 },
]

/** 操作类型选项 */
const operTypeOptions = [
  { label: '查询', value: '查询' },
  { label: '新增', value: '新增' },
  { label: '修改', value: '修改' },
  { label: '删除', value: '删除' },
  { label: '导出', value: '导出' },
]

/** 操作状态选项 */
const operStatusOptions = [
  { label: '成功', value: '0' },
  { label: '失败', value: '1' },
]

/** 筛选表单 */
const filterForm = reactive({
  title: '',
  operName: '',
  operType: '',
  operStatus: '' as '' | '0' | '1',
  dateRange: [] as string[],
})

/** 表格数据 */
const tableData = ref<LogRecord[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
})

/** 详情对话框 */
const detailVisible = ref(false)
const detailData = ref<LogRecord | null>(null)

/** 加载数据 */
function loadData() {
  loading.value = true
  setTimeout(() => {
    let filtered = [...mockLogs]
    if (filterForm.title) {
      filtered = filtered.filter(l => l.title.includes(filterForm.title))
    }
    if (filterForm.operName) {
      filtered = filtered.filter(l => l.operName.includes(filterForm.operName))
    }
    if (filterForm.operType) {
      filtered = filtered.filter(l => l.operType === filterForm.operType)
    }
    if (filterForm.operStatus !== '') {
      filtered = filtered.filter(l => l.operStatus === filterForm.operStatus)
    }
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      const start = filterForm.dateRange[0]
      const end = filterForm.dateRange[1]
      filtered = filtered.filter(l => l.operTime >= start && l.operTime <= end + ' 23:59:59')
    }
    total.value = filtered.length
    const start = (pagination.pageNum - 1) * pagination.pageSize
    tableData.value = filtered.slice(start, start + pagination.pageSize)
    loading.value = false
  }, 300)
}

/** 搜索 */
function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

/** 重置 */
function handleReset() {
  Object.assign(filterForm, { title: '', operName: '', operType: '', operStatus: '' as const, dateRange: [] })
  pagination.pageNum = 1
  loadData()
}

/** 分页 */
function handlePageChange(page: number) {
  pagination.pageNum = page
  loadData()
}
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

/** 查看详情 */
function handleDetail(row: LogRecord) {
  detailData.value = row
  detailVisible.value = true
}

/** 导出 */
function handleExport() {
  ElMessage.success('导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="log-manage">
    <!-- 筛选区 -->
    <div class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="操作模块">
          <el-input v-model="filterForm.title" placeholder="请输入操作模块" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input v-model="filterForm.operName" placeholder="请输入操作人员" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filterForm.operType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="opt in operTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作状态">
          <el-select v-model="filterForm.operStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="opt in operStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button :icon="Download" @click="handleExport">导出</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="operId" label="日志编号" width="90" align="center" />
        <el-table-column prop="title" label="操作模块" width="120" align="center" />
        <el-table-column prop="operType" label="操作类型" width="90" align="center" />
        <el-table-column prop="operName" label="操作人员" width="100" align="center" />
        <el-table-column prop="operIp" label="操作IP" width="140" align="center" />
        <el-table-column prop="operLocation" label="操作地点" width="140" align="center" />
        <el-table-column prop="operStatus" label="操作状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.operStatus === '0' ? 'success' : 'danger'" size="small">{{ row.operStatus === '0' ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" width="170" align="center" />
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <button class="table-action-btn table-action-btn--primary" @click="handleDetail(row)">
              <el-icon><View /></el-icon>详情
            </button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="700px" destroy-on-close append-to-body align-center>
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="日志编号">{{ detailData.operId }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ detailData.title }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detailData.operType }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ detailData.operName }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ detailData.operIp }}</el-descriptions-item>
        <el-descriptions-item label="操作地点">{{ detailData.operLocation }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ detailData.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detailData.operMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag :type="detailData.operStatus === '0' ? 'success' : 'danger'" size="small">{{ detailData.operStatus === '0' ? '成功' : '失败' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailData.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detailData.operTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="log-param">{{ detailData.operParam }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.log-manage {
  padding: clamp(16px, 2vw, 24px);
  min-height: 100%;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
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
}

/* 轻拟态按钮 */
.action-bar :deep(.el-button) {
  background: linear-gradient(145deg, #ffffff 0%, #f7f8f9 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transition: all 0.2s ease;
}

.action-bar :deep(.el-button:hover) {
  background: linear-gradient(145deg, #f7f8f9 0%, #ffffff 100%);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.8) inset;
  transform: translateY(-2px);
}

.action-bar :deep(.el-button:active) {
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

/* ===== 日志参数样式 ===== */
.log-param {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: clamp(11px, 1.2vw, 13px);
  color: #374151;
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.9) 0%, rgba(243, 244, 246, 0.85) 100%);
  backdrop-filter: blur(8px);
  padding: clamp(8px, 1vw, 12px);
  border-radius: 8px;
  max-height: clamp(150px, 20vh, 200px);
  overflow-y: auto;
  border: 1px solid rgba(0, 0, 0, 0.05);
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
  .log-manage {
    padding: 12px;
  }

  .filter-card,
  .table-card {
    padding: 12px;
    border-radius: 8px;
  }
}
</style>
