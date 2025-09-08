import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/pages/HomePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import AppAdminManagePage from '@/pages/admin/AppManagePage.vue'
import ChatHistoryManagePage from '@/pages/admin/ChatHistoryManagePage.vue'
import AppDetailPage from '@/pages/app/AppDetailPage.vue'
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
      path: '/app/chat/:id',
      name: '应用生成对话',
      component: AppChatPage,
      meta: { access: ACCESS_ENUM.USER },
    },
    {
      path: '/app/detail/:id',
      name: '应用详情',
      component: AppDetailPage,
      meta: { access: ACCESS_ENUM.USER },
    },
    {
      path: '/app/edit/:id',
      name: '应用信息修改',
      component: AppEditPage,
      meta: { access: ACCESS_ENUM.USER },
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppAdminManagePage,
      meta: { access: ACCESS_ENUM.ADMIN },
    },
    {
      path: '/admin/chatHistoryManage',
      name: '对话管理',
      component: ChatHistoryManagePage,
      meta: { access: ACCESS_ENUM.ADMIN },
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
