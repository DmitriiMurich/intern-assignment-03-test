"""
API tests — /api/v1/catalog/:productId endpoint (live server).
"""
from __future__ import annotations

import pytest
import httpx

from tests.helpers.assertions import (
    assert_ok,
    assert_status,
    assert_product_details_shape,
    assert_error_shape,
    assert_review_shape,
)


def _get_first_product_id(api_client: httpx.Client) -> str:
    products = api_client.get("/api/v1/catalog", params={"pageSize": 1}).json()["products"]
    if not products:
        pytest.skip("No products available in catalog")
    return products[0]["id"]


@pytest.mark.api
@pytest.mark.positive
class TestProductDetailsPositive:

    def test_valid_product_returns_200(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        response = api_client.get(f"/api/v1/catalog/{pid}")
        assert_ok(response)

    def test_response_has_correct_shape(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        body = api_client.get(f"/api/v1/catalog/{pid}").json()
        assert_product_details_shape(body)

    def test_product_id_in_response_matches_request(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        body = api_client.get(f"/api/v1/catalog/{pid}").json()
        assert body["product"]["id"] == pid

    def test_reviews_array_is_present(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        reviews = api_client.get(f"/api/v1/catalog/{pid}").json()["reviews"]
        assert isinstance(reviews, list)

    def test_each_review_has_required_fields(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        reviews = api_client.get(f"/api/v1/catalog/{pid}").json()["reviews"]
        for review in reviews:
            assert_review_shape(review)

    @pytest.mark.parametrize("lang", ["en", "ru", "de"])
    def test_language_param_accepted(self, api_client: httpx.Client, lang: str):
        pid = _get_first_product_id(api_client)
        response = api_client.get(f"/api/v1/catalog/{pid}", params={"lang": lang})
        assert_ok(response)

    @pytest.mark.parametrize("currency", ["USD", "EUR", "GBP"])
    def test_currency_param_accepted(self, api_client: httpx.Client, currency: str):
        pid = _get_first_product_id(api_client)
        response = api_client.get(f"/api/v1/catalog/{pid}", params={"currency": currency})
        assert_ok(response)
        body = response.json()
        assert body["product"]["price"]["currency"] == currency

    def test_price_is_positive(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        price = api_client.get(f"/api/v1/catalog/{pid}").json()["product"]["price"]["amount"]
        assert price > 0, f"Product price must be positive, got: {price}"

    def test_rating_is_in_valid_range(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        rating = api_client.get(f"/api/v1/catalog/{pid}").json()["product"]["rating"]
        assert 0 <= rating <= 5, f"Rating out of [0,5]: {rating}"


@pytest.mark.api
@pytest.mark.negative
class TestProductDetailsNegative:

    def test_nonexistent_product_returns_404(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog/999999999")
        assert_status(response, 404)
        assert_error_shape(response.json())

    def test_non_numeric_id_returns_400_or_404(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog/not-a-number")
        assert response.status_code in (400, 404), (
            f"Expected 400 or 404 for non-numeric ID, got {response.status_code}"
        )

    def test_invalid_language_returns_400(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        response = api_client.get(f"/api/v1/catalog/{pid}", params={"lang": "zz"})
        assert_status(response, 400)

    def test_invalid_currency_returns_400(self, api_client: httpx.Client):
        pid = _get_first_product_id(api_client)
        response = api_client.get(f"/api/v1/catalog/{pid}", params={"currency": "FAKE"})
        assert_status(response, 400)

    def test_negative_product_id_returns_404(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog/-1")
        assert response.status_code in (400, 404)

    def test_zero_product_id_returns_404(self, api_client: httpx.Client):
        response = api_client.get("/api/v1/catalog/0")
        assert response.status_code in (400, 404)
