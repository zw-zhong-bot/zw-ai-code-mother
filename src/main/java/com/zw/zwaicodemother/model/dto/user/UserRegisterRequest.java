package com.zw.zwaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;
/*
* 在 Java 接口开发中，为每个接口定义一个专门的类来؜接收请求参数，
* 可以提高代码的可读性和维护性，
* 便于对参数进行统一验证和扩展，
* 同时减؜少接口方法参数过多导致的复杂性，
* 有助于在复杂场景下更清晰地管理和传递数据。*/

/*
* 用户注册请求
* */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    //账号
    private String  userAccount;

    //密码
    private String  userPassword;

    //确认密码
    private String  checkPassword;

}
