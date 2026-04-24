"""API tests — /health endpoint (ISTQB: Smoke testing)."""
from __future__ import annotations

import allure
import httpx
import pytest

from tests.helpers.assertions import assert_ok, assert_json_content_type


@allure.feature("Infrastructure")
@allure.story("Health Check")
@pytest.mark.api
@pytest.mark.smoke
@pytest.mark.positive
class TestHealthEndpoint:

    @allure.title("GET /health returns HTTP 200")
    @allure.severity(allure.severity_level.BLOCKER)
    def test_health_returns_200(self, api_client: httpx.Client):
        with allure.step("Send GET /health"):
            response = api_client.get("/health")
        with allure.step("Assert HTTP 200"):
            assert_ok(response)

    @allure.title("GET /health returns JSON content-type")
    @allure.severity(allure.severity_level.NORMAL)
    def test_health_returns_json(self, api_client: httpx.Client):
        response = api_client.get("/health")
        assert_json_content_type(response)

    @allure.title("GET /health body contains 'status' field")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_health_body_has_status_field(self, api_client: httpx.Client):
        body = api_client.get("/health").json()
        with allure.step("Verify 'status' key exists"):
            assert "status" in body, f"Expected 'status' in health response, got: {body}"

    @allure.title("GET /health returns status='ok'")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_health_status_is_ok(self, api_client: httpx.Client):
        body = api_client.get("/health").json()
        with allure.step("Verify status value is 'ok'"):
            assert body["status"] == "ok", f"Expected status='ok', got: {body['status']}"
