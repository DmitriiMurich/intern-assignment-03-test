---
description: Run pytest suite, calculate API surface coverage and quality score
---

Run the pytest test suite and produce a quality report.

Steps:
1. Check if backend is reachable: curl -sf http://localhost:8080/health
2. Always run unit tests: python -m pytest tests/unit/ -v --tb=short -m unit
3. If backend is alive, also run: python -m pytest tests/api/ tests/ui/ -v --tb=short
4. Calculate API surface coverage:
   - Endpoints tested (max 5): /health, /languages, /currencies, /catalog, /catalog/:id
   - Languages tested (max 10): en, ru, de, fr, es, it, pt, tr, uk, zh
   - Currencies tested (max 10): USD, EUR, RUB, GBP, UAH, TRY, CNY, JPY, CAD, CHF
   - Sort options tested (max 3): price_asc, price_desc, rating_desc
   - Error cases tested (max 8): 400/404 per endpoint
   - User journeys tested (max 13): from tests/ui/test_smoke.py
   Coverage % = tested / total × 100
5. Score test quality (0–100) across: positive coverage, negative coverage, BVA, naming, markers, parametrize
6. Write full results to tests/pipeline/test-results.json
7. Show formatted report with pass/fail counts, coverage %, quality score, top recommendations

If backend is offline, clearly state that API and smoke tests were skipped.
