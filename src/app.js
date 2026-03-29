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
const allowedOrigins = [
  "https://rajaganeshcse.vercel.app",
  "http://localhost:3000",
];

app.set("trust proxy", 1);

app.use(
  cors({
    origin: function (origin, callback) {
      if (!origin) {
        return callback(null, true);
      }

      if (allowedOrigins.includes(origin)) {
        return callback(null, true);
      } else {
        return callback(new Error("CORS not allowed"), false);
      }
    },
    credentials: true,
  })
);

app.options("*", cors());

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
