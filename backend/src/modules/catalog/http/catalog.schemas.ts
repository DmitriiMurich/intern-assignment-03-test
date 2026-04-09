import { sourceLanguageCode, supportedLanguageCodes } from "../../../shared/constants/languages.js";
import { sortOptions } from "../domain/catalog.types.js";
import { errorResponseSchema } from "../../../shared/http/schemas/common.schemas.js";

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

const productSchema = {
  type: "object",
  additionalProperties: false,
  required: ["id", "title", "description", "price", "rating", "imageUrl", "category"],
  properties: {
    id: { type: "string" },
    title: { type: "string" },
    description: { type: "string" },
    price: { type: "number" },
    rating: { type: "number" },
    imageUrl: { type: "string" },
    category: categorySchema,
  },
} as const;

const sortSchema = {
  type: "string",
  enum: [...sortOptions],
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

export const getCatalogSchema = {
  tags: ["Catalog"],
  summary: "Get localized catalog",
  description:
    "Returns a server-paginated localized catalog. English source data is fetched automatically if the database is empty. Non-English responses use cached Yandex Translate results or create them on demand.",
  querystring: {
    type: "object",
    additionalProperties: false,
    properties: {
      lang: {
        ...languageCodeSchema,
        default: sourceLanguageCode,
        description: "Language code for localized catalog content.",
      },
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
      required: ["language", "categories", "items", "meta"],
      properties: {
        language: languageCodeSchema,
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
            "translationProvider",
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
            translationProvider: {
              anyOf: [
                { type: "string", enum: ["yandex"] },
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

export const syncCatalogSchema = {
  tags: ["Catalog"],
  summary: "Force source catalog synchronization",
  description:
    "Refreshes English source data from DummyJSON and clears cached translations so they can be regenerated on the next localized request.",
  response: {
    200: {
      type: "object",
      additionalProperties: false,
      required: [
        "status",
        "sourceLanguage",
        "invalidatedTranslations",
        "syncedProducts",
        "syncedCategories",
      ],
      properties: {
        status: { type: "string", enum: ["ok"] },
        sourceLanguage: { type: "string", enum: [sourceLanguageCode] },
        invalidatedTranslations: { type: "boolean", enum: [true] },
        syncedProducts: { type: "integer" },
        syncedCategories: { type: "integer" },
      },
    },
    502: errorResponseSchema,
    500: errorResponseSchema,
  },
} as const;
