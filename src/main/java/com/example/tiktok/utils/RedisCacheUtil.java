package com.example.tiktok.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component(value="redisCacheUtil")
public class RedisCacheUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加有序集合
     * @param key
     * @param score
     * @param val
     * @param time
     */
    public void zAdd(String key, double score, Object val, long time) {
        redisTemplate.opsForZSet()
                     .add(key, val, score);
        this.expire(key, time);
    }

    /**
     * 对 zSet中的元素进行加减分值操作，如果元素不存在，新建一个
     * @param key
     * @param value
     * @param delta
     */
    public void zIncrementScore(String key, Object value, double delta) {
        redisTemplate.opsForZSet()
                     .incrementScore(key, value, delta);
    }

    /**
     * 取出所有有序集合，进行降序排序
     * @param key
     * @return
     */
    public Set<ZSetOperations.TypedTuple<Object>> getZSet(String key) {
        return redisTemplate.opsForZSet()
                            .rangeWithScores(key, 0, -1);
    }

    /**
     * 获取指定key的有序集合中的元素,降序返回
     * @param key
     * @return
     */
    public Set zGet(String key) {
        return redisTemplate.opsForZSet()
                            .reverseRange(key, 0, -1);
    }


    public Set<ZSetOperations.TypedTuple<Object>> zSetGetByPage(String key, long pageNum, long pageSize) {
        try {
            if (redisTemplate.hasKey(key)) {
                long start = (pageNum - 1) * pageSize;
                long end = pageNum * pageSize - 1;
                Long size = redisTemplate.opsForZSet().size(key);
                if (end > size) {
                    end = -1;
                }

                return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取指定键 key 的有序集合中的元素
     * @param key
     * @return
     */
    public Set<Object> getZSetObject(String key) {
        return redisTemplate.opsForZSet().range(key, 0, -1);
    }

    /**
     * 增量
     * @param key
     * @param object
     * @param score
     */
    public void increment(String key, Object object, double score) {
        redisTemplate.opsForZSet()
                     .incrementScore(key, object, score);
    }

    /**
     * 指定缓存失效时间
     * @param key  键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     * @param key 键 非空
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return 存在 | 不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     * @param key 可以传单个值或多个值
     */
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null
               ? null
               : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存存储
     * @param key   键
     * @param value 值
     * @return 成功 | 失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存存储并设置过期时间
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 则视为永久存储
     * @return 成功 | 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue()
                             .set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 递增
     * @param key   键
     * @param delta 要增加多少(大于0)
     * @return
     */
    public Long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue()
                            .increment(key, delta);
    }

    /**
     * 递减
     * @param key   键
     * @param delta 要减少多少(小于0)
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue()
                            .increment(key, -delta);
    }

    /**
     * HashGet
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hGet(String key, String item) {
        return redisTemplate.opsForHash()
                            .get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmGet(String key) {
        return redisTemplate.opsForHash()
                            .entries(key);
    }

    /**
     * HashSet
     * @param key 键
     * @param map 对应多个键值
     * @return 成功 | 失败
     */
    public boolean hmSet(String key, Map<Object, Object> map) {
        try {
            redisTemplate.opsForHash()
                         .putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置过期时间
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return 成功 | 失败
     */
    public boolean hmSet(String key, Map<Object, Object> map, long time) {
        try {
            redisTemplate.opsForHash()
                         .putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向Hash表中放入数据，如果不存在则创建
     * @param key   键
     * @param item  项
     * @param value 值
     * @return 成功 | 失败
     */
    public boolean hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash()
                         .put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向Hash表中放入数据，如果不存在则创建
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 如果hash表中已设置过期时间，这里会重新设置hash表的过期时间
     * @return 成功 | 失败
     */
    public boolean hSet(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash()
                         .put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     * @param key  键 非Null
     * @param item 项 可以设置多个 非Null
     */
    public void hDel(String key, Object... item) {
        redisTemplate.opsForHash()
                     .delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     * @param key  键 非Null
     * @param item 项 可以设置多个 非Null
     * @return 成功 | 失败
     */
    public boolean hHashKey(String key, String item) {
        return redisTemplate.opsForHash()
                            .hasKey(key, item);
    }

    /**
     * hash递增，如果不存在则会新建，并将新建后的值返回
     * @param key  键
     * @param item 项
     * @param by   要增加多少(大于0)
     * @return
     */
    public double hIncr(String key, String item, double by) {
        return redisTemplate.opsForHash()
                            .increment(key, item, by);
    }

    /**
     * hash递减，如果不存在则会新建，并将新建后的值返回
     * @param key  键
     * @param item 项
     * @param by   要减少多少(大于0)
     * @return
     */
    public double hDecr(String key, String item, double by) {
        return redisTemplate.opsForHash()
                            .increment(key, item, -by);
    }

    /**
     * 根据key获取Set中的所有值
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet()
                                .members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据Value从Set中查询，是否存在
     * @param key   键
     * @param value 值
     * @return 成功 | 失败
     */
    public boolean sHashKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet()
                                .isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入Set缓存中
     * @param key    键
     * @param values 值 可以为多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet()
                                .add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将Set数据放入缓存中
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以为多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet()
                                      .add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取Set缓存的长度
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet()
                                .size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除Set中值为value项
     * @param key    键
     * @param values 值 可以为多个
     * @return 移除的数量
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet()
                                      .remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取list缓存的内容
     * @param key   键
     * @param start 开始
     * @param end   结束 0 到 -1代表所有值
     * @return
     */
    public <T> List<T> lGet(String key, long start, long end) {
        List<Object> list = redisTemplate.opsForList().range(key, start, end);
        List<T> res = new ArrayList<>();
        for (Object o : list) {
            res.add((T) o);
        }
        try {
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取List中的元素个数
     * @param key 键
     * @return
     */
    public long lSize(String key) {
        return redisTemplate.opsForList()
                            .size(key);
    }

    public Object sRandom(String key) {
        return redisTemplate.opsForSet()
                            .randomMember(key);
    }

    public List<Object> sRandom(List<String> keys) {
        final List<Object> list = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (String key : keys) {
                    connection.sRandMember(key.getBytes());
                }
                return null;
            }
        });
        // 可能会有null
        final List result = new ArrayList();
        for (Object aLong : list) {
            if (!Objects.isNull(aLong)) {
                result.add(aLong);
            }
        }
        return result;
    }

    /**
     * 随机Key中随机拿数据
     * @param map
     * @return
     */
    public List<Object> lGetIndex(Map<String, Long> map) {
        final List<Object> list = redisTemplate.executePipelined((RedisCallback<Long>) connection -> {
            map.forEach((k, v) -> {
                connection.lIndex(k.getBytes(), v);
            });
            return null;
        });
        return list;
    }

    public List<Object> lSize(List<String> keys) {
        final List<Object> list = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.lLen(key.getBytes());
            }
            return null;
        });
        return list;
    }

    /**
     * 获取List缓存的长度
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取队列头元素并删除
     * @param key 键
     * @return
     */
    public Object lPopFirst(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 插入新元素到队列尾
     * @param key   键
     * @param value 值
     */
    public void lPushRight(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 插入新元素到队列头
     * @param key   键
     * @param value 值
     */
    public void lPushLeft(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 通过索引来获取List中的值
     * @param key   键
     * @param index index 索引 index >=0 时正向; index < 0 时反向
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 插入元素到List的队尾
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 插入元素到List的队尾，设置过期时间
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 插入多个元素到List的队尾
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 插入多个元素到List的队尾，设置过期时间
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     * @param key   键
     * @param count 移除多少个 count > 0 正向 | count < 0 反向 | count = 0 移除所有匹配的元素
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 返回两个 Redis 集合的差集
     * @param key
     * @param otherKey
     * @return
     */
    public Set<Object> sDiff(String key, String otherKey) {
        return redisTemplate.opsForSet().difference(key, otherKey);
    }

    // 接收一个管道
    public List pipeline(RedisCallback redisCallback) {
        return redisTemplate.executePipelined(redisCallback);
    }
}
