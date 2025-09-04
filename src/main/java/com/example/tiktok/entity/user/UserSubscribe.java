package com.example.tiktok.entity.user;

import lombok.Data;

/**
 *  用户订阅表
 */
@Data
public class UserSubscribe {

    private Long id;
    // 视频分类ID
    private Long typeId;
    private Long userId;
}
