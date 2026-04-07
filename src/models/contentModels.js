const mongoose = require("mongoose");

const educationDocumentSchema = new mongoose.Schema(
  {
    title: { type: String, default: "" },
    pdf_path: { type: String, default: "" },
    pdf_name: { type: String, default: "" },
  },
  { _id: false }
);

const heroSchema = new mongoose.Schema(
  {
    name: { type: String, default: "Rajaganesh T" },
    title: { type: String, default: "Aspiring Android App Developer" },
    bio: { type: String, default: "" },
    email: { type: String, default: "rajaganeshcse2005@gmail.com" },
    phone: { type: String, default: "+91 6382641748" },
    github: { type: String, default: "https://github.com/rajaganeshcse" },
    linkedin: { type: String, default: "https://www.linkedin.com/in/rajaganesh-t-835a21364" },
    leetcode: { type: String, default: "" },
    instagram: { type: String, default: "" },
    portfolio: { type: String, default: "" },
    location: { type: String, default: "Vanrasankuppam, Cuddalore, Tamil Nadu, India" },
    college: {
      type: String,
      default: "Dhanalakshmi Srinivasan Engineering College, Perambalur - B.E CSE (2023 - 2027)",
    },
    address: {
      type: String,
      default: "70, MaariAmman Kovil Street, Vanrasankuppam, Cuddalore (TK), Cuddalore (DT) - 607102",
    },
    photo_path: { type: String, default: "" },
    resume_path: { type: String, default: "" },
    resume_name: { type: String, default: "" },
  },
  { timestamps: true }
);

const skillSchema = new mongoose.Schema(
  {
    category: { type: String, required: true },
    name: { type: String, required: true },
    level: { type: Number, default: 80 },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const projectSchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    description: { type: String, default: "" },
    tech_stack: { type: String, default: "" },
    github_url: { type: String, default: "" },
    live_url: { type: String, default: "" },
    video_url: { type: String, default: "" },
    image_paths: { type: [String], default: [] },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const appSchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    description: { type: String, default: "" },
    dashboard_url: { type: String, default: "" },
    github_url: { type: String, default: "" },
    playstore_url: { type: String, default: "" },
    cover_image_path: { type: String, default: "" },
    screenshot_paths: { type: [String], default: [] },
    apk_path: { type: String, default: "" },
    apk_name: { type: String, default: "" },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const educationSchema = new mongoose.Schema(
  {
    degree: { type: String, required: true },
    institution: { type: String, required: true },
    year: { type: String, default: "" },
    score: { type: String, default: "" },
    result_pdf_path: { type: String, default: "" },
    result_pdf_name: { type: String, default: "" },
    documents: { type: [educationDocumentSchema], default: [] },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const internshipSchema = new mongoose.Schema(
  {
    role: { type: String, required: true },
    company: { type: String, default: "" },
    duration: { type: String, default: "" },
    description: { type: String, default: "" },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const certificationSchema = new mongoose.Schema(
  {
    name: { type: String, required: true },
    issuer: { type: String, default: "" },
    year: { type: String, default: "" },
    description: { type: String, default: "" },
    credential_url: { type: String, default: "" },
    image_path: { type: String, default: "" },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const workshopSchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    organizer: { type: String, default: "" },
    date: { type: String, default: "" },
    description: { type: String, default: "" },
    link_url: { type: String, default: "" },
    image_paths: { type: [String], default: [] },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const gallerySchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    category: { type: String, default: "certificate" },
    caption: { type: String, default: "" },
    order: { type: Number, default: 0 },
    image_path: { type: String, default: "" },
  },
  { timestamps: true }
);

const journalSchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    details: { type: String, default: "" },
    pdf_path: { type: String, default: "" },
    pdf_name: { type: String, default: "" },
    order: { type: Number, default: 0 },
  },
  { timestamps: true }
);

const messageSchema = new mongoose.Schema(
  {
    name: { type: String, required: true },
    email: { type: String, required: true },
    message: { type: String, required: true },
    sent_at: { type: Date, default: Date.now },
    is_read: { type: Boolean, default: false },
  },
  { timestamps: true }
);

const mediaAssetSchema = new mongoose.Schema(
  {
    owner_model: { type: String, required: true },
    owner_id: { type: String, required: true },
    stored_path: { type: String, required: true },
    source_field: { type: String, default: "" },
    original_name: { type: String, default: "" },
    media_kind: { type: String, enum: ["image", "pdf"], required: true },
    expires_at: { type: Date, required: true },
    deleted_at: { type: Date, default: null },
    cleanup_status: {
      type: String,
      enum: ["scheduled", "deleted", "failed"],
      default: "scheduled",
    },
    cleanup_error: { type: String, default: "" },
  },
  { timestamps: true }
);

mediaAssetSchema.index(
  { owner_model: 1, owner_id: 1, stored_path: 1 },
  { unique: true }
);
mediaAssetSchema.index({ cleanup_status: 1, expires_at: 1 });
mediaAssetSchema.index({ deleted_at: 1 }, { expireAfterSeconds: 60 * 60 * 24 * 7 });

const Hero = mongoose.models.Hero || mongoose.model("Hero", heroSchema);
const Skill = mongoose.models.Skill || mongoose.model("Skill", skillSchema);
const Project = mongoose.models.Project || mongoose.model("Project", projectSchema);
const AppItem = mongoose.models.AppItem || mongoose.model("AppItem", appSchema);
const Education = mongoose.models.Education || mongoose.model("Education", educationSchema);
const Internship = mongoose.models.Internship || mongoose.model("Internship", internshipSchema);
const Certification = mongoose.models.Certification || mongoose.model("Certification", certificationSchema);
const Workshop = mongoose.models.Workshop || mongoose.model("Workshop", workshopSchema);
const GalleryItem = mongoose.models.GalleryItem || mongoose.model("GalleryItem", gallerySchema);
const Journal = mongoose.models.Journal || mongoose.model("Journal", journalSchema);
const Message = mongoose.models.Message || mongoose.model("Message", messageSchema);
const MediaAsset = mongoose.models.MediaAsset || mongoose.model("MediaAsset", mediaAssetSchema);

module.exports = {
  Hero,
  Skill,
  Project,
  AppItem,
  Education,
  Internship,
  Certification,
  Workshop,
  GalleryItem,
  Journal,
  Message,
  MediaAsset,
};
