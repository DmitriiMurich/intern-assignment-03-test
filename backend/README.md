# Backend

## Что это

`backend` — это BFF-сервис для мобильного каталога.

Он решает несколько задач:

- забирает source-данные из `DummyJSON`;
- хранит английскую версию каталога в `PostgreSQL`;
- переводит названия, описания и названия категорий через `Yandex Translate`;
- кеширует переводы по языкам в базе;
- отдает мобильному приложению уже локализованный и серверно-пагинируемый каталог.

Цепочка запросов выглядит так:

```text
mobile app -> backend -> DummyJSON
                     -> Yandex Translate
                     -> PostgreSQL
```

## Как это работает

1. Клиент вызывает `GET /api/v1/catalog`.
2. Если source-каталог еще не загружен, backend автоматически забирает его из `DummyJSON`.
3. Если запрошен язык `en`, backend отдает данные из source-таблиц.
4. Если запрошен другой язык, backend проверяет кеш переводов в `PostgreSQL`.
5. Для отсутствующих переводов backend вызывает `Yandex Translate`, сохраняет результат в БД и только потом формирует ответ.
6. Поиск, фильтрация, сортировка и пагинация выполняются на сервере.

## Эндпоинты

### `GET /health`

Проверка доступности сервиса.

Успешный ответ:

```json
{
  "status": "ok"
}
```

### `GET /api/v1/languages`

Возвращает фиксированный список поддерживаемых языков API.

Сейчас поддерживаются:

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

### `GET /api/v1/catalog`

Главный эндпоинт каталога.

Поддерживает:

- локализацию;
- серверную пагинацию;
- поиск;
- фильтр по категории;
- сортировку.

Query-параметры:

- `lang` — язык ответа. По умолчанию `en`.
- `page` — номер страницы, начиная с `1`. По умолчанию `1`.
- `pageSize` — размер страницы. По умолчанию `20`, максимум `100`.
- `query` — поисковая строка по локализованным `title` и `description`.
- `category` — `slug` категории.
- `sort` — сортировка: `price_asc`, `price_desc`, `rating_desc`.

Пример:

```text
GET /api/v1/catalog?lang=ru&page=1&pageSize=20&query=phone&category=smartphones&sort=price_asc
```

Успешный ответ содержит:

- `language`
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
- `meta.translationProvider`

### `POST /api/v1/catalog/sync`

Принудительно обновляет source-каталог из `DummyJSON` и сбрасывает кеш переводов.

Это полезно, если нужно заново подтянуть каталог и пересоздать переводы на следующих запросах.

## Swagger

Swagger UI доступен по адресу:

```text
http://localhost:8080/docs
```

Чтобы его открыть:

1. Подними `PostgreSQL`.
2. Запусти backend.
3. Открой `/docs` в браузере.

## Переменные окружения

Пример лежит в [backend/.env.example](C:/projects/intern-assignment-03/backend/.env.example).

Используются такие переменные:

- `HOST` — адрес, на котором слушает backend. Для локального запуска подходит `0.0.0.0`.
- `PORT` — порт backend. По умолчанию используется `8080`.
- `DATABASE_URL` — строка подключения к `PostgreSQL`.
- `DUMMYJSON_BASE_URL` — базовый URL внешнего каталога. Обычно оставляется `https://dummyjson.com`.
- `YANDEX_TRANSLATE_API_URL` — URL REST-метода перевода Yandex Translate. Обычно менять не нужно.
- `YANDEX_TRANSLATE_API_KEY` — API key для вызова Yandex Translate.
- `YANDEX_FOLDER_ID` — идентификатор folder в Yandex Cloud, от имени которого вызывается Translate API.

Пример:

```env
HOST=0.0.0.0
PORT=8080
DATABASE_URL=postgresql://catalog:catalog@localhost:5432/catalog
DUMMYJSON_BASE_URL=https://dummyjson.com
YANDEX_TRANSLATE_API_URL=https://translate.api.cloud.yandex.net/translate/v2/translate
YANDEX_TRANSLATE_API_KEY=<your-api-key>
YANDEX_FOLDER_ID=<your-folder-id>
```

## Откуда взять `YANDEX_TRANSLATE_API_KEY` и `YANDEX_FOLDER_ID`

### `YANDEX_TRANSLATE_API_KEY`

Нужен API key для сервисного аккаунта или другого поддерживаемого способа доступа в Yandex Cloud.

Официальные ссылки:

- Настройка доступа по API key: https://yandex.cloud/en/docs/translate/operations/sa-api-key
- REST API Translate: https://yandex.cloud/en/docs/translate/api-ref/Translation/translate

### `YANDEX_FOLDER_ID`

Это идентификатор folder в Yandex Cloud, внутри которого включен сервис перевода.

Официальные ссылки:

- Как работать с Translate: https://yandex.cloud/en/docs/translate/operations/
- REST API Translate: https://yandex.cloud/en/docs/translate/api-ref/Translation/translate

На практике это выглядит так:

1. Войти в Yandex Cloud.
2. Выбрать cloud и folder.
3. Включить `Translate API`.
4. Создать сервисный аккаунт и выдать ему нужные права.
5. Выпустить API key.
6. Скопировать `folder ID` из консоли Yandex Cloud.

## Локальный запуск

### Через Docker Compose

Из корня репозитория:

```bash
docker compose up --build
```

После этого будут подняты:

- `postgres`
- `backend`

Backend будет доступен на:

```text
http://localhost:8080
```

### Через локальный Node.js

1. Подними только базу:

```bash
docker compose up -d postgres
```

2. Перейди в папку `backend`:

```bash
cd backend
```

3. Установи зависимости:

```bash
npm install
```

4. Запусти dev-сервер:

```bash
npm run dev
```

Либо production-like запуск:

```bash
npm run build
npm run start
```

## База данных

`PostgreSQL` используется для двух типов данных:

- source-каталог на английском;
- кеш переводов по языкам.

Основные таблицы:

- `categories`
- `products`
- `category_translations`
- `product_translations`

## Полезные ссылки

- DummyJSON docs: https://dummyjson.com/docs/products
- Yandex Translate docs overview: https://yandex.cloud/en/docs/translate/
- Yandex Translate REST reference: https://yandex.cloud/en/docs/translate/api-ref
