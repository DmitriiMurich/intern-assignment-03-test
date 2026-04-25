"""
Generate tests/test-report.pdf — visual test report with screenshots and coverage.
Matches Allure report: 184 total | 169 pass | 15 fail.
"""
import os
import sys
from pathlib import Path
from datetime import date

try:
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.units import cm
    from reportlab.lib import colors
    from reportlab.platypus import (
        SimpleDocTemplate, Paragraph, Spacer, Image, Table,
        TableStyle, PageBreak, HRFlowable, KeepTogether,
    )
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
except ImportError:
    sys.exit("reportlab not installed — run: pip install reportlab")

ROOT = Path(__file__).parent.parent.parent
SCREENSHOTS = ROOT / "tests" / "screenshots"
OUT_PDF = ROOT / "tests" / "test-report.pdf"

W, H = A4

# ── Colors ────────────────────────────────────────────────────────────────────
GREEN  = colors.HexColor("#2ecc71")
RED    = colors.HexColor("#e74c3c")
ORANGE = colors.HexColor("#f39c12")
BLUE   = colors.HexColor("#3498db")
PURPLE = colors.HexColor("#9b59b6")
DARK   = colors.HexColor("#2c3e50")
LIGHT  = colors.HexColor("#ecf0f1")
GRAY   = colors.HexColor("#95a5a6")
WHITE  = colors.white

# ── Styles ────────────────────────────────────────────────────────────────────
styles = getSampleStyleSheet()

def S(name, parent="Normal", **kw):
    return ParagraphStyle(name, parent=styles[parent], **kw)

title_s   = S("T",  fontSize=24, textColor=WHITE, alignment=TA_CENTER, leading=30)
sub_s     = S("Su", fontSize=11, textColor=colors.HexColor("#bdc3c7"), alignment=TA_CENTER)
h1_s      = S("H1", fontSize=14, textColor=DARK, spaceBefore=12, spaceAfter=5, leading=18)
h2_s      = S("H2", fontSize=10, textColor=colors.HexColor("#555"), spaceBefore=6, spaceAfter=3)
body_s    = S("Bo", fontSize=8.5, textColor=colors.HexColor("#555"), spaceAfter=3, leading=12)
cap_s     = S("Ca", fontSize=7.5, textColor=GRAY, alignment=TA_CENTER, spaceAfter=1)
mono_s    = S("Mo", fontName="Courier", fontSize=7.5, textColor=DARK, leading=11)
note_s    = S("No", fontSize=7.5, textColor=GRAY, leading=11, spaceAfter=2)


def img(path, max_w, max_h):
    if not os.path.exists(path):
        return Spacer(1, 0.5 * cm)
    im = Image(str(path))
    iw, ih = im.imageWidth, im.imageHeight
    scale = min(max_w / iw, max_h / ih, 1.0)
    im.drawWidth = iw * scale
    im.drawHeight = ih * scale
    im.hAlign = "CENTER"
    return im


# ── Table helpers ─────────────────────────────────────────────────────────────
HDR_STYLE = [
    ("BACKGROUND",   (0, 0), (-1, 0), DARK),
    ("TEXTCOLOR",    (0, 0), (-1, 0), WHITE),
    ("FONTNAME",     (0, 0), (-1, 0), "Helvetica-Bold"),
    ("FONTSIZE",     (0, 0), (-1, -1), 8),
    ("TOPPADDING",   (0, 0), (-1, -1), 4),
    ("BOTTOMPADDING",(0, 0), (-1, -1), 4),
    ("GRID",         (0, 0), (-1, -1), 0.4, colors.HexColor("#dee2e6")),
    ("ROWBACKGROUNDS",(0, 1), (-1, -1), [WHITE, colors.HexColor("#f8f9fa")]),
    ("VALIGN",       (0, 0), (-1, -1), "TOP"),
]

FW = W - 3 * cm  # full table width


# ── Sections ──────────────────────────────────────────────────────────────────

def cover(els):
    els.append(Spacer(1, 0.8 * cm))
    bg_data = [[""]]
    bg = Table(bg_data, colWidths=[FW], rowHeights=[5 * cm])
    bg.setStyle(TableStyle([("BACKGROUND", (0,0), (-1,-1), DARK)]))

    els.append(Paragraph("Product Catalog", title_s))
    els.append(Paragraph("Full Test Report", sub_s))
    els.append(Paragraph(f"Generated: {date.today().isoformat()}", sub_s))
    els.append(Spacer(1, 0.5 * cm))
    els.append(Paragraph(
        "Automated test suite covering backend API, Android/KMP unit tests, and "
        "Android UI instrumented tests. Screenshots captured on Pixel 3a emulator "
        "(API 29, fast_avd) against live backend at http://10.0.2.2:8080.",
        body_s,
    ))
    els.append(Spacer(1, 0.4 * cm))


def summary_table(els):
    els.append(Paragraph("Test Results Summary", h1_s))
    # Match Allure report exactly: unit 34, api 81, e2e 13, KMP 33, Android UI 19, Bugs 4
    data = [
        ["Layer",                         "Total", "Pass", "Fail", "Pass %"],
        ["Backend Unit (pytest + mock)",    "34",   "34",   "0",   "100%"],
        ["Backend API (pytest + live)",     "81",   "81",   "0",   "100%"],
        ["Backend E2E / Smoke (pytest)",    "13",   "13",   "0",   "100%"],
        ["Android/KMP Unit (Kotlin JVM)",   "33",   "33",   "0",   "100%"],
        ["Android UI (Compose UI Test)",    "19",    "8",  "11",    "42%"],
        ["Bug Showcase (real backend bugs)", "4",    "0",   "4",     "—"],
        ["TOTAL",                          "184",  "169",  "15",   "93.9%"],
    ]
    col_w = [FW * p for p in [0.42, 0.10, 0.10, 0.10, 0.28]]
    t = Table(data, colWidths=col_w)
    styles_list = list(HDR_STYLE) + [
        ("FONTNAME", (0, -1), (-1, -1), "Helvetica-Bold"),
        ("BACKGROUND", (0, -1), (-1, -1), LIGHT),
        ("ALIGN", (1, 0), (-1, -1), "CENTER"),
        ("TEXTCOLOR", (4, 1), (4, 4), GREEN),
        ("TEXTCOLOR", (4, 5), (4, 5), ORANGE),
        ("TEXTCOLOR", (4, 6), (4, 6), RED),
        ("TEXTCOLOR", (4, 7), (4, 7), BLUE),
        ("FONTNAME", (4, 1), (4, -1), "Helvetica-Bold"),
    ]
    t.setStyle(TableStyle(styles_list))
    els.append(t)
    els.append(Spacer(1, 0.3 * cm))


def coverage_section(els):
    els.append(Paragraph("Test Coverage by Feature Area", h1_s))

    # Inline mini progress bars via Table
    layers = [
        ("Backend Unit",      34,  34,  "100%", GREEN),
        ("Backend API",       81,  81,  "100%", GREEN),
        ("Backend E2E",       13,  13,  "100%", GREEN),
        ("Android/KMP Unit",  33,  33,  "100%", GREEN),
        ("Android UI",        19,   8,   "42%", ORANGE),
    ]
    data = [["Layer", "Passed", "Total", "Pass Rate", "Coverage"]]
    for name, total, passed, pct, color in layers:
        data.append([name, str(passed), str(total), pct,
                     f"{passed} of {total} test cases"])
    col_w = [FW * p for p in [0.28, 0.10, 0.10, 0.15, 0.37]]
    t = Table(data, colWidths=col_w)
    st = list(HDR_STYLE) + [
        ("ALIGN", (1, 0), (3, -1), "CENTER"),
        ("TEXTCOLOR", (3, 1), (3, 4), GREEN),
        ("TEXTCOLOR", (3, 5), (3, 5), ORANGE),
        ("FONTNAME", (3, 1), (3, -1), "Helvetica-Bold"),
    ]
    t.setStyle(TableStyle(st))
    els.append(t)
    els.append(Spacer(1, 0.3 * cm))

    # Backend endpoint coverage
    els.append(Paragraph("Backend Endpoint Coverage (5/5 endpoints, 4 bugs found)", h2_s))
    ep_data = [["Endpoint", "Layer", "Tests", "Status"]]
    endpoints = [
        ("GET /health",                       "api",  "4",  "OK",    False),
        ("GET /api/v1/languages",             "api",  "4",  "OK",    False),
        ("GET /api/v1/currencies",            "api",  "4",  "OK",    False),
        ("GET /api/v1/catalog (all params)",  "api", "11",  "OK",    False),
        ("GET /api/v1/catalog BVA page",      "unit", "4",  "OK",    False),
        ("GET /api/v1/catalog BVA pageSize",  "unit", "3",  "OK",    False),
        ("GET /catalog?pageSize=101",         "api",  "1", "BUG-001",True),
        ("GET /catalog?currency=usd",         "api",  "1", "BUG-002",True),
        ("GET /catalog (no lang param)",      "api",  "1", "BUG-003",True),
        ("GET /catalog?page=0",              "unit",  "1", "BUG-004",True),
        ("GET /api/v1/catalog/:id",           "api",  "5",  "OK",    False),
    ]
    for ep, layer, cnt, status, is_bug in endpoints:
        ep_data.append([ep, layer, cnt, status])
    col_w2 = [FW * p for p in [0.50, 0.12, 0.10, 0.28]]
    t2 = Table(ep_data, colWidths=col_w2)
    st2 = list(HDR_STYLE) + [("ALIGN", (2, 0), (2, -1), "CENTER")]
    for i, (_, _, _, status, is_bug) in enumerate(endpoints, 1):
        c = RED if is_bug else GREEN
        st2.append(("TEXTCOLOR", (3, i), (3, i), c))
        if is_bug:
            st2.append(("FONTNAME", (3, i), (3, i), "Helvetica-Bold"))
    t2.setStyle(TableStyle(st2))
    els.append(t2)
    els.append(Spacer(1, 0.3 * cm))


def bugs_section(els):
    els.append(Paragraph("Backend Bugs Found via BVA Probing", h1_s))
    bugs = [
        ("BUG-001", "CRITICAL", "pageSize silently capped at 100",
         "pageSize=101 returns 100 items without 4xx error — undocumented limit"),
        ("BUG-002", "MAJOR",    "Currency code is case-sensitive",
         "currency=usd returns 422; currency=USD returns 200 — RFC 4217 codes are case-insensitive"),
        ("BUG-003", "MINOR",    "Missing lang param silently defaults",
         "lang omitted -> silently falls back to 'en' instead of returning 422"),
        ("BUG-004", "MINOR",    "page=0 returns empty list instead of 422",
         "pageSize=0 returns 422 (correct), but page=0 returns empty list — inconsistent"),
    ]
    data = [["ID", "Severity", "Title", "Description"]]
    for b in bugs:
        data.append(list(b))
    col_w = [FW * p for p in [0.10, 0.11, 0.30, 0.49]]
    t = Table(data, colWidths=col_w)
    st = list(HDR_STYLE) + [("VALIGN", (0, 0), (-1, -1), "TOP")]
    sev_c = {"CRITICAL": RED, "MAJOR": ORANGE, "MINOR": BLUE}
    for i, (_, sev, *_) in enumerate(bugs, 1):
        st.append(("TEXTCOLOR", (1, i), (1, i), sev_c[sev]))
        st.append(("FONTNAME",  (1, i), (1, i), "Helvetica-Bold"))
    t.setStyle(TableStyle(st))
    els.append(t)
    els.append(Spacer(1, 0.3 * cm))


def android_ui_table(els):
    els.append(Paragraph("Android UI Test Results (Compose UI Test — 23 test methods, 19 in Allure report)", h1_s))
    data = [["#", "Test Method", "Class", "Result"]]
    tests = [
        (1,  "appLaunch_showsCatalogWithProducts",             "Positive", True),
        (2,  "productsListContainer_isDisplayed",              "Positive", True),
        (3,  "searchByValidQuery_updatesProductList",          "Positive", True),
        (4,  "clickProductCard_opensDetailsScreen",            "Positive", True),
        (5,  "backButton_returnsToProductCatalog",             "Positive", True),
        (6,  "languageDropdown_isDisplayedAndClickable",       "Positive", True),
        (7,  "currencyDropdown_isDisplayedAndClickable",       "Positive", True),
        (8,  "sortDropdown_isDisplayed",                       "Positive", True),
        (9,  "searchField_acceptsSingleCharacter",             "Positive", True),
        (10, "languageDropdown_selectRussian_updatesLanguage", "Positive", False),
        (11, "sortDropdown_showsOptionsOnClick",               "Positive", True),
        (12, "clearSearch_restoresFullCatalog",                "Positive", False),
        (13, "searchWithNoMatch_showsEmptyStateCard",          "Negative", False),
        (14, "searchNoMatch_resetFilters_restoresCatalog",     "Negative", False),
        (15, "searchWithSpecialCharsOnly_noCrash",             "Negative", False),
        (16, "searchWithVeryLongString_noCrash",               "Negative", False),
        (17, "searchWithWhitespaceOnly_showsAllProducts",      "Negative", False),
        (18, "searchWithDigitsOnly_noUnhandledError",          "Negative", False),
        (19, "clickProduct_detailsScreenOpens",                "Details",  True),
        (20, "detailsScreen_backButton_restoresCatalog",       "Details",  True),
        (21, "clickSecondProduct_detailsScreenOpens",          "Details",  True),
        (22, "openTwoDifferentProducts_bothShowDetails",       "Details",  True),
        (23, "afterBackNavigation_productCountUnchanged",      "Details",  True),
    ]
    for num, method, cls, passed in tests:
        data.append([str(num), method, cls, "PASS" if passed else "FAIL"])

    col_w = [FW * p for p in [0.05, 0.55, 0.18, 0.22]]
    t = Table(data, colWidths=col_w)
    st = list(HDR_STYLE) + [("ALIGN", (0, 0), (0, -1), "CENTER")]
    for i, (_, _, _, passed) in enumerate(tests, 1):
        c = GREEN if passed else RED
        st.append(("TEXTCOLOR", (3, i), (3, i), c))
        st.append(("FONTNAME",  (3, i), (3, i), "Helvetica-Bold"))
    t.setStyle(TableStyle(st))
    els.append(t)
    els.append(Spacer(1, 0.3 * cm))


def screenshot_grid(els, title, shots):
    els.append(Paragraph(title, h1_s))
    els.append(HRFlowable(width="100%", thickness=0.8, color=LIGHT))
    els.append(Spacer(1, 0.15 * cm))

    cell_w = FW / 2 - 0.2 * cm
    cell_h = 9 * cm

    rows = []
    row = []
    for path, caption in shots:
        full = SCREENSHOTS / path
        cell = [img(full, cell_w, cell_h - 0.9 * cm), Spacer(1, 0.05 * cm),
                Paragraph(caption, cap_s)]
        row.append(cell)
        if len(row) == 2:
            rows.append(row)
            row = []
    if row:
        row.append([""])
        rows.append(row)

    for pair in rows:
        t = Table([pair], colWidths=[cell_w + 0.15 * cm] * 2)
        t.setStyle(TableStyle([
            ("VALIGN",       (0, 0), (-1, -1), "TOP"),
            ("ALIGN",        (0, 0), (-1, -1), "CENTER"),
            ("TOPPADDING",   (0, 0), (-1, -1), 3),
            ("BOTTOMPADDING",(0, 0), (-1, -1), 6),
            ("LEFTPADDING",  (0, 0), (-1, -1), 3),
            ("RIGHTPADDING", (0, 0), (-1, -1), 3),
        ]))
        els.append(t)
    els.append(Spacer(1, 0.2 * cm))


def istqb_table(els):
    els.append(Paragraph("ISTQB Techniques Applied", h1_s))
    data = [
        ["Technique", "Where Applied", "Count"],
        ["Equivalence Partitioning (EP)",
         "10 langs x 10 currencies = 100 EP classes; valid/invalid categories",
         "100 EP classes"],
        ["Boundary Value Analysis (BVA)",
         "page=0/1/totalPages/totalPages+1; pageSize=1/100/101; search len 0/1/200",
         "10 boundary points"],
        ["Decision Table",
         "lang x currency combos; sort x category; open product A, back, product B",
         "4 tables"],
        ["State Transition",
         "Catalog -> Loading -> Content -> Details -> Back -> Catalog; filtered -> reset -> full",
         "6 states"],
    ]
    col_w = [FW * p for p in [0.28, 0.56, 0.16]]
    t = Table(data, colWidths=col_w)
    t.setStyle(TableStyle(list(HDR_STYLE) + [("VALIGN", (0, 0), (-1, -1), "TOP")]))
    els.append(t)
    els.append(Spacer(1, 0.3 * cm))


def kmp_table(els):
    els.append(Paragraph("Android/KMP Unit Tests — 33/33 passed (100%)", h1_s))
    data = [["Test Class", "Tests", "Pass", "Fail", "Coverage Area"]]
    classes = [
        ("ProductCatalogViewModelTest",   13, 13, "ViewModel state, pagination, search, filters"),
        ("CatalogBackendRepositoryTest",   7,  7, "HTTP requests, error handling, retry logic"),
        ("SearchProductsUseCaseTest",      6,  6, "Search debounce, query normalization"),
        ("CatalogStringsTest",             5,  5, "Price formatting, rating display, date parsing"),
        ("CatalogBackendMapperTest",       2,  2, "DTO -> domain model mapping, currency fallback"),
    ]
    for name, total, passed, area in classes:
        data.append([name, str(total), str(passed), "0", area])
    col_w = [FW * p for p in [0.34, 0.08, 0.08, 0.07, 0.43]]
    t = Table(data, colWidths=col_w)
    st = list(HDR_STYLE) + [
        ("ALIGN", (1, 0), (3, -1), "CENTER"),
        ("TEXTCOLOR", (2, 1), (2, -1), GREEN),
        ("FONTNAME", (2, 1), (2, -1), "Helvetica-Bold"),
    ]
    t.setStyle(TableStyle(st))
    els.append(t)
    els.append(Spacer(1, 0.3 * cm))


# ── Build ─────────────────────────────────────────────────────────────────────
def build():
    doc = SimpleDocTemplate(
        str(OUT_PDF), pagesize=A4,
        leftMargin=1.5*cm, rightMargin=1.5*cm,
        topMargin=1.5*cm,  bottomMargin=1.5*cm,
    )
    els = []

    # Page 1 — Cover + Summary + Coverage + Bugs
    cover(els)
    summary_table(els)
    coverage_section(els)
    els.append(PageBreak())

    # Page 2 — Bugs + Android UI table
    bugs_section(els)
    android_ui_table(els)
    els.append(PageBreak())

    # Page 3 — KMP table + ISTQB
    kmp_table(els)
    istqb_table(els)
    els.append(PageBreak())

    # Page 4 — Screenshots: launch, catalog, details
    screenshot_grid(els, "Section A — App Launch & Catalog", [
        ("A1_loading_state.png",       "A1 · Loading — \"Fetching products from the API\""),
        ("A2_catalog_english_all.png", "A2 · Catalog — English, All Categories, 194 products"),
        ("A3_category_laptops.png",    "A3 · Filter: Laptops — \"Showing 5 of 5 items\""),
        ("A5_laptops_results.png",     "A5 · Laptop products sorted by price"),
    ])
    els.append(PageBreak())

    # Page 5 — Product details
    screenshot_grid(els, "Section B — Product Details", [
        ("A6_product_details.png", "A6 · Lenovo Yoga 920, $1,099.99, Back to catalog"),
        ("A7_product_reviews.png", "A7 · Reviews: Nathan Reed 5.0, Bella 4.0, Hazel 4.0"),
    ])

    screenshot_grid(els, "Section C — Search & Empty State", [
        ("B1_search_laptop.png",    "B1 · Search \"laptop\" — 5 results"),
        ("B3_empty_state_card.png", "B3 · Empty state — \"Nothing found\", Reset filters"),
    ])
    els.append(PageBreak())

    # Page 6 — Language / Currency / Sort
    screenshot_grid(els, "Section D — Language & Currency", [
        ("C1_language_dropdown_open.png", "C1 · Language dropdown — 6 languages"),
        ("C2_language_russian.png",       "C2 · Russian UI — catalog in Russian"),
        ("C3_currency_dropdown_open.png", "C3 · Currency dropdown — USD, EUR, RUB, GBP..."),
        ("C9_top_rated_products_eur.png", "C9 · Top rated, EUR prices in Russian UI"),
    ])
    els.append(PageBreak())

    # Page 7 — Sort + Russian details
    screenshot_grid(els, "Section E — Sort & Product Details in Russian", [
        ("C7_sort_dropdown_open.png", "C7 · Sort dropdown — 3 options in Russian"),
        ("D1_echo_plus_details.png",  "D1 · Echo Plus in Russian, EUR 85.48, Rating 5.0"),
    ])

    # Footer
    els.append(HRFlowable(width="100%", thickness=0.5, color=LIGHT))
    els.append(Spacer(1, 0.15 * cm))
    els.append(Paragraph(
        f"Product Catalog — Test Report  |  {date.today().isoformat()}  |  "
        "pytest + httpx + respx + allure-pytest + Compose UI Test + Kotlin Test",
        cap_s,
    ))

    doc.build(els)
    print(f"PDF written to: {OUT_PDF}")


if __name__ == "__main__":
    build()
