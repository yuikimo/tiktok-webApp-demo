package com.example.tiktok.utils;

import io.fusionauth.jwt.JWTException;
import io.fusionauth.jwt.Signer;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.hmac.HMACSigner;
import io.fusionauth.jwt.hmac.HMACVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.ObjectUtils;

import java.time.ZonedDateTime;
import java.util.Objects;

public class JwtUtils {

    // Token 过期时间
    public static final long EXPIRE = 7;
    public static final Signer SIGNER = HMACSigner.newSHA256Signer("ukc8BDbRigUDaY6pZFfWus2jZWLPHO");
    public static final Verifier VERIFIER = HMACVerifier.newVerifier("ukc8BDbRigUDaY6pZFfWus2jZWLPHO");

    /**
     * 生成 Token 字符串
     * @param id 用户ID
     * @return token
     */
    public static String getJwtToken (String id) {
        JWT jwt = new JWT()
                .setIssuer("https://www.tiktok.com")
                .setIssuedAt(ZonedDateTime.now())
                .setSubject(id)
                .setExpiration(ZonedDateTime.now().plusDays(EXPIRE));

        return JWT.getEncoder().encode(jwt, SIGNER);
    }

    /**
     * 判断 Token 是否存在 | 有效
     * @param jwtToken
     * @return
     */
    public static boolean checkToken (String jwtToken) {
        if (ObjectUtils.isEmpty(jwtToken)) {
            return false;
        }
        try {
            JWT.getDecoder().decode(jwtToken, VERIFIER);
        } catch (JWTException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 判断 Token 是否存在 | 有效
     * @param request
     * @return
     */
    public static boolean checkToken (HttpServletRequest request) {
        try {
            String jwtToken = request.getHeader("token");
            if (ObjectUtils.isEmpty(jwtToken)) {
                return false;
            }
            JWT.getDecoder().decode(jwtToken, VERIFIER);
        } catch (JWTException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Long getUserId (HttpServletRequest request) {
        String jwtToken = request.getHeader("token");
        if (ObjectUtils.isEmpty(jwtToken)) {
            return null;
        }
        JWT jwt = JWT.getDecoder().decode(jwtToken, VERIFIER);
        return Long.valueOf(jwt.subject);
    }
}
