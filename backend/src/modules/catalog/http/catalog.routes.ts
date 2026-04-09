import type { FastifyInstance } from "fastify";
import {
  languageDisplayName,
  sourceLanguageCode,
  supportedLanguageCodes,
} from "../../../shared/constants/languages.js";
import type {
  CatalogQuerystring,
  CatalogResponse,
  LanguagesResponse,
  SyncCatalogResponse,
} from "./catalog.contracts.js";
import { CatalogService } from "../application/catalog.service.js";
import { getCatalogSchema, getLanguagesSchema, syncCatalogSchema } from "./catalog.schemas.js";

interface CatalogRoutesOptions {
  catalogService: CatalogService;
}

export async function registerCatalogRoutes(
  app: FastifyInstance,
  options: CatalogRoutesOptions,
): Promise<void> {
  app.get<{ Reply: LanguagesResponse }>(
    "/api/v1/languages",
    {
      schema: getLanguagesSchema,
    },
    async () => ({
      items: supportedLanguageCodes.map((languageCode) => ({
        code: languageCode,
        name: languageDisplayName(languageCode),
        isSourceLanguage: languageCode === sourceLanguageCode,
      })),
    }),
  );

  app.get<{ Querystring: CatalogQuerystring; Reply: CatalogResponse }>(
    "/api/v1/catalog",
    {
      schema: getCatalogSchema,
    },
    async (request) => {
      const requestedLanguage = request.query.lang ?? sourceLanguageCode;
      const page = request.query.page ?? 1;
      const pageSize = request.query.pageSize ?? 20;
      const query = request.query.query ?? "";
      const category = request.query.category ?? null;
      const sort = request.query.sort ?? "price_asc";
      const catalog = await options.catalogService.getCatalog({
        languageCode: requestedLanguage,
        page,
        pageSize,
        query,
        categorySlug: category,
        sort,
      });

      return {
        language: catalog.language,
        categories: catalog.categories,
        items: catalog.products,
        meta: {
          totalProducts: catalog.totalProducts,
          totalCategories: catalog.categories.length,
          currentPage: catalog.page,
          pageSize: catalog.pageSize,
          totalPages: catalog.totalPages,
          query: catalog.query,
          category: catalog.categorySlug,
          sort: catalog.sort,
          sourceLanguage: sourceLanguageCode,
          translationProvider: requestedLanguage === sourceLanguageCode ? null : "yandex",
        },
      };
    },
  );

  app.post<{ Reply: SyncCatalogResponse }>(
    "/api/v1/catalog/sync",
    {
      schema: syncCatalogSchema,
    },
    async () => {
      const syncResult = await options.catalogService.syncCatalog();

      return {
        status: "ok",
        sourceLanguage: sourceLanguageCode,
        invalidatedTranslations: true,
        syncedProducts: syncResult.products,
        syncedCategories: syncResult.categories,
      };
    },
  );
}
