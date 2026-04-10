import assert from "node:assert/strict";
import test from "node:test";
import { MockTranslationSeedService, mockTranslateText } from "./mock-translation.seed.service";

test("seeds translations for every supported non-source language", async () => {
  const savedCategoryTranslations: string[] = [];
  const savedProductTranslations: string[] = [];

  const service = new MockTranslationSeedService({
    async getCategoryTranslationSources() {
      return [
        { categorySlug: "beauty", sourceTitle: "Beauty" },
      ];
    },
    async getProductTranslationSources() {
      return [
        {
          productId: 10,
          sourceTitle: "Essence Mascara Lash Princess",
          sourceDescription: "The Essence Mascara Lash Princess is a popular mascara.",
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
  } as never);

  await service.seedAllTranslations();

  assert.equal(savedCategoryTranslations.length, 9);
  assert.equal(savedProductTranslations.length, 9);
  assert.ok(savedCategoryTranslations.some((translation) => translation.startsWith("ru:Красота")));
  assert.ok(savedCategoryTranslations.some((translation) => translation.startsWith("zh:美妆")));
});

test("pseudo-localizes product text deterministically", () => {
  assert.equal(
    mockTranslateText("Fresh Water", "ru"),
    "Фресх Шатер",
  );
  assert.equal(
    mockTranslateText("Fresh Water", "zh"),
    "「Ｆｒｅｓｈ　Ｗａｔｅｒ」",
  );
});
