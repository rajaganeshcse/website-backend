const express = require("express");

const asyncHandler = require("../utils/asyncHandler");
const { requireAdminAuth } = require("../middleware/auth");
const { createUploader } = require("../middleware/upload");
const { toStoredPath, requestOrigin } = require("../utils/files");
const { DEFAULT_HERO } = require("../utils/defaults");
const { isDatabaseReady } = require("../utils/dbState");
const {
  getGalleryResponse,
  getMessagesResponse,
  saveHero,
  createRecord,
  updateRecord,
  deleteRecord,
} = require("../data/localStore");
const {
  Hero,
  Skill,
  Project,
  AppItem,
  Education,
  Internship,
  Certification,
  Workshop,
  Journal,
  GalleryItem,
  Message,
} = require("../models/contentModels");
const {
  serializeHero,
  serializeSkill,
  serializeProject,
  serializeApp,
  serializeEducation,
  serializeInternship,
  serializeCertification,
  serializeWorkshop,
  serializeJournal,
  serializeGalleryItem,
  serializeMessage,
} = require("../utils/serializers");

const router = express.Router();

const heroUpload = createUploader("hero");
const projectUpload = createUploader("projects");
const appUpload = createUploader("apps");
const educationUpload = createUploader("education");
const journalUpload = createUploader("journals");
const certUpload = createUploader("certs");
const workshopUpload = createUploader("workshops");
const galleryUpload = createUploader("gallery");

router.use(requireAdminAuth);

function numberOr(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function pickFields(body, fields) {
  return fields.reduce((result, field) => {
    if (body[field] !== undefined) {
      result[field] = typeof body[field] === "string" ? body[field].trim() : body[field];
    }
    return result;
  }, {});
}

function getFile(req, field) {
  if (!req.files) {
    return null;
  }

  if (Array.isArray(req.files)) {
    return req.files.find((file) => file.fieldname === field) || null;
  }

  if (!req.files[field] || !req.files[field][0]) {
    return null;
  }

  return req.files[field][0];
}

function getFiles(req, fields) {
  return fields.map((field) => getFile(req, field)).filter(Boolean);
}

function normalizeStoredDocumentPath(req, value) {
  const rawValue = String(value || "").trim();
  if (!rawValue) {
    return "";
  }

  if (rawValue.startsWith("/")) {
    return rawValue;
  }

  try {
    const parsed = new URL(rawValue);
    if (parsed.origin === requestOrigin(req)) {
      return parsed.pathname;
    }
  } catch {
    return rawValue;
  }

  return rawValue;
}

function buildEducationDocumentsFromRequest(req, currentDocuments = []) {
  const hasDocumentsJson = typeof req.body.documents_json === "string";
  const rawDocuments = (() => {
    if (!hasDocumentsJson) {
      return null;
    }

    try {
      return JSON.parse(req.body.documents_json || "[]");
    } catch {
      return [];
    }
  })();

  if (Array.isArray(rawDocuments)) {
    return rawDocuments
      .map((document, index) => {
        const uploadedFile = document.fileField ? getFile(req, document.fileField) : null;
        const pdfPath = uploadedFile
          ? toStoredPath(uploadedFile)
          : normalizeStoredDocumentPath(req, document.pdf_path || document.pdf_url || document.pdf_download_url);
        const pdfName = uploadedFile ? uploadedFile.originalname || "" : String(document.pdf_name || "").trim();
        const title = String(document.title || "").trim() || pdfName || `Document ${index + 1}`;

        return {
          title,
          pdf_path: pdfPath,
          pdf_name: pdfName,
        };
      })
      .filter((document) => document.pdf_path);
  }

  const legacyPdf = getFile(req, "result_pdf");
  if (legacyPdf) {
    return [
      {
        title: "Uploaded PDF",
        pdf_path: toStoredPath(legacyPdf),
        pdf_name: legacyPdf.originalname || "",
      },
    ];
  }

  return currentDocuments || [];
}

function applyEducationDocuments(target, documents) {
  const normalizedDocuments = documents.filter((document) => document.pdf_path || document.title);
  const primaryDocument = normalizedDocuments[0] || {};

  target.documents = normalizedDocuments;
  target.result_pdf_path = primaryDocument.pdf_path || "";
  target.result_pdf_name = primaryDocument.pdf_name || "";
}

async function getOrCreateHero() {
  let hero = await Hero.findOne().sort({ createdAt: 1 });
  if (!hero) {
    hero = await Hero.create(DEFAULT_HERO);
  }
  return hero;
}

async function findByIdOrThrow(Model, id, label) {
  const item = await Model.findById(id);
  if (!item) {
    const error = new Error(`${label} not found.`);
    error.statusCode = 404;
    throw error;
  }
  return item;
}

router.get(
  "/messages/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      return res.json(getMessagesResponse());
    }

    const messages = await Message.find().sort({ sent_at: -1, createdAt: -1 });
    res.json(messages.map(serializeMessage));
  })
);

router.get(
  "/gallery/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      return res.json(getGalleryResponse(req));
    }

    const items = await GalleryItem.find().sort({ category: 1, order: 1, createdAt: -1 });
    res.json(items.map((item) => serializeGalleryItem(item, req)));
  })
);

router.put(
  "/hero/",
  heroUpload.fields([
    { name: "photo", maxCount: 1 },
    { name: "resume", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    const updates = pickFields(req.body, [
      "name",
      "title",
      "bio",
      "email",
      "phone",
      "github",
      "linkedin",
      "leetcode",
      "instagram",
      "portfolio",
      "location",
      "college",
      "address",
    ]);

    const photo = getFile(req, "photo");
    const resume = getFile(req, "resume");

    if (!isDatabaseReady()) {
      if (photo) {
        updates.photo_path = toStoredPath(photo);
      }

      if (resume) {
        updates.resume_path = toStoredPath(resume);
        updates.resume_name = resume.originalname || "";
      }

      const hero = saveHero(updates);
      return res.json(serializeHero(hero, req));
    }

    const hero = await getOrCreateHero();
    Object.assign(hero, updates);

    if (photo) {
      hero.photo_path = toStoredPath(photo);
    }

    if (resume) {
      hero.resume_path = toStoredPath(resume);
      hero.resume_name = resume.originalname || "";
    }

    await hero.save();
    res.json(serializeHero(hero, req));
  })
);

router.post(
  "/skills/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = createRecord("skills", {
        ...pickFields(req.body, ["category", "name"]),
        level: numberOr(req.body.level, 80),
        order: numberOr(req.body.order, 0),
      });

      return res.status(201).json(serializeSkill(item));
    }

    const item = await Skill.create({
      ...pickFields(req.body, ["category", "name"]),
      level: numberOr(req.body.level, 80),
      order: numberOr(req.body.order, 0),
    });

    res.status(201).json(serializeSkill(item));
  })
);

router.put(
  "/skills/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = updateRecord("skills", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["category", "name"]),
        level: req.body.level !== undefined ? numberOr(req.body.level, current.level) : current.level,
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
      }));

      return res.json(serializeSkill(item));
    }

    const item = await findByIdOrThrow(Skill, req.params.id, "Skill");
    Object.assign(item, pickFields(req.body, ["category", "name"]));
    if (req.body.level !== undefined) item.level = numberOr(req.body.level, item.level);
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);
    await item.save();
    res.json(serializeSkill(item));
  })
);

router.delete(
  "/skills/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("skills", req.params.id);
      return res.json({ detail: "Skill deleted." });
    }

    const item = await findByIdOrThrow(Skill, req.params.id, "Skill");
    await item.deleteOne();
    res.json({ detail: "Skill deleted." });
  })
);

router.post(
  "/projects/",
  projectUpload.fields([
    { name: "image", maxCount: 1 },
    { name: "image2", maxCount: 1 },
    { name: "image3", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = createRecord("projects", {
        ...pickFields(req.body, ["title", "description", "tech_stack", "github_url", "live_url", "video_url"]),
        order: numberOr(req.body.order, 0),
        image_paths: getFiles(req, ["image", "image2", "image3"]).map(toStoredPath),
      });

      return res.status(201).json(serializeProject(item, req));
    }

    const item = await Project.create({
      ...pickFields(req.body, ["title", "description", "tech_stack", "github_url", "live_url", "video_url"]),
      order: numberOr(req.body.order, 0),
      image_paths: getFiles(req, ["image", "image2", "image3"]).map(toStoredPath),
    });

    res.status(201).json(serializeProject(item, req));
  })
);

router.put(
  "/projects/:id/",
  projectUpload.fields([
    { name: "image", maxCount: 1 },
    { name: "image2", maxCount: 1 },
    { name: "image3", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const newImages = getFiles(req, ["image", "image2", "image3"]).map(toStoredPath);
      const item = updateRecord("projects", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["title", "description", "tech_stack", "github_url", "live_url", "video_url"]),
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
        image_paths:
          req.body.replace_project_gallery && newImages.length > 0
            ? newImages
            : current.image_paths || [],
      }));

      return res.json(serializeProject(item, req));
    }

    const item = await findByIdOrThrow(Project, req.params.id, "Project");
    Object.assign(item, pickFields(req.body, ["title", "description", "tech_stack", "github_url", "live_url", "video_url"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);

    const newImages = getFiles(req, ["image", "image2", "image3"]).map(toStoredPath);
    if (req.body.replace_project_gallery && newImages.length > 0) {
      item.image_paths = newImages;
    }

    await item.save();
    res.json(serializeProject(item, req));
  })
);

router.delete(
  "/projects/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("projects", req.params.id);
      return res.json({ detail: "Project deleted." });
    }

    const item = await findByIdOrThrow(Project, req.params.id, "Project");
    await item.deleteOne();
    res.json({ detail: "Project deleted." });
  })
);

router.post(
  "/apps/",
  appUpload.fields([
    { name: "cover_image", maxCount: 1 },
    { name: "screenshot1", maxCount: 1 },
    { name: "screenshot2", maxCount: 1 },
    { name: "screenshot3", maxCount: 1 },
    { name: "apk", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    const cover = getFile(req, "cover_image");
    const apk = getFile(req, "apk");
    const screenshots = getFiles(req, ["screenshot1", "screenshot2", "screenshot3"]).map(toStoredPath);

    if (!isDatabaseReady()) {
      const item = createRecord("apps", {
        ...pickFields(req.body, ["title", "description", "dashboard_url", "github_url", "playstore_url"]),
        order: numberOr(req.body.order, 0),
        cover_image_path: cover ? toStoredPath(cover) : "",
        screenshot_paths: screenshots,
        apk_path: apk ? toStoredPath(apk) : "",
        apk_name: apk ? apk.originalname || "" : "",
      });

      return res.status(201).json(serializeApp(item, req));
    }

    const item = await AppItem.create({
      ...pickFields(req.body, ["title", "description", "dashboard_url", "github_url", "playstore_url"]),
      order: numberOr(req.body.order, 0),
      cover_image_path: cover ? toStoredPath(cover) : "",
      screenshot_paths: screenshots,
      apk_path: apk ? toStoredPath(apk) : "",
      apk_name: apk ? apk.originalname || "" : "",
    });

    res.status(201).json(serializeApp(item, req));
  })
);

router.put(
  "/apps/:id/",
  appUpload.fields([
    { name: "cover_image", maxCount: 1 },
    { name: "screenshot1", maxCount: 1 },
    { name: "screenshot2", maxCount: 1 },
    { name: "screenshot3", maxCount: 1 },
    { name: "apk", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const cover = getFile(req, "cover_image");
      const apk = getFile(req, "apk");
      const screenshots = getFiles(req, ["screenshot1", "screenshot2", "screenshot3"]).map(toStoredPath);

      const item = updateRecord("apps", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["title", "description", "dashboard_url", "github_url", "playstore_url"]),
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
        cover_image_path: cover ? toStoredPath(cover) : current.cover_image_path,
        screenshot_paths:
          req.body.replace_app_screenshots && screenshots.length > 0
            ? screenshots
            : current.screenshot_paths || [],
        apk_path: apk ? toStoredPath(apk) : current.apk_path,
        apk_name: apk ? apk.originalname || "" : current.apk_name,
      }));

      return res.json(serializeApp(item, req));
    }

    const item = await findByIdOrThrow(AppItem, req.params.id, "App");
    Object.assign(item, pickFields(req.body, ["title", "description", "dashboard_url", "github_url", "playstore_url"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);

    const cover = getFile(req, "cover_image");
    const apk = getFile(req, "apk");
    const screenshots = getFiles(req, ["screenshot1", "screenshot2", "screenshot3"]).map(toStoredPath);

    if (cover) item.cover_image_path = toStoredPath(cover);
    if (req.body.replace_app_screenshots && screenshots.length > 0) item.screenshot_paths = screenshots;
    if (apk) {
      item.apk_path = toStoredPath(apk);
      item.apk_name = apk.originalname || "";
    }

    await item.save();
    res.json(serializeApp(item, req));
  })
);

router.delete(
  "/apps/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("apps", req.params.id);
      return res.json({ detail: "App deleted." });
    }

    const item = await findByIdOrThrow(AppItem, req.params.id, "App");
    await item.deleteOne();
    res.json({ detail: "App deleted." });
  })
);

router.post(
  "/education/",
  educationUpload.any(),
  asyncHandler(async (req, res) => {
    const documents = buildEducationDocumentsFromRequest(req, []);

    if (!isDatabaseReady()) {
      const item = createRecord("education", {
        ...pickFields(req.body, ["degree", "institution", "year", "score"]),
        order: numberOr(req.body.order, 0),
        documents,
        result_pdf_path: documents[0]?.pdf_path || "",
        result_pdf_name: documents[0]?.pdf_name || "",
      });

      return res.status(201).json(serializeEducation(item, req));
    }

    const item = await Education.create({
      ...pickFields(req.body, ["degree", "institution", "year", "score"]),
      order: numberOr(req.body.order, 0),
    });

    applyEducationDocuments(item, documents);
    await item.save();

    res.status(201).json(serializeEducation(item, req));
  })
);

router.put(
  "/education/:id/",
  educationUpload.any(),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = updateRecord("education", req.params.id, (current) => ({
        ...(() => {
          const documents = buildEducationDocumentsFromRequest(req, current.documents || []);
          return {
            ...current,
            ...pickFields(req.body, ["degree", "institution", "year", "score"]),
            order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
            documents,
            result_pdf_path: documents[0]?.pdf_path || "",
            result_pdf_name: documents[0]?.pdf_name || "",
          };
        })(),
      }));

      return res.json(serializeEducation(item, req));
    }

    const item = await findByIdOrThrow(Education, req.params.id, "Education");
    Object.assign(item, pickFields(req.body, ["degree", "institution", "year", "score"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);
    applyEducationDocuments(item, buildEducationDocumentsFromRequest(req, item.documents || []));

    await item.save();
    res.json(serializeEducation(item, req));
  })
);

router.delete(
  "/education/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("education", req.params.id);
      return res.json({ detail: "Education deleted." });
    }

    const item = await findByIdOrThrow(Education, req.params.id, "Education");
    await item.deleteOne();
    res.json({ detail: "Education deleted." });
  })
);

router.post(
  "/internships/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = createRecord("internships", {
        ...pickFields(req.body, ["role", "company", "duration", "description"]),
        order: numberOr(req.body.order, 0),
      });

      return res.status(201).json(serializeInternship(item));
    }

    const item = await Internship.create({
      ...pickFields(req.body, ["role", "company", "duration", "description"]),
      order: numberOr(req.body.order, 0),
    });

    res.status(201).json(serializeInternship(item));
  })
);

router.put(
  "/internships/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = updateRecord("internships", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["role", "company", "duration", "description"]),
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
      }));

      return res.json(serializeInternship(item));
    }

    const item = await findByIdOrThrow(Internship, req.params.id, "Internship");
    Object.assign(item, pickFields(req.body, ["role", "company", "duration", "description"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);
    await item.save();
    res.json(serializeInternship(item));
  })
);

router.delete(
  "/internships/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("internships", req.params.id);
      return res.json({ detail: "Internship deleted." });
    }

    const item = await findByIdOrThrow(Internship, req.params.id, "Internship");
    await item.deleteOne();
    res.json({ detail: "Internship deleted." });
  })
);

router.post(
  "/certs/",
  certUpload.fields([{ name: "image", maxCount: 1 }]),
  asyncHandler(async (req, res) => {
    const image = getFile(req, "image");

    if (!isDatabaseReady()) {
      const item = createRecord("certifications", {
        ...pickFields(req.body, ["name", "issuer", "year", "description", "credential_url"]),
        order: numberOr(req.body.order, 0),
        image_path: image ? toStoredPath(image) : "",
      });

      return res.status(201).json(serializeCertification(item, req));
    }

    const item = await Certification.create({
      ...pickFields(req.body, ["name", "issuer", "year", "description", "credential_url"]),
      order: numberOr(req.body.order, 0),
      image_path: image ? toStoredPath(image) : "",
    });

    res.status(201).json(serializeCertification(item, req));
  })
);

router.put(
  "/certs/:id/",
  certUpload.fields([{ name: "image", maxCount: 1 }]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const image = getFile(req, "image");
      const item = updateRecord("certifications", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["name", "issuer", "year", "description", "credential_url"]),
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
        image_path: image ? toStoredPath(image) : current.image_path,
      }));

      return res.json(serializeCertification(item, req));
    }

    const item = await findByIdOrThrow(Certification, req.params.id, "Certification");
    Object.assign(item, pickFields(req.body, ["name", "issuer", "year", "description", "credential_url"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);

    const image = getFile(req, "image");
    if (image) {
      item.image_path = toStoredPath(image);
    }

    await item.save();
    res.json(serializeCertification(item, req));
  })
);

router.delete(
  "/certs/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("certifications", req.params.id);
      return res.json({ detail: "Certification deleted." });
    }

    const item = await findByIdOrThrow(Certification, req.params.id, "Certification");
    await item.deleteOne();
    res.json({ detail: "Certification deleted." });
  })
);

router.post(
  "/workshops/",
  workshopUpload.fields([
    { name: "image", maxCount: 1 },
    { name: "image2", maxCount: 1 },
    { name: "image3", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const item = createRecord("workshops", {
        ...pickFields(req.body, ["title", "organizer", "date", "description", "link_url"]),
        order: numberOr(req.body.order, 0),
        image_paths: getFiles(req, ["image", "image2", "image3"]).map(toStoredPath),
      });

      return res.status(201).json(serializeWorkshop(item, req));
    }

    const item = await Workshop.create({
      ...pickFields(req.body, ["title", "organizer", "date", "description", "link_url"]),
      order: numberOr(req.body.order, 0),
      image_paths: getFiles(req, ["image", "image2", "image3"]).map(toStoredPath),
    });

    res.status(201).json(serializeWorkshop(item, req));
  })
);

router.put(
  "/workshops/:id/",
  workshopUpload.fields([
    { name: "image", maxCount: 1 },
    { name: "image2", maxCount: 1 },
    { name: "image3", maxCount: 1 },
  ]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const image1 = getFile(req, "image");
      const image2 = getFile(req, "image2");
      const image3 = getFile(req, "image3");

      const item = updateRecord("workshops", req.params.id, (current) => {
        const images = [...(current.image_paths || [])];
        if (image1) images[0] = toStoredPath(image1);
        if (image2) images[1] = toStoredPath(image2);
        if (image3) images[2] = toStoredPath(image3);

        return {
          ...current,
          ...pickFields(req.body, ["title", "organizer", "date", "description", "link_url"]),
          order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
          image_paths: images.filter(Boolean),
        };
      });

      return res.json(serializeWorkshop(item, req));
    }

    const item = await findByIdOrThrow(Workshop, req.params.id, "Workshop");
    Object.assign(item, pickFields(req.body, ["title", "organizer", "date", "description", "link_url"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);

    const images = [...(item.image_paths || [])];
    const image1 = getFile(req, "image");
    const image2 = getFile(req, "image2");
    const image3 = getFile(req, "image3");

    if (image1) images[0] = toStoredPath(image1);
    if (image2) images[1] = toStoredPath(image2);
    if (image3) images[2] = toStoredPath(image3);

    item.image_paths = images.filter(Boolean);
    await item.save();
    res.json(serializeWorkshop(item, req));
  })
);

router.delete(
  "/workshops/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("workshops", req.params.id);
      return res.json({ detail: "Workshop deleted." });
    }

    const item = await findByIdOrThrow(Workshop, req.params.id, "Workshop");
    await item.deleteOne();
    res.json({ detail: "Workshop deleted." });
  })
);

router.post(
  "/journals/",
  journalUpload.fields([{ name: "pdf", maxCount: 1 }]),
  asyncHandler(async (req, res) => {
    const pdf = getFile(req, "pdf");

    if (!isDatabaseReady()) {
      const item = createRecord("journals", {
        ...pickFields(req.body, ["title", "details"]),
        order: numberOr(req.body.order, 0),
        pdf_path: pdf ? toStoredPath(pdf) : "",
        pdf_name: pdf ? pdf.originalname || "" : "",
      });

      return res.status(201).json(serializeJournal(item, req));
    }

    const item = await Journal.create({
      ...pickFields(req.body, ["title", "details"]),
      order: numberOr(req.body.order, 0),
      pdf_path: pdf ? toStoredPath(pdf) : "",
      pdf_name: pdf ? pdf.originalname || "" : "",
    });

    res.status(201).json(serializeJournal(item, req));
  })
);

router.put(
  "/journals/:id/",
  journalUpload.fields([{ name: "pdf", maxCount: 1 }]),
  asyncHandler(async (req, res) => {
    const pdf = getFile(req, "pdf");

    if (!isDatabaseReady()) {
      const item = updateRecord("journals", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["title", "details"]),
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
        pdf_path: pdf ? toStoredPath(pdf) : current.pdf_path,
        pdf_name: pdf ? pdf.originalname || "" : current.pdf_name,
      }));

      return res.json(serializeJournal(item, req));
    }

    const item = await findByIdOrThrow(Journal, req.params.id, "Journal");
    Object.assign(item, pickFields(req.body, ["title", "details"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);
    if (pdf) {
      item.pdf_path = toStoredPath(pdf);
      item.pdf_name = pdf.originalname || "";
    }
    await item.save();
    res.json(serializeJournal(item, req));
  })
);

router.delete(
  "/journals/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("journals", req.params.id);
      return res.json({ detail: "Journal deleted." });
    }

    const item = await findByIdOrThrow(Journal, req.params.id, "Journal");
    await item.deleteOne();
    res.json({ detail: "Journal deleted." });
  })
);

router.post(
  "/gallery/",
  galleryUpload.fields([{ name: "image", maxCount: 1 }]),
  asyncHandler(async (req, res) => {
    const image = getFile(req, "image");

    if (!isDatabaseReady()) {
      const item = createRecord("gallery", {
        ...pickFields(req.body, ["title", "category", "caption"]),
        order: numberOr(req.body.order, 0),
        image_path: image ? toStoredPath(image) : "",
      });

      return res.status(201).json(serializeGalleryItem(item, req));
    }

    const item = await GalleryItem.create({
      ...pickFields(req.body, ["title", "category", "caption"]),
      order: numberOr(req.body.order, 0),
      image_path: image ? toStoredPath(image) : "",
    });

    res.status(201).json(serializeGalleryItem(item, req));
  })
);

router.put(
  "/gallery/:id/",
  galleryUpload.fields([{ name: "image", maxCount: 1 }]),
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      const image = getFile(req, "image");
      const item = updateRecord("gallery", req.params.id, (current) => ({
        ...current,
        ...pickFields(req.body, ["title", "category", "caption"]),
        order: req.body.order !== undefined ? numberOr(req.body.order, current.order) : current.order,
        image_path: image ? toStoredPath(image) : current.image_path,
      }));

      return res.json(serializeGalleryItem(item, req));
    }

    const item = await findByIdOrThrow(GalleryItem, req.params.id, "Gallery item");
    Object.assign(item, pickFields(req.body, ["title", "category", "caption"]));
    if (req.body.order !== undefined) item.order = numberOr(req.body.order, item.order);

    const image = getFile(req, "image");
    if (image) {
      item.image_path = toStoredPath(image);
    }

    await item.save();
    res.json(serializeGalleryItem(item, req));
  })
);

router.delete(
  "/gallery/:id/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      deleteRecord("gallery", req.params.id);
      return res.json({ detail: "Gallery item deleted." });
    }

    const item = await findByIdOrThrow(GalleryItem, req.params.id, "Gallery item");
    await item.deleteOne();
    res.json({ detail: "Gallery item deleted." });
  })
);

module.exports = router;
