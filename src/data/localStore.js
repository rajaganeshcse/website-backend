const fs = require("fs");
const path = require("path");

const {
  heroSeed,
  skillSeed,
  projectSeed,
  appSeed,
  educationSeed,
  internshipSeed,
  certificationSeed,
  workshopSeed,
  journalSeed,
  gallerySeed,
} = require("./seedData");
const {
  serializeHero,
  serializeSkill,
  serializeProject,
  serializeApp,
  serializeEducation,
  serializeInternship,
  serializeCertification,
  serializeWorkshop,
  serializeGalleryItem,
  serializeJournal,
  serializeMessage,
} = require("../utils/serializers");

const storePath = path.join(__dirname, "localStore.json");

const collectionPrefixes = {
  skills: "skill",
  projects: "project",
  apps: "app",
  education: "education",
  internships: "internship",
  certifications: "cert",
  workshops: "workshop",
  journals: "journal",
  gallery: "gallery",
  messages: "message",
};

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function withIds(items, prefix) {
  return items.map((item, index) => ({
    id: `${prefix}-${index + 1}`,
    ...clone(item),
  }));
}

function createInitialStore() {
  return {
    hero: {
      id: "hero-1",
      ...clone(heroSeed),
    },
    skills: withIds(skillSeed, "skill"),
    projects: withIds(projectSeed, "project"),
    apps: withIds(appSeed, "app"),
    education: withIds(educationSeed, "education"),
    internships: withIds(internshipSeed, "internship"),
    certifications: withIds(certificationSeed, "cert"),
    workshops: withIds(workshopSeed, "workshop"),
    journals: withIds(journalSeed, "journal"),
    gallery: withIds(gallerySeed, "gallery"),
    messages: [],
  };
}

function normalizeEducationItem(item) {
  return {
    ...item,
    documents: Array.isArray(item.documents) ? item.documents : [],
  };
}

function normalizeStore(store) {
  const initialStore = createInitialStore();
  let hasChanges = false;
  const normalizedStore = {
    ...store,
  };

  if (!normalizedStore.hero || typeof normalizedStore.hero !== "object") {
    normalizedStore.hero = clone(initialStore.hero);
    hasChanges = true;
  } else if (!normalizedStore.hero.id) {
    normalizedStore.hero = {
      ...normalizedStore.hero,
      id: "hero-1",
    };
    hasChanges = true;
  }

  [
    "skills",
    "projects",
    "apps",
    "education",
    "internships",
    "certifications",
    "workshops",
    "journals",
    "gallery",
    "messages",
  ].forEach((collection) => {
    if (!Array.isArray(normalizedStore[collection])) {
      normalizedStore[collection] = clone(initialStore[collection]);
      hasChanges = true;
    }
  });

  const normalizedEducation = normalizedStore.education.map(normalizeEducationItem);
  if (JSON.stringify(normalizedEducation) !== JSON.stringify(normalizedStore.education)) {
    normalizedStore.education = normalizedEducation;
    hasChanges = true;
  }

  return {
    store: normalizedStore,
    hasChanges,
  };
}

function ensureStore() {
  if (!fs.existsSync(storePath)) {
    fs.writeFileSync(storePath, JSON.stringify(createInitialStore(), null, 2));
  }
}

function readStore() {
  ensureStore();
  const parsedStore = JSON.parse(fs.readFileSync(storePath, "utf8"));
  const { store, hasChanges } = normalizeStore(parsedStore);
  if (hasChanges) {
    writeStore(store);
  }
  return store;
}

function writeStore(store) {
  fs.writeFileSync(storePath, JSON.stringify(store, null, 2));
}

function nextId(items, prefix) {
  const currentMax = items.reduce((max, item) => {
    const numeric = Number(String(item.id || "").split("-").pop());
    return Number.isFinite(numeric) ? Math.max(max, numeric) : max;
  }, 0);

  return `${prefix}-${currentMax + 1}`;
}

function sortByCategoryThenOrder(items) {
  return [...items].sort((a, b) => {
    const categoryCompare = String(a.category || "").localeCompare(String(b.category || ""));
    if (categoryCompare !== 0) return categoryCompare;
    return Number(a.order || 0) - Number(b.order || 0);
  });
}

function sortByOrder(items) {
  return [...items].sort((a, b) => Number(a.order || 0) - Number(b.order || 0));
}

function sortMessages(items) {
  return [...items].sort((a, b) => new Date(b.sent_at || 0) - new Date(a.sent_at || 0));
}

function getPortfolioResponse(req) {
  const store = readStore();

  return {
    hero: serializeHero(store.hero, req),
    skills: sortByCategoryThenOrder(store.skills).map(serializeSkill),
    projects: sortByOrder(store.projects).map((item) => serializeProject(item, req)),
    apps: sortByOrder(store.apps).map((item) => serializeApp(item, req)),
    education: sortByOrder(store.education).map((item) => serializeEducation(item, req)),
    internships: sortByOrder(store.internships).map(serializeInternship),
    certifications: sortByOrder(store.certifications).map((item) => serializeCertification(item, req)),
    workshops: sortByOrder(store.workshops).map((item) => serializeWorkshop(item, req)),
    journals: sortByOrder(store.journals).map((item) => serializeJournal(item, req)),
  };
}

function getGalleryResponse(req) {
  const store = readStore();
  return sortByCategoryThenOrder(store.gallery).map((item) => serializeGalleryItem(item, req));
}

function getMessagesResponse() {
  const store = readStore();
  return sortMessages(store.messages).map(serializeMessage);
}

function saveHero(updates) {
  const store = readStore();
  store.hero = {
    ...(store.hero || { id: "hero-1" }),
    ...updates,
    id: "hero-1",
  };
  writeStore(store);
  return store.hero;
}

function createRecord(collection, record) {
  const store = readStore();
  const items = store[collection];
  const prefix = collectionPrefixes[collection];

  if (!Array.isArray(items) || !prefix) {
    throw new Error(`Unsupported collection: ${collection}`);
  }

  const nextRecord = {
    id: nextId(items, prefix),
    ...record,
  };

  items.push(nextRecord);
  writeStore(store);
  return nextRecord;
}

function updateRecord(collection, id, updater) {
  const store = readStore();
  const items = store[collection];

  if (!Array.isArray(items)) {
    throw new Error(`Unsupported collection: ${collection}`);
  }

  const index = items.findIndex((item) => String(item.id) === String(id));
  if (index === -1) {
    const error = new Error("Record not found.");
    error.statusCode = 404;
    throw error;
  }

  items[index] = updater(clone(items[index]));
  writeStore(store);
  return items[index];
}

function deleteRecord(collection, id) {
  const store = readStore();
  const items = store[collection];

  if (!Array.isArray(items)) {
    throw new Error(`Unsupported collection: ${collection}`);
  }

  const index = items.findIndex((item) => String(item.id) === String(id));
  if (index === -1) {
    const error = new Error("Record not found.");
    error.statusCode = 404;
    throw error;
  }

  items.splice(index, 1);
  writeStore(store);
}

function createMessage(record) {
  return createRecord("messages", {
    ...record,
    sent_at: new Date().toISOString(),
    is_read: false,
  });
}

module.exports = {
  getPortfolioResponse,
  getGalleryResponse,
  getMessagesResponse,
  saveHero,
  createRecord,
  updateRecord,
  deleteRecord,
  createMessage,
  readStore,
  writeStore,
};
