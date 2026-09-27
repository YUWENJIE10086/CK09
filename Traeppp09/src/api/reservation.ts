/**
 * 预约记录API - 对接RuoYi后端
 */
import { get, post, put, del } from './request'

export interface ReservationListParams {
  pageNum?: number
  pageSize?: number
  ovenId?: string
  status?: string
  userId?: string
}

export function getReservationList(params: ReservationListParams = {}): Promise<any> {
  return get('/barn/reservation/list', params) as any
}

export function getReservationInfo(id: number): Promise<any> {
  return get(`/barn/reservation/${id}`) as any
}

/** 预约日历 - 复用 list 接口 */
export function getReservationCalendar(params: ReservationListParams = {}): Promise<any> {
  return get('/barn/reservation/list', { pageNum: 1, pageSize: 500, ...params }) as any
}

export function addReservation(data: any): Promise<any> {
  return post('/barn/reservation', data)
}

export function updateReservation(data: any): Promise<any> {
  return put('/barn/reservation', data)
}

export function deleteReservation(id: number): Promise<any> {
  return del(`/barn/reservation/${id}`)
}

/** 审核预约 */
export function approveReservation(
  id: string,
  status: string,
  opinion?: string
): Promise<any> {
  return post(`/barn/reservation/approve`, {
    id,
    status,
    reviewOpinion: opinion,
  })
}
