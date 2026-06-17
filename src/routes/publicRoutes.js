const express = require("express");

const asyncHandler = require("../utils/asyncHandler");
const { DEFAULT_HERO } = require("../utils/defaults");
const { isDatabaseReady } = require("../utils/dbState");
const { getPortfolioResponse, getGalleryResponse, createMessage, readStore } = require("../data/localStore");
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

async function getOrCreateHero() {
  let hero = await Hero.findOne().sort({ createdAt: 1 });
  if (!hero) {
    hero = await Hero.create(DEFAULT_HERO);
  }
  return hero;
}

function withCacheBust(url, value) {
  if (!url) {
    return "";
  }

  const version = value ? new Date(value).getTime() : 0;
  if (!Number.isFinite(version) || version <= 0) {
    return url;
  }

  const separator = url.includes("?") ? "&" : "?";
  return `${url}${separator}v=${version}`;
}

router.get(
  "/health/",
  asyncHandler(async (req, res) => {
    res.json({
      status: "ok",
      date: new Date().toISOString(),
    });
  })
);

router.get(
  "/portfolio/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      return res.json(getPortfolioResponse(req));
    }

    const hero = await getOrCreateHero();
    const [skills, projects, apps, education, internships, certifications, workshops, journals] = await Promise.all([
      Skill.find().sort({ category: 1, order: 1, createdAt: 1 }),
      Project.find().sort({ order: 1, createdAt: -1 }),
      AppItem.find().sort({ order: 1, createdAt: -1 }),
      Education.find().sort({ order: 1, createdAt: -1 }),
      Internship.find().sort({ order: 1, createdAt: -1 }),
      Certification.find().sort({ order: 1, createdAt: -1 }),
      Workshop.find().sort({ order: 1, createdAt: -1 }),
      Journal.find().sort({ order: 1, createdAt: -1 }),
    ]);

    res.json({
      hero: serializeHero(hero, req),
      skills: skills.map(serializeSkill),
      projects: projects.map((item) => serializeProject(item, req)),
      apps: apps.map((item) => serializeApp(item, req)),
      education: education.map((item) => serializeEducation(item, req)),
      internships: internships.map(serializeInternship),
      certifications: certifications.map((item) => serializeCertification(item, req)),
      workshops: workshops.map((item) => serializeWorkshop(item, req)),
      journals: journals.map((item) => serializeJournal(item, req)),
    });
  })
);

router.get(
  "/site-icon/",
  asyncHandler(async (req, res) => {
    const rawHero = !isDatabaseReady() ? readStore().hero : await getOrCreateHero();
    const hero = serializeHero(rawHero, req);
    const targetUrl = hero?.site_icon_url || hero?.photo_url || "";

    if (!targetUrl) {
      return res.status(404).json({ detail: "Site icon not found." });
    }

    res.set("Cache-Control", "no-store, max-age=0");
    return res.redirect(withCacheBust(targetUrl, rawHero?.updatedAt || rawHero?.createdAt));
  })
);

router.get(
  "/gallery/",
  asyncHandler(async (req, res) => {
    if (!isDatabaseReady()) {
      return res.json(getGalleryResponse(req));
    }

    const images = await GalleryItem.find().sort({ category: 1, order: 1, createdAt: -1 });
    res.json(images.map((item) => serializeGalleryItem(item, req)));
  })
);

router.post(
  "/contact/",
  asyncHandler(async (req, res) => {
    const name = String(req.body.name || "").trim();
    const email = String(req.body.email || "").trim();
    const message = String(req.body.message || "").trim();

    if (!name || !email || !message) {
      return res.status(400).json({ detail: "Name, email, and message are required." });
    }

    if (!isDatabaseReady()) {
      createMessage({ name, email, message });
      return res.status(201).json({ message: "Sent!" });
    }

    const created = await Message.create({
      name,
      email,
      message,
      sent_at: new Date(),
      is_read: false,
    });

    return res.status(201).json(serializeMessage(created));
  })
);

module.exports = router;
