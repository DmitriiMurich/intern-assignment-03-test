import {
  sourceLanguageCode,
  supportedLanguageCodes,
  type SupportedLanguageCode,
} from "../../../shared/constants/languages";
import type { MissingCategoryTranslation, MissingProductTranslation } from "../domain/catalog.types";
import { CatalogRepository } from "../infrastructure/catalog.repository";

const translationProvider = "mock-seeded";

const categoryTitleTranslations: Record<string, Record<Exclude<SupportedLanguageCode, "en">, string>> = {
  beauty: {
    ru: "Красота",
    de: "Schoenheit",
    fr: "Beaute",
    es: "Belleza",
    it: "Bellezza",
    pt: "Beleza",
    tr: "Guzellik",
    uk: "Краса",
    zh: "美妆",
  },
  fragrances: {
    ru: "Ароматы",
    de: "Duefte",
    fr: "Parfums",
    es: "Fragancias",
    it: "Fragranze",
    pt: "Fragrancias",
    tr: "Parfumler",
    uk: "Аромати",
    zh: "香氛",
  },
  furniture: {
    ru: "Мебель",
    de: "Moebel",
    fr: "Meubles",
    es: "Muebles",
    it: "Mobili",
    pt: "Moveis",
    tr: "Mobilya",
    uk: "Меблi",
    zh: "家具",
  },
  groceries: {
    ru: "Продукты",
    de: "Lebensmittel",
    fr: "Epicerie",
    es: "Alimentos",
    it: "Alimentari",
    pt: "Mercearia",
    tr: "Market Urunleri",
    uk: "Продукти",
    zh: "杂货",
  },
  "home-decoration": {
    ru: "Домашний декор",
    de: "Wohnaccessoires",
    fr: "Decoration interieure",
    es: "Decoracion del hogar",
    it: "Decorazioni per la casa",
    pt: "Decoracao para casa",
    tr: "Ev dekorasyonu",
    uk: "Декор для дому",
    zh: "家居装饰",
  },
  "kitchen-accessories": {
    ru: "Кухонные аксессуары",
    de: "Kuechenzubehoer",
    fr: "Accessoires de cuisine",
    es: "Accesorios de cocina",
    it: "Accessori da cucina",
    pt: "Acessorios de cozinha",
    tr: "Mutfak aksesuarlari",
    uk: "Кухоннi аксесуари",
    zh: "厨房配件",
  },
  laptops: {
    ru: "Ноутбуки",
    de: "Laptops",
    fr: "Ordinateurs portables",
    es: "Portatiles",
    it: "Laptop",
    pt: "Laptops",
    tr: "Dizustu bilgisayarlar",
    uk: "Ноутбуки",
    zh: "笔记本电脑",
  },
  "mens-shirts": {
    ru: "Мужские рубашки",
    de: "Herrenhemden",
    fr: "Chemises homme",
    es: "Camisas de hombre",
    it: "Camicie da uomo",
    pt: "Camisas masculinas",
    tr: "Erkek gomlekleri",
    uk: "Чоловiчi сорочки",
    zh: "男士衬衫",
  },
  "mens-shoes": {
    ru: "Мужская обувь",
    de: "Herrenschuhe",
    fr: "Chaussures homme",
    es: "Zapatos de hombre",
    it: "Scarpe da uomo",
    pt: "Sapatos masculinos",
    tr: "Erkek ayakkabilari",
    uk: "Чоловiче взуття",
    zh: "男士鞋履",
  },
  "mens-watches": {
    ru: "Мужские часы",
    de: "Herrenuhren",
    fr: "Montres homme",
    es: "Relojes de hombre",
    it: "Orologi da uomo",
    pt: "Relogios masculinos",
    tr: "Erkek saatleri",
    uk: "Чоловiчi годинники",
    zh: "男士手表",
  },
  "mobile-accessories": {
    ru: "Аксессуары для телефона",
    de: "Handyzubehoer",
    fr: "Accessoires mobiles",
    es: "Accesorios moviles",
    it: "Accessori per cellulari",
    pt: "Acessorios moveis",
    tr: "Mobil aksesuarlar",
    uk: "Мобiльнi аксесуари",
    zh: "手机配件",
  },
  motorcycle: {
    ru: "Мотоцикл",
    de: "Motorrad",
    fr: "Moto",
    es: "Motocicleta",
    it: "Motocicletta",
    pt: "Motocicleta",
    tr: "Motosiklet",
    uk: "Мотоцикл",
    zh: "摩托车",
  },
  "skin-care": {
    ru: "Уход за кожей",
    de: "Hautpflege",
    fr: "Soins de la peau",
    es: "Cuidado de la piel",
    it: "Cura della pelle",
    pt: "Cuidados com a pele",
    tr: "Cilt bakimi",
    uk: "Догляд за шкiрою",
    zh: "护肤",
  },
  smartphones: {
    ru: "Смартфоны",
    de: "Smartphones",
    fr: "Smartphones",
    es: "Smartphones",
    it: "Smartphone",
    pt: "Smartphones",
    tr: "Akilli telefonlar",
    uk: "Смартфони",
    zh: "智能手机",
  },
  "sports-accessories": {
    ru: "Спортивные аксессуары",
    de: "Sportzubehoer",
    fr: "Accessoires de sport",
    es: "Accesorios deportivos",
    it: "Accessori sportivi",
    pt: "Acessorios esportivos",
    tr: "Spor aksesuarlari",
    uk: "Спортивнi аксесуари",
    zh: "运动配件",
  },
  sunglasses: {
    ru: "Солнцезащитные очки",
    de: "Sonnenbrillen",
    fr: "Lunettes de soleil",
    es: "Gafas de sol",
    it: "Occhiali da sole",
    pt: "Oculos de sol",
    tr: "Gunes gozlukleri",
    uk: "Сонцезахиснi окуляри",
    zh: "太阳镜",
  },
  tablets: {
    ru: "Планшеты",
    de: "Tablets",
    fr: "Tablettes",
    es: "Tabletas",
    it: "Tablet",
    pt: "Tablets",
    tr: "Tabletler",
    uk: "Планшети",
    zh: "平板电脑",
  },
  tops: {
    ru: "Топы",
    de: "Oberteile",
    fr: "Tops",
    es: "Tops",
    it: "Top",
    pt: "Tops",
    tr: "Ust giyim",
    uk: "Топи",
    zh: "上衣",
  },
  vehicle: {
    ru: "Транспорт",
    de: "Fahrzeuge",
    fr: "Vehicules",
    es: "Vehiculos",
    it: "Veicoli",
    pt: "Veiculos",
    tr: "Araclar",
    uk: "Транспорт",
    zh: "车辆",
  },
  "womens-bags": {
    ru: "Женские сумки",
    de: "Damenhandtaschen",
    fr: "Sacs femme",
    es: "Bolsos de mujer",
    it: "Borse da donna",
    pt: "Bolsas femininas",
    tr: "Kadin cantalari",
    uk: "Жiночi сумки",
    zh: "女士包袋",
  },
  "womens-dresses": {
    ru: "Женские платья",
    de: "Damenkleider",
    fr: "Robes femme",
    es: "Vestidos de mujer",
    it: "Abiti da donna",
    pt: "Vestidos femininos",
    tr: "Kadin elbiseleri",
    uk: "Жiночi сукнi",
    zh: "女士连衣裙",
  },
  "womens-jewellery": {
    ru: "Женские украшения",
    de: "Damenschmuck",
    fr: "Bijoux femme",
    es: "Joyeria de mujer",
    it: "Gioielli da donna",
    pt: "Joias femininas",
    tr: "Kadin takilari",
    uk: "Жiночi прикраси",
    zh: "女士珠宝",
  },
  "womens-shoes": {
    ru: "Женская обувь",
    de: "Damenschuhe",
    fr: "Chaussures femme",
    es: "Zapatos de mujer",
    it: "Scarpe da donna",
    pt: "Sapatos femininos",
    tr: "Kadin ayakkabilari",
    uk: "Жiноче взуття",
    zh: "女士鞋履",
  },
  "womens-watches": {
    ru: "Женские часы",
    de: "Damenuhren",
    fr: "Montres femme",
    es: "Relojes de mujer",
    it: "Orologi da donna",
    pt: "Relogios femininos",
    tr: "Kadin saatleri",
    uk: "Жiночi годинники",
    zh: "女士手表",
  },
};

const pseudoLocalizationMaps: Record<Exclude<SupportedLanguageCode, "en">, Record<string, string>> = {
  ru: {
    a: "а", b: "б", c: "к", d: "д", e: "е", f: "ф", g: "г", h: "х", i: "и", j: "й", k: "к", l: "л",
    m: "м", n: "н", o: "о", p: "п", q: "к", r: "р", s: "с", t: "т", u: "у", v: "в", w: "ш", x: "кс", y: "ы", z: "з",
    A: "А", B: "Б", C: "К", D: "Д", E: "Е", F: "Ф", G: "Г", H: "Х", I: "И", J: "Й", K: "К", L: "Л",
    M: "М", N: "Н", O: "О", P: "П", Q: "К", R: "Р", S: "С", T: "Т", U: "У", V: "В", W: "Ш", X: "КС", Y: "Ы", Z: "З",
  },
  de: {
    a: "ä", A: "Ä", o: "ö", O: "Ö", u: "ü", U: "Ü", s: "ß", S: "ẞ",
  },
  fr: {
    a: "à", A: "À", e: "é", E: "É", i: "î", I: "Î", o: "ô", O: "Ô", u: "û", U: "Û", c: "ç", C: "Ç",
  },
  es: {
    a: "á", A: "Á", e: "é", E: "É", i: "í", I: "Í", o: "ó", O: "Ó", u: "ú", U: "Ú", n: "ñ", N: "Ñ",
  },
  it: {
    a: "à", A: "À", e: "è", E: "È", i: "ì", I: "Ì", o: "ò", O: "Ò", u: "ù", U: "Ù",
  },
  pt: {
    a: "ã", A: "Ã", e: "ê", E: "Ê", i: "í", I: "Í", o: "õ", O: "Õ", u: "ú", U: "Ú", c: "ç", C: "Ç",
  },
  tr: {
    c: "ç", C: "Ç", g: "ğ", G: "Ğ", i: "ı", I: "İ", o: "ö", O: "Ö", s: "ş", S: "Ş", u: "ü", U: "Ü",
  },
  uk: {
    a: "а", b: "б", c: "к", d: "д", e: "е", f: "ф", g: "г", h: "х", i: "i", j: "й", k: "к", l: "л",
    m: "м", n: "н", o: "о", p: "п", q: "к", r: "р", s: "с", t: "т", u: "у", v: "в", w: "в", x: "кс", y: "и", z: "з",
    A: "А", B: "Б", C: "К", D: "Д", E: "Е", F: "Ф", G: "Г", H: "Х", I: "I", J: "Й", K: "К", L: "Л",
    M: "М", N: "Н", O: "О", P: "П", Q: "К", R: "Р", S: "С", T: "Т", U: "У", V: "В", W: "В", X: "КС", Y: "И", Z: "З",
  },
  zh: {},
};

export class MockTranslationSeedService {
  constructor(
    private readonly catalogRepository: CatalogRepository,
  ) {}

  async seedAllTranslations(): Promise<void> {
    const [categorySources, productSources] = await Promise.all([
      this.catalogRepository.getCategoryTranslationSources(),
      this.catalogRepository.getProductTranslationSources(),
    ]);

    for (const languageCode of supportedLanguageCodes) {
      if (languageCode === sourceLanguageCode) {
        continue;
      }

      const targetLanguage = languageCode as Exclude<SupportedLanguageCode, "en">;

      await this.catalogRepository.saveCategoryTranslations(
        targetLanguage,
        categorySources.map((category) => ({
          categorySlug: category.categorySlug,
          title: mockTranslateCategory(category, targetLanguage),
        })),
        translationProvider,
      );

      await this.catalogRepository.saveProductTranslations(
        targetLanguage,
        productSources.map((product) => ({
          productId: product.productId,
          title: mockTranslateText(product.sourceTitle, targetLanguage),
          description: mockTranslateText(product.sourceDescription, targetLanguage),
        })),
        translationProvider,
      );
    }
  }
}

function mockTranslateCategory(
  category: MissingCategoryTranslation,
  languageCode: Exclude<SupportedLanguageCode, "en">,
): string {
  return categoryTitleTranslations[category.categorySlug]?.[languageCode]
    ?? mockTranslateText(category.sourceTitle, languageCode);
}

export function mockTranslateText(
  text: string,
  languageCode: Exclude<SupportedLanguageCode, "en">,
): string {
  if (text.trim().length === 0) {
    return text;
  }

  if (languageCode === "zh") {
    return `「${toFullWidth(text)}」`;
  }

  const characterMap = pseudoLocalizationMaps[languageCode];
  return Array.from(text)
    .map((character) => characterMap[character] ?? character)
    .join("");
}

function toFullWidth(text: string): string {
  return Array.from(text)
    .map((character) => {
      const codePoint = character.codePointAt(0);

      if (codePoint === 32) {
        return "\u3000";
      }

      if (codePoint && codePoint >= 33 && codePoint <= 126) {
        return String.fromCodePoint(codePoint + 65248);
      }

      return character;
    })
    .join("");
}
