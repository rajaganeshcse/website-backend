const path = require("path");
const multer = require("multer");

const backendRoot = path.join(__dirname, "..", "..");

function createUploader(folder) {
  return multer({
    storage: multer.memoryStorage(),
    limits: {
      fileSize: 25 * 1024 * 1024,
    },
  });
}

module.exports = {
  createUploader,
  backendRoot,
};
