package com.example.tiktok.entity.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改用户信息
 */
@Data
public class UpdateUserVO {
    @NotBlank(message = "昵称不可为空")
    private String nickName;
    // 用户ID
    private Long id;
    // 用户头像
    private Long avatar;
    // 性别
    private Boolean sex;
    // 个人简介
    private String description;
    // 默认收藏夹ID
    private Long defaultFavoritesId;
}
