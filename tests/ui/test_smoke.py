"""Smoke tests — critical user journeys (ISTQB: System Testing)."""
from __future__ import annotations

import allure
import httpx
import pytest

from tests.helpers.assertions import (
    assert_ok, assert_catalog_page_shape,
    assert_product_details_shape, assert_pagination_consistency,
)


@allure.feature("End-to-End")
@allure.story("Critical User Journeys")
@pytest.mark.ui
@pytest.mark.smoke
class TestCriticalUserJourneys:

    @allure.title("Server responds to health check")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_server_is_alive(self, smoke_client: httpx.Client):
        assert_ok(smoke_client.get("/health"))

    @allure.title("Catalog loads on app startup")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_catalog_loads_on_startup(self, smoke_client: httpx.Client):
        response = smoke_client.get("/api/v1/catalog")
        assert_ok(response)
        body = response.json()
        assert_catalog_page_shape(body)
        assert body["meta"]["totalProducts"] > 0 and len(body["items"]) > 0

    @allure.title("User can search products")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_can_search_products(self, smoke_client: httpx.Client):
        response = smoke_client.get("/api/v1/catalog", params={"query": "a"})
        assert_ok(response)
        assert_catalog_page_shape(response.json())

    @allure.title("User can switch language to Russian")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_can_switch_language(self, smoke_client: httpx.Client):
        response = smoke_client.get("/api/v1/catalog", params={"lang": "ru"})
        assert_ok(response)
        assert response.json()["language"] == "ru"

    @allure.title("USD prices shown correctly for all products")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_usd_currency_works(self, smoke_client: httpx.Client):
        response = smoke_client.get("/api/v1/catalog", params={"currency": "USD"})
        assert_ok(response)
        body = response.json()
        assert body["currency"] == "USD"
        for product in body["items"]:
            assert product["price"]["currency"] == "USD"

    @allure.title("User can view product details with reviews")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_user_can_view_product_details(self, smoke_client: httpx.Client):
        items = smoke_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["items"]
        if not items:
            pytest.skip("No products")
        response = smoke_client.get(f"/api/v1/catalog/{items[0]['id']}")
        assert_ok(response)
        assert_product_details_shape(response.json())

    @allure.title("User can paginate to page 2")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_can_paginate(self, smoke_client: httpx.Client):
        body = smoke_client.get("/api/v1/catalog", params={"pageSize": 5}).json()
        if body["meta"]["totalPages"] < 2:
            pytest.skip("Not enough products")
        page2 = smoke_client.get("/api/v1/catalog", params={"page": 2, "pageSize": 5})
        assert_ok(page2)
        assert page2.json()["meta"]["currentPage"] == 2

    @allure.title("User can filter by category")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_can_filter_by_category(self, smoke_client: httpx.Client):
        categories = [c for c in smoke_client.get("/api/v1/catalog").json()["categories"] if c["slug"]]
        if not categories:
            pytest.skip("No categories")
        assert_ok(smoke_client.get("/api/v1/catalog", params={"category": categories[0]["slug"]}))

    @allure.title("Products are sorted by price ascending")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_sort_by_price_ascending(self, smoke_client: httpx.Client):
        items = smoke_client.get(
            "/api/v1/catalog", params={"sort": "price_asc", "pageSize": 20}
        ).json()["items"]
        prices = [p["price"]["amount"] for p in items]
        assert prices == sorted(prices)

    @allure.title("Languages endpoint returns 10 languages")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_languages_endpoint_works(self, smoke_client: httpx.Client):
        assert len(smoke_client.get("/api/v1/languages").json()["items"]) == 10

    @allure.title("Currencies endpoint returns 10 currencies")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_currencies_endpoint_works(self, smoke_client: httpx.Client):
        assert len(smoke_client.get("/api/v1/currencies").json()["items"]) == 10

    @allure.title("Product details available in German with language field")
    @allure.severity(allure.severity_level.NORMAL)
    def test_product_details_with_language(self, smoke_client: httpx.Client):
        items = smoke_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["items"]
        if not items:
            pytest.skip("No products")
        response = smoke_client.get(f"/api/v1/catalog/{items[0]['id']}", params={"lang": "de"})
        assert_ok(response)
        assert response.json()["language"] == "de"

    @allure.title("Nonsense search returns zero results")
    @allure.severity(allure.severity_level.NORMAL)
    def test_gobbledegook_search_returns_empty(self, smoke_client: httpx.Client):
        body = smoke_client.get(
            "/api/v1/catalog", params={"query": "xyzzy_no_such_product_12345"}
        ).json()
        assert body["meta"]["totalProducts"] == 0 and body["items"] == []
