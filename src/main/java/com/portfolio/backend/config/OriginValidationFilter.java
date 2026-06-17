package com.portfolio.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backend.util.RequestOriginUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OriginValidationFilter extends OncePerRequestFilter {

  private final AppProperties properties;
  private final ObjectMapper objectMapper;

  public OriginValidationFilter(AppProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String origin = request.getHeader("Origin");
    if (origin == null || origin.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    Set<String> allowedOrigins = properties.getAllowedOrigins().stream()
        .map(RequestOriginUtil::normalizeOrigin)
        .collect(Collectors.toSet());

    if (!allowedOrigins.contains(RequestOriginUtil.normalizeOrigin(origin))) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      objectMapper.writeValue(response.getWriter(), new LinkedHashMap<>(java.util.Map.of("detail", "CORS not allowed")));
      return;
    }

    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Vary", "Origin");
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
    response.setHeader(
        "Access-Control-Allow-Headers",
        headerOrDefault(request, "Access-Control-Request-Headers", "Authorization,Content-Type")
    );

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String headerOrDefault(HttpServletRequest request, String name, String fallback) {
    String value = request.getHeader(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
