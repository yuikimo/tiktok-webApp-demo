package com.example.tiktok.service.video;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.video.VideoShare;

import java.util.List;

public interface VideoShareService extends IService<VideoShare> {

    /**
     * 添加分享记录
     * @param videoShare
     * @return
     */
    boolean share(VideoShare videoShare);

    /**
     * 获取分享用户ID
     * @param videoId
     * @return
     */
    List<Long> getShareUserId(Long videoId);
}
