package com.example.tiktok.service.impl;

import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.service.FeedService;
import com.example.tiktok.utils.DateUtil;
import com.example.tiktok.utils.RedisCacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
public class FeedServiceImpl implements FeedService {

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户发布视频
     * @param userId  发件箱用户ID
     * @param videoId 视频ID
     * @param time
     */
    @Override
    @Async
    public void pushOutBoxFeed(Long userId, Long videoId, Long time) {
        // 发视频后，推送到自己的发件箱中
        redisCacheUtil.zAdd(RedisConstant.OUT_FOLLOW + userId, time, videoId, -1);
    }

    @Override
    public void pushInBoxFeed(Long userId, Long videoId, Long time) {
        // 需要推吗?这个场景？ 只需要拉
    }

    /**
     * 用户删除发布视频时
     * @param userId  当前用户
     * @param fans    粉丝ID
     * @param videoId 视频ID 需要删除的
     */
    @Override
    @Async
    public void deleteOutBoxFeed(Long userId, Collection<Long> fans, Long videoId) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 删除用户粉丝收件箱中的视频
            for (Long fan : fans) {
                connection.zRem((RedisConstant.IN_FOLLOW + fan).getBytes(), String.valueOf(videoId).getBytes());
            }
            // 删除用户发件箱中的视频
            connection.zRem((RedisConstant.OUT_FOLLOW + userId).getBytes(), String.valueOf(videoId).getBytes());
            return null;
        });
    }

    /**
     * 删除用户收件箱中的视频
     * @param userId   用户
     * @param videoIds 视频ID
     */
    @Override
    @Async
    public void deleteInBoxFeed(Long userId, List<Long> videoIds) {
        // 当前用户取关时调用 -> 删除自己收件箱中的videoIds
        redisTemplate.opsForZSet().remove(RedisConstant.IN_FOLLOW + userId, videoIds.toArray());
    }

    /**
     * 拉模式，用户拉取视频
     * @param userId 用户ID
     * @param followIds 关注ID
     */
    @Override
    @Async
    public void initFollowFeed(Long userId, Collection<Long> followIds) {
        String inFollow = RedisConstant.IN_FOLLOW;
        final Date curDate = new Date();
        final Date limitDate = DateUtil.addDateDays(curDate, -7);

        // 查询当前用户的收件箱中有没有数据
        final Set<ZSetOperations.TypedTuple<Long>> set =
                redisTemplate.opsForZSet().rangeWithScores(inFollow + userId, -1, -1);
        // 如果有数据，获取关注人时间差内的视频(时间差 = 最近视频的日期 - 当前的日期)并推入到用户的收件箱中
        if (!ObjectUtils.isEmpty(set)) {
            Double oldTime = set.iterator().next().getScore();
            update(userId, oldTime.longValue(), curDate.getTime(), followIds);
        } else {
            // 如果没有数据，获取7天内的视频
            update(userId, limitDate.getTime(), curDate.getTime(), followIds);
        }
    }

    /**
     * 将关注人发件箱中的视频添加到用户收件箱中
     * @param userId    当前用户
     * @param min       之前时间
     * @param max       当前时间
     * @param followIds 关注ID列表
     */
    public void update(Long userId, Long min, Long max, Collection<Long> followIds) {
        String outFollow = RedisConstant.OUT_FOLLOW;
        String inFollow = RedisConstant.IN_FOLLOW;

        // 获取关注人发件箱中的50条视频
        final List<Set<DefaultTypedTuple>> result =
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (Long followId : followIds) {
                        // 按指定时间排序，并分页
                        connection.zRevRangeByScoreWithScores((outFollow + followId).getBytes(), min, max, 0, 50);
                    }
                    return null;
                });

        final ObjectMapper objectMapper = new ObjectMapper();

        // 放入收件箱，并设置过期时间
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 遍历关注人发件箱视频
            for (Set<DefaultTypedTuple> tuples : result) {
                if (!ObjectUtils.isEmpty(tuples)) {
                    // 遍历关注人发件箱视频推入到用户收件箱中
                    for (DefaultTypedTuple tuple : tuples) {
                        final Object value = tuple.getValue();

                        final byte[] key = (inFollow + userId).getBytes();
                        try {
                            connection.zAdd(key, tuple.getScore(), objectMapper.writeValueAsBytes(value));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        connection.expire(key, RedisConstant.HISTORY_TIME);
                    }
                }
            }
            return null;
        });
    }
}
