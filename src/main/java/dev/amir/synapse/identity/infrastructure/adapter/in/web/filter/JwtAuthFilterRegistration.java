package dev.amir.synapse.identity.infrastructure.adapter.in.web.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JwtAuthFilterRegistration {

  @Bean
  FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterServletRegistration(
      JwtAuthFilter jwtAuthFilter) {
    FilterRegistrationBean<JwtAuthFilter> registration =
        new FilterRegistrationBean<>(jwtAuthFilter);
    registration.setEnabled(false);
    return registration;
  }
}
