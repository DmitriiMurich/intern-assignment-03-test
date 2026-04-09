import type { SupportedLanguageCode } from "../../../shared/constants/languages.js";

export const sortOptions = [
  "price_asc",
  "price_desc",
  "rating_desc",
] as const;

export type CatalogSortOption = (typeof sortOptions)[number];

export interface CatalogListParams {
  languageCode: SupportedLanguageCode;
  page: number;
  pageSize: number;
  query: string;
  categorySlug: string | null;
  sort: CatalogSortOption;
}

export interface SourceCategory {
  slug: string;
  title: string;
}

export interface SourceProduct {
  externalId: number;
  title: string;
  description: string;
  price: number;
  rating: number;
  imageUrl: string;
  categorySlug: string;
}

export interface LocalizedCategory {
  slug: string;
  title: string;
}

export interface LocalizedProduct {
  id: string;
  title: string;
  description: string;
  price: number;
  rating: number;
  imageUrl: string;
  category: LocalizedCategory;
}

export interface LocalizedCatalog {
  language: SupportedLanguageCode;
  categories: LocalizedCategory[];
  products: LocalizedProduct[];
  totalProducts: number;
  page: number;
  pageSize: number;
  totalPages: number;
  query: string;
  categorySlug: string | null;
  sort: CatalogSortOption;
}

export interface MissingProductTranslation {
  productId: number;
  sourceTitle: string;
  sourceDescription: string;
}

export interface MissingCategoryTranslation {
  categorySlug: string;
  sourceTitle: string;
}
