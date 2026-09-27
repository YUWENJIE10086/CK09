/**
 * Axios请求封装
 * 创建axios实例，配置请求/响应拦截器，导出通用请求方法
 */
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** 创建axios实例 */
const service: AxiosInstance = axios.create({
  baseURL: '/dev-api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

/** 请求拦截器 */
service.interceptors.request.use(
  (config) => {
    // 从localStorage获取token，添加到请求头
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/** 响应拦截器 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 如果返回的状态码不是200，说明接口有问题
    if (res && typeof res === 'object' && 'code' in res && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      // 401：未登录或token过期
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    // 解开 R 包装：如果存在 data 字段且是 R<T> 结构，返回 data；否则原样返回
    if (res && typeof res === 'object' && 'data' in res && 'code' in res) {
      return res.data
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    switch (status) {
      case 401:
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        router.push('/login')
        break
      case 403:
        ElMessage.error('没有权限访问该资源')
        break
      case 404:
        ElMessage.error('请求的资源不存在')
        break
      case 500:
        ElMessage.error('服务器内部错误')
        break
      default:
        ElMessage.error(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

/** 通用请求方法 */

/** GET请求 */
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config })
}

/** POST请求 */
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

/** PUT请求 */
export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

/** DELETE请求 */
export function del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, { params, ...config })
}

/** 下载文件 */
export function download<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, responseType: 'blob', ...config })
}

export default service
