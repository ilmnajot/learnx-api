package uz.learnx.learnxapi.common.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(LearnxProperties.class)
public class CoreConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
