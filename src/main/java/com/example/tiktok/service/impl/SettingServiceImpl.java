package com.example.tiktok.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.Setting;
import com.example.tiktok.mapper.SettingMapper;
import com.example.tiktok.service.SettingService;
import org.springframework.stereotype.Service;

@Service
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting> implements SettingService {

}
