package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.user.FavoritesVideo;
import com.example.tiktok.mapper.user.FavoritesVideoMapper;
import com.example.tiktok.service.user.FavoritesVideoService;
import org.springframework.stereotype.Service;

@Service
public class FavoritesVideoServiceImpl extends ServiceImpl<FavoritesVideoMapper, FavoritesVideo>
        implements FavoritesVideoService {

}
