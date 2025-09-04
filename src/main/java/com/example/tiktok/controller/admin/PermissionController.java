package com.example.tiktok.controller.admin;

import com.example.tiktok.authority.Authority;
import com.example.tiktok.entity.user.Permission;
import com.example.tiktok.holder.UserHolder;
import com.example.tiktok.service.user.PermissionService;
import com.example.tiktok.utils.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authorize/permission")
public class PermissionController {
    @Resource
    private PermissionService permissionService;

    /**
     * 权限列表
     * @return
     */
    @GetMapping("/list")
    @Authority("permission:list")
    public List<Permission> list() {
        return permissionService.list();
    }

    /**
     * 新增权限时树形结构
     * @return
     */
    @GetMapping("/treeSelect")
    @Authority("permission:treeSelect")
    public List<Permission> treeSelect() {
        List<Permission> data = permissionService.treeSelect();
        return data;
    }

    /**
     * 添加权限
     * @param permission
     * @return
     */
    @PostMapping
    @Authority("permission:add")
    public R add(@RequestBody Permission permission) {
        permission.setIcon("fa " + permission.getIcon());
        permissionService.save(permission);

        return R.ok();
    }

    /**
     * 修改权限
     * @param permission
     * @return
     */
    @PutMapping
    @Authority("permission:update")
    public R update(@RequestBody Permission permission) {
        permission.setIcon("fa " + permission.getIcon());
        permissionService.updateById(permission);
        return R.ok();
    }

    /**
     * 删除权限
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Authority("permission:delete")
    public R delete(@PathVariable Long id) {
        permissionService.removeMenu(id);
        return R.ok().message("删除成功");
    }

    /**
     * 初始化菜单
     * @return
     */
    @GetMapping("/initMenu")
    public Map<String, Object> initMenu() {
        Map<String, Object> data = permissionService.initMenu(UserHolder.get());
        return data;
    }
}
