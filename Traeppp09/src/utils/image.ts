/**
 * 图片工具函数
 */

/**
 * 将原始图片路径转换为缩略图 URL
 * 通过后端 /api/image/thumb 接口按需生成缩略图，大幅减少传输体积
 *
 * @param path 原始图片路径（数据库中存储的值）
 * @param size 缩略图尺寸（短边像素），默认 200
 * @returns 缩略图 URL；如果是外部链接则原样返回；空值返回空字符串
 */
export function thumbUrl(path: string | null | undefined, size: number = 200): string {
  if (!path) return ''
  // 外部完整 URL 不走缩略图接口
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `/dev-api/api/image/thumb?path=${encodeURIComponent(path)}&size=${size}`
}
