import { Pool } from "pg";
import type { SupportedLanguageCode } from "../../../shared/constants/languages";
import type { SupportedCurrencyCode } from "../../../shared/constants/currencies";
import type {
  CatalogProductDetailsParams,
  CatalogListParams,
  CatalogSortOption,
  LocalizedCatalog,
  LocalizedCategory,
  LocalizedProductDetails,
  LocalizedProduct,
  LocalizedProductReview,
  MissingCategoryTranslation,
  MissingProductTranslation,
  MissingProductReviewTranslation,
  SourceCategory,
  SourceProduct,
} from "../domain/catalog.types";

const schemaSql = `
CREATE TABLE IF NOT EXISTS categories (
  slug TEXT PRIMARY KEY,
  source_title TEXT NOT NULL,
  source_language VARCHAR(16) NOT NULL DEFAULT 'en',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
  id BIGSERIAL PRIMARY KEY,
  external_id BIGINT NOT NULL UNIQUE,
  category_slug TEXT NOT NULL REFERENCES categories(slug) ON DELETE CASCADE,
  source_title TEXT NOT NULL,
  source_description TEXT NOT NULL,
  source_language VARCHAR(16) NOT NULL DEFAULT 'en',
  price DOUBLE PRECISION NOT NULL,
  rating DOUBLE PRECISION NOT NULL,
  image_url TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_translations (
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  language_code VARCHAR(16) NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  provider TEXT NOT NULL,
  translated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (product_id, language_code)
);

CREATE TABLE IF NOT EXISTS category_translations (
  category_slug TEXT NOT NULL REFERENCES categories(slug) ON DELETE CASCADE,
  language_code VARCHAR(16) NOT NULL,
  title TEXT NOT NULL,
  provider TEXT NOT NULL,
  translated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (category_slug, language_code)
);

CREATE TABLE IF NOT EXISTS product_reviews (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  source_position INTEGER NOT NULL,
  rating DOUBLE PRECISION NOT NULL,
  source_comment TEXT NOT NULL,
  reviewer_name TEXT NOT NULL,
  review_date TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (product_id, source_position)
);

CREATE TABLE IF NOT EXISTS product_review_translations (
  review_id BIGINT NOT NULL REFERENCES product_reviews(id) ON DELETE CASCADE,
  language_code VARCHAR(16) NOT NULL,
  comment TEXT NOT NULL,
  provider TEXT NOT NULL,
  translated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (review_id, language_code)
);

CREATE TABLE IF NOT EXISTS currency_rates (
  base_currency VARCHAR(16) NOT NULL,
  currency_code VARCHAR(16) NOT NULL,
  rate DOUBLE PRECISION NOT NULL,
  fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (base_currency, currency_code)
);

CREATE INDEX IF NOT EXISTS idx_products_category_slug ON products(category_slug);
CREATE INDEX IF NOT EXISTS idx_product_translations_language ON product_translations(language_code);
CREATE INDEX IF NOT EXISTS idx_category_translations_language ON category_translations(language_code);
CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id ON product_reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_product_review_translations_language ON product_review_translations(language_code);
CREATE INDEX IF NOT EXISTS idx_currency_rates_base_currency ON currency_rates(base_currency);
`;

interface LocalizedProductRow {
  id: string;
  title: string;
  description: string;
  price: number;
  rating: number;
  imageurl: string;
  categoryslug: string;
  categorytitle: string;
}

interface LocalizedCategoryRow {
  slug: string;
  title: string;
}

interface CountRow {
  count: string;
}

interface CurrencyRateRow {
  rate: number;
}

interface MissingProductTranslationRow {
  productid: string;
  categoryslug: string;
  sourcetitle: string;
  sourcedescription: string;
}

interface MissingCategoryTranslationRow {
  categoryslug: string;
  sourcetitle: string;
}

interface MissingProductReviewTranslationRow {
  reviewid: string;
  sourcecomment: string;
}

interface ProductTranslationSourceRow {
  productid: string;
  categoryslug: string;
  sourcetitle: string;
  sourcedescription: string;
}

interface CategoryTranslationSourceRow {
  categoryslug: string;
  sourcetitle: string;
}

interface ProductIdRow {
  id: string;
  externalid: string;
}

interface LocalizedProductReviewRow {
  id: string;
  rating: number;
  comment: string;
  reviewdate: string;
  reviewername: string;
}

interface LocalizedCountRow {
  totalcount: string;
}

export class CatalogRepository {
  constructor(
    private readonly pool: Pool,
  ) {}

  async ensureSchema(): Promise<void> {
    await this.pool.query(schemaSql);
  }

  async countProducts(): Promise<number> {
    const result = await this.pool.query<CountRow>("SELECT COUNT(*)::text AS count FROM products");
    return Number(result.rows[0]?.count ?? "0");
  }

  async countCurrencyRates(baseCurrency: SupportedCurrencyCode): Promise<number> {
    const result = await this.pool.query<CountRow>(
      "SELECT COUNT(*)::text AS count FROM currency_rates WHERE base_currency = $1",
      [baseCurrency],
    );
    return Number(result.rows[0]?.count ?? "0");
  }

  async replaceSourceCatalog(
    products: SourceProduct[],
    categories: SourceCategory[],
  ): Promise<void> {
    const client = await this.pool.connect();
    const sourceExternalIds = products.map((product) => product.externalId);
    const sourceCategorySlugs = categories.map((category) => category.slug);

    try {
      await client.query("BEGIN");
      await client.query("DELETE FROM product_review_translations");
      await client.query("DELETE FROM product_reviews");
      await client.query("DELETE FROM product_translations");
      await client.query("DELETE FROM category_translations");

      for (const category of categories) {
        await client.query(
          `
          INSERT INTO categories (slug, source_title, source_language, updated_at)
          VALUES ($1, $2, 'en', NOW())
          ON CONFLICT (slug) DO UPDATE
          SET source_title = EXCLUDED.source_title,
              source_language = 'en',
              updated_at = NOW()
          `,
          [category.slug, category.title],
        );
      }

      for (const product of products) {
        await client.query(
          `
          INSERT INTO products (
            external_id,
            category_slug,
            source_title,
            source_description,
            source_language,
            price,
            rating,
            image_url,
            updated_at
          )
          VALUES ($1, $2, $3, $4, 'en', $5, $6, $7, NOW())
          ON CONFLICT (external_id) DO UPDATE
          SET category_slug = EXCLUDED.category_slug,
              source_title = EXCLUDED.source_title,
              source_description = EXCLUDED.source_description,
              source_language = 'en',
              price = EXCLUDED.price,
              rating = EXCLUDED.rating,
              image_url = EXCLUDED.image_url,
              updated_at = NOW()
          `,
          [
            product.externalId,
            product.categorySlug,
            product.title,
            product.description,
            product.price,
            product.rating,
            product.imageUrl,
          ],
        );
      }

      await client.query(
        "DELETE FROM products WHERE NOT (external_id = ANY($1::bigint[]))",
        [sourceExternalIds],
      );
      await client.query(
        "DELETE FROM categories WHERE NOT (slug = ANY($1::text[]))",
        [sourceCategorySlugs],
      );

      const productIdRows = await client.query<ProductIdRow>(
        `
        SELECT
          id::text AS id,
          external_id::text AS externalId
        FROM products
        WHERE external_id = ANY($1::bigint[])
        `,
        [sourceExternalIds],
      );
      const productIdMap = new Map<number, number>(
        productIdRows.rows.map((row) => [Number(row.externalid), Number(row.id)]),
      );

      for (const product of products) {
        const productId = productIdMap.get(product.externalId);

        if (!productId) {
          continue;
        }

        for (const [index, review] of product.reviews.entries()) {
          await client.query(
            `
            INSERT INTO product_reviews (
              product_id,
              source_position,
              rating,
              source_comment,
              reviewer_name,
              review_date,
              updated_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, NOW())
            ON CONFLICT (product_id, source_position) DO UPDATE
            SET rating = EXCLUDED.rating,
                source_comment = EXCLUDED.source_comment,
                reviewer_name = EXCLUDED.reviewer_name,
                review_date = EXCLUDED.review_date,
                updated_at = NOW()
            `,
            [
              productId,
              index,
              review.rating,
              review.comment,
              review.reviewerName,
              review.date,
            ],
          );
        }
      }

      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async getMissingProductTranslations(
    languageCode: SupportedLanguageCode,
    externalProductIds?: readonly string[],
  ): Promise<MissingProductTranslation[]> {
    if (externalProductIds && externalProductIds.length === 0) {
      return [];
    }

    const filteredExternalIds = externalProductIds?.map((externalProductId) => Number(externalProductId));
    const productFilterSql = filteredExternalIds ? " AND p.external_id = ANY($2::bigint[])" : "";
    const queryParams = filteredExternalIds
      ? [languageCode, filteredExternalIds]
      : [languageCode];
    const result = await this.pool.query<MissingProductTranslationRow>(
      `
      SELECT
        p.id::text AS productId,
        p.category_slug AS categorySlug,
        p.source_title AS sourceTitle,
        p.source_description AS sourceDescription
      FROM products p
      LEFT JOIN product_translations pt
        ON pt.product_id = p.id
       AND pt.language_code = $1
      WHERE pt.product_id IS NULL
      ${productFilterSql}
      ORDER BY p.external_id ASC
      `,
      queryParams,
    );

    return result.rows.map((row) => ({
      productId: Number(row.productid),
      categorySlug: row.categoryslug,
      sourceTitle: row.sourcetitle,
      sourceDescription: row.sourcedescription,
    }));
  }

  async saveProductTranslations(
    languageCode: SupportedLanguageCode,
    translations: Array<{ productId: number; title: string; description: string }>,
    provider: string,
  ): Promise<void> {
    if (translations.length === 0) {
      return;
    }

    const client = await this.pool.connect();

    try {
      await client.query("BEGIN");

      for (const translation of translations) {
        await client.query(
          `
          INSERT INTO product_translations (
            product_id,
            language_code,
            title,
            description,
            provider,
            translated_at
          )
          VALUES ($1, $2, $3, $4, $5, NOW())
          ON CONFLICT (product_id, language_code) DO UPDATE
          SET title = EXCLUDED.title,
              description = EXCLUDED.description,
              provider = EXCLUDED.provider,
              translated_at = NOW()
          `,
          [
            translation.productId,
            languageCode,
            translation.title,
            translation.description,
            provider,
          ],
        );
      }

      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async getMissingCategoryTranslations(
    languageCode: SupportedLanguageCode,
    categorySlugs?: readonly string[],
  ): Promise<MissingCategoryTranslation[]> {
    if (categorySlugs && categorySlugs.length === 0) {
      return [];
    }

    const categoryFilterSql = categorySlugs ? " AND c.slug = ANY($2::text[])" : "";
    const queryParams = categorySlugs
      ? [languageCode, categorySlugs]
      : [languageCode];
    const result = await this.pool.query<MissingCategoryTranslationRow>(
      `
      SELECT
        c.slug AS categorySlug,
        c.source_title AS sourceTitle
      FROM categories c
      LEFT JOIN category_translations ct
        ON ct.category_slug = c.slug
       AND ct.language_code = $1
      WHERE ct.category_slug IS NULL
      ${categoryFilterSql}
      ORDER BY c.slug ASC
      `,
      queryParams,
    );

    return result.rows.map((row) => ({
      categorySlug: row.categoryslug,
      sourceTitle: row.sourcetitle,
    }));
  }

  async getProductTranslationSources(): Promise<MissingProductTranslation[]> {
    const result = await this.pool.query<ProductTranslationSourceRow>(
      `
      SELECT
        p.id::text AS productId,
        p.category_slug AS categorySlug,
        p.source_title AS sourceTitle,
        p.source_description AS sourceDescription
      FROM products p
      ORDER BY p.external_id ASC
      `,
    );

    return result.rows.map((row) => ({
      productId: Number(row.productid),
      categorySlug: row.categoryslug,
      sourceTitle: row.sourcetitle,
      sourceDescription: row.sourcedescription,
    }));
  }

  async getCategoryTranslationSources(): Promise<MissingCategoryTranslation[]> {
    const result = await this.pool.query<CategoryTranslationSourceRow>(
      `
      SELECT
        c.slug AS categorySlug,
        c.source_title AS sourceTitle
      FROM categories c
      ORDER BY c.slug ASC
      `,
    );

    return result.rows.map((row) => ({
      categorySlug: row.categoryslug,
      sourceTitle: row.sourcetitle,
    }));
  }

  async getReviewTranslationSources(): Promise<MissingProductReviewTranslation[]> {
    const result = await this.pool.query<MissingProductReviewTranslationRow>(
      `
      SELECT
        pr.id::text AS reviewId,
        pr.source_comment AS sourceComment
      FROM product_reviews pr
      ORDER BY pr.product_id ASC, pr.source_position ASC
      `,
    );

    return result.rows.map((row) => ({
      reviewId: Number(row.reviewid),
      sourceComment: row.sourcecomment,
    }));
  }

  async saveCategoryTranslations(
    languageCode: SupportedLanguageCode,
    translations: Array<{ categorySlug: string; title: string }>,
    provider: string,
  ): Promise<void> {
    if (translations.length === 0) {
      return;
    }

    const client = await this.pool.connect();

    try {
      await client.query("BEGIN");

      for (const translation of translations) {
        await client.query(
          `
          INSERT INTO category_translations (
            category_slug,
            language_code,
            title,
            provider,
            translated_at
          )
          VALUES ($1, $2, $3, $4, NOW())
          ON CONFLICT (category_slug, language_code) DO UPDATE
          SET title = EXCLUDED.title,
              provider = EXCLUDED.provider,
              translated_at = NOW()
          `,
          [translation.categorySlug, languageCode, translation.title, provider],
        );
      }

      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async saveReviewTranslations(
    languageCode: SupportedLanguageCode,
    translations: Array<{ reviewId: number; comment: string }>,
    provider: string,
  ): Promise<void> {
    if (translations.length === 0) {
      return;
    }

    const client = await this.pool.connect();

    try {
      await client.query("BEGIN");

      for (const translation of translations) {
        await client.query(
          `
          INSERT INTO product_review_translations (
            review_id,
            language_code,
            comment,
            provider,
            translated_at
          )
          VALUES ($1, $2, $3, $4, NOW())
          ON CONFLICT (review_id, language_code) DO UPDATE
          SET comment = EXCLUDED.comment,
              provider = EXCLUDED.provider,
              translated_at = NOW()
          `,
          [translation.reviewId, languageCode, translation.comment, provider],
        );
      }

      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async saveCurrencyRates(
    baseCurrency: SupportedCurrencyCode,
    rates: Array<{ currencyCode: SupportedCurrencyCode; rate: number }>,
  ): Promise<void> {
    if (rates.length === 0) {
      return;
    }

    const client = await this.pool.connect();

    try {
      await client.query("BEGIN");

      for (const rate of rates) {
        await client.query(
          `
          INSERT INTO currency_rates (
            base_currency,
            currency_code,
            rate,
            fetched_at
          )
          VALUES ($1, $2, $3, NOW())
          ON CONFLICT (base_currency, currency_code) DO UPDATE
          SET rate = EXCLUDED.rate,
              fetched_at = NOW()
          `,
          [baseCurrency, rate.currencyCode, rate.rate],
        );
      }

      await client.query(
        "DELETE FROM currency_rates WHERE base_currency = $1 AND NOT (currency_code = ANY($2::text[]))",
        [baseCurrency, rates.map((rate) => rate.currencyCode)],
      );

      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }

  async getCurrencyRate(
    baseCurrency: SupportedCurrencyCode,
    currencyCode: SupportedCurrencyCode,
  ): Promise<number | null> {
    const result = await this.pool.query<CurrencyRateRow>(
      `
      SELECT rate
      FROM currency_rates
      WHERE base_currency = $1
        AND currency_code = $2
      `,
      [baseCurrency, currencyCode],
    );

    return result.rows[0]?.rate ?? null;
  }

  async getLocalizedProductDetails(
    params: CatalogProductDetailsParams,
  ): Promise<LocalizedProductDetails | null> {
    const productResult = await this.pool.query<LocalizedProductRow>(
      `
      SELECT
        p.external_id::text AS id,
        COALESCE(pt.title, p.source_title) AS title,
        COALESCE(pt.description, p.source_description) AS description,
        p.price::double precision AS price,
        p.rating::double precision AS rating,
        p.image_url AS imageUrl,
        c.slug AS categorySlug,
        COALESCE(ct.title, c.source_title) AS categoryTitle
      FROM products p
      INNER JOIN categories c
        ON c.slug = p.category_slug
      LEFT JOIN product_translations pt
        ON pt.product_id = p.id
       AND pt.language_code = $1
      LEFT JOIN category_translations ct
        ON ct.category_slug = c.slug
       AND ct.language_code = $1
      WHERE p.external_id::text = $2
      LIMIT 1
      `,
      [params.languageCode, params.productId],
    );

    const productRow = productResult.rows[0];

    if (!productRow) {
      return null;
    }

    const reviewsResult = await this.pool.query<LocalizedProductReviewRow>(
      `
      SELECT
        pr.id::text AS id,
        pr.rating::double precision AS rating,
        COALESCE(prt.comment, pr.source_comment) AS comment,
        pr.review_date AS reviewDate,
        pr.reviewer_name AS reviewerName
      FROM product_reviews pr
      INNER JOIN products p
        ON p.id = pr.product_id
      LEFT JOIN product_review_translations prt
        ON prt.review_id = pr.id
       AND prt.language_code = $1
      WHERE p.external_id::text = $2
      ORDER BY pr.source_position ASC
      `,
      [params.languageCode, params.productId],
    );

    return {
      language: params.languageCode,
      currency: params.currencyCode,
      product: mapLocalizedProductRow(productRow),
      reviews: reviewsResult.rows.map((row) => ({
        id: row.id,
        rating: row.rating,
        comment: row.comment,
        date: row.reviewdate,
        reviewerName: row.reviewername,
      })),
    };
  }

  async getLocalizedCatalog(params: CatalogListParams): Promise<LocalizedCatalog> {
    const normalizedQuery = params.query.trim();
    const offset = (params.page - 1) * params.pageSize;
    const orderByClause = buildOrderByClause(params.sort);

    const categoriesResult = await this.pool.query<LocalizedCategoryRow>(
      `
      SELECT
        c.slug AS slug,
        COALESCE(ct.title, c.source_title) AS title
      FROM categories c
      LEFT JOIN category_translations ct
        ON ct.category_slug = c.slug
       AND ct.language_code = $1
      ORDER BY c.slug ASC
      `,
      [params.languageCode],
    );

    const countResult = await this.pool.query<LocalizedCountRow>(
      `
      SELECT COUNT(*)::text AS totalCount
      FROM products p
      INNER JOIN categories c
        ON c.slug = p.category_slug
      LEFT JOIN product_translations pt
        ON pt.product_id = p.id
       AND pt.language_code = $1
      LEFT JOIN category_translations ct
        ON ct.category_slug = c.slug
       AND ct.language_code = $1
      WHERE
        ($2::text = '' OR
          COALESCE(pt.title, p.source_title) ILIKE '%' || $2 || '%' OR
          COALESCE(pt.description, p.source_description) ILIKE '%' || $2 || '%')
        AND ($3::text IS NULL OR c.slug = $3)
      `,
      [
        params.languageCode,
        normalizedQuery,
        params.categorySlug,
      ],
    );

    const productsResult = await this.pool.query<LocalizedProductRow>(
      `
      SELECT
        p.external_id::text AS id,
        COALESCE(pt.title, p.source_title) AS title,
        COALESCE(pt.description, p.source_description) AS description,
        p.price::double precision AS price,
        p.rating::double precision AS rating,
        p.image_url AS imageUrl,
        c.slug AS categorySlug,
        COALESCE(ct.title, c.source_title) AS categoryTitle
      FROM products p
      INNER JOIN categories c
        ON c.slug = p.category_slug
      LEFT JOIN product_translations pt
        ON pt.product_id = p.id
       AND pt.language_code = $1
      LEFT JOIN category_translations ct
        ON ct.category_slug = c.slug
       AND ct.language_code = $1
      WHERE
        ($2::text = '' OR
          COALESCE(pt.title, p.source_title) ILIKE '%' || $2 || '%' OR
          COALESCE(pt.description, p.source_description) ILIKE '%' || $2 || '%')
        AND ($3::text IS NULL OR c.slug = $3)
      ORDER BY ${orderByClause}
      LIMIT $4
      OFFSET $5
      `,
      [
        params.languageCode,
        normalizedQuery,
        params.categorySlug,
        params.pageSize,
        offset,
      ],
    );

    const categories: LocalizedCategory[] = categoriesResult.rows.map((row) => ({
      slug: row.slug,
      title: row.title,
    }));

    const products: LocalizedProduct[] = productsResult.rows.map(mapLocalizedProductRow);

    const totalProducts = Number(countResult.rows[0]?.totalcount ?? "0");
    const totalPages = totalProducts === 0 ? 0 : Math.ceil(totalProducts / params.pageSize);

    return {
      language: params.languageCode,
      currency: params.currencyCode,
      categories,
      products,
      totalProducts,
      page: params.page,
      pageSize: params.pageSize,
      totalPages,
      query: normalizedQuery,
      categorySlug: params.categorySlug,
      sort: params.sort,
    };
  }
}

function mapLocalizedProductRow(row: LocalizedProductRow): LocalizedProduct {
  return {
    id: row.id,
    title: row.title,
    description: row.description,
    price: {
      amount: row.price,
      currency: "USD",
    },
    rating: row.rating,
    imageUrl: row.imageurl,
    category: {
      slug: row.categoryslug,
      title: row.categorytitle,
    },
  };
}

function buildOrderByClause(sort: CatalogSortOption): string {
  switch (sort) {
    case "price_asc":
      return "p.price ASC, COALESCE(pt.title, p.source_title) ASC, p.external_id ASC";
    case "price_desc":
      return "p.price DESC, COALESCE(pt.title, p.source_title) ASC, p.external_id ASC";
    case "rating_desc":
      return "p.rating DESC, COALESCE(pt.title, p.source_title) ASC, p.external_id ASC";
    default:
      return "p.external_id ASC";
  }
}
