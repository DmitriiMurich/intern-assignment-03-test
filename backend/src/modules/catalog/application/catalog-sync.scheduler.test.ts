import assert from "node:assert/strict";
import test from "node:test";
import { setTimeout as delay } from "node:timers/promises";
import {
  CatalogSyncScheduler,
  type CatalogSyncRunner,
} from "./catalog-sync.scheduler";

const silentLogger = {
  info() {},
  warn() {},
  error() {},
};

test("runs source bootstrap once on scheduler start", async () => {
  let ensureCalls = 0;
  let refreshCalls = 0;

  const scheduler = new CatalogSyncScheduler(
    {
      async ensureSourceCatalog() {
        ensureCalls += 1;
      },
      async refreshSourceCatalog() {
        refreshCalls += 1;
        return { products: 1, categories: 1 };
      },
    } satisfies CatalogSyncRunner,
    {
      intervalMs: 1_000,
      logger: silentLogger,
    },
  );

  await scheduler.start();
  scheduler.stop();

  assert.equal(ensureCalls, 1);
  assert.equal(refreshCalls, 0);
});

test("runs scheduled catalog refresh on interval", async () => {
  let refreshCalls = 0;

  const scheduler = new CatalogSyncScheduler(
    {
      async ensureSourceCatalog() {},
      async refreshSourceCatalog() {
        refreshCalls += 1;
        return { products: 2, categories: 1 };
      },
    } satisfies CatalogSyncRunner,
    {
      intervalMs: 15,
      logger: silentLogger,
    },
  );

  await scheduler.start();
  await delay(90);
  scheduler.stop();

  assert.ok(refreshCalls >= 2);
});

test("does not run overlapping synchronizations", async () => {
  let refreshCalls = 0;

  const scheduler = new CatalogSyncScheduler(
    {
      async ensureSourceCatalog() {},
      async refreshSourceCatalog() {
        refreshCalls += 1;
        await delay(50);
        return { products: 2, categories: 1 };
      },
    } satisfies CatalogSyncRunner,
    {
      intervalMs: 10,
      logger: silentLogger,
    },
  );

  await scheduler.start();
  await delay(35);
  scheduler.stop();

  assert.equal(refreshCalls, 1);
});
