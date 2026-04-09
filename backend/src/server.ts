import { Pool } from "pg";
import { env } from "./config/env.js";
import { createApp } from "./app/create-app.js";

const pool = new Pool({
  connectionString: env.databaseUrl,
});
let serverApp: Awaited<ReturnType<typeof createApp>>["app"] | null = null;

async function start() {
  const { app, catalogRepository } = await createApp({
    pool,
    dummyJsonBaseUrl: env.dummyJsonBaseUrl,
    yandexTranslateApiUrl: env.yandexTranslateApiUrl,
    yandexTranslateApiKey: env.yandexTranslateApiKey,
    yandexFolderId: env.yandexFolderId,
  });
  serverApp = app;

  await catalogRepository.ensureSchema();

  await app.listen({
    host: env.host,
    port: env.port,
  });
}

start().catch(async (error) => {
  console.error(error);
  await pool.end();
  process.exit(1);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, async () => {
    if (serverApp) {
      await serverApp.close();
    }
    await pool.end();
    process.exit(0);
  });
}
