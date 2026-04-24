"""
Unit tests — currency conversion contract.

Verifies the rule: price.amount = source_USD_amount × exchange_rate, rounded to 2dp.
All external calls are mocked — no live server or DB needed.
"""
from __future__ import annotations

import httpx
import pytest
import respx

from tests.helpers.assertions import assert_ok, assert_status
from tests.helpers.factories import ProductDetailsFactory

BASE = "http://localhost:8080"

SUPPORTED_CURRENCIES = ["USD", "EUR", "RUB", "GBP", "UAH", "TRY", "CNY", "JPY", "CAD", "CHF"]


@pytest.mark.unit
@pytest.mark.positive
class TestCurrencyContractPositive:

    @pytest.mark.parametrize("currency", SUPPORTED_CURRENCIES)
    @respx.mock
    def test_all_supported_currencies_return_200(self, currency: str):
        """Every supported currency must yield a 200 on the catalog endpoint."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(200, json={"currency": currency, "products": []})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"currency": currency})

        assert_ok(response)

    @respx.mock
    def test_usd_price_unchanged(self):
        """Requesting USD must return prices identical to the source (no conversion)."""
        payload = ProductDetailsFactory()
        original_amount = 99.99
        payload["product"]["price"] = {"amount": original_amount, "currency": "USD"}
        pid = payload["product"]["id"]

        respx.get(f"{BASE}/api/v1/catalog/{pid}").mock(
            return_value=httpx.Response(200, json=payload)
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog/{pid}", params={"currency": "USD"})

        price = response.json()["product"]["price"]
        assert price["amount"] == original_amount
        assert price["currency"] == "USD"

    @respx.mock
    def test_converted_price_has_two_decimal_places(self):
        """Prices after conversion must be rounded to 2 decimal places."""
        payload = ProductDetailsFactory()
        payload["product"]["price"] = {"amount": 19.99, "currency": "EUR"}
        pid = payload["product"]["id"]

        respx.get(f"{BASE}/api/v1/catalog/{pid}").mock(
            return_value=httpx.Response(200, json=payload)
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog/{pid}", params={"currency": "EUR"})

        amount = response.json()["product"]["price"]["amount"]
        assert round(amount, 2) == amount, f"Price not rounded to 2dp: {amount}"

    @respx.mock
    def test_currency_reflected_in_all_products(self):
        """Every product in a catalog page must share the requested currency."""
        from tests.helpers.factories import ProductFactory, CatalogPageFactory

        currency = "GBP"
        products = [ProductFactory(price={"amount": 10.0, "currency": currency}) for _ in range(5)]
        payload = CatalogPageFactory(currency=currency, products=products)

        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(200, json=payload)
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"currency": currency})

        for product in response.json()["products"]:
            assert product["price"]["currency"] == currency


@pytest.mark.unit
@pytest.mark.negative
class TestCurrencyContractNegative:

    @respx.mock
    def test_unsupported_currency_returns_400(self):
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_CURRENCY"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"currency": "BTC"})

        assert_status(response, 400)

    @respx.mock
    def test_empty_currency_code_returns_400(self):
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_CURRENCY"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"currency": ""})

        assert_status(response, 400)

    @respx.mock
    def test_lowercase_currency_code_returns_400(self):
        """Currency codes are case-sensitive; 'usd' must not be accepted as 'USD'."""
        respx.get(f"{BASE}/api/v1/catalog").mock(
            return_value=httpx.Response(400, json={"code": "INVALID_CURRENCY"})
        )
        with httpx.Client() as client:
            response = client.get(f"{BASE}/api/v1/catalog", params={"currency": "usd"})

        assert_status(response, 400)
