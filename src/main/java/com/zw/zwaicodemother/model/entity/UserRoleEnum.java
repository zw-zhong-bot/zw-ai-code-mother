package com.zw.zwaicodemother.model.entity;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;
/*
*
* 对于用户角色这样值的数量有限的؜、
* 可枚举的字段，
* 最好定义一个枚举类，
* 便于؜在项目中获取值、
* 减少枚举值输入错误的情况。*/
@Getter
public enum UserRoleEnum {
    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;

    private final  String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    /*
    * 根据value获取枚举2
    * getEnumByVa؜lue 是通过 value 找到具体的枚؜举对象
    * */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        for(UserRoleEnum  anEnim : UserRoleEnum.values()){
            if(anEnim.value.equals(value)){
                return anEnim;
            }
        }
        return null;
    }
}
