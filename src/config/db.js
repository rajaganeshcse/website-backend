const mongoose = require("mongoose");
const { startMediaCleanupScheduler } = require("../utils/mediaCleanup");

async function connectToDatabase() {
  const mongoUri = process.env.MONGODB_URI;

  if (!mongoUri) {
    throw new Error("MONGODB_URI is missing. Add your MongoDB Atlas URI to backend/.env");
  }

  await mongoose.connect(mongoUri, {
    autoIndex: true,
  });

  startMediaCleanupScheduler();
  console.log("Connected to MongoDB Atlas");
}

module.exports = connectToDatabase;
