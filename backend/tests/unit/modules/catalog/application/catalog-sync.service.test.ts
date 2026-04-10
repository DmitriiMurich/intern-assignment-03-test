import { describe, expect, test } from "@jest/globals";
import { CatalogSyncService } from "../../../../../src/modules/catalog/application/catalog-sync.service";
import type { SourceCategory, SourceProduct } from "../../../../../src/modules/catalog/domain/catalog.types";

describe("CatalogSyncService", () => {
  test("refreshes the source catalog, adds missing categories, and seeds translations", async () => {
    const sourceProducts: SourceProduct[] = [
      {
        externalId: 10,
        title: "Night Serum",
        description: "Skin brightening serum.",
        price: 19.99,
        rating: 4.8,
        imageUrl: "https://example.com/serum.webp",
        categorySlug: "beauty",
        reviews: [],
      },
      {
        externalId: 11,
        title: "Trail Boots",
        description: "Boots for long walks.",
        price: 72.5,
        rating: 4.6,
        imageUrl: "https://example.com/boots.webp",
        categorySlug: "outdoor-gear",
        reviews: [],
      },
    ];
    const fetchedCategories: SourceCategory[] = [
      { slug: "beauty", title: "Beauty" },
    ];
    let replacePayload: { products: SourceProduct[]; categories: SourceCategory[] } | null = null;
    let seedCalls = 0;

    const service = new CatalogSyncService(
      {
        async replaceSourceCatalog(products: SourceProduct[], categories: SourceCategory[]) {
          replacePayload = { products, categories };
        },
      } as never,
      {
        async fetchProducts() {
          return sourceProducts;
        },
        async fetchCategories() {
          return fetchedCategories;
        },
      } as never,
      {
        async seedAllTranslations() {
          seedCalls += 1;
        },
      } as never,
    );

    const result = await service.refreshSourceCatalog();

    expect(result).toEqual({
      products: 2,
      categories: 2,
    });
    expect(seedCalls).toBe(1);
    expect(replacePayload).not.toBeNull();

    const capturedPayload = replacePayload!;

    expect(capturedPayload.products).toEqual(sourceProducts);
    expect(capturedPayload.categories).toEqual([
      { slug: "beauty", title: "Beauty" },
      { slug: "outdoor-gear", title: "Outdoor Gear" },
    ]);
  });
});
