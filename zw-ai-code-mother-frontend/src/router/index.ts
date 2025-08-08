import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserPage from '@/pages/user/UserPage.vue'
import UserUpdatePage from '@/pages/user/UserUpdate.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import noAuth from '@/pages/noAuth.vue'
import ACCESS_ENUM from '@/access/accessEnum.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/user',
      name: '用户信息',
      component: UserPage,
      meta: {
        access: ACCESS_ENUM.USER,
      },
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注销',
      component: UserRegisterPage,
    },
    {
      path: '/user/update',
      name: '修改用户信息',
      component: UserUpdatePage,
      meta: {
        access: ACCESS_ENUM.USER,
      },
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
      meta: {
        access: ACCESS_ENUM.ADMIN,
      },
    },
    {
      path: '/noAuth',
      name: '权限不足',
      component: noAuth,
    },
  ],
})

router.beforeEach((to) => {
  const userLoginUserStore = useLoginUserStore()
  if (to.meta.requiresAuth && !userLoginUserStore.loginUser) return '/noAuth'
})
export default router
