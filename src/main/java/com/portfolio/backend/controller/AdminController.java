package com.portfolio.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Sorts;
import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.model.AppItem;
import com.portfolio.backend.model.Certification;
import com.portfolio.backend.model.Education;
import com.portfolio.backend.model.EducationDocument;
import com.portfolio.backend.model.GalleryItem;
import com.portfolio.backend.model.Hero;
import com.portfolio.backend.model.Internship;
import com.portfolio.backend.model.Journal;
import com.portfolio.backend.model.MessageItem;
import com.portfolio.backend.model.Project;
import com.portfolio.backend.model.Skill;
import com.portfolio.backend.model.Workshop;
import com.portfolio.backend.service.CloudinaryService;
import com.portfolio.backend.service.LocalStoreService;
import com.portfolio.backend.service.MediaCleanupService;
import com.portfolio.backend.service.MongoStoreService;
import com.portfolio.backend.service.SerializationService;
import com.portfolio.backend.util.Defaults;
import com.portfolio.backend.util.MongoCollections;
import com.portfolio.backend.util.MultipartRequestUtil;
import com.portfolio.backend.util.RequestOriginUtil;
import com.portfolio.backend.util.UploadedAsset;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AppProperties properties;
  private final ObjectMapper objectMapper;
  private final MongoStoreService mongoStoreService;
  private final LocalStoreService localStoreService;
  private final SerializationService serializationService;
  private final CloudinaryService cloudinaryService;
  private final MediaCleanupService mediaCleanupService;

  public AdminController(
      AppProperties properties,
      ObjectMapper objectMapper,
      MongoStoreService mongoStoreService,
      LocalStoreService localStoreService,
      SerializationService serializationService,
      CloudinaryService cloudinaryService,
      MediaCleanupService mediaCleanupService
  ) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.mongoStoreService = mongoStoreService;
    this.localStoreService = localStoreService;
    this.serializationService = serializationService;
    this.cloudinaryService = cloudinaryService;
    this.mediaCleanupService = mediaCleanupService;
  }

  @GetMapping("/messages/")
  public Object messages() {
    if (!mongoStoreService.isDatabaseReady()) {
      return localStoreService.getMessagesResponse();
    }

    List<MessageItem> messages = mongoStoreService.findAll(
        MongoCollections.MESSAGES,
        Sorts.orderBy(Sorts.descending("sent_at"), Sorts.descending("createdAt")),
        MessageItem.class
    );
    return messages.stream().map(serializationService::serializeMessage).toList();
  }

  @GetMapping("/gallery/")
  public Object adminGallery(HttpServletRequest request) {
    if (!mongoStoreService.isDatabaseReady()) {
      return localStoreService.getGalleryResponse(request);
    }

    List<GalleryItem> items = mongoStoreService.findAll(
        MongoCollections.GALLERY,
        Sorts.orderBy(Sorts.ascending("category"), Sorts.ascending("order"), Sorts.descending("createdAt")),
        GalleryItem.class
    );
    return items.stream().map(item -> serializationService.serializeGalleryItem(item, request)).toList();
  }

  @PutMapping(value = "/hero/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateHero(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile photo = MultipartRequestUtil.file(request, "photo");
    MultipartFile siteIcon = MultipartRequestUtil.file(request, "site_icon");
    MultipartFile resume = MultipartRequestUtil.file(request, "resume");

    if (!mongoStoreService.isDatabaseReady()) {
      Hero hero = localStoreService.updateHero(current -> {
        applyHeroFields(current, fields);
        UploadedAsset uploadedPhoto = cloudinaryService.uploadFile(photo, "hero");
        UploadedAsset uploadedSiteIcon = cloudinaryService.uploadFile(siteIcon, "hero");
        UploadedAsset uploadedResume = cloudinaryService.uploadFile(resume, "hero");
        if (uploadedPhoto != null) {
          current.photo_path = uploadedPhoto.storedPath();
        }
        if (uploadedSiteIcon != null) {
          current.site_icon_path = uploadedSiteIcon.storedPath();
        }
        if (uploadedResume != null) {
          current.resume_path = uploadedResume.storedPath();
          current.resume_name = safe(resume == null ? "" : resume.getOriginalFilename());
        }
        return current;
      });

      return serializationService.serializeHero(hero, request);
    }

    Hero hero = getOrCreateHero();
    List<String> previousPaths = List.of(safe(hero.photo_path), safe(hero.site_icon_path), safe(hero.resume_path));
    List<UploadedAsset> uploads = new ArrayList<>();

    applyHeroFields(hero, fields);
    UploadedAsset uploadedPhoto = cloudinaryService.uploadFile(photo, "hero");
    UploadedAsset uploadedSiteIcon = cloudinaryService.uploadFile(siteIcon, "hero");
    UploadedAsset uploadedResume = cloudinaryService.uploadFile(resume, "hero");
    if (uploadedPhoto != null) {
      hero.photo_path = uploadedPhoto.storedPath();
      uploads.add(uploadedPhoto);
    }
    if (uploadedSiteIcon != null) {
      hero.site_icon_path = uploadedSiteIcon.storedPath();
      uploads.add(uploadedSiteIcon);
    }
    if (uploadedResume != null) {
      hero.resume_path = uploadedResume.storedPath();
      hero.resume_name = safe(resume == null ? "" : resume.getOriginalFilename());
      uploads.add(uploadedResume);
    }

    mongoStoreService.save(MongoCollections.HEROES, hero, Hero.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Hero", hero.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, List.of(safe(hero.photo_path), safe(hero.site_icon_path), safe(hero.resume_path)));
    return serializationService.serializeHero(hero, request);
  }

  @PostMapping("/skills/")
  public ResponseEntity<?> createSkill(@RequestBody Map<String, Object> payload) {
    Skill item = new Skill();
    item.category = text(payload.get("category"));
    item.name = text(payload.get("name"));
    item.level = numberOr(payload.get("level"), 80);
    item.order = numberOr(payload.get("order"), 0);

    if (!mongoStoreService.isDatabaseReady()) {
      Skill created = localStoreService.createRecord("skills", "skill", item, Skill.class);
      return ResponseEntity.status(201).body(serializationService.serializeSkill(created));
    }

    mongoStoreService.save(MongoCollections.SKILLS, item, Skill.class);
    return ResponseEntity.status(201).body(serializationService.serializeSkill(item));
  }

  @PutMapping("/skills/{id}/")
  public Object updateSkill(@PathVariable String id, @RequestBody Map<String, Object> payload) {
    if (!mongoStoreService.isDatabaseReady()) {
      Skill updated = localStoreService.updateRecord("skills", id, Skill.class, current -> {
        current.category = payload.containsKey("category") ? text(payload.get("category")) : current.category;
        current.name = payload.containsKey("name") ? text(payload.get("name")) : current.name;
        if (payload.containsKey("level")) {
          current.level = numberOr(payload.get("level"), current.level);
        }
        if (payload.containsKey("order")) {
          current.order = numberOr(payload.get("order"), current.order);
        }
        return current;
      });
      return serializationService.serializeSkill(updated);
    }

    Skill item = findByIdOrThrow(MongoCollections.SKILLS, id, Skill.class, "Skill");
    if (payload.containsKey("category")) {
      item.category = text(payload.get("category"));
    }
    if (payload.containsKey("name")) {
      item.name = text(payload.get("name"));
    }
    if (payload.containsKey("level")) {
      item.level = numberOr(payload.get("level"), item.level);
    }
    if (payload.containsKey("order")) {
      item.order = numberOr(payload.get("order"), item.order);
    }
    mongoStoreService.save(MongoCollections.SKILLS, item, Skill.class);
    return serializationService.serializeSkill(item);
  }

  @DeleteMapping("/skills/{id}/")
  public Map<String, Object> deleteSkill(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("skills", id, Skill.class);
      return Map.of("detail", "Skill deleted.");
    }

    Skill item = findByIdOrThrow(MongoCollections.SKILLS, id, Skill.class, "Skill");
    mongoStoreService.deleteById(MongoCollections.SKILLS, item.id, Skill.class);
    return Map.of("detail", "Skill deleted.");
  }

  @PostMapping(value = "/projects/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createProject(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    List<UploadedAsset> uploads = cloudinaryService.uploadFiles(MultipartRequestUtil.files(request, "image", "image2", "image3"), "projects");

    Project item = new Project();
    item.title = safe(fields.get("title"));
    item.description = safe(fields.get("description"));
    item.tech_stack = safe(fields.get("tech_stack"));
    item.github_url = safe(fields.get("github_url"));
    item.live_url = safe(fields.get("live_url"));
    item.video_url = safe(fields.get("video_url"));
    item.order = numberOr(fields.get("order"), 0);
    item.image_paths = uploads.stream().map(UploadedAsset::storedPath).toList();

    if (!mongoStoreService.isDatabaseReady()) {
      Project created = localStoreService.createRecord("projects", "project", item, Project.class);
      return ResponseEntity.status(201).body(serializationService.serializeProject(created, request));
    }

    mongoStoreService.save(MongoCollections.PROJECTS, item, Project.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Project", item.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeProject(item, request));
  }

  @PutMapping(value = "/projects/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateProject(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    List<UploadedAsset> uploads = cloudinaryService.uploadFiles(MultipartRequestUtil.files(request, "image", "image2", "image3"), "projects");

    if (!mongoStoreService.isDatabaseReady()) {
      Project updated = localStoreService.updateRecord("projects", id, Project.class, current -> {
        current.title = fields.containsKey("title") ? safe(fields.get("title")) : current.title;
        current.description = fields.containsKey("description") ? safe(fields.get("description")) : current.description;
        current.tech_stack = fields.containsKey("tech_stack") ? safe(fields.get("tech_stack")) : current.tech_stack;
        current.github_url = fields.containsKey("github_url") ? safe(fields.get("github_url")) : current.github_url;
        current.live_url = fields.containsKey("live_url") ? safe(fields.get("live_url")) : current.live_url;
        current.video_url = fields.containsKey("video_url") ? safe(fields.get("video_url")) : current.video_url;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        if (MultipartRequestUtil.truthy(fields.get("replace_project_gallery")) && !uploads.isEmpty()) {
          current.image_paths = uploads.stream().map(UploadedAsset::storedPath).toList();
        }
        return current;
      });
      return serializationService.serializeProject(updated, request);
    }

    Project item = findByIdOrThrow(MongoCollections.PROJECTS, id, Project.class, "Project");
    List<String> previousPaths = new ArrayList<>(item.image_paths);
    item.title = fields.containsKey("title") ? safe(fields.get("title")) : item.title;
    item.description = fields.containsKey("description") ? safe(fields.get("description")) : item.description;
    item.tech_stack = fields.containsKey("tech_stack") ? safe(fields.get("tech_stack")) : item.tech_stack;
    item.github_url = fields.containsKey("github_url") ? safe(fields.get("github_url")) : item.github_url;
    item.live_url = fields.containsKey("live_url") ? safe(fields.get("live_url")) : item.live_url;
    item.video_url = fields.containsKey("video_url") ? safe(fields.get("video_url")) : item.video_url;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    if (MultipartRequestUtil.truthy(fields.get("replace_project_gallery")) && !uploads.isEmpty()) {
      item.image_paths = uploads.stream().map(UploadedAsset::storedPath).toList();
    }

    mongoStoreService.save(MongoCollections.PROJECTS, item, Project.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Project", item.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, item.image_paths);
    return serializationService.serializeProject(item, request);
  }

  @DeleteMapping("/projects/{id}/")
  public Map<String, Object> deleteProject(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("projects", id, Project.class);
      return Map.of("detail", "Project deleted.");
    }

    Project item = findByIdOrThrow(MongoCollections.PROJECTS, id, Project.class, "Project");
    mongoStoreService.deleteById(MongoCollections.PROJECTS, item.id, Project.class);
    mediaCleanupService.deleteOwnedMedia("Project", item);
    return Map.of("detail", "Project deleted.");
  }

  @PostMapping(value = "/apps/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createApp(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile cover = MultipartRequestUtil.file(request, "cover_image");
    MultipartFile apk = MultipartRequestUtil.file(request, "apk");
    List<MultipartFile> screenshotFiles = MultipartRequestUtil.files(request, "screenshot1", "screenshot2", "screenshot3");

    UploadedAsset coverUpload = cloudinaryService.uploadFile(cover, "apps");
    UploadedAsset apkUpload = cloudinaryService.uploadFile(apk, "apps");
    List<UploadedAsset> screenshotUploads = cloudinaryService.uploadFiles(screenshotFiles, "apps");
    List<UploadedAsset> uploads = joinUploads(coverUpload, apkUpload, screenshotUploads);

    AppItem item = new AppItem();
    item.title = safe(fields.get("title"));
    item.description = safe(fields.get("description"));
    item.dashboard_url = safe(fields.get("dashboard_url"));
    item.github_url = safe(fields.get("github_url"));
    item.playstore_url = safe(fields.get("playstore_url"));
    item.order = numberOr(fields.get("order"), 0);
    item.cover_image_path = coverUpload == null ? "" : coverUpload.storedPath();
    item.screenshot_paths = screenshotUploads.stream().map(UploadedAsset::storedPath).toList();
    item.apk_path = apkUpload == null ? "" : apkUpload.storedPath();
    item.apk_name = apk == null ? "" : safe(apk.getOriginalFilename());

    if (!mongoStoreService.isDatabaseReady()) {
      AppItem created = localStoreService.createRecord("apps", "app", item, AppItem.class);
      return ResponseEntity.status(201).body(serializationService.serializeApp(created, request));
    }

    mongoStoreService.save(MongoCollections.APPS, item, AppItem.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("AppItem", item.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeApp(item, request));
  }

  @PutMapping(value = "/apps/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateApp(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile cover = MultipartRequestUtil.file(request, "cover_image");
    MultipartFile apk = MultipartRequestUtil.file(request, "apk");
    List<MultipartFile> screenshotFiles = MultipartRequestUtil.files(request, "screenshot1", "screenshot2", "screenshot3");

    UploadedAsset coverUpload = cloudinaryService.uploadFile(cover, "apps");
    UploadedAsset apkUpload = cloudinaryService.uploadFile(apk, "apps");
    List<UploadedAsset> screenshotUploads = cloudinaryService.uploadFiles(screenshotFiles, "apps");
    List<UploadedAsset> uploads = joinUploads(coverUpload, apkUpload, screenshotUploads);

    if (!mongoStoreService.isDatabaseReady()) {
      AppItem updated = localStoreService.updateRecord("apps", id, AppItem.class, current -> {
        current.title = fields.containsKey("title") ? safe(fields.get("title")) : current.title;
        current.description = fields.containsKey("description") ? safe(fields.get("description")) : current.description;
        current.dashboard_url = fields.containsKey("dashboard_url") ? safe(fields.get("dashboard_url")) : current.dashboard_url;
        current.github_url = fields.containsKey("github_url") ? safe(fields.get("github_url")) : current.github_url;
        current.playstore_url = fields.containsKey("playstore_url") ? safe(fields.get("playstore_url")) : current.playstore_url;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        if (coverUpload != null) {
          current.cover_image_path = coverUpload.storedPath();
        }
        if (MultipartRequestUtil.truthy(fields.get("replace_app_screenshots")) && !screenshotUploads.isEmpty()) {
          current.screenshot_paths = screenshotUploads.stream().map(UploadedAsset::storedPath).toList();
        }
        if (apkUpload != null) {
          current.apk_path = apkUpload.storedPath();
          current.apk_name = safe(apk == null ? "" : apk.getOriginalFilename());
        }
        return current;
      });
      return serializationService.serializeApp(updated, request);
    }

    AppItem item = findByIdOrThrow(MongoCollections.APPS, id, AppItem.class, "App");
    List<String> previousPaths = new ArrayList<>();
    previousPaths.add(safe(item.cover_image_path));
    previousPaths.addAll(item.screenshot_paths);
    previousPaths.add(safe(item.apk_path));
    item.title = fields.containsKey("title") ? safe(fields.get("title")) : item.title;
    item.description = fields.containsKey("description") ? safe(fields.get("description")) : item.description;
    item.dashboard_url = fields.containsKey("dashboard_url") ? safe(fields.get("dashboard_url")) : item.dashboard_url;
    item.github_url = fields.containsKey("github_url") ? safe(fields.get("github_url")) : item.github_url;
    item.playstore_url = fields.containsKey("playstore_url") ? safe(fields.get("playstore_url")) : item.playstore_url;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    if (coverUpload != null) {
      item.cover_image_path = coverUpload.storedPath();
    }
    if (MultipartRequestUtil.truthy(fields.get("replace_app_screenshots")) && !screenshotUploads.isEmpty()) {
      item.screenshot_paths = screenshotUploads.stream().map(UploadedAsset::storedPath).toList();
    }
    if (apkUpload != null) {
      item.apk_path = apkUpload.storedPath();
      item.apk_name = safe(apk == null ? "" : apk.getOriginalFilename());
    }

    mongoStoreService.save(MongoCollections.APPS, item, AppItem.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("AppItem", item.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(
        previousPaths,
        joinStrings(List.of(item.cover_image_path, item.apk_path), item.screenshot_paths)
    );
    return serializationService.serializeApp(item, request);
  }

  @DeleteMapping("/apps/{id}/")
  public Map<String, Object> deleteApp(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("apps", id, AppItem.class);
      return Map.of("detail", "App deleted.");
    }

    AppItem item = findByIdOrThrow(MongoCollections.APPS, id, AppItem.class, "App");
    mongoStoreService.deleteById(MongoCollections.APPS, item.id, AppItem.class);
    mediaCleanupService.deleteOwnedMedia("AppItem", item);
    return Map.of("detail", "App deleted.");
  }

  @PostMapping(value = "/education/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createEducation(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    EducationDocumentBuildResult documentsResult = buildEducationDocumentsFromRequest(request, List.of());

    Education item = new Education();
    item.degree = safe(fields.get("degree"));
    item.institution = safe(fields.get("institution"));
    item.year = safe(fields.get("year"));
    item.score = safe(fields.get("score"));
    item.order = numberOr(fields.get("order"), 0);
    applyEducationDocuments(item, documentsResult.documents());

    if (!mongoStoreService.isDatabaseReady()) {
      Education created = localStoreService.createRecord("education", "education", item, Education.class);
      return ResponseEntity.status(201).body(serializationService.serializeEducation(created, request));
    }

    mongoStoreService.save(MongoCollections.EDUCATION, item, Education.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Education", item.id, documentsResult.uploads(), mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeEducation(item, request));
  }

  @PutMapping(value = "/education/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateEducation(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);

    if (!mongoStoreService.isDatabaseReady()) {
      Education currentEducation = localStoreService.findRecord("education", id, Education.class);
      EducationDocumentBuildResult documentsResult = buildEducationDocumentsFromRequest(request, currentEducation.documents);
      Education updated = localStoreService.updateRecord("education", id, Education.class, current -> {
        current.degree = fields.containsKey("degree") ? safe(fields.get("degree")) : current.degree;
        current.institution = fields.containsKey("institution") ? safe(fields.get("institution")) : current.institution;
        current.year = fields.containsKey("year") ? safe(fields.get("year")) : current.year;
        current.score = fields.containsKey("score") ? safe(fields.get("score")) : current.score;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        applyEducationDocuments(current, documentsResult.documents());
        return current;
      });
      return serializationService.serializeEducation(updated, request);
    }

    Education item = findByIdOrThrow(MongoCollections.EDUCATION, id, Education.class, "Education");
    EducationDocumentBuildResult documentsResult = buildEducationDocumentsFromRequest(request, item.documents);
    List<String> previousPaths = new ArrayList<>();
    previousPaths.add(safe(item.result_pdf_path));
    item.documents.forEach(document -> previousPaths.add(safe(document.pdf_path)));
    item.degree = fields.containsKey("degree") ? safe(fields.get("degree")) : item.degree;
    item.institution = fields.containsKey("institution") ? safe(fields.get("institution")) : item.institution;
    item.year = fields.containsKey("year") ? safe(fields.get("year")) : item.year;
    item.score = fields.containsKey("score") ? safe(fields.get("score")) : item.score;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    applyEducationDocuments(item, documentsResult.documents());

    mongoStoreService.save(MongoCollections.EDUCATION, item, Education.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Education", item.id, documentsResult.uploads(), mediaCleanupService.resolveAutoDeleteHours(fields));
    List<String> nextPaths = new ArrayList<>();
    nextPaths.add(safe(item.result_pdf_path));
    item.documents.forEach(document -> nextPaths.add(safe(document.pdf_path)));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, nextPaths);
    return serializationService.serializeEducation(item, request);
  }

  @DeleteMapping("/education/{id}/")
  public Map<String, Object> deleteEducation(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("education", id, Education.class);
      return Map.of("detail", "Education deleted.");
    }

    Education item = findByIdOrThrow(MongoCollections.EDUCATION, id, Education.class, "Education");
    mongoStoreService.deleteById(MongoCollections.EDUCATION, item.id, Education.class);
    mediaCleanupService.deleteOwnedMedia("Education", item);
    return Map.of("detail", "Education deleted.");
  }

  @PostMapping("/internships/")
  public ResponseEntity<?> createInternship(@RequestParam Map<String, String> fields) {
    Internship item = new Internship();
    item.role = safe(fields.get("role"));
    item.company = safe(fields.get("company"));
    item.duration = safe(fields.get("duration"));
    item.description = safe(fields.get("description"));
    item.order = numberOr(fields.get("order"), 0);

    if (!mongoStoreService.isDatabaseReady()) {
      Internship created = localStoreService.createRecord("internships", "internship", item, Internship.class);
      return ResponseEntity.status(201).body(serializationService.serializeInternship(created));
    }

    mongoStoreService.save(MongoCollections.INTERNSHIPS, item, Internship.class);
    return ResponseEntity.status(201).body(serializationService.serializeInternship(item));
  }

  @PutMapping("/internships/{id}/")
  public Object updateInternship(@PathVariable String id, @RequestParam Map<String, String> fields) {
    if (!mongoStoreService.isDatabaseReady()) {
      Internship updated = localStoreService.updateRecord("internships", id, Internship.class, current -> {
        current.role = fields.containsKey("role") ? safe(fields.get("role")) : current.role;
        current.company = fields.containsKey("company") ? safe(fields.get("company")) : current.company;
        current.duration = fields.containsKey("duration") ? safe(fields.get("duration")) : current.duration;
        current.description = fields.containsKey("description") ? safe(fields.get("description")) : current.description;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        return current;
      });
      return serializationService.serializeInternship(updated);
    }

    Internship item = findByIdOrThrow(MongoCollections.INTERNSHIPS, id, Internship.class, "Internship");
    item.role = fields.containsKey("role") ? safe(fields.get("role")) : item.role;
    item.company = fields.containsKey("company") ? safe(fields.get("company")) : item.company;
    item.duration = fields.containsKey("duration") ? safe(fields.get("duration")) : item.duration;
    item.description = fields.containsKey("description") ? safe(fields.get("description")) : item.description;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    mongoStoreService.save(MongoCollections.INTERNSHIPS, item, Internship.class);
    return serializationService.serializeInternship(item);
  }

  @DeleteMapping("/internships/{id}/")
  public Map<String, Object> deleteInternship(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("internships", id, Internship.class);
      return Map.of("detail", "Internship deleted.");
    }

    Internship item = findByIdOrThrow(MongoCollections.INTERNSHIPS, id, Internship.class, "Internship");
    mongoStoreService.deleteById(MongoCollections.INTERNSHIPS, item.id, Internship.class);
    return Map.of("detail", "Internship deleted.");
  }

  @PostMapping(value = "/certs/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createCertification(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile image = MultipartRequestUtil.file(request, "image");
    UploadedAsset imageUpload = cloudinaryService.uploadFile(image, "certs");

    Certification item = new Certification();
    item.name = safe(fields.get("name"));
    item.issuer = safe(fields.get("issuer"));
    item.year = safe(fields.get("year"));
    item.description = safe(fields.get("description"));
    item.credential_url = safe(fields.get("credential_url"));
    item.order = numberOr(fields.get("order"), 0);
    item.image_path = imageUpload == null ? "" : imageUpload.storedPath();

    if (!mongoStoreService.isDatabaseReady()) {
      Certification created = localStoreService.createRecord("certifications", "cert", item, Certification.class);
      return ResponseEntity.status(201).body(serializationService.serializeCertification(created, request));
    }

    mongoStoreService.save(MongoCollections.CERTIFICATIONS, item, Certification.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Certification", item.id, listOf(imageUpload), mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeCertification(item, request));
  }

  @PutMapping(value = "/certs/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateCertification(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile image = MultipartRequestUtil.file(request, "image");
    UploadedAsset imageUpload = cloudinaryService.uploadFile(image, "certs");

    if (!mongoStoreService.isDatabaseReady()) {
      Certification updated = localStoreService.updateRecord("certifications", id, Certification.class, current -> {
        current.name = fields.containsKey("name") ? safe(fields.get("name")) : current.name;
        current.issuer = fields.containsKey("issuer") ? safe(fields.get("issuer")) : current.issuer;
        current.year = fields.containsKey("year") ? safe(fields.get("year")) : current.year;
        current.description = fields.containsKey("description") ? safe(fields.get("description")) : current.description;
        current.credential_url = fields.containsKey("credential_url") ? safe(fields.get("credential_url")) : current.credential_url;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        if (imageUpload != null) {
          current.image_path = imageUpload.storedPath();
        }
        return current;
      });
      return serializationService.serializeCertification(updated, request);
    }

    Certification item = findByIdOrThrow(MongoCollections.CERTIFICATIONS, id, Certification.class, "Certification");
    List<String> previousPaths = List.of(safe(item.image_path));
    item.name = fields.containsKey("name") ? safe(fields.get("name")) : item.name;
    item.issuer = fields.containsKey("issuer") ? safe(fields.get("issuer")) : item.issuer;
    item.year = fields.containsKey("year") ? safe(fields.get("year")) : item.year;
    item.description = fields.containsKey("description") ? safe(fields.get("description")) : item.description;
    item.credential_url = fields.containsKey("credential_url") ? safe(fields.get("credential_url")) : item.credential_url;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    if (imageUpload != null) {
      item.image_path = imageUpload.storedPath();
    }

    mongoStoreService.save(MongoCollections.CERTIFICATIONS, item, Certification.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Certification", item.id, listOf(imageUpload), mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, List.of(safe(item.image_path)));
    return serializationService.serializeCertification(item, request);
  }

  @DeleteMapping("/certs/{id}/")
  public Map<String, Object> deleteCertification(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("certifications", id, Certification.class);
      return Map.of("detail", "Certification deleted.");
    }

    Certification item = findByIdOrThrow(MongoCollections.CERTIFICATIONS, id, Certification.class, "Certification");
    mongoStoreService.deleteById(MongoCollections.CERTIFICATIONS, item.id, Certification.class);
    mediaCleanupService.deleteOwnedMedia("Certification", item);
    return Map.of("detail", "Certification deleted.");
  }

  @PostMapping(value = "/workshops/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createWorkshop(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    List<UploadedAsset> uploads = cloudinaryService.uploadFiles(MultipartRequestUtil.files(request, "image", "image2", "image3"), "workshops");

    Workshop item = new Workshop();
    item.title = safe(fields.get("title"));
    item.organizer = safe(fields.get("organizer"));
    item.date = safe(fields.get("date"));
    item.description = safe(fields.get("description"));
    item.link_url = safe(fields.get("link_url"));
    item.order = numberOr(fields.get("order"), 0);
    item.image_paths = uploads.stream().map(UploadedAsset::storedPath).toList();

    if (!mongoStoreService.isDatabaseReady()) {
      Workshop created = localStoreService.createRecord("workshops", "workshop", item, Workshop.class);
      return ResponseEntity.status(201).body(serializationService.serializeWorkshop(created, request));
    }

    mongoStoreService.save(MongoCollections.WORKSHOPS, item, Workshop.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Workshop", item.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeWorkshop(item, request));
  }

  @PutMapping(value = "/workshops/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateWorkshop(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    UploadedAsset image1 = cloudinaryService.uploadFile(MultipartRequestUtil.file(request, "image"), "workshops");
    UploadedAsset image2 = cloudinaryService.uploadFile(MultipartRequestUtil.file(request, "image2"), "workshops");
    UploadedAsset image3 = cloudinaryService.uploadFile(MultipartRequestUtil.file(request, "image3"), "workshops");
    List<UploadedAsset> uploads = joinUploads(image1, image2, image3);

    if (!mongoStoreService.isDatabaseReady()) {
      Workshop updated = localStoreService.updateRecord("workshops", id, Workshop.class, current -> {
        current.title = fields.containsKey("title") ? safe(fields.get("title")) : current.title;
        current.organizer = fields.containsKey("organizer") ? safe(fields.get("organizer")) : current.organizer;
        current.date = fields.containsKey("date") ? safe(fields.get("date")) : current.date;
        current.description = fields.containsKey("description") ? safe(fields.get("description")) : current.description;
        current.link_url = fields.containsKey("link_url") ? safe(fields.get("link_url")) : current.link_url;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }

        List<String> images = new ArrayList<>(current.image_paths == null ? List.of() : current.image_paths);
        replaceAt(images, 0, image1);
        replaceAt(images, 1, image2);
        replaceAt(images, 2, image3);
        current.image_paths = images.stream().filter(value -> value != null && !value.isBlank()).toList();
        return current;
      });
      return serializationService.serializeWorkshop(updated, request);
    }

    Workshop item = findByIdOrThrow(MongoCollections.WORKSHOPS, id, Workshop.class, "Workshop");
    List<String> previousPaths = new ArrayList<>(item.image_paths);
    item.title = fields.containsKey("title") ? safe(fields.get("title")) : item.title;
    item.organizer = fields.containsKey("organizer") ? safe(fields.get("organizer")) : item.organizer;
    item.date = fields.containsKey("date") ? safe(fields.get("date")) : item.date;
    item.description = fields.containsKey("description") ? safe(fields.get("description")) : item.description;
    item.link_url = fields.containsKey("link_url") ? safe(fields.get("link_url")) : item.link_url;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }

    List<String> images = new ArrayList<>(item.image_paths == null ? List.of() : item.image_paths);
    replaceAt(images, 0, image1);
    replaceAt(images, 1, image2);
    replaceAt(images, 2, image3);
    item.image_paths = images.stream().filter(value -> value != null && !value.isBlank()).toList();

    mongoStoreService.save(MongoCollections.WORKSHOPS, item, Workshop.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Workshop", item.id, uploads, mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, item.image_paths);
    return serializationService.serializeWorkshop(item, request);
  }

  @DeleteMapping("/workshops/{id}/")
  public Map<String, Object> deleteWorkshop(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("workshops", id, Workshop.class);
      return Map.of("detail", "Workshop deleted.");
    }

    Workshop item = findByIdOrThrow(MongoCollections.WORKSHOPS, id, Workshop.class, "Workshop");
    mongoStoreService.deleteById(MongoCollections.WORKSHOPS, item.id, Workshop.class);
    mediaCleanupService.deleteOwnedMedia("Workshop", item);
    return Map.of("detail", "Workshop deleted.");
  }

  @PostMapping(value = "/journals/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createJournal(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile pdf = MultipartRequestUtil.file(request, "pdf");
    UploadedAsset pdfUpload = cloudinaryService.uploadFile(pdf, "journals");

    Journal item = new Journal();
    item.title = safe(fields.get("title"));
    item.details = safe(fields.get("details"));
    item.order = numberOr(fields.get("order"), 0);
    item.pdf_path = pdfUpload == null ? "" : pdfUpload.storedPath();
    item.pdf_name = pdf == null ? "" : safe(pdf.getOriginalFilename());

    if (!mongoStoreService.isDatabaseReady()) {
      Journal created = localStoreService.createRecord("journals", "journal", item, Journal.class);
      return ResponseEntity.status(201).body(serializationService.serializeJournal(created, request));
    }

    mongoStoreService.save(MongoCollections.JOURNALS, item, Journal.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Journal", item.id, listOf(pdfUpload), mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeJournal(item, request));
  }

  @PutMapping(value = "/journals/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateJournal(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile pdf = MultipartRequestUtil.file(request, "pdf");
    UploadedAsset pdfUpload = cloudinaryService.uploadFile(pdf, "journals");

    if (!mongoStoreService.isDatabaseReady()) {
      Journal updated = localStoreService.updateRecord("journals", id, Journal.class, current -> {
        current.title = fields.containsKey("title") ? safe(fields.get("title")) : current.title;
        current.details = fields.containsKey("details") ? safe(fields.get("details")) : current.details;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        if (pdfUpload != null) {
          current.pdf_path = pdfUpload.storedPath();
          current.pdf_name = pdf == null ? current.pdf_name : safe(pdf.getOriginalFilename());
        }
        return current;
      });
      return serializationService.serializeJournal(updated, request);
    }

    Journal item = findByIdOrThrow(MongoCollections.JOURNALS, id, Journal.class, "Journal");
    List<String> previousPaths = List.of(safe(item.pdf_path));
    item.title = fields.containsKey("title") ? safe(fields.get("title")) : item.title;
    item.details = fields.containsKey("details") ? safe(fields.get("details")) : item.details;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    if (pdfUpload != null) {
      item.pdf_path = pdfUpload.storedPath();
      item.pdf_name = pdf == null ? item.pdf_name : safe(pdf.getOriginalFilename());
    }

    mongoStoreService.save(MongoCollections.JOURNALS, item, Journal.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("Journal", item.id, listOf(pdfUpload), mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, List.of(safe(item.pdf_path)));
    return serializationService.serializeJournal(item, request);
  }

  @DeleteMapping("/journals/{id}/")
  public Map<String, Object> deleteJournal(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("journals", id, Journal.class);
      return Map.of("detail", "Journal deleted.");
    }

    Journal item = findByIdOrThrow(MongoCollections.JOURNALS, id, Journal.class, "Journal");
    mongoStoreService.deleteById(MongoCollections.JOURNALS, item.id, Journal.class);
    mediaCleanupService.deleteOwnedMedia("Journal", item);
    return Map.of("detail", "Journal deleted.");
  }

  @PostMapping(value = "/gallery/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createGalleryItem(MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile image = MultipartRequestUtil.file(request, "image");
    UploadedAsset imageUpload = cloudinaryService.uploadFile(image, "gallery");

    GalleryItem item = new GalleryItem();
    item.title = safe(fields.get("title"));
    item.category = safe(fields.get("category"));
    item.caption = safe(fields.get("caption"));
    item.order = numberOr(fields.get("order"), 0);
    item.image_path = imageUpload == null ? "" : imageUpload.storedPath();

    if (!mongoStoreService.isDatabaseReady()) {
      GalleryItem created = localStoreService.createRecord("gallery", "gallery", item, GalleryItem.class);
      return ResponseEntity.status(201).body(serializationService.serializeGalleryItem(created, request));
    }

    mongoStoreService.save(MongoCollections.GALLERY, item, GalleryItem.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("GalleryItem", item.id, listOf(imageUpload), mediaCleanupService.resolveAutoDeleteHours(fields));
    return ResponseEntity.status(201).body(serializationService.serializeGalleryItem(item, request));
  }

  @PutMapping(value = "/gallery/{id}/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Object updateGalleryItem(@PathVariable String id, MultipartHttpServletRequest request) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    MultipartFile image = MultipartRequestUtil.file(request, "image");
    UploadedAsset imageUpload = cloudinaryService.uploadFile(image, "gallery");

    if (!mongoStoreService.isDatabaseReady()) {
      GalleryItem updated = localStoreService.updateRecord("gallery", id, GalleryItem.class, current -> {
        current.title = fields.containsKey("title") ? safe(fields.get("title")) : current.title;
        current.category = fields.containsKey("category") ? safe(fields.get("category")) : current.category;
        current.caption = fields.containsKey("caption") ? safe(fields.get("caption")) : current.caption;
        if (fields.containsKey("order")) {
          current.order = numberOr(fields.get("order"), current.order);
        }
        if (imageUpload != null) {
          current.image_path = imageUpload.storedPath();
        }
        return current;
      });
      return serializationService.serializeGalleryItem(updated, request);
    }

    GalleryItem item = findByIdOrThrow(MongoCollections.GALLERY, id, GalleryItem.class, "Gallery item");
    List<String> previousPaths = List.of(safe(item.image_path));
    item.title = fields.containsKey("title") ? safe(fields.get("title")) : item.title;
    item.category = fields.containsKey("category") ? safe(fields.get("category")) : item.category;
    item.caption = fields.containsKey("caption") ? safe(fields.get("caption")) : item.caption;
    if (fields.containsKey("order")) {
      item.order = numberOr(fields.get("order"), item.order);
    }
    if (imageUpload != null) {
      item.image_path = imageUpload.storedPath();
    }

    mongoStoreService.save(MongoCollections.GALLERY, item, GalleryItem.class);
    mediaCleanupService.scheduleUploadedMediaExpiry("GalleryItem", item.id, listOf(imageUpload), mediaCleanupService.resolveAutoDeleteHours(fields));
    mediaCleanupService.cleanupRemovedStoredPaths(previousPaths, List.of(safe(item.image_path)));
    return serializationService.serializeGalleryItem(item, request);
  }

  @DeleteMapping("/gallery/{id}/")
  public Map<String, Object> deleteGalleryItem(@PathVariable String id) {
    if (!mongoStoreService.isDatabaseReady()) {
      localStoreService.deleteRecord("gallery", id, GalleryItem.class);
      return Map.of("detail", "Gallery item deleted.");
    }

    GalleryItem item = findByIdOrThrow(MongoCollections.GALLERY, id, GalleryItem.class, "Gallery item");
    mongoStoreService.deleteById(MongoCollections.GALLERY, item.id, GalleryItem.class);
    mediaCleanupService.deleteOwnedMedia("GalleryItem", item);
    return Map.of("detail", "Gallery item deleted.");
  }

  private Hero getOrCreateHero() {
    Hero hero = mongoStoreService.findFirst(
        MongoCollections.HEROES,
        null,
        Sorts.ascending("createdAt"),
        Hero.class
    );

    if (hero != null) {
      return hero;
    }

    Hero defaultHero = Defaults.defaultHero();
    return mongoStoreService.save(MongoCollections.HEROES, defaultHero, Hero.class);
  }

  private void applyHeroFields(Hero hero, Map<String, String> fields) {
    if (fields.containsKey("name")) hero.name = safe(fields.get("name"));
    if (fields.containsKey("title")) hero.title = safe(fields.get("title"));
    if (fields.containsKey("bio")) hero.bio = safe(fields.get("bio"));
    if (fields.containsKey("email")) hero.email = safe(fields.get("email"));
    if (fields.containsKey("phone")) hero.phone = safe(fields.get("phone"));
    if (fields.containsKey("github")) hero.github = safe(fields.get("github"));
    if (fields.containsKey("linkedin")) hero.linkedin = safe(fields.get("linkedin"));
    if (fields.containsKey("leetcode")) hero.leetcode = safe(fields.get("leetcode"));
    if (fields.containsKey("instagram")) hero.instagram = safe(fields.get("instagram"));
    if (fields.containsKey("portfolio")) hero.portfolio = safe(fields.get("portfolio"));
    if (fields.containsKey("location")) hero.location = safe(fields.get("location"));
    if (fields.containsKey("college")) hero.college = safe(fields.get("college"));
    if (fields.containsKey("address")) hero.address = safe(fields.get("address"));
  }

  private EducationDocumentBuildResult buildEducationDocumentsFromRequest(
      MultipartHttpServletRequest request,
      List<EducationDocument> currentDocuments
  ) {
    Map<String, String> fields = MultipartRequestUtil.fields(request);
    String documentsJson = fields.get("documents_json");
    List<UploadedAsset> uploads = new ArrayList<>();

    if (documentsJson != null) {
      List<Map<String, Object>> rawDocuments;
      try {
        rawDocuments = objectMapper.readValue(documentsJson, new TypeReference<>() {
        });
      } catch (Exception exception) {
        rawDocuments = List.of();
      }

      List<EducationDocument> documents = new ArrayList<>();
      for (int index = 0; index < rawDocuments.size(); index++) {
        Map<String, Object> document = rawDocuments.get(index);
        String fileField = text(document.get("fileField"));
        MultipartFile uploadedFile = fileField.isBlank() ? null : MultipartRequestUtil.file(request, fileField);
        UploadedAsset upload = uploadedFile == null ? null : cloudinaryService.uploadFile(uploadedFile, "education");
        if (upload != null) {
          uploads.add(upload);
        }
        String pdfPath = upload != null
            ? upload.storedPath()
            : normalizeStoredDocumentPath(request,
                firstNonBlank(
                    text(document.get("pdf_path")),
                    text(document.get("pdf_url")),
                    text(document.get("pdf_download_url"))
                ));
        String pdfName = upload != null
            ? safe(uploadedFile == null ? "" : uploadedFile.getOriginalFilename())
            : text(document.get("pdf_name"));
        String title = text(document.get("title"));
        if (title.isBlank()) {
          title = pdfName.isBlank() ? "Document " + (index + 1) : pdfName;
        }

        if (!pdfPath.isBlank()) {
          EducationDocument educationDocument = new EducationDocument();
          educationDocument.title = title;
          educationDocument.pdf_path = pdfPath;
          educationDocument.pdf_name = pdfName;
          documents.add(educationDocument);
        }
      }
      return new EducationDocumentBuildResult(documents, uploads);
    }

    MultipartFile legacyPdf = MultipartRequestUtil.file(request, "result_pdf");
    if (legacyPdf != null && !legacyPdf.isEmpty()) {
      UploadedAsset upload = cloudinaryService.uploadFile(legacyPdf, "education");
      if (upload != null) {
        uploads.add(upload);
      }
      EducationDocument document = new EducationDocument();
      document.title = "Uploaded PDF";
      document.pdf_path = upload == null ? "" : upload.storedPath();
      document.pdf_name = safe(legacyPdf.getOriginalFilename());
      return new EducationDocumentBuildResult(List.of(document), uploads);
    }

    return new EducationDocumentBuildResult(currentDocuments == null ? List.of() : currentDocuments, uploads);
  }

  private void applyEducationDocuments(Education target, List<EducationDocument> documents) {
    List<EducationDocument> normalizedDocuments = documents == null
        ? new ArrayList<>()
        : documents.stream()
            .filter(document -> !safe(document.pdf_path).isBlank() || !safe(document.title).isBlank())
            .toList();
    target.documents = new ArrayList<>(normalizedDocuments);
    EducationDocument primaryDocument = target.documents.isEmpty() ? null : target.documents.get(0);
    target.result_pdf_path = primaryDocument == null ? "" : safe(primaryDocument.pdf_path);
    target.result_pdf_name = primaryDocument == null ? "" : safe(primaryDocument.pdf_name);
  }

  private String normalizeStoredDocumentPath(HttpServletRequest request, String value) {
    String rawValue = safe(value);
    if (rawValue.isBlank() || rawValue.startsWith("/")) {
      return rawValue;
    }

    try {
      URI uri = URI.create(rawValue);
      if (RequestOriginUtil.normalizeOrigin(uri.getScheme() + "://" + uri.getHost()
          + (uri.getPort() > -1 ? ":" + uri.getPort() : ""))
          .equals(RequestOriginUtil.normalizeOrigin(RequestOriginUtil.requestOrigin(request, properties)))) {
        return safe(uri.getPath());
      }
    } catch (Exception ignored) {
      return rawValue;
    }

    return rawValue;
  }

  private <T> T findByIdOrThrow(String collection, String id, Class<T> type, String label) {
    T item = mongoStoreService.findById(collection, id, type);
    if (item == null) {
      throw new ApiException(404, label + " not found.");
    }
    return item;
  }

  private List<UploadedAsset> listOf(UploadedAsset upload) {
    return upload == null ? List.of() : List.of(upload);
  }

  private List<UploadedAsset> joinUploads(UploadedAsset first, UploadedAsset second, List<UploadedAsset> others) {
    List<UploadedAsset> uploads = new ArrayList<>();
    if (first != null) uploads.add(first);
    if (second != null) uploads.add(second);
    if (others != null) uploads.addAll(others);
    return uploads;
  }

  private List<UploadedAsset> joinUploads(UploadedAsset first, UploadedAsset second, UploadedAsset third) {
    List<UploadedAsset> uploads = new ArrayList<>();
    if (first != null) uploads.add(first);
    if (second != null) uploads.add(second);
    if (third != null) uploads.add(third);
    return uploads;
  }

  private List<String> joinStrings(List<String> primary, List<String> extra) {
    List<String> joined = new ArrayList<>(primary);
    if (extra != null) {
      joined.addAll(extra);
    }
    return joined;
  }

  private void replaceAt(List<String> items, int index, UploadedAsset upload) {
    if (upload == null) {
      return;
    }

    while (items.size() <= index) {
      items.add("");
    }
    items.set(index, upload.storedPath());
  }

  private int numberOr(Object value, int fallback) {
    if (value == null) {
      return fallback;
    }

    try {
      return Integer.parseInt(String.valueOf(value).trim());
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!safe(value).isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private record EducationDocumentBuildResult(
      List<EducationDocument> documents,
      List<UploadedAsset> uploads
  ) {
  }
}
