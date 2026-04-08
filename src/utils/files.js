const path = require("path");

const { backendRoot } = require("../middleware/upload");
const { requestOrigin } = require("./origin");

function toStoredPath(file) {
  if (!file) {
    return "";
  }

  if (file.cloudinaryUrl) {
    return String(file.cloudinaryUrl || "");
  }

  if (!file.path) {
    return "";
  }

  const relative = path.relative(backendRoot, file.path).replace(/\\/g, "/");
  return `/${relative}`;
}

function absoluteUrl(req, storedPath) {
  if (!storedPath) {
    return "";
  }

  if (/^https?:\/\//i.test(storedPath)) {
    return storedPath;
  }

  return `${requestOrigin(req)}${storedPath}`;
}

module.exports = {
  toStoredPath,
  requestOrigin,
  absoluteUrl,
};
