package com.portfolio.backend.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.model.AppItem;
import com.portfolio.backend.model.BaseEntity;
import com.portfolio.backend.model.Certification;
import com.portfolio.backend.model.Education;
import com.portfolio.backend.model.GalleryItem;
import com.portfolio.backend.model.Hero;
import com.portfolio.backend.model.Journal;
import com.portfolio.backend.model.MediaAsset;
import com.portfolio.backend.model.Project;
import com.portfolio.backend.model.Workshop;
import com.portfolio.backend.util.MongoCollections;
import com.portfolio.backend.util.UploadedAsset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MediaCleanupService {

  private final AppProperties properties;
  private final MongoStoreService mongoStoreService;
  private final CloudinaryService cloudinaryService;
  private final AtomicBoolean cleanupInFlight = new AtomicBoolean(false);

  public MediaCleanupService(
      AppProperties properties,
      MongoStoreService mongoStoreService,
      CloudinaryService cloudinaryService
  ) {
    this.properties = properties;
    this.mongoStoreService = mongoStoreService;
    this.cloudinaryService = cloudinaryService;
  }

  public int resolveAutoDeleteHours(Map<String, String> fields) {
    if (fields.containsKey("auto_delete_hours")) {
      String rawValue = safe(fields.get("auto_delete_hours"));
      if (rawValue.isBlank()) {
        return 0;
      }

      try {
        int parsedValue = Integer.parseInt(rawValue);
        if (parsedValue < 0) {
          throw new NumberFormatException("negative");
        }
        return parsedValue;
      } catch (NumberFormatException exception) {
        throw new ApiException(400, "auto_delete_hours must be a number greater than or equal to 0.");
      }
    }

    return properties.getUploadAutoDeleteHours();
  }

  public void scheduleUploadedMediaExpiry(String ownerModel, String ownerId, List<UploadedAsset> uploads, int autoDeleteHours) {
    if (!mongoStoreService.isDatabaseReady() || autoDeleteHours <= 0 || uploads == null || uploads.isEmpty()) {
      return;
    }

    Date expiresAt = new Date(System.currentTimeMillis() + autoDeleteHours * 60L * 60L * 1000L);

    for (UploadedAsset upload : uploads) {
      if (upload == null || upload.storedPath().isBlank() || upload.mediaKind() == null) {
        continue;
      }

      MediaAsset asset = new MediaAsset();
      asset.owner_model = ownerModel;
      asset.owner_id = ownerId;
      asset.stored_path = upload.storedPath();
      asset.source_field = upload.sourceField();
      asset.original_name = upload.originalName();
      asset.media_kind = upload.mediaKind();
      asset.expires_at = expiresAt;
      asset.deleted_at = null;
      asset.cleanup_status = "scheduled";
      asset.cleanup_error = "";

      mongoStoreService.upsertByFilter(
          MongoCollections.MEDIA_ASSETS,
          Filters.and(
              Filters.eq("owner_model", ownerModel),
              Filters.eq("owner_id", ownerId),
              Filters.eq("stored_path", upload.storedPath())
          ),
          asset,
          MediaAsset.class
      );
    }
  }

  public void cleanupRemovedStoredPaths(List<String> previousPaths, List<String> nextPaths) {
    Set<String> next = uniquePaths(nextPaths);
    for (String storedPath : uniquePaths(previousPaths)) {
      if (!next.contains(storedPath)) {
        try {
          cloudinaryService.deleteStoredMedia(storedPath);
        } catch (Exception ignored) {
        }
      }
    }
  }

  public void deleteOwnedMedia(String ownerModel, BaseEntity item) {
    for (String storedPath : extractStoredPaths(ownerModel, item)) {
      cloudinaryService.deleteStoredMedia(storedPath);
    }

    if (mongoStoreService.isDatabaseReady()) {
      mongoStoreService.deleteMany(
          MongoCollections.MEDIA_ASSETS,
          Filters.and(
              Filters.eq("owner_model", ownerModel),
              Filters.eq("owner_id", item.id)
          )
      );
    }
  }

  @Scheduled(
      fixedDelayString = "#{${app.upload-cleanup-interval-minutes:5} * 60000}",
      initialDelay = 1000L
  )
  public void cleanupExpiredMediaAssets() {
    if (!mongoStoreService.isDatabaseReady() || !cleanupInFlight.compareAndSet(false, true)) {
      return;
    }

    try {
      List<MediaAsset> expiredAssets = mongoStoreService.find(
          MongoCollections.MEDIA_ASSETS,
          Filters.and(
              Filters.in("cleanup_status", List.of("scheduled", "failed")),
              Filters.lte("expires_at", new Date())
          ),
          Sorts.orderBy(Sorts.ascending("expires_at"), Sorts.ascending("createdAt")),
          50,
          MediaAsset.class
      );

      for (MediaAsset asset : expiredAssets) {
        try {
          removeStoredPathReference(asset.owner_model, asset.owner_id, asset.stored_path);
          cloudinaryService.deleteStoredMedia(asset.stored_path);
          asset.deleted_at = new Date();
          asset.cleanup_status = "deleted";
          asset.cleanup_error = "";
        } catch (Exception exception) {
          asset.cleanup_status = "failed";
          asset.cleanup_error = exception.getMessage() == null ? String.valueOf(exception) : exception.getMessage();
        }

        mongoStoreService.save(MongoCollections.MEDIA_ASSETS, asset, MediaAsset.class);
      }
    } finally {
      cleanupInFlight.set(false);
    }
  }

  private void removeStoredPathReference(String ownerModel, String ownerId, String storedPath) {
    switch (ownerModel) {
      case "Hero" -> {
        Hero hero = mongoStoreService.findById(MongoCollections.HEROES, ownerId, Hero.class);
        if (hero == null) {
          return;
        }
        boolean changed = false;
        if (storedPath.equals(safe(hero.photo_path))) {
          hero.photo_path = "";
          changed = true;
        }
        if (storedPath.equals(safe(hero.site_icon_path))) {
          hero.site_icon_path = "";
          changed = true;
        }
        if (storedPath.equals(safe(hero.resume_path))) {
          hero.resume_path = "";
          hero.resume_name = "";
          changed = true;
        }
        if (changed) {
          mongoStoreService.save(MongoCollections.HEROES, hero, Hero.class);
        }
      }
      case "Project" -> {
        Project project = mongoStoreService.findById(MongoCollections.PROJECTS, ownerId, Project.class);
        if (project == null) {
          return;
        }
        List<String> projectImages = project.image_paths == null ? List.of() : project.image_paths;
        List<String> nextImages = projectImages.stream().filter(value -> !storedPath.equals(value)).toList();
        if (nextImages.size() != projectImages.size()) {
          project.image_paths = new ArrayList<>(nextImages);
          mongoStoreService.save(MongoCollections.PROJECTS, project, Project.class);
        }
      }
      case "AppItem" -> {
        AppItem app = mongoStoreService.findById(MongoCollections.APPS, ownerId, AppItem.class);
        if (app == null) {
          return;
        }
        boolean changed = false;
        if (storedPath.equals(safe(app.cover_image_path))) {
          app.cover_image_path = "";
          changed = true;
        }
        List<String> appScreenshots = app.screenshot_paths == null ? List.of() : app.screenshot_paths;
        List<String> nextScreenshots = appScreenshots.stream().filter(value -> !storedPath.equals(value)).toList();
        if (nextScreenshots.size() != appScreenshots.size()) {
          app.screenshot_paths = new ArrayList<>(nextScreenshots);
          changed = true;
        }
        if (storedPath.equals(safe(app.apk_path))) {
          app.apk_path = "";
          app.apk_name = "";
          changed = true;
        }
        if (changed) {
          mongoStoreService.save(MongoCollections.APPS, app, AppItem.class);
        }
      }
      case "Education" -> {
        Education education = mongoStoreService.findById(MongoCollections.EDUCATION, ownerId, Education.class);
        if (education == null) {
          return;
        }
        int originalSize = education.documents.size();
        education.documents = education.documents.stream()
            .filter(document -> !storedPath.equals(safe(document.pdf_path)))
            .toList();
        if (education.documents.size() != originalSize || storedPath.equals(safe(education.result_pdf_path))) {
          education.documents = new ArrayList<>(education.documents);
          if (education.documents.isEmpty()) {
            education.result_pdf_path = "";
            education.result_pdf_name = "";
          } else {
            education.result_pdf_path = safe(education.documents.get(0).pdf_path);
            education.result_pdf_name = safe(education.documents.get(0).pdf_name);
          }
          mongoStoreService.save(MongoCollections.EDUCATION, education, Education.class);
        }
      }
      case "Certification" -> {
        Certification certification = mongoStoreService.findById(MongoCollections.CERTIFICATIONS, ownerId, Certification.class);
        if (certification == null) {
          return;
        }
        if (storedPath.equals(safe(certification.image_path))) {
          certification.image_path = "";
          mongoStoreService.save(MongoCollections.CERTIFICATIONS, certification, Certification.class);
        }
      }
      case "Workshop" -> {
        Workshop workshop = mongoStoreService.findById(MongoCollections.WORKSHOPS, ownerId, Workshop.class);
        if (workshop == null) {
          return;
        }
        List<String> workshopImages = workshop.image_paths == null ? List.of() : workshop.image_paths;
        List<String> nextImages = workshopImages.stream().filter(value -> !storedPath.equals(value)).toList();
        if (nextImages.size() != workshopImages.size()) {
          workshop.image_paths = new ArrayList<>(nextImages);
          mongoStoreService.save(MongoCollections.WORKSHOPS, workshop, Workshop.class);
        }
      }
      case "GalleryItem" -> {
        GalleryItem item = mongoStoreService.findById(MongoCollections.GALLERY, ownerId, GalleryItem.class);
        if (item == null) {
          return;
        }
        if (storedPath.equals(safe(item.image_path))) {
          item.image_path = "";
          mongoStoreService.save(MongoCollections.GALLERY, item, GalleryItem.class);
        }
      }
      case "Journal" -> {
        Journal journal = mongoStoreService.findById(MongoCollections.JOURNALS, ownerId, Journal.class);
        if (journal == null) {
          return;
        }
        if (storedPath.equals(safe(journal.pdf_path))) {
          journal.pdf_path = "";
          journal.pdf_name = "";
          mongoStoreService.save(MongoCollections.JOURNALS, journal, Journal.class);
        }
      }
      default -> {
      }
    }
  }

  private Set<String> extractStoredPaths(String ownerModel, BaseEntity item) {
    return switch (ownerModel) {
      case "Hero" -> uniquePaths(List.of(
          safe(((Hero) item).photo_path),
          safe(((Hero) item).site_icon_path),
          safe(((Hero) item).resume_path)
      ));
      case "Project" -> uniquePaths(((Project) item).image_paths);
      case "AppItem" -> uniquePaths(join(
          List.of(
              safe(((AppItem) item).cover_image_path),
              safe(((AppItem) item).apk_path)
          ),
          ((AppItem) item).screenshot_paths == null ? List.of() : ((AppItem) item).screenshot_paths
      ));
      case "Education" -> {
        Education education = (Education) item;
        List<String> paths = new ArrayList<>();
        paths.add(safe(education.result_pdf_path));
        if (education.documents != null) {
          education.documents.forEach(document -> paths.add(document == null ? "" : safe(document.pdf_path)));
        }
        yield uniquePaths(paths);
      }
      case "Certification" -> uniquePaths(List.of(safe(((Certification) item).image_path)));
      case "Workshop" -> uniquePaths(((Workshop) item).image_paths == null ? List.of() : ((Workshop) item).image_paths);
      case "GalleryItem" -> uniquePaths(List.of(safe(((GalleryItem) item).image_path)));
      case "Journal" -> uniquePaths(List.of(safe(((Journal) item).pdf_path)));
      default -> Set.of();
    };
  }

  private Set<String> uniquePaths(List<String> values) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String value : values) {
      String normalized = safe(value);
      if (!normalized.isBlank()) {
        result.add(normalized);
      }
    }
    return result;
  }

  private List<String> join(List<String> primary, List<String> extra) {
    List<String> joined = new ArrayList<>(primary);
    if (extra != null) {
      joined.addAll(extra);
    }
    return joined;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
