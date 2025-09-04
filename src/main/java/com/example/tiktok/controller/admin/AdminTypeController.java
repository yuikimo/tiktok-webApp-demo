package com.example.tiktok.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.tiktok.authority.Authority;
import com.example.tiktok.entity.video.Type;
import com.example.tiktok.entity.vo.BasePage;
import com.example.tiktok.service.video.TypeService;
import com.example.tiktok.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/type")
public class AdminTypeController {

    @Autowired
    private TypeService typeService;

    /**
     * 获取指定分类
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Authority("admin:type:get")
    public R get(@PathVariable Long id) {
        return R.ok()
                .data(typeService.getById(id));
    }

    /**
     * 分页获取分类
     * @param basePage
     * @return
     */
    @GetMapping("/page")
    @Authority("admin:type:page")
    public R page(BasePage basePage) {
        final IPage page = typeService.page(basePage.page());
        return R.ok()
                .data(page.getRecords())
                .count(page.getTotal());
    }

    /**
     * 添加新的分类
     * @param type
     * @return
     */
    @PostMapping
    @Authority("admin:type:add")
    public R add(@RequestBody @Validated Type type) {
        final long count = typeService.count(new LambdaQueryWrapper<Type>().eq(Type::getName, type.getName())
                                                                           .ne(Type::getId, type.getId()));
        if (count == 1) {
            return R.error()
                    .message("该分类已存在");
        }

        typeService.save(type);
        return R.ok()
                .message("添加成功");
    }

    @PutMapping
    @Authority("admin:type:update")
    public R update(@RequestBody @Validated Type type) {
        final long count = typeService.count(new LambdaQueryWrapper<Type>().eq(Type::getName, type.getName())
                                                                           .ne(Type::getId, type.getId()));
        if (count == 1) {
            return R.error()
                    .message("该分类已存在");
        }

        typeService.updateById(type);
        return R.ok()
                .message("修改成功");
    }

    @DeleteMapping("/{id}")
    @Authority("admin:type:delete")
    public R delete(@PathVariable Long id) {
        typeService.removeById(id);
        return R.ok()
                .message("删除成功");
    }
}
