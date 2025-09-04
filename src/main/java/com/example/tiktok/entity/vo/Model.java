package com.example.tiktok.entity.vo;

import lombok.Data;

@Data
public class Model {
    private String label;
    private Long videoId;
    /**
     * 暴漏的接口只有根据停留时长 {@link com.example.tiktok.controller.CustomerController#updateUserModel}
     */
    private Double score;
}
