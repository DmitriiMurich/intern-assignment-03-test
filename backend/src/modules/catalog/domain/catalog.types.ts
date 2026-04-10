import type { SupportedLanguageCode } from "../../../shared/constants/languages";
import type { SupportedCurrencyCode } from "../../../shared/constants/currencies";

export const sortOptions = [
  "price_asc",
  "price_desc",
  "rating_desc",
] as const;

export type CatalogSortOption = (typeof sortOptions)[number];

export interface CatalogListParams {
  languageCode: SupportedLanguageCode;
  currencyCode: SupportedCurrencyCode;
  page: number;
  pageSize: number;
  query: string;
  categorySlug: string | null;
  sort: CatalogSortOption;
}

export interface CatalogProductDetailsParams {
  productId: string;
  languageCode: SupportedLanguageCode;
  currencyCode: SupportedCurrencyCode;
}

export interface SourceCategory {
  slug: string;
  title: string;
}

export interface SourceReview {
  rating: number;
  comment: string;
  date: string;
  reviewerName: string;
}

export interface SourceProduct {
  externalId: number;
  title: string;
  description: string;
  price: number;
  rating: number;
  imageUrl: string;
  categorySlug: string;
  reviews: SourceReview[];
}

export interface LocalizedCategory {
  slug: string;
  title: string;
}

export interface LocalizedProduct {
  id: string;
  title: string;
  description: string;
  price: {
    amount: number;
    currency: SupportedCurrencyCode;
  };
  rating: number;
  imageUrl: string;
  category: LocalizedCategory;
}

export interface LocalizedProductReview {
  id: string;
  rating: number;
  comment: string;
  date: string;
  reviewerName: string;
}

export interface LocalizedCatalog {
  language: SupportedLanguageCode;
  currency: SupportedCurrencyCode;
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

export interface LocalizedProductDetails {
  language: SupportedLanguageCode;
  currency: SupportedCurrencyCode;
  product: LocalizedProduct;
  reviews: LocalizedProductReview[];
}

export interface MissingProductTranslation {
  productId: number;
  categorySlug: string;
  sourceTitle: string;
  sourceDescription: string;
}

export interface MissingCategoryTranslation {
  categorySlug: string;
  sourceTitle: string;
}

export interface MissingProductReviewTranslation {
  reviewId: number;
  sourceComment: string;
}
