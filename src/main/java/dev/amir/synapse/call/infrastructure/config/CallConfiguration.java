package dev.amir.synapse.call.infrastructure.config;

import dev.amir.synapse.call.application.model.CallSettings;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(CallProperties.class)
public class CallConfiguration {
  @Bean
  Clock callClock() {
    return Clock.systemUTC();
  }

  @Bean
  CallSettings callSettings(CallProperties properties) {
    return new CallSettings(
        properties.ringingTimeout(),
        properties.connectionTimeout(),
        properties.recoveryTimeout(),
        properties.livenessLease(),
        properties.timeoutBatchSize());
  }

  @Bean("callServerInstanceId")
  String callServerInstanceId() {
    return UUID.randomUUID().toString();
  }
}
