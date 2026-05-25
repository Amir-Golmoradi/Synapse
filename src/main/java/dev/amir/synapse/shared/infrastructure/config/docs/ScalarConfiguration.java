package dev.amir.synapse.shared.infrastructure.config.docs;

import com.scalar.maven.core.ScalarProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "scalar", name = "enabled", havingValue = "true")
public class ScalarConfiguration {

  @Bean
  @ConfigurationProperties("scalar")
  public ScalarProperties scalarProperties() {
    return new ScalarProperties();
  }
}
