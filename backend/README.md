# Backend

## Что это

`backend` — это BFF-сервис для мобильного каталога.

Он решает несколько задач:

- забирает source-данные из `DummyJSON`;
- хранит английскую версию каталога в `PostgreSQL`;
- заранее записывает mock-переводы для всех поддерживаемых языков в базе;
- отдает мобильному приложению уже локализованный и серверно-пагинируемый каталог.

Цепочка запросов выглядит так:

```text
mobile app -> backend -> DummyJSON
                     -> PostgreSQL
```

## Как это работает

1. Клиент вызывает `GET /api/v1/catalog`.
2. При старте backend выполняет фоновую синхронизацию source-каталога из `DummyJSON`, а затем повторяет её каждый час.
3. Если запрошен язык `en`, backend отдает данные из source-таблиц.
4. Для всех поддерживаемых не-английских языков backend заранее записывает mock-переводы в `PostgreSQL` после синхронизации source-каталога.
5. Поиск, фильтрация, сортировка и пагинация выполняются на сервере.

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
- `meta.sourceCurrency`

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

Пример:

```env
HOST=0.0.0.0
PORT=8080
DATABASE_URL=postgresql://catalog:catalog@localhost:5432/catalog
DUMMYJSON_BASE_URL=https://dummyjson.com
```

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

3. Подними `postgres`:

```bash
docker compose up -d postgres
```

4. Установи зависимости:

```bash
npm install
```

5. Запусти dev-сервер:

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
