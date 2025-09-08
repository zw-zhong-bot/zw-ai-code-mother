// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /static/${param0}/&#42;&#42; */
export async function serveStaticResource(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.serveStaticResourceParams,
  options?: { [key: string]: any }
) {
  const { deployKey: param0, ...queryParams } = params
  return request<string>(`/static/${param0}/**`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /static/fengmian/${param0} */
export async function serveCoverFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.serveCoverFileParams,
  options?: { [key: string]: any }
) {
  const { fileName: param0, ...queryParams } = params
  return request<string>(`/static/fengmian/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /static/ping/${param0} */
export async function serveAvatarFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.serveAvatarFileParams,
  options?: { [key: string]: any }
) {
  const { fileName: param0, ...queryParams } = params
  return request<string>(`/static/ping/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
