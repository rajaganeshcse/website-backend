const fs = require("fs");
const path = require("path");
const multer = require("multer");

const backendRoot = path.join(__dirname, "..", "..");
const mediaRoot = path.join(backendRoot, "media");

function safeName(name) {
  return name.replace(/[^a-zA-Z0-9.\-_]/g, "-").toLowerCase();
}

function createUploader(folder) {
  const storage = multer.diskStorage({
    destination(req, file, callback) {
      const directory = path.join(mediaRoot, folder);
      fs.mkdirSync(directory, { recursive: true });
      callback(null, directory);
    },
    filename(req, file, callback) {
      const ext = path.extname(file.originalname || "");
      const baseName = path.basename(file.originalname || "file", ext);
      const nextName = `${Date.now()}-${Math.round(Math.random() * 1e9)}-${safeName(baseName)}${ext}`;
      callback(null, nextName);
    },
  });

  return multer({
    storage,
    limits: {
      fileSize: 25 * 1024 * 1024,
    },
  });
}

module.exports = {
  createUploader,
  backendRoot,
};
