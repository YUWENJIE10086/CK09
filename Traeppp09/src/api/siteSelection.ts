/**
 * 烤房选址 API（测试用，新增功能）
 */
import { get, post, put, del } from './request'

/** 候选点列表（分页） */
export function getSiteSelectionList(params: any = {}): Promise<any> {
  return get('/site-selection/list', params) as any
}

/** 全部候选点（地图用） */
export function getSiteSelectionAll(): Promise<any[]> {
  return get('/site-selection/all') as any
}

/** 新增候选点 */
export function addSiteSelection(data: any): Promise<any> {
  return post('/site-selection', data)
}

/** 更新候选点 */
export function updateSiteSelection(id: number, data: any): Promise<any> {
  return put(`/site-selection/${id}`, data)
}

/** 删除候选点 */
export function deleteSiteSelection(id: number): Promise<any> {
  return del(`/site-selection/${id}`)
}

/** 清空所有候选点 */
export function clearSiteSelection(): Promise<any> {
  return del('/site-selection/clear')
}

/** 自动生成网格候选点 */
export function autoGenerateSiteSelection(params: any = {}): Promise<any> {
  return post('/site-selection/auto-generate', params, { timeout: 180000 })
}

/** 运行选址评价算法（AHP+熵权法+TOPSIS，耗时较长） */
export function evaluateSiteSelection(params: any = {}): Promise<any> {
  return post('/site-selection/evaluate', params, { timeout: 180000 })
}

/** 选址统计 */
export function getSiteSelectionStats(): Promise<any> {
  return get('/site-selection/stats') as any
}

/** 算法描述 */
export function getSiteSelectionAlgoDesc(): Promise<any> {
  return get('/site-selection/algorithm-desc') as any
}
