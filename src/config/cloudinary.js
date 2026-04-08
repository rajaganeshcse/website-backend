const { v2: cloudinary } = require("cloudinary");

const {
  cloudinaryApiKey,
  cloudinaryApiSecret,
  cloudinaryCloudName,
  cloudinaryFolder,
} = require("./env");

let configured = false;

function getCloudinary() {
  if (!configured) {
    cloudinary.config({
      cloud_name: cloudinaryCloudName(),
      api_key: cloudinaryApiKey(),
      api_secret: cloudinaryApiSecret(),
      secure: true,
    });
    configured = true;
  }

  return cloudinary;
}

function buildCloudinaryFolder(folder = "") {
  return [cloudinaryFolder(), String(folder || "").trim().replace(/^\/+|\/+$/g, "")]
    .filter(Boolean)
    .join("/");
}

module.exports = {
  buildCloudinaryFolder,
  getCloudinary,
};
