import { describe, expect, test } from "@jest/globals";
import { CurrencyRateService } from "../../../../../src/modules/catalog/application/currency-rate.service";
import type {
  LocalizedCatalog,
  LocalizedProductDetails,
} from "../../../../../src/modules/catalog/domain/catalog.types";

function createCatalog(overrides: Partial<LocalizedCatalog> = {}): LocalizedCatalog {
  return {
    language: "en",
    currency: "USD",
    categories: [],
    products: [
      {
        id: "10",
        title: "Night Serum",
        description: "A brightening serum.",
        price: {
          amount: 10.005,
          currency: "USD",
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
    sort: "price_asc",
    ...overrides,
  };
}

function createProductDetails(overrides: Partial<LocalizedProductDetails> = {}): LocalizedProductDetails {
  return {
    language: "en",
    currency: "USD",
    product: createCatalog().products[0]!,
    reviews: [],
    ...overrides,
  };
}

describe("CurrencyRateService", () => {
  test("refreshes rates on bootstrap when they are missing", async () => {
    const savedRates: Array<{ currencyCode: string; rate: number }> = [];
    const catalogRepository = {
      async countCurrencyRates() {
        return 0;
      },
      async saveCurrencyRates(_baseCurrency: string, rates: Array<{ currencyCode: string; rate: number }>) {
        savedRates.push(...rates);
      },
    };
    const frankfurterClient = {
      async fetchLatestRates() {
        return [
          { currencyCode: "EUR", rate: 0.92 },
          { currencyCode: "CHF", rate: 0.96 },
        ];
      },
    };

    const service = new CurrencyRateService(
      catalogRepository as never,
      frankfurterClient as never,
    );

    await service.ensureRatesAvailable();

    expect(savedRates).toEqual([
      { currencyCode: "EUR", rate: 0.92 },
      { currencyCode: "CHF", rate: 0.96 },
    ]);
  });

  test("returns rounded catalog prices in source currency without rate lookup", async () => {
    const catalogRepository = {
      async getCurrencyRate() {
        throw new Error("should not request exchange rate");
      },
    };

    const service = new CurrencyRateService(
      catalogRepository as never,
      {} as never,
    );

    const catalog = await service.convertCatalog(createCatalog(), "USD");

    expect(catalog.currency).toBe("USD");
    expect(catalog.products[0]?.price.amount).toBe(10.01);
  });

  test("converts product details price with fetched exchange rate", async () => {
    const catalogRepository = {
      async getCurrencyRate() {
        return 0.91;
      },
    };

    const service = new CurrencyRateService(
      catalogRepository as never,
      {} as never,
    );

    const productDetails = await service.convertProductDetails(createProductDetails(), "EUR");

    expect(productDetails.currency).toBe("EUR");
    expect(productDetails.product.price.currency).toBe("EUR");
    expect(productDetails.product.price.amount).toBe(9.1);
  });

  test("throws when exchange rates are not available for catalog conversion", async () => {
    const catalogRepository = {
      async getCurrencyRate() {
        return null;
      },
    };

    const service = new CurrencyRateService(
      catalogRepository as never,
      {} as never,
    );

    await expect(service.convertCatalog(createCatalog(), "EUR")).rejects.toMatchObject({
      code: "EXCHANGE_RATES_UNAVAILABLE",
      statusCode: 503,
    });
  });
});
