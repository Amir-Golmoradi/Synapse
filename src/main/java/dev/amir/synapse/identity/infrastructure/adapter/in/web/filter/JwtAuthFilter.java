package dev.amir.synapse.identity.infrastructure.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

@Service
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtAdapter jwtAdapter;

  public JwtAuthFilter(JwtAdapter jwtAdapter) {
    this.jwtAdapter = jwtAdapter;
  }

  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (Objects.isNull(authHeader) || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwt = authHeader.substring(7);
    final String subject = jwtAdapter.getSubject(jwt);

    if (SecurityContextHolder.getContext().getAuthentication() == null
        && jwtAdapter.isTokenValid(jwt, subject)) {
      var usernamePasswordAuthToken =
          new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
      usernamePasswordAuthToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthToken);
    }

    filterChain.doFilter(request, response);
  }
}
