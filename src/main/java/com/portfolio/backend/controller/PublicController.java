package com.portfolio.backend.controller;

import com.mongodb.client.model.Sorts;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.model.AppItem;
import com.portfolio.backend.model.Certification;
import com.portfolio.backend.model.Education;
import com.portfolio.backend.model.GalleryItem;
import com.portfolio.backend.model.Hero;
import com.portfolio.backend.model.Journal;
import com.portfolio.backend.model.MessageItem;
import com.portfolio.backend.model.Project;
import com.portfolio.backend.model.Skill;
import com.portfolio.backend.model.Workshop;
import com.portfolio.backend.service.LocalStoreService;
import com.portfolio.backend.service.MongoStoreService;
import com.portfolio.backend.service.SerializationService;
import com.portfolio.backend.util.Defaults;
import com.portfolio.backend.util.MongoCollections;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PublicController {

  private final MongoStoreService mongoStoreService;
  private final LocalStoreService localStoreService;
  private final SerializationService serializationService;

  public PublicController(
      MongoStoreService mongoStoreService,
      LocalStoreService localStoreService,
      SerializationService serializationService
  ) {
    this.mongoStoreService = mongoStoreService;
    this.localStoreService = localStoreService;
    this.serializationService = serializationService;
  }

  @GetMapping("/health/")
  public Map<String, Object> health() {
    return Map.of(
        "status", "ok",
        "date", Instant.now().toString()
    );
  }

  @GetMapping("/portfolio/")
  public Map<String, Object> portfolio(HttpServletRequest request) {
    if (!mongoStoreService.isDatabaseReady()) {
      return localStoreService.getPortfolioResponse(request);
    }

    Hero hero = getOrCreateHero();
    List<Skill> skills = mongoStoreService.findAll(
        MongoCollections.SKILLS,
        Sorts.orderBy(Sorts.ascending("category"), Sorts.ascending("order"), Sorts.ascending("createdAt")),
        Skill.class
    );
    List<Project> projects = mongoStoreService.findAll(
        MongoCollections.PROJECTS,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        Project.class
    );
    List<AppItem> apps = mongoStoreService.findAll(
        MongoCollections.APPS,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        AppItem.class
    );
    List<Education> education = mongoStoreService.findAll(
        MongoCollections.EDUCATION,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        Education.class
    );
    List<com.portfolio.backend.model.Internship> internships = mongoStoreService.findAll(
        MongoCollections.INTERNSHIPS,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        com.portfolio.backend.model.Internship.class
    );
    List<Certification> certifications = mongoStoreService.findAll(
        MongoCollections.CERTIFICATIONS,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        Certification.class
    );
    List<Workshop> workshops = mongoStoreService.findAll(
        MongoCollections.WORKSHOPS,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        Workshop.class
    );
    List<Journal> journals = mongoStoreService.findAll(
        MongoCollections.JOURNALS,
        Sorts.orderBy(Sorts.ascending("order"), Sorts.descending("createdAt")),
        Journal.class
    );

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("hero", serializationService.serializeHero(hero, request));
    response.put("skills", skills.stream().map(serializationService::serializeSkill).toList());
    response.put("projects", projects.stream().map(item -> serializationService.serializeProject(item, request)).toList());
    response.put("apps", apps.stream().map(item -> serializationService.serializeApp(item, request)).toList());
    response.put("education", education.stream().map(item -> serializationService.serializeEducation(item, request)).toList());
    response.put("internships", internships.stream().map(serializationService::serializeInternship).toList());
    response.put("certifications", certifications.stream().map(item -> serializationService.serializeCertification(item, request)).toList());
    response.put("workshops", workshops.stream().map(item -> serializationService.serializeWorkshop(item, request)).toList());
    response.put("journals", journals.stream().map(item -> serializationService.serializeJournal(item, request)).toList());
    return response;
  }

  @GetMapping("/gallery/")
  public Object gallery(HttpServletRequest request) {
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

  @PostMapping("/contact/")
  public ResponseEntity<?> contact(@RequestBody Map<String, Object> payload) {
    String name = text(payload.get("name"));
    String email = text(payload.get("email"));
    String message = text(payload.get("message"));

    if (name.isBlank() || email.isBlank() || message.isBlank()) {
      throw new ApiException(400, "Name, email, and message are required.");
    }

    if (!mongoStoreService.isDatabaseReady()) {
      MessageItem localMessage = new MessageItem();
      localMessage.name = name;
      localMessage.email = email;
      localMessage.message = message;
      localMessage.sent_at = new Date();
      localMessage.is_read = false;
      localStoreService.createMessage(localMessage);
      return ResponseEntity.status(201).body(Map.of("message", "Sent!"));
    }

    MessageItem created = new MessageItem();
    created.name = name;
    created.email = email;
    created.message = message;
    created.sent_at = new Date();
    created.is_read = false;
    mongoStoreService.save(MongoCollections.MESSAGES, created, MessageItem.class);
    return ResponseEntity.status(201).body(serializationService.serializeMessage(created));
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

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }
}
