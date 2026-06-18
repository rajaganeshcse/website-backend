# Backend

This backend has been ported to Spring Boot while keeping the existing API routes, payload shapes, auth flow, MongoDB usage, Cloudinary uploads, and local JSON fallback behavior used by the React frontend.

## Stack

- Java 17+
- Spring Boot
- MongoDB Atlas
- Custom JWT admin auth
- Multipart upload handling
- Cloudinary media storage

## Run

```powershell
mvn spring-boot:run
```

The API listens on `http://127.0.0.1:8000` by default.

## Environment

The app reads `backend/.env` automatically. Keep using the same environment variables that the previous backend used:

- `PORT`
- `MONGODB_URI`
- `JWT_SECRET`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `CLIENT_URL`
- `PUBLIC_BASE_URL`
- `UPLOAD_AUTO_DELETE_HOURS`
- `UPLOAD_CLEANUP_INTERVAL_MINUTES`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `CLOUDINARY_FOLDER`

## Build

```powershell
mvn -DskipTests compile
```

## Notes

- The frontend API contract stays the same.
- If MongoDB is unavailable, the backend falls back to `src/data/localStore.json`.
- The current backend runtime lives under `src/main/java`, with `src/data/localStore.json` acting as the local fallback store.
