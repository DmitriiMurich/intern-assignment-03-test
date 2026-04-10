import { afterAll, beforeAll, describe, expect, test } from "@jest/globals";
import { CatalogRepository } from "../../../../../src/modules/catalog/infrastructure/catalog.repository";
import type {
  MissingProductTranslation,
  SourceCategory,
  SourceProduct,
} from "../../../../../src/modules/catalog/domain/catalog.types";
import { createTestDatabase, type TestDatabaseContext } from "../../../helpers/postgres";

async function seedProductTranslations(
  repository: CatalogRepository,
  languageCode: "ru",
) {
  const translationSources = await repository.getProductTranslationSources();
  const translationByTitle = new Map<string, MissingProductTranslation>(
    translationSources.map((translation) => [translation.sourceTitle, translation]),
  );

  await repository.saveProductTranslations(
    languageCode,
    [
      {
        productId: translationByTitle.get("Night Serum")!.productId,
        title: "Ночная сыворотка",
        description: "Легкая сыворотка для сияния кожи.",
      },
      {
        productId: translationByTitle.get("Citrus Soda")!.productId,
        title: "Цитрусовая газировка",
        description: "Освежающий цитрусовый напиток.",
      },
    ],
    "integration-test",
  );
}

describe("CatalogRepository integration", () => {
  let database: TestDatabaseContext;
  let repository: CatalogRepository;

  const sourceCategories: SourceCategory[] = [
    { slug: "beauty", title: "Beauty" },
    { slug: "groceries", title: "Groceries" },
  ];

  const sourceProducts: SourceProduct[] = [
    {
      externalId: 101,
      title: "Night Serum",
      description: "A brightening serum for evening care.",
      price: 19.99,
      rating: 4.8,
      imageUrl: "https://example.com/serum.webp",
      categorySlug: "beauty",
      reviews: [
        {
          rating: 5,
          comment: "Highly recommended!",
          date: "2026-04-10T10:15:00.000Z",
          reviewerName: "Emma Wilson",
        },
        {
          rating: 4,
          comment: "Very satisfied!",
          date: "2026-04-11T08:45:00.000Z",
          reviewerName: "Lucas Brown",
        },
      ],
    },
    {
      externalId: 102,
      title: "Citrus Soda",
      description: "Sparkling citrus drink.",
      price: 3.5,
      rating: 4.2,
      imageUrl: "https://example.com/soda.webp",
      categorySlug: "groceries",
      reviews: [],
    },
  ];

  beforeAll(async () => {
    database = await createTestDatabase();
    repository = new CatalogRepository(database.pool);
    await repository.ensureSchema();
  });

  afterAll(async () => {
    await database.dispose();
  });

  test("returns filtered localized catalog data", async () => {
    await repository.replaceSourceCatalog(sourceProducts, sourceCategories);
    await repository.saveCategoryTranslations(
      "ru",
      [
        { categorySlug: "beauty", title: "Красота" },
        { categorySlug: "groceries", title: "Продукты" },
      ],
      "integration-test",
    );
    await seedProductTranslations(repository, "ru");

    const catalog = await repository.getLocalizedCatalog({
      languageCode: "ru",
      currencyCode: "USD",
      page: 1,
      pageSize: 1,
      query: "сыворотка",
      categorySlug: "beauty",
      sort: "price_desc",
    });

    expect(catalog.totalProducts).toBe(1);
    expect(catalog.totalPages).toBe(1);
    expect(catalog.products).toHaveLength(1);
    expect(catalog.products[0]).toMatchObject({
      id: "101",
      title: "Ночная сыворотка",
      description: "Легкая сыворотка для сияния кожи.",
      category: {
        slug: "beauty",
        title: "Красота",
      },
    });
  });

  test("returns localized product details with translated reviews", async () => {
    await repository.replaceSourceCatalog(sourceProducts, sourceCategories);
    await repository.saveCategoryTranslations(
      "ru",
      [
        { categorySlug: "beauty", title: "Красота" },
        { categorySlug: "groceries", title: "Продукты" },
      ],
      "integration-test",
    );
    await seedProductTranslations(repository, "ru");

    const reviewSources = await repository.getReviewTranslationSources();
    const reviewByComment = new Map(
      reviewSources.map((review) => [review.sourceComment, review.reviewId]),
    );

    await repository.saveReviewTranslations(
      "ru",
      [
        {
          reviewId: reviewByComment.get("Highly recommended!")!,
          comment: "Очень рекомендую!",
        },
        {
          reviewId: reviewByComment.get("Very satisfied!")!,
          comment: "Полностью устраивает!",
        },
      ],
      "integration-test",
    );

    const productDetails = await repository.getLocalizedProductDetails({
      productId: "101",
      languageCode: "ru",
      currencyCode: "USD",
    });

    expect(productDetails).not.toBeNull();
    expect(productDetails?.product).toMatchObject({
      id: "101",
      title: "Ночная сыворотка",
      category: {
        title: "Красота",
      },
    });
    expect(productDetails?.reviews).toEqual([
      expect.objectContaining({
        comment: "Очень рекомендую!",
        reviewerName: "Emma Wilson",
      }),
      expect.objectContaining({
        comment: "Полностью устраивает!",
        reviewerName: "Lucas Brown",
      }),
    ]);
  });

  test("returns null for unknown product details", async () => {
    await repository.replaceSourceCatalog(sourceProducts, sourceCategories);

    const productDetails = await repository.getLocalizedProductDetails({
      productId: "999",
      languageCode: "en",
      currencyCode: "USD",
    });

    expect(productDetails).toBeNull();
  });
});
