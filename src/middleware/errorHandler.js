const multer = require("multer");

function notFoundHandler(req, res) {
  res.status(404).json({
    detail: `Route not found: ${req.method} ${req.originalUrl}`,
  });
}

function errorHandler(error, req, res, next) {
  if (res.headersSent) {
    return next(error);
  }

  if (error instanceof multer.MulterError) {
    return res.status(400).json({ detail: error.message });
  }

  if (error.message === "Origin not allowed by CORS") {
    return res.status(403).json({ detail: error.message });
  }

  return res.status(error.statusCode || 500).json({
    detail: error.message || "Internal server error",
  });
}

module.exports = {
  notFoundHandler,
  errorHandler,
};
