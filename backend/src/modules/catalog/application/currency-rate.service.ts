import {
  sourceCurrencyCode,
  supportedCurrencyCodes,
  type SupportedCurrencyCode,
} from "../../../shared/constants/currencies";
import { AppError } from "../../../shared/errors/app-error";
import type { LocalizedCatalog, LocalizedProductDetails } from "../domain/catalog.types";
import { CatalogRepository } from "../infrastructure/catalog.repository";
import { FrankfurterClient } from "../infrastructure/frankfurter.client";

export class CurrencyRateService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
    private readonly frankfurterClient: FrankfurterClient,
  ) {}

  async ensureRatesAvailable(): Promise<void> {
    const availableRates = await this.catalogRepository.countCurrencyRates(sourceCurrencyCode);

    if (availableRates > 0) {
      return;
    }

    await this.refreshRates();
  }

  async refreshRates(): Promise<{ rates: number }> {
    const quoteCurrencies = supportedCurrencyCodes.filter((currencyCode) => currencyCode !== sourceCurrencyCode);
    const rates = await this.frankfurterClient.fetchLatestRates(sourceCurrencyCode, quoteCurrencies);
    await this.catalogRepository.saveCurrencyRates(sourceCurrencyCode, rates);

    return {
      rates: rates.length,
    };
  }

  async convertCatalog(
    catalog: LocalizedCatalog,
  currencyCode: SupportedCurrencyCode,
  ): Promise<LocalizedCatalog> {
    if (currencyCode === sourceCurrencyCode) {
      return {
        ...catalog,
        currency: currencyCode,
        products: catalog.products.map((product) => ({
          ...product,
          price: {
            amount: roundMoney(product.price.amount),
            currency: currencyCode,
          },
        })),
      };
    }

    const exchangeRate = await this.getExchangeRateOrThrow(currencyCode);

    return {
      ...catalog,
      currency: currencyCode,
      products: catalog.products.map((product) => ({
        ...product,
        price: {
          amount: roundMoney(product.price.amount * exchangeRate),
          currency: currencyCode,
        },
        })),
      };
  }

  async convertProductDetails(
    productDetails: LocalizedProductDetails,
    currencyCode: SupportedCurrencyCode,
  ): Promise<LocalizedProductDetails> {
    if (currencyCode === sourceCurrencyCode) {
      return {
        ...productDetails,
        currency: currencyCode,
        product: {
          ...productDetails.product,
          price: {
            amount: roundMoney(productDetails.product.price.amount),
            currency: currencyCode,
          },
        },
      };
    }

    const exchangeRate = await this.getExchangeRateOrThrow(currencyCode);

    return {
      ...productDetails,
      currency: currencyCode,
      product: {
        ...productDetails.product,
        price: {
          amount: roundMoney(productDetails.product.price.amount * exchangeRate),
          currency: currencyCode,
        },
      },
    };
  }

  private async getExchangeRateOrThrow(
    currencyCode: SupportedCurrencyCode,
  ): Promise<number> {
    const exchangeRate = await this.catalogRepository.getCurrencyRate(sourceCurrencyCode, currencyCode);

    if (exchangeRate !== null) {
      return exchangeRate;
    }

    throw new AppError(
      503,
      "EXCHANGE_RATES_UNAVAILABLE",
      "Currency exchange rates are not available yet. Background synchronization is still in progress.",
    );
  }
}

function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}
