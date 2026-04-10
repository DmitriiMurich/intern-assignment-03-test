import assert from "node:assert/strict";
import test from "node:test";
import { CatalogTranslationSeedService } from "./catalog-translation.seed.service";

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
      savedProductTranslations.push(`${languageCode}:${translations[0]?.title ?? ""}:${translations[0]?.description ?? ""}`);
    },
    async saveReviewTranslations(
      languageCode: string,
      translations: Array<{ reviewId: number; comment: string }>,
    ) {
      savedReviewTranslations.push(`${languageCode}:${translations[0]?.comment ?? ""}`);
    },
  } as never);

  await service.seedAllTranslations();

  assert.equal(savedCategoryTranslations.length, 9);
  assert.equal(savedProductTranslations.length, 9);
  assert.equal(savedReviewTranslations.length, 9);
  assert.ok(savedCategoryTranslations.every((translation) => translation.length > 3));
  assert.ok(savedProductTranslations.every((translation) => translation.length > 3));
  assert.ok(savedReviewTranslations.every((translation) => translation.length > 3));
  assert.equal(
    savedReviewTranslations.find((translation) => translation.startsWith("ru:")),
    "ru:Не рекомендую!",
  );
  assert.equal(
    savedReviewTranslations.find((translation) => translation.startsWith("de:")),
    "de:Würde ich nicht empfehlen!",
  );
});
