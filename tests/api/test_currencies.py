"""
API tests — /api/v1/currencies endpoint.
"""
from __future__ import annotations

import pytest
import httpx

from tests.helpers.assertions import assert_ok, assert_json_content_type


EXPECTED_CURRENCY_CODES = {"USD", "EUR", "RUB", "GBP", "UAH", "TRY", "CNY", "JPY", "CAD", "CHF"}


@pytest.mark.api
@pytest.mark.positive
class TestCurrenciesEndpoint:

    def test_returns_200(self, api_client: httpx.Client):
        assert_ok(api_client.get("/api/v1/currencies"))

    def test_returns_json(self, api_client: httpx.Client):
        assert_json_content_type(api_client.get("/api/v1/currencies"))

    def test_response_has_currencies_array(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/currencies").json()
        assert "currencies" in body
        assert isinstance(body["currencies"], list)

    def test_returns_10_supported_currencies(self, api_client: httpx.Client):
        currencies = api_client.get("/api/v1/currencies").json()["currencies"]
        assert len(currencies) == 10, f"Expected 10 currencies, got {len(currencies)}"

    def test_each_currency_has_code_name_symbol(self, api_client: httpx.Client):
        for c in api_client.get("/api/v1/currencies").json()["currencies"]:
            assert "code" in c
            assert "name" in c
            assert "symbol" in c

    def test_all_expected_codes_present(self, api_client: httpx.Client):
        codes = {c["code"] for c in api_client.get("/api/v1/currencies").json()["currencies"]}
        missing = EXPECTED_CURRENCY_CODES - codes
        assert not missing, f"Missing currency codes: {missing}"

    def test_usd_is_present(self, api_client: httpx.Client):
        """USD is the source currency and must always be present."""
        codes = {c["code"] for c in api_client.get("/api/v1/currencies").json()["currencies"]}
        assert "USD" in codes

    def test_no_duplicate_codes(self, api_client: httpx.Client):
        codes = [c["code"] for c in api_client.get("/api/v1/currencies").json()["currencies"]]
        assert len(codes) == len(set(codes))
