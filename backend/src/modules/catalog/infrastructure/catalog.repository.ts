import { Pool } from "pg";
import type { SupportedLanguageCode } from "../../../shared/constants/languages.js";
import type {
  CatalogListParams,
  CatalogSortOption,
  LocalizedCatalog,
  LocalizedCategory,
  LocalizedProduct,
  MissingCategoryTranslation,
  MissingProductTranslation,
  SourceCategory,
  SourceProduct,
} from "../domain/catalog.types.js";

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

CREATE INDEX IF NOT EXISTS idx_products_category_slug ON products(category_slug);
CREATE INDEX IF NOT EXISTS idx_product_translations_language ON product_translations(language_code);
CREATE INDEX IF NOT EXISTS idx_category_translations_language ON category_translations(language_code);
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

interface MissingProductTranslationRow {
  productid: string;
  sourcetitle: string;
  sourcedescription: string;
}

interface MissingCategoryTranslationRow {
  categoryslug: string;
  sourcetitle: string;
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

  async replaceSourceCatalog(
    products: SourceProduct[],
    categories: SourceCategory[],
  ): Promise<void> {
    const client = await this.pool.connect();

    try {
      await client.query("BEGIN");
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
        [products.map((product) => product.externalId)],
      );
      await client.query(
        "DELETE FROM categories WHERE NOT (slug = ANY($1::text[]))",
        [categories.map((category) => category.slug)],
      );

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
  ): Promise<MissingProductTranslation[]> {
    const result = await this.pool.query<MissingProductTranslationRow>(
      `
      SELECT
        p.id::text AS productId,
        p.source_title AS sourceTitle,
        p.source_description AS sourceDescription
      FROM products p
      LEFT JOIN product_translations pt
        ON pt.product_id = p.id
       AND pt.language_code = $1
      WHERE pt.product_id IS NULL
      ORDER BY p.external_id ASC
      `,
      [languageCode],
    );

    return result.rows.map((row) => ({
      productId: Number(row.productid),
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
  ): Promise<MissingCategoryTranslation[]> {
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
      ORDER BY c.slug ASC
      `,
      [languageCode],
    );

    return result.rows.map((row) => ({
      categorySlug: row.categoryslug,
      sourceTitle: row.sourcetitle,
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

    const products: LocalizedProduct[] = productsResult.rows.map((row) => ({
      id: row.id,
      title: row.title,
      description: row.description,
      price: row.price,
      rating: row.rating,
      imageUrl: row.imageurl,
      category: {
        slug: row.categoryslug,
        title: row.categorytitle,
      },
    }));

    const totalProducts = Number(countResult.rows[0]?.totalcount ?? "0");
    const totalPages = totalProducts === 0 ? 0 : Math.ceil(totalProducts / params.pageSize);

    return {
      language: params.languageCode,
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
