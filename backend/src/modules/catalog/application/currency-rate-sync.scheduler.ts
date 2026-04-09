import type { Logger } from "../../../shared/types/logger";

const hourlyIntervalMs = 60 * 60 * 1000;

export interface CurrencyRateSyncRunner {
  ensureRatesAvailable(): Promise<void>;
  refreshRates(): Promise<{ rates: number }>;
}

interface CurrencyRateSyncSchedulerOptions {
  intervalMs?: number;
  logger?: Logger;
}

export class CurrencyRateSyncScheduler {
  private readonly intervalMs: number;
  private readonly logger: Logger;
  private timer: NodeJS.Timeout | null = null;
  private syncInFlight: Promise<void> | null = null;

  constructor(
    private readonly syncRunner: CurrencyRateSyncRunner,
    options: CurrencyRateSyncSchedulerOptions = {},
  ) {
    this.intervalMs = options.intervalMs ?? hourlyIntervalMs;
    this.logger = options.logger ?? console;
  }

  async start(): Promise<void> {
    if (this.timer) {
      return;
    }

    await this.runSync("startup", async () => {
      await this.syncRunner.ensureRatesAvailable();
      this.logger.info("Currency rate startup synchronization completed");
    });

    this.timer = setInterval(() => {
      void this.runSync("scheduled", async () => {
        const result = await this.syncRunner.refreshRates();
        this.logger.info(`Currency rate scheduled synchronization completed: ${result.rates} rates`);
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
      this.logger.warn(`Currency rate ${label} synchronization skipped because another sync is already running`);
      await this.syncInFlight;
      return;
    }

    this.syncInFlight = (async () => {
      try {
        await task();
      } catch (error) {
        this.logger.error(`Currency rate ${label} synchronization failed`, error);
      } finally {
        this.syncInFlight = null;
      }
    })();

    await this.syncInFlight;
  }
}
