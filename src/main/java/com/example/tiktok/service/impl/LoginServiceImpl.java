package com.example.tiktok.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.entity.Captcha;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.vo.FindPWVO;
import com.example.tiktok.entity.vo.RegisterVO;
import com.example.tiktok.exception.BaseException;
import com.example.tiktok.mapper.CaptchaMapper;
import com.example.tiktok.service.CaptchaService;
import com.example.tiktok.service.LoginService;
import com.example.tiktok.service.user.UserService;
import com.example.tiktok.utils.RedisCacheUtil;
import com.example.tiktok.utils.SaltMD5Util;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.http.HttpResponse;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private CaptchaMapper captchaMapper;

    @Override
    public User login (User user) {
        final String password = user.getPassword();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        user = userService.getOne(wrapper.eq(User::getEmail, user.getEmail()));

        if (ObjectUtils.isEmpty(user)) {
            throw new BaseException("该账号不存在");
        }
        if (!SaltMD5Util.verifySaltPassword(password, user.getPassword())) {
            throw new BaseException("密码输出错误");
        }

        return user;
    }

    @Override
    public Boolean checkCode (String email, Integer code) {
        if (ObjectUtils.isEmpty(email) || ObjectUtils.isEmpty(code)) {
            throw new BaseException("参数为空");
        }

        final Object o = redisCacheUtil.get(RedisConstant.EMAIL_CODE + email);

        if (!code.toString().equals(o)) {
            throw new BaseException("验证码不正确");
        }

        return true;
    }

    @Override
    public void captcha (String uuid, HttpServletResponse response) throws IOException {
        if (ObjectUtils.isEmpty(uuid)) {
            throw new IllegalArgumentException("UUID不能为空");
        }

        response.setHeader("Cache-Control", "no-store,no-cache");
        response.setContentType("image/jpeg");

        BufferedImage image = captchaService.getCaptcha(uuid);
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        IOUtils.closeQuietly(out);
    }

    @Override
    public Boolean getCode (Captcha captcha) throws Exception {
        return captchaService.validate(captcha);
    }

    @Override
    public Boolean register (RegisterVO registerVO) throws Exception {
        // 注册成功后删除图形验证码
        if (userService.register(registerVO)) {
            captchaService.removeById(registerVO.getUuid());
            return true;
        }
        return false;
    }

    @Override
    public Boolean findPassword (FindPWVO findPWVO) {
        final Boolean b = userService.findPassword(findPWVO);
        return b;
    }
}
