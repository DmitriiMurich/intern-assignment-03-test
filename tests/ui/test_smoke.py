"""
Smoke tests — critical user journeys.

These are the first tests to run in CI after deployment.
Each test maps to a user-visible scenario from testing-spec.md.
Failure here means the feature is broken for real users.

ISTQB: Smoke / Sanity testing
"""
from __future__ import annotations

import pytest
import httpx

from tests.helpers.assertions import (
    assert_ok,
    assert_catalog_page_shape,
    assert_product_details_shape,
    assert_pagination_consistency,
)


@pytest.mark.ui
@pytest.mark.smoke
class TestCriticalUserJourneys:

    def test_server_is_alive(self, smoke_client: httpx.Client):
        """Journey: app opens → backend must respond."""
        assert_ok(smoke_client.get("/health"))

    def test_catalog_loads_on_startup(self, smoke_client: httpx.Client):
        """Journey: user opens the app → catalog list is shown."""
        response = smoke_client.get("/api/v1/catalog")
        assert_ok(response)
        body = response.json()
        assert_catalog_page_shape(body)
        assert body["totalProducts"] > 0, "Catalog must have products on startup"
        assert len(body["products"]) > 0

    def test_user_can_search_products(self, smoke_client: httpx.Client):
        """Journey: user types in the search bar → filtered results appear."""
        response = smoke_client.get("/api/v1/catalog", params={"query": "a"})
        assert_ok(response)
        assert_catalog_page_shape(response.json())

    def test_user_can_switch_language(self, smoke_client: httpx.Client):
        """Journey: user selects Russian → catalog titles returned in Russian."""
        response = smoke_client.get("/api/v1/catalog", params={"lang": "ru"})
        assert_ok(response)
        assert response.json()["language"] == "ru"

    def test_user_can_switch_currency(self, smoke_client: httpx.Client):
        """Journey: user selects EUR → prices shown in EUR."""
        response = smoke_client.get("/api/v1/catalog", params={"currency": "EUR"})
        assert_ok(response)
        body = response.json()
        assert body["currency"] == "EUR"
        for product in body["products"]:
            assert product["price"]["currency"] == "EUR"

    def test_user_can_view_product_details(self, smoke_client: httpx.Client):
        """Journey: user taps a product → detail screen opens with reviews."""
        products = smoke_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["products"]
        if not products:
            pytest.skip("No products in catalog")

        pid = products[0]["id"]
        response = smoke_client.get(f"/api/v1/catalog/{pid}")
        assert_ok(response)
        assert_product_details_shape(response.json())

    def test_user_can_paginate(self, smoke_client: httpx.Client):
        """Journey: user scrolls to bottom → next page loads."""
        body = smoke_client.get("/api/v1/catalog", params={"pageSize": 5}).json()
        if body["totalPages"] < 2:
            pytest.skip("Not enough products for pagination")

        page2 = smoke_client.get("/api/v1/catalog", params={"page": 2, "pageSize": 5}).json()
        assert_ok(smoke_client.get("/api/v1/catalog", params={"page": 2, "pageSize": 5}))
        assert page2["page"] == 2
        assert len(page2["products"]) > 0

    def test_user_can_filter_by_category(self, smoke_client: httpx.Client):
        """Journey: user taps a category chip → only matching products shown."""
        categories = smoke_client.get("/api/v1/catalog").json()["categories"]
        real_categories = [c for c in categories if c["slug"]]
        if not real_categories:
            pytest.skip("No categories available")

        slug = real_categories[0]["slug"]
        response = smoke_client.get("/api/v1/catalog", params={"category": slug})
        assert_ok(response)

    def test_sort_by_price_ascending_journey(self, smoke_client: httpx.Client):
        """Journey: user selects 'Price: Low to High' sort → products reordered."""
        products = smoke_client.get(
            "/api/v1/catalog", params={"sort": "price_asc", "pageSize": 20}
        ).json()["products"]
        prices = [p["price"]["amount"] for p in products]
        assert prices == sorted(prices), "Products not sorted by price ascending"

    def test_languages_and_currencies_endpoints_work(self, smoke_client: httpx.Client):
        """Journey: app bootstraps → fetches supported languages and currencies."""
        langs = smoke_client.get("/api/v1/languages").json()["languages"]
        currencies = smoke_client.get("/api/v1/currencies").json()["currencies"]
        assert len(langs) == 10
        assert len(currencies) == 10

    def test_product_details_with_localization(self, smoke_client: httpx.Client):
        """Journey: user views product details in non-English language."""
        products = smoke_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["products"]
        if not products:
            pytest.skip("No products")
        pid = products[0]["id"]

        response = smoke_client.get(f"/api/v1/catalog/{pid}", params={"lang": "de", "currency": "EUR"})
        assert_ok(response)
        body = response.json()
        assert body["product"]["price"]["currency"] == "EUR"
