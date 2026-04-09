import Fastify from "fastify";
import type { FastifyError } from "fastify";
import type { Pool } from "pg";
import { AppError } from "../shared/errors/app-error.js";
import { healthResponseSchema } from "../shared/http/schemas/common.schemas.js";
import { registerSwagger } from "./register-swagger.js";
import { CatalogRepository } from "../modules/catalog/infrastructure/catalog.repository.js";
import { DummyJsonClient } from "../modules/catalog/infrastructure/dummy-json.client.js";
import { YandexTranslateClient } from "../modules/catalog/infrastructure/yandex-translate.client.js";
import { CatalogSyncService } from "../modules/catalog/application/catalog-sync.service.js";
import { CatalogService } from "../modules/catalog/application/catalog.service.js";
import { registerCatalogRoutes } from "../modules/catalog/http/catalog.routes.js";

interface CreateAppOptions {
  pool: Pool;
  dummyJsonBaseUrl: string;
  yandexTranslateApiUrl: string;
  yandexTranslateApiKey: string;
  yandexFolderId: string;
}

export async function createApp(options: CreateAppOptions) {
  const app = Fastify({
    logger: true,
  });

  const catalogRepository = new CatalogRepository(options.pool);
  const dummyJsonClient = new DummyJsonClient(
    options.dummyJsonBaseUrl,
  );
  const yandexTranslateClient = new YandexTranslateClient(
    options.yandexTranslateApiUrl,
    options.yandexTranslateApiKey,
    options.yandexFolderId,
  );
  const catalogSyncService = new CatalogSyncService(
    catalogRepository,
    dummyJsonClient,
  );
  const catalogService = new CatalogService(
    catalogRepository,
    catalogSyncService,
    yandexTranslateClient,
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
  };
}

function isValidationError(error: unknown): error is FastifyError & { validation: unknown } {
  return error instanceof Error && "validation" in error;
}
