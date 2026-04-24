---
description: Run tests, generate Allure HTML report and open it in browser via HTTP server
---

Generate an Allure HTML report from the pytest suite and open it in the browser.

Steps:
1. Kill any old server on port 4040: pkill -f "http.server 4040" 2>/dev/null || true

2. Run pytest collecting Allure results:
   cd c:/Users/Dmitrii/projects/intern-assignment-03-test
   rm -rf tests/allure-results
   python -m pytest tests/ --alluredir=tests/allure-results --tb=short -q
   (If backend offline at http://localhost:8080/health — run only tests/unit/ instead)

3. Write tests/allure-results/environment.properties:
   Backend.URL=http://localhost:8080
   Project=Product Catalog API
   Test.Layers=unit,api,ui
   ISTQB.Techniques=EP,BVA,Decision Table,State Transition

4. Write tests/allure-results/categories.json with failure categories:
   Infrastructure failures (broken + ConnectionError), Assertion failures (failed + AssertionError), API contract violations (failed + KeyError)

5. Generate HTML report:
   ALLURE_BIN="$TEMP/allure-dist/allure-2.30.0/bin/allure"
   rm -rf tests/allure-report
   "$ALLURE_BIN" generate tests/allure-results --output tests/allure-report --clean

6. Start HTTP server (required — file:// causes CORS "loading" issue):
   cd tests/allure-report && python -m http.server 4040 --bind 127.0.0.1 > /dev/null 2>&1 &
   sleep 1

7. Open in browser:
   python -c "import webbrowser; webbrowser.open('http://127.0.0.1:4040/index.html')"

Show summary: total/passed/failed/skipped, report URL http://127.0.0.1:4040.
