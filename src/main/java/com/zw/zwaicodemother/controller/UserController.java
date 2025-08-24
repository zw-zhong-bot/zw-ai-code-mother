package com.zw.zwaicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.zw.zwaicodemother.annotation.AuthCheck;
import com.zw.zwaicodemother.common.BaseResponse;
import com.zw.zwaicodemother.common.DeleteRequest;
import com.zw.zwaicodemother.common.ResultUtils;
import com.zw.zwaicodemother.constant.UserConstant;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.model.dto.user.*;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.vo.LoginUserVO;
import com.zw.zwaicodemother.model.vo.UserVO;
import com.zw.zwaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /*
     *用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
    * */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody  UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest  == null , ErrorCode.PARAMS_ERROR);
        String userAccount =userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }
    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest  == null , ErrorCode.PARAMS_ERROR);
        String userAccount =userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO=userService.userLogin(userAccount,userPassword,request);
        return ResultUtils.success(loginUserVO);
    }
/*获取当前登录用户接口，
注意一定要 返回脱敏后的用户信息！
*
* */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    /*
    * 用户注销接口
    * */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout (HttpServletRequest request){
        ThrowUtils.throwIf(request == null , ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /*
    * 创建用户（仅管理员）
    * */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest  == null , ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtil.copyProperties(userAddRequest,user);
        //默认密码123456789
        final  String DEFAULT_PASSWORD ="123456789";
        String encryptPassword =userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result =userService.save(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /*
    *
    * 根据id获取用户（仅管理员）
    * */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public  BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id<=0 , ErrorCode.PARAMS_ERROR);
        User user =userService.getById(id);
        ThrowUtils.throwIf(user==null , ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user);
    }
    /*
    * 根据id获取包装类
    * */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id){
        BaseResponse<User> response =getUserById(id);
        User user =response.getData();
        return ResultUtils.success(userService.getUserVo(user));
    }

    /*
    * 删除用户
    * */
    @PostMapping("/delete")
    @AuthCheck (mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest ){
        if (deleteRequest == null || deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b=userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /*
    * 更新用户信息
    * */
    @PostMapping("/update")
//    @AuthCheck (mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        if (userUpdateRequest == null || userUpdateRequest.getId()==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);

        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest,user);
        boolean result =userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck (mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVoByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest == null ,ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage =userService.page(Page.of(pageNum,pageSize),
                userService.getQueryWrapper(userQueryRequest));
        //数据脱敏
        Page<UserVO> userVOPage=new Page<>(pageNum,pageSize,userPage.getTotalRow());
        List<UserVO> userVOList =userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);

    }

}
