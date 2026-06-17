package com.portfolio.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
  private static final long ADMIN_TOKEN_TTL_SECONDS = 7L * 24L * 60L * 60L;

  private final AppProperties properties;
  private final ObjectMapper objectMapper;

  public JwtService(AppProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public String signAdminToken(String username) {
    try {
      Map<String, Object> header = Map.of(
          "alg", "HS256",
          "typ", "JWT"
      );

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("username", username);
      payload.put("role", "admin");
      payload.put("exp", Instant.now().getEpochSecond() + ADMIN_TOKEN_TTL_SECONDS);

      String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
      String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
      String unsignedToken = encodedHeader + "." + encodedPayload;

      return unsignedToken + "." + sign(unsignedToken);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to sign JWT.", exception);
    }
  }

  public Map<String, Object> verify(String token) {
    try {
      String[] parts = String.valueOf(token).split("\\.");
      if (parts.length != 3) {
        throw new ApiException(401, "Invalid or expired token.");
      }

      String unsignedToken = parts[0] + "." + parts[1];
      String expectedSignature = sign(unsignedToken);
      if (!MessageDigest.isEqual(
          expectedSignature.getBytes(StandardCharsets.UTF_8),
          parts[2].getBytes(StandardCharsets.UTF_8))) {
        throw new ApiException(401, "Invalid or expired token.");
      }

      Map<String, Object> payload = objectMapper.readValue(
          URL_DECODER.decode(parts[1]),
          new TypeReference<>() {
          }
      );

      Number exp = (Number) payload.get("exp");
      if (exp == null || exp.longValue() <= Instant.now().getEpochSecond()) {
        throw new ApiException(401, "Invalid or expired token.");
      }

      return payload;
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ApiException(401, "Invalid or expired token.");
    }
  }

  private String sign(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }

  private String encode(byte[] value) {
    return URL_ENCODER.encodeToString(value);
  }
}
