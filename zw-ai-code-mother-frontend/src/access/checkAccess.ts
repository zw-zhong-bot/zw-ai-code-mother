/*
编写通用的权限校验方法
*因为菜单组件中要判断权限؜来过滤展示的菜单项、
* 权限拦截也要用到؜权限判断功能，所以抽离成公共模块。
* */
import ACCESS_ENUM from '@/access/accessEnum.ts'

/**
 * 检查权限（判断当前登录用户是否具有某个权限）
 * @param loginUser 当前登录用户
 * @param needAccess 需要有的权限
 * @return boolean 有无权限
 */
const checkAccess =( loginUser:any,needAccess = ACCESS_ENUM.NOT_LOGIN)=>{
  // 获取当前登录用户具有的权限（如果没有 loginUser，则表示未登录）
  const loginUserAccess =loginUser?.userRole ?? ACCESS_ENUM.NOT_LOGIN;
  if(needAccess === ACCESS_ENUM.NOT_LOGIN){
    return true
  }
  //如果用户登录，才能访问
  if(needAccess === ACCESS_ENUM.NOT_LOGIN){
    // 如果用户没登录，那么表示无权限
    if(loginUserAccess === ACCESS_ENUM.NOT_LOGIN){
      return false;
    }
  }
  // 如果需要管理员权限
  if(needAccess === ACCESS_ENUM.ADMIN){
    //如果不为管理员，表示无权限
    if(loginUserAccess !== ACCESS_ENUM.ADMIN){
      return false;
    }
  }
  return true;
}

export default checkAccess;

