package com.example.tiktok.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.user.Permission;

import java.util.List;
import java.util.Map;

public interface PermissionService extends IService<Permission> {
    Map<String, Object> initMenu(Long uId);

    List<Permission> treeSelect();

    void removeMenu(Long id);
}
