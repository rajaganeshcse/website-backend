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

function normalizeOrigin(value) {
  return String(value || "").trim().replace(/\/+$/, "");
}

function parseAllowedOrigins(value) {
  return String(value || "")
    .split(",")
    .map(normalizeOrigin)
    .filter(Boolean);
}

const allowedOrigins = new Set([
  ...parseAllowedOrigins(process.env.CLIENT_URL),
  "https://rajaganeshcse.vercel.app",
  "https://react-website-five-theta.vercel.app",
  "http://localhost:3000",
  "http://127.0.0.1:3000",
].map(normalizeOrigin));

const corsOptions = {
  origin: function (origin, callback) {
    if (!origin) {
      return callback(null, true);
    }

    if (allowedOrigins.has(normalizeOrigin(origin))) {
      return callback(null, true);
    }

    return callback(new Error("Origin not allowed by CORS"), false);
  },
  credentials: true,
};

app.set("trust proxy", 1);

app.use(cors(corsOptions));
app.options("*", cors(corsOptions));

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
