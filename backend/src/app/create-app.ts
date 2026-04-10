import Fastify from "fastify";
import type { FastifyError } from "fastify";
import type { Pool } from "pg";
import { AppError } from "../shared/errors/app-error";
import { healthResponseSchema } from "../shared/http/schemas/common.schemas";
import { registerSwagger } from "./register-swagger";
import { CatalogRepository } from "../modules/catalog/infrastructure/catalog.repository";
import { DummyJsonClient } from "../modules/catalog/infrastructure/dummy-json.client";
import { CatalogSyncService } from "../modules/catalog/application/catalog-sync.service";
import { CatalogService } from "../modules/catalog/application/catalog.service";
import { registerCatalogRoutes } from "../modules/catalog/http/catalog.routes";
import { FrankfurterClient } from "../modules/catalog/infrastructure/frankfurter.client";
import { CurrencyRateService } from "../modules/catalog/application/currency-rate.service";
import { CatalogTranslationSeedService } from "../modules/catalog/application/catalog-translation.seed.service";

interface CreateAppOptions {
  pool: Pool;
  dummyJsonBaseUrl: string;
}

export async function createApp(options: CreateAppOptions) {
  const app = Fastify({
    logger: true,
  });

  const catalogRepository = new CatalogRepository(options.pool);
  const dummyJsonClient = new DummyJsonClient(
    options.dummyJsonBaseUrl,
  );
  const catalogTranslationSeedService = new CatalogTranslationSeedService(
    catalogRepository,
  );
  const frankfurterClient = new FrankfurterClient();
  const catalogSyncService = new CatalogSyncService(
    catalogRepository,
    dummyJsonClient,
    catalogTranslationSeedService,
  );
  const currencyRateService = new CurrencyRateService(
    catalogRepository,
    frankfurterClient,
  );
  const catalogService = new CatalogService(
    catalogRepository,
    currencyRateService,
  );

  await registerSwagger(app);

  app.get(
    "/health",
    {
      schema: {
        tags: ["System"],
        summary: "Health check",
        description: "Returns service liveness status.",
        response: {
          200: healthResponseSchema,
        },
      },
    },
    async () => ({
      status: "ok",
    }),
  );

  await registerCatalogRoutes(app, {
    catalogService,
    currencyRateService,
  });

  app.setErrorHandler((error, _request, reply) => {
    app.log.error(error);

    if (isValidationError(error)) {
      reply.status(400).send({
        statusCode: 400,
        error: "BAD_REQUEST",
        message: error.message,
      });
      return;
    }

    if (error instanceof AppError) {
      reply.status(error.statusCode).send({
        statusCode: error.statusCode,
        error: error.code,
        message: error.message,
      });
      return;
    }

    reply.status(500).send({
      statusCode: 500,
      error: "INTERNAL_SERVER_ERROR",
      message: error instanceof Error ? error.message : "Unexpected server error",
    });
  });

  return {
    app,
    catalogRepository,
    catalogSyncService,
    currencyRateService,
  };
}

function isValidationError(error: unknown): error is FastifyError & { validation: unknown } {
  return error instanceof Error && "validation" in error;
}
