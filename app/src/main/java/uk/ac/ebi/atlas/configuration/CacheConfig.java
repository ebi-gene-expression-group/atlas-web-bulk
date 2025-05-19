package uk.ac.ebi.atlas.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@EnableCaching
@Configuration
public class CacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    private final Path experimentsDirPath;

    public CacheConfig(Path experimentsDirPath) {
        this.experimentsDirPath = experimentsDirPath;
    }


    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new JedisConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public CacheManager cacheManager(JedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(60)); // Default TTL (can be overridden per cache)

        Map<String, RedisCacheConfiguration> cacheConfigs = new LinkedHashMap<>();

        cacheConfigs.put("designElementsByGeneId", defaultConfig);
        cacheConfigs.put("arrayDesignByAccession", defaultConfig);
        cacheConfigs.put("bioentityProperties", defaultConfig);
        cacheConfigs.put("experiment", defaultConfig.entryTtl(Duration.ZERO));
        cacheConfigs.put("experimentAttributes", defaultConfig.entryTtl(Duration.ZERO));
        cacheConfigs.put("speciesSummary", defaultConfig.entryTtl(Duration.ZERO));
        cacheConfigs.put("experimentCollection", defaultConfig);
        cacheConfigs.put("experiment2Collections", defaultConfig);
        cacheConfigs.put("experimentContent", defaultConfig.entryTtl(Duration.ZERO));
        cacheConfigs.put("publicBioentityIdentifiers", defaultConfig.entryTtl(Duration.ZERO));
        cacheConfigs.put("publicSpecies", defaultConfig.entryTtl(Duration.ZERO));
        cacheConfigs.put("baselineExperimentData", defaultConfig.entryTtl(Duration.ZERO));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

}
