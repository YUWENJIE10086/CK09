/**
 * 烘烤师管理API
 */
import { get, post, put, del } from './request'

/** 烘烤师列表查询参数 */
export interface BakerListParams {
  pageNum?: number
  pageSize?: number
  name?: string
  phone?: string
  status?: number
}

/** 烘烤师统计信息 */
export interface BakerStats {
  total: number
  active: number
  senior: number
  mid: number
  junior: number
}

/** 获取烘烤师统计信息 */
export function getBakerStats(): Promise<BakerStats> {
  return get('/baker/stats') as any
}

/** 获取烘烤师分页列表 */
export function getBakerList(params: BakerListParams = {}): Promise<any> {
  return get('/baker/list', params) as any
}

/** 获取所有在岗烘烤师（下拉选项用） */
export function getBakerOptions(): Promise<any> {
  return get('/baker/options') as any
}

/** 获取所有磅组（下拉选项用） */
export function getPoundGroups(): Promise<any> {
  return get('/baker/poundGroups') as any
}

/** 新增烘烤师 */
export function addBaker(data: any): Promise<any> {
  return post('/baker', data) as any
}

/** 更新烘烤师 */
export function updateBaker(data: any): Promise<any> {
  return put('/baker', data) as any
}

/** 删除烘烤师 */
export function deleteBaker(id: string): Promise<any> {
  return del(`/baker/${id}`) as any
}

/** 批量删除烘烤师 */
export function batchDeleteBaker(ids: string[]): Promise<any> {
  return del('/baker/batch', { ids }) as any
}
