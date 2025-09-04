package com.example.tiktok.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 模型
 */
@Data
public class ModelVO {

    private Long userId;
    // 兴趣视频标签
    private List<String> labels;
}

