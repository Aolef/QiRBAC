package org.zzq.qirbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    /*
     * 创建一个 RedisTemplate，并交给 Spring 管理。
     *
     * RedisTemplate 可以理解成后端操作 Redis 的工具对象，类似前端项目里
     * 封装好的 request/client。其他代码只要注入它，就可以读写 Redis，
     * 不需要每次都自己处理连接、序列化这些细节。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        /*
         * Redis 的 key 使用字符串格式保存。
         *
         * 这样在 Redis 管理工具里看到的 key 会是 token:user:1 这种可读文本，
         * 而不是一串 Java 序列化后的乱码。
         */
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        /*
         * Redis 的 value 使用 JSON 格式保存。
         *
         * ObjectMapper 是 Spring Boot 提供的 JSON 转换器，
         * 这里用它把 Java 对象和 JSON 互相转换。
         */
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

        /*
         * 设置 Redis 连接工厂。
         *
         * 连接工厂会读取 application.yml 里的 Redis 地址、端口等配置，
         * 负责真正连接本地 Redis 服务。
         */
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /*
         * 设置普通 key/value 的保存格式。
         *
         * key 用字符串，value 用 JSON。
         */
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(jsonSerializer);

        /*
         * 设置 hash 类型的 key/value 保存格式。
         *
         * Redis hash 类似一个对象或 Map，例如：
         * user:1 -> { username: "admin", enabled: true }
         */
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        /*
         * 初始化 RedisTemplate。
         *
         * 上面的连接和序列化配置都设置完后，
         * 调用这个方法让配置正式生效。
         */
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}
