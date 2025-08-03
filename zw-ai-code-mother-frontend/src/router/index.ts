import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path:'/user/login',
      name:"用户登录",
      component:UserLoginPage
    },
    {
      path:'/user/register',
      name:'用户注销',
      component:UserRegisterPage
    },
    {
      path:'/admin/userManage',
      name:'用户管理',
      component:UserManagePage
    }
  ],
})

export default router
