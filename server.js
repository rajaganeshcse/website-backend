require("dotenv").config();

const http = require("http");
const app = require("./src/app");
const connectToDatabase = require("./src/config/db");

const PORT = Number(process.env.PORT) || 8000;
const RETRY_DELAY_MS = 15000;

let isConnecting = false;

async function connectWithRetry() {
  if (isConnecting) {
    return;
  }

  isConnecting = true;

  try {
    await connectToDatabase();
  } catch (error) {
    console.error(`MongoDB connection failed: ${error.message}`);
    console.error(`Retrying MongoDB connection in ${RETRY_DELAY_MS / 1000} seconds...`);
    setTimeout(connectWithRetry, RETRY_DELAY_MS);
  } finally {
    isConnecting = false;
  }
}

async function start() {
  const server = http.createServer(app);

  server.on("error", (error) => {
    if (error.code === "EADDRINUSE") {
      console.error(`Port ${PORT} is already in use. Stop the existing process or change PORT in backend/.env.`);
      process.exit(1);
    }

    console.error("Failed to start backend:", error.message);
    process.exit(1);
  });

  server.listen(PORT, () => {
    console.log(`Portfolio API running on http://127.0.0.1:${PORT}`);
  });

  connectWithRetry();
}

start();
