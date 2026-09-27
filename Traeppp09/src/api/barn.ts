/**
 * 烤房API - 对接RuoYi后端
 */
import { get, post, put, del } from './request'

/** 烤房列表查询参数 */
export interface BarnListParams {
  pageNum?: number
  pageSize?: number
  countyCode?: string
  townCode?: string
  useStatus?: string
  healthLevel?: string
  id?: string
  barnName?: string
}

/** 分页结果 */
export interface PageResult<T> {
  rows: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 获取烤房列表（分页） */
export function getBarnList(params: BarnListParams = {}): Promise<PageResult<any>> {
  return get('/barn/project/list', params) as any
}

/** 获取全部烤房（不分页，给下拉选择/日历用） */
export function getBarnOptions(): Promise<any[]> {
  return get('/barn/project/list-options') as any
}

/** 获取烤房详情 */
export function getBarnInfo(id: string): Promise<any> {
  return get(`/barn/project/${id}`) as any
}

/** 获取烤房部件列表 */
export function getBarnComponentList(ovenId: string): Promise<any[]> {
  return get('/barn/component/list', { ovenId }) as any
}

/** 新增烤房 */
export function addBarn(data: any): Promise<any> {
  return post('/barn/project', data)
}

/** 修改烤房 */
export function updateBarn(data: any): Promise<any> {
  return put('/barn/project', data)
}

/** 删除烤房 */
export function deleteBarn(id: string): Promise<any> {
  return del(`/barn/project/${id}`)
}

/** 健康等级统计 - 使用合并的stats接口 */
export function getHealthStats(): Promise<any> {
  return get('/barn/project/stats').then((res: any) => ({
    total: res.total,
    excellent: res.healthExcellent,
    maintenance: res.healthMaintenance,
    urgent: res.healthUrgent,
    retired: res.healthRetired
  })) as any
}

/** 县区分布 - 使用合并的stats接口 */
export function getCountyStats(): Promise<any[]> {
  return get('/barn/project/stats').then((res: any) => res.countyStats || []) as any
}

/** 项目类型分布 - 使用合并的stats接口 */
export function getProjectTypeStats(): Promise<any[]> {
  return get('/barn/project/stats').then((res: any) => res.projectTypeStats || []) as any
}

/** 建设方式分布 - 使用合并的stats接口 */
export function getBuildModeStats(): Promise<any[]> {
  return get('/barn/project/stats').then((res: any) => res.buildMethodStats || []) as any
}

/** 烤房分配专用列表 - 从kf_basedata表获取，确保和小程序数据源一致 */
export function getBarnAssignList(params: {
  pageNum?: number
  pageSize?: number
  county?: string
  township?: string
  keyword?: string
  status?: string
  healthLevel?: string
  facilityStatus?: string
} = {}): Promise<PageResult<any>> {
  return get('/barn/project/assign-list', params) as any
}

// ========== 烤房部件管理 API ==========

/** 部件列表查询参数 */
export interface ComponentQuery {
  category?: string  // JR/SR/ZK/ZT/FS
  projectId?: string
  county?: string
  township?: string
  status?: string  // 正常/缺失/损坏
  facilityStatus?: string  // 设施现状(正常/闲置/损坏/另作他用)
  pageNum?: number
  pageSize?: number
}

/** 获取部件列表 */
export function getComponentList(params: ComponentQuery): Promise<PageResult<any>> {
  return get('/barn/components/list', params) as any
}

/** 获取烤房完整档案（5大部位） */
export function getBarnFullProfile(projectId: string): Promise<any> {
  return get(`/barn/components/${projectId}`) as any
}

/** 按部位查看烤房详情 */
export function getBarnComponentByCategory(projectId: string, category: string): Promise<any> {
  return get(`/barn/components/${projectId}/${category}`) as any
}

/** 获取烤房统计数据 */
export function getBarnProjectStats() {
  return get('/barn/project/stats') as any
}

/** 更新烤房信息 */
export function updateBarnProject(data: any) {
  return put('/barn/project/edit', data) as any
}
