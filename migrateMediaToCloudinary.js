require("dotenv").config();

const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");

const connectToDatabase = require("./src/config/db");
const { uploadLocalFileToCloudinary } = require("./src/utils/cloudinaryMedia");
const {
  Hero,
  Project,
  AppItem,
  Education,
  Certification,
  Workshop,
  GalleryItem,
  Journal,
} = require("./src/models/contentModels");
const { readStore, writeStore } = require("./src/data/localStore");

const backendRoot = __dirname;

function isLocalMediaPath(value) {
  return String(value || "").trim().startsWith("/media/");
}

function resolveLocalMediaPath(value) {
  if (!isLocalMediaPath(value)) {
    return null;
  }

  return path.join(backendRoot, String(value || "").replace(/^\/+/, "").replace(/\//g, path.sep));
}

async function uploadStoredPath(value, folder) {
  if (!isLocalMediaPath(value)) {
    return value;
  }

  const localPath = resolveLocalMediaPath(value);
  if (!localPath || !fs.existsSync(localPath)) {
    console.warn(`Skipped missing local media: ${value}`);
    return value;
  }

  const uploaded = await uploadLocalFileToCloudinary(localPath, folder, path.basename(localPath));
  return uploaded?.secure_url || value;
}

async function migrateCollection(label, Model, migrateFn) {
  const items = await Model.find();
  let updated = 0;

  for (const item of items) {
    const changed = await migrateFn(item);
    if (changed) {
      await item.save();
      updated += 1;
    }
  }

  console.log(`${label}: ${updated}/${items.length} updated`);
}

async function migrateMongoMedia() {
  await migrateCollection("Hero", Hero, async (item) => {
    let changed = false;

    const nextPhoto = await uploadStoredPath(item.photo_path, "hero");
    if (nextPhoto !== item.photo_path) {
      item.photo_path = nextPhoto;
      changed = true;
    }

    const nextResume = await uploadStoredPath(item.resume_path, "hero");
    if (nextResume !== item.resume_path) {
      item.resume_path = nextResume;
      changed = true;
    }

    return changed;
  });

  await migrateCollection("Projects", Project, async (item) => {
    const nextImages = [];
    let changed = false;

    for (const imagePath of item.image_paths || []) {
      const nextImage = await uploadStoredPath(imagePath, "projects");
      nextImages.push(nextImage);
      if (nextImage !== imagePath) {
        changed = true;
      }
    }

    if (changed) {
      item.image_paths = nextImages;
    }

    return changed;
  });

  await migrateCollection("Apps", AppItem, async (item) => {
    let changed = false;

    const nextCover = await uploadStoredPath(item.cover_image_path, "apps");
    if (nextCover !== item.cover_image_path) {
      item.cover_image_path = nextCover;
      changed = true;
    }

    const nextApk = await uploadStoredPath(item.apk_path, "apps");
    if (nextApk !== item.apk_path) {
      item.apk_path = nextApk;
      changed = true;
    }

    const nextScreenshots = [];
    for (const screenshot of item.screenshot_paths || []) {
      const nextScreenshot = await uploadStoredPath(screenshot, "apps");
      nextScreenshots.push(nextScreenshot);
      if (nextScreenshot !== screenshot) {
        changed = true;
      }
    }

    if (changed) {
      item.screenshot_paths = nextScreenshots;
    }

    return changed;
  });

  await migrateCollection("Education", Education, async (item) => {
    let changed = false;

    const nextResultPdf = await uploadStoredPath(item.result_pdf_path, "education");
    if (nextResultPdf !== item.result_pdf_path) {
      item.result_pdf_path = nextResultPdf;
      changed = true;
    }

    const nextDocuments = [];
    for (const document of item.documents || []) {
      const nextPdf = await uploadStoredPath(document.pdf_path, "education");
      nextDocuments.push({
        ...(typeof document?.toObject === "function" ? document.toObject() : document),
        pdf_path: nextPdf,
      });
      if (nextPdf !== document.pdf_path) {
        changed = true;
      }
    }

    if (changed) {
      item.documents = nextDocuments;
      if (!item.result_pdf_path && nextDocuments[0]?.pdf_path) {
        item.result_pdf_path = nextDocuments[0].pdf_path;
      }
    }

    return changed;
  });

  await migrateCollection("Certifications", Certification, async (item) => {
    const nextImage = await uploadStoredPath(item.image_path, "certs");
    if (nextImage !== item.image_path) {
      item.image_path = nextImage;
      return true;
    }

    return false;
  });

  await migrateCollection("Workshops", Workshop, async (item) => {
    const nextImages = [];
    let changed = false;

    for (const imagePath of item.image_paths || []) {
      const nextImage = await uploadStoredPath(imagePath, "workshops");
      nextImages.push(nextImage);
      if (nextImage !== imagePath) {
        changed = true;
      }
    }

    if (changed) {
      item.image_paths = nextImages;
    }

    return changed;
  });

  await migrateCollection("Gallery", GalleryItem, async (item) => {
    const nextImage = await uploadStoredPath(item.image_path, "gallery");
    if (nextImage !== item.image_path) {
      item.image_path = nextImage;
      return true;
    }

    return false;
  });

  await migrateCollection("Journals", Journal, async (item) => {
    const nextPdf = await uploadStoredPath(item.pdf_path, "journals");
    if (nextPdf !== item.pdf_path) {
      item.pdf_path = nextPdf;
      return true;
    }

    return false;
  });
}

async function migrateLocalStoreMedia() {
  const store = readStore();
  let changed = false;

  const nextHeroPhoto = await uploadStoredPath(store.hero?.photo_path, "hero");
  if (nextHeroPhoto !== store.hero?.photo_path) {
    store.hero.photo_path = nextHeroPhoto;
    changed = true;
  }

  const nextHeroResume = await uploadStoredPath(store.hero?.resume_path, "hero");
  if (nextHeroResume !== store.hero?.resume_path) {
    store.hero.resume_path = nextHeroResume;
    changed = true;
  }

  for (const item of store.projects || []) {
    const nextImages = [];
    let itemChanged = false;

    for (const imagePath of item.image_paths || []) {
      const nextImage = await uploadStoredPath(imagePath, "projects");
      nextImages.push(nextImage);
      if (nextImage !== imagePath) {
        itemChanged = true;
      }
    }

    if (itemChanged) {
      item.image_paths = nextImages;
      changed = true;
    }
  }

  for (const item of store.apps || []) {
    const nextCover = await uploadStoredPath(item.cover_image_path, "apps");
    if (nextCover !== item.cover_image_path) {
      item.cover_image_path = nextCover;
      changed = true;
    }

    const nextApk = await uploadStoredPath(item.apk_path, "apps");
    if (nextApk !== item.apk_path) {
      item.apk_path = nextApk;
      changed = true;
    }

    const nextScreenshots = [];
    let itemChanged = false;

    for (const screenshot of item.screenshot_paths || []) {
      const nextScreenshot = await uploadStoredPath(screenshot, "apps");
      nextScreenshots.push(nextScreenshot);
      if (nextScreenshot !== screenshot) {
        itemChanged = true;
      }
    }

    if (itemChanged) {
      item.screenshot_paths = nextScreenshots;
      changed = true;
    }
  }

  for (const item of store.education || []) {
    const nextResultPdf = await uploadStoredPath(item.result_pdf_path, "education");
    if (nextResultPdf !== item.result_pdf_path) {
      item.result_pdf_path = nextResultPdf;
      changed = true;
    }

    for (const document of item.documents || []) {
      const nextPdf = await uploadStoredPath(document.pdf_path, "education");
      if (nextPdf !== document.pdf_path) {
        document.pdf_path = nextPdf;
        changed = true;
      }
    }
  }

  for (const item of store.certifications || []) {
    const nextImage = await uploadStoredPath(item.image_path, "certs");
    if (nextImage !== item.image_path) {
      item.image_path = nextImage;
      changed = true;
    }
  }

  for (const item of store.workshops || []) {
    const nextImages = [];
    let itemChanged = false;

    for (const imagePath of item.image_paths || []) {
      const nextImage = await uploadStoredPath(imagePath, "workshops");
      nextImages.push(nextImage);
      if (nextImage !== imagePath) {
        itemChanged = true;
      }
    }

    if (itemChanged) {
      item.image_paths = nextImages;
      changed = true;
    }
  }

  for (const item of store.journals || []) {
    const nextPdf = await uploadStoredPath(item.pdf_path, "journals");
    if (nextPdf !== item.pdf_path) {
      item.pdf_path = nextPdf;
      changed = true;
    }
  }

  for (const item of store.gallery || []) {
    const nextImage = await uploadStoredPath(item.image_path, "gallery");
    if (nextImage !== item.image_path) {
      item.image_path = nextImage;
      changed = true;
    }
  }

  if (changed) {
    writeStore(store);
  }

  console.log(`Local store: ${changed ? "updated" : "already migrated"}`);
}

async function main() {
  await connectToDatabase();
  await migrateMongoMedia();
  await migrateLocalStoreMedia();
  console.log("Media migration completed.");
}

main()
  .catch((error) => {
    console.error("Media migration failed:", error.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    await mongoose.disconnect().catch(() => {});
  });
