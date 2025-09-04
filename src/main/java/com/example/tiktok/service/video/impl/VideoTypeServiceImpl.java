package com.example.tiktok.service.video.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.video.VideoType;
import com.example.tiktok.mapper.video.VideoTypeMapper;
import com.example.tiktok.service.video.VideoTypeService;
import org.springframework.stereotype.Service;

@Service
public class VideoTypeServiceImpl extends ServiceImpl<VideoTypeMapper, VideoType> implements VideoTypeService {

}

