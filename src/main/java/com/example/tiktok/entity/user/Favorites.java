package com.example.tiktok.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.example.tiktok.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收藏夹
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Favorites extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "收藏夹名称不能为空")
    private String name;

    private String description;

    private Long userId;

    // 收藏夹下的视频总数，前端未实现
    @TableField(exist = false)
    private Long videoCount;
}
