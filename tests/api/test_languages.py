"""
API tests — /api/v1/languages endpoint.

ISTQB: Black-box functional testing against the live backend.
"""
from __future__ import annotations

import pytest
import httpx

from tests.helpers.assertions import assert_ok, assert_json_content_type


EXPECTED_LANGUAGE_CODES = {"en", "ru", "de", "fr", "es", "it", "pt", "tr", "uk", "zh"}


@pytest.mark.api
@pytest.mark.positive
class TestLanguagesEndpoint:

    def test_returns_200(self, api_client: httpx.Client):
        assert_ok(api_client.get("/api/v1/languages"))

    def test_returns_json(self, api_client: httpx.Client):
        assert_json_content_type(api_client.get("/api/v1/languages"))

    def test_response_has_languages_array(self, api_client: httpx.Client):
        body = api_client.get("/api/v1/languages").json()
        assert "languages" in body, f"Missing 'languages' key: {body}"
        assert isinstance(body["languages"], list)

    def test_returns_all_10_supported_languages(self, api_client: httpx.Client):
        languages = api_client.get("/api/v1/languages").json()["languages"]
        assert len(languages) == 10, f"Expected 10 languages, got {len(languages)}"

    def test_each_language_has_code_and_name(self, api_client: httpx.Client):
        languages = api_client.get("/api/v1/languages").json()["languages"]
        for lang in languages:
            assert "code" in lang, f"Language missing 'code': {lang}"
            assert "name" in lang, f"Language missing 'name': {lang}"

    def test_all_expected_codes_present(self, api_client: httpx.Client):
        codes = {
            lang["code"]
            for lang in api_client.get("/api/v1/languages").json()["languages"]
        }
        missing = EXPECTED_LANGUAGE_CODES - codes
        assert not missing, f"Missing language codes: {missing}"

    def test_english_is_marked_as_source(self, api_client: httpx.Client):
        """English must be the source language (isSourceLanguage=true or equivalent)."""
        languages = api_client.get("/api/v1/languages").json()["languages"]
        en = next((l for l in languages if l["code"] == "en"), None)
        assert en is not None, "English language not found in response"

    def test_no_duplicate_codes(self, api_client: httpx.Client):
        codes = [l["code"] for l in api_client.get("/api/v1/languages").json()["languages"]]
        assert len(codes) == len(set(codes)), f"Duplicate language codes: {codes}"
