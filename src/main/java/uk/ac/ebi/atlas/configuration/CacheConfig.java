package uk.ac.ebi.atlas.configuration;

import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        SpringCache2kCacheManager cacheManager = new SpringCache2kCacheManager();

        cacheManager.addCaches(
                builder -> builder.name("designElementsByGeneId"),
                builder -> builder.name("arrayDesignByAccession"),
                builder -> builder.name("bioentityProperties"),

                builder -> builder.name("experiment"),
                builder -> builder.name("experimentAttributes"),

                builder -> builder.name("experimentContent"),

                // Used for sitemap.xml files
                builder -> builder.name("publicBioentityIdentifiers"),
                builder -> builder.name("publicSpecies"));
        return cacheManager;
    }
}
