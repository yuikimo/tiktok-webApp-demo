package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.user.UserSubscribe;
import com.example.tiktok.mapper.user.UserSubscribeMapper;
import com.example.tiktok.service.user.UserSubscribeService;
import org.springframework.stereotype.Service;

@Service
public class UserSubscribeServiceImpl extends ServiceImpl<UserSubscribeMapper, UserSubscribe> implements
                                                                                               UserSubscribeService {
}
