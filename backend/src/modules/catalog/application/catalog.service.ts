import {
  sourceLanguageCode,
  type SupportedLanguageCode,
} from "../../../shared/constants/languages";
import { AppError } from "../../../shared/errors/app-error";
import type { CatalogListParams } from "../domain/catalog.types";
import { LibreTranslateClient } from "../infrastructure/libre-translate.client";
import { CatalogRepository } from "../infrastructure/catalog.repository";
import { CurrencyRateService } from "./currency-rate.service";

const translationProvider = "libretranslate";

export class CatalogService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
    private readonly libreTranslateClient: LibreTranslateClient,
    private readonly currencyRateService: CurrencyRateService,
  ) {}

  async getCatalog(params: CatalogListParams) {
    await this.ensureSourceCatalogAvailable();

    if (params.languageCode !== sourceLanguageCode) {
      await this.ensureCategoryTranslations(params.languageCode);
      await this.ensureProductTranslations(params.languageCode);
    }

    const localizedCatalog = await this.catalogRepository.getLocalizedCatalog(params);
    return this.currencyRateService.convertCatalog(localizedCatalog, params.currencyCode);
  }

  private async ensureProductTranslations(languageCode: SupportedLanguageCode): Promise<void> {
    const missingTranslations = await this.catalogRepository.getMissingProductTranslations(languageCode);

    if (missingTranslations.length === 0) {
      return;
    }

    const translatedTitles = await this.libreTranslateClient.translateTexts(
      missingTranslations.map((translation) => translation.sourceTitle),
      languageCode,
    );
    const translatedDescriptions = await this.libreTranslateClient.translateTexts(
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

    const translatedTitles = await this.libreTranslateClient.translateTexts(
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

  private async ensureSourceCatalogAvailable(): Promise<void> {
    const productsCount = await this.catalogRepository.countProducts();

    if (productsCount > 0) {
      return;
    }

    throw new AppError(
      503,
      "SOURCE_CATALOG_UNAVAILABLE",
      "Source catalog is not available yet. Background synchronization is still in progress.",
    );
  }
}
