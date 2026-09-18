/**
 * 动态视口高度适配
 *
 * 背景：CSS 的 100vh 恒等于「浏览器地址栏隐藏时」的视口高度，在移动端会导致
 * 内容被裁切；且 iOS Safari 不支持 viewport 的 interactive-widget=resizes-content，
 * 软键盘弹出时视口不收缩，底部输入框会被键盘遮挡。
 *
 * 方案：用 visualViewport 的实时高度写入 --zw-vh，软键盘弹出时该值会同步变小，
 * 从而使使用 var(--zw-vh) 的容器跟随收缩，输入框保持在键盘上方可见。
 */
export function initViewportHeight() {
  const root = document.documentElement
  const vv = window.visualViewport

  const apply = () => {
    const height = vv ? vv.height : window.innerHeight
    if (height > 0) {
      root.style.setProperty('--zw-vh', `${Math.round(height)}px`)
    }
  }

  apply()

  if (vv) {
    // 键盘弹出/收起、页面缩放都会触发 resize；滚动时 visualViewport 偏移变化，
    // 在 iOS 上同样会引起可视高度变化，故一并监听。
    vv.addEventListener('resize', apply)
    vv.addEventListener('scroll', apply)
  } else {
    window.addEventListener('resize', apply)
  }

  // 横竖屏切换后可视高度变化存在延迟，补一次读取
  window.addEventListener('orientationchange', () => {
    window.setTimeout(apply, 150)
  })
}
