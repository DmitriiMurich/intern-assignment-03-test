import dotenv from "dotenv";

dotenv.config();

function readString(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;

  if (!value || value.trim().length === 0) {
    throw new Error(`Environment variable ${name} is required`);
  }

  return value.trim();
}

function readNumber(name: string, fallback: number): number {
  const rawValue = process.env[name];

  if (!rawValue) {
    return fallback;
  }

  const parsedValue = Number(rawValue);

  if (!Number.isInteger(parsedValue) || parsedValue <= 0) {
    throw new Error(`Environment variable ${name} must be a positive integer`);
  }

  return parsedValue;
}

function readUrl(name: string, fallback?: string): string {
  const value = readString(name, fallback);
  new URL(value);
  return value;
}

export const env = {
  host: readString("HOST", "0.0.0.0"),
  port: readNumber("PORT", 8080),
  databaseUrl: readUrl("DATABASE_URL"),
  dummyJsonBaseUrl: readUrl("DUMMYJSON_BASE_URL", "https://dummyjson.com"),
  yandexTranslateApiUrl: readUrl(
    "YANDEX_TRANSLATE_API_URL",
    "https://translate.api.cloud.yandex.net/translate/v2/translate",
  ),
  yandexTranslateApiKey: process.env.YANDEX_TRANSLATE_API_KEY?.trim() ?? "",
  yandexFolderId: process.env.YANDEX_FOLDER_ID?.trim() ?? "",
};
