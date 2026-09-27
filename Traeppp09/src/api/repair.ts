/**
 * 维修记录API - 对接RuoYi后端
 */
import { get, post, put, del } from './request'

export interface RepairListParams {
  pageNum?: number
  pageSize?: number
  ovenId?: string
  status?: string
  repairType?: string
  urgency?: string
  repairYear?: string
  replacementType?: string
  componentName?: string
}

/** 获取维修详情（别名） */
export function getRepairDetail(id: number): Promise<any> {
  return get(`/barn/repair/${id}`) as any
}

/** 审批维修 */
export function approveRepair(id: number, data: { repairStatus: string; auditOpinion?: string }): Promise<any> {
  return put('/barn/repair', { id, ...data })
}

/** 验收维修 */
export function acceptRepair(id: number, data: { repairStatus: string; acceptOpinion?: string; actualCost?: number }): Promise<any> {
  return put('/barn/repair', { id, ...data })
}

export function getRepairList(params: RepairListParams = {}): Promise<any> {
  return get('/barn/repair/list', params) as any
}

export function getRepairInfo(id: number): Promise<any> {
  return get(`/barn/repair/${id}`) as any
}

export function addRepair(data: any): Promise<any> {
  return post('/barn/repair', data)
}

/** 烟农/合作社维修提报 - 自动写申请人、写时间 */
export function applyRepair(data: {
  ovenId: string
  repairType: string
  urgency: string
  applyDesc?: string
  estimatedCost?: number
  applicant?: string
  remark?: string
}): Promise<any> {
  const payload: any = {
    ...data,
    applyTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
    repairStatus: '待审核',
  }
  return post('/barn/repair', payload)
}

export function updateRepair(data: any): Promise<any> {
  return put('/barn/repair', data)
}

export function deleteRepair(id: number): Promise<any> {
  return del(`/barn/repair/${id}`)
}

export function getRepairStats(): Promise<any> {
  return get('/barn/repair/stats') as any
}

/** 维修分析数据 */
export function getRepairAnalysis(): Promise<any> {
  return get('/barn/repair/analysis') as any
}
