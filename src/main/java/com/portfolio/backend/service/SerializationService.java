package com.portfolio.backend.service;

import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.model.AppItem;
import com.portfolio.backend.model.BaseEntity;
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
import com.portfolio.backend.util.RequestOriginUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SerializationService {

  private final AppProperties properties;

  public SerializationService(AppProperties properties) {
    this.properties = properties;
  }

  public Map<String, Object> serializeHero(Hero hero, HttpServletRequest request) {
    if (hero == null) {
      return null;
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(hero));
    response.put("name", hero.name);
    response.put("title", hero.title);
    response.put("bio", hero.bio);
    response.put("email", hero.email);
    response.put("phone", hero.phone);
    response.put("github", hero.github);
    response.put("linkedin", hero.linkedin);
    response.put("leetcode", hero.leetcode);
    response.put("instagram", hero.instagram);
    response.put("portfolio", hero.portfolio);
    response.put("location", hero.location);
    response.put("college", hero.college);
    response.put("address", hero.address);
    response.put("photo_url", absoluteUrl(request, hero.photo_path));
    response.put("site_icon_url", absoluteUrl(request, hero.site_icon_path));
    response.put("resume_url", absoluteUrl(request, hero.resume_path));
    response.put("resume_name", safe(hero.resume_name));
    return response;
  }

  public Map<String, Object> serializeSkill(Skill skill) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(skill));
    response.put("category", skill.category);
    response.put("name", skill.name);
    response.put("level", skill.level);
    response.put("order", skill.order);
    return response;
  }

  public Map<String, Object> serializeProject(Project project, HttpServletRequest request) {
    List<String> imageUrls = nonBlankUrls(request, project.image_paths);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(project));
    response.put("title", project.title);
    response.put("description", project.description);
    response.put("tech_stack", project.tech_stack);
    response.put("tech_list", splitTech(project.tech_stack));
    response.put("github_url", project.github_url);
    response.put("live_url", project.live_url);
    response.put("video_url", project.video_url);
    response.put("image_urls", imageUrls);
    response.put("image_url", imageUrls.size() > 0 ? imageUrls.get(0) : "");
    response.put("image_url2", imageUrls.size() > 1 ? imageUrls.get(1) : "");
    response.put("image_url3", imageUrls.size() > 2 ? imageUrls.get(2) : "");
    response.put("order", project.order);
    return response;
  }

  public Map<String, Object> serializeApp(AppItem app, HttpServletRequest request) {
    List<String> screenshots = nonBlankUrls(request, app.screenshot_paths);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(app));
    response.put("title", app.title);
    response.put("description", app.description);
    response.put("dashboard_url", app.dashboard_url);
    response.put("github_url", app.github_url);
    response.put("playstore_url", app.playstore_url);
    response.put("cover_image_url", absoluteUrl(request, app.cover_image_path));
    response.put("screenshot_urls", screenshots);
    response.put("screenshot_url1", screenshots.size() > 0 ? screenshots.get(0) : "");
    response.put("screenshot_url2", screenshots.size() > 1 ? screenshots.get(1) : "");
    response.put("screenshot_url3", screenshots.size() > 2 ? screenshots.get(2) : "");
    response.put("apk_url", absoluteUrl(request, app.apk_path));
    response.put("apk_name", safe(app.apk_name));
    response.put("order", app.order);
    return response;
  }

  public Map<String, Object> serializeEducation(Education education, HttpServletRequest request) {
    List<Map<String, Object>> documents = new ArrayList<>();
    List<EducationDocument> sourceDocuments = education.documents == null ? List.of() : education.documents;

    for (int index = 0; index < sourceDocuments.size(); index++) {
      EducationDocument document = sourceDocuments.get(index);
      String pdfUrl = absoluteUrl(request, document == null ? "" : document.pdf_path);
      if (pdfUrl.isEmpty()) {
        continue;
      }

      Map<String, Object> serializedDocument = new LinkedHashMap<>();
      serializedDocument.put("id", idOf(education) + "-doc-" + (index + 1));
      serializedDocument.put("title", safe(document.title).isEmpty() ? fallbackDocumentTitle(document, index) : document.title);
      serializedDocument.put("pdf_url", pdfUrl);
      serializedDocument.put("pdf_download_url", pdfUrl);
      serializedDocument.put("pdf_name", safe(document.pdf_name));
      documents.add(serializedDocument);
    }

    String legacyPdfUrl = absoluteUrl(request, education.result_pdf_path);
    Map<String, Object> primaryDocument = documents.isEmpty() ? null : documents.get(0);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(education));
    response.put("degree", education.degree);
    response.put("institution", education.institution);
    response.put("year", education.year);
    response.put("score", education.score);
    response.put("documents", documents);
    response.put("result_pdf_url", primaryDocument == null ? legacyPdfUrl : primaryDocument.get("pdf_url"));
    response.put("result_pdf_download_url", primaryDocument == null ? legacyPdfUrl : primaryDocument.get("pdf_download_url"));
    response.put("result_pdf_name", primaryDocument == null ? safe(education.result_pdf_name) : primaryDocument.get("pdf_name"));
    response.put("order", education.order);
    return response;
  }

  public Map<String, Object> serializeInternship(Internship internship) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(internship));
    response.put("role", internship.role);
    response.put("company", internship.company);
    response.put("duration", internship.duration);
    response.put("description", internship.description);
    response.put("order", internship.order);
    return response;
  }

  public Map<String, Object> serializeCertification(Certification certification, HttpServletRequest request) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(certification));
    response.put("name", certification.name);
    response.put("issuer", certification.issuer);
    response.put("year", certification.year);
    response.put("description", certification.description);
    response.put("credential_url", certification.credential_url);
    response.put("image_url", absoluteUrl(request, certification.image_path));
    response.put("order", certification.order);
    return response;
  }

  public Map<String, Object> serializeWorkshop(Workshop workshop, HttpServletRequest request) {
    List<String> imageUrls = nonBlankUrls(request, workshop.image_paths);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(workshop));
    response.put("title", workshop.title);
    response.put("organizer", workshop.organizer);
    response.put("date", workshop.date);
    response.put("description", workshop.description);
    response.put("link_url", workshop.link_url);
    response.put("image_urls", imageUrls);
    response.put("image_url", imageUrls.size() > 0 ? imageUrls.get(0) : "");
    response.put("image_url2", imageUrls.size() > 1 ? imageUrls.get(1) : "");
    response.put("image_url3", imageUrls.size() > 2 ? imageUrls.get(2) : "");
    response.put("order", workshop.order);
    return response;
  }

  public Map<String, Object> serializeGalleryItem(GalleryItem item, HttpServletRequest request) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(item));
    response.put("title", item.title);
    response.put("category", item.category);
    response.put("caption", item.caption);
    response.put("order", item.order);
    response.put("image_url", absoluteUrl(request, item.image_path));
    return response;
  }

  public Map<String, Object> serializeJournal(Journal journal, HttpServletRequest request) {
    String pdfUrl = absoluteUrl(request, journal.pdf_path);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(journal));
    response.put("title", journal.title);
    response.put("details", safe(journal.details));
    response.put("pdf_url", pdfUrl);
    response.put("pdf_download_url", pdfUrl);
    response.put("pdf_name", safe(journal.pdf_name));
    response.put("order", journal.order);
    return response;
  }

  public Map<String, Object> serializeMessage(MessageItem message) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", idOf(message));
    response.put("name", message.name);
    response.put("email", message.email);
    response.put("message", message.message);
    response.put("sent_at", message.sent_at == null ? message.createdAt : message.sent_at);
    response.put("is_read", message.is_read);
    return response;
  }

  public String absoluteUrl(HttpServletRequest request, String storedPath) {
    String value = safe(storedPath);
    if (value.isEmpty()) {
      return "";
    }

    if (value.matches("^https?://.*")) {
      return value;
    }

    return RequestOriginUtil.requestOrigin(request, properties) + value;
  }

  private List<String> splitTech(String value) {
    return java.util.Arrays.stream(safe(value).split(","))
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .toList();
  }

  private List<String> nonBlankUrls(HttpServletRequest request, List<String> storedPaths) {
    if (storedPaths == null) {
      return List.of();
    }

    return storedPaths.stream()
        .map(path -> absoluteUrl(request, path))
        .filter(url -> !url.isEmpty())
        .toList();
  }

  private String fallbackDocumentTitle(EducationDocument document, int index) {
    String pdfName = document == null ? "" : safe(document.pdf_name);
    return pdfName.isEmpty() ? "Document " + (index + 1) : pdfName;
  }

  private String idOf(BaseEntity entity) {
    return entity == null || entity.id == null ? "" : entity.id;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
