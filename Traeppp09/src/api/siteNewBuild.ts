/**
 * 烤房新建 API（测试用，新增功能）
 */
import { get, post, put, del } from './request'

/** 候选点列表（分页） */
export function getSiteNewBuildList(params: any = {}): Promise<any> {
  return get('/site-new-build/list', params) as any
}

/** 全部候选点（地图用） */
export function getSiteNewBuildAll(): Promise<any[]> {
  return get('/site-new-build/all') as any
}

/** 新增候选点 */
export function addSiteNewBuild(data: any): Promise<any> {
  return post('/site-new-build', data)
}

/** 更新候选点 */
export function updateSiteNewBuild(id: number, data: any): Promise<any> {
  return put(`/site-new-build/${id}`, data)
}

/** 删除候选点 */
export function deleteSiteNewBuild(id: number): Promise<any> {
  return del(`/site-new-build/${id}`)
}

/** 清空所有候选点 */
export function clearSiteNewBuild(): Promise<any> {
  return del('/site-new-build/clear')
}

/** 自动生成网格候选点 */
export function autoGenerateSiteNewBuild(params: any = {}): Promise<any> {
  return post('/site-new-build/auto-generate', params, { timeout: 180000 })
}

/** 运行新建算法（AHP+熵权法+TOPSIS，耗时较长） */
export function evaluateSiteNewBuild(params: any = {}): Promise<any> {
  return post('/site-new-build/evaluate', params, { timeout: 180000 })
}

/** 新建统计 */
export function getSiteNewBuildStats(): Promise<any> {
  return get('/site-new-build/stats') as any
}

/** 算法描述 */
export function getSiteNewBuildAlgoDesc(): Promise<any> {
  return get('/site-new-build/algorithm-desc') as any
}

/** 村烤房负载均衡推演（供需缺口指数法） */
export function getVillageLoad(capacityKg: number = 5000): Promise<any> {
  return get('/site-new-build/village-load', { capacityKg }) as any
}

/** 推荐新建村列表（按优先级排序） */
export function getVillageLoadRecommend(capacityKg: number = 5000): Promise<any[]> {
  return get('/site-new-build/village-load/recommend', { capacityKg }) as any
}
