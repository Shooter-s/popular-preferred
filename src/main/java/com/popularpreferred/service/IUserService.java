package com.popularpreferred.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.popularpreferred.dto.LoginFormDTO;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {



    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sedCode(String phone, HttpSession session);

    Result sign();

    Result signCount();

}
