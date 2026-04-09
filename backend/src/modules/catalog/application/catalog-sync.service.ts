import { CatalogRepository } from "../infrastructure/catalog.repository.js";
import { DummyJsonClient } from "../infrastructure/dummy-json.client.js";
import { humanizeCategorySlug } from "../../../shared/utils/text.js";
import type { SourceCategory } from "../domain/catalog.types.js";

export class CatalogSyncService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
    private readonly dummyJsonClient: DummyJsonClient,
  ) {}

  async ensureSourceCatalog(): Promise<void> {
    const productsCount = await this.catalogRepository.countProducts();

    if (productsCount > 0) {
      return;
    }

    await this.refreshSourceCatalog();
  }

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

    return {
      products: products.length,
      categories: categories.length,
    };
  }
}
