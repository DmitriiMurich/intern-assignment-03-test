# Test Suite — Product Catalog

Автоматизированные тесты для backend-сервиса и Android-приложения каталога продуктов.  
Стек: **pytest** · **httpx** · **respx** · **allure-pytest** · **Compose UI Test** · **Kotlin Test**

---

## Структура

```
tests/
├── conftest.py                  # Session-scope фикстуры: http_client, pipeline helpers
├── pytest.ini                   # Конфигурация pytest, маркеры
├── requirements-test.txt        # Python-зависимости
├── allure.properties            # Конфигурация Allure
│
├── helpers/
│   ├── factories.py             # Factory Boy — генераторы тестовых данных
│   └── assertions.py            # Переиспользуемые assertion-хелперы
│
├── unit/                        # Компонентные тесты (без живого сервера)
│   ├── conftest.py              # respx mock_api fixture
│   ├── test_catalog_contract.py # EP + BVA для /catalog и /catalog/:id
│   └── test_currency_logic.py   # Все 10 валют, форматирование цен
│
├── api/                         # API-тесты (требуют запущенный бэкенд)
│   ├── conftest.py              # api_client fixture, auto-skip если сервер недоступен
│   ├── test_health.py           # GET /health
│   ├── test_languages.py        # GET /api/v1/languages
│   ├── test_currencies.py       # GET /api/v1/currencies
│   ├── test_catalog.py          # GET /api/v1/catalog — поиск, фильтры, сортировка, пагинация
│   └── test_product_details.py  # GET /api/v1/catalog/:id
│
├── e2e/                         # Backend E2E/smoke-тесты — критические пользовательские сценарии
│   ├── conftest.py              # smoke_client fixture
│   └── test_smoke.py            # 13 user journeys
│
├── screenshots/                 # Скриншоты с эмулятора (ключевые закоммичены, остальные в .gitignore)
│   ├── 07_app_fast_avd.png      # Приложение с 194 продуктами (fast_avd, API 29)
│   ├── 12_tests_running_live.png # Приложение во время выполнения UI-тестов
│   └── 24_tests_executing_live.png # Эмулятор во время 3-го прогона тестов (60 s timeout)
│
└── pipeline/                    # Файлы коммуникации между агентами (в .gitignore)
    ├── project-analysis.json    # ← write-tests агент
    ├── test-results.json        # ← run-tests агент
    ├── coverage-report.json     # ← run-tests агент
    └── run-status.json          # ← run-project-and-tests агент
```

---

## Слои тестирования

| Слой | Инструмент | Зависимости | Кол-во тестов |
|------|-----------|-------------|----------------|
| **Backend Unit** | pytest + respx mock | Нет сервера | ~30 |
| **Backend API** | pytest + httpx | Живой бэкенд `:8080` | ~40 |
| **Backend E2E** | pytest + httpx | Живой бэкенд `:8080` | 13 |
| **Android KMP Unit** | Kotlin Test / JVM | Java 8+, без устройства | ~20 |
| **Android UI** | Compose UI Test | Эмулятор + бэкенд | 23 |

---

## Быстрый старт

### 1. Backend-тесты (Python)

```bash
pip install -r tests/requirements-test.txt

# Unit (без сервера)
python -m pytest tests/unit/ -v -m unit

# API + E2E (нужен запущенный бэкенд)
python -m pytest tests/ -v

# По маркеру
python -m pytest tests/ -m "positive"    # только позитивные кейсы
python -m pytest tests/ -m "negative"   # только негативные кейсы
python -m pytest tests/ -m "boundary"   # только BVA-тесты
python -m pytest tests/ -m "smoke"      # только smoke
```

### 2. Android KMP Unit-тесты (без устройства)

```bash
# Запускает тесты на JVM — эмулятор не нужен
./gradlew :feature:jvmTest

# С Allure-отчётом через агент:
# /android-tests
```

### 3. Android UI-тесты (Compose UI Test)

Требуют: запущенный эмулятор Android (`fast_avd`, API 29) и работающий бэкенд на `:8080`.

```bash
# Запустить все 23 UI-теста на подключённом эмуляторе
./gradlew :app:connectedDebugAndroidTest

# Проверить подключение эмулятора
adb devices

# Проверить бэкенд из эмулятора (10.0.2.2 = хост-машина)
adb shell "echo -e 'GET /health HTTP/1.0\r\n' | nc 10.0.2.2 8080"
```

**Состав UI-тестов (23 теста, ISTQB):**

| Класс | Техника | Тестов | Покрытие |
|-------|---------|--------|----------|
| `CatalogScreenPositiveTest` | EP, BVA | 12 | Загрузка каталога, поиск, навигация, фильтры |
| `CatalogScreenNegativeTest` | EP, BVA, ST | 6 | Нет результатов, спецсимволы, длинный запрос |
| `ProductDetailsUiTest` | EP, ST, Decision Table | 5 | Открытие деталей, навигация, счётчик продуктов |

Эмулятор настроен на максимальную производительность:
- **Pixel 3a** (API 29, Android 10), **x86_64**
- GPU: **ANGLE** (DirectX 11 backend), 8 CPU ядер, 3 GB RAM
- AVD: `fast_avd` — старт ~20 с, загрузка 194 продуктов ~35-40 с

---

## Просмотр Allure-отчёта

### Вариант 1 — Через Claude Code команду (рекомендуется)

```
/allure-report         # Backend-тесты (pytest)
/android-tests         # KMP Unit + backend, совмещённый отчёт
```

### Вариант 2 — Вручную

```bash
# 1. Установить Allure CLI (один раз)
#    Windows: скачать allure-2.30.0.zip с github.com/allure-framework/allure2/releases

# 2. Запустить тесты с генерацией результатов
python -m pytest tests/ --alluredir=tests/allure-results -q

# 3. Сгенерировать HTML-отчёт
allure generate tests/allure-results --output tests/allure-report --clean

# 4. Открыть через HTTP-сервер (file:// не работает из-за CORS)
cd tests/allure-report && python -m http.server 4040
# Открыть: http://127.0.0.1:4040
```

### Что есть в отчёте

- **Overview** — сводка: passed / failed / skipped, время выполнения
- **Suites** — тесты сгруппированы по классу
- **Features** — группировка по `@allure.feature` / `@allure.story`
- **Behaviors** — ISTQB-сценарии: EP, BVA, State Transition, Decision Table
- **Timeline** — параллельность выполнения
- **Attachments** — скриншоты эмулятора (для UI-тестов)

---

## Покрытие автотестами

### Backend API

| Эндпоинт | Методы | Покрыто тестами |
|----------|--------|-----------------|
| `GET /health` | status 200, поле status | ✅ |
| `GET /api/v1/languages` | список, isSourceLanguage, 10 языков | ✅ |
| `GET /api/v1/currencies` | список, isSourceCurrency, 10 валют | ✅ |
| `GET /api/v1/catalog` | lang, currency, page, pageSize, query, sort, category | ✅ |
| `GET /api/v1/catalog/:id` | детали продукта, 404, lang, currency | ✅ |

**EP-классы**: 10 языков × 10 валют = 100 комбинаций (выборочно проверены)  
**BVA**: `page=0` (невалид), `page=1` (min), `page=totalPages` (max), `page=totalPages+1` (beyond)

### Android UI

| Сценарий | Тест | ISTQB |
|---------|------|-------|
| Загрузка каталога с продуктами | `appLaunch_showsCatalogWithProducts` | EP |
| Поиск по валидному запросу | `searchByValidQuery_updatesProductList` | EP |
| Клик на карточку → детали | `clickProductCard_opensDetailsScreen` | EP |
| Кнопка «Назад» → каталог | `backButton_returnsToProductCatalog` | State Transition |
| Поиск без результатов → empty state | `searchWithNoMatch_showsEmptyStateCard` | EP |
| Очень длинный запрос → нет краша | `searchWithVeryLongString_noCrash` | BVA |
| Спецсимволы → нет краша | `searchWithSpecialCharsOnly_noCrash` | EP |
| Два разных продукта → детали | `openTwoDifferentProducts_bothShowDetails` | Decision Table |

---

## Скриншоты (доказательства)

Ключевые скриншоты находятся в [`tests/screenshots/`](screenshots/):

| Скриншот | Что показывает |
|---------|----------------|
| [`A1_loading_state.png`](screenshots/A1_loading_state.png) | Экран загрузки — «Loading catalog / Fetching products from the API» |
| [`A2_catalog_english_all.png`](screenshots/A2_catalog_english_all.png) | Каталог на английском, фильтры языка/валюты/категорий |
| [`A3_category_laptops.png`](screenshots/A3_category_laptops.png) | Категория «Laptops» выбрана — «Showing 5 of 5 items» |
| [`A5_laptops_results.png`](screenshots/A5_laptops_results.png) | Результаты по категории «Laptops», сортировка по цене |
| [`A6_product_details.png`](screenshots/A6_product_details.png) | Экран деталей продукта — Lenovo Yoga 920, кнопка «Back to catalog» |
| [`A7_product_reviews.png`](screenshots/A7_product_reviews.png) | Детали продукта с разделом отзывов (несколько reviews) |
| [`B1_search_laptop.png`](screenshots/B1_search_laptop.png) | Поиск «laptop» — 5 результатов |
| [`B3_empty_state_card.png`](screenshots/B3_empty_state_card.png) | Пустой результат поиска — «Nothing found», кнопка «Reset filters» |
| [`C1_language_dropdown_open.png`](screenshots/C1_language_dropdown_open.png) | Открытый дроп с выбором языка (English, Русский, Deutsch, Français…) |
| [`C2_language_russian.png`](screenshots/C2_language_russian.png) | Каталог переключён на Русский — «Подборки, скидки и быстрые фильтры» |
| [`C3_currency_dropdown_open.png`](screenshots/C3_currency_dropdown_open.png) | Открытый дроп валюты в русском UI (USD, EUR, RUB, GBP, UAH, TRY) |
| [`C7_sort_dropdown_open.png`](screenshots/C7_sort_dropdown_open.png) | Открытый дроп сортировки — три опции на русском |
| [`C9_top_rated_products_eur.png`](screenshots/C9_top_rated_products_eur.png) | Продукты с сортировкой «Рейтинг: по убыванию», цены в EUR |
| [`D1_echo_plus_details.png`](screenshots/D1_echo_plus_details.png) | Детали Amazon Echo Plus на русском — €85.48, Рейтинг 5.0, отзывы |

---

## ISTQB-техники

| Техника | Применение |
|---------|-----------|
| **Equivalence Partitioning (EP)** | 10 языков, 10 валют, 3 sort-опции, категории продуктов |
| **Boundary Value Analysis (BVA)** | `page=0/1/totalPages/totalPages+1`, длина строки поиска (0, 1, 200 символов) |
| **Decision Table** | Комбинации lang × currency, sort × category, открытие 2 разных продуктов |
| **State Transition** | Каталог → загрузка → контент → детали → назад → каталог |

---

## Слэш-команды (Claude Code)

| Команда | Что делает |
|---------|-----------|
| `/write-tests` | Анализирует код и дописывает недостающие тесты |
| `/run-tests` | Запускает pytest, считает покрытие и quality score |
| `/run-project-and-tests` | Стартует Docker + бэкенд, затем запускает полный сьют |
| `/allure-report` | Генерирует Allure-отчёт и открывает на `http://127.0.0.1:4040` |
| `/android-tests` | KMP unit-тесты → Allure → совмещённый отчёт |

---

## Allure-severity уровни

| Severity | Примеры |
|----------|---------|
| `BLOCKER` | Сервер недоступен, каталог не загружается, детали продукта не открываются |
| `CRITICAL` | Поиск работает, навигация каталог↔детали, фильтры по категориям |
| `NORMAL` | Смена языка/валюты, пустой результат поиска, счётчик продуктов |
| `MINOR` | Спецсимволы в поиске, очень длинный запрос, isSourceLanguage флаг |

---

## Запуск бэкенда

```bash
# Через WSL2
wsl -d Ubuntu -- bash -c "cd /path/to/project && node backend/dist/index.js"

# Или через Docker
wsl -d Ubuntu -- bash -c "cd /path/to/project && docker compose up -d"

# Проверить здоровье
curl http://localhost:8080/health
# → {"status":"ok"}

# Проверить API
curl "http://localhost:8080/api/v1/catalog?lang=en&page=1&pageSize=5" | python -m json.tool
```
