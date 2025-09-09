/**
 * 代码生成类型枚举
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'mulit_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型选项
 */
export const CODE_GEN_TYPE_OPTIONS = [
  {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
  },
  {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
  },
  {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
  },

] as const

/**
 * 代码生成类型映射
 */
export const CODE_GEN_TYPE_MAP = {
  [CodeGenTypeEnum.HTML]: '原生 HTML 模式',
  [CodeGenTypeEnum.MULTI_FILE]: '原生多文件模式',
} as const
