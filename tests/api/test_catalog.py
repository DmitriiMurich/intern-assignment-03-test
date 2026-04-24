"""
API tests — /api/v1/catalog endpoint (live server).

Coverage matrix (ISTQB):
  Positive:  defaults, search, category filter, sort, language, currency, pagination
  Negative:  invalid lang/currency/sort/page values
  Boundary:  page=1 (min), large pageSize, empty result set
"""
from __future__ import annotations

import pytest
import httpx

from tests.helpers.assertions import (
    assert_ok,
    assert_status,
    assert_catalog_page_shape,
    assert_pagination_consistency,
    assert_error_shape,
)


@pytest.mark.api
@pytest.mark.positive
class TestCatalogEndpointPositive:

    def test_no_params_returns_catalog(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog")
        assert_ok(response)
        assert_catalog_page_shape(response.json())

    def test_default_language_is_english(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog").json()
        assert body["language"] == "en"

    def test_default_currency_is_usd(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog").json()
        assert body["currency"] == "USD"

    def test_default_page_is_1(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog").json()
        assert body["page"] == 1

    def test_pagination_consistency_on_first_page(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog", params={"page": 1, "pageSize": 10}).json()
        assert_pagination_consistency(body)

    def test_search_query_filters_results(self, api_client: httpx.Client):
        """A non-empty query must narrow down results (totalProducts ≤ unfiltered)."""
        all_body = api_client.get("/api/v1/catalog").json()
        filtered_body = api_client.get("/api/v1/catalog", params={"query": "phone"}).json()
        assert filtered_body["totalProducts"] <= all_body["totalProducts"]

    def test_search_query_echoed_in_response(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog", params={"query": "laptop"}).json()
        assert body["query"] == "laptop"

    def test_category_filter_returns_only_matching_products(self, api_client: httpx.Client):
        """If a category slug is provided, all returned products must belong to it."""
        categories_body = api_client.get("/api/v1/catalog").json()["categories"]
        if not categories_body:
            pytest.skip("No categories available")

        slug = next((c["slug"] for c in categories_body if c["slug"]), None)
        if not slug:
            pytest.skip("No non-empty category slug available")

        body = api_client.get("/api/v1/catalog", params={"category": slug}).json()
        for product in body["products"]:
            assert product["category"]["slug"] == slug, (
                f"Product {product['id']} has category {product['category']['slug']}, expected {slug}"
            )

    @pytest.mark.parametrize("sort", ["price_asc", "price_desc", "rating_desc"])
    def test_sort_options_return_200(self, api_client: httpx.Client, sort: str):
        response = api_client.get("/api/v1/catalog", params={"sort": sort})
        assert_ok(response)

    def test_price_asc_ordering(self, api_client: httpx.Client):
        """price_asc: each product's price must be ≥ the previous one."""
        products = api_client.get(
            "/api/v1/catalog", params={"sort": "price_asc", "pageSize": 20}
        ).json()["products"]
        prices = [p["price"]["amount"] for p in products]
        assert prices == sorted(prices), f"Products not sorted price_asc: {prices}"

    def test_price_desc_ordering(self, api_client: httpx.Client):
        products = api_client.get(
            "/api/v1/catalog", params={"sort": "price_desc", "pageSize": 20}
        ).json()["products"]
        prices = [p["price"]["amount"] for p in products]
        assert prices == sorted(prices, reverse=True), f"Products not sorted price_desc: {prices}"

    def test_rating_desc_ordering(self, api_client: httpx.Client):
        products = api_client.get(
            "/api/v1/catalog", params={"sort": "rating_desc", "pageSize": 20}
        ).json()["products"]
        ratings = [p["rating"] for p in products]
        assert ratings == sorted(ratings, reverse=True), f"Products not sorted by rating: {ratings}"

    @pytest.mark.parametrize("lang", ["en", "ru", "de", "fr", "es", "it", "pt", "tr", "uk", "zh"])
    def test_all_languages_return_200(self, api_client: httpx.Client, lang: str):
        response = api_client.get("/api/v1/catalog", params={"lang": lang})
        assert_ok(response)
        assert response.json()["language"] == lang

    @pytest.mark.parametrize(
        "currency", ["USD", "EUR", "RUB", "GBP", "UAH", "TRY", "CNY", "JPY", "CAD", "CHF"]
    )
    def test_all_currencies_return_200(self, api_client: httpx.Client, currency: str):
        response = api_client.get("/api/v1/catalog", params={"currency": currency})
        assert_ok(response)
        assert response.json()["currency"] == currency

    def test_page_2_returns_different_products(self, api_client: httpx.Client):
        """Page 2 must not repeat products from page 1."""
        page1 = api_client.get("/api/v1/catalog", params={"page": 1, "pageSize": 10}).json()
        if page1["totalPages"] < 2:
            pytest.skip("Not enough products for page 2")
        page2 = api_client.get("/api/v1/catalog", params={"page": 2, "pageSize": 10}).json()

        ids_p1 = {p["id"] for p in page1["products"]}
        ids_p2 = {p["id"] for p in page2["products"]}
        overlap = ids_p1 & ids_p2
        assert not overlap, f"Products repeated across pages: {overlap}"

    def test_gobbledegook_search_returns_empty(self, api_client: httpx.Client):
        """Search for a nonsense string must return 200 with zero products."""
        body = api_client.get(
            "/api/v1/catalog", params={"query": "xyzzy_no_such_product_12345"}
        ).json()
        assert_ok(api_client.get("/api/v1/catalog", params={"query": "xyzzy_no_such_product_12345"}))
        assert body["totalProducts"] == 0
        assert body["products"] == []


@pytest.mark.api
@pytest.mark.negative
class TestCatalogEndpointNegative:

    def test_invalid_language_returns_400(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"lang": "xx"})
        assert_status(response, 400)
        assert_error_shape(response.json())

    def test_invalid_currency_returns_400(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"currency": "XYZ"})
        assert_status(response, 400)
        assert_error_shape(response.json())

    def test_invalid_sort_returns_400(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"sort": "most_expensive"})
        assert_status(response, 400)

    def test_page_0_returns_400(self, api_client: httpx.Client):
        """ISTQB BVA: below minimum boundary."""
        response = api_client.get("/api/v1/catalog", params={"page": 0})
        assert_status(response, 400)

    def test_page_size_exceeds_maximum_returns_400_or_capped(self, api_client: httpx.Client):
        """ISTQB BVA: above maximum boundary. Server may cap or reject."""
        response = api_client.get("/api/v1/catalog", params={"pageSize": 999999})
        assert response.status_code in (200, 400)


@pytest.mark.api
@pytest.mark.boundary
class TestCatalogBoundaryValues:

    def test_page_1_is_valid_minimum(self, api_client: httpx.Client):
        """ISTQB BVA: minimum valid page."""
        assert_ok(api_client.get("/api/v1/catalog", params={"page": 1}))

    def test_page_size_1_returns_single_product(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog", params={"pageSize": 1}).json()
        assert len(body["products"]) <= 1

    def test_page_beyond_last_returns_empty_or_400(self, api_client: httpx.Client):
        """Requesting a page beyond totalPages must return empty products or 400."""
        total_pages = api_client.get("/api/v1/catalog").json()["totalPages"]
        response = api_client.get("/api/v1/catalog", params={"page": total_pages + 100})
        if response.status_code == 200:
            assert response.json()["products"] == []
        else:
            assert_status(response, 400)
