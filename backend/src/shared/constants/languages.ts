export const sourceLanguageCode = "en" as const;

export const supportedLanguageCodes = [
  sourceLanguageCode,
  "ru",
  "de",
  "fr",
  "es",
  "it",
  "pt",
  "tr",
  "uk",
  "zh",
] as const;

export type SupportedLanguageCode = (typeof supportedLanguageCodes)[number];

const languageNameMap: Record<SupportedLanguageCode, string> = {
  en: "English",
  ru: "Russian",
  de: "German",
  fr: "French",
  es: "Spanish",
  it: "Italian",
  pt: "Portuguese",
  tr: "Turkish",
  uk: "Ukrainian",
  zh: "Chinese",
};

export function isSupportedLanguageCode(value: string): value is SupportedLanguageCode {
  return supportedLanguageCodes.includes(value as SupportedLanguageCode);
}

export function languageDisplayName(languageCode: SupportedLanguageCode): string {
  return languageNameMap[languageCode];
}
