package com.portfolio.backend.service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.portfolio.backend.config.AppProperties;
import com.portfolio.backend.exception.ApiException;
import com.portfolio.backend.model.BaseEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Service
public class MongoStoreService {

  private static final Logger logger = LoggerFactory.getLogger(MongoStoreService.class);

  private final AppProperties properties;
  private final AtomicBoolean databaseReady = new AtomicBoolean(false);

  private MongoClient mongoClient;
  private MongoDatabase database;

  public MongoStoreService(AppProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  public void initialize() {
    if (!properties.hasMongoUri()) {
      return;
    }

    ConnectionString connectionString = new ConnectionString(properties.getMongodbUri());
    String databaseName = connectionString.getDatabase();
    if (databaseName == null || databaseName.isBlank()) {
      throw new IllegalStateException("MONGODB_URI must include the database name.");
    }

    var codecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
    );

    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .codecRegistry(codecRegistry)
        .build();

    mongoClient = MongoClients.create(settings);
    database = mongoClient.getDatabase(databaseName).withCodecRegistry(codecRegistry);
  }

  @PreDestroy
  public void close() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void initialPing() {
    refreshDatabaseState();
  }

  @Scheduled(fixedDelay = 15000L, initialDelay = 15000L)
  public void refreshDatabaseState() {
    if (database == null) {
      databaseReady.set(false);
      return;
    }

    try {
      database.runCommand(new Document("ping", 1));
      if (databaseReady.compareAndSet(false, true)) {
        logger.info("Connected to MongoDB Atlas");
      }
    } catch (Exception exception) {
      if (databaseReady.compareAndSet(true, false)) {
        logger.warn("MongoDB connection failed: {}", exception.getMessage());
      }
    }
  }

  public boolean isDatabaseReady() {
    return databaseReady.get();
  }

  public <T> List<T> findAll(String collectionName, Bson sort, Class<T> type) {
    List<T> results = new ArrayList<>();
    var iterable = collection(collectionName, type).find();
    if (sort != null) {
      iterable = iterable.sort(sort);
    }
    iterable.into(results);
    return results;
  }

  public <T> List<T> find(String collectionName, Bson filter, Bson sort, int limit, Class<T> type) {
    List<T> results = new ArrayList<>();
    var iterable = collection(collectionName, type).find(filter == null ? new Document() : filter);
    if (sort != null) {
      iterable = iterable.sort(sort);
    }
    if (limit > 0) {
      iterable = iterable.limit(limit);
    }
    iterable.into(results);
    return results;
  }

  public <T> T findFirst(String collectionName, Bson filter, Bson sort, Class<T> type) {
    var iterable = collection(collectionName, type).find(filter == null ? new Document() : filter);
    if (sort != null) {
      iterable = iterable.sort(sort);
    }
    return iterable.first();
  }

  public <T> T findById(String collectionName, String id, Class<T> type) {
    return collection(collectionName, type).find(idFilter(id)).first();
  }

  public <T extends BaseEntity> T save(String collectionName, T entity, Class<T> type) {
    Date now = new Date();
    if (entity.id == null || entity.id.isBlank()) {
      entity.id = new ObjectId().toHexString();
      entity.createdAt = now;
    } else if (entity.createdAt == null) {
      T existing = findById(collectionName, entity.id, type);
      entity.createdAt = existing == null ? now : existing.createdAt;
    }

    entity.updatedAt = now;
    collection(collectionName, type).replaceOne(idFilter(entity.id), entity, new ReplaceOptions().upsert(true));
    return entity;
  }

  public <T extends BaseEntity> T upsertByFilter(String collectionName, Bson filter, T entity, Class<T> type) {
    Date now = new Date();
    if (entity.id == null || entity.id.isBlank()) {
      T existing = findFirst(collectionName, filter, null, type);
      if (existing != null) {
        entity.id = existing.id;
        entity.createdAt = existing.createdAt;
      } else {
        entity.id = new ObjectId().toHexString();
        entity.createdAt = now;
      }
    } else if (entity.createdAt == null) {
      T existing = findFirst(collectionName, filter, null, type);
      entity.createdAt = existing == null ? now : existing.createdAt;
    }

    entity.updatedAt = now;
    collection(collectionName, type).replaceOne(filter, entity, new ReplaceOptions().upsert(true));
    return findFirst(collectionName, filter, null, type);
  }

  public long deleteById(String collectionName, String id, Class<?> type) {
    return collection(collectionName, type).deleteOne(idFilter(id)).getDeletedCount();
  }

  public long deleteMany(String collectionName, Bson filter) {
    return database.getCollection(collectionName).deleteMany(filter).getDeletedCount();
  }

  private <T> MongoCollection<T> collection(String collectionName, Class<T> type) {
    if (database == null) {
      throw new ApiException(500, "MongoDB is not configured.");
    }
    return database.getCollection(collectionName, type);
  }

  private Bson idFilter(String id) {
    if (ObjectId.isValid(id)) {
      return new Document("_id", new ObjectId(id));
    }
    return new Document("_id", id);
  }
}
