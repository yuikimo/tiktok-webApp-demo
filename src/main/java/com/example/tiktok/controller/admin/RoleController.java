package com.example.tiktok.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.tiktok.authority.Authority;
import com.example.tiktok.entity.user.Role;
import com.example.tiktok.entity.user.RolePermission;
import com.example.tiktok.entity.user.Tree;
import com.example.tiktok.entity.user.UserRole;
import com.example.tiktok.entity.vo.AssignRoleVO;
import com.example.tiktok.entity.vo.AuthorityVO;
import com.example.tiktok.entity.vo.BasePage;
import com.example.tiktok.service.user.RolePermissionService;
import com.example.tiktok.service.user.RoleService;
import com.example.tiktok.service.user.UserRoleService;
import com.example.tiktok.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/authorize/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserRoleService userRoleService;

    @GetMapping("/treeList")
    @Authority("permission:treeList")
    public List<Tree> treeList() {
        List<Tree> data = roleService.tree();
        return data;
    }

    @PostMapping("/assignRole")
    @Authority("user:assignRole")
    public R assignRole(@RequestBody AssignRoleVO assignRoleVO) {
        return roleService.gaveRole(assignRoleVO) ?
               R.ok().message("分配角色成功") :
               R.error().message("分配角色失败");
    }

    @GetMapping("/getUserRole/{userId}")
    @Authority("role:getRole")
    public List getRole(@PathVariable Integer userId) {
        List<Long> list = userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
                                                                                 .select(UserRole::getRoleId))
                                         .stream()
                                         .map(UserRole::getRoleId)
                                         .toList();
        return list;
    }

    /**
     * 初始化角色
     */
    @GetMapping("/initRole")
    @Authority("role:initRole")
    public List<Map<String, Object>> initRole() {
        // 查出所有角色
        List<Map<String, Object>> list = roleService.list()
                                                    .stream()
                                                    .map(role -> {
                                                        Map<String, Object> data = new HashMap<>();
                                                        data.put("value", role.getId());
                                                        data.put("title", role.getName());
                                                        return data;
                                                    })
                                                    .toList();
        return list;
    }

    @GetMapping("/list")
    @Authority("role:list")
    public R list(BasePage basePage, @RequestParam(required=false) String name) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!ObjectUtils.isEmpty(name), Role::getName, name);
        final IPage iPage = basePage.page();
        IPage<Role> page = roleService.page(iPage, wrapper);
        return R.ok()
                .data(page.getRecords())
                .count(page.getRecords().size());
    }

    /**
     * 添加角色
     * @param role
     * @return
     */
    @PostMapping
    @Authority("role:add")
    public R add(@RequestBody Role role) {
        roleService.save(role);
        return R.ok();
    }

    /**
     * 修改角色
     * @param role
     * @return
     */
    @PutMapping
    @Authority("role:update")
    public R update(@RequestBody Role role) {
        roleService.updateById(role);
        return R.ok();
    }

    /**
     * 删除角色
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Authority("role:delete")
    public R delete(@PathVariable String id) {
        return roleService.removeRole(id) ?
               R.ok().message("删除成功") :
               R.error().message("删除失败");
    }

    /**
     * 给角色分配权限
     * @param authorityVO
     * @return 给角色分配权限前把该角色的权限都删除，然后再插入新的权限
     */
    @PostMapping("/authority")
    @Authority("role:authority")
    public R authority(@RequestBody AuthorityVO authorityVO) {
        return roleService.gavePermission(authorityVO) ?
               R.ok().message("分配权限成功") :
               R.error().message("分配权限失败");
    }

    /**
     * 获取角色权限
     * @param id
     * @return
     */
    @GetMapping("/getPermission/{id}")
    @Authority("role:getPermission")
    public Integer[] getPermission(@PathVariable Integer id) {
        Integer[] list = rolePermissionService.list(new LambdaQueryWrapper<RolePermission>()
                                                            .eq(RolePermission::getRoleId, id)
                                                            .select(RolePermission::getPermissionId))
                                              .stream()
                                              .map(RolePermission::getPermissionId)
                                              .toArray(Integer[]::new);
        return list;
    }
}
