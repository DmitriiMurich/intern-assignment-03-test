# Product Catalog

Тестовое задание на Kotlin Multiplatform: экран каталога продуктов с поиском,
фильтрацией по категориям, сортировкой и пагинацией.

## Что реализовано

- загрузка каталога продуктов из сети
- поиск по названию с `debounce 300ms`
- фильтрация по категориям через chips
- сортировка по цене и рейтингу
- комбинирование `search + category + sort`
- пагинация в формате `load more`
- состояния `loading`, `empty`, `error`, `retry`
- отображение изображений товаров
- unit-тесты на основную бизнес-логику
- `Detekt` без ошибок

## Технологии

- Kotlin Multiplatform
- Jetpack Compose
- Koin
- Kotlinx Coroutines
- Kotlinx Serialization
- Ktor Client
- Coil 3
- Detekt

## Архитектура

Проект разделён на три модуля:

- `app` — Android host: `Application`, `MainActivity`, тема, Android-ресурсы,
  entry point
- `feature` — логика и UI каталога: экран, `ViewModel`, `State`, use case,
  data-layer конкретной фичи
- `model` — общие domain-модели и контракты

Цепочка зависимостей:

```text
app -> feature -> model
```

### Почему структура именно такая

- `app` не хранит бизнес-логику и отвечает только за Android-слой
- `feature` содержит конкретную фичу каталога целиком
- `model` хранит переиспользуемые сущности и контракты

Внутри `feature/catalog` presentation-сущности (`ProductCatalogScreen`,
`ProductCatalogState`, `ProductCatalogViewModel`) оставлены рядом, потому что
фича пока компактная. Дополнительное дробление на `screen/`, `state/`,
`viewmodel/` сейчас дало бы больше шума, чем пользы. При дальнейшем росте фичи
естественный следующий шаг — выделение слоя `presentation/`.

## Источник данных

В качестве API выбран `DummyJSON`.

Почему именно он:

- подходит под формат e-commerce каталога
- отдаёт товары, категории, рейтинг и изображения
- поддерживает `limit/skip`, поиск и сортировку
- не требует авторизации
- позволяет быстро собрать реалистичный demo-flow

### Почему пагинация реализована локально

В `DummyJSON` есть отдельные механики для `limit/skip`, поиска, сортировки и
категорий. Но экран каталога в этом проекте требует устойчивую комбинацию
`search + category + sort + pagination` в одном сценарии.

В текущем решении каталог загружается целиком, а поиск, фильтрация, сортировка
и `load more` выполняются локально. Это даёт несколько плюсов:

- предсказуемое поведение при комбинировании фильтров
- простой и прозрачный `debounce 300ms`
- стабильное unit-тестирование бизнес-логики
- отсутствие расхождений между разными сетевыми endpoint'ами

Для датасета `DummyJSON` на `194` товаров это инженерно оправданный компромисс.
Для production-версии следующим шагом был бы собственный backend или BFF-слой
с единым контрактом под комбинированную фильтрацию, сортировку и серверную
пагинацию.

## Строки и ресурсы

Статические UI-строки теперь вынесены в multiplatform resources внутри shared-модуля:

- `feature/src/commonMain/composeResources/values/strings.xml`

Экран и его компоненты читают строки напрямую из `commonMain` через:

- `org.jetbrains.compose.resources.stringResource(...)`
- сгенерированный класс `Res`

Это убирает Android-only bridge и делает строковые ресурсы доступными на уровне shared UI. В `app`
остался только Android-ресурс `app_name`, который нужен хост-модулю.

### Почему каталог остаётся на английском

Локализация интерфейса и локализация самих данных каталога — это разные задачи. Кнопки, заголовки и
служебные сообщения теперь можно локализовать на уровне shared resources. Но названия, описания и
категории товаров приходят из `DummyJSON` на английском языке.

Для автоматического перевода контента в реальном приложении уже нужен не только resource-слой, а
отдельный backend/proxy к translation API. Это нужно для:

- защиты ключей внешнего переводчика
- кэширования переводов
- контроля стоимости и лимитов перевода
- единообразного перевода на всех клиентах

Поэтому в текущем решении локализован статический UI-слой, а данные каталога отображаются в
оригинале API.
## Тесты

Реализованы unit-тесты для:

- `SearchProductsUseCase`
- `ProductCatalogViewModel`
- `DummyJsonProductMapper`
- `DummyJsonProductRepository`

Что покрыто:

- начальная загрузка каталога
- поиск по query
- фильтр по категории
- сортировка по цене
- сортировка по рейтингу
- комбинация `search + category`
- `debounce 300ms`
- `load more`
- `reset filters`
- empty result
- error state и `retry`
- mapper и repository data-layer

Что мокалось:

- в тестах `ViewModel` использован `FakeProductRepository`
- в тестах `repository` использован `Ktor MockEngine`
- `SearchProductsUseCase` и mapper тестируются без моков, как чистая логика

## Качество кода

`Detekt` подключён и проходит без ошибок.

Команда запуска:

```powershell
$env:JAVA_HOME="<path-to-jdk>"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:detekt :feature:detekt :model:detekt
```

## Запуск проекта

### Через Android Studio

1. Открыть корень проекта `internassignment03`
2. Дождаться `Gradle Sync`
3. Проверить `Gradle JDK`: должен быть выбран `JDK 17+` или `JDK 21+`
4. Запустить конфигурацию `app` на эмуляторе или устройстве

### Через терминал

Сборка debug APK:

```powershell
$env:JAVA_HOME="<path-to-jdk>"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

Запуск тестов:

```powershell
$env:JAVA_HOME="<path-to-jdk>"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :feature:allTests
```

## Структура проекта

```text
intern-assignment-03/
├── app/
├── feature/
├── model/
├── docs/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Скриншоты

### Главный экран
![Главный экран](docs/images/catalog-home.png)

### Поиск: ввод запроса
![Поиск: ввод запроса](docs/images/catalog-search-1.png)

### Поиск: результат фильтрации
![Поиск: результат фильтрации](docs/images/catalog-search-2.png)

### Фильтрация по категории: выбор категории
![Фильтрация по категории: выбор категории](docs/images/catalog-category-1.png)

### Фильтрация по категории: результат
![Фильтрация по категории: результат](docs/images/catalog-category-2.png)

### Пустой результат
![Пустой результат](docs/images/catalog-empty.png)

### Ошибка загрузки
![Ошибка загрузки](docs/images/catalog-error.png)
## Видео

[YouTube demo](https://youtu.be/VGIUjqqG7-8)

## APK

[Download APK](https://github.com/Bellou1337/intern-assignment-03/releases/download/v1.0.0/app-release.apk)

## Использованные ИИ-инструменты

- Codex

## Что можно улучшить дальше

- добавить backend/BFF-слой для серверной пагинации и единой комбинированной фильтрации
- добавить мультиязычность UI и перевод контента каталога через backend/proxy
## Backend

В репозитории появился отдельный backend-сервис на `Node.js + TypeScript`, который живёт рядом с Android-приложением в папке `backend/`.

Зачем он нужен:

- мобильный клиент больше не должен ходить напрямую в `DummyJSON`;
- переводы нельзя безопасно делать прямо на клиенте, потому что ключи переводчика нельзя хранить в приложении;
- поиск, фильтрация, сортировка и пагинация теперь могут выполняться на сервере;
- переводы кешируются в `PostgreSQL`, поэтому backend не переводит один и тот же товар заново на каждый запрос.

### Как работает backend

Схема работы такая:

```text
mobile app -> backend -> DummyJSON
                     -> Yandex Translate
                     -> PostgreSQL
```

Backend:

- загружает source-каталог из `DummyJSON`;
- хранит исходные данные на английском в `PostgreSQL`;
- при запросе не-английского языка переводит `title`, `description` и названия категорий через `Yandex Translate`;
- сохраняет переводы в БД;
- отдает клиенту уже локализованный список товаров;
- сам применяет `query`, `category`, `sort`, `page`, `pageSize`.

Если source-каталог еще не загружен, backend подтянет его автоматически при первом запросе к каталогу.

### Эндпоинты backend

`GET /health`

- liveness-check сервиса

`GET /api/v1/languages`

- возвращает список поддерживаемых языков API

`GET /api/v1/catalog`

- главный эндпоинт каталога
- поддерживает локализацию, поиск, фильтрацию, сортировку и серверную пагинацию

Параметры `GET /api/v1/catalog`:

- `lang` — язык ответа, по умолчанию `en`
- `page` — номер страницы, начиная с `1`
- `pageSize` — размер страницы, по умолчанию `20`, максимум `100`
- `query` — поисковая строка
- `category` — `slug` категории
- `sort` — одно из значений: `price_asc`, `price_desc`, `rating_desc`

Пример:

```text
GET /api/v1/catalog?lang=ru&page=1&pageSize=20&query=phone&category=smartphones&sort=price_asc
```

`POST /api/v1/catalog/sync`

- принудительно обновляет source-каталог из `DummyJSON`
- сбрасывает кеш переводов

### Поддерживаемые языки

Список языков сейчас фиксирован в коде backend-а:

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

### Переменные окружения backend

Пример лежит в `backend/.env.example`.

Используются такие переменные:

- `HOST` — адрес, на котором слушает backend
- `PORT` — порт backend
- `DATABASE_URL` — строка подключения к `PostgreSQL`
- `DUMMYJSON_BASE_URL` — базовый URL `DummyJSON`
- `YANDEX_TRANSLATE_API_URL` — URL REST-метода `Yandex Translate`
- `YANDEX_TRANSLATE_API_KEY` — API key для `Yandex Translate`
- `YANDEX_FOLDER_ID` — folder ID в `Yandex Cloud`

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

### Откуда взять Yandex Cloud параметры

Для `YANDEX_TRANSLATE_API_KEY` и `YANDEX_FOLDER_ID` нужны ресурсы в `Yandex Cloud`.

Что нужно сделать:

1. Создать или выбрать cloud и folder.
2. Включить `Translate API`.
3. Создать сервисный аккаунт.
4. Выдать ему права на работу с `Translate API`.
5. Создать `API key`.
6. Скопировать `folder ID`.

Официальные ссылки:

- Translate overview: https://yandex.cloud/en/docs/translate/
- REST reference: https://yandex.cloud/en/docs/translate/api-ref/Translation/translate
- Access via API key: https://yandex.cloud/en/docs/translate/operations/sa-api-key

### Как запустить backend

Через Docker:

```bash
docker compose up --build
```

После запуска backend будет доступен на:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/docs
```

Локальный запуск без Docker для backend:

```bash
docker compose up -d postgres
cd backend
npm install
npm run dev
```

### Где лежит полная документация

Подробная документация backend-а вынесена в отдельный файл:

- [backend/README.md](backend/README.md)
