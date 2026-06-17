package com.portfolio.backend.util;

public record CloudinaryAssetReference(
    String deliveryType,
    String publicId,
    String resourceType
) {
}
