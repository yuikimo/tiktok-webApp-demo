package com.example.tiktok.entity.response;

import com.example.tiktok.entity.task.VideoTask;
import lombok.Data;
import lombok.ToString;

/**
 * 封装视频审核返回结果
 */
@Data
@ToString
public class VideoAuditResponse {

    // 视频审核
    private AuditResponse videoAuditResponse;
    // 封面审核
    private AuditResponse imageAuditResponse;
    // 视频简介审核
    private AuditResponse textAuditResponse;
    // 发布视频
    private VideoTask videoTask;
}

