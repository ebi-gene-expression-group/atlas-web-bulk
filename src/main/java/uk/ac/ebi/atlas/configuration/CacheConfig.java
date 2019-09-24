package uk.ac.ebi.atlas.configuration;

import org.cache2k.configuration.Cache2kConfiguration;
import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

@EnableCaching
@Configuration
public class CacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    private static final long DEFAULT_CACHE_CAPACITY =
            Cache2kConfiguration.of(Object.class, Object.class).getEntryCapacity();
    private Path experimentsDirPath;

    public CacheConfig(Path experimentsDirPath) {
        this.experimentsDirPath = experimentsDirPath;
    }

    @Bean
    public CacheManager cacheManager() {
        return new SpringCache2kCacheManager().addCaches(
                builder -> builder.name("designElementsByGeneId"),
                builder -> builder.name("arrayDesignByAccession"),
                builder -> builder.name("bioentityProperties"),

                builder ->
                        builder.name("experiment")
                                .eternal(true)
                                .entryCapacity(
                                        countExperimentDirectories().map(count ->
                                                Double.valueOf(Math.ceil(1.25 * count)).longValue())
                                                .orElse(DEFAULT_CACHE_CAPACITY)),
                builder -> builder.name("experimentAttributes").eternal(true),
                builder -> builder.name("speciesSummary").eternal(true),

                builder -> builder.name("experimentContent").eternal(true),

                // Used for sitemap.xml files
                builder -> builder.name("publicBioentityIdentifiers").eternal(true),
                builder -> builder.name("publicSpecies").eternal(true));
    }

    private Optional<Long> countExperimentDirectories() {
        try {
            long experimentDirCount = Arrays.stream(experimentsDirPath.resolve("magetab").toFile().listFiles())
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .filter(filename -> filename.startsWith("E-"))
                    .count();
            LOGGER.info("Found {} experiment directories", experimentDirCount);
            return Optional.of(experimentDirCount);
        } catch (Exception e) {
            LOGGER.error("There was an error reading {}", experimentsDirPath.resolve("magetab").toString());
            return Optional.empty();
        }
    }
}
