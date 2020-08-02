package com.imooc.miaoshaproject.service;

import com.imooc.miaoshaproject.error.BusinessException;
import com.imooc.miaoshaproject.service.model.UserModel;

/**
 * Created by hzllb on 2018/11/11.
 */
public interface UserService {
    /**
     * @param id 通过用户ID获取用户对象的方法
     * @return
     */
    UserModel getUserById(Integer id);

    /**
     * 获取指定用户的缓存信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserModel getUserByIdInCache(Integer id);

    void register(UserModel userModel) throws BusinessException;

    /*
    telphone:用户注册手机
    password:用户加密后的密码
     */
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
