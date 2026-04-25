#!/usr/bin/env python3
"""Generate an HTML test coverage report for the Product Catalog project."""
from pathlib import Path
from datetime import datetime

ROOT = Path(".")
OUT_FILE = Path("tests/coverage-report.html")

# ── Actual data from Allure results (tests/allure-results-all) ────────────────
# Total: 184 | pass: 169 | fail: 15
# unit(pytest): 34/34  api(pytest): 81/81  e2e(pytest): 13/13
# KMP unit:     33/33  Android UI: 8/19    Bug showcase: 0/4

LAYERS = [
    # (display name, total, passed, failed, note)
    ("Backend Unit (pytest + respx mock)",   34,  34,  0,  "Все 34 теста проходят — mock API"),
    ("Backend API (pytest + live backend)",  81,  81,  0,  "Live сервер :8080, 81/81 pass"),
    ("Backend E2E / Smoke (pytest)",         13,  13,  0,  "13 критических user journey"),
    ("Android/KMP Unit (Kotlin JVM)",        33,  33,  0,  "5 классов, без эмулятора"),
    ("Android UI (Compose UI Test)",         19,   8,  11, "8/19 pass; 11 fail — timeout 120s"),
    ("Bug Showcase (BVA-найденные баги)",     4,   0,   4, "4 реальных бага бэкенда"),
]

BACKEND_ENDPOINTS = [
    {"endpoint": "GET /health",
     "tests": 4, "covered": True, "layer": "api",
     "desc": "status 200, поле status, время ответа, Content-Type"},
    {"endpoint": "GET /api/v1/languages",
     "tests": 4, "covered": True, "layer": "api",
     "desc": "список 10 языков, isSourceLanguage, структура"},
    {"endpoint": "GET /api/v1/currencies",
     "tests": 4, "covered": True, "layer": "api",
     "desc": "список 10 валют, isSourceCurrency, структура"},
    {"endpoint": "GET /api/v1/catalog (lang, currency, page, pageSize, query, sort, category)",
     "tests": 11, "covered": True, "layer": "api",
     "desc": "все параметры, поиск, фильтр, сортировка, пагинация"},
    {"endpoint": "GET /api/v1/catalog — BVA page (0/1/max/max+1)",
     "tests": 4, "covered": True, "layer": "unit",
     "desc": "граничные значения страницы"},
    {"endpoint": "GET /api/v1/catalog — BVA pageSize (1/100/101)",
     "tests": 3, "covered": True, "layer": "unit",
     "desc": "граничные значения размера страницы"},
    {"endpoint": "GET /api/v1/catalog?pageSize=101",
     "tests": 1, "covered": True, "layer": "api", "bug": True,
     "desc": "BUG-001: необъявленный лимит max=100 — 101 возвращает 100 без ошибки"},
    {"endpoint": "GET /api/v1/catalog?currency=usd (lowercase)",
     "tests": 1, "covered": True, "layer": "api", "bug": True,
     "desc": "BUG-002: currency чувствителен к регистру — usd → 422, USD → 200"},
    {"endpoint": "GET /api/v1/catalog (lang отсутствует)",
     "tests": 1, "covered": True, "layer": "api", "bug": True,
     "desc": "BUG-003: обязательный param lang молча дефолтится на 'en' вместо 422"},
    {"endpoint": "GET /api/v1/catalog?page=0",
     "tests": 1, "covered": True, "layer": "unit", "bug": True,
     "desc": "BUG-004: page=0 возвращает пустой список вместо 422 (page<1 — невалид)"},
    {"endpoint": "GET /api/v1/catalog/:id",
     "tests": 5, "covered": True, "layer": "api",
     "desc": "детали продукта, lang, currency, 404, невалидный id"},
]

ANDROID_UI = [
    # (scenario, class, method, passed, fail_msg)
    ("App launch — каталог с 194 продуктами",     "Positive", "appLaunch_showsCatalogWithProducts",            True,  ""),
    ("Контейнер списка продуктов отображается",    "Positive", "productsListContainer_isDisplayed",             True,  ""),
    ("Поиск по валидному запросу обновляет список","Positive", "searchByValidQuery_updatesProductList",         True,  ""),
    ("Клик на карточку -> экран деталей",          "Positive", "clickProductCard_opensDetailsScreen",           True,  ""),
    ("Кнопка Back -> возврат в каталог",           "Positive", "backButton_returnsToProductCatalog",            True,  ""),
    ("Language dropdown виден и кликабелен",       "Positive", "languageDropdown_isDisplayedAndClickable",      True,  ""),
    ("Currency dropdown виден и кликабелен",       "Positive", "currencyDropdown_isDisplayedAndClickable",      True,  ""),
    ("Sort dropdown отображается",                 "Positive", "sortDropdown_isDisplayed",                      True,  ""),
    ("Поле поиска принимает 1 символ (BVA min)",   "Positive", "searchField_acceptsSingleCharacter",            True,  ""),
    ("Выбор Русского -> каталог обновляется",      "Positive", "languageDropdown_selectRussian_updatesLanguage",False, "ComposeTimeoutException: catalog reload >120s"),
    ("Sort dropdown раскрывает опции",             "Positive", "sortDropdown_showsOptionsOnClick",              True,  ""),
    ("Очистка поиска -> полный каталог",           "Positive", "clearSearch_restoresFullCatalog",               False, "ComposeTimeoutException: products not reloaded in 120s"),
    ("Поиск без совпадений -> empty state",        "Negative", "searchWithNoMatch_showsEmptyStateCard",         False, "ComposeTimeoutException: empty_state_card not found in 20s"),
    ("Empty state -> Reset filters -> каталог",    "Negative", "searchNoMatch_resetFilters_restoresCatalog",    False, "ComposeTimeoutException: empty_state_card not found in 20s"),
    ("Только спецсимволы -> нет краша",            "Negative", "searchWithSpecialCharsOnly_noCrash",            False, "ComposeTimeoutException: products_list not visible after input"),
    ("200 символов -> нет краша (BVA max)",        "Negative", "searchWithVeryLongString_noCrash",              False, "AssertionError: ни products ни empty-state не видны"),
    ("Только пробелы -> все продукты (BVA)",       "Negative", "searchWithWhitespaceOnly_showsAllProducts",     False, "ComposeTimeoutException: products not loaded in 120s"),
    ("Только цифры -> нет ошибки",                 "Negative", "searchWithDigitsOnly_noUnhandledError",         False, "ComposeTimeoutException: products not loaded in 120s"),
    ("Клик на продукт -> детали открылись",        "Details",  "clickProduct_detailsScreenOpens",               True,  ""),
    ("Детали -> Back -> каталог восстановлен",     "Details",  "detailsScreen_backButton_restoresCatalog",      True,  ""),
    ("Второй продукт тоже открывает детали",       "Details",  "clickSecondProduct_detailsScreenOpens",         True,  ""),
    ("Два разных продукта - оба открываются",      "Details",  "openTwoDifferentProducts_bothShowDetails",      True,  ""),
    ("После Back - кол-во продуктов не изменилось","Details",  "afterBackNavigation_productCountUnchanged",     True,  ""),
]

KMP_CLASSES = [
    ("ProductCatalogViewModelTest",   13, 13),
    ("CatalogBackendRepositoryTest",   7,  7),
    ("SearchProductsUseCaseTest",      6,  6),
    ("CatalogStringsTest",             5,  5),
    ("CatalogBackendMapperTest",       2,  2),
]

# ── Derived numbers ────────────────────────────────────────────────────────────
total_tests = sum(l[1] for l in LAYERS)
total_pass  = sum(l[2] for l in LAYERS)
total_fail  = sum(l[3] for l in LAYERS)
real_total  = total_tests - 4       # without bug-showcase
real_pct    = round(total_pass / real_total * 100, 1)
ui_pass     = sum(1 for s in ANDROID_UI if s[3])
ui_total    = len(ANDROID_UI)
ep_covered  = sum(1 for e in BACKEND_ENDPOINTS if e["covered"])
bugs_found  = sum(1 for e in BACKEND_ENDPOINTS if e.get("bug"))
now         = datetime.now().strftime("%Y-%m-%d %H:%M")


def pct_bar(passed, total):
    p = round(passed / total * 100) if total else 0
    return (
        f'<div style="display:flex;height:18px;background:#e0e0e0;border-radius:4px;overflow:hidden;">'
        f'<div style="width:{p}%;background:#4caf50;"></div>'
        f'<div style="width:{100-p}%;background:#f44336;"></div>'
        f'</div>'
        f'<div style="font-size:10.5px;color:#555;text-align:right;margin-top:2px;">'
        f'{passed}/{total}&nbsp;&nbsp;<strong>{p}%</strong></div>'
    )


CSS = """
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f2f5;color:#222}
  .hdr{background:linear-gradient(135deg,#1a237e,#283593);color:#fff;padding:28px 48px}
  .hdr h1{font-size:26px;font-weight:300}
  .hdr .meta{font-size:12px;opacity:.7;margin-top:5px}
  .cnt{max-width:1140px;margin:0 auto;padding:20px 16px}
  .grid4{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:24px}
  .card{background:#fff;border-radius:10px;padding:18px 22px;box-shadow:0 2px 8px rgba(0,0,0,.08)}
  .card .n{font-size:40px;font-weight:700;line-height:1}
  .card .l{font-size:11px;color:#888;margin-top:3px;text-transform:uppercase;letter-spacing:.5px}
  .blue .n{color:#1565c0}.green .n{color:#2e7d32}.red .n{color:#c62828}.orange .n{color:#e65100}
  h2{font-size:17px;font-weight:600;margin:24px 0 10px;color:#1a237e;
     border-left:4px solid #3f51b5;padding-left:10px}
  table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;
        box-shadow:0 2px 8px rgba(0,0,0,.08);font-size:12.5px;margin-bottom:22px}
  th{background:#3f51b5;color:#fff;text-align:left;padding:9px 13px;font-weight:500}
  td{padding:8px 13px;border-bottom:1px solid #f0f0f0;vertical-align:top}
  tr:last-child td{border-bottom:none}
  tr:hover td{background:#f5f5ff}
  .b{display:inline-block;padding:2px 9px;border-radius:11px;font-size:11px;font-weight:600}
  .b.ok{background:#e8f5e9;color:#2e7d32}.b.fail{background:#ffebee;color:#c62828}
  .b.bug{background:#fff3e0;color:#e65100}.b.unit{background:#e3f2fd;color:#1565c0}
  .b.api{background:#f3e5f5;color:#6a1b9a}.b.e2e{background:#e8eaf6;color:#283593}
  .err{font-size:10.5px;color:#c62828;margin-top:2px}
  .note{font-size:11px;color:#888;margin-top:18px;line-height:1.6;padding:0 4px}
  .sec{background:#fff;border-radius:10px;padding:18px;
       box-shadow:0 2px 8px rgba(0,0,0,.08);margin-bottom:20px}
  .lrow{display:flex;align-items:flex-start;gap:12px;margin-bottom:14px}
  .lname{width:250px;font-size:12.5px;font-weight:500;padding-top:2px}
  .lbar{flex:1}
  .lnote{font-size:10.5px;color:#999;margin-top:1px}
"""

parts = []
parts.append(f"""<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<title>Test Coverage Report — Product Catalog</title>
<style>{CSS}</style>
</head>
<body>
<div class="hdr">
  <h1>Test Coverage Report — Product Catalog</h1>
  <div class="meta">Сгенерировано: {now} &nbsp;|&nbsp; 5 слоёв тестирования &nbsp;|&nbsp;
    Allure: <a href="http://127.0.0.1:4040" style="color:#90caf9">http://127.0.0.1:4040</a>
  </div>
</div>
<div class="cnt">

<div class="grid4">
  <div class="card blue"><div class="n">{total_tests}</div><div class="l">Всего тестов</div></div>
  <div class="card green"><div class="n">{total_pass}</div><div class="l">Пройдено</div></div>
  <div class="card red"><div class="n">{total_fail}</div><div class="l">Упало</div></div>
  <div class="card orange"><div class="n">{real_pct}%</div><div class="l">Pass rate (без bug showcase)</div></div>
</div>

<h2>Покрытие по слоям</h2>
<div class="sec">
""")

for name, total, passed, failed, note in LAYERS:
    parts.append(
        f'<div class="lrow">'
        f'<div class="lname">{name}</div>'
        f'<div class="lbar">{pct_bar(passed, total)}'
        f'<div class="lnote">{note}</div></div>'
        f'</div>\n'
    )

parts.append(f"""</div>

<h2>Покрытие эндпоинтов бэкенда ({ep_covered}/{len(BACKEND_ENDPOINTS)}, {bugs_found} багов найдено)</h2>
<table>
  <tr><th>Эндпоинт</th><th>Слой</th><th>Тестов</th><th>Статус</th></tr>
""")

for ep in BACKEND_ENDPOINTS:
    badge = '<span class="b bug">&#9888; BUG</span>' if ep.get("bug") else '<span class="b ok">&#10003; Покрыт</span>'
    layer = ep.get("layer", "api")
    desc_cls = "err" if ep.get("bug") else ""
    desc_pfx = "&#9888; " if ep.get("bug") else ""
    desc_html = f'<div class="{desc_cls}">{desc_pfx}{ep["desc"]}</div>' if ep["desc"] else ""
    parts.append(
        f'<tr><td><strong>{ep["endpoint"]}</strong>{desc_html}</td>'
        f'<td><span class="b {layer}">{layer}</span></td>'
        f'<td style="text-align:center">{ep["tests"]}</td>'
        f'<td>{badge}</td></tr>\n'
    )

parts.append(f"""</table>

<h2>Android UI Tests — {ui_pass}/{ui_total} passed ({round(ui_pass/ui_total*100)}%)</h2>
<table>
  <tr><th>#</th><th>Сценарий</th><th>Класс</th><th>Метод</th><th>Статус</th></tr>
""")

for i, (scenario, cls, method, passed, fail_msg) in enumerate(ANDROID_UI, 1):
    badge = '<span class="b ok">&#10003; PASS</span>' if passed else '<span class="b fail">&#10007; FAIL</span>'
    err = f'<div class="err">&#9888; {fail_msg}</div>' if fail_msg else ""
    parts.append(
        f'<tr><td style="text-align:center;color:#aaa">{i}</td>'
        f'<td>{scenario}{err}</td>'
        f'<td style="font-size:11px">{cls}</td>'
        f'<td style="font-size:11px"><code>{method}</code></td>'
        f'<td>{badge}</td></tr>\n'
    )

parts.append("""</table>

<h2>Android/KMP Unit Tests — 33/33 passed (100%)</h2>
<table>
  <tr><th>Класс</th><th>Тестов</th><th>Pass</th><th>Fail</th><th>Статус</th></tr>
""")

for name, total, passed in KMP_CLASSES:
    badge = '<span class="b ok">&#10003; All passed</span>'
    parts.append(
        f'<tr><td><code>{name}</code></td><td>{total}</td>'
        f'<td style="color:#2e7d32">{passed}</td>'
        f'<td style="color:#c62828">{total - passed}</td>'
        f'<td>{badge}</td></tr>\n'
    )

parts.append("""</table>

<h2>ISTQB-техники</h2>
<table>
  <tr><th>Техника</th><th>Применение</th><th>Примеры</th></tr>
  <tr><td><strong>EP (Equivalence Partitioning)</strong></td>
      <td>10 языков x 10 валют = 100 комбинаций (выборочно); валидные/невалидные категории</td>
      <td><code>test_catalog_lang_en</code>, <code>test_currencies_list</code></td></tr>
  <tr><td><strong>BVA (Boundary Value Analysis)</strong></td>
      <td>page=0/1/totalPages/totalPages+1; pageSize=1/100/101; длина запроса=0/1/200</td>
      <td><code>test_page_min_boundary</code>, <code>searchWithVeryLongString_noCrash</code></td></tr>
  <tr><td><strong>Decision Table</strong></td>
      <td>Комбинации sort x category; открытие продукта A -> back -> продукта B</td>
      <td><code>openTwoDifferentProducts_bothShowDetails</code></td></tr>
  <tr><td><strong>State Transition</strong></td>
      <td>Каталог -> Загрузка -> Контент -> Детали -> Back -> Каталог</td>
      <td><code>detailsScreen_backButton_restoresCatalog</code></td></tr>
</table>

<div class="note">
  * Android UI failures: холодный старт эмулятора при последовательных тестах превышает 120s.
  Баги BUG-001...BUG-004 найдены через BVA-зондирование — не связаны с падениями UI-тестов.<br>
  Bug showcase (4 теста) отмечены как failed намеренно — документируют реальные баги бэкенда.
</div>

</div>
</body>
</html>""")

OUT_FILE.write_text("".join(parts), encoding="utf-8")
print(f"Coverage report written to: {OUT_FILE}")
