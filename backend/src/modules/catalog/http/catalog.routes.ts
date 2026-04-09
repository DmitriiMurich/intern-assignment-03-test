import type { FastifyInstance } from "fastify";
import {
  currencyDisplayName,
  currencySymbol,
  isSourceCurrency,
  sourceCurrencyCode,
  supportedCurrencyCodes,
} from "../../../shared/constants/currencies";
import {
  languageDisplayName,
  sourceLanguageCode,
  supportedLanguageCodes,
} from "../../../shared/constants/languages";
import type {
  CurrenciesResponse,
  CatalogQuerystring,
  CatalogResponse,
  LanguagesResponse,
} from "./catalog.contracts";
import { CatalogService } from "../application/catalog.service";
import { CurrencyRateService } from "../application/currency-rate.service";
import { getCatalogSchema, getCurrenciesSchema, getLanguagesSchema } from "./catalog.schemas";

interface CatalogRoutesOptions {
  catalogService: CatalogService;
  currencyRateService: CurrencyRateService;
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

  app.get<{ Reply: CurrenciesResponse }>(
    "/api/v1/currencies",
    {
      schema: getCurrenciesSchema,
    },
    async () => ({
      items: supportedCurrencyCodes.map((currencyCode) => ({
        code: currencyCode,
        name: currencyDisplayName(currencyCode),
        symbol: currencySymbol(currencyCode),
        isSourceCurrency: isSourceCurrency(currencyCode),
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
      const requestedCurrency = request.query.currency ?? sourceCurrencyCode;
      const page = request.query.page ?? 1;
      const pageSize = request.query.pageSize ?? 20;
      const query = request.query.query ?? "";
      const category = request.query.category ?? null;
      const sort = request.query.sort ?? "price_asc";
      const catalog = await options.catalogService.getCatalog({
        languageCode: requestedLanguage,
        currencyCode: requestedCurrency,
        page,
        pageSize,
        query,
        categorySlug: category,
        sort,
      });

      return {
        language: catalog.language,
        currency: catalog.currency,
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
          sourceCurrency: sourceCurrencyCode,
          translationProvider: requestedLanguage === sourceLanguageCode ? null : "libretranslate",
          exchangeRateProvider: requestedCurrency === sourceCurrencyCode ? null : "frankfurter",
        },
      };
    },
  );
}
