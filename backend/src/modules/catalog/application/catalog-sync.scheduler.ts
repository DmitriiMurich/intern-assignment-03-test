import type { Logger } from "../../../shared/types/logger";

const hourlyIntervalMs = 60 * 60 * 1000;

export interface CatalogSyncRunner {
  ensureSourceCatalog(): Promise<void>;
  refreshSourceCatalog(): Promise<{ products: number; categories: number }>;
}

interface CatalogSyncSchedulerOptions {
  intervalMs?: number;
  logger?: Logger;
}

export class CatalogSyncScheduler {
  private readonly intervalMs: number;
  private readonly logger: Logger;
  private timer: NodeJS.Timeout | null = null;
  private syncInFlight: Promise<void> | null = null;

  constructor(
    private readonly syncRunner: CatalogSyncRunner,
    options: CatalogSyncSchedulerOptions = {},
  ) {
    this.intervalMs = options.intervalMs ?? hourlyIntervalMs;
    this.logger = options.logger ?? console;
  }

  async start(): Promise<void> {
    if (this.timer) {
      return;
    }

    await this.runSync("startup", async () => {
      await this.syncRunner.ensureSourceCatalog();
      this.logger.info("Catalog startup synchronization completed");
    });

    this.timer = setInterval(() => {
      void this.runSync("scheduled", async () => {
        const result = await this.syncRunner.refreshSourceCatalog();
        this.logger.info(
          `Catalog scheduled synchronization completed: ${result.products} products, ${result.categories} categories`,
        );
      });
    }, this.intervalMs);
    this.timer.unref?.();
  }

  stop(): void {
    if (!this.timer) {
      return;
    }

    clearInterval(this.timer);
    this.timer = null;
  }

  private async runSync(label: string, task: () => Promise<void>): Promise<void> {
    if (this.syncInFlight) {
      this.logger.warn(`Catalog ${label} synchronization skipped because another sync is already running`);
      await this.syncInFlight;
      return;
    }

    this.syncInFlight = (async () => {
      try {
        await task();
      } catch (error) {
        this.logger.error(`Catalog ${label} synchronization failed`, error);
      } finally {
        this.syncInFlight = null;
      }
    })();

    await this.syncInFlight;
  }
}
