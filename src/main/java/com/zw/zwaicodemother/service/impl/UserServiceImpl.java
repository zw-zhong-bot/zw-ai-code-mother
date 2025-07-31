package com.zw.zwaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.model.dto.user.UserQueryRequest;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.mapper.UserMapper;
import com.zw.zwaicodemother.model.entity.UserRoleEnum;
import com.zw.zwaicodemother.model.vo.LoginUserVO;
import com.zw.zwaicodemother.model.vo.UserVO;
import com.zw.zwaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zw.zwaicodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if (userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户过短");

        }
        if (userPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if (userPassword.length()>16){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过长");
        }
        if (!checkPassword.equals(userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入密码不一致");

        }
        //检查是否重复
        QueryWrapper  queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count=this.mapper.selectCountByQuery(queryWrapper);
        if (count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户重复");
        }
        //加密
        String enctypttPassword = getEncryptPassword(userPassword);
        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(enctypttPassword);
        user.setUserName("userAccount");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user==null){
            return null;
        }
        LoginUserVO loginUserVO=new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVO);
        return loginUserVO;
    }

    /*
    * 用户登录成功后؜，将用户信息存储在当前的 Session 中
    * */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验
        if (StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if (userAccount.length()<4){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"账号错误");
        }
        if (userPassword.length()<8){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
        }
        //加密
        String enctypttPassword = getEncryptPassword(userPassword);
        //查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", enctypttPassword);
        User user=this.mapper.selectOneByQuery(queryWrapper);
        //用户不存在
        if (user==null){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        }
        //记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE ,user);
        //获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

/*
*
* 先从 Session ؜中获取登录用户的 id，然后从数据库中查询最新的结果*/
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //先判断是否已登录
        Object userObj=request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser=(User) userObj;
        if(currentUser==null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId=currentUser.getId();
        currentUser=this.getById(userId);
        if (currentUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        //判断是否已登录
        Object userObj =request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未登录");
        }
        //移除登录状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVo(User user) {
        if(user==null)return null;

    UserVO userVO = new UserVO();
    BeanUtil.copyProperties(user,userVO);
    return  userVO;
    }

    public String getEncryptPassword(String userPassword) {
        //盐值，混淆密码
        final String SALT ="zwpass";
        return DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id=userQueryRequest.getId();
        String userAccount=userQueryRequest.getUserAccount();
        String userName=userQueryRequest.getUserName();
        String userProfile=userQueryRequest.getUserProfile();
        String userRole=userQueryRequest.getUserRole();
        String sortField=userQueryRequest.getSortField();
        String sortOrder=userQueryRequest.getSortOrder();

        return QueryWrapper.create()
                 . eq("id",id)
                .like("userAccount",userAccount)
                .like("userName",userName)
                .like("userProfile",userProfile)
                .eq("userRole",userRole)
                .orderBy(sortField,"ascend".equals(sortOrder));

    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }
}
