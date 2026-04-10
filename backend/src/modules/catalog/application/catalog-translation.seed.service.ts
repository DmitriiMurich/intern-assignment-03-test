import {
  sourceLanguageCode,
  supportedLanguageCodes,
  type SupportedLanguageCode,
} from "../../../shared/constants/languages";
import type { MissingProductTranslation } from "../domain/catalog.types";
import { CatalogRepository } from "../infrastructure/catalog.repository";
import {
  localizeCategoryTitle,
  localizeProductDescription,
  localizeProductTitle,
} from "./catalog-translation.static";
import { localizeReviewComment } from "./catalog-review-translation.static";

const translationProvider = "static-curated";

type TargetLanguageCode = Exclude<SupportedLanguageCode, typeof sourceLanguageCode>;

export class CatalogTranslationSeedService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
  ) {}

  async seedAllTranslations(): Promise<void> {
    for (const languageCode of supportedLanguageCodes) {
      if (languageCode === sourceLanguageCode) {
        continue;
      }

      await this.seedLanguageTranslations(languageCode);
    }
  }

  private async seedLanguageTranslations(
    languageCode: TargetLanguageCode,
  ): Promise<void> {
    await this.seedCategoryTranslations(languageCode);
    await this.seedProductTranslations(languageCode);
    await this.seedReviewTranslations(languageCode);
  }

  private async seedCategoryTranslations(
    languageCode: TargetLanguageCode,
  ): Promise<void> {
    const categoryTranslations = await this.catalogRepository.getCategoryTranslationSources();

    if (categoryTranslations.length === 0) {
      return;
    }

    await this.catalogRepository.saveCategoryTranslations(
      languageCode,
      categoryTranslations.map((translation) => ({
        categorySlug: translation.categorySlug,
        title: localizeCategoryTitle(translation, languageCode),
      })),
      translationProvider,
    );
  }

  private async seedProductTranslations(
    languageCode: TargetLanguageCode,
  ): Promise<void> {
    const productTranslations = await this.catalogRepository.getProductTranslationSources();

    if (productTranslations.length === 0) {
      return;
    }

    await this.catalogRepository.saveProductTranslations(
      languageCode,
      productTranslations.map((translation) => this.localizeProductTranslation(translation, languageCode)),
      translationProvider,
    );
  }

  private localizeProductTranslation(
    translation: MissingProductTranslation,
    languageCode: TargetLanguageCode,
  ): { productId: number; title: string; description: string } {
    const localizedTitle = localizeProductTitle(translation, languageCode);

    return {
      productId: translation.productId,
      title: localizedTitle,
      description: localizeProductDescription(
        {
          productId: translation.productId,
          sourceDescription: translation.sourceDescription,
        },
        languageCode,
      ),
    };
  }

  private async seedReviewTranslations(
    languageCode: TargetLanguageCode,
  ): Promise<void> {
    const reviewTranslations = await this.catalogRepository.getReviewTranslationSources();

    if (reviewTranslations.length === 0) {
      return;
    }

    await this.catalogRepository.saveReviewTranslations(
      languageCode,
      reviewTranslations.map((translation) => ({
        reviewId: translation.reviewId,
        comment: localizeReviewComment(translation.sourceComment, languageCode),
      })),
      translationProvider,
    );
  }
}
