package com.nhnacademy.book_data_batch.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // RedisConnectionFactory 설정: LettuceConnectionFactory 사용
        // LettuceConnectionFactory는 Redis와의 연결을 관리하는 팩토리
        // RedisTemplate에 연결 팩토리를 설정하여 Redis 서버와 통신할 수 있도록 함
        // Lettuce는 비동기 및 반응형 Redis 클라이언트 라이브러리로, 성능이 우수함
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 직렬화 설정: Key와 HashKey는 String으로, Value와 HashValue는 JSON으로 직렬화
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
