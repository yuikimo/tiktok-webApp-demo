package com.example.tiktok.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.video.VideoShare;
import com.example.tiktok.mapper.video.VideoShareMapper;
import com.example.tiktok.service.video.VideoShareService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoShareServiceImpl extends ServiceImpl<VideoShareMapper, VideoShare> implements VideoShareService {
    @Override
    public boolean share (VideoShare videoShare) {
        try {
            // 利用videoId和IP作为唯一索引，少一次查询
            this.save(videoShare);
        } catch (Exception e) {
            // 不用删除
            return false;
        }
        return true;
    }

    @Override
    public List<Long> getShareUserId (Long videoId) {
        return this.list(new LambdaQueryWrapper<VideoShare>()
                                 .eq(VideoShare::getVideoId, videoId))
                   .stream()
                   .map(VideoShare::getUserId)
                   .toList();
    }
}
