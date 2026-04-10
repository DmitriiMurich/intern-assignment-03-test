# Backend

## Что Это

`backend/` — это BFF-сервис для Android-клиента каталога.

Он нужен, чтобы:

- не ходить из мобильного приложения напрямую в `DummyJSON`;
- хранить source-каталог и локализованные данные в своей базе;
- отдавать приложению один стабильный API-контракт;
- переводить каталог и отзывы на сервере;
- конвертировать цены в выбранную валюту;
- централизовать серверные поиск, фильтрацию, сортировку и пагинацию.

## Стек

- Node.js `20+`
- TypeScript
- Fastify
- PostgreSQL
- Jest
- Docker Compose

Внешние сервисы:

- `DummyJSON` — источник source-товаров и отзывов;
- `Frankfurter` — поставщик валютных курсов.

## Как Это Работает

```text
mobile app -> backend -> DummyJSON
                     -> Frankfurter
                     -> PostgreSQL
```

### Поведение На Старте

При запуске backend:

1. создает и проверяет схему в `PostgreSQL`;
2. полностью перезагружает source-каталог из `DummyJSON`;
3. пересеивает локализации для всех поддерживаемых не-английских языков;
4. проверяет наличие валютных курсов и при необходимости обновляет их.

Важно:

- каталог синхронизируется при каждом старте backend;
- почасовой sync каталога больше не используется;
- почасовым остался только refresh валютных курсов.

### Локализация

Сейчас локализация каталога сделана как demo-friendly server-side решение:

- source-язык — `en`;
- для остальных языков backend заполняет curated static translations;
- переводятся категории, названия товаров, описания и комментарии отзывов.

Это не live machine translation и не внешний платный переводчик, а контролируемые статические переводы, пригодные для демонстрации сценария локализованного каталога.

## Поддерживаемые Языки И Валюты

### Языки

- `en`
- `ru`
- `de`
- `fr`
- `es`
- `it`
- `pt`
- `tr`
- `uk`
- `zh`

### Валюты

- `USD`
- `EUR`
- `RUB`
- `GBP`
- `UAH`
- `TRY`
- `CNY`
- `JPY`
- `CAD`
- `CHF`

## API

## `GET /health`

Liveness-check сервиса.

Успешный ответ:

```json
{
  "status": "ok"
}
```

## `GET /api/v1/languages`

Возвращает фиксированный список поддерживаемых языков.

Пример ответа:

```json
{
  "items": [
    { "code": "en", "name": "English", "isSourceLanguage": true },
    { "code": "ru", "name": "Russian", "isSourceLanguage": false }
  ]
}
```

## `GET /api/v1/currencies`

Возвращает фиксированный список поддерживаемых валют.

Пример ответа:

```json
{
  "items": [
    { "code": "USD", "name": "US Dollar", "symbol": "$", "isSourceCurrency": true },
    { "code": "EUR", "name": "Euro", "symbol": "€", "isSourceCurrency": false }
  ]
}
```

## `GET /api/v1/catalog`

Главный endpoint каталога.

Поддерживает:

- локализацию данных;
- выбор валюты;
- серверную пагинацию;
- поиск;
- фильтр по категории;
- сортировку.

### Query-параметры

- `lang` — код языка ответа, по умолчанию `en`
- `currency` — код валюты ответа, по умолчанию `USD`
- `page` — номер страницы, начиная с `1`, по умолчанию `1`
- `pageSize` — размер страницы, по умолчанию `20`, максимум `100`
- `query` — поисковая строка
- `category` — `slug` категории
- `sort` — `price_asc`, `price_desc`, `rating_desc`

Пример:

```text
GET /api/v1/catalog?lang=ru&currency=EUR&page=1&pageSize=20&query=phone&category=smartphones&sort=price_asc
```

В ответе возвращаются:

- `language`
- `currency`
- `categories`
- `items`
- `meta.totalProducts`
- `meta.totalCategories`
- `meta.currentPage`
- `meta.pageSize`
- `meta.totalPages`
- `meta.query`
- `meta.category`
- `meta.sort`
- `meta.sourceLanguage`
- `meta.sourceCurrency`
- `meta.exchangeRateProvider`

Если выбрана исходная валюта `USD`, поле `exchangeRateProvider` будет `null`. Для конвертации используется `frankfurter`.

## `GET /api/v1/catalog/:productId`

Возвращает карточку конкретного товара.

Поддерживает:

- локализацию карточки;
- локализацию комментариев отзывов;
- конвертацию цены;
- выдачу отзывов для detail-экрана.

### Query-параметры

- `lang` — код языка ответа, по умолчанию `en`
- `currency` — код валюты ответа, по умолчанию `USD`

Пример:

```text
GET /api/v1/catalog/10?lang=ru&currency=EUR
```

В ответе возвращаются:

- `language`
- `currency`
- `product`
- `reviews`
- `meta.sourceLanguage`
- `meta.sourceCurrency`
- `meta.exchangeRateProvider`

## Формат Ошибок

Ошибки отдаются в одном формате:

```json
{
  "statusCode": 404,
  "error": "PRODUCT_NOT_FOUND",
  "message": "Product 10 was not found"
}
```

Типовые ответы:

- `400` — ошибка валидации query/path параметров
- `404` — товар не найден
- `502` — ошибка внешнего провайдера
- `503` — необходимые source-данные или курсы еще недоступны
- `500` — внутренняя ошибка сервера

## Swagger

Swagger UI доступен по адресу:

```text
http://localhost:8080/docs
```

## Переменные Окружения

### Для Docker Compose

Docker Compose читает root-файл:

- [../.env.example](../.env.example)

Обычно достаточно:

```powershell
Copy-Item .env.example .env
```

Пример root `.env`:

```env
POSTGRES_DB=catalog
POSTGRES_USER=catalog
POSTGRES_PASSWORD=catalog
HOST=0.0.0.0
PORT=8080
DATABASE_URL=postgresql://catalog:catalog@postgres:5432/catalog
DUMMYJSON_BASE_URL=https://dummyjson.com
```

### Для Локального Запуска Backend Без Docker

`dotenv` читает env-файл из папки `backend`, поэтому для локального `npm run dev` нужен отдельный файл:

- [backend/.env.example](./.env.example)

Пример:

```env
HOST=0.0.0.0
PORT=8080
DATABASE_URL=postgresql://catalog:catalog@127.0.0.1:5433/catalog
DUMMYJSON_BASE_URL=https://dummyjson.com
```

Разница важная:

- внутри Docker Compose используется хост `postgres:5432`;
- при локальном запуске с хоста используется `127.0.0.1:5433`.

## Запуск

### Через Docker Compose

Из корня репозитория:

```powershell
Copy-Item .env.example .env
docker compose up -d --build postgres backend
```

После запуска:

- backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/docs`
- PostgreSQL на хосте: `localhost:5433`

### Локально Через Node.js

1. Поднять только базу:

```powershell
docker compose up -d postgres
```

2. Подготовить backend env:

```powershell
Copy-Item .\backend\.env.example .\backend\.env
```

3. Перейти в `backend` и установить зависимости:

```powershell
cd backend
npm install
```

4. Запустить dev-сервер:

```powershell
npm run dev
```

Production-like запуск:

```powershell
npm run build
npm run start
```

## Данные В PostgreSQL

В базе используются такие таблицы:

- `categories`
- `products`
- `product_translations`
- `category_translations`
- `product_reviews`
- `product_review_translations`
- `currency_rates`

Что хранится:

- source-каталог на английском;
- локализованные данные по языкам;
- отзывы и переводы отзывов;
- валютные курсы для конвертации цен.

## Тесты

Тесты разделены на две группы:

- `tests/unit`
- `tests/integration`

Скрипты:

```powershell
npm run test:unit
npm run test:integration
npm run test:all
```

### Integration-тесты

Integration-тесты работают с реальной `PostgreSQL` и создают отдельную schema на каждый прогон.

Порядок выбора строки подключения:

1. `TEST_DATABASE_URL`
2. `DATABASE_URL`
3. fallback: `postgresql://catalog:catalog@127.0.0.1:5433/catalog`

Если база недоступна, integration-тесты завершатся ошибкой с явным сообщением.

## Полезные Ссылки

- DummyJSON: https://dummyjson.com/docs/products
- Frankfurter: https://www.frankfurter.app/
