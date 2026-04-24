"""
Unit-test conftest: mocks all network I/O with respx so tests run
without a live server or database.
"""
from __future__ import annotations

import pytest
import httpx
import respx

from tests.helpers.factories import (
    CatalogPageFactory,
    ProductDetailsFactory,
    LanguageFactory,
    CurrencyFactory,
)

SUPPORTED_LANGUAGES = [
    {"code": "en", "name": "English"},
    {"code": "ru", "name": "Russian"},
    {"code": "de", "name": "German"},
    {"code": "fr", "name": "French"},
    {"code": "es", "name": "Spanish"},
    {"code": "it", "name": "Italian"},
    {"code": "pt", "name": "Portuguese"},
    {"code": "tr", "name": "Turkish"},
    {"code": "uk", "name": "Ukrainian"},
    {"code": "zh", "name": "Chinese"},
]

SUPPORTED_CURRENCIES = [
    {"code": "USD", "name": "US Dollar", "symbol": "$"},
    {"code": "EUR", "name": "Euro", "symbol": "€"},
    {"code": "RUB", "name": "Russian Ruble", "symbol": "₽"},
    {"code": "GBP", "name": "British Pound", "symbol": "£"},
    {"code": "UAH", "name": "Ukrainian Hryvnia", "symbol": "₴"},
    {"code": "TRY", "name": "Turkish Lira", "symbol": "₺"},
    {"code": "CNY", "name": "Chinese Yuan", "symbol": "¥"},
    {"code": "JPY", "name": "Japanese Yen", "symbol": "¥"},
    {"code": "CAD", "name": "Canadian Dollar", "symbol": "CA$"},
    {"code": "CHF", "name": "Swiss Franc", "symbol": "CHF"},
]


@pytest.fixture
def mock_api(respx_mock):
    """Pre-wire respx routes matching the backend contract."""
    base = "http://localhost:8080"

    respx_mock.get(f"{base}/health").mock(
        return_value=httpx.Response(200, json={"status": "ok"})
    )
    respx_mock.get(f"{base}/api/v1/languages").mock(
        return_value=httpx.Response(200, json={"languages": SUPPORTED_LANGUAGES})
    )
    respx_mock.get(f"{base}/api/v1/currencies").mock(
        return_value=httpx.Response(200, json={"currencies": SUPPORTED_CURRENCIES})
    )
    respx_mock.get(url__startswith=f"{base}/api/v1/catalog/").mock(
        return_value=httpx.Response(200, json=ProductDetailsFactory())
    )
    respx_mock.get(f"{base}/api/v1/catalog").mock(
        return_value=httpx.Response(200, json=CatalogPageFactory())
    )

    yield respx_mock
