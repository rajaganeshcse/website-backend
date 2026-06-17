function requiredEnv(name) {
  const value = String(process.env[name] || "").trim();

  if (!value) {
    throw new Error(`${name} is required.`);
  }

  return value;
}

function requiredEnvAny(names) {
  for (const name of names) {
    const value = String(process.env[name] || "").trim();
    if (value) {
      return value;
    }
  }

  throw new Error(`${names[0]} is required.`);
}

function nonNegativeNumberEnv(name, fallback) {
  const rawValue = String(process.env[name] ?? "").trim();

  if (!rawValue) {
    return fallback;
  }

  const parsedValue = Number(rawValue);
  if (!Number.isFinite(parsedValue) || parsedValue < 0) {
    throw new Error(`${name} must be a number greater than or equal to 0.`);
  }

  return parsedValue;
}

function adminUsernames() {
  return requiredEnv("ADMIN_USERNAME")
    .split(",")
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean);
}

function adminPassword() {
  return requiredEnv("ADMIN_PASSWORD");
}

function jwtSecret() {
  return requiredEnv("JWT_SECRET");
}

function uploadAutoDeleteHours() {
  return nonNegativeNumberEnv("UPLOAD_AUTO_DELETE_HOURS", 0);
}

function uploadCleanupIntervalMinutes() {
  return nonNegativeNumberEnv("UPLOAD_CLEANUP_INTERVAL_MINUTES", 5);
}

function cloudinaryCloudName() {
  return requiredEnvAny(["CLOUDINARY_CLOUD_NAME", "CLOUDINARY_NAME", "CLOUD_NAME"]);
}

function cloudinaryApiKey() {
  return requiredEnvAny(["CLOUDINARY_API_KEY", "CLOUDINARY_KEY"]);
}

function cloudinaryApiSecret() {
  return requiredEnvAny(["CLOUDINARY_API_SECRET", "CLOUDINARY_SECRET"]);
}

function cloudinaryFolder() {
  return String(process.env.CLOUDINARY_FOLDER || "portfolio")
    .trim()
    .replace(/^\/+|\/+$/g, "");
}

function validateSecurityConfig() {
  adminUsernames();
  adminPassword();
  jwtSecret();
  uploadAutoDeleteHours();
  uploadCleanupIntervalMinutes();
}

module.exports = {
  adminPassword,
  adminUsernames,
  cloudinaryApiKey,
  cloudinaryApiSecret,
  cloudinaryCloudName,
  cloudinaryFolder,
  jwtSecret,
  uploadAutoDeleteHours,
  uploadCleanupIntervalMinutes,
  validateSecurityConfig,
};
