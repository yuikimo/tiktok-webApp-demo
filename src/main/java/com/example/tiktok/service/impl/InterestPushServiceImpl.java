package com.example.tiktok.service.impl;

import com.example.tiktok.constant.RedisConstant;
import com.example.tiktok.entity.user.User;
import com.example.tiktok.entity.video.Video;
import com.example.tiktok.entity.vo.HotVideo;
import com.example.tiktok.entity.vo.Model;
import com.example.tiktok.entity.vo.UserModel;
import com.example.tiktok.service.InterestPushService;
import com.example.tiktok.service.video.TypeService;
import com.example.tiktok.utils.RedisCacheUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// 暂时为异步
@Service
public class InterestPushServiceImpl implements InterestPushService {

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TypeService typeService;

    final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将审核通过的视频放入系统库中
     * @param video
     */
    @Override
    @Async
    public void pushSystemStockIn(Video video) {
        // 往系统库中添加
        final List<String> labels = video.buildLabel();
        final Long videoId = video.getId();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String label : labels) {
                connection.sAdd((RedisConstant.SYSTEM_STOCK + label).getBytes(), String.valueOf(videoId).getBytes());
            }
            return null;
        });
    }

    /**
     * 对分类推送进行分片，防止大Key问题
     * @param video
     */
    @Override
    @Async
    public void pushSystemTypeStockIn(Video video) {
        final Long typeId = video.getTypeId();
        final String typeKey = RedisConstant.SYSTEM_TYPE_STOCK + typeId;

        // 获取当前分类的分片ID
        Integer currentShardId = (Integer) redisCacheUtil.get(typeKey);
        if (currentShardId == null) {
            currentShardId = 1;
            redisCacheUtil.set(typeKey, currentShardId);
        }

        // 拼接分片Key
        String shardKey = typeKey + ":" + currentShardId;

        // 获取当前分片的视频数量
        Long currentSize = redisCacheUtil.lGetListSize(shardKey);

        if (currentSize != null && currentSize >= 5000) {
            currentShardId += 1;
            shardKey = typeKey + ":" + currentShardId;
        }

        redisCacheUtil.lSet(shardKey, video.getId());
    }

    /**
     * 根据分类返回视频ID列表
     * @param typeId 分类ID
     * @return
     */
    @Override
    public Collection<Long> listVideoIdByTypeId(Long typeId) {
        // 获取当前分类的分片ID
        final String typeKey = RedisConstant.SYSTEM_TYPE_STOCK + typeId;
        Integer currentShardId = (Integer) redisCacheUtil.get(typeKey);

        if (currentShardId == null) {
            return Collections.emptyList();
        }

        final HashSet<Long> result = new HashSet<>();

        final int totalCount = 10;
        final int maxNewCount = 7;
        long newVideoAvailable = 0;

        // 取最新分片Key和分片Key-1
        List<Integer> newVideoShardIds = new ArrayList<>();
        if (currentShardId >= 1) {
            newVideoShardIds.add(currentShardId);
        }
        if (currentShardId - 1 >= 1) {
            newVideoShardIds.add(currentShardId - 1);
        }

        // 获取其分片下的视频数量
        for (Integer shardId : newVideoShardIds) {
            String shardKey = typeKey + ":" + shardId;
            Long size = redisCacheUtil.lGetListSize(shardKey);
            if (size != null) {
                newVideoAvailable += size;
            }
        }

        // 新视频最多取7条
        int newVideoCount = (int) Math.min(newVideoAvailable, maxNewCount);
        // 老视频取剩余的
        int oldVideoCount = totalCount - newVideoCount;

        int need = newVideoCount;
        for (Integer shardId : newVideoShardIds) {
            if (need <= 0) {
                break;
            }
            String shardKey = typeKey + ":" + shardId;
            Long size = redisCacheUtil.lGetListSize(shardKey);

            if (size == null || size == 0) {
                continue;
            }

            int endIndex = (int) Math.min(need - 1, size - 1);
            List<Object> vIds = redisCacheUtil.lGet(shardKey, 0, endIndex);

            for (Object vId : vIds) {
                if (vId != null) {
                    result.add(Long.parseLong(vId.toString()));
                }
            }
            need -= vIds.size();
        }

        // 获取老分片ID列表，假设老分片ID小于 currentShardId - 1
        List<Integer> oldShardIds = new ArrayList<>();
        for (int i = 1; i < currentShardId - 1; i++) {
            oldShardIds.add(i);
        }

        if (oldShardIds.isEmpty() || oldVideoCount <= 0) {
            return result;
        }

        // 随机打乱老分片ID列表
        Collections.shuffle(oldShardIds);

        for (Integer oldShardId : oldShardIds) {
            if (oldVideoCount <= 0) {
                break;
            }
            String oldShardKey = typeKey + ":" + oldShardId;
            Long size = redisCacheUtil.lGetListSize(oldShardKey);
            if (size == null || size == 0) {
                continue;
            }

            // 随机获取一个索引
            int randomIndex = (int) (Math.random() * size);
            List<Object> vIdList = redisCacheUtil.lGet(oldShardKey, randomIndex, randomIndex);

            if (vIdList != null && !vIdList.isEmpty()) {
                Object vId = vIdList.get(0);
                if (vId != null) {
                    result.add(Long.parseLong(vId.toString()));
                    oldVideoCount--;
                }
            }
        }

        return result;
    }

    /**
     * 删除系统标签库中的视频
     * @param video
     */
    @Override
    @Async
    public void deleteSystemStockIn(Video video) {
        final List<String> labels = video.buildLabel();
        final Long videoId = video.getId();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String label : labels) {
                connection.sRem((RedisConstant.SYSTEM_STOCK + label).getBytes(), String.valueOf(videoId).getBytes());
            }
            return null;
        });
    }

    /**
     * 删除系统分类库中的视频
     * @param video
     */
    @Override
    @Async
    public void deleteSystemTypeStockIn(Video video) {
        final Long typeId = video.getTypeId();
        redisCacheUtil.setRemove(RedisConstant.SYSTEM_TYPE_STOCK + typeId, video.getId());
    }

    /**
     * 初始化用户模型（用户在订阅分类后，需要重新初始化模型）
     * @param userId 用户ID
     * @param labels 标签名
     */
    @Override
    @Async
    public void initUserModel(Long userId, List<String> labels) {
        final String key = RedisConstant.USER_MODEL + userId;
        // Key: label Value: 概率
        Map<Object, Object> modelMap = new HashMap<>();

        if (!ObjectUtils.isEmpty(redisCacheUtil.hmGet(key))) {
            redisCacheUtil.del(key);
        }

        if (!ObjectUtils.isEmpty(labels)) {
            final int size = labels.size();
            // 将标签分为等分概率，不可能超过100个标签
            double probabilityValue = (double) 100 / size;
            for (String labelName : labels) {
                modelMap.put(labelName, probabilityValue);
            }
        }

        // 用户模型设置ttl，如果用户一段时间没有上线后，请空用户模型.
        redisCacheUtil.hmSet(key, modelMap, RedisConstant.USER_MODEL_TIME);
    }

    /**
     * 更新用户模型
     * @param userModel
     */
    @Override
    @Async
    public void updateUserModel(UserModel userModel) {
        final Long userId = userModel.getUserId();

        // 如果是用户，获取用户模型并更新
        if (!Objects.isNull(userId)) {
            final List<Model> models = userModel.getModels();
            // 用户模型Key
            String key = RedisConstant.USER_MODEL + userId;
            // Key: label Value: 概率
            Map<Object, Object> modelMap = redisCacheUtil.hmGet(key);
            // 可能为空
            if (Objects.isNull(modelMap)) {
                modelMap = new HashMap<>();
            }

            // 衰减系数，控制旧数据衰减速度
            final double alpha = 0.9;

            // 先队旧数据进行指数衰减
            Iterator<Map.Entry<Object, Object>> iterator = modelMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Object> entry = iterator.next();
                double oldScore = Double.parseDouble(entry.getValue().toString());
                double decayedScore = oldScore * alpha;

                if (decayedScore <= 0.0001) {
                    iterator.remove();
                } else {
                    entry.setValue(decayedScore);
                }
            }

            for (Model model : models) {
                String label = model.getLabel();
                double deltaScore = model.getScore();

                double currentScore = 0.0;

                // 修改用户模型
                if (modelMap.containsKey(label)) {
                    currentScore = Double.parseDouble(modelMap.get(model.getLabel()).toString());
                }
                double newScore = currentScore + deltaScore;

                if (newScore <= 0.00001) {
                    modelMap.remove(label);
                } else {
                    modelMap.put(label, newScore);
                }
            }

            double totalScore = 0.0;
            for (Object value : modelMap.values()) {
                totalScore += Double.parseDouble(value.toString());
            }

            if (totalScore <= 0.0) {
                modelMap.clear();
            } else {
                for (Object keyLabel : modelMap.keySet()) {
                    double score = Double.parseDouble(modelMap.get(keyLabel).toString()) / totalScore;
                    modelMap.put(keyLabel, score);
                }
            }
            // 更新用户模型
            redisCacheUtil.hmSet(key, modelMap, RedisConstant.USER_MODEL_TIME);
        }
    }

    /**
     * 随机推送热门视频
     * @return
     */
    public Long randomHotVideoId() {
        final Object o = redisTemplate.opsForZSet().randomMember(RedisConstant.HOT_RANK);
        try {
            return objectMapper.readValue(o.toString(), HotVideo.class).getVideoId();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据用户性别推送相关标签视频
     * @param sex 用户性别
     * @return
     */
    public Long randomVideoId(Boolean sex) {
        String key = RedisConstant.SYSTEM_STOCK + (sex ? "美女" : "宠物");
        final Object o = redisCacheUtil.sRandom(key);
        if (!Objects.isNull(o)) {
            return Long.parseLong(o.toString());
        }
        return null;
    }

    // 随机获取视频ID
    public Long getVideoId(Random random, String[] probabilityArray) {
        // 随机获取标签数组中的标签
        String labelName = probabilityArray[random.nextInt(probabilityArray.length)];

        // 获取对应所有视频
        String key = RedisConstant.SYSTEM_STOCK + labelName;
        final Object o = redisCacheUtil.sRandom(key);
        if (!Objects.isNull(o)) {
            return Long.parseLong(o.toString());
        }

        return null;
    }

    // 初始化概率数组 -> 保存的元素是标签
    // 对标抽奖算法
    public String[] initProbabilityArray(Map<Object, Object> modelMap) {
        Map<String, Integer> probabilityMap = new HashMap<>();
        int size = modelMap.size();
        // 总概率
        final AtomicInteger n = new AtomicInteger();
        modelMap.forEach((k, v) -> {
            // 防止结果为0，每个同等加上标签数
            // 可能为同比概率增减
            int probability = (((Double) v).intValue()) / size;
            // 将计算得到的probability值累加到n中，更新总概率。
            n.getAndAdd(probability);
            probabilityMap.put(k.toString(), probability);
        });
        // 创建一个字符串数组其长度为总概率
        final String[] probabilityArray = new String[n.get()];
        // 跟踪当前的索引位置。
        final AtomicInteger index = new AtomicInteger(0);
        // 初始化数组
        probabilityMap.forEach((label, p) -> {
            // 获取当前索引i，并计算当前标签的填充限制limit，即当前索引加上概率p。
            int i = index.get();
            int limit = i + p;
            while (i < limit) {
                probabilityArray[i++] = label;
            }
            // 更新index为limit，以便在下一次填充时使用新的索引位置。
            index.set(limit);
        });
        return probabilityArray;
    }

    // 标签对应的视频ID是存储在 Redis中的，体育标签: 视频1 视频2 视频3 id
    @Override
    public Collection<Long> listVideoIdByUserModel(User user) {
        // 创建结果集，默认推送10条视频
        Set<Long> videoIds = new HashSet<>(10);

        // 如果是用户，从用户模型中获取
        if (!Objects.isNull(user)) {
            final Long userId = user.getId();
            // 获取用户模型
            final Map<Object, Object> modelMap = redisCacheUtil.hmGet(RedisConstant.USER_MODEL + userId);

            if (!ObjectUtils.isEmpty(modelMap)) {
                // 将用户模型抽象为数组
                final String[] probabilityArray = initProbabilityArray(modelMap);
                final Boolean sex = user.getSex();
                // 获取视频
                final Random randomObject = new Random();
                final ArrayList<String> labelNames = new ArrayList<>();

                if (probabilityArray.length == 0) {
                    return Collections.emptySet();
                }

                // 随机获取X个视频
                for (int i = 0; i < 8; i++) {
                    // 在数组上随机获取标签
                    String labelName = probabilityArray[randomObject.nextInt(probabilityArray.length)];
                    // 封装标签Key添加到列表中
                    labelNames.add(RedisConstant.SYSTEM_STOCK + labelName);
                }

                // 随机获取视频ID列表
                List<Object> list = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String labelName : labelNames) {
                        connection.sRandMember(labelName.getBytes());
                    }
                    return null;
                });

                // 将获取到的视频ID列表进行过滤操作
                Set<Long> ids = list.stream()
                                    .filter(Objects::nonNull)
                                    .map(id -> Long.parseLong(id.toString()))
                                    .collect(Collectors.toSet());

                String key2 = RedisConstant.HISTORY_VIDEO;
                // 去重，推送过的视频就不要再进行推送了
                // 查大量数据中某条数据是否存在 -> 布隆过滤器
                BloomFilter<Long> bloomFilter =
                        BloomFilter.create(Funnels.longFunnel(), 10000, 0.01);

                List historyIds = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (Long id : ids) {
                        connection.get((key2 + id + ":" + userId).getBytes());
                    }
                    return null;
                });

                // 可能为空
                for (Object historyId : historyIds) {
                    if (!ObjectUtils.isEmpty(historyId)) {
                        bloomFilter.put(Long.valueOf(historyId.toString()));
                    }
                }

                Iterator<Long> iterator = ids.iterator();
                while (iterator.hasNext()) {
                    Long id = iterator.next();
                    if (bloomFilter.mightContain(id)) {
                        iterator.remove();
                    }
                }

                videoIds.addAll(ids);
                // 随机挑选一个视频,根据性别: 男：美女 女：宠物
                final Long aLong = randomVideoId(sex);
                if (aLong != null) {
                    videoIds.add(aLong);
                }

                return videoIds;
            }
        }

        // 如果是游客，随机获取10个标签
        final List<String> labels = typeService.random10Labels();
        final ArrayList<String> labelNames = new ArrayList<>();
        int size = labels.size();
        final Random random = new Random();
        // 封装标签Key
        for (int i = 0; i < 10; i++) {
            final int randomIndex = random.nextInt(size);
            labelNames.add(RedisConstant.SYSTEM_STOCK + labels.get(randomIndex));
        }
        // 获取标签下的视频Id列表
        final List<Object> list = redisCacheUtil.sRandom(labelNames);
        // 过滤
        if (!ObjectUtils.isEmpty(list)) {
            videoIds = list.stream()
                           .filter(id -> !ObjectUtils.isEmpty(id))
                           .map(id -> Long.valueOf(id.toString()))
                           .collect(Collectors.toSet());
        }

        return videoIds;
    }

    @Override
    public Collection<Long> listVideoIdByLabels(List<String> labelNames) {
        // 封装系统库标签Key
        final ArrayList<String> labelKeys = new ArrayList<>();
        for (String labelName : labelNames) {
            labelKeys.add(RedisConstant.SYSTEM_STOCK + labelName);
        }

        // 获取标签下的视频ID
        Set<Long> videoIds = new HashSet<>();
        final List<Object> list = redisCacheUtil.sRandom(labelKeys);
        if (!ObjectUtils.isEmpty(list)) {
            videoIds = list.stream()
                           .filter(id -> !ObjectUtils.isEmpty(id))
                           .map(id -> Long.valueOf(id.toString()))
                           .collect(Collectors.toSet());
        }
        return videoIds;
    }
}
