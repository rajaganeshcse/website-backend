const path = require("path");

const { backendRoot } = require("../middleware/upload");

function toStoredPath(file) {
  if (!file || !file.path) {
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

  return `${req.protocol}://${req.get("host")}${storedPath}`;
}

module.exports = {
  toStoredPath,
  absoluteUrl,
};
