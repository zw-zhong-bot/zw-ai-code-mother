import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 *
 * 注意：拉取登录态的请求必须捕获异常。后端不可用或接口报错时若放任异常抛出，
 * 会中断导航守卫，导致 next() 永不执行、页面渲染为空白。
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    try {
      await loginUserStore.fetchLoginUser()
      loginUser = loginUserStore.loginUser
    } catch (error) {
      console.warn('获取登录态失败，按未登录处理：', error)
    }
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
