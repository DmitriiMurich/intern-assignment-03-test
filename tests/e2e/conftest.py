"""
Backend E2E / smoke test conftest.

These tests verify the full backend stack (HTTP API + database) end-to-end.
They are NOT Android UI tests — for Android UI tests see .claude/agents/android-tests.md.
ISTQB: system testing of the server-side component.
"""
from __future__ import annotations

import os

import httpx
import pytest

BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="module")
def smoke_client() -> httpx.Client:
    try:
        r = httpx.get(f"{BASE_URL}/health", timeout=5.0)
        if r.status_code != 200:
            pytest.skip("Backend not healthy — skipping smoke tests")
    except Exception:
        pytest.skip("Backend unreachable — skipping smoke tests")

    with httpx.Client(base_url=BASE_URL, timeout=30.0) as client:
        yield client
