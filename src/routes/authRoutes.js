const express = require("express");

const asyncHandler = require("../utils/asyncHandler");
const { signAdminToken } = require("../middleware/auth");

const router = express.Router();

router.post(
  "/login/",
  asyncHandler(async (req, res) => {
    const username = String(req.body.username || "").trim();
    const password = String(req.body.password || "").trim();

    const adminUsernames = String(process.env.ADMIN_USERNAME || "admin")
      .split(",")
      .map((value) => value.trim().toLowerCase())
      .filter(Boolean);
    const adminPassword = process.env.ADMIN_PASSWORD || "ChangeMe123!";
    const normalizedUsername = username.toLowerCase();

    if (!adminUsernames.includes(normalizedUsername) || password !== adminPassword) {
      return res.status(401).json({ detail: "Invalid username or password." });
    }

    return res.json({
      access: signAdminToken(username),
      username,
    });
  })
);

module.exports = router;
