import { describe, expect, test } from "@jest/globals";
import { CatalogTranslationSeedService } from "../../../../../src/modules/catalog/application/catalog-translation.seed.service";

describe("CatalogTranslationSeedService", () => {
  test("seeds product, category, and review translations for every supported non-source language", async () => {
    const savedCategoryTranslations: string[] = [];
    const savedProductTranslations: string[] = [];
    const savedReviewTranslations: string[] = [];

    const service = new CatalogTranslationSeedService({
      async getCategoryTranslationSources() {
        return [
          { categorySlug: "beauty", sourceTitle: "Beauty" },
        ];
      },
      async getProductTranslationSources() {
        return [
          {
            productId: 1,
            categorySlug: "beauty",
            sourceTitle: "Essence Mascara Lash Princess",
            sourceDescription: "The Essence Mascara Lash Princess is a popular mascara.",
          },
        ];
      },
      async getReviewTranslationSources() {
        return [
          {
            reviewId: 11,
            sourceComment: "Would not recommend!",
          },
        ];
      },
      async saveCategoryTranslations(languageCode: string, translations: Array<{ categorySlug: string; title: string }>) {
        savedCategoryTranslations.push(`${languageCode}:${translations[0]?.title ?? ""}`);
      },
      async saveProductTranslations(
        languageCode: string,
        translations: Array<{ productId: number; title: string; description: string }>,
      ) {
        savedProductTranslations.push(
          `${languageCode}:${translations[0]?.title ?? ""}:${translations[0]?.description ?? ""}`,
        );
      },
      async saveReviewTranslations(
        languageCode: string,
        translations: Array<{ reviewId: number; comment: string }>,
      ) {
        savedReviewTranslations.push(`${languageCode}:${translations[0]?.comment ?? ""}`);
      },
    } as never);

    await service.seedAllTranslations();

    expect(savedCategoryTranslations).toHaveLength(9);
    expect(savedProductTranslations).toHaveLength(9);
    expect(savedReviewTranslations).toHaveLength(9);
    expect(savedCategoryTranslations.every((translation) => translation.length > 3)).toBe(true);
    expect(savedProductTranslations.every((translation) => translation.length > 3)).toBe(true);
    expect(savedReviewTranslations.every((translation) => translation.length > 3)).toBe(true);
    expect(savedReviewTranslations.find((translation) => translation.startsWith("ru:"))).toBe("ru:Не рекомендую!");
    expect(savedReviewTranslations.find((translation) => translation.startsWith("de:"))).toBe("de:Würde ich nicht empfehlen!");
  });
});
