package com.portfolio.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.model.AppItem;
import com.portfolio.backend.model.BaseEntity;
import com.portfolio.backend.model.Certification;
import com.portfolio.backend.model.Education;
import com.portfolio.backend.model.GalleryItem;
import com.portfolio.backend.model.Hero;
import com.portfolio.backend.model.Internship;
import com.portfolio.backend.model.Journal;
import com.portfolio.backend.model.LocalStoreSnapshot;
import com.portfolio.backend.model.MessageItem;
import com.portfolio.backend.model.Project;
import com.portfolio.backend.model.Skill;
import com.portfolio.backend.model.Workshop;
import com.portfolio.backend.util.Defaults;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class LocalStoreService {

  private final AppProperties properties;
  private final ObjectMapper objectMapper;
  private final SerializationService serializationService;

  public LocalStoreService(
      AppProperties properties,
      ObjectMapper objectMapper,
      SerializationService serializationService
  ) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.serializationService = serializationService;
  }

  public synchronized LocalStoreSnapshot readStore() {
    ensureStore();
    try {
      LocalStoreSnapshot store = normalizeStore(objectMapper.readValue(properties.getLocalStoreAbsolutePath().toFile(), LocalStoreSnapshot.class));
      writeStoreIfNeeded(store);
      return store;
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read local store.", exception);
    }
  }

  public synchronized Hero updateHero(UnaryOperator<Hero> updater) {
    LocalStoreSnapshot store = readStore();
    Hero hero = store.hero == null ? defaultHeroWithId() : store.hero;
    Hero updatedHero = updater.apply(hero);
    updatedHero.id = "hero-1";
    store.hero = updatedHero;
    writeStore(store);
    return updatedHero;
  }

  public synchronized <T extends BaseEntity> T createRecord(String collection, String prefix, T record, Class<T> type) {
    LocalStoreSnapshot store = readStore();
    List<T> items = collection(store, collection, type);
    record.id = nextId(items, prefix);
    items.add(record);
    writeStore(store);
    return record;
  }

  public synchronized <T extends BaseEntity> T updateRecord(
      String collection,
      String id,
      Class<T> type,
      UnaryOperator<T> updater
  ) {
    LocalStoreSnapshot store = readStore();
    List<T> items = collection(store, collection, type);
    int index = findIndex(items, id);
    if (index < 0) {
      throw new ApiException(404, "Record not found.");
    }

    T updated = updater.apply(items.get(index));
    updated.id = items.get(index).id;
    items.set(index, updated);
    writeStore(store);
    return updated;
  }

  public synchronized <T extends BaseEntity> T findRecord(String collection, String id, Class<T> type) {
    LocalStoreSnapshot store = readStore();
    List<T> items = collection(store, collection, type);
    return items.stream()
        .filter(item -> String.valueOf(item.id).equals(String.valueOf(id)))
        .findFirst()
        .orElseThrow(() -> new ApiException(404, "Record not found."));
  }

  public synchronized <T extends BaseEntity> void deleteRecord(String collection, String id, Class<T> type) {
    LocalStoreSnapshot store = readStore();
    List<T> items = collection(store, collection, type);
    int index = findIndex(items, id);
    if (index < 0) {
      throw new ApiException(404, "Record not found.");
    }

    items.remove(index);
    writeStore(store);
  }

  public synchronized MessageItem createMessage(MessageItem message) {
    return createRecord("messages", "message", message, MessageItem.class);
  }

  public Map<String, Object> getPortfolioResponse(HttpServletRequest request) {
    LocalStoreSnapshot store = readStore();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("hero", serializationService.serializeHero(store.hero, request));
    response.put("skills", sortSkills(store.skills).stream().map(serializationService::serializeSkill).toList());
    response.put("projects", sortByOrder(store.projects).stream().map(item -> serializationService.serializeProject(item, request)).toList());
    response.put("apps", sortByOrder(store.apps).stream().map(item -> serializationService.serializeApp(item, request)).toList());
    response.put("education", sortByOrder(store.education).stream().map(item -> serializationService.serializeEducation(item, request)).toList());
    response.put("internships", sortByOrder(store.internships).stream().map(serializationService::serializeInternship).toList());
    response.put("certifications", sortByOrder(store.certifications).stream().map(item -> serializationService.serializeCertification(item, request)).toList());
    response.put("workshops", sortByOrder(store.workshops).stream().map(item -> serializationService.serializeWorkshop(item, request)).toList());
    response.put("journals", sortByOrder(store.journals).stream().map(item -> serializationService.serializeJournal(item, request)).toList());
    return response;
  }

  public List<Map<String, Object>> getGalleryResponse(HttpServletRequest request) {
    return sortGallery(readStore().gallery).stream()
        .map(item -> serializationService.serializeGalleryItem(item, request))
        .toList();
  }

  public List<Map<String, Object>> getMessagesResponse() {
    return sortMessages(readStore().messages).stream()
        .map(serializationService::serializeMessage)
        .toList();
  }

  public synchronized void writeStore(LocalStoreSnapshot store) {
    try {
      Files.createDirectories(properties.getLocalStoreAbsolutePath().getParent());
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(properties.getLocalStoreAbsolutePath().toFile(), normalizeStore(store));
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to write local store.", exception);
    }
  }

  private synchronized void ensureStore() {
    Path storePath = properties.getLocalStoreAbsolutePath();
    if (Files.exists(storePath)) {
      return;
    }

    try {
      Files.createDirectories(storePath.getParent());
      ClassPathResource seed = new ClassPathResource("seed/localStore.seed.json");
      if (seed.exists()) {
        try (InputStream inputStream = seed.getInputStream()) {
          Files.copy(inputStream, storePath);
          return;
        }
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to create local store.", exception);
    }

    writeStore(createFallbackStore());
  }

  private LocalStoreSnapshot normalizeStore(LocalStoreSnapshot store) {
    LocalStoreSnapshot normalized = store == null ? new LocalStoreSnapshot() : store;
    normalized.hero = normalized.hero == null ? defaultHeroWithId() : normalized.hero;
    if (normalized.hero.id == null || normalized.hero.id.isBlank()) {
      normalized.hero.id = "hero-1";
    }
    normalized.skills = normalized.skills == null ? new ArrayList<>() : normalized.skills;
    normalized.projects = normalized.projects == null ? new ArrayList<>() : normalized.projects;
    normalized.apps = normalized.apps == null ? new ArrayList<>() : normalized.apps;
    normalized.education = normalized.education == null ? new ArrayList<>() : normalized.education;
    normalized.internships = normalized.internships == null ? new ArrayList<>() : normalized.internships;
    normalized.certifications = normalized.certifications == null ? new ArrayList<>() : normalized.certifications;
    normalized.workshops = normalized.workshops == null ? new ArrayList<>() : normalized.workshops;
    normalized.journals = normalized.journals == null ? new ArrayList<>() : normalized.journals;
    normalized.gallery = normalized.gallery == null ? new ArrayList<>() : normalized.gallery;
    normalized.messages = normalized.messages == null ? new ArrayList<>() : normalized.messages;
    normalized.education.forEach(item -> item.documents = item.documents == null ? new ArrayList<>() : item.documents);
    return normalized;
  }

  private void writeStoreIfNeeded(LocalStoreSnapshot store) {
    if (store.hero == null || store.hero.id == null || store.hero.id.isBlank()) {
      writeStore(store);
    }
  }

  private LocalStoreSnapshot createFallbackStore() {
    LocalStoreSnapshot snapshot = new LocalStoreSnapshot();
    snapshot.hero = defaultHeroWithId();
    return snapshot;
  }

  private Hero defaultHeroWithId() {
    Hero hero = Defaults.defaultHero();
    hero.id = "hero-1";
    return hero;
  }

  private List<Skill> sortSkills(List<Skill> items) {
    return items.stream()
        .sorted(Comparator.comparing((Skill item) -> safe(item.category))
            .thenComparingInt(item -> item.order))
        .toList();
  }

  private List<GalleryItem> sortGallery(List<GalleryItem> items) {
    return items.stream()
        .sorted(Comparator.comparing((GalleryItem item) -> safe(item.category))
            .thenComparingInt(item -> item.order))
        .toList();
  }

  private <T extends BaseEntity> List<T> sortByOrder(List<T> items) {
    return items.stream()
        .sorted(Comparator.comparingInt(this::orderOf))
        .toList();
  }

  private List<MessageItem> sortMessages(List<MessageItem> items) {
    return items.stream()
        .sorted(Comparator.comparing((MessageItem item) -> item.sent_at == null ? 0L : item.sent_at.getTime()).reversed())
        .toList();
  }

  private int orderOf(BaseEntity item) {
    if (item instanceof Skill skill) {
      return skill.order;
    }
    if (item instanceof Project project) {
      return project.order;
    }
    if (item instanceof AppItem appItem) {
      return appItem.order;
    }
    if (item instanceof Education education) {
      return education.order;
    }
    if (item instanceof Internship internship) {
      return internship.order;
    }
    if (item instanceof Certification certification) {
      return certification.order;
    }
    if (item instanceof Workshop workshop) {
      return workshop.order;
    }
    if (item instanceof Journal journal) {
      return journal.order;
    }
    if (item instanceof GalleryItem galleryItem) {
      return galleryItem.order;
    }
    return 0;
  }

  private int findIndex(List<? extends BaseEntity> items, String id) {
    for (int index = 0; index < items.size(); index++) {
      if (String.valueOf(items.get(index).id).equals(String.valueOf(id))) {
        return index;
      }
    }
    return -1;
  }

  private String nextId(List<? extends BaseEntity> items, String prefix) {
    int currentMax = items.stream()
        .map(item -> item.id == null ? "" : item.id)
        .map(value -> value.contains("-") ? value.substring(value.lastIndexOf('-') + 1) : value)
        .mapToInt(value -> {
          try {
            return Integer.parseInt(value);
          } catch (NumberFormatException ignored) {
            return 0;
          }
        })
        .max()
        .orElse(0);
    return prefix + "-" + (currentMax + 1);
  }

  @SuppressWarnings("unchecked")
  private <T extends BaseEntity> List<T> collection(LocalStoreSnapshot store, String collection, Class<T> type) {
    return switch (collection) {
      case "skills" -> (List<T>) store.skills;
      case "projects" -> (List<T>) store.projects;
      case "apps" -> (List<T>) store.apps;
      case "education" -> (List<T>) store.education;
      case "internships" -> (List<T>) store.internships;
      case "certifications" -> (List<T>) store.certifications;
      case "workshops" -> (List<T>) store.workshops;
      case "journals" -> (List<T>) store.journals;
      case "gallery" -> (List<T>) store.gallery;
      case "messages" -> (List<T>) store.messages;
      default -> throw new IllegalArgumentException("Unsupported collection: " + collection);
    };
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
