package uk.ac.ebi.atlas.monitoring;

import org.cache2k.Cache;
import org.cache2k.jmx.CacheInfoMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;

public class EnsemblSpeciesCacheMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnsemblSpeciesCacheMonitor.class);
    private static final String CACHE_NAME = "ensemblSpecies";

    private final CacheManager cacheManager;

    public EnsemblSpeciesCacheMonitor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelayString = "${ensembl.cache.stats.log.interval.ms:3600000}")
    public void logCacheStats() {
        var springCache = cacheManager.getCache(CACHE_NAME);
        if (springCache == null) {
            return;
        }

        Object nativeCache = springCache.getNativeCache();
        if (!(nativeCache instanceof Cache)) {
            return;
        }

        CacheInfoMXBean stats = ((Cache<?, ?>) nativeCache).requestInterface(CacheInfoMXBean.class);
        if (stats == null) {
            return;
        }

        LOGGER.info(
                "ensemblSpecies cache: entries={}, capacity={}, hits={}, misses={}, evictions={}",
                stats.getSize(),
                stats.getEntryCapacity(),
                stats.getGetCount() - stats.getMissCount(),
                stats.getMissCount(),
                stats.getEvictedCount());
    }
}
