import { AppError } from "../../../shared/errors/app-error";
import { humanizeCategorySlug } from "../../../shared/utils/text";
import type { SourceCategory, SourceProduct } from "../domain/catalog.types";

interface DummyJsonProductDto {
  id: number;
  title: string;
  description: string;
  category: string;
  price: number;
  rating?: number;
  images?: string[];
  thumbnail?: string;
}

interface DummyJsonProductsResponseDto {
  products: DummyJsonProductDto[];
}

interface DummyJsonCategoryDto {
  slug: string;
  name: string;
}

export class DummyJsonClient {
  constructor(
    private readonly baseUrl: string,
  ) {}

  async fetchProducts(): Promise<SourceProduct[]> {
    const url = new URL("/products", this.baseUrl);
    url.searchParams.set("limit", "0");
    url.searchParams.set("skip", "0");

    const response = await fetch(url, {
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      throw new AppError(
        502,
        "DUMMYJSON_PRODUCTS_REQUEST_FAILED",
        `DummyJSON products request failed with status ${response.status}`,
      );
    }

    const payload = (await response.json()) as DummyJsonProductsResponseDto;

    return payload.products.map((product) => ({
      externalId: product.id,
      title: product.title,
      description: product.description,
      price: product.price,
      rating: product.rating ?? 0,
      imageUrl: product.thumbnail ?? product.images?.[0] ?? "",
      categorySlug: product.category,
    }));
  }

  async fetchCategories(): Promise<SourceCategory[]> {
    const url = new URL("/products/categories", this.baseUrl);
    const response = await fetch(url, {
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      throw new AppError(
        502,
        "DUMMYJSON_CATEGORIES_REQUEST_FAILED",
        `DummyJSON categories request failed with status ${response.status}`,
      );
    }

    const categories = (await response.json()) as DummyJsonCategoryDto[];

    return categories.map((category) => ({
      slug: category.slug,
      title: category.name || humanizeCategorySlug(category.slug),
    }));
  }
}
