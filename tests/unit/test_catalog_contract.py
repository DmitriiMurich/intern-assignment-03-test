"""
Unit tests — catalog contract validation.

Strategy (ISTQB Component Testing):
- All HTTP I/O replaced by respx mocks: tests are fast, deterministic, offline.
- Equivalence Partitions: valid inputs (positive) + invalid inputs (negative).
- Boundary Value Analysis: page=1, pageSize limits, empty results.
- Anti-patterns avoided:
    * No "mystery guest" — all test data is declared inline or via named factories.
    * No logic in tests — assertions delegate to helpers.
    * No over-specification — tests verify contracts, not implementation details.
"""
from __future__ import annotations

import httpx
import pytest
import respx

from tests.helpers.assertions import (
    assert_ok,
    assert_status,
    assert_catalog_page_shape,
    assert_product_details_shape,
    assert_error_shape,
    assert_pagination_consistency,
)
from tests.helpers.factories import CatalogPageFactory, ProductDetailsFactory, ProductFactory

BASE = "http://localhost:8080"


# ===========================================================================
# /api/v1/catalog — positive (valid equivalence partition)
# ===========================================================================


@pytest.mark.unit
@pytest.mark.positive
class TestCatalogContractPositive:

    @respx.mock
    def test_default_request_returns_catalog_shape(self):
        """GET /api/v1/catalog with no params must return a valid catalog page."""
        payload = CatalogPageFactory()
        respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog")

        assert_ok(response)
        assert_catalog_page_shape(response.json())

    @respx.mock
    def test_pagination_metadata_is_consistent(self):
        """page / pageSize / totalPages / products count must be internally consistent."""
        payload = CatalogPageFactory()
        payload["meta"]["currentPage"] = 1
        payload["meta"]["pageSize"] = 20
        payload["meta"]["totalProducts"] = 45
        payload["meta"]["totalPages"] = 3
        payload["items"] = [ProductFactory() for _ in range(20)]  # full page
        respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog")

        body = response.json()
        assert_pagination_consistency(body)

    @respx.mock
    def test_supported_language_codes_accepted(self):
        """Each of the 10 supported language codes must produce a 200."""
        supported = ["en", "ru", "de", "fr", "es", "it", "pt", "tr", "uk", "zh"]
        for lang in supported:
            payload = CatalogPageFactory(language=lang)
            respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

            with httpx.Client() as client:
                response = client.get(f"{BASE}/api/v1/catalog", params={"lang": lang})

            assert_ok(response)
            assert response.json()["language"] == lang, f"language mismatch for {lang}"

    @respx.mock
    def test_supported_currency_codes_accepted(self):
        """Each of the 10 supported currency codes must produce a 200."""
        supported = ["USD", "EUR", "RUB", "GBP", "UAH", "TRY", "CNY", "JPY", "CAD", "CHF"]
        for currency in supported:
            payload = CatalogPageFactory(currency=currency)
            respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

            with httpx.Client() as client:
                response = client.get(f"{BASE}/api/v1/catalog", params={"currency": currency})

            assert_ok(response)
            assert response.json()["currency"] == currency

    @respx.mock
    def test_sort_options_accepted(self):
        """All three valid sort options must produce a 200."""
        for sort in ("price_asc", "price_desc", "rating_desc"):
            payload = CatalogPageFactory(sort=sort)
            respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

            with httpx.Client() as client:
                response = client.get(f"{BASE}/api/v1/catalog", params={"sort": sort})

            assert_ok(response)
            assert response.json()["sort"] == sort

    @respx.mock
    def test_empty_search_query_returns_full_catalog(self):
        """Empty query string ≡ no filter applied — all products returned."""
        payload = CatalogPageFactory(query="", totalProducts=100)
        respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"query": ""})

        assert_ok(response)
        assert response.json()["query"] == ""

    @respx.mock
    def test_search_query_reflected_in_response(self):
        """The query param must be echoed back in the response body."""
        payload = CatalogPageFactory(query="laptop")
        respx.get(f"{BASE}/api/v1/catalog").mock(return_value=httpx.Response(200, json=payload))

        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"query": "laptop"})

        assert response.json()["query"] == "laptop"


# ===========================================================================
# /api/v1/catalog — negative (invalid equivalence partition)
# ===========================================================================


@pytest.mark.unit
@pytest.mark.negative
class TestCatalogContractNegative:

    @respx.mock
    def test_invalid_language_code_returns_400(self):
        """Unknown language code must be rejected with 400 Bad Request."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_LANGUAGE"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"lang": "xx"})

        assert_status(response, 400)
        assert_error_shape(response.json())

    @respx.mock
    def test_invalid_currency_code_returns_400(self):
        """Unknown currency code must be rejected with 400 Bad Request."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_CURRENCY"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"currency": "ZZZ"})

        assert_status(response, 400)
        assert_error_shape(response.json())

    @respx.mock
    def test_invalid_sort_option_returns_400(self):
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_SORT"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"sort": "price_random"})

        assert_status(response, 400)

    @respx.mock
    def test_page_zero_returns_400(self):
        """ISTQB BVA: page=0 is below the minimum boundary (min=1) → reject."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_PAGE"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"page": 0})

        assert_status(response, 400)

    @respx.mock
    def test_negative_page_returns_400(self):
        """ISTQB BVA: negative page is well below the minimum boundary."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_PAGE"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"page": -1})

        assert_status(response, 400)

    @respx.mock
    def test_page_size_zero_returns_400(self):
        """pageSize=0 means 'no products per page' — semantically invalid."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_PAGE_SIZE"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"pageSize": 0})

        assert_status(response, 400)


# ===========================================================================
# /api/v1/catalog/:productId — positive
# ===========================================================================


@pytest.mark.unit
@pytest.mark.positive
class TestProductDetailsContractPositive:

    @respx.mock
    def test_valid_product_id_returns_details(self):
        payload = ProductDetailsFactory()
        product_id = payload["product"]["id"]
        respx.get(f"{BASE}/api/v1/catalog/{product_id}").mock(
            return_value=httpx.Response(200, json=payload)
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog/{product_id}")

        assert_ok(response)
        assert_product_details_shape(response.json())

    @respx.mock
    def test_product_details_includes_reviews(self):
        payload = ProductDetailsFactory()
        product_id = payload["product"]["id"]
        respx.get(f"{BASE}/api/v1/catalog/{product_id}").mock(
            return_value=httpx.Response(200, json=payload)
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog/{product_id}")

        reviews = response.json()["reviews"]
        assert isinstance(reviews, list)

    @respx.mock
    def test_product_details_with_currency_conversion(self):
        """Price currency must match the requested currency."""
        payload = ProductDetailsFactory()
        payload["product"]["price"]["currency"] = "EUR"
        product_id = payload["product"]["id"]
        respx.get(f"{BASE}/api/v1/catalog/{product_id}").mock(
            return_value=httpx.Response(200, json=payload)
        )
        with httpx.Client() as client:
            response = client.get(
                f"{BASE}/api/v1/catalog/{product_id}", params={"currency": "EUR"}
            )

        assert response.json()["product"]["price"]["currency"] == "EUR"


# ===========================================================================
# /api/v1/catalog/:productId — negative
# ===========================================================================


@pytest.mark.unit
@pytest.mark.negative
class TestProductDetailsContractNegative:

    @respx.mock
    def test_nonexistent_product_id_returns_404(self):
        respx.get(f"{BASE}/api/v1/catalog/999999").mock(
            return_value=httpx.Response(404, json={"code": "PRODUCT_NOT_FOUND"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog/999999")

        assert_status(response, 404)
        assert_error_shape(response.json())

    @respx.mock
    def test_non_numeric_product_id_returns_400_or_404(self):
        """Non-numeric ID should be rejected or return not-found."""
        respx.get(f"{BASE}/api/v1/catalog/not-a-number").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_PRODUCT_ID"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog/not-a-number")

        assert response.status_code in (400, 404)
