package com.example.tiktok.entity.task;

import com.example.tiktok.entity.video.Video;
import lombok.Data;

/**
 * 封装发布视频
 */
@Data
public class VideoTask {
    // 新视频
    private Video video;

    // 老视频
    private Video oldVideo;

    // 是否是新增
    private Boolean isAdd;

    // 新 | 老状态: 0公开 1私密
    private Boolean oldState;
    private Boolean newState;
}
