import { sourceLanguageCode, supportedLanguageCodes } from "../../../shared/constants/languages";
import {
  sourceCurrencyCode,
  supportedCurrencyCodes,
} from "../../../shared/constants/currencies";
import { sortOptions } from "../domain/catalog.types";
import { errorResponseSchema } from "../../../shared/http/schemas/common.schemas";

const languageCodeSchema = {
  type: "string",
  enum: [...supportedLanguageCodes],
} as const;

const categorySchema = {
  type: "object",
  additionalProperties: false,
  required: ["slug", "title"],
  properties: {
    slug: { type: "string" },
    title: { type: "string" },
  },
} as const;

const currencyCodeSchema = {
  type: "string",
  enum: [...supportedCurrencyCodes],
} as const;

const moneySchema = {
  type: "object",
  additionalProperties: false,
  required: ["amount", "currency"],
  properties: {
    amount: { type: "number" },
    currency: currencyCodeSchema,
  },
} as const;

const productSchema = {
  type: "object",
  additionalProperties: false,
  required: ["id", "title", "description", "price", "rating", "imageUrl", "category"],
  properties: {
    id: { type: "string" },
    title: { type: "string" },
    description: { type: "string" },
    price: moneySchema,
    rating: { type: "number" },
    imageUrl: { type: "string" },
    category: categorySchema,
  },
} as const;

const productReviewSchema = {
  type: "object",
  additionalProperties: false,
  required: ["id", "rating", "comment", "date", "reviewerName"],
  properties: {
    id: { type: "string" },
    rating: { type: "number" },
    comment: { type: "string" },
    date: { type: "string" },
    reviewerName: { type: "string" },
  },
} as const;

const sortSchema = {
  type: "string",
  enum: [...sortOptions],
} as const;

const localizationQuerystringSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    lang: {
      ...languageCodeSchema,
      default: sourceLanguageCode,
      description: "Language code for localized catalog content.",
    },
    currency: {
      ...currencyCodeSchema,
      default: sourceCurrencyCode,
      description: "Currency code for converted product prices.",
    },
  },
} as const;

export const getLanguagesSchema = {
  tags: ["Catalog"],
  summary: "List supported catalog languages",
  description: "Returns the fixed set of languages accepted by the catalog API.",
  response: {
    200: {
      type: "object",
      additionalProperties: false,
      required: ["items"],
      properties: {
        items: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            required: ["code", "name", "isSourceLanguage"],
            properties: {
              code: languageCodeSchema,
              name: { type: "string" },
              isSourceLanguage: { type: "boolean" },
            },
          },
        },
      },
    },
  },
} as const;

export const getCurrenciesSchema = {
  tags: ["Catalog"],
  summary: "List supported catalog currencies",
  description: "Returns the fixed set of currencies accepted by the catalog API.",
  response: {
    200: {
      type: "object",
      additionalProperties: false,
      required: ["items"],
      properties: {
        items: {
          type: "array",
          items: {
            type: "object",
            additionalProperties: false,
            required: ["code", "name", "symbol", "isSourceCurrency"],
            properties: {
              code: currencyCodeSchema,
              name: { type: "string" },
              symbol: { type: "string" },
              isSourceCurrency: { type: "boolean" },
            },
          },
        },
      },
    },
  },
} as const;

export const getCatalogSchema = {
  tags: ["Catalog"],
  summary: "Get localized catalog",
  description:
    "Returns a server-paginated localized catalog. English source data is synchronized in the background at startup and then refreshed every hour. Non-English responses use curated static translations seeded into PostgreSQL during synchronization.",
  querystring: {
    ...localizationQuerystringSchema,
    properties: {
      ...localizationQuerystringSchema.properties,
      page: {
        type: "integer",
        minimum: 1,
        default: 1,
        description: "1-based page index.",
      },
      pageSize: {
        type: "integer",
        minimum: 1,
        maximum: 100,
        default: 20,
        description: "Number of products per page.",
      },
      query: {
        type: "string",
        default: "",
        description: "Search query applied to localized title and description.",
      },
      category: {
        anyOf: [
          { type: "string" },
          { type: "null" },
        ],
        description: "Category slug filter.",
      },
      sort: {
        ...sortSchema,
        default: "price_asc",
        description: "Catalog sorting mode.",
      },
    },
  },
  response: {
    200: {
      type: "object",
      additionalProperties: false,
      required: ["language", "currency", "categories", "items", "meta"],
      properties: {
        language: languageCodeSchema,
        currency: currencyCodeSchema,
        categories: {
          type: "array",
          items: categorySchema,
        },
        items: {
          type: "array",
          items: productSchema,
        },
        meta: {
          type: "object",
          additionalProperties: false,
          required: [
            "totalProducts",
            "totalCategories",
            "currentPage",
            "pageSize",
            "totalPages",
            "query",
            "category",
            "sort",
            "sourceLanguage",
            "sourceCurrency",
            "exchangeRateProvider",
          ],
          properties: {
            totalProducts: { type: "integer" },
            totalCategories: { type: "integer" },
            currentPage: { type: "integer" },
            pageSize: { type: "integer" },
            totalPages: { type: "integer" },
            query: { type: "string" },
            category: {
              anyOf: [
                { type: "string" },
                { type: "null" },
              ],
            },
            sort: sortSchema,
            sourceLanguage: { type: "string", enum: [sourceLanguageCode] },
            sourceCurrency: { type: "string", enum: [sourceCurrencyCode] },
            exchangeRateProvider: {
              anyOf: [
                { type: "string", enum: ["frankfurter"] },
                { type: "null" },
              ],
            },
          },
        },
      },
    },
    400: errorResponseSchema,
    502: errorResponseSchema,
    503: errorResponseSchema,
    500: errorResponseSchema,
  },
} as const;

export const getProductDetailsSchema = {
  tags: ["Catalog"],
  summary: "Get localized product details",
  description:
    "Returns a localized product card with translated review comments for the selected language and converted price for the selected currency.",
  params: {
    type: "object",
    additionalProperties: false,
    required: ["productId"],
    properties: {
      productId: {
        type: "string",
        description: "External product identifier from the catalog response.",
      },
    },
  },
  querystring: localizationQuerystringSchema,
  response: {
    200: {
      type: "object",
      additionalProperties: false,
      required: ["language", "currency", "product", "reviews", "meta"],
      properties: {
        language: languageCodeSchema,
        currency: currencyCodeSchema,
        product: productSchema,
        reviews: {
          type: "array",
          items: productReviewSchema,
        },
        meta: {
          type: "object",
          additionalProperties: false,
          required: ["sourceLanguage", "sourceCurrency", "exchangeRateProvider"],
          properties: {
            sourceLanguage: { type: "string", enum: [sourceLanguageCode] },
            sourceCurrency: { type: "string", enum: [sourceCurrencyCode] },
            exchangeRateProvider: {
              anyOf: [
                { type: "string", enum: ["frankfurter"] },
                { type: "null" },
              ],
            },
          },
        },
      },
    },
    400: errorResponseSchema,
    404: errorResponseSchema,
    502: errorResponseSchema,
    503: errorResponseSchema,
    500: errorResponseSchema,
  },
} as const;
