function normalizeOrigin(value) {
  const rawValue = String(value || "").trim();
  if (!rawValue) {
    return "";
  }

  try {
    return new URL(rawValue).origin.toLowerCase();
  } catch {
    return rawValue.replace(/\/+$/, "").toLowerCase();
  }
}

function publicBaseUrl() {
  return normalizeOrigin(process.env.PUBLIC_BASE_URL || process.env.BACKEND_PUBLIC_URL || "");
}

function requestProtocol(req) {
  const forwardedProto = String(req.get("x-forwarded-proto") || "")
    .split(",")[0]
    .trim();

  return forwardedProto || req.protocol || "http";
}

function requestHost(req) {
  const forwardedHost = String(req.get("x-forwarded-host") || "")
    .split(",")[0]
    .trim();

  return forwardedHost || req.get("host") || "";
}

function requestOrigin(req) {
  return publicBaseUrl() || `${requestProtocol(req)}://${requestHost(req)}`;
}

module.exports = {
  normalizeOrigin,
  requestOrigin,
};
