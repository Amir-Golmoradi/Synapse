package dev.amir.synapse.messaging.infrastructure.config;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(VideoMessageProperties.class)
public class VideoMessageConfiguration {
  @Bean
  VideoMessageSettings videoMessageSettings(VideoMessageProperties properties) {
    return new VideoMessageSettings(
        properties.storageRoot(),
        properties.maxFileSize().toBytes(),
        properties.maxDuration(),
        properties.maxDimension(),
        properties.maxPixels(),
        properties.probeTimeout(),
        properties.stagingGrace(),
        properties.orphanGrace(),
        properties.cleanupBatchSize());
  }
}
