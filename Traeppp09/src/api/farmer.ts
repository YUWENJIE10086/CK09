import { get, post, put, del, download } from './request'

/** 烟农列表查询参数 */
export interface FarmerListParams {
  pageNum?: number
  pageSize?: number
  name?: string
  phone?: string
  creditLevel?: string
  status?: number
}

/** 烟农信息 */
export interface Farmer {
  id: string
  name: string
  phone: string
  creditLevel: string
  creditScore: number
  area: string
  poundGroupId: number
  poundGroupName?: string
  plantingArea: number
  plantingYears: number
  totalBakes: number
  avatarUrl?: string
  status: number
  createTime?: string
  updateTime?: string
}

/** 统计信息 */
export interface FarmerStats {
  total: number
  active: number
  creditA: number
  creditB: number
  creditC: number
  creditD: number
}

/**
 * 获取烟农列表
 */
export function getFarmerList(params: FarmerListParams = {}): Promise<any> {
  return get('/farmer/list', params)
}

/**
 * 获取烟农统计信息
 */
export function getFarmerStats(): Promise<any> {
  return get('/farmer/stats')
}

/**
 * 获取烟农详情
 */
export function getFarmerDetail(id: string): Promise<any> {
  return get(`/farmer/${id}`)
}

/**
 * 添加烟农
 */
export function addFarmer(data: Partial<Farmer>): Promise<any> {
  return post('/farmer', data)
}

/**
 * 更新烟农
 */
export function updateFarmer(data: Partial<Farmer>): Promise<any> {
  return put('/farmer', data)
}

/**
 * 删除烟农
 */
export function deleteFarmer(id: string): Promise<any> {
  return del(`/farmer/${id}`)
}

/**
 * 批量删除烟农
 */
export function batchDeleteFarmer(ids: string[]): Promise<any> {
  return del('/farmer/batch', undefined, { data: { ids } })
}

/**
 * 导入Excel
 */
export function importFarmer(file: File): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  return post('/farmer/import', formData)
}

/**
 * 导入烟农数据（JSON数组，前端解析Excel后调用）
 */
export function importFarmerJson(data: any[]): Promise<any> {
  return post('/farmer/import-json', data)
}

/**
 * 导出Excel
 */
export function exportFarmer(params: FarmerListParams = {}): Promise<any> {
  return get('/farmer/export', params, {
    responseType: 'blob'
  })
}

/**
 * 下载导入模板
 */
export function downloadImportTemplate(): Promise<any> {
  return get('/farmer/import-template', {}, {
    responseType: 'blob'
  })
}

/**
 * 同步到小程序
 */
export function syncToMiniProgram(id: string): Promise<any> {
  return post(`/farmer/${id}/sync`)
}

/**
 * 批量同步到小程序
 */
export function batchSyncToMiniProgram(ids: string[]): Promise<any> {
  return post('/farmer/batch-sync', { ids })
}
