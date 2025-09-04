package com.example.tiktok.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.video.VideoStar;
import com.example.tiktok.mapper.video.VideoStarMapper;
import com.example.tiktok.service.video.VideoStarService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class VideoStarServiceImpl extends ServiceImpl<VideoStarMapper, VideoStar> implements VideoStarService {
    @Override
    public boolean startVideo (VideoStar videoStar) {
        try {
            // 唯一索引
            this.save(videoStar);
        } catch (Exception e) {
            // 存在则取消点赞
            this.remove(new LambdaQueryWrapper<VideoStar>().eq(VideoStar::getVideoId, videoStar.getVideoId())
                                                           .eq(VideoStar::getUserId, videoStar.getUserId()));
            return false;
        }
        return true;
    }

    @Override
    public List<Long> getStarUserIds (Long videoId) {
        return this.list(new LambdaQueryWrapper<VideoStar>().eq(VideoStar::getVideoId, videoId))
                   .stream()
                   .map(VideoStar::getUserId)
                   .toList();
    }

    @Override
    public Boolean startState (Long videoId, Long userId) {
        if (Objects.isNull(userId)) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<VideoStar>().eq(VideoStar::getVideoId, videoId)
                                                             .eq(VideoStar::getUserId, userId))
               == 1;
    }
}
