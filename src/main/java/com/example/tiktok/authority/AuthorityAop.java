package com.example.tiktok.authority;

import com.example.tiktok.exception.AuthorityException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验
 * 多个aop的情况下使用 @Order(1) 来指定顺序
 */
@Aspect
@Component
public class AuthorityAop {

    @Autowired
    private HttpServletRequest request;

    @Around("@annotation(authority)")
    public Object authority(ProceedingJoinPoint joinPoint, Authority authority) throws Throwable {
        Boolean result;
        Method method;
        Object verifyObject;

        if (!AuthorityUtils.getPostAuthority()) {
            // 全局校验类
            Class globalVerify = AuthorityUtils.getGlobalVerify();
            verifyObject = globalVerify.newInstance();
            method = globalVerify.getMethod("authorityVerify", HttpServletRequest.class, String[].class);
            result = (Boolean) method.invoke(verifyObject, request, authority.value());
            if (!result) {
                throw new AuthorityException("权限不足");
            }
        }
        return joinPoint.proceed();
    }
}
