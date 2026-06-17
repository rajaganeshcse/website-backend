package com.portfolio.backend.util;

import com.portfolio.backend.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;

public final class RequestOriginUtil {

  private RequestOriginUtil() {
  }

  public static String normalizeOrigin(String value) {
    String rawValue = value == null ? "" : value.trim();
    if (rawValue.isEmpty()) {
      return "";
    }

    try {
      return java.net.URI.create(rawValue).normalize().toString().replaceAll("/+$", "").toLowerCase();
    } catch (IllegalArgumentException ignored) {
      return rawValue.replaceAll("/+$", "").toLowerCase();
    }
  }

  public static String requestOrigin(HttpServletRequest request, AppProperties properties) {
    String publicBaseUrl = properties.getNormalizedPublicBaseUrl();
    if (!publicBaseUrl.isEmpty()) {
      return publicBaseUrl;
    }

    return requestProtocol(request) + "://" + requestHost(request);
  }

  public static String requestProtocol(HttpServletRequest request) {
    String forwardedProto = headerPart(request, "x-forwarded-proto");
    if (!forwardedProto.isEmpty()) {
      return forwardedProto;
    }

    return request.getScheme() == null || request.getScheme().isBlank()
        ? "http"
        : request.getScheme();
  }

  public static String requestHost(HttpServletRequest request) {
    String forwardedHost = headerPart(request, "x-forwarded-host");
    if (!forwardedHost.isEmpty()) {
      return forwardedHost;
    }

    String host = request.getHeader("host");
    return host == null ? "" : host.trim();
  }

  private static String headerPart(HttpServletRequest request, String name) {
    String header = request.getHeader(name);
    if (header == null || header.isBlank()) {
      return "";
    }

    return header.split(",")[0].trim();
  }
}
