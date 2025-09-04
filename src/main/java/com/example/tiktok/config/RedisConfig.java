package com.example.tiktok.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // json 方式存储值
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // @Bean
    // public <T> RedisTemplate<String, T> redisTemplate(RedisConnectionFactory factory) {
    //     RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();
    //
    //     // 连接工厂
    //     redisTemplate.setConnectionFactory(factory);
    //
    //     // key 的序列化
    //     redisTemplate.setKeySerializer(RedisSerializer.string());
    //
    //     // value 的序列化方式
    //     redisTemplate.setValueSerializer(RedisSerializer.json());
    //
    //     // 针对Hash类型的key的序列化方式
    //     redisTemplate.setHashKeySerializer(RedisSerializer.string());
    //
    //     // 针对Hash类型的value的序列化方式
    //     redisTemplate.setHashValueSerializer(RedisSerializer.json());
    //
    //     return redisTemplate;
    // }
}
