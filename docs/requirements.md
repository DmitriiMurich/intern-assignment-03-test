# Требования к тестовому заданию Product Catalog

## Зачем этот файл

Этот документ фиксирует требования задания в одном месте, чтобы не сверяться с полным
ТЗ перед каждым шагом реализации.

## Исходное состояние проекта

- Сейчас проект представляет собой Android-приложение с одним модулем `app`
- В текущем состоянии отсутствуют:
- KMP-структура
- shared-модули `model` и `feature`
- DI через `Koin`
- сетевой слой на `Ktor Client`
- сериализация через `kotlinx.serialization`
- unit-тесты на основную бизнес-логику

## Обязательные требования

- Kotlin Multiplatform и MVVM-архитектура
- Рабочий экран с UI и бизнес-логикой
- Unit-тесты на основные сценарии с зелёным прогоном
- Видео с примером работы приложения
- Ссылка на APK в `GitHub Releases`
- Возможность объяснить код, сгенерированный с помощью ИИ

## Обязательный стек

- Kotlin Multiplatform
- Jetpack Compose или Compose Multiplatform
- Koin
- Kotlinx Coroutines
- Kotlinx Serialization
- Ktor Client

## Функциональные требования

### UI

- Поисковая строка
- Chips для выбора категории
- Список продуктов на `LazyColumn`
- Карточка продукта: название, цена, картинка, рейтинг
- Сортировка по цене по возрастанию
- Сортировка по цене по убыванию
- Сортировка по рейтингу
- Состояние пустого списка

### Бизнес-логика

- Поиск по названию
- Debounce для поиска: `300ms`
- Фильтрация по категории
- Сортировка по выбранному критерию
- Комбинация фильтров: поиск + категория
- Пагинация через `load more`

## Целевая структура проекта по ТЗ

```text
intern-assignment-03/
├── README.md
├── feature/
│   ├── src/commonMain/
│   │   ├── ProductCatalogViewModel.kt
│   │   ├── ProductCatalogState.kt
│   │   ├── ProductCatalogScreen.kt
│   │   ├── components/ProductCard.kt
│   │   ├── components/SearchBar.kt
│   │   └── domain/SearchProductsUseCase.kt
│   └── src/commonTest/
│       ├── ProductCatalogViewModelTest.kt
│       └── SearchProductsUseCaseTest.kt
├── model/
│   ├── src/commonMain/
│   │   ├── domain/Product.kt
│   │   ├── domain/ProductCategory.kt
│   │   ├── domain/SortOption.kt
│   │   └── repository/ProductRepository.kt
│   └── src/commonTest/
├── app/
├── build.gradle.kts
└── gradle.properties
```

## Минимальные тестовые сценарии

- `should display products on init`
- `should filter products by search query`
- `should filter products by category`
- `should sort products by price ascending`
- `should combine search and category filters`

## Критерии оценки из ТЗ

- Debounce для поиска
- Комбинирование `Flow` для фильтров
- `LazyColumn` с пагинацией
- Unit-тесты с моками репозитория
- Понимание `StateFlow` и `SharedFlow`

## README должен содержать

- Описание задания
- Скриншоты экранов
- Ссылку на видео
- Ссылку на APK в `GitHub Releases`
- Инструкцию по запуску
- Перечень использованных ИИ-инструментов

## Требования к коду

- `Detekt` без предупреждений
- `internal` по умолчанию
- trailing commas
- максимальная длина строки: `120`

## Артефакты для сдачи

- Исходный код
- Заполненный `README.md`
- Скриншоты
- Видео
- APK в `GitHub Releases`

## Definition of Done

- Проект приведён к KMP-структуре, достаточной для выполнения задания
- Реализован экран каталога с UI и бизнес-логикой
- Поиск, фильтрация, сортировка и пагинация работают совместно
- Основные сценарии покрыты unit-тестами
- Тесты проходят
- README заполнен полностью
- Есть видео и APK-артефакт
- Все спорные решения задокументированы и объяснимы на собеседовании

## Что в ТЗ не определено явно

- Конкретный источник данных не задан
- Механика пагинации не описана детально
- Ошибки сети и retry не перечислены как обязательные, но их стоит продумать
- Обязательность iOS-части не зафиксирована, хотя KMP обязателен

## Отдельные замечания по текущему репозиторию

- На текущий момент проект ещё не соответствует KMP-структуре из ТЗ
- В папке проекта сейчас нет инициализированного `.git`, а для `GitHub Releases` это понадобится
