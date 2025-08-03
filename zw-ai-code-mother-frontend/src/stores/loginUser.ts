//定义了登录用户的存储、远程获取、修改逻辑
import {defineStore}  from 'pinia'
import {ref } from 'vue'
import {getLoginUser} from '@/api/userController.ts'

export const useLoginUserStore = defineStore('loginUser', ()=>{
  //默认值
  const loginUser= ref<API.LoginUserVO>({
    userName:'未登录',
  })

  //获取登录用户信息
  async function fetchLoginUser(){
    const res = await getLoginUser()
    if(res.data.code === 0 && res.data.data){
      loginUser.value = res.data.data
    }
  }

  //更新登录用户信息
  function setLoginUser(newLoginUser:any){
    loginUser.value =newLoginUser
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})




