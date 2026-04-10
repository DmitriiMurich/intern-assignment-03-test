import { afterEach, describe, expect, test } from "@jest/globals";
import Fastify, { type FastifyInstance } from "fastify";
import { registerCatalogRoutes } from "../../../../../src/modules/catalog/http/catalog.routes";

function createCatalogResponse() {
  return {
    language: "en" as const,
    currency: "USD" as const,
    categories: [
      { slug: "beauty", title: "Beauty" },
    ],
    products: [
      {
        id: "10",
        title: "Night Serum",
        description: "A brightening serum.",
        price: {
          amount: 19.99,
          currency: "USD" as const,
        },
        rating: 4.9,
        imageUrl: "https://example.com/serum.webp",
        category: {
          slug: "beauty",
          title: "Beauty",
        },
      },
    ],
    totalProducts: 1,
    page: 1,
    pageSize: 20,
    totalPages: 1,
    query: "",
    categorySlug: null,
    sort: "price_asc" as const,
  };
}

function createProductDetailsResponse() {
  return {
    language: "ru" as const,
    currency: "EUR" as const,
    product: {
      id: "10",
      title: "Ночная сыворотка",
      description: "Легкая сыворотка.",
      price: {
        amount: 18.19,
        currency: "EUR" as const,
      },
      rating: 4.9,
      imageUrl: "https://example.com/serum.webp",
      category: {
        slug: "beauty",
        title: "Красота",
      },
    },
    reviews: [
      {
        id: "review-1",
        rating: 5,
        comment: "Очень рекомендую!",
        date: "2026-04-10T10:15:00.000Z",
        reviewerName: "Emma Wilson",
      },
    ],
  };
}

describe("registerCatalogRoutes", () => {
  let app: FastifyInstance | null = null;

  afterEach(async () => {
    if (app) {
      await app.close();
      app = null;
    }
  });

  test("uses default catalog query params and returns response metadata", async () => {
    let capturedRequest: unknown;
    app = Fastify();

    await registerCatalogRoutes(app, {
      catalogService: {
        async getCatalog(params: unknown) {
          capturedRequest = params;
          return createCatalogResponse();
        },
      } as never,
      currencyRateService: {} as never,
    });

    const response = await app.inject({
      method: "GET",
      url: "/api/v1/catalog",
    });

    expect(response.statusCode).toBe(200);
    expect(capturedRequest).toEqual({
      languageCode: "en",
      currencyCode: "USD",
      page: 1,
      pageSize: 20,
      query: "",
      categorySlug: null,
      sort: "price_asc",
    });

    expect(response.json()).toMatchObject({
      language: "en",
      currency: "USD",
      meta: {
        sourceLanguage: "en",
        sourceCurrency: "USD",
        exchangeRateProvider: null,
      },
    });
  });

  test("returns localized product details and forwards selected params", async () => {
    let capturedRequest: unknown;
    app = Fastify();

    await registerCatalogRoutes(app, {
      catalogService: {
        async getCatalog() {
          throw new Error("unexpected catalog request");
        },
        async getProductDetails(params: unknown) {
          capturedRequest = params;
          return createProductDetailsResponse();
        },
      } as never,
      currencyRateService: {} as never,
    });

    const response = await app.inject({
      method: "GET",
      url: "/api/v1/catalog/10?lang=ru&currency=EUR",
    });

    expect(response.statusCode).toBe(200);
    expect(capturedRequest).toEqual({
      productId: "10",
      languageCode: "ru",
      currencyCode: "EUR",
    });
    expect(response.json()).toMatchObject({
      language: "ru",
      currency: "EUR",
      reviews: [
        {
          comment: "Очень рекомендую!",
        },
      ],
      meta: {
        exchangeRateProvider: "frankfurter",
      },
    });
  });

  test("rejects invalid query params with 400", async () => {
    app = Fastify();

    await registerCatalogRoutes(app, {
      catalogService: {
        async getCatalog() {
          return createCatalogResponse();
        },
      } as never,
      currencyRateService: {} as never,
    });

    const response = await app.inject({
      method: "GET",
      url: "/api/v1/catalog?lang=invalid",
    });

    expect(response.statusCode).toBe(400);
  });
});
