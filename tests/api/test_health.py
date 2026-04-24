"""
API tests — /health endpoint.

ISTQB: Smoke testing — the health check is the entry-point sanity test.
If it fails, all other API tests are meaningless.
"""
from __future__ import annotations

import pytest
import httpx

from tests.helpers.assertions import assert_ok, assert_json_content_type


@pytest.mark.api
@pytest.mark.smoke
@pytest.mark.positive
class TestHealthEndpoint:

    def test_health_returns_200(self, api_client: httpx.Client):
        """Server must respond 200 on /health."""
        response = api_client.get("/health")
        assert_ok(response)

    def test_health_returns_json(self, api_client: httpx.Client):
        response = api_client.get("/health")
        assert_json_content_type(response)

    def test_health_body_has_status_field(self, api_client: httpx.Client):
        body = api_client.get("/health").json()
        assert "status" in body, f"Expected 'status' in health response, got: {body}"

    def test_health_status_is_ok(self, api_client: httpx.Client):
        body = api_client.get("/health").json()
        assert body["status"] == "ok", f"Expected status='ok', got: {body['status']}"
