import {
  sourceLanguageCode,
  type SupportedLanguageCode,
} from "../../../shared/constants/languages.js";
import type { CatalogListParams } from "../domain/catalog.types.js";
import { YandexTranslateClient } from "../infrastructure/yandex-translate.client.js";
import { CatalogRepository } from "../infrastructure/catalog.repository.js";
import { CatalogSyncService } from "./catalog-sync.service.js";

const translationProvider = "yandex";

export class CatalogService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
    private readonly catalogSyncService: CatalogSyncService,
    private readonly yandexTranslateClient: YandexTranslateClient,
  ) {}

  async getCatalog(params: CatalogListParams) {
    await this.catalogSyncService.ensureSourceCatalog();

    if (params.languageCode !== sourceLanguageCode) {
      await this.ensureCategoryTranslations(params.languageCode);
      await this.ensureProductTranslations(params.languageCode);
    }

    return this.catalogRepository.getLocalizedCatalog(params);
  }

  async syncCatalog() {
    return this.catalogSyncService.refreshSourceCatalog();
  }

  private async ensureProductTranslations(languageCode: SupportedLanguageCode): Promise<void> {
    const missingTranslations = await this.catalogRepository.getMissingProductTranslations(languageCode);

    if (missingTranslations.length === 0) {
      return;
    }

    const translatedTitles = await this.yandexTranslateClient.translateTexts(
      missingTranslations.map((translation) => translation.sourceTitle),
      languageCode,
    );
    const translatedDescriptions = await this.yandexTranslateClient.translateTexts(
      missingTranslations.map((translation) => translation.sourceDescription),
      languageCode,
    );

    await this.catalogRepository.saveProductTranslations(
      languageCode,
      missingTranslations.map((translation, index) => ({
        productId: translation.productId,
        title: translatedTitles[index] ?? translation.sourceTitle,
        description: translatedDescriptions[index] ?? translation.sourceDescription,
      })),
      translationProvider,
    );
  }

  private async ensureCategoryTranslations(languageCode: SupportedLanguageCode): Promise<void> {
    const missingTranslations = await this.catalogRepository.getMissingCategoryTranslations(languageCode);

    if (missingTranslations.length === 0) {
      return;
    }

    const translatedTitles = await this.yandexTranslateClient.translateTexts(
      missingTranslations.map((translation) => translation.sourceTitle),
      languageCode,
    );

    await this.catalogRepository.saveCategoryTranslations(
      languageCode,
      missingTranslations.map((translation, index) => ({
        categorySlug: translation.categorySlug,
        title: translatedTitles[index] ?? translation.sourceTitle,
      })),
      translationProvider,
    );
  }
}
