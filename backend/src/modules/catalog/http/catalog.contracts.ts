import type { SupportedLanguageCode } from "../../../shared/constants/languages.js";
import type { CatalogSortOption } from "../domain/catalog.types.js";

export interface CatalogQuerystring {
  lang?: SupportedLanguageCode;
  page?: number;
  pageSize?: number;
  query?: string;
  category?: string;
  sort?: CatalogSortOption;
}

export interface ApiErrorResponse {
  statusCode: number;
  error: string;
  message: string;
}

export interface LanguageItemResponse {
  code: SupportedLanguageCode;
  name: string;
  isSourceLanguage: boolean;
}

export interface LanguagesResponse {
  items: LanguageItemResponse[];
}

export interface CatalogCategoryResponse {
  slug: string;
  title: string;
}

export interface CatalogProductResponse {
  id: string;
  title: string;
  description: string;
  price: number;
  rating: number;
  imageUrl: string;
  category: CatalogCategoryResponse;
}

export interface CatalogResponse {
  language: SupportedLanguageCode;
  categories: CatalogCategoryResponse[];
  items: CatalogProductResponse[];
  meta: {
    totalProducts: number;
    totalCategories: number;
    currentPage: number;
    pageSize: number;
    totalPages: number;
    query: string;
    category: string | null;
    sort: CatalogSortOption;
    sourceLanguage: "en";
    translationProvider: "yandex" | null;
  };
}

export interface SyncCatalogResponse {
  status: "ok";
  sourceLanguage: "en";
  invalidatedTranslations: true;
  syncedProducts: number;
  syncedCategories: number;
}
