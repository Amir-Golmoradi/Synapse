package dev.amir.synapse.shared.websocket.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final StompAuthChannelInterceptor authInterceptor;
  private final StompDestinationAuthorizationInterceptor authorizationInterceptor;
  private final SanitizedStompErrorHandler errorHandler;
  private final WebSocketProperties properties;
  private TaskScheduler taskScheduler;

  public WebSocketConfig(
      StompAuthChannelInterceptor authInterceptor,
      StompDestinationAuthorizationInterceptor authorizationInterceptor,
      SanitizedStompErrorHandler errorHandler,
      WebSocketProperties properties) {
    this.authInterceptor = authInterceptor;
    this.authorizationInterceptor = authorizationInterceptor;
    this.errorHandler = errorHandler;
    this.properties = properties;
  }

  @Autowired
  public void setTaskScheduler(
      @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
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
    registry
        .enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[] {10_000, 10_000})
        .setTaskScheduler(taskScheduler);
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
    registry.setPreservePublishOrder(true);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(authInterceptor, authorizationInterceptor);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.setMessageSizeLimit(64 * 1024);
    registration.setSendBufferSizeLimit(512 * 1024);
    registration.setSendTimeLimit(15_000);
  }
}
