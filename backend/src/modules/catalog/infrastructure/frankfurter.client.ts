import { AppError } from "../../../shared/errors/app-error";
import type { SupportedCurrencyCode } from "../../../shared/constants/currencies";

interface FrankfurterRateItemDto {
  base: string;
  quote: string;
  rate: number;
}

export class FrankfurterClient {
  private static readonly ApiBaseUrl = "https://api.frankfurter.dev/v2";

  async fetchLatestRates(
    baseCurrency: SupportedCurrencyCode,
    quoteCurrencies: SupportedCurrencyCode[],
  ): Promise<Array<{ currencyCode: SupportedCurrencyCode; rate: number }>> {
    if (quoteCurrencies.length === 0) {
      return [];
    }

    const requestUrl = new URL(`${FrankfurterClient.ApiBaseUrl}/rates`);
    requestUrl.searchParams.set("base", baseCurrency);
    requestUrl.searchParams.set("quotes", quoteCurrencies.join(","));

    const response = await fetch(requestUrl);

    if (!response.ok) {
      const errorText = await response.text();
      throw new AppError(
        502,
        "EXCHANGE_RATE_REQUEST_FAILED",
        `Frankfurter request failed with status ${response.status}: ${errorText}`,
      );
    }

    const payload = (await response.json()) as FrankfurterRateItemDto[];

    if (payload.length !== quoteCurrencies.length) {
      throw new AppError(
        502,
        "EXCHANGE_RATE_RESPONSE_INVALID",
        "Frankfurter returned an unexpected number of exchange rates",
      );
    }

    return payload.map((item) => ({
      currencyCode: item.quote as SupportedCurrencyCode,
      rate: item.rate,
    }));
  }
}
