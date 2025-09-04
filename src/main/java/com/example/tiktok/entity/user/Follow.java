package com.example.tiktok.entity.user;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class Follow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 关注人id
    private Long followId;

    // 粉丝人id
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreated;
}
