package com.example.tiktok.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.tiktok.authority.Authority;
import com.example.tiktok.entity.user.Role;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.user.UserRole;
import com.example.tiktok.entity.vo.BasePage;
import com.example.tiktok.service.user.RoleService;
import com.example.tiktok.service.user.UserRoleService;
import com.example.tiktok.service.user.UserService;
import com.example.tiktok.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleService roleService;

    /**
     * 用户列表
     * @return
     */
    @GetMapping("/list")
    @Authority("admin:user:list")
    public R list() {
        return R.ok()
                .data(userService.list());
    }

    @GetMapping("/page")
    @Authority("admin:user:page")
    public R list(BasePage basePage,
                  @RequestParam(required=false) String name) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>();
        wrapper.like(!ObjectUtils.isEmpty(name), User::getEmail, name);
        IPage<User> page = userService.page(basePage.page(), wrapper);

        // 查出用户角色中间表
        Map<Long, List<UserRole>> userRoleMap =
                userRoleService.list()
                               .stream()
                               .collect(Collectors.groupingBy(UserRole::getUserId));
        // 根据角色查出角色表信息
        Map<Long, String> roleMap =
                roleService.list()
                           .stream()
                           .collect(Collectors.toMap(Role::getId, Role::getName));

        Map<Long, Set<String>> map = new HashMap();
        userRoleMap.forEach((uId, rIds) -> {
            Set<String> roles = new HashSet<>();
            for (UserRole rId : rIds) {
                roles.add(roleMap.get(rId.getRoleId()));
            }
            map.put(uId, roles);
        });

        for (User user : page.getRecords()) {
            user.setRoleName(map.get(user.getId()));
        }

        return R.ok().data(page.getRecords()).count(page.getTotal());
    }
}
