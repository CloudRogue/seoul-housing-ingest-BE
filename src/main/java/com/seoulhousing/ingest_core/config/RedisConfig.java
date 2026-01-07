package com.seoulhousing.ingest_core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    //레디스 연결 팩토리를 생성하는 메서드
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    // 문자열 기반으로 쓰기좋은 레디스템플릿 빈으로 등록
    @Bean(name="stringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory); //레디스 연결 주입

        // key 와 value 직렬화해주기
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        // hash field 와 value 직렬화 해주기
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        // 적용완료
        template.afterPropertiesSet();
        return template;
    }

    // 스냅샷을 byte[]로 저장하기 위한 레디스템플릿 빈으로 등록하기
    @Bean(name="redisBytesTemplate")
    public RedisTemplate<String, byte[]> redisBytesTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.byteArray());

        template.afterPropertiesSet();
        return template;
    }
}
