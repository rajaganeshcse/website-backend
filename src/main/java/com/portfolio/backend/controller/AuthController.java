package com.portfolio.backend.controller;

import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.util.JwtService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AppProperties properties;
  private final JwtService jwtService;

  public AuthController(AppProperties properties, JwtService jwtService) {
    this.properties = properties;
    this.jwtService = jwtService;
  }

  @PostMapping("/login/")
  public Map<String, Object> login(@RequestBody Map<String, Object> payload) {
    String username = text(payload.get("username"));
    String password = text(payload.get("password"));
    String normalizedUsername = username.toLowerCase();

    if (!properties.getAdminUsernames().contains(normalizedUsername)
        || !password.equals(properties.getAdminPassword())) {
      throw new ApiException(401, "Invalid username or password.");
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("access", jwtService.signAdminToken(username));
    response.put("username", username);
    return response;
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }
}
