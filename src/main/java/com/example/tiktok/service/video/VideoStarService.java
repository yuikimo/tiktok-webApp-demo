package com.example.tiktok.service.video;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.video.VideoStar;

import java.util.List;

public interface VideoStarService extends IService<VideoStar> {

    /**
     * 视频点赞
     * @param videoStar
     * @return
     */
    boolean startVideo(VideoStar videoStar);

    /**
     * 视频点赞用户
     * @param videoId
     * @return
     */
    List<Long> getStarUserIds(Long videoId);

    /**
     * 点赞状态
     * @param videoId
     * @param userId
     * @return
     */
    Boolean startState(Long videoId, Long userId);
}
