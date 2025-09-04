package com.example.tiktok.service.video.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.entity.video.Type;
import com.example.tiktok.mapper.video.TypeMapper;
import com.example.tiktok.service.video.TypeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
public class TypeServiceImpl extends ServiceImpl<TypeMapper, Type> implements TypeService {

    /**
     * 返回指定分类下的标签列表
     * @param typeId 指定分类ID
     * @return 标签列表
     */
    @Override
    public List<String> getLabels (Long typeId) {
        return this.getById(typeId).buildLabel();
    }

    /**
     * 随机返回10个标签
     * @return 标签列表
     */
    @Override
    public List<String> random10Labels () {

        final List<Type> types = list();
        Collections.shuffle(types);

        // 对每个分类都有其迭代器
        List<Iterator<String>> iterators = new ArrayList<>();
        for (Type type : types) {
            iterators.add(type.buildLabel().iterator());
        }

        final List<String> labels = new ArrayList<>();
        // 是否添加了标签
        boolean added = true;

        // 轮询算法
        while (labels.size() < 10 && added) {
            added = false;

            for (Iterator<String> iterator : iterators) {
                if (labels.size() == 10) {
                    break;
                }

                if (iterator.hasNext()) {
                    labels.add(iterator.next());
                    added = true;
                }
            }
        }

        return labels;
    }
}
