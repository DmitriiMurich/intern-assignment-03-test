import { describe, expect, test } from "@jest/globals";
import {
  MockTranslationSeedService,
  mockTranslateText,
} from "../../../../../src/modules/catalog/application/mock-translation.seed.service";

describe("MockTranslationSeedService", () => {
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
        savedProductTranslations.push(
          `${languageCode}:${translations[0]?.title ?? ""}:${translations[0]?.description ?? ""}`,
        );
      },
    } as never);

    await service.seedAllTranslations();

    expect(savedCategoryTranslations).toHaveLength(9);
    expect(savedProductTranslations).toHaveLength(9);
    expect(savedCategoryTranslations.find((translation) => translation.startsWith("ru:"))).toBeTruthy();
    expect(savedCategoryTranslations.find((translation) => translation.startsWith("zh:"))).toBeTruthy();
    expect(savedCategoryTranslations).not.toContain("ru:Beauty");
  });

  test("pseudo-localizes product text deterministically", () => {
    const ruTranslation = mockTranslateText("Fresh Water", "ru");
    const zhTranslation = mockTranslateText("Fresh Water", "zh");

    expect(mockTranslateText("Fresh Water", "ru")).toBe(ruTranslation);
    expect(mockTranslateText("Fresh Water", "zh")).toBe(zhTranslation);
    expect(ruTranslation).not.toBe("Fresh Water");
    expect(zhTranslation).not.toBe("Fresh Water");
  });
});
