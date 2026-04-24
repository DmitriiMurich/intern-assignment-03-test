"""
API test conftest — fixtures for tests that run against a live server.

The server URL is read from API_BASE_URL env var (default: http://localhost:8080).
Tests in this package are skipped automatically when the server is unreachable.
"""
from __future__ import annotations

import os

import httpx
import pytest

BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")


def _server_is_reachable(url: str) -> bool:
    try:
        r = httpx.get(f"{url}/health", timeout=5.0)
        return r.status_code == 200
    except Exception:
        return False


@pytest.fixture(scope="session")
def api_client() -> httpx.Client:
    if not _server_is_reachable(BASE_URL):
        pytest.skip("Backend server is not reachable — skipping API tests")
    with httpx.Client(base_url=BASE_URL, timeout=30.0) as client:
        yield client
