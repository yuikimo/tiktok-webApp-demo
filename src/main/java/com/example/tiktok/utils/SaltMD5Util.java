package com.example.tiktok.utils;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.util.Random;

/**
 * 将明文密码进行MD5加盐加密
 */
public class SaltMD5Util {

    /**
     * 生成盐和加盐后的MD5码，并将盐混入到MD5码中,对MD5密码进行加强
     * @param password
     * @return
     */
    public static String generateSaltPassword(String password) {
        Random random = new Random();

        StringBuilder stringBuilder = new StringBuilder(16);
        stringBuilder.append(random.nextInt(99999999)).append(random.nextInt(99999999));
        int len = stringBuilder.length();
        if (len < 16) {
            for (int i = 0; i < 16 - len; i++) {
                stringBuilder.append("0");
            }
        }
        // 生成盐
        String salt = stringBuilder.toString();
        // 将盐加到明文中，并生成新的MD5码
        password = md5Hex(password + salt);
        // 将盐混到新生成的MD5码中，之所以这样做是为了后期更方便的校验明文和秘文，也可以不用这么做，不过要将盐单独存下来，不推荐这种方式
        char[] cs = new char[48];
        for (int i = 0; i < 48; i += 3) {
            cs[i] = password.charAt(i / 3 * 2);
            char c = salt.charAt(i / 3);
            cs[i + 1] = c;
            cs[i + 2] = password.charAt(i / 3 * 2 + 1);
        }
        return new String(cs);
    }

    /**
     * 验证明文和加盐后的MD5码是否匹配
     * @param password
     * @param md5
     * @return
     */
    public static boolean verifySaltPassword(String password, String md5) {
        // 先从MD5码中取出之前加的盐和加盐后生成的MD5码
        char[] cs1 = new char[32];
        char[] cs2 = new char[16];
        for (int i = 0; i < 48; i += 3) {
            cs1[i / 3 * 2] = md5.charAt(i);
            cs1[i / 3 * 2 + 1] = md5.charAt(i + 2);
            cs2[i / 3] = md5.charAt(i + 1);
        }
        String salt = new String(cs2);
        // 比较二者是否相同
        return md5Hex(password + salt).equals(new String(cs1));
    }

    /**
     * 生成MD5密码
     * @param src
     * @return
     */
    private static String md5Hex(String src) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bs = md5.digest(src.getBytes());
            return new String(new Hex().encode(bs));
        } catch (Exception e) {
            return null;
        }
    }
}