"""API tests — /api/v1/languages endpoint."""
from __future__ import annotations

import allure
import httpx
import pytest

from tests.helpers.assertions import assert_ok, assert_json_content_type

EXPECTED_LANGUAGE_CODES = {"en", "ru", "de", "fr", "es", "it", "pt", "tr", "uk", "zh"}


@allure.feature("Localization")
@allure.story("Supported Languages")
@pytest.mark.api
@pytest.mark.positive
class TestLanguagesEndpoint:

    @allure.title("GET /api/v1/languages returns HTTP 200")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_returns_200(self, api_client: httpx.Client):
        assert_ok(api_client.get("/api/v1/languages"))

    @allure.title("Languages endpoint returns JSON")
    @allure.severity(allure.severity_level.NORMAL)
    def test_returns_json(self, api_client: httpx.Client):
        assert_json_content_type(api_client.get("/api/v1/languages"))

    @allure.title("Response contains 'items' array")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_response_has_items_array(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/languages").json()
        assert "items" in body, f"Missing 'items' key: {body}"
        assert isinstance(body["items"], list)

    @allure.title("Exactly 10 supported languages returned")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_returns_all_10_supported_languages(self, api_client: httpx.Client):
        languages = api_client.get("/api/v1/languages").json()["items"]
        with allure.step(f"Count languages (expected 10, got {len(languages)})"):
            assert len(languages) == 10

    @allure.title("Each language has code, name, isSourceLanguage")
    @allure.severity(allure.severity_level.NORMAL)
    def test_each_language_has_code_and_name(self, api_client: httpx.Client):
        for lang in api_client.get("/api/v1/languages").json()["items"]:
            assert "code" in lang and "name" in lang

    @allure.title("Each language has isSourceLanguage flag")
    @allure.severity(allure.severity_level.MINOR)
    def test_each_language_has_is_source_flag(self, api_client: httpx.Client):
        for lang in api_client.get("/api/v1/languages").json()["items"]:
            assert "isSourceLanguage" in lang

    @allure.title("All 10 expected language codes are present")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_all_expected_codes_present(self, api_client: httpx.Client):
        codes = {l["code"] for l in api_client.get("/api/v1/languages").json()["items"]}
        missing = EXPECTED_LANGUAGE_CODES - codes
        assert not missing, f"Missing language codes: {missing}"

    @allure.title("English is marked as source language")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_english_is_marked_as_source(self, api_client: httpx.Client):
        languages = api_client.get("/api/v1/languages").json()["items"]
        en = next((l for l in languages if l["code"] == "en"), None)
        assert en is not None and en["isSourceLanguage"] is True

    @allure.title("Non-English languages are not marked as source")
    @allure.severity(allure.severity_level.NORMAL)
    def test_non_english_not_source(self, api_client: httpx.Client):
        languages = api_client.get("/api/v1/languages").json()["items"]
        for lang in (l for l in languages if l["code"] != "en"):
            assert lang["isSourceLanguage"] is False

    @allure.title("No duplicate language codes in response")
    @allure.severity(allure.severity_level.NORMAL)
    def test_no_duplicate_codes(self, api_client: httpx.Client):
        codes = [l["code"] for l in api_client.get("/api/v1/languages").json()["items"]]
        assert len(codes) == len(set(codes))
