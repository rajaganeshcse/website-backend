const path = require("path");
const { getCloudinary, buildCloudinaryFolder } = require("../config/cloudinary");

function uploadFileToCloudinary(file, folder) {
  if (!file || !file.buffer) {
    return Promise.resolve(null);
  }

  const targetFolder = buildCloudinaryFolder(folder);
  const originalName = String(file.originalname || "file").trim() || "file";

  return new Promise((resolve, reject) => {
    const uploadStream = getCloudinary().uploader.upload_stream(
      {
        folder: targetFolder || undefined,
        resource_type: "auto",
        use_filename: true,
        unique_filename: true,
        overwrite: false,
        filename_override: path.basename(originalName, path.extname(originalName)),
      },
      (error, result) => {
        if (error) {
          return reject(error);
        }

        resolve(result);
      }
    );

    uploadStream.end(file.buffer);
  });
}

async function uploadLocalFileToCloudinary(filePath, folder, originalName = "") {
  if (!filePath) {
    return null;
  }

  return getCloudinary().uploader.upload(filePath, {
    folder: buildCloudinaryFolder(folder) || undefined,
    resource_type: "auto",
    use_filename: true,
    unique_filename: true,
    overwrite: false,
    filename_override: path.basename(originalName || filePath, path.extname(originalName || filePath)),
  });
}

function isCloudinaryUrl(value) {
  try {
    const parsed = new URL(String(value || ""));
    return /(^|\.)cloudinary\.com$/i.test(parsed.hostname);
  } catch {
    return false;
  }
}

function parseCloudinaryAsset(value) {
  if (!isCloudinaryUrl(value)) {
    return null;
  }

  const parsed = new URL(String(value || ""));
  const segments = parsed.pathname.split("/").filter(Boolean);

  if (segments.length < 5) {
    return null;
  }

  const resourceType = segments[1];
  const deliveryType = segments[2];
  let publicIdSegments = segments.slice(3);
  const versionIndex = publicIdSegments.findIndex((segment) => /^v\d+$/.test(segment));

  if (versionIndex >= 0) {
    publicIdSegments = publicIdSegments.slice(versionIndex + 1);
  }

  if (publicIdSegments.length === 0) {
    return null;
  }

  const decodedSegments = publicIdSegments.map((segment) => decodeURIComponent(segment));
  const lastSegment = decodedSegments.pop();
  const publicId = [
    ...decodedSegments,
    resourceType === "raw" ? lastSegment : String(lastSegment || "").replace(/\.[^.]+$/, ""),
  ]
    .filter(Boolean)
    .join("/");

  if (!publicId) {
    return null;
  }

  return {
    deliveryType,
    publicId,
    resourceType,
  };
}

async function deleteCloudinaryAsset(value) {
  const asset = parseCloudinaryAsset(value);
  if (!asset) {
    return false;
  }

  await getCloudinary().uploader.destroy(asset.publicId, {
    resource_type: asset.resourceType,
    type: asset.deliveryType,
    invalidate: true,
  });

  return true;
}

module.exports = {
  deleteCloudinaryAsset,
  isCloudinaryUrl,
  parseCloudinaryAsset,
  uploadFileToCloudinary,
  uploadLocalFileToCloudinary,
};
