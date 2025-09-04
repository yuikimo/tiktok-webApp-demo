package com.example.tiktok.entity.vo;

import com.example.tiktok.config.QiNiuConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
public class VideoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // YV ID 以及 YV + UUID
    private String yv;

    @NotBlank(message = "标题不能为空")
    private String title;
    // 视频简介
    private String description;

    @NotBlank(message = "URL不能为空")
    private String url;
    // 视频封面
    private String cover;

    // 是否公开
    private Boolean open;

    private Long userId;

    // 审核快慢状态 0 慢速 1 快速
    private Boolean auditQueueStatus;

    // 视频分类
    private String videoType;

    @NotBlank(message = "视频必须有相关标签")
    private String labelNames;

    @NotNull(message = "分类不能为空")
    private Long typeId;

    private Date gmtCreated;

    public List<String> buildLabel() {
        if (ObjectUtils.isEmpty(this.labelNames)) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(this.labelNames.split(","));
    }

    // 和get方法分开，避免发生歧义
    public String getVideoUrl(){
        return QiNiuConfig.CNAME + "/" + this.url;
    }

    public String getCoverUrl(){
        return QiNiuConfig.CNAME + "/" + this.cover;
    }
}
