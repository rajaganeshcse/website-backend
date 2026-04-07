const fs = require("fs/promises");
const path = require("path");

const { uploadAutoDeleteHours, uploadCleanupIntervalMinutes } = require("../config/env");
const { backendRoot } = require("../middleware/upload");
const {
  Hero,
  Project,
  AppItem,
  Education,
  Certification,
  Workshop,
  GalleryItem,
  Journal,
  MediaAsset,
} = require("../models/contentModels");
const { toStoredPath } = require("./files");

const mediaRoot = path.join(backendRoot, "media");
const imageExtensions = new Set([
  ".jpg",
  ".jpeg",
  ".png",
  ".gif",
  ".webp",
  ".bmp",
  ".svg",
  ".avif",
  ".heic",
  ".heif",
]);
const ownerModels = {
  Hero,
  Project,
  AppItem,
  Education,
  Certification,
  Workshop,
  GalleryItem,
  Journal,
};

let cleanupIntervalHandle = null;
let cleanupInFlight = false;

function badRequest(message) {
  const error = new Error(message);
  error.statusCode = 400;
  return error;
}

function uniquePaths(paths) {
  return [...new Set(paths.map((value) => String(value || "").trim()).filter(Boolean))];
}

function getUploadedFiles(req) {
  if (!req || !req.files) {
    return [];
  }

  if (Array.isArray(req.files)) {
    return req.files;
  }

  return Object.values(req.files).flat().filter(Boolean);
}

function detectTrackableMediaKind(file) {
  const mimeType = String(file?.mimetype || "").toLowerCase();
  if (mimeType.startsWith("image/")) {
    return "image";
  }

  if (mimeType === "application/pdf") {
    return "pdf";
  }

  const extension = path.extname(String(file?.originalname || "")).toLowerCase();
  if (imageExtensions.has(extension)) {
    return "image";
  }

  if (extension === ".pdf") {
    return "pdf";
  }

  return null;
}

function resolveAutoDeleteHours(req) {
  const body = req?.body || {};
  if (Object.prototype.hasOwnProperty.call(body, "auto_delete_hours")) {
    const rawValue = String(body.auto_delete_hours ?? "").trim();

    if (!rawValue) {
      return 0;
    }

    const parsedValue = Number(rawValue);
    if (!Number.isFinite(parsedValue) || parsedValue < 0) {
      throw badRequest("auto_delete_hours must be a number greater than or equal to 0.");
    }

    return parsedValue;
  }

  return uploadAutoDeleteHours();
}

function expiresAtFromHours(hours) {
  return new Date(Date.now() + hours * 60 * 60 * 1000);
}

function resolveStoredAbsolutePath(storedPath) {
  const normalizedPath = String(storedPath || "").trim();
  if (!normalizedPath.startsWith("/media/")) {
    return null;
  }

  const relativePath = normalizedPath.replace(/^\/+/, "").replace(/\//g, path.sep);
  const absolutePath = path.resolve(backendRoot, relativePath);
  const relativeToMediaRoot = path.relative(mediaRoot, absolutePath);

  if (
    !relativeToMediaRoot ||
    relativeToMediaRoot.startsWith("..") ||
    path.isAbsolute(relativeToMediaRoot)
  ) {
    return null;
  }

  return absolutePath;
}

async function deleteStoredFile(storedPath) {
  const absolutePath = resolveStoredAbsolutePath(storedPath);
  if (!absolutePath) {
    return false;
  }

  try {
    await fs.unlink(absolutePath);
    return true;
  } catch (error) {
    if (error.code === "ENOENT") {
      return false;
    }

    throw error;
  }
}

function extractStoredPaths(ownerModel, ownerSnapshot) {
  switch (ownerModel) {
    case "Hero":
      return uniquePaths([ownerSnapshot.photo_path, ownerSnapshot.resume_path]);
    case "Project":
      return uniquePaths(ownerSnapshot.image_paths || []);
    case "AppItem":
      return uniquePaths([
        ownerSnapshot.cover_image_path,
        ...(ownerSnapshot.screenshot_paths || []),
        ownerSnapshot.apk_path,
      ]);
    case "Education":
      return uniquePaths([
        ownerSnapshot.result_pdf_path,
        ...((ownerSnapshot.documents || []).map((document) => document.pdf_path)),
      ]);
    case "Certification":
      return uniquePaths([ownerSnapshot.image_path]);
    case "Workshop":
      return uniquePaths(ownerSnapshot.image_paths || []);
    case "GalleryItem":
      return uniquePaths([ownerSnapshot.image_path]);
    case "Journal":
      return uniquePaths([ownerSnapshot.pdf_path]);
    default:
      return [];
  }
}

async function deleteOwnedMedia({ ownerModel, ownerId, ownerSnapshot }) {
  const storedPaths = extractStoredPaths(ownerModel, ownerSnapshot);

  for (const storedPath of storedPaths) {
    await deleteStoredFile(storedPath);
  }

  await MediaAsset.deleteMany({
    owner_model: ownerModel,
    owner_id: String(ownerId),
  });
}

async function scheduleUploadedMediaExpiry({ req, ownerModel, ownerId }) {
  const autoDeleteHours = resolveAutoDeleteHours(req);
  if (!(autoDeleteHours > 0)) {
    return [];
  }

  const uploadedFiles = getUploadedFiles(req);
  if (uploadedFiles.length === 0) {
    return [];
  }

  const expiresAt = expiresAtFromHours(autoDeleteHours);
  const scheduledAssets = [];

  for (const file of uploadedFiles) {
    const mediaKind = detectTrackableMediaKind(file);
    if (!mediaKind) {
      continue;
    }

    const storedPath = toStoredPath(file);
    if (!storedPath) {
      continue;
    }

    const asset = await MediaAsset.findOneAndUpdate(
      {
        owner_model: ownerModel,
        owner_id: String(ownerId),
        stored_path: storedPath,
      },
      {
        owner_model: ownerModel,
        owner_id: String(ownerId),
        stored_path: storedPath,
        source_field: String(file.fieldname || ""),
        original_name: String(file.originalname || ""),
        media_kind: mediaKind,
        expires_at: expiresAt,
        deleted_at: null,
        cleanup_status: "scheduled",
        cleanup_error: "",
      },
      {
        upsert: true,
        new: true,
        setDefaultsOnInsert: true,
      }
    );

    scheduledAssets.push(asset);
  }

  return scheduledAssets;
}

async function removeStoredPathReference(ownerModel, ownerId, storedPath) {
  const Model = ownerModels[ownerModel];
  if (!Model) {
    return "owner_missing";
  }

  const item = await Model.findById(ownerId);
  if (!item) {
    return "owner_missing";
  }

  let changed = false;

  switch (ownerModel) {
    case "Hero":
      if (item.photo_path === storedPath) {
        item.photo_path = "";
        changed = true;
      }
      if (item.resume_path === storedPath) {
        item.resume_path = "";
        item.resume_name = "";
        changed = true;
      }
      break;
    case "Project": {
      const nextImages = (item.image_paths || []).filter((value) => value !== storedPath);
      if (nextImages.length !== (item.image_paths || []).length) {
        item.image_paths = nextImages;
        changed = true;
      }
      break;
    }
    case "AppItem": {
      if (item.cover_image_path === storedPath) {
        item.cover_image_path = "";
        changed = true;
      }

      const nextScreenshots = (item.screenshot_paths || []).filter((value) => value !== storedPath);
      if (nextScreenshots.length !== (item.screenshot_paths || []).length) {
        item.screenshot_paths = nextScreenshots;
        changed = true;
      }

      if (item.apk_path === storedPath) {
        item.apk_path = "";
        item.apk_name = "";
        changed = true;
      }
      break;
    }
    case "Education": {
      const nextDocuments = (item.documents || []).filter((document) => document.pdf_path !== storedPath);
      if (nextDocuments.length !== (item.documents || []).length) {
        item.documents = nextDocuments;
        changed = true;
      }

      const primaryDocument = (item.documents || [])[0] || {};
      const nextResultPdfPath = primaryDocument.pdf_path || "";
      const nextResultPdfName = primaryDocument.pdf_name || "";

      if (item.result_pdf_path !== nextResultPdfPath) {
        item.result_pdf_path = nextResultPdfPath;
        changed = true;
      }

      if (item.result_pdf_name !== nextResultPdfName) {
        item.result_pdf_name = nextResultPdfName;
        changed = true;
      }
      break;
    }
    case "Certification":
      if (item.image_path === storedPath) {
        item.image_path = "";
        changed = true;
      }
      break;
    case "Workshop": {
      const nextImages = (item.image_paths || []).filter((value) => value !== storedPath);
      if (nextImages.length !== (item.image_paths || []).length) {
        item.image_paths = nextImages;
        changed = true;
      }
      break;
    }
    case "GalleryItem":
      if (item.image_path === storedPath) {
        item.image_path = "";
        changed = true;
      }
      break;
    case "Journal":
      if (item.pdf_path === storedPath) {
        item.pdf_path = "";
        item.pdf_name = "";
        changed = true;
      }
      break;
    default:
      break;
  }

  if (!changed) {
    return "unchanged";
  }

  await item.save();
  return "updated";
}

async function cleanupExpiredMediaAssets() {
  if (cleanupInFlight) {
    return;
  }

  cleanupInFlight = true;

  try {
    const expiredAssets = await MediaAsset.find({
      cleanup_status: { $in: ["scheduled", "failed"] },
      expires_at: { $lte: new Date() },
    })
      .sort({ expires_at: 1, createdAt: 1 })
      .limit(50);

    for (const asset of expiredAssets) {
      try {
        await removeStoredPathReference(asset.owner_model, asset.owner_id, asset.stored_path);
        await deleteStoredFile(asset.stored_path);

        asset.deleted_at = new Date();
        asset.cleanup_status = "deleted";
        asset.cleanup_error = "";
        await asset.save();
      } catch (error) {
        asset.cleanup_status = "failed";
        asset.cleanup_error = String(error.message || error);
        await asset.save();
      }
    }
  } finally {
    cleanupInFlight = false;
  }
}

function startMediaCleanupScheduler() {
  if (cleanupIntervalHandle) {
    return cleanupIntervalHandle;
  }

  const intervalMinutes = uploadCleanupIntervalMinutes();
  const intervalMs = Math.max(intervalMinutes, 1) * 60 * 1000;

  cleanupIntervalHandle = setInterval(() => {
    cleanupExpiredMediaAssets().catch((error) => {
      console.error(`Media cleanup failed: ${error.message}`);
    });
  }, intervalMs);

  if (typeof cleanupIntervalHandle.unref === "function") {
    cleanupIntervalHandle.unref();
  }

  const initialRun = setTimeout(() => {
    cleanupExpiredMediaAssets().catch((error) => {
      console.error(`Initial media cleanup failed: ${error.message}`);
    });
  }, 1000);

  if (typeof initialRun.unref === "function") {
    initialRun.unref();
  }

  return cleanupIntervalHandle;
}

module.exports = {
  deleteOwnedMedia,
  resolveAutoDeleteHours,
  scheduleUploadedMediaExpiry,
  cleanupExpiredMediaAssets,
  startMediaCleanupScheduler,
};
