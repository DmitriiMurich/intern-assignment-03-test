import { CatalogRepository } from "../infrastructure/catalog.repository";
import { DummyJsonClient } from "../infrastructure/dummy-json.client";
import { humanizeCategorySlug } from "../../../shared/utils/text";
import type { SourceCategory } from "../domain/catalog.types";
import { CatalogTranslationSeedService } from "./catalog-translation.seed.service";

export class CatalogSyncService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
    private readonly dummyJsonClient: DummyJsonClient,
    private readonly catalogTranslationSeedService: CatalogTranslationSeedService,
  ) {}

  async refreshSourceCatalog(): Promise<{ products: number; categories: number }> {
    const [products, fetchedCategories] = await Promise.all([
      this.dummyJsonClient.fetchProducts(),
      this.dummyJsonClient.fetchCategories(),
    ]);

    const categoryMap = new Map<string, SourceCategory>();

    for (const category of fetchedCategories) {
      categoryMap.set(category.slug, category);
    }

    for (const product of products) {
      if (!categoryMap.has(product.categorySlug)) {
        categoryMap.set(product.categorySlug, {
          slug: product.categorySlug,
          title: humanizeCategorySlug(product.categorySlug),
        });
      }
    }

    const categories = Array.from(categoryMap.values()).sort((left, right) =>
      left.slug.localeCompare(right.slug),
    );

    await this.catalogRepository.replaceSourceCatalog(products, categories);
    await this.catalogTranslationSeedService.seedAllTranslations();

    return {
      products: products.length,
      categories: categories.length,
    };
  }
}
