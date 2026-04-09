const { absoluteUrl } = require("./files");

function idOf(document) {
  return String(document._id || document.id || "");
}

function splitTech(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function serializeHero(hero, req) {
  if (!hero) {
    return null;
  }

  return {
    id: idOf(hero),
    name: hero.name,
    title: hero.title,
    bio: hero.bio,
    email: hero.email,
    phone: hero.phone,
    github: hero.github,
    linkedin: hero.linkedin,
    leetcode: hero.leetcode,
    instagram: hero.instagram,
    portfolio: hero.portfolio,
    location: hero.location,
    college: hero.college,
    address: hero.address,
    photo_url: absoluteUrl(req, hero.photo_path),
    site_icon_url: absoluteUrl(req, hero.site_icon_path),
    resume_url: absoluteUrl(req, hero.resume_path),
    resume_name: hero.resume_name || "",
  };
}

function serializeSkill(skill) {
  return {
    id: idOf(skill),
    category: skill.category,
    name: skill.name,
    level: skill.level,
    order: skill.order || 0,
  };
}

function serializeProject(project, req) {
  const imageUrls = (project.image_paths || []).map((item) => absoluteUrl(req, item)).filter(Boolean);

  return {
    id: idOf(project),
    title: project.title,
    description: project.description,
    tech_stack: project.tech_stack,
    tech_list: splitTech(project.tech_stack),
    github_url: project.github_url,
    live_url: project.live_url,
    video_url: project.video_url,
    image_urls: imageUrls,
    image_url: imageUrls[0] || "",
    image_url2: imageUrls[1] || "",
    image_url3: imageUrls[2] || "",
    order: project.order || 0,
  };
}

function serializeApp(app, req) {
  const screenshots = (app.screenshot_paths || []).map((item) => absoluteUrl(req, item)).filter(Boolean);

  return {
    id: idOf(app),
    title: app.title,
    description: app.description,
    dashboard_url: app.dashboard_url,
    github_url: app.github_url,
    playstore_url: app.playstore_url,
    cover_image_url: absoluteUrl(req, app.cover_image_path),
    screenshot_urls: screenshots,
    screenshot_url1: screenshots[0] || "",
    screenshot_url2: screenshots[1] || "",
    screenshot_url3: screenshots[2] || "",
    apk_url: absoluteUrl(req, app.apk_path),
    apk_name: app.apk_name || "",
    order: app.order || 0,
  };
}

function serializeEducation(item, req) {
  const documents = (item.documents || [])
    .map((document, index) => ({
      id: `${idOf(item)}-doc-${index + 1}`,
      title: document.title || document.pdf_name || `Document ${index + 1}`,
      pdf_url: absoluteUrl(req, document.pdf_path),
      pdf_download_url: absoluteUrl(req, document.pdf_path),
      pdf_name: document.pdf_name || "",
    }))
    .filter((document) => document.pdf_url);

  const legacyPdfUrl = absoluteUrl(req, item.result_pdf_path);
  const primaryDocument = documents[0] || null;

  return {
    id: idOf(item),
    degree: item.degree,
    institution: item.institution,
    year: item.year,
    score: item.score,
    documents,
    result_pdf_url: primaryDocument?.pdf_url || legacyPdfUrl,
    result_pdf_download_url: primaryDocument?.pdf_download_url || legacyPdfUrl,
    result_pdf_name: primaryDocument?.pdf_name || item.result_pdf_name || "",
    order: item.order || 0,
  };
}

function serializeInternship(item) {
  return {
    id: idOf(item),
    role: item.role,
    company: item.company,
    duration: item.duration,
    description: item.description,
    order: item.order || 0,
  };
}

function serializeCertification(item, req) {
  return {
    id: idOf(item),
    name: item.name,
    issuer: item.issuer,
    year: item.year,
    description: item.description,
    credential_url: item.credential_url,
    image_url: absoluteUrl(req, item.image_path),
    order: item.order || 0,
  };
}

function serializeWorkshop(item, req) {
  const images = (item.image_paths || []).map((value) => absoluteUrl(req, value)).filter(Boolean);

  return {
    id: idOf(item),
    title: item.title,
    organizer: item.organizer,
    date: item.date,
    description: item.description,
    link_url: item.link_url,
    image_urls: images,
    image_url: images[0] || "",
    image_url2: images[1] || "",
    image_url3: images[2] || "",
    order: item.order || 0,
  };
}

function serializeGalleryItem(item, req) {
  return {
    id: idOf(item),
    title: item.title,
    category: item.category,
    caption: item.caption,
    order: item.order || 0,
    image_url: absoluteUrl(req, item.image_path),
  };
}

function serializeJournal(item, req) {
  const pdfUrl = absoluteUrl(req, item.pdf_path);

  return {
    id: idOf(item),
    title: item.title,
    details: item.details || "",
    pdf_url: pdfUrl,
    pdf_download_url: pdfUrl,
    pdf_name: item.pdf_name || "",
    order: item.order || 0,
  };
}

function serializeMessage(item) {
  return {
    id: idOf(item),
    name: item.name,
    email: item.email,
    message: item.message,
    sent_at: item.sent_at || item.createdAt,
    is_read: Boolean(item.is_read),
  };
}

module.exports = {
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
};
