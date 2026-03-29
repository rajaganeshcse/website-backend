const express = require("express");
const cors = require("cors");
const morgan = require("morgan");
const path = require("path");
const publicRoutes = require("./routes/publicRoutes");
const authRoutes = require("./routes/authRoutes");
const adminRoutes = require("./routes/adminRoutes");
const { notFoundHandler, errorHandler } = require("./middleware/errorHandler");
const { normalizeOrigin } = require("./utils/origin");

const app = express();
const mediaRoot = path.join(__dirname, "..", "media");
const allowedOrigins = (process.env.CLIENT_URL || "")
  .split(",")
  .map((value) => normalizeOrigin(value))
  .filter(Boolean);

// Respect proxy-forwarded HTTPS headers from Render so generated asset URLs use https in production.
app.set("trust proxy", 1);

app.use(
  cors({
    origin(origin, callback) {
      if (!origin) {
        return callback(null, true);
      }

      const normalizedOrigin = normalizeOrigin(origin);
      const isLocal = /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(normalizedOrigin);

      if (isLocal || allowedOrigins.includes(normalizedOrigin)) {
        return callback(null, true);
      }

      return callback(new Error("Origin not allowed by CORS"));
    },
  })
);

app.use(morgan("dev"));
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true }));
app.use("/media", express.static(mediaRoot));

app.get("/", (req, res) => {
  res.json({
    message: "Portfolio backend is running",
    health: "/api/health/",
  });
});

app.use("/api/auth", authRoutes);
app.use("/api/admin", adminRoutes);
app.use("/api", publicRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
