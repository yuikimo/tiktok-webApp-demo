package com.example.tiktok.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.tiktok.entity.user.Follow;
import com.example.tiktok.entity.vo.BasePage;

import java.util.Collection;

public interface FollowService extends IService<Follow> {
    /**
     * 获取关注数量
     */
    Long getFollowCount(Long userId);

    /**
     * 获取粉丝数量
     */
    Long getFansCount(Long userId);

    /**
     * 获取关注人员且按照关注时间排序
     */
    Collection<Long> getFollow(Long userId, BasePage basePage);

    /**
     * 获取粉丝人员且按照关注时间排序
     */
    Collection<Long> getFans(Long userId, BasePage basePage);

    /**
     * 关注/取关
     * @param followsId 对方id
     * @param userId 自己id
     * @return
     */
    Boolean follows(Long followsId, Long userId);

    /**
     * userId 是否关注 followId
     */
    Boolean isFollows(Long followId, Long userId);
}
