package uk.ac.ebi.atlas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = "uk.ac.ebi.atlas")
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return RestTemplateFactory.createDefault();
    }
}
