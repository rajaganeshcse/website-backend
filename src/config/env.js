function requiredEnv(name) {
  const value = String(process.env[name] || "").trim();

  if (!value) {
    throw new Error(`${name} is required.`);
  }

  return value;
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

function validateSecurityConfig() {
  adminUsernames();
  adminPassword();
  jwtSecret();
}

module.exports = {
  adminPassword,
  adminUsernames,
  jwtSecret,
  validateSecurityConfig,
};
