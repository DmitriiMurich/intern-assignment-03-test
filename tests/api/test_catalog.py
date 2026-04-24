"""
API tests — /api/v1/catalog endpoint (live server).

Real response shape:
{
  language, currency, categories,
  items: [{ id, title, description, price, rating, imageUrl, category }],
  meta: { totalProducts, totalCategories, currentPage, pageSize, totalPages,
          query, category, sort, sourceLanguage, sourceCurrency, exchangeRateProvider }
}
"""
from __future__ import annotations

import allure
import httpx
import pytest

from tests.helpers.assertions import (
    assert_ok,
    assert_status,
    assert_catalog_page_shape,
    assert_pagination_consistency,
    assert_error_shape,
)


def _items(response: httpx.Response) -> list:
    return response.json()["items"]


def _meta(response: httpx.Response) -> dict:
    return response.json()["meta"]


@allure.feature("Catalog")
@allure.story("Product Listing")
@pytest.mark.api
@pytest.mark.positive
class TestCatalogEndpointPositive:

    @allure.title("Default request returns valid catalog structure")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_no_params_returns_catalog(self, api_client: httpx.Client):
        with allure.step("GET /api/v1/catalog without params"):
            response = api_client.get("/api/v1/catalog")
        assert_ok(response)
        assert_catalog_page_shape(response.json())

    @allure.title("Default language is English")
    @allure.severity(allure.severity_level.NORMAL)
    def test_default_language_is_english(self, api_client: httpx.Client):
        assert api_client.get("/api/v1/catalog").json()["language"] == "en"

    @allure.title("Default currency is USD")
    @allure.severity(allure.severity_level.NORMAL)
    def test_default_currency_is_usd(self, api_client: httpx.Client):
        assert api_client.get("/api/v1/catalog").json()["currency"] == "USD"

    @allure.title("Default page number is 1")
    @allure.severity(allure.severity_level.NORMAL)
    def test_default_page_is_1(self, api_client: httpx.Client):
        assert _meta(api_client.get("/api/v1/catalog"))["currentPage"] == 1

    @allure.title("Catalog returns products after startup")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_catalog_has_products(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog").json()
        assert body["meta"]["totalProducts"] > 0
        assert len(body["items"]) > 0

    @allure.title("Pagination metadata is internally consistent (ISTQB BVA)")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_pagination_consistency_on_first_page(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"page": 1, "pageSize": 10})
        assert_pagination_consistency(response.json())

    @allure.title("Search query is echoed in meta.query")
    @allure.severity(allure.severity_level.NORMAL)
    def test_search_query_echoed_in_response(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/catalog", params={"query": "laptop"}).json()
        assert body["meta"]["query"] == "laptop"

    @allure.title("Search query narrows down totalProducts")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_search_query_filters_results(self, api_client: httpx.Client):
        all_total = api_client.get("/api/v1/catalog").json()["meta"]["totalProducts"]
        filtered_total = api_client.get(
            "/api/v1/catalog", params={"query": "phone"}
        ).json()["meta"]["totalProducts"]
        assert filtered_total <= all_total

    @allure.title("Category filter returns only matching products")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_category_filter_returns_only_matching_products(self, api_client: httpx.Client):
        categories = api_client.get("/api/v1/catalog").json()["categories"]
        if not categories:
            pytest.skip("No categories available")
        slug = next((c["slug"] for c in categories if c["slug"]), None)
        if not slug:
            pytest.skip("No non-empty category slug")
        items = api_client.get("/api/v1/catalog", params={"category": slug}).json()["items"]
        for product in items:
            assert product["category"]["slug"] == slug

    @allure.title("Sort option '{sort}' returns HTTP 200")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("sort", ["price_asc", "price_desc", "rating_desc"])
    def test_sort_options_return_200(self, api_client: httpx.Client, sort: str):
        assert_ok(api_client.get("/api/v1/catalog", params={"sort": sort}))

    @allure.title("Sort option is echoed in meta.sort")
    @allure.severity(allure.severity_level.NORMAL)
    def test_sort_echoed_in_meta(self, api_client: httpx.Client):
        for sort in ("price_asc", "price_desc", "rating_desc"):
            meta = _meta(api_client.get("/api/v1/catalog", params={"sort": sort}))
            assert meta["sort"] == sort

    @allure.title("Products are ordered by price ascending")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_price_asc_ordering(self, api_client: httpx.Client):
        items = api_client.get(
            "/api/v1/catalog", params={"sort": "price_asc", "pageSize": 20}
        ).json()["items"]
        prices = [p["price"]["amount"] for p in items]
        assert prices == sorted(prices)

    @allure.title("Products are ordered by price descending")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_price_desc_ordering(self, api_client: httpx.Client):
        items = api_client.get(
            "/api/v1/catalog", params={"sort": "price_desc", "pageSize": 20}
        ).json()["items"]
        prices = [p["price"]["amount"] for p in items]
        assert prices == sorted(prices, reverse=True)

    @allure.title("Products are ordered by rating descending")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_rating_desc_ordering(self, api_client: httpx.Client):
        items = api_client.get(
            "/api/v1/catalog", params={"sort": "rating_desc", "pageSize": 20}
        ).json()["items"]
        ratings = [p["rating"] for p in items]
        assert ratings == sorted(ratings, reverse=True)

    @allure.title("Language '{lang}' returns HTTP 200")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("lang", ["en", "ru", "de", "fr", "es", "it", "pt", "tr", "uk", "zh"])
    def test_all_languages_return_200(self, api_client: httpx.Client, lang: str):
        response = api_client.get("/api/v1/catalog", params={"lang": lang})
        assert_ok(response)
        assert response.json()["language"] == lang

    @allure.title("USD currency returns HTTP 200")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_usd_currency_returns_200(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"currency": "USD"})
        assert_ok(response)
        assert response.json()["currency"] == "USD"

    @allure.title("Non-USD currencies return 200 or 503 (exchange rates may be loading)")
    @allure.severity(allure.severity_level.NORMAL)
    def test_non_usd_currency_returns_200_or_503(self, api_client: httpx.Client):
        for currency in ["EUR", "RUB", "GBP"]:
            response = api_client.get("/api/v1/catalog", params={"currency": currency})
            assert response.status_code in (200, 503)

    @allure.title("Page 2 contains different products than page 1")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_page_2_returns_different_products(self, api_client: httpx.Client):
        page1 = api_client.get("/api/v1/catalog", params={"page": 1, "pageSize": 10}).json()
        if page1["meta"]["totalPages"] < 2:
            pytest.skip("Not enough products for page 2")
        page2 = api_client.get("/api/v1/catalog", params={"page": 2, "pageSize": 10}).json()
        ids_p1 = {p["id"] for p in page1["items"]}
        ids_p2 = {p["id"] for p in page2["items"]}
        assert not (ids_p1 & ids_p2)

    @allure.title("Nonsense search query returns zero products")
    @allure.severity(allure.severity_level.NORMAL)
    def test_gobbledegook_search_returns_empty(self, api_client: httpx.Client):
        body = api_client.get(
            "/api/v1/catalog", params={"query": "xyzzy_no_such_product_12345"}
        ).json()
        assert body["meta"]["totalProducts"] == 0 and body["items"] == []


@allure.feature("Catalog")
@allure.story("Input Validation")
@pytest.mark.api
@pytest.mark.negative
class TestCatalogEndpointNegative:

    @allure.title("Invalid language code returns HTTP 400")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_invalid_language_returns_400(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"lang": "xx"})
        assert_status(response, 400)
        assert_error_shape(response.json())

    @allure.title("Invalid currency code returns HTTP 400")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_invalid_currency_returns_400(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog", params={"currency": "XYZ"})
        assert_status(response, 400)
        assert_error_shape(response.json())

    @allure.title("Invalid sort option returns HTTP 400")
    @allure.severity(allure.severity_level.NORMAL)
    def test_invalid_sort_returns_400(self, api_client: httpx.Client):
        assert_status(api_client.get("/api/v1/catalog", params={"sort": "most_expensive"}), 400)

    @allure.title("Page=0 returns HTTP 400 (ISTQB BVA: below minimum)")
    @allure.severity(allure.severity_level.NORMAL)
    def test_page_0_returns_400(self, api_client: httpx.Client):
        assert_status(api_client.get("/api/v1/catalog", params={"page": 0}), 400)

    @allure.title("Huge pageSize returns 200 (capped) or 400")
    @allure.severity(allure.severity_level.MINOR)
    def test_page_size_exceeds_maximum_returns_400_or_capped(self, api_client: httpx.Client):
        assert api_client.get(
            "/api/v1/catalog", params={"pageSize": 999999}
        ).status_code in (200, 400)


@allure.feature("Catalog")
@allure.story("Pagination Boundaries")
@pytest.mark.api
@pytest.mark.boundary
class TestCatalogBoundaryValues:

    @allure.title("Page=1 is accepted (minimum valid boundary)")
    @allure.severity(allure.severity_level.NORMAL)
    def test_page_1_is_valid_minimum(self, api_client: httpx.Client):
        assert_ok(api_client.get("/api/v1/catalog", params={"page": 1}))

    @allure.title("pageSize=1 returns at most 1 product")
    @allure.severity(allure.severity_level.NORMAL)
    def test_page_size_1_returns_single_product(self, api_client: httpx.Client):
        assert len(api_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["items"]) <= 1

    @allure.title("Page beyond totalPages returns empty list or HTTP 400")
    @allure.severity(allure.severity_level.NORMAL)
    def test_page_beyond_last_returns_empty_or_400(self, api_client: httpx.Client):
        total_pages = api_client.get("/api/v1/catalog").json()["meta"]["totalPages"]
        response = api_client.get("/api/v1/catalog", params={"page": total_pages + 100})
        if response.status_code == 200:
            assert response.json()["items"] == []
        else:
            assert_status(response, 400)
