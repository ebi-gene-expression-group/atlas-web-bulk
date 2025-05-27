package uk.ac.ebi.atlas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.atlas.experiments.ExperimentCellCountDao;

@Configuration
// Enabling component scanning will also load BasePathsConfig, JdbcConfig and SolrConfig, so just using this class as
// application context is enough in integration tests. It’s important to exclude CacheConfig, otherwise Spring will
// complain if you want to inject classes such as GxaExperimentTrader, since a proxy will be injected instead! As an
// exercise, remove CacheConfig.class and run tests in GxaExperimentTraderIT.
@ComponentScan(basePackages = "uk.ac.ebi.atlas",
               includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                                        value = TestJdbcConfig.class),
               excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                                        value = {JdbcConfig.class, AppConfig.class, CacheConfig.class}))
public class TestConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ExperimentCellCountDao experimentCellCountDao() {
        return __ -> 0;
    }
}
