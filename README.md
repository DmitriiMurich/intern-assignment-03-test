# Product Catalog

Kotlin Multiplatform Android-приложение каталога товаров и отдельный BFF/backend на `Node.js + TypeScript`.

Проект больше не работает напрямую с `DummyJSON`: мобильный клиент ходит в собственный backend, который:

- синхронизирует source-каталог из `DummyJSON`;
- хранит данные в `PostgreSQL`;
- отдает локализованный каталог и карточку товара;
- конвертирует цены в выбранную валюту;
- возвращает отзывы для страницы товара.

## Что В Репозитории

- `app` — Android host-модуль: `Application`, `MainActivity`, Android-specific wiring.
- `feature` — shared UI и логика каталога: экран списка, карточка товара, `ViewModel`, repository, mappers.
- `model` — общие domain-модели и контракты.
- `backend` — BFF-сервис на `Fastify + PostgreSQL`.
- `docs` — скриншоты и вспомогательные материалы.

## Что Реализовано

### Мобильное приложение

- каталог товаров с загрузкой с backend;
- поиск с `debounce 300ms`;
- фильтрация по категориям;
- сортировка по цене и рейтингу;
- серверная пагинация с UX в формате `load more`;
- переключение языка интерфейса и языка данных каталога;
- переключение валюты с пересчетом цены на backend;
- карточка товара с кнопкой назад;
- отдельный экран товара с ценой, рейтингом, описанием и отзывами;
- состояния `loading`, `empty`, `error`, `retry`;
- загрузка изображений через `Coil 3`.

### Backend

- полная синхронизация source-каталога из `DummyJSON` при каждом старте;
- хранение source-данных, переводов, отзывов и валютных курсов в `PostgreSQL`;
- демо-локализация названий, описаний, категорий и отзывов через curated static translations;
- серверные поиск, фильтрация, сортировка и пагинация;
- конвертация цен по курсам `Frankfurter`;
- `Swagger UI` и `health` endpoint;
- unit- и integration-тесты на `Jest`.

## Архитектура

### Android-клиент

Зависимости между мобильными модулями:

```text
app -> feature -> model
```

Почему структура такая:

- `app` отвечает только за Android host-слой;
- `feature` содержит всю конкретную фичу каталога;
- `model` хранит переиспользуемые domain-сущности и repository-контракты.

### Backend

Поток данных выглядит так:

```text
Android app -> backend -> DummyJSON
                       -> Frankfurter
                       -> PostgreSQL
```

Роли backend:

- скрывает внешние API от мобильного клиента;
- отдает один стабильный контракт для приложения;
- хранит локализованные данные и отзывы;
- конвертирует цены на сервере;
- берет на себя серверную пагинацию и фильтрацию.

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

## Быстрый Старт

Рекомендуемый сценарий для локального запуска:

1. Скопировать root env-файл:

```powershell
Copy-Item .env.example .env
```

2. Поднять `PostgreSQL` и backend:

```powershell
docker compose up -d --build postgres backend
```

3. Дождаться, пока backend станет доступен:

```text
http://localhost:8080/health
```

4. Запустить Android-приложение.

`backend` на старте выполняет полную синхронизацию каталога из `DummyJSON`, поэтому первый запуск может занять немного больше времени, чем обычный hot restart.

## Запуск Проекта

### Через Android Studio

1. Открыть корень проекта `internassignment03`.
2. Дождаться `Gradle Sync`.
3. Убедиться, что выбран JDK из Android Studio или любой совместимый `JDK 17+`.
4. Поднять backend.
5. Запустить конфигурацию `app` на эмуляторе или устройстве.

### Через Терминал

Сборка debug APK:

```powershell
$env:JAVA_HOME='D:\android-studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

Сборка release APK:

```powershell
$env:JAVA_HOME='D:\android-studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleRelease
```

### Базовый URL Backend Для Android

По умолчанию Android-приложение собирается с:

```text
http://10.0.2.2:8080/
```

Это подходит для Android Emulator, когда backend запущен на хост-машине.

Если приложение запускается на физическом устройстве, базовый URL можно переопределить через Gradle property:

```powershell
$env:JAVA_HOME='D:\android-studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug -Pbackend.baseUrl=http://<LAN-IP>:8080/
```

## Документация

Дополнительные документы по проекту:

- [backend/README.md](backend/README.md)
- [docs/product-details-feature-spec.md](docs/product-details-feature-spec.md) — спецификация фичи detail-экрана товара и backend-контракта для него
- [docs/testing-spec.md](docs/testing-spec.md) — подробная QA-спецификация

## Backend

Коротко:

- Docker Compose использует root `.env`;
- backend слушает `http://localhost:8080`;
- Swagger доступен на `http://localhost:8080/docs`;
- `PostgreSQL` публикуется на хосте как `localhost:5433`.

## Тесты И Качество Кода

### Mobile

Прогон shared/mobile-тестов:

```powershell
$env:GRADLE_USER_HOME='D:\internassignment03\.gradle-user-mobile-tests'
$env:JAVA_HOME='D:\android-studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :feature:allTests --no-daemon
```

Прогон `detekt`:

```powershell
$env:JAVA_HOME='D:\android-studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:detekt :feature:detekt :model:detekt
```

### Backend

Из папки `backend`:

```powershell
npm run test:unit
npm run test:integration
npm run test:all
```

Integration-тесты backend ожидают доступную PostgreSQL-базу. По умолчанию используется:

```text
postgresql://catalog:catalog@127.0.0.1:5433/catalog
```

При необходимости можно переопределить `TEST_DATABASE_URL`.

## Основные Технологии

### Mobile

- Kotlin Multiplatform
- Jetpack Compose
- Koin
- Kotlinx Coroutines
- Kotlinx Serialization
- Ktor Client
- Coil 3
- Detekt

### Backend

- Node.js 20+
- TypeScript
- Fastify
- PostgreSQL
- Jest
- Docker Compose

### Внешние сервисы

- DummyJSON
- Frankfurter

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

## APK И Релизы

- [GitHub Releases](https://github.com/Bellou1337/intern-assignment-03/releases/tag/v.2.0.0)

## Использованные AI-Инструменты

- Codex
