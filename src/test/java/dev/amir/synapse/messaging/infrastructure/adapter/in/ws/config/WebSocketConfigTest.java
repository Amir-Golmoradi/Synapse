package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class WebSocketConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesTestConfig.class);

  private final StompAuthChannelInterceptor authInterceptor =
      mock(StompAuthChannelInterceptor.class);
  private final StompDestinationAuthorizationInterceptor destinationInterceptor =
      mock(StompDestinationAuthorizationInterceptor.class);
  private final SanitizedStompErrorHandler errorHandler = mock(SanitizedStompErrorHandler.class);

  @Test
  void registersNativeEndpointWithSameOriginDefaultAndOrderedReceive() {
    var endpointRegistry = mock(StompEndpointRegistry.class);
    var endpoint = mock(StompWebSocketEndpointRegistration.class);
    when(endpointRegistry.addEndpoint("/ws")).thenReturn(endpoint);
    var config = config(new WebSocketProperties(List.of()));

    config.registerStompEndpoints(endpointRegistry);

    verify(endpointRegistry).addEndpoint("/ws");
    verify(endpointRegistry).setErrorHandler(errorHandler);
    verify(endpointRegistry).setPreserveReceiveOrder(true);
    verify(endpoint, never()).setAllowedOrigins(org.mockito.ArgumentMatchers.any(String[].class));
    verify(endpoint, never())
        .setAllowedOriginPatterns(org.mockito.ArgumentMatchers.any(String[].class));
    verify(endpoint, never()).withSockJS();
  }

  @Test
  void configuresOnlyExplicitOrigins() {
    var endpointRegistry = mock(StompEndpointRegistry.class);
    var endpoint = mock(StompWebSocketEndpointRegistration.class);
    when(endpointRegistry.addEndpoint("/ws")).thenReturn(endpoint);
    var config =
        config(new WebSocketProperties(List.of(" https://app.example ", "https://admin.example")));

    config.registerStompEndpoints(endpointRegistry);

    verify(endpoint).setAllowedOrigins("https://app.example", "https://admin.example");
    verify(endpoint, never())
        .setAllowedOriginPatterns(org.mockito.ArgumentMatchers.any(String[].class));
  }

  @Test
  void rejectsWildcardOrigins() {
    assertThatThrownBy(() -> new WebSocketProperties(List.of("*")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Wildcard WebSocket origins are not allowed");
  }

  @Test
  void configuresTheSimpleBrokerAndOrderedPublication() {
    var brokerRegistry = mock(MessageBrokerRegistry.class);

    config(new WebSocketProperties(List.of())).configureMessageBroker(brokerRegistry);

    verify(brokerRegistry).enableSimpleBroker("/topic", "/queue");
    verify(brokerRegistry).setApplicationDestinationPrefixes("/app");
    verify(brokerRegistry).setUserDestinationPrefix("/user");
    verify(brokerRegistry).setPreservePublishOrder(true);
  }

  @Test
  void registersAuthenticationBeforeDestinationAuthorization() {
    var registration = mock(ChannelRegistration.class);

    config(new WebSocketProperties(List.of())).configureClientInboundChannel(registration);

    verify(registration).interceptors(authInterceptor, destinationInterceptor);
  }

  @Test
  void normalizesBlankAndDuplicateOrigins() {
    assertThat(
            new WebSocketProperties(
                    java.util.Arrays.asList(
                        null, "", " https://app.example ", "https://app.example"))
                .allowedOrigins())
        .containsExactly("https://app.example");
  }

  @Test
  void bindsAnEmptyAllowlistAsSameOriginOnly() {
    contextRunner
        .withPropertyValues("synapse.websocket.allowed-origins=")
        .run(
            context ->
                assertThat(context.getBean(WebSocketProperties.class).allowedOrigins()).isEmpty());
  }

  @Test
  void bindsCommaSeparatedExplicitOrigins() {
    contextRunner
        .withPropertyValues(
            "synapse.websocket.allowed-origins=https://app.example,https://admin.example")
        .run(
            context ->
                assertThat(context.getBean(WebSocketProperties.class).allowedOrigins())
                    .containsExactly("https://app.example", "https://admin.example"));
  }

  private WebSocketConfig config(WebSocketProperties properties) {
    return new WebSocketConfig(authInterceptor, destinationInterceptor, errorHandler, properties);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(WebSocketProperties.class)
  static class PropertiesTestConfig {}
}
