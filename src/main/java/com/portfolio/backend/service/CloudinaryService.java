package com.portfolio.backend.service;

import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.util.CloudinaryAssetReference;
import com.portfolio.backend.util.UploadedAsset;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryService {

  private static final List<String> IMAGE_EXTENSIONS = List.of(
      ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg", ".avif", ".heic", ".heif"
  );

  private final AppProperties properties;
  private final RestTemplate restTemplate = new RestTemplate();

  public CloudinaryService(AppProperties properties) {
    this.properties = properties;
  }

  public UploadedAsset uploadFile(MultipartFile file, String folder) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    try {
      long timestamp = Instant.now().getEpochSecond();
      String originalName = safe(file.getOriginalFilename(), "file");
      String filenameOverride = stripExtension(originalName);
      String cloudinaryFolder = properties.buildCloudinaryFolder(folder);

      Map<String, String> signatureParams = new LinkedHashMap<>();
      if (!cloudinaryFolder.isBlank()) {
        signatureParams.put("folder", cloudinaryFolder);
      }
      signatureParams.put("filename_override", filenameOverride);
      signatureParams.put("overwrite", "false");
      signatureParams.put("timestamp", String.valueOf(timestamp));
      signatureParams.put("unique_filename", "true");
      signatureParams.put("use_filename", "true");

      MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
      form.add("file", new NamedByteArrayResource(file.getBytes(), originalName));
      form.add("api_key", properties.getCloudinaryApiKey());
      form.add("timestamp", String.valueOf(timestamp));
      form.add("signature", sign(signatureParams));
      form.add("use_filename", "true");
      form.add("unique_filename", "true");
      form.add("overwrite", "false");
      form.add("filename_override", filenameOverride);
      if (!cloudinaryFolder.isBlank()) {
        form.add("folder", cloudinaryFolder);
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      @SuppressWarnings("unchecked")
      Map<String, Object> response = restTemplate.postForObject(
          uploadUrl("auto", "upload"),
          new HttpEntity<>(form, headers),
          Map.class
      );

      if (response == null || safe((String) response.get("secure_url"), "").isBlank()) {
        throw new ApiException(500, "Cloudinary upload failed.");
      }

      return new UploadedAsset(
          (String) response.get("secure_url"),
          safe((String) response.get("public_id"), ""),
          safe((String) response.get("resource_type"), ""),
          safe(file.getName(), ""),
          safe(originalName, ""),
          detectTrackableMediaKind(file)
      );
    } catch (IOException exception) {
      throw new ApiException(500, "Failed to read upload.");
    }
  }

  public List<UploadedAsset> uploadFiles(List<MultipartFile> files, String folder) {
    List<UploadedAsset> uploads = new ArrayList<>();

    try {
      for (MultipartFile file : files) {
        UploadedAsset uploadedAsset = uploadFile(file, folder);
        if (uploadedAsset != null && !uploadedAsset.storedPath().isBlank()) {
          uploads.add(uploadedAsset);
        }
      }
      return uploads;
    } catch (Exception exception) {
      for (UploadedAsset uploadedAsset : uploads) {
        try {
          deleteStoredMedia(uploadedAsset.storedPath());
        } catch (Exception ignored) {
        }
      }
      throw exception;
    }
  }

  public boolean deleteStoredMedia(String storedPath) {
    String value = safe(storedPath, "");
    if (value.isBlank()) {
      return false;
    }

    if (value.matches("^https?://.*")) {
      return deleteCloudinaryAsset(value);
    }

    Path absolutePath = resolveStoredAbsolutePath(value);
    if (absolutePath == null) {
      return false;
    }

    try {
      return Files.deleteIfExists(absolutePath);
    } catch (IOException exception) {
      throw new ApiException(500, exception.getMessage());
    }
  }

  public String detectTrackableMediaKind(MultipartFile file) {
    if (file == null) {
      return null;
    }

    String mimeType = safe(file.getContentType(), "").toLowerCase();
    if (mimeType.startsWith("image/")) {
      return "image";
    }
    if ("application/pdf".equals(mimeType)) {
      return "pdf";
    }

    String extension = extensionOf(file.getOriginalFilename()).toLowerCase();
    if (IMAGE_EXTENSIONS.contains(extension)) {
      return "image";
    }
    if (".pdf".equals(extension)) {
      return "pdf";
    }

    return null;
  }

  public CloudinaryAssetReference parseCloudinaryAsset(String value) {
    try {
      URI uri = URI.create(safe(value, ""));
      String host = safe(uri.getHost(), "");
      if (!host.toLowerCase().endsWith("cloudinary.com")) {
        return null;
      }

      List<String> segments = new ArrayList<>();
      for (String segment : uri.getPath().split("/")) {
        if (!segment.isBlank()) {
          segments.add(segment);
        }
      }

      if (segments.size() < 5) {
        return null;
      }

      String resourceType = segments.get(1);
      String deliveryType = segments.get(2);
      List<String> publicIdSegments = new ArrayList<>(segments.subList(3, segments.size()));
      int versionIndex = -1;
      for (int index = 0; index < publicIdSegments.size(); index++) {
        if (publicIdSegments.get(index).matches("^v\\d+$")) {
          versionIndex = index;
          break;
        }
      }

      if (versionIndex >= 0) {
        publicIdSegments = new ArrayList<>(publicIdSegments.subList(versionIndex + 1, publicIdSegments.size()));
      }

      if (publicIdSegments.isEmpty()) {
        return null;
      }

      String lastSegment = decode(publicIdSegments.remove(publicIdSegments.size() - 1));
      String publicId = String.join("/",
          publicIdSegments.stream().map(this::decode).toList()
      );

      String normalizedLastSegment = "raw".equals(resourceType)
          ? lastSegment
          : lastSegment.replaceFirst("\\.[^.]+$", "");

      publicId = publicId.isBlank() ? normalizedLastSegment : publicId + "/" + normalizedLastSegment;
      return publicId.isBlank() ? null : new CloudinaryAssetReference(deliveryType, publicId, resourceType);
    } catch (Exception ignored) {
      return null;
    }
  }

  private boolean deleteCloudinaryAsset(String value) {
    CloudinaryAssetReference asset = parseCloudinaryAsset(value);
    if (asset == null) {
      return false;
    }

    long timestamp = Instant.now().getEpochSecond();
    Map<String, String> signatureParams = new LinkedHashMap<>();
    signatureParams.put("invalidate", "true");
    signatureParams.put("public_id", asset.publicId());
    signatureParams.put("timestamp", String.valueOf(timestamp));
    signatureParams.put("type", asset.deliveryType());

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("public_id", asset.publicId());
    form.add("timestamp", String.valueOf(timestamp));
    form.add("type", asset.deliveryType());
    form.add("invalidate", "true");
    form.add("api_key", properties.getCloudinaryApiKey());
    form.add("signature", sign(signatureParams));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    restTemplate.postForObject(
        uploadUrl(asset.resourceType(), "destroy"),
        new HttpEntity<>(form, headers),
        Map.class
    );
    return true;
  }

  private Path resolveStoredAbsolutePath(String storedPath) {
    String normalizedPath = safe(storedPath, "");
    if (!normalizedPath.startsWith("/media/")) {
      return null;
    }

    Path candidate = properties.getBackendRoot()
        .resolve(normalizedPath.replaceFirst("^/+", "").replace("/", java.io.File.separator))
        .normalize();
    Path mediaRoot = properties.getMediaRootAbsolutePath();

    if (!candidate.startsWith(mediaRoot)) {
      return null;
    }

    return candidate;
  }

  private String uploadUrl(String resourceType, String action) {
    return "https://api.cloudinary.com/v1_1/" + properties.getCloudinaryCloudName() + "/" + resourceType + "/" + action;
  }

  private String sign(Map<String, String> params) {
    String base = params.entrySet().stream()
        .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .reduce((left, right) -> left + "&" + right)
        .orElse("");

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] hash = digest.digest((base + properties.getCloudinaryApiSecret()).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to sign Cloudinary request.", exception);
    }
  }

  private String decode(String value) {
    return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private String stripExtension(String name) {
    String value = safe(name, "file");
    int index = value.lastIndexOf('.');
    return index <= 0 ? value : value.substring(0, index);
  }

  private String extensionOf(String name) {
    String value = safe(name, "");
    int index = value.lastIndexOf('.');
    return index < 0 ? "" : value.substring(index).toLowerCase();
  }

  private String safe(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static final class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    private NamedByteArrayResource(byte[] byteArray, String filename) {
      super(byteArray);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
