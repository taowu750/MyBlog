package com.ncoxs.myblog.conf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfiguration {

    @Bean
    public GenericJackson2JsonRedisSerializer serializer() {
        // 对值使用 json 序列化
        ObjectMapper mapper = new ObjectMapper();
        // 类型转换
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        // 不序列化空值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
                                                       GenericJackson2JsonRedisSerializer valueSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 对键使用 String 序列化
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // 对值使用 JDK 序列化
//        JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();
        // 对值使用 JSON 序列化

        redisTemplate.setConnectionFactory(factory);
        // 设置 redis 普通条目的序列化方式
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);
        // 设置 redis 哈希条目的序列化方式
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }

    @Bean
    public CacheManager redisCache(RedisConnectionFactory factory,
                                   GenericJackson2JsonRedisSerializer valueSerializer) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(3))
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(valueSerializer))
                                // 这个配置不是自动忽略 null 值，而是输入 null 会报错，
                                // 所以需要在 unless 中进行过滤
                                .disableCachingNullValues())
                .build();
    }
}
