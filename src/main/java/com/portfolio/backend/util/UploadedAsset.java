package com.portfolio.backend.util;

public record UploadedAsset(
    String storedPath,
    String publicId,
    String resourceType,
    String sourceField,
    String originalName,
    String mediaKind
) {
}
