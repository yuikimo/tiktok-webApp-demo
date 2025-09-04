package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.user.RolePermission;
import com.example.tiktok.mapper.user.RolePermissionMapper;
import com.example.tiktok.service.user.RolePermissionService;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements
                                                                                                 RolePermissionService {

}
