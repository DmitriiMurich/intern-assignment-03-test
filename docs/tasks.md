# Бэклог задач

## Основные задачи

- [x] T-001 Привести проект к KMP-структуре с модулями `app`, `feature`, `model`.
- [x] T-002 Подключить базовые зависимости: Compose, Koin, Coroutines, Serialization, Ktor, test.
- [x] T-003 Описать domain-модели `Product`, `ProductCategory`, `SortOption`.
- [x] T-004 Описать контракт `ProductRepository`.
- [x] T-005 Выбрать и зафиксировать источник данных.
- [x] T-006 Реализовать DTO, mapper и сетевой репозиторий.
- [x] T-007 Реализовать `SearchProductsUseCase` для поиска, фильтрации и сортировки.
- [x] T-008 Реализовать комбинирование `Flow` для query, category и sort.
- [x] T-009 Реализовать пагинацию `load more`.
- [x] T-010 Реализовать `ProductCatalogState`.
- [x] T-011 Реализовать `ProductCatalogViewModel`.
- [x] T-012 Настроить DI через `Koin`.
- [x] T-013 Собрать рабочий `ProductCatalogScreen`.
- [x] T-014 Реализовать `SearchBar`, `ProductCard`, chips для категорий и UI для сортировки.
- [x] T-015 Реализовать состояния `loading`, `empty`, `error` и `load more`.
- [x] T-016 Написать unit-тесты для `SearchProductsUseCase`.
- [x] T-017 Написать unit-тесты для `ProductCatalogViewModel`.
- [x] T-018 Проверить `debounce 300ms` тестами корутин.
- [x] T-019 Настроить `Detekt` и довести код до зелёного прогона.
- [ ] T-020 Подготовить финальный `README.md`, скриншоты, видео и APK release.

## Статус по сдаче

Сейчас полностью закрыты:
- рабочий экран каталога
- бизнес-логика
- unit-тесты
- `Detekt`

Осталось закрыть до финальной сдачи:
- добавить скриншоты в `README`
- записать видео работы приложения
- выложить APK в `GitHub Releases`
- подготовить финальные ответы на собеседование

## Принятые решения

- Источник данных: `DummyJSON`
- Архитектура зависимостей: `app -> feature -> model`
- Основное состояние экрана хранится в `StateFlow`
- Поиск, фильтрация, сортировка и пагинация реализованы вне UI
- Для shared UI строки передаются из Android `strings.xml` через `CatalogStrings`
- Пагинация реализована как mobile-friendly `load more`, а не веб-страницы по номерам

## Что протестировано

- начальная загрузка продуктов
- поиск по query
- фильтр по категории
- сортировка по цене и рейтингу
- комбинация `search + category`
- `debounce 300ms`
- `load more`
- пустой результат без ошибки
- retry после ошибки
- mapper и repository на data-layer
