import { describe, expect, test } from "@jest/globals";
import { CatalogService } from "../../../../../src/modules/catalog/application/catalog.service";
import type {
  LocalizedCatalog,
  LocalizedProductDetails,
} from "../../../../../src/modules/catalog/domain/catalog.types";

function createCatalog(overrides: Partial<LocalizedCatalog> = {}): LocalizedCatalog {
  return {
    language: "ru",
    currency: "USD",
    categories: [
      { slug: "beauty", title: "Beauty RU" },
    ],
    products: [
      {
        id: "10",
        title: "Mascara RU",
        description: "Localized product description.",
        price: {
          amount: 9.99,
          currency: "USD",
        },
        rating: 4.2,
        imageUrl: "https://example.com/10.webp",
        category: {
          slug: "beauty",
          title: "Beauty RU",
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
    language: "ru",
    currency: "USD",
    product: createCatalog().products[0]!,
    reviews: [
      {
        id: "review-1",
        rating: 5,
        comment: "Localized review comment.",
        date: "2026-04-10T10:15:00.000Z",
        reviewerName: "Emma Wilson",
      },
    ],
    ...overrides,
  };
}

describe("CatalogService", () => {
  test("returns localized catalog from repository", async () => {
    let localizedCatalogReads = 0;
    const catalogRepository = {
      async countProducts() {
        return 194;
      },
      async getLocalizedCatalog() {
        localizedCatalogReads += 1;
        return createCatalog();
      },
    };
    const currencyRateService = {
      convertCatalog(catalog: LocalizedCatalog) {
        return catalog;
      },
    };

    const service = new CatalogService(
      catalogRepository as never,
      currencyRateService as never,
    );

    const catalog = await service.getCatalog({
      languageCode: "ru",
      currencyCode: "USD",
      page: 1,
      pageSize: 20,
      query: "",
      categorySlug: null,
      sort: "price_asc",
    });

    expect(localizedCatalogReads).toBe(1);
    expect(catalog.categories[0]?.title).toBe("Beauty RU");
    expect(catalog.products[0]?.title).toBe("Mascara RU");
  });

  test("returns localized product details from repository", async () => {
    let localizedProductDetailReads = 0;
    const catalogRepository = {
      async countProducts() {
        return 194;
      },
      async getLocalizedProductDetails() {
        localizedProductDetailReads += 1;
        return createProductDetails();
      },
    };
    const currencyRateService = {
      async convertProductDetails(productDetails: LocalizedProductDetails) {
        return productDetails;
      },
    };

    const service = new CatalogService(
      catalogRepository as never,
      currencyRateService as never,
    );

    const productDetails = await service.getProductDetails({
      productId: "10",
      languageCode: "ru",
      currencyCode: "USD",
    });

    expect(localizedProductDetailReads).toBe(1);
    expect(productDetails.product.title).toBe("Mascara RU");
    expect(productDetails.reviews[0]?.reviewerName).toBe("Emma Wilson");
  });

  test("throws when source catalog is not available yet", async () => {
    const catalogRepository = {
      async countProducts() {
        return 0;
      },
    };
    const currencyRateService = {
      convertCatalog(catalog: LocalizedCatalog) {
        return catalog;
      },
    };

    const service = new CatalogService(
      catalogRepository as never,
      currencyRateService as never,
    );

    await expect(
      service.getCatalog({
        languageCode: "en",
        currencyCode: "USD",
        page: 1,
        pageSize: 20,
        query: "",
        categorySlug: null,
        sort: "price_asc",
      }),
    ).rejects.toMatchObject({
      message: "Source catalog is not available yet. Background synchronization is still in progress.",
    });
  });

  test("throws when product details are requested for an unknown product", async () => {
    const catalogRepository = {
      async countProducts() {
        return 194;
      },
      async getLocalizedProductDetails() {
        return null;
      },
    };
    const currencyRateService = {
      async convertProductDetails(productDetails: LocalizedProductDetails) {
        return productDetails;
      },
    };

    const service = new CatalogService(
      catalogRepository as never,
      currencyRateService as never,
    );

    await expect(
      service.getProductDetails({
        productId: "999",
        languageCode: "en",
        currencyCode: "USD",
      }),
    ).rejects.toMatchObject({
      message: "Product 999 was not found",
      code: "PRODUCT_NOT_FOUND",
    });
  });
});
