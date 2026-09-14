package dev.amir.synapse.call.infrastructure.adapter.in.web.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CallRequestCorrelationFilter extends OncePerRequestFilter {
  public static final String HEADER = "X-Request-ID";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/v1/calls");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var requestId = requestId(request.getHeader(HEADER));
    response.setHeader(HEADER, requestId);
    try (var ignored = MDC.putCloseable("requestId", requestId)) {
      filterChain.doFilter(request, response);
    }
  }

  private static String requestId(String candidate) {
    if (candidate != null) {
      try {
        return UUID.fromString(candidate).toString();
      } catch (IllegalArgumentException ignored) {
        // Replace malformed untrusted correlation identifiers.
      }
    }
    return UUID.randomUUID().toString();
  }
}
