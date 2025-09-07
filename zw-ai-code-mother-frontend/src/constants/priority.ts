/**
 * 优先级枚举
 */
export enum PriorityEnum {
  NORMAL = 0,
  FEATURED = 99,
}

/**
 * 优先级标签配置
 */
export const PRIORITY_CONFIG = {
  [PriorityEnum.NORMAL]: {
    label: '一般',
    color: 'warning',
  },
  [PriorityEnum.FEATURED]: {
    label: '精选',
    color: 'success',
  },
} as const
