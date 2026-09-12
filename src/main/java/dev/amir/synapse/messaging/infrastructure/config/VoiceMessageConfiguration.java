package dev.amir.synapse.messaging.infrastructure.config;

import dev.amir.synapse.messaging.application.model.VoiceMediaCleanupSettings;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(VoiceMessageProperties.class)
public class VoiceMessageConfiguration {
  @Bean("voiceMessageClock")
  Clock voiceMessageClock() {
    return Clock.systemUTC();
  }

  @Bean
  VoiceMediaCleanupSettings voiceMediaCleanupSettings(VoiceMessageProperties properties) {
    return new VoiceMediaCleanupSettings(
        properties.partialCleanupAge(), properties.orphanCleanupAge());
  }
}
