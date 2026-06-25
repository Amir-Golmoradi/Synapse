package dev.amir.synapse.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
  static final String BEARER_AUTH = "bearerAuth";

  @Bean
  OpenAPI synapseOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Synapse API")
                .version("v1")
                .description("HTTP API for Synapse identity and collaboration services.")
                .contact(new Contact().name("Synapse API Support")))
        .servers(List.of(new Server().url("/").description("Current host")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
