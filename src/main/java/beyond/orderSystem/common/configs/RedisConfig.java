package beyond.orderSystem.common.configs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    //    application.yml의 spring.redis.host의 정보를 소스코드의 변수로 가져오는 것
    @Value("${spring.redis.host}")
    public String host;

    @Value("${spring.redis.port}")
    public int port;

    @Bean
//    RedisConnectionFactory는 Redis 서버와의 연결을 설정하는 역할
    @Qualifier("2")
    public RedisConnectionFactory redisConnectionFactory(){
        // 이렇게 쓰면 포트가 바뀌거나 host가 바뀌면 작업을 한번 더 해야됨
//        return new LettuceConnectionFactory("localhost", 6379);
//        LettuceConnectionFactory는 RedisConnectionFactory의 구현체로서 실질적인 역할 수행
//        return new LettuceConnectionFactory(host,port);
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        // 1번 db 사용
        configuration.setDatabase(1);
//        configuration.setPassword("1234");
        return new LettuceConnectionFactory(configuration);
    }

    //    redisTemplate은 redis와 상호작용할 때 redis key, value의 형식을 정의
    @Bean
    @Qualifier("2")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("2") RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer()); // json 직렬화 툴 세팅
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

//    redisTemplate.opsForValue().set(key, value)
//    redisTemplate.opsForValue().get(key)
//    redisTemplate.opsForValue().increment or decrement

}