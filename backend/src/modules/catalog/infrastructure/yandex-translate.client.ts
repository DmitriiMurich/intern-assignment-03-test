import {
  sourceLanguageCode,
  toYandexLanguageCode,
  type SupportedLanguageCode,
} from "../../../shared/constants/languages.js";
import { AppError } from "../../../shared/errors/app-error.js";
import { chunkStrings } from "../../../shared/utils/text.js";

interface YandexTranslateResponseDto {
  translations: Array<{
    text: string;
    detectedLanguageCode?: string;
  }>;
}

export class YandexTranslateClient {
  constructor(
    private readonly apiUrl: string,
    private readonly apiKey: string,
    private readonly folderId: string,
  ) {}

  async translateTexts(
    texts: string[],
    targetLanguage: SupportedLanguageCode,
  ): Promise<string[]> {
    if (texts.length === 0 || targetLanguage === sourceLanguageCode) {
      return texts;
    }

    if (this.apiKey.length === 0) {
      throw new AppError(
        503,
        "TRANSLATION_PROVIDER_NOT_CONFIGURED",
        "YANDEX_TRANSLATE_API_KEY is required for translated catalog requests",
      );
    }

    if (this.folderId.length === 0) {
      throw new AppError(
        503,
        "TRANSLATION_PROVIDER_NOT_CONFIGURED",
        "YANDEX_FOLDER_ID is required for translated catalog requests",
      );
    }

    const translatedChunks: string[] = [];

    for (const chunk of chunkStrings(texts, 50)) {
      const response = await fetch(this.apiUrl, {
        method: "POST",
        headers: {
          Authorization: `Api-Key ${this.apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          folderId: this.folderId,
          sourceLanguageCode: toYandexLanguageCode(sourceLanguageCode),
          targetLanguageCode: toYandexLanguageCode(targetLanguage),
          texts: chunk,
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new AppError(
          502,
          "YANDEX_TRANSLATION_REQUEST_FAILED",
          `Yandex Translate request failed with status ${response.status}: ${errorText}`,
        );
      }

      const payload = (await response.json()) as YandexTranslateResponseDto;

      if (payload.translations.length !== chunk.length) {
        throw new AppError(
          502,
          "YANDEX_TRANSLATION_RESPONSE_INVALID",
          "Yandex Translate returned an unexpected number of translated texts",
        );
      }

      translatedChunks.push(...payload.translations.map((translation) => translation.text));
    }

    return translatedChunks;
  }
}
