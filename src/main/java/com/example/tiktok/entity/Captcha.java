package com.example.tiktok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统验证码
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Captcha implements Serializable {

    private static final long serialVersionUID = 1L;

    // uuid
    @TableId(value = "uuid", type = IdType.INPUT)
    @NotBlank(message = "uuid为空")
    private String uuid;

    // 验证码
    @NotBlank(message = "code为空")
    private String code;

    // 过期时间
    private Date expireTime;

    @Email
    @TableField(exist = false)
    private String email;
}
