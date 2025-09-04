package com.example.tiktok.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.user.Role;
import com.example.tiktok.entity.user.Tree;
import com.example.tiktok.entity.vo.AssignRoleVO;
import com.example.tiktok.entity.vo.AuthorityVO;
import com.example.tiktok.utils.R;

import java.util.List;

public interface RoleService extends IService<Role> {
    List<Tree> tree();

    Boolean removeRole(String id);

    Boolean gavePermission(AuthorityVO authorityVO);

    Boolean gaveRole(AssignRoleVO assignRoleVO);
}
