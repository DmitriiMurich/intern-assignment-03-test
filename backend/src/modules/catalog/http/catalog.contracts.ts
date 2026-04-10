import type { SupportedLanguageCode } from "../../../shared/constants/languages";
import type { SupportedCurrencyCode } from "../../../shared/constants/currencies";
import type { CatalogSortOption } from "../domain/catalog.types";

export interface CatalogQuerystring {
  lang?: SupportedLanguageCode;
  currency?: SupportedCurrencyCode;
  page?: number;
  pageSize?: number;
  query?: string;
  category?: string;
  sort?: CatalogSortOption;
}

export interface CatalogProductDetailsQuerystring {
  lang?: SupportedLanguageCode;
  currency?: SupportedCurrencyCode;
}

export interface CatalogProductParams {
  productId: string;
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

export interface CurrencyItemResponse {
  code: SupportedCurrencyCode;
  name: string;
  symbol: string;
  isSourceCurrency: boolean;
}

export interface CurrenciesResponse {
  items: CurrencyItemResponse[];
}

export interface CatalogCategoryResponse {
  slug: string;
  title: string;
}

export interface CatalogProductResponse {
  id: string;
  title: string;
  description: string;
  price: {
    amount: number;
    currency: SupportedCurrencyCode;
  };
  rating: number;
  imageUrl: string;
  category: CatalogCategoryResponse;
}

export interface CatalogReviewResponse {
  id: string;
  rating: number;
  comment: string;
  date: string;
  reviewerName: string;
}

export interface CatalogResponse {
  language: SupportedLanguageCode;
  currency: SupportedCurrencyCode;
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
    sourceCurrency: "USD";
    exchangeRateProvider: "frankfurter" | null;
  };
}

export interface CatalogProductDetailsResponse {
  language: SupportedLanguageCode;
  currency: SupportedCurrencyCode;
  product: CatalogProductResponse;
  reviews: CatalogReviewResponse[];
  meta: {
    sourceLanguage: "en";
    sourceCurrency: "USD";
    exchangeRateProvider: "frankfurter" | null;
  };
}
