require("dotenv").config();

const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");

const connectToDatabase = require("./src/config/db");
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
} = require("./src/data/seedData");
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
} = require("./src/models/contentModels");
const { uploadLocalFileToCloudinary } = require("./src/utils/cloudinaryMedia");

const DEFAULT_RESUME_SOURCE = "C:\\Users\\HP\\OneDrive\\Desktop\\Rajaganesh Resume (2).pdf";

async function uploadResumeToCloudinary() {
  const sourcePath = process.env.SEED_RESUME_PATH || DEFAULT_RESUME_SOURCE;

  if (!fs.existsSync(sourcePath)) {
    console.warn(`Resume PDF not found at: ${sourcePath}`);
    return { storedPath: "", fileName: "" };
  }

  const uploaded = await uploadLocalFileToCloudinary(sourcePath, "hero", path.basename(sourcePath));

  return {
    storedPath: uploaded?.secure_url || "",
    fileName: path.basename(sourcePath),
  };
}

async function replaceCollection(Model, documents) {
  await Model.deleteMany({});
  if (documents.length > 0) {
    await Model.insertMany(documents);
  }
}

async function main() {
  await connectToDatabase();

  const resume = await uploadResumeToCloudinary();

  await Hero.deleteMany({});
  await Hero.create({
    ...heroSeed,
    resume_path: resume.storedPath,
    resume_name: resume.fileName,
  });

  await replaceCollection(Skill, skillSeed);
  await replaceCollection(Project, projectSeed);
  await replaceCollection(AppItem, appSeed);
  await replaceCollection(Education, educationSeed);
  await replaceCollection(Internship, internshipSeed);
  await replaceCollection(Certification, certificationSeed);
  await replaceCollection(Workshop, workshopSeed);
  await replaceCollection(Journal, journalSeed);
  await replaceCollection(GalleryItem, gallerySeed);

  console.log("Seed completed successfully.");
  console.log(`Hero: 1`);
  console.log(`Skills: ${skillSeed.length}`);
  console.log(`Projects: ${projectSeed.length}`);
  console.log(`Apps: ${appSeed.length}`);
  console.log(`Education: ${educationSeed.length}`);
  console.log(`Internships: ${internshipSeed.length}`);
  console.log(`Certifications: ${certificationSeed.length}`);
  console.log(`Workshops: ${workshopSeed.length}`);
  console.log(`Journals: ${journalSeed.length}`);
  console.log(`Gallery items: ${gallerySeed.length}`);
  if (resume.storedPath) {
    console.log(`Resume uploaded to ${resume.storedPath}`);
  }
}

main()
  .catch((error) => {
    console.error("Seed failed:", error.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    await mongoose.disconnect().catch(() => {});
  });
