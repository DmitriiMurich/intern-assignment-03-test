import { sourceLanguageCode, type SupportedLanguageCode } from "../../../shared/constants/languages";
import { AppError } from "../../../shared/errors/app-error";
import { chunkStrings } from "../../../shared/utils/text";

interface LibreTranslateResponseDto {
  translatedText: string | string[];
}

export class LibreTranslateClient {
  constructor(
    private readonly apiUrl: string,
  ) {}

  async translateTexts(
    texts: string[],
    targetLanguage: SupportedLanguageCode,
  ): Promise<string[]> {
    if (texts.length === 0 || targetLanguage === sourceLanguageCode) {
      return texts;
    }

    const translatedChunks: string[] = [];

    for (const chunk of chunkStrings(texts, 50)) {
      const response = await this.requestTranslation(chunk, targetLanguage);

      if (!response.ok) {
        const errorText = await response.text();
        throw new AppError(
          502,
          "LIBRETRANSLATE_REQUEST_FAILED",
          `LibreTranslate request failed with status ${response.status}: ${errorText}`,
        );
      }

      const payload = (await response.json()) as LibreTranslateResponseDto;
      const translatedTexts = Array.isArray(payload.translatedText)
        ? payload.translatedText
        : [payload.translatedText];

      if (translatedTexts.length !== chunk.length) {
        throw new AppError(
          502,
          "LIBRETRANSLATE_RESPONSE_INVALID",
          "LibreTranslate returned an unexpected number of translated texts",
        );
      }

      translatedChunks.push(...translatedTexts);
    }

    return translatedChunks;
  }

  private async requestTranslation(
    texts: string[],
    targetLanguage: SupportedLanguageCode,
  ): Promise<Response> {
    try {
      return await fetch(new URL("/translate", this.apiUrl), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          q: texts,
          source: sourceLanguageCode,
          target: targetLanguage,
          format: "text",
        }),
      });
    } catch (error) {
      throw new AppError(
        503,
        "TRANSLATION_PROVIDER_UNAVAILABLE",
        error instanceof Error
          ? `LibreTranslate is unavailable: ${error.message}`
          : "LibreTranslate is unavailable",
      );
    }
  }
}
