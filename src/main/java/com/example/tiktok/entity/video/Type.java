package com.example.tiktok.entity.video;

import com.baomidou.mybatisplus.annotation.TableField;
import com.example.tiktok.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.List;

/**
 * 分类,隐藏视频标签
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Type extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分类名称不可为空")
    private String name;

    private String description;

    private String icon;

    private Boolean open;

    // 分类下的标签
    private String labelNames;

    private Integer sort;

    @TableField(exist = false)
    private Boolean used;

    public List<String> buildLabel() {
        return Arrays.asList(labelNames.split(","));
    }
}
