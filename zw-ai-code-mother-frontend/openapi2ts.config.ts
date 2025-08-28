/*
 * 注意，要将schemaPath改为自己后端服务提供的 Swagger 接口文档的地址，生成前确保后端已启动！
 * */
export default {
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
}
