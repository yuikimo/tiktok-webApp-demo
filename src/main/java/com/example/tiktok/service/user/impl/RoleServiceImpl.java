package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.user.Role;
import com.example.tiktok.entity.user.RolePermission;
import com.example.tiktok.entity.user.Tree;
import com.example.tiktok.entity.user.UserRole;
import com.example.tiktok.entity.vo.AssignRoleVO;
import com.example.tiktok.entity.vo.AuthorityVO;
import com.example.tiktok.mapper.user.RoleMapper;
import com.example.tiktok.service.user.PermissionService;
import com.example.tiktok.service.user.RolePermissionService;
import com.example.tiktok.service.user.RoleService;
import com.example.tiktok.service.user.UserRoleService;
import com.example.tiktok.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserRoleService userRoleService;

    @Override
    public List<Tree> tree() {
        List<Tree> trees = permissionService.list()
                                            .stream()
                                            .map(permission -> {
                                                Tree tree = new Tree();
                                                BeanUtils.copyProperties(permission, tree);
                                                tree.setTitle(permission.getName());
                                                tree.setSpread(true);
                                                return tree;
                                            })
                                            .collect(Collectors.toList());

        // 找到根节点
        List<Tree> parent = trees.stream()
                                 .filter(tree -> tree.getPId().compareTo(0L) == 0)
                                 .toList();

        for (Tree item : parent) {
            item.setChildren(new ArrayList<>());
            item.getChildren().add(findChildren(item, trees));
        }

        return parent;
    }

    @Override
    @Transactional
    public Boolean removeRole(String id) {
        try {
            // 删除角色权限中间表
            rolePermissionService.remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
            // 删除角色表
            baseMapper.deleteById(id);
            // 删除角色用户表
            userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        } catch (Exception e) {
            // 手动调用事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
        return true;
    }

    /**
     * 给角色分配权限
     * @param authorityVO
     * @return
     */
    @Override
    @Transactional
    public Boolean gavePermission(AuthorityVO authorityVO) {
        try {
            rolePermissionService.remove(new LambdaQueryWrapper<RolePermission>()
                                                 .eq(RolePermission::getRoleId, authorityVO.getRId()));

            Integer rid = authorityVO.getRId();
            List<RolePermission> list = new ArrayList<>();
            for (Integer pId : authorityVO.getPId()) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(rid);
                rolePermission.setPermissionId(pId);
                list.add(rolePermission);
            }
            rolePermissionService.saveBatch(list);
        } catch (Exception e) {
            // 手动调用事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
        return true;
    }

    /**
     * 给用户分配角色
     * @param assignRoleVO
     * @return
     */
    @Override
    @Transactional
    public Boolean gaveRole(AssignRoleVO assignRoleVO) {
        // 获取被分配角色的用户信息
        Long uId = assignRoleVO.getUId();
        try {
            userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, uId));
            List<UserRole> userRoles = new ArrayList<>();
            for (Long id : assignRoleVO.getRId()) {
                UserRole userRole = new UserRole();
                userRole.setUserId(uId);
                userRole.setRoleId(id);
                userRoles.add(userRole);
            }
            userRoleService.saveBatch(userRoles);
        } catch (Exception e) {
            // 手动调用事务回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
        return true;
    }

    private Tree findChildren(Tree datum, List<Tree> trees) {
        datum.setChildren(new ArrayList<>());
        for (Tree tree : trees) {
            if (tree.getPId().compareTo(datum.getId()) == 0) {
                datum.getChildren().add(findChildren(tree, trees));
            }
        }
        return datum;
    }
}
