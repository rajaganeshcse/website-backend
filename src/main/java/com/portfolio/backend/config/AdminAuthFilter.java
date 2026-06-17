package com.portfolio.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.util.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AdminAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ObjectMapper objectMapper;

  public AdminAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (!request.getRequestURI().startsWith("/api/admin/")) {
      filterChain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader("Authorization");
    String[] parts = authHeader == null ? new String[0] : authHeader.split(" ", 2);
    if (parts.length != 2 || !"Bearer".equals(parts[0]) || parts[1].isBlank()) {
      writeUnauthorized(response, "Authentication credentials were not provided.");
      return;
    }

    try {
      Map<String, Object> claims = jwtService.verify(parts[1]);
      request.setAttribute("adminClaims", claims);
      filterChain.doFilter(request, response);
    } catch (ApiException exception) {
      writeUnauthorized(response, exception.getMessage());
    }
  }

  private void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    objectMapper.writeValue(response.getWriter(), new LinkedHashMap<>(Map.of("detail", detail)));
  }
}
