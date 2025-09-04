package com.example.tiktok.service;

import com.example.tiktok.entity.Captcha;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.vo.FindPWVO;
import com.example.tiktok.entity.vo.RegisterVO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.http.HttpResponse;

public interface LoginService {

    /**
     * 登录
     */
    User login(User user);

    /**
     * 检查验证码
     */
    Boolean checkCode(String email, Integer code);

    /**
     * 生成图形验证码
     */
    void captcha(String uuid, HttpServletResponse response) throws IOException;

    /**
     * 获取邮箱验证码
     */
    Boolean getCode(Captcha captcha) throws Exception;

    /**
     * 注册账号
     */
    Boolean register(RegisterVO registerVO) throws Exception;

    /**
     * 找回密码
     */
    Boolean findPassword(FindPWVO findPWVO);
}
