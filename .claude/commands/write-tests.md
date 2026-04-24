---
description: Analyze the project and write comprehensive pytest tests (ISTQB, Allure decorators)
---

Analyze this project's codebase and generate comprehensive pytest tests.

Steps:
1. Read backend/src/ to understand all endpoints, services, and error codes
2. Read existing tests in tests/ to find coverage gaps
3. Write new tests using ISTQB techniques: Equivalence Partitioning, Boundary Value Analysis, Decision Table
4. Cover all three layers:
   - tests/unit/ — mocked with respx, no live server needed
   - tests/api/ — against live server (auto-skip if unreachable)
   - tests/ui/ — smoke tests mapping to user journeys in docs/testing-spec.md
5. Every test must have markers: @pytest.mark.unit/api/ui AND @pytest.mark.positive/negative/boundary
6. Every test must have Allure decorators: @allure.feature, @allure.story, @allure.title, @allure.severity
7. Use factories from tests/helpers/factories.py for test data
8. Use assertions from tests/helpers/assertions.py
9. Update tests/pipeline/project-analysis.json when done

Show a summary table of tests added per file when done.
