const express = require("express");
const cors = require("cors");
const morgan = require("morgan");
const path = require("path");
const publicRoutes = require("./routes/publicRoutes");
const authRoutes = require("./routes/authRoutes");
const adminRoutes = require("./routes/adminRoutes");
const { notFoundHandler, errorHandler } = require("./middleware/errorHandler");

const app = express();
const mediaRoot = path.join(__dirname, "..", "media");
const allowedOrigins = (process.env.CLIENT_URL || "")
  .split(",")
  .map((value) => value.trim())
  .filter(Boolean);

app.use(
  cors({
    origin(origin, callback) {
      if (!origin) {
        return callback(null, true);
      }

      const isLocal = /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(origin);
      if (isLocal || allowedOrigins.includes(origin)) {
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
