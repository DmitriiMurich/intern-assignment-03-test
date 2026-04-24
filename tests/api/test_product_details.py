"""API tests — /api/v1/catalog/:productId endpoint (live server)."""
from __future__ import annotations

import allure
import httpx
import pytest

from tests.helpers.assertions import (
    assert_ok, assert_status, assert_product_details_shape,
    assert_error_shape, assert_review_shape,
)


def _get_first_product_id(api_client: httpx.Client) -> str:
    items = api_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["items"]
    if not items:
        pytest.skip("No products available in catalog")
    return items[0]["id"]


@allure.feature("Catalog")
@allure.story("Product Details")
@pytest.mark.api
@pytest.mark.positive
class TestProductDetailsPositive:

    @allure.title("Valid product ID returns HTTP 200")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_valid_product_returns_200(self, api_client: httpx.Client):
        assert_ok(api_client.get(f"/api/v1/catalog/{_get_first_product_id(api_client)}"))

    @allure.title("Product details response has correct shape")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_response_has_correct_shape(self, api_client: httpx.Client):
        assert_product_details_shape(
            api_client.get(f"/api/v1/catalog/{_get_first_product_id(api_client)}").json()
        )

    @allure.title("Product ID in response matches requested ID")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_product_id_in_response_matches_request(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        assert api_client.get(f"/api/v1/catalog/{pid}").json()["product"]["id"] == pid

    @allure.title("Product details includes reviews array")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_reviews_array_is_present(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        assert isinstance(api_client.get(f"/api/v1/catalog/{pid}").json()["reviews"], list)

    @allure.title("Each review has all required fields")
    @allure.severity(allure.severity_level.NORMAL)
    def test_each_review_has_required_fields(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        for review in api_client.get(f"/api/v1/catalog/{pid}").json()["reviews"]:
            assert_review_shape(review)

    @allure.title("Response includes meta object")
    @allure.severity(allure.severity_level.MINOR)
    def test_response_has_meta(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        assert "meta" in api_client.get(f"/api/v1/catalog/{pid}").json()

    @allure.title("Language '{lang}' accepted in product details")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("lang", ["en", "ru", "de"])
    def test_language_param_accepted(self, api_client: httpx.Client, lang: str):
        pid = _get_first_product_id(api_client)
        assert_ok(api_client.get(f"/api/v1/catalog/{pid}", params={"lang": lang}))

    @allure.title("USD currency param works in product details")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_usd_currency_works(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        response = api_client.get(f"/api/v1/catalog/{pid}", params={"currency": "USD"})
        assert_ok(response)
        assert response.json()["product"]["price"]["currency"] == "USD"

    @allure.title("Non-USD currency returns 200 or 503")
    @allure.severity(allure.severity_level.NORMAL)
    def test_non_usd_currency_returns_200_or_503(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        assert api_client.get(
            f"/api/v1/catalog/{pid}", params={"currency": "EUR"}
        ).status_code in (200, 503)

    @allure.title("Product price is positive")
    @allure.severity(allure.severity_level.NORMAL)
    def test_price_is_positive(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        price = api_client.get(f"/api/v1/catalog/{pid}").json()["product"]["price"]["amount"]
        assert price > 0

    @allure.title("Product rating is in range [0, 5]")
    @allure.severity(allure.severity_level.NORMAL)
    def test_rating_is_in_valid_range(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        rating = api_client.get(f"/api/v1/catalog/{pid}").json()["product"]["rating"]
        assert 0 <= rating <= 5


@allure.feature("Catalog")
@allure.story("Product Details — Error Handling")
@pytest.mark.api
@pytest.mark.negative
class TestProductDetailsNegative:

    @allure.title("Non-existent product ID returns HTTP 404")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_nonexistent_product_returns_404(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog/999999999")
        assert_status(response, 404)
        assert_error_shape(response.json())

    @allure.title("Non-numeric product ID returns 400 or 404")
    @allure.severity(allure.severity_level.NORMAL)
    def test_non_numeric_id_returns_400_or_404(self, api_client: httpx.Client):
        assert api_client.get("/api/v1/catalog/not-a-number").status_code in (400, 404)

    @allure.title("Invalid language code returns HTTP 400")
    @allure.severity(allure.severity_level.NORMAL)
    def test_invalid_language_returns_400(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        assert_status(api_client.get(f"/api/v1/catalog/{pid}", params={"lang": "zz"}), 400)

    @allure.title("Invalid currency code returns HTTP 400")
    @allure.severity(allure.severity_level.NORMAL)
    def test_invalid_currency_returns_400(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        assert_status(api_client.get(f"/api/v1/catalog/{pid}", params={"currency": "FAKE"}), 400)

    @allure.title("Negative product ID returns 400 or 404 (ISTQB BVA)")
    @allure.severity(allure.severity_level.MINOR)
    def test_negative_product_id_returns_400_or_404(self, api_client: httpx.Client):
        assert api_client.get("/api/v1/catalog/-1").status_code in (400, 404)

    @allure.title("Zero product ID returns 400 or 404 (ISTQB BVA: min boundary)")
    @allure.severity(allure.severity_level.MINOR)
    def test_zero_product_id_returns_400_or_404(self, api_client: httpx.Client):
        assert api_client.get("/api/v1/catalog/0").status_code in (400, 404)
