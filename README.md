# Backend

This folder now contains a JavaScript backend for the existing React frontend.

## Stack

- Node.js
- Express
- MongoDB Atlas via Mongoose
- JWT admin auth
- Multer for file uploads

## Run

```powershell
npm install
npm run dev
```

The API listens on `http://127.0.0.1:8000`.

## Seed Database

```powershell
npm run seed
```

The seed script uses the resume PDF to populate MongoDB and copies the resume into `backend/media/resumes/`.

## Notes

- The frontend does not need changes.
- Old Django files are still in this folder for reference.
- The new runtime backend is `server.js` plus the files under `src/`.
