import { AppError } from "../../../shared/errors/app-error";
import type { CatalogListParams, CatalogProductDetailsParams } from "../domain/catalog.types";
import { CatalogRepository } from "../infrastructure/catalog.repository";
import { CurrencyRateService } from "./currency-rate.service";

export class CatalogService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
    private readonly currencyRateService: CurrencyRateService,
  ) {}

  async getCatalog(params: CatalogListParams) {
    await this.ensureSourceCatalogAvailable();
    const localizedCatalog = await this.catalogRepository.getLocalizedCatalog(params);
    return this.currencyRateService.convertCatalog(localizedCatalog, params.currencyCode);
  }

  async getProductDetails(params: CatalogProductDetailsParams) {
    await this.ensureSourceCatalogAvailable();
    const localizedProductDetails = await this.catalogRepository.getLocalizedProductDetails(params);

    if (!localizedProductDetails) {
      throw new AppError(
        404,
        "PRODUCT_NOT_FOUND",
        `Product ${params.productId} was not found`,
      );
    }

    return this.currencyRateService.convertProductDetails(localizedProductDetails, params.currencyCode);
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
