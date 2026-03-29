const mongoose = require("mongoose");

async function connectToDatabase() {
  const mongoUri = process.env.MONGODB_URI;

  if (!mongoUri) {
    throw new Error("MONGODB_URI is missing. Add your MongoDB Atlas URI to backend/.env");
  }

  await mongoose.connect(mongoUri, {
    autoIndex: true,
  });

  console.log("Connected to MongoDB Atlas");
}

module.exports = connectToDatabase;
