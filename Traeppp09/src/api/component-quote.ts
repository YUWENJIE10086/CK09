/**
 * 部件报价API - 对接RuoYi后端
 */
import { get, post, put, del } from './request'

export interface QuoteListParams {
  pageNum?: number
  pageSize?: number
  itemName?: string
}

/** 获取部件报价列表 */
export function getQuoteList(params: QuoteListParams = {}): Promise<any> {
  return get('/barn/component-quote/list', params) as any
}

/** 获取部件报价详情 */
export function getQuoteInfo(id: number): Promise<any> {
  return get(`/barn/component-quote/${id}`) as any
}

/** 新增部件报价 */
export function addQuote(data: any): Promise<any> {
  return post('/barn/component-quote', data)
}

/** 修改部件报价 */
export function updateQuote(data: any): Promise<any> {
  return put('/barn/component-quote', data)
}

/** 删除部件报价 */
export function deleteQuote(id: number): Promise<any> {
  return del(`/barn/component-quote/${id}`)
}
