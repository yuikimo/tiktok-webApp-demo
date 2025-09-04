package com.example.tiktok.schedul;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.tiktok.constant.AuditStatus;
import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.entity.video.Video;
import com.example.tiktok.entity.vo.HotVideo;
import com.example.tiktok.service.SettingService;
import com.example.tiktok.service.video.VideoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * 热度排行榜
 */
@Component
public class HotRank {

    @Autowired
    private VideoService videoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SettingService settingService;

    ObjectMapper objectMapper = new ObjectMapper();
    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer =
            new Jackson2JsonRedisSerializer(objectMapper, Object.class);

    /**
     * 热度排行榜
     */
    @Scheduled(cron="0 0 */1 * * ?")
    public void hotRank() {
        // 最小堆
        final TopK topK = new TopK(10, new PriorityQueue<>(10, Comparator.comparing(HotVideo::getHot)));

        // TODO 优化查询
        // 每次拿1000个
        long limit = 1000;
        long id = 0;

        List<Video> videos = getHotVideoList(id, limit);

        while (!ObjectUtils.isEmpty(videos)) {
            for (Video video : videos) {
                // 热度权重：点赞、评论、分享、转发、收藏...
                Long shareCount = video.getShareCount();
                Double historyCount = video.getHistoryCount() * 0.8;
                Long startCount = video.getStartCount();
                Double favoritesCount = video.getFavoritesCount() * 1.5;
                // 距离发布视频的时间差
                final Date date = new Date();
                long t = date.getTime() - video.getGmtCreated().getTime();

                // Redis zSet中是通过score进行去重 A B,添加随机获取6位小数，用于去重D
                final double weight = shareCount + historyCount + startCount + favoritesCount + weightRandom();

                final double hot = hot(weight, TimeUnit.MILLISECONDS.toDays(t));
                final HotVideo hotVideo = new HotVideo(hot, video.getId(), video.getTitle());

                topK.add(hotVideo);
            }

            // 获取上一次查询最后视频的ID
            id = videos.get(videos.size() - 1).getId();
            videos = getHotVideoList(id, limit);
        }
        final byte[] key = RedisConstant.HOT_RANK.getBytes();

        final List<HotVideo> hotVideos = topK.get();
        final Double minHot = hotVideos.get(hotVideos.size() - 1).getHot();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (HotVideo hotVideo : hotVideos) {
                final Double hot = hotVideo.getHot();
                try {
                    hotVideo.setHot(null);
                    connection.zAdd(key, hot,
                                    jackson2JsonRedisSerializer.serialize(objectMapper.writeValueAsString(hotVideo)));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            return null;
        });

        redisTemplate.opsForZSet().removeRangeByScore(RedisConstant.HOT_RANK, 0, minHot);
    }

    // 热门视频，没有热度排行榜实时且重要
    @Scheduled(cron="0 0 */3 * * ?")
    public void hotVideo() {
        // 分片查询3天内的视频
        int limit = 1000;
        long id = 1;

        // 查询3天前的视频
        List<Video> videos = videoService.selectNDaysAgeVideo(id, 3, limit);
        // 获取系统的热度阈值
        final Double hotLimit = settingService.list()
                                              .get(0)
                                              .getHotLimit();

        while (!ObjectUtils.isEmpty(videos)) {
            final ArrayList<Long> hotVideos = new ArrayList<>();

            // 对最近视频进行热度判断
            for (Video video : videos) {
                Long shareCount = video.getShareCount();
                Double historyCount = video.getHistoryCount() * 0.8;
                Long startCount = video.getStartCount();
                Double favoritesCount = video.getFavoritesCount() * 1.5;

                // 距离视频发布时间差
                final Date date = new Date();
                long t = date.getTime() - video.getGmtCreated().getTime();
                // 视频热度权重
                double weight = shareCount + historyCount + startCount + favoritesCount;
                final double hot = hot(weight, TimeUnit.MILLISECONDS.toDays(t));

                // 如果视频热度大于系统热度阈值
                if (hot > hotLimit) {
                    hotVideos.add(video.getId());
                }
            }

            // RedisConstant.HOT_VIDEO + 今日日期作为KEY 达到元素过期的效果
            // 结合VideoServiceImpl的listHotVideo()方法控制推送今天的还是昨天的视频
            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DATE);

            if (!ObjectUtils.isEmpty(hotVideos)) {
                String key = RedisConstant.HOT_VIDEO + today;
                redisTemplate.opsForSet().add(key, hotVideos.toArray(new Object[hotVideos.size()]));
                redisTemplate.expire(key, 3, TimeUnit.DAYS);
            }

            id = videos.get(videos.size() - 1).getId();
            // mysql的分页 limit offset
            // 现在的方式 id > ? limit x 对查询进行了优化
            videos = videoService.selectNDaysAgeVideo(id, 3, limit);
        }
    }

    // 半衰期参数
    static double a = 0.011;

    /**
     * 半衰期 -> 用于控制时间衰减对热度的影响
     * @param weight 热度权重
     * @param t      时间
     * @return
     */
    public static double hot(double weight, double t) {
        // 按照时间来进行指数衰减
        return weight * Math.exp(-a * t);
    }

    // 生成 [0.1 - 0.999999)之间的随机数
    public double weightRandom() {
        int i = (int) ((Math.random() * 9 + 1) * 100000);
        return i / 1000000.0;
    }

    private List<Video> getHotVideoList(long id, long limit) {
        return videoService.list(
                new LambdaQueryWrapper<Video>().select(Video::getId, Video::getShareCount, Video::getHistoryCount,
                                                       Video::getStartCount, Video::getFavoritesCount,
                                                       Video::getGmtCreated, Video::getTitle)
                                               .gt(Video::getId, id)
                                               .eq(Video::getAuditStatus, AuditStatus.SUCCESS)
                                               .eq(Video::getOpen, 0)
                                               .last("limit " + limit));
    }
}
