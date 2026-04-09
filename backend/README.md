# Backend

This service acts as a BFF for the mobile catalog app.

It:

- fetches the source catalog from DummyJSON;
- stores the English source data in PostgreSQL;
- translates product titles, descriptions, and category titles through Yandex Translate;
- caches translated fields in PostgreSQL by language;
- invalidates translation cache after a full source sync;
- auto-loads source data on the first catalog request if the database is empty;
- applies search, category filtering, sorting, and pagination on the server.

## Endpoints

- `GET /health`
- `GET /api/v1/languages`
- `GET /api/v1/catalog?lang=en&page=1&pageSize=20&sort=price_asc`
- `POST /api/v1/catalog/sync`

## Environment

See `backend/.env.example`.

Important variables:

- `DATABASE_URL`
- `YANDEX_TRANSLATE_API_KEY`
- `YANDEX_FOLDER_ID`

Supported API languages are fixed in code: `en`, `ru`, `de`, `fr`, `es`, `it`, `pt`, `tr`, `uk`, `zh`.

## Local run

```bash
cd backend
npm install
npm run dev
```

## Docker

From the repository root:

```bash
docker compose up --build
```

The backend listens on `http://localhost:8080`.

## Swagger

Swagger UI is available at `http://localhost:8080/docs`.
