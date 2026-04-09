export const sourceCurrencyCode = "USD" as const;

export const supportedCurrencyCodes = [
  sourceCurrencyCode,
  "EUR",
  "RUB",
  "GBP",
  "UAH",
  "TRY",
  "CNY",
  "JPY",
  "CAD",
  "CHF",
] as const;

export type SupportedCurrencyCode = (typeof supportedCurrencyCodes)[number];

interface CurrencyMetadata {
  name: string;
  symbol: string;
  isSourceCurrency: boolean;
}

const currencyMetadataMap: Record<SupportedCurrencyCode, CurrencyMetadata> = {
  USD: { name: "US Dollar", symbol: "$", isSourceCurrency: true },
  EUR: { name: "Euro", symbol: "\u20AC", isSourceCurrency: false },
  RUB: { name: "Russian Ruble", symbol: "\u20BD", isSourceCurrency: false },
  GBP: { name: "British Pound", symbol: "\u00A3", isSourceCurrency: false },
  UAH: { name: "Ukrainian Hryvnia", symbol: "\u20B4", isSourceCurrency: false },
  TRY: { name: "Turkish Lira", symbol: "\u20BA", isSourceCurrency: false },
  CNY: { name: "Chinese Yuan", symbol: "\u00A5", isSourceCurrency: false },
  JPY: { name: "Japanese Yen", symbol: "\u00A5", isSourceCurrency: false },
  CAD: { name: "Canadian Dollar", symbol: "C$", isSourceCurrency: false },
  CHF: { name: "Swiss Franc", symbol: "CHF", isSourceCurrency: false },
};

export function currencyDisplayName(currencyCode: SupportedCurrencyCode): string {
  return currencyMetadataMap[currencyCode].name;
}

export function currencySymbol(currencyCode: SupportedCurrencyCode): string {
  return currencyMetadataMap[currencyCode].symbol;
}

export function isSourceCurrency(currencyCode: SupportedCurrencyCode): boolean {
  return currencyMetadataMap[currencyCode].isSourceCurrency;
}
