// eslint-disable-next-line
// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 查询模型配置列表（仅管理员） POST /admin/model/list */
export async function listModelConfig(
  body: API.ModelConfigQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListModelConfigVO>('/admin/model/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 查询能力与提供方选项（仅管理员） GET /admin/model/capabilities */
export async function listModelCapabilities(options?: { [key: string]: any }) {
  return request<API.BaseResponseListModelCapabilityOptionVO>('/admin/model/capabilities', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 新增模型配置（仅管理员） POST /admin/model/add */
export async function addModelConfig(
  body: API.ModelConfigAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong>('/admin/model/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改模型配置（仅管理员，apiKey 留空表示不修改） POST /admin/model/update */
export async function updateModelConfig(
  body: API.ModelConfigUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/admin/model/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 删除模型配置（仅管理员） POST /admin/model/delete */
export async function deleteModelConfig(
  body: API.ModelConfigIdRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/admin/model/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 设为该能力下的默认配置（仅管理员） POST /admin/model/setDefault */
export async function setDefaultModelConfig(
  params: { id: string },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/admin/model/setDefault', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 启用或停用配置（仅管理员） POST /admin/model/toggle */
export async function toggleModelConfigStatus(
  params: { id: string; status: number },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/admin/model/toggle', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 连通性测试（仅管理员） POST /admin/model/test */
export async function testModelConfig(
  body: API.ModelConfigTestRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseModelConfigTestVO>('/admin/model/test', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 强制刷新模型缓存（仅管理员） POST /admin/model/refresh */
export async function refreshModelConfigCache(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/admin/model/refresh', {
    method: 'POST',
    ...(options || {}),
  })
}
