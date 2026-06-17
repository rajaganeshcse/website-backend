package com.portfolio.backend.config;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private static final Set<String> DEFAULT_ALLOWED_ORIGINS = Set.of(
      "https://rajaganeshcse.vercel.app",
      "http://localhost:3000"
  );

  private String mongodbUri = "";
  private String adminUsername = "";
  private String adminPassword = "";
  private String jwtSecret = "";
  private String clientUrl = "";
  private String publicBaseUrl = "";
  private int uploadAutoDeleteHours = 0;
  private int uploadCleanupIntervalMinutes = 5;
  private String cloudinaryCloudName = "";
  private String cloudinaryApiKey = "";
  private String cloudinaryApiSecret = "";
  private String cloudinaryFolder = "portfolio";
  private String localStorePath = "src/data/localStore.json";
  private String mediaPath = "media";

  @PostConstruct
  public void validateSecurityConfig() {
    required("ADMIN_USERNAME", adminUsername);
    required("ADMIN_PASSWORD", adminPassword);
    required("JWT_SECRET", jwtSecret);
    required("CLOUDINARY_CLOUD_NAME", cloudinaryCloudName);
    required("CLOUDINARY_API_KEY", cloudinaryApiKey);
    required("CLOUDINARY_API_SECRET", cloudinaryApiSecret);

    if (uploadAutoDeleteHours < 0) {
      throw new IllegalStateException("UPLOAD_AUTO_DELETE_HOURS must be a number greater than or equal to 0.");
    }

    if (uploadCleanupIntervalMinutes < 0) {
      throw new IllegalStateException("UPLOAD_CLEANUP_INTERVAL_MINUTES must be a number greater than or equal to 0.");
    }
  }

  private void required(String name, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalStateException(name + " is required.");
    }
  }

  public boolean hasMongoUri() {
    return mongodbUri != null && !mongodbUri.trim().isEmpty();
  }

  public Set<String> getAdminUsernames() {
    return Arrays.stream(String.valueOf(adminUsername).split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<String> getAllowedOrigins() {
    LinkedHashSet<String> origins = new LinkedHashSet<>(DEFAULT_ALLOWED_ORIGINS);
    origins.addAll(Arrays.stream(String.valueOf(clientUrl).split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    return origins;
  }

  public String getNormalizedPublicBaseUrl() {
    String value = String.valueOf(publicBaseUrl).trim();
    if (value.isEmpty()) {
      return "";
    }

    try {
      return java.net.URI.create(value).normalize().toString().replaceAll("/+$", "");
    } catch (IllegalArgumentException ignored) {
      return value.replaceAll("/+$", "");
    }
  }

  public String buildCloudinaryFolder(String folder) {
    return Arrays.stream(new String[]{cloudinaryFolder, folder})
        .map(value -> value == null ? "" : value.trim().replaceAll("^/+", "").replaceAll("/+$", ""))
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("/"));
  }

  public Path getBackendRoot() {
    return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
  }

  public Path getLocalStoreAbsolutePath() {
    return getBackendRoot().resolve(localStorePath).normalize();
  }

  public Path getMediaRootAbsolutePath() {
    return getBackendRoot().resolve(mediaPath).normalize();
  }

  public String getMongodbUri() {
    return mongodbUri;
  }

  public void setMongodbUri(String mongodbUri) {
    this.mongodbUri = mongodbUri;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public void setAdminUsername(String adminUsername) {
    this.adminUsername = adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public String getClientUrl() {
    return clientUrl;
  }

  public void setClientUrl(String clientUrl) {
    this.clientUrl = clientUrl;
  }

  public String getPublicBaseUrl() {
    return publicBaseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public int getUploadAutoDeleteHours() {
    return uploadAutoDeleteHours;
  }

  public void setUploadAutoDeleteHours(int uploadAutoDeleteHours) {
    this.uploadAutoDeleteHours = uploadAutoDeleteHours;
  }

  public int getUploadCleanupIntervalMinutes() {
    return uploadCleanupIntervalMinutes;
  }

  public void setUploadCleanupIntervalMinutes(int uploadCleanupIntervalMinutes) {
    this.uploadCleanupIntervalMinutes = uploadCleanupIntervalMinutes;
  }

  public String getCloudinaryCloudName() {
    return cloudinaryCloudName;
  }

  public void setCloudinaryCloudName(String cloudinaryCloudName) {
    this.cloudinaryCloudName = cloudinaryCloudName;
  }

  public String getCloudinaryApiKey() {
    return cloudinaryApiKey;
  }

  public void setCloudinaryApiKey(String cloudinaryApiKey) {
    this.cloudinaryApiKey = cloudinaryApiKey;
  }

  public String getCloudinaryApiSecret() {
    return cloudinaryApiSecret;
  }

  public void setCloudinaryApiSecret(String cloudinaryApiSecret) {
    this.cloudinaryApiSecret = cloudinaryApiSecret;
  }

  public String getCloudinaryFolder() {
    return cloudinaryFolder;
  }

  public void setCloudinaryFolder(String cloudinaryFolder) {
    this.cloudinaryFolder = cloudinaryFolder;
  }

  public String getLocalStorePath() {
    return localStorePath;
  }

  public void setLocalStorePath(String localStorePath) {
    this.localStorePath = localStorePath;
  }

  public String getMediaPath() {
    return mediaPath;
  }

  public void setMediaPath(String mediaPath) {
    this.mediaPath = mediaPath;
  }
}
