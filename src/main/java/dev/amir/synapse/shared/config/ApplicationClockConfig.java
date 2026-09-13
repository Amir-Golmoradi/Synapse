package dev.amir.synapse.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationClockConfig {
  @Bean
  Clock applicationClock() {
    return Clock.systemUTC();
  }
}
