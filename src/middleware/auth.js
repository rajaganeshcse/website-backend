const jwt = require("jsonwebtoken");

function jwtSecret() {
  return process.env.JWT_SECRET || "replace-this-with-a-strong-secret";
}

function signAdminToken(username) {
  return jwt.sign({ username, role: "admin" }, jwtSecret(), {
    expiresIn: "7d",
  });
}

function requireAdminAuth(req, res, next) {
  const authHeader = req.headers.authorization || "";
  const [scheme, token] = authHeader.split(" ");

  if (scheme !== "Bearer" || !token) {
    return res.status(401).json({ detail: "Authentication credentials were not provided." });
  }

  try {
    req.user = jwt.verify(token, jwtSecret());
    return next();
  } catch (error) {
    return res.status(401).json({ detail: "Invalid or expired token." });
  }
}

module.exports = {
  signAdminToken,
  requireAdminAuth,
};
