package com.example.tiktok.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.entity.user.Follow;
import com.example.tiktok.entity.vo.BasePage;
import com.example.tiktok.exception.BaseException;
import com.example.tiktok.mapper.FollowMapper;
import com.example.tiktok.service.FeedService;
import com.example.tiktok.service.user.FollowService;
import com.example.tiktok.service.video.VideoService;
import com.example.tiktok.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Autowired
    private FeedService feedService;

    @Autowired
    @Lazy
    private VideoService videoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 获取用户关注数量
     * @param userId 用户ID
     * @return
     */
    @Override
    public Long getFollowCount (Long userId) {
        return count(new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId));
    }

    /**
     * 获取用户粉丝数量
     * @param userId 用户ID
     * @return
     */
    @Override
    public Long getFansCount (Long userId) {
        return count(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowId, userId));
    }

    /**
     * 获取用户关注ID列表
     * @param userId   用户ID
     * @param basePage
     * @return
     */
    @Override
    public Collection<Long> getFollow (Long userId, BasePage basePage) {
        // 如果不是分页查询
        if (Objects.isNull(basePage)) {
            final Set<Object> set = redisCacheUtil.zGet(RedisConstant.USER_FOLLOW + userId);
            if (ObjectUtils.isEmpty(set)) {
                return Collections.EMPTY_SET;
            }
            return set.stream()
                      .map(o -> Long.valueOf(o.toString()))
                      .toList();
        }

        // 如果是分页查询
        final Set<ZSetOperations.TypedTuple<Object>> typedTuples =
                redisCacheUtil.zSetGetByPage(RedisConstant.USER_FOLLOW + userId,
                                             basePage.getPage(),
                                             basePage.getLimit());
        // 可能Redis崩了，从db拿
        if (ObjectUtils.isEmpty(typedTuples)) {
            final List<Follow> follows = page(basePage.page(),
                                              new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId)
                                                                              .orderByDesc(Follow::getGmtCreated))
                    .getRecords();
            if (ObjectUtils.isEmpty(follows)) {
                return Collections.EMPTY_LIST;
            }
            return follows.stream()
                          .map(Follow::getFollowId)
                          .collect(Collectors.toList());
        }

        return typedTuples.stream()
                          .map(t -> Long.parseLong(t.getValue().toString()))
                          .toList();
    }

    /**
     * 获取用户粉丝ID列表
     * @param userId
     * @param basePage
     * @return
     */
    @Override
    public Collection<Long> getFans (Long userId, BasePage basePage) {
        // 如果不是分页查询
        if (Objects.isNull(basePage)) {
            final Set<Object> set = redisCacheUtil.zGet(RedisConstant.USER_FANS + userId);
            if (ObjectUtils.isEmpty(set)) {
                return Collections.EMPTY_SET;
            }
            return set.stream()
                      .map(o -> Long.valueOf(o.toString()))
                      .toList();
        }

        // 如果是分页查询
        final Set<ZSetOperations.TypedTuple<Object>> typedTuples =
                redisCacheUtil.zSetGetByPage(RedisConstant.USER_FANS + userId,
                                             basePage.getPage(),
                                             basePage.getLimit());
        // 可能Redis崩了，从db拿
        if (ObjectUtils.isEmpty(typedTuples)) {
            final List<Follow> follows = page(basePage.page(),
                                              new LambdaQueryWrapper<Follow>().eq(Follow::getFollowId, userId)
                                                                              .orderByDesc(Follow::getGmtCreated))
                    .getRecords();
            if (ObjectUtils.isEmpty(follows)) {
                return Collections.EMPTY_LIST;
            }

            return follows.stream()
                          .map(Follow::getUserId)
                          .toList();
        }

        return typedTuples.stream()
                          .map(t -> Long.parseLong(t.getValue().toString()))
                          .toList();
    }

    /**
     * 关注 | 取关
     * @param followsId 对方id
     * @param userId    自己id
     * @return
     */
    @Override
    public Boolean follows (Long followsId, Long userId) {
        if (followsId.equals(userId)) {
            throw new BaseException("不能关注自己");
        }

        // 直接保存(唯一索引),保存失败则删除
        final Follow follow = new Follow();
        follow.setFollowId(followsId);
        follow.setUserId(userId);

        try {
            save(follow);

            final Date date = new Date();
            // 用户自己的关注列表中添加
            redisTemplate.opsForZSet().add(RedisConstant.USER_FOLLOW + userId, followsId, date.getTime());
            // 对方的粉丝列表中添加
            redisTemplate.opsForZSet().add(RedisConstant.USER_FANS + followsId, userId, date.getTime());
        } catch (Exception e) {
            // 删除
            remove(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowId, followsId)
                                                   .eq(Follow::getUserId, userId));

            // 获取关注人的视频
            final List<Long> videoIds = (List<Long>) videoService.listVideoIdByUserId(followsId);
            // 删除用户收件箱中推送的关注人视频
            feedService.deleteInBoxFeed(userId, videoIds);

            // 用户自己的关注列表中删除
            redisTemplate.opsForZSet().remove(RedisConstant.USER_FOLLOW + userId, followsId);
            // 对方的粉丝列表中删除
            redisTemplate.opsForZSet().remove(RedisConstant.USER_FANS + followsId, userId);
            return false;
        }
        return true;
    }

    @Override
    public Boolean isFollows (Long followId, Long userId) {
        if (Objects.isNull(userId) || Objects.isNull(followId)) {
            return false;
        }
        return count(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowId, followId).eq(Follow::getUserId, userId)) == 1;
    }
}
