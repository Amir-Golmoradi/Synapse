package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final StompAuthChannelInterceptor authInterceptor;
  private final StompDestinationAuthorizationInterceptor destinationAuthorizationInterceptor;
  private final SanitizedStompErrorHandler errorHandler;
  private final WebSocketProperties properties;

  public WebSocketConfig(
      StompAuthChannelInterceptor authInterceptor,
      StompDestinationAuthorizationInterceptor destinationAuthorizationInterceptor,
      SanitizedStompErrorHandler errorHandler,
      WebSocketProperties properties) {
    this.authInterceptor = authInterceptor;
    this.destinationAuthorizationInterceptor = destinationAuthorizationInterceptor;
    this.errorHandler = errorHandler;
    this.properties = properties;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    var endpoint = registry.addEndpoint("/ws");
    if (!properties.allowedOrigins().isEmpty()) {
      endpoint.setAllowedOrigins(properties.allowedOrigins().toArray(String[]::new));
    }
    registry.setErrorHandler(errorHandler);
    registry.setPreserveReceiveOrder(true);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
    registry.setPreservePublishOrder(true);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(authInterceptor, destinationAuthorizationInterceptor);
  }
}
