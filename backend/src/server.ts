import { Pool } from "pg";
import { env } from "./config/env";
import { createApp } from "./app/create-app";
import { CatalogSyncScheduler } from "./modules/catalog/application/catalog-sync.scheduler";
import { CurrencyRateSyncScheduler } from "./modules/catalog/application/currency-rate-sync.scheduler";

const pool = new Pool({
  connectionString: env.databaseUrl,
});
let serverApp: Awaited<ReturnType<typeof createApp>>["app"] | null = null;
let catalogSyncScheduler: CatalogSyncScheduler | null = null;
let currencyRateSyncScheduler: CurrencyRateSyncScheduler | null = null;

async function start() {
  const { app, catalogRepository, catalogSyncService, currencyRateService } = await createApp({
    pool,
    dummyJsonBaseUrl: env.dummyJsonBaseUrl,
    libreTranslateUrl: env.libreTranslateUrl,
  });
  serverApp = app;

  await catalogRepository.ensureSchema();
  catalogSyncScheduler = new CatalogSyncScheduler(catalogSyncService);
  await catalogSyncScheduler.start();
  currencyRateSyncScheduler = new CurrencyRateSyncScheduler(currencyRateService);
  await currencyRateSyncScheduler.start();

  await app.listen({
    host: env.host,
    port: env.port,
  });
}

start().catch(async (error) => {
  console.error(error);
  catalogSyncScheduler?.stop();
  currencyRateSyncScheduler?.stop();
  await pool.end();
  process.exit(1);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, async () => {
    catalogSyncScheduler?.stop();
    currencyRateSyncScheduler?.stop();
    if (serverApp) {
      await serverApp.close();
    }
    await pool.end();
    process.exit(0);
  });
}
