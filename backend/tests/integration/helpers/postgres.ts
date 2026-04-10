import { Pool } from "pg";

const defaultDatabaseUrl = "postgresql://catalog:catalog@127.0.0.1:5433/catalog";

export interface TestDatabaseContext {
  pool: Pool;
  schemaName: string;
  dispose(): Promise<void>;
}

export async function createTestDatabase(): Promise<TestDatabaseContext> {
  const baseConnectionString = process.env.TEST_DATABASE_URL ?? process.env.DATABASE_URL ?? defaultDatabaseUrl;
  const schemaName = `test_${Date.now()}_${Math.random().toString(16).slice(2, 10)}`;
  const adminPool = new Pool({
    connectionString: baseConnectionString,
  });

  try {
    await adminPool.query(`CREATE SCHEMA "${schemaName}"`);
  } catch (error) {
    await adminPool.end();
    throw new Error(
      error instanceof Error
        ? `PostgreSQL integration tests require an available database at ${baseConnectionString}: ${error.message}`
        : `PostgreSQL integration tests require an available database at ${baseConnectionString}`,
    );
  }

  const scopedConnectionString = new URL(baseConnectionString);
  scopedConnectionString.searchParams.set("options", `-c search_path=${schemaName}`);

  const pool = new Pool({
    connectionString: scopedConnectionString.toString(),
  });

  return {
    pool,
    schemaName,
    async dispose() {
      await pool.end();
      await adminPool.query(`DROP SCHEMA IF EXISTS "${schemaName}" CASCADE`);
      await adminPool.end();
    },
  };
}
