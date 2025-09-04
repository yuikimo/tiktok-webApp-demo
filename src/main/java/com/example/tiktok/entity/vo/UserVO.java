package com.example.tiktok.entity.vo;

import lombok.Data;

@Data
public class UserVO {

    private Long id;
    // 用户昵称
    private String nickName;
    // 头像
    private Long avatar;
    // 性别
    private Boolean sex;
    // 个人简介
    private String description;
    // 关注数
    private Long follow;
    // 粉丝数
    private Long fans;
}
