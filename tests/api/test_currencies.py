"""API tests — /api/v1/currencies endpoint."""
from __future__ import annotations

import allure
import httpx
import pytest

from tests.helpers.assertions import assert_ok, assert_json_content_type

EXPECTED_CURRENCY_CODES = {"USD", "EUR", "RUB", "GBP", "UAH", "TRY", "CNY", "JPY", "CAD", "CHF"}


@allure.feature("Currency")
@allure.story("Supported Currencies List")
@pytest.mark.api
@pytest.mark.positive
class TestCurrenciesEndpoint:

    @allure.title("GET /api/v1/currencies returns HTTP 200")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_returns_200(self, api_client: httpx.Client):
        assert_ok(api_client.get("/api/v1/currencies"))

    @allure.title("Currencies endpoint returns JSON")
    @allure.severity(allure.severity_level.NORMAL)
    def test_returns_json(self, api_client: httpx.Client):
        assert_json_content_type(api_client.get("/api/v1/currencies"))

    @allure.title("Response contains 'items' array")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_response_has_items_array(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/currencies").json()
        assert "items" in body and isinstance(body["items"], list)

    @allure.title("Exactly 10 supported currencies returned")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_returns_10_supported_currencies(self, api_client: httpx.Client):
        assert len(api_client.get("/api/v1/currencies").json()["items"]) == 10

    @allure.title("Each currency has code, name and symbol")
    @allure.severity(allure.severity_level.NORMAL)
    def test_each_currency_has_code_name_symbol(self, api_client: httpx.Client):
        for c in api_client.get("/api/v1/currencies").json()["items"]:
            assert "code" in c and "name" in c and "symbol" in c

    @allure.title("Each currency has isSourceCurrency flag")
    @allure.severity(allure.severity_level.MINOR)
    def test_each_currency_has_is_source_flag(self, api_client: httpx.Client):
        for c in api_client.get("/api/v1/currencies").json()["items"]:
            assert "isSourceCurrency" in c

    @allure.title("All 10 expected currency codes are present")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_all_expected_codes_present(self, api_client: httpx.Client):
        codes = {c["code"] for c in api_client.get("/api/v1/currencies").json()["items"]}
        assert not (EXPECTED_CURRENCY_CODES - codes)

    @allure.title("USD is marked as source currency")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_usd_is_marked_as_source(self, api_client: httpx.Client):
        currencies = api_client.get("/api/v1/currencies").json()["items"]
        usd = next((c for c in currencies if c["code"] == "USD"), None)
        assert usd is not None and usd["isSourceCurrency"] is True

    @allure.title("Non-USD currencies are not marked as source")
    @allure.severity(allure.severity_level.NORMAL)
    def test_non_usd_not_source(self, api_client: httpx.Client):
        for c in api_client.get("/api/v1/currencies").json()["items"]:
            if c["code"] != "USD":
                assert c["isSourceCurrency"] is False

    @allure.title("No duplicate currency codes in response")
    @allure.severity(allure.severity_level.NORMAL)
    def test_no_duplicate_codes(self, api_client: httpx.Client):
        codes = [c["code"] for c in api_client.get("/api/v1/currencies").json()["items"]]
        assert len(codes) == len(set(codes))
