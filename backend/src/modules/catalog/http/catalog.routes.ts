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
  CatalogProductDetailsQuerystring,
  CatalogProductDetailsResponse,
  CatalogProductParams,
  CurrenciesResponse,
  CatalogQuerystring,
  CatalogResponse,
  LanguagesResponse,
} from "./catalog.contracts";
import { CatalogService } from "../application/catalog.service";
import { CurrencyRateService } from "../application/currency-rate.service";
import {
  getCatalogSchema,
  getCurrenciesSchema,
  getLanguagesSchema,
  getProductDetailsSchema,
} from "./catalog.schemas";

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
          exchangeRateProvider: requestedCurrency === sourceCurrencyCode ? null : "frankfurter",
        },
      };
    },
  );

  app.get<{
    Params: CatalogProductParams;
    Querystring: CatalogProductDetailsQuerystring;
    Reply: CatalogProductDetailsResponse;
  }>(
    "/api/v1/catalog/:productId",
    {
      schema: getProductDetailsSchema,
    },
    async (request) => {
      const requestedLanguage = request.query.lang ?? sourceLanguageCode;
      const requestedCurrency = request.query.currency ?? sourceCurrencyCode;
      const productDetails = await options.catalogService.getProductDetails({
        productId: request.params.productId,
        languageCode: requestedLanguage,
        currencyCode: requestedCurrency,
      });

      return {
        language: productDetails.language,
        currency: productDetails.currency,
        product: productDetails.product,
        reviews: productDetails.reviews,
        meta: {
          sourceLanguage: sourceLanguageCode,
          sourceCurrency: sourceCurrencyCode,
          exchangeRateProvider: requestedCurrency === sourceCurrencyCode ? null : "frankfurter",
        },
      };
    },
  );
}
