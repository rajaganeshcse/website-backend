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

## Render Deployment

Set these environment variables in Render:

- `PORT=10000`
- `MONGODB_URI=<your atlas uri with the database name included>`
- `JWT_SECRET=<strong secret>`
- `ADMIN_USERNAME=<admin username>`
- `ADMIN_PASSWORD=<admin password>`
- `CLIENT_URL=https://your-frontend.vercel.app`
- `PUBLIC_BASE_URL=https://your-backend.onrender.com`
- `NODE_ENV=production`

Deployment notes:

- `CLIENT_URL` supports comma-separated origins and ignores trailing slashes.
- `PUBLIC_BASE_URL` makes generated media and file URLs use your public Render HTTPS URL.
- The backend trusts proxy headers so Render/Vercel traffic resolves to HTTPS correctly.

## Auto-delete uploads

You can automatically delete uploaded images and PDFs after a few hours.

- Set `UPLOAD_AUTO_DELETE_HOURS=3` to delete newly uploaded images/PDFs about 3 hours after upload.
- Leave `UPLOAD_AUTO_DELETE_HOURS=0` to keep uploads permanently.
- `UPLOAD_CLEANUP_INTERVAL_MINUTES` controls how often the cleanup worker checks for expired uploads.
- You can override the default per request by sending `auto_delete_hours` in the upload form data.

Important:

- This feature removes both the MongoDB file reference and the physical file inside `backend/media/`.
- If you turn it on globally, portfolio uploads such as gallery images, certificates, journals, education PDFs, hero photo, and resume can disappear after that time.
