import { get, post, put, del } from './request'

export interface AssignmentListParams {
  pageNum?: number
  pageSize?: number
  barnId?: number
  farmerName?: string
  assignStatus?: string
}

export function getAssignmentList(params: AssignmentListParams = {}): Promise<any> {
  return get('/barn/assignment/list', params) as any
}

export function getAssignmentInfo(id: number): Promise<any> {
  return get(`/barn/assignment/${id}`) as any
}

export function addAssignment(data: any): Promise<any> {
  return post('/barn/assignment', data)
}

export function updateAssignment(data: any): Promise<any> {
  return put('/barn/assignment', data)
}

export function deleteAssignment(id: number): Promise<any> {
  return del(`/barn/assignment/${id}`)
}

export function getAssignmentsByBarn(barnId: number): Promise<any> {
  return get(`/barn/assignment/by-barn/${barnId}`) as any
}

export function searchFarmer(keyword: string): Promise<any> {
  return get('/barn/assignment/search-farmer', { keyword }) as any
}

export function getAssignmentStats(): Promise<any> {
  return get('/barn/assignment/stats') as any
}

/** 自动释放到期分配 */
export function autoReleaseAssignment(): Promise<any> {
  return post('/barn/assignment/auto-release')
}
