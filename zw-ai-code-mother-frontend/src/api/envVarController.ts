// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /api/key */
export async function getApiKey(options?: { [key: string]: any }) {
  return request<string>('/api/key', {
    method: 'GET',
    ...(options || {}),
  })
}
