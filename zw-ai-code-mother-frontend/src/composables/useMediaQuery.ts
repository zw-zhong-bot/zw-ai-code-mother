import { onMounted, onUnmounted, ref } from 'vue'

/** 移动端断点（与 theme.css / 各页面 @media 保持一致） */
export const MOBILE_MAX_WIDTH = 768
/** 平板断点：对话页在此宽度以下改为上下堆叠 */
export const TABLET_MAX_WIDTH = 1024

/**
 * 响应式断点判断
 *
 * 用 matchMedia 而不是监听 resize：避免滚动（移动端地址栏伸缩会触发 resize）
 * 引发的无意义重算，且能直接复用 CSS 的同一套断点定义。
 *
 * @param query 媒体查询字符串，默认判断是否为手机宽度
 */
export function useMediaQuery(query = `(max-width: ${MOBILE_MAX_WIDTH}px)`) {
  const matches = ref(false)
  let mql: MediaQueryList | undefined

  const update = (event: MediaQueryList | MediaQueryListEvent) => {
    matches.value = event.matches
  }

  onMounted(() => {
    if (typeof window === 'undefined' || !window.matchMedia) {
      return
    }
    mql = window.matchMedia(query)
    matches.value = mql.matches
    mql.addEventListener('change', update)
  })

  onUnmounted(() => {
    mql?.removeEventListener('change', update)
  })

  return matches
}

/** 是否为手机尺寸 */
export function useIsMobile() {
  return useMediaQuery(`(max-width: ${MOBILE_MAX_WIDTH}px)`)
}
