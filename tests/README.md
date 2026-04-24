# Test Suite — Product Catalog API

Автоматизированные тесты для backend-сервиса каталога продуктов.  
Стек: **pytest** · **httpx** · **respx** · **allure-pytest** · **factory-boy**

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
├── ui/                          # Smoke-тесты — критические пользовательские сценарии
│   ├── conftest.py              # smoke_client fixture
│   └── test_smoke.py            # 13 user journeys из testing-spec.md
│
└── pipeline/                    # Файлы коммуникации между агентами (в .gitignore)
    ├── project-analysis.json    # ← write-tests агент
    ├── test-results.json        # ← run-tests агент
    ├── coverage-report.json     # ← run-tests агент
    └── run-status.json          # ← run-project-and-tests агент
```

---

## Быстрый старт

### Установка зависимостей

```bash
pip install -r tests/requirements-test.txt
```

### Запуск unit-тестов (без сервера)

```bash
python -m pytest tests/unit/ -v -m unit
```

### Запуск всех тестов (сервер должен быть запущен)

```bash
python -m pytest tests/ -v
```

### Запуск по маркеру

```bash
python -m pytest tests/ -m "positive"      # только позитивные кейсы
python -m pytest tests/ -m "negative"      # только негативные кейсы
python -m pytest tests/ -m "boundary"      # только BVA-тесты
python -m pytest tests/ -m "smoke"         # только smoke-тесты
python -m pytest tests/ -m "api and not boundary"  # API без BVA
```

---

## Слэш-команды (Claude Code)

| Команда | Что делает |
|---------|-----------|
| `/write-tests` | Анализирует код и дописывает недостающие тесты |
| `/run-tests` | Запускает тесты, считает покрытие и quality score |
| `/run-project-and-tests` | Стартует Docker + бэкенд, затем запускает полный сьют |
| `/allure-report` | Генерирует Allure-отчёт и открывает на `http://127.0.0.1:4040` |

---

## Слои тестирования

| Слой | Маркер | Зависимости | Когда запускать |
|------|--------|-------------|-----------------|
| Unit | `@pytest.mark.unit` | Нет (respx mock) | Всегда, включая CI без сервера |
| API | `@pytest.mark.api` | Живой бэкенд на :8080 | После запуска `docker compose up` |
| Smoke/UI | `@pytest.mark.ui` | Живой бэкенд на :8080 | Перед релизом |

---

## ISTQB-техники

| Техника | Применение |
|---------|-----------|
| **Equivalence Partitioning (EP)** | Все 10 языков, 10 валют, 3 sort-опции тестируются как классы |
| **Boundary Value Analysis (BVA)** | `page=0` (invalid), `page=1` (min), `page=totalPages+1` (beyond max) |
| **Decision Table** | Комбинации lang × currency, sort × category |
| **State Transition** | Пустой каталог → populated, exchange rates loading → 503 |

---

## Allure-категории severity

| Severity | Примеры тестов |
|----------|---------------|
| `BLOCKER` | Сервер недоступен, каталог пуст, детали продукта не открываются |
| `CRITICAL` | Неверный код языка → 400, сортировка работает, пагинация консистентна |
| `NORMAL` | Дубликаты кодов, isSourceLanguage флаг, meta.query эхо |
| `MINOR` | isSourceCurrency флаг, negative product ID |

---

## Запуск сервера (необходим для API и Smoke тестов)

```bash
# Через WSL2 (если Docker не установлен на Windows)
wsl -d Ubuntu -- bash -c "cd /mnt/c/Users/Dmitrii/projects/intern-assignment-03-test && sudo dockerd > /tmp/dockerd.log 2>&1 & sleep 3 && sudo docker compose up -d"

# Проверить здоровье
curl http://localhost:8080/health
```
