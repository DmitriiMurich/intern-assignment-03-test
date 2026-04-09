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

See [backend/README.md](backend/README.md) for the new Node.js + TypeScript BFF service with PostgreSQL, DeepL-based translation caching, and Docker startup instructions.
