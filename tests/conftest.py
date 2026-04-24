"""
Root conftest.py — session-scoped fixtures shared across all test layers.

Design decisions:
- BASE_URL read from env so CI can point at any deployed environment.
- http_client is session-scoped: one TCP connection pool per test run (faster).
- All fixtures are typed for IDE support and early-failure diagnostics.
"""
from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Generator

import httpx
import pytest

BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
PIPELINE_DIR = Path(__file__).parent / "pipeline"


# ---------------------------------------------------------------------------
# HTTP client fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(scope="session")
def base_url() -> str:
    return BASE_URL


@pytest.fixture(scope="session")
def http_client(base_url: str) -> Generator[httpx.Client, None, None]:
    """Synchronous HTTP client reused across all API/integration tests."""
    with httpx.Client(base_url=base_url, timeout=30.0) as client:
        yield client


# ---------------------------------------------------------------------------
# Pipeline communication helpers
# ---------------------------------------------------------------------------


@pytest.fixture(scope="session", autouse=True)
def ensure_pipeline_dir() -> None:
    PIPELINE_DIR.mkdir(parents=True, exist_ok=True)


def write_pipeline(filename: str, data: dict) -> None:
    """Write agent-communication data to the shared pipeline directory."""
    (PIPELINE_DIR / filename).write_text(json.dumps(data, indent=2, ensure_ascii=False))


def read_pipeline(filename: str) -> dict:
    path = PIPELINE_DIR / filename
    if not path.exists():
        return {}
    return json.loads(path.read_text())
