package com.example.tiktok.entity.user;

import com.baomidou.mybatisplus.annotation.TableField;
import com.example.tiktok.entity.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String nickName;

    @Email
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    // 个人简介
    private String description;

    // true 为男 | false 为女
    private Boolean sex;

    // 用户默认收藏夹ID
    private Long defaultFavoritesId;

    // 头像
    private Long avatar;

    // 是否互关
    @TableField(exist = false)
    private Boolean each;

    // 用户所属角色
    @TableField(exist = false)
    private Set<String> roleName;
}
