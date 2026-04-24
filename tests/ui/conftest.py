"""
UI / smoke test conftest.

These tests verify the full user-visible stack: backend + database together.
They are the coarsest-grained layer (ISTQB: system testing / E2E).
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
