import type { FastifyInstance } from "fastify";
import fastifySwagger from "@fastify/swagger";
import fastifySwaggerUi from "@fastify/swagger-ui";

export async function registerSwagger(app: FastifyInstance): Promise<void> {
  await app.register(fastifySwagger, {
    openapi: {
      info: {
        title: "Catalog BFF API",
        description:
          "Backend-for-frontend for the mobile product catalog. Provides localized catalog data with PostgreSQL caching and curated static translations seeded into PostgreSQL.",
        version: "1.0.0",
      },
      tags: [
        { name: "System", description: "Operational service endpoints" },
        { name: "Catalog", description: "Catalog localization and synchronization endpoints" },
      ],
    },
  });

  await app.register(fastifySwaggerUi, {
    routePrefix: "/docs",
    uiConfig: {
      docExpansion: "list",
      deepLinking: true,
    },
  });
}
