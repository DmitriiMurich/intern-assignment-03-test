"""
Custom assertion helpers — reusable contract checkers.

Using assertion helpers (not base-class inheritance) keeps tests
readable and avoids the "magic base class" anti-pattern.
"""
from __future__ import annotations

import httpx


# ---------------------------------------------------------------------------
# HTTP-level assertions
# ---------------------------------------------------------------------------


def assert_ok(response: httpx.Response) -> None:
    assert response.status_code == 200, (
        f"Expected 200, got {response.status_code}. Body: {response.text[:500]}"
    )


def assert_status(response: httpx.Response, expected: int) -> None:
    assert response.status_code == expected, (
        f"Expected {expected}, got {response.status_code}. Body: {response.text[:500]}"
    )


def assert_json_content_type(response: httpx.Response) -> None:
    ct = response.headers.get("content-type", "")
    assert "application/json" in ct, f"Expected JSON content-type, got: {ct}"


def assert_error_shape(body: dict, expected_code: str | None = None) -> None:
    """Validate the standard ApiErrorResponse contract."""
    assert "error" in body or "code" in body, f"Response is not an error shape: {body}"
    if expected_code:
        actual = body.get("code") or body.get("error", {}).get("code")
        assert actual == expected_code, f"Expected error code {expected_code!r}, got {actual!r}"


# ---------------------------------------------------------------------------
# Domain-level assertions
# ---------------------------------------------------------------------------


def assert_product_shape(product: dict) -> None:
    required = {"id", "title", "description", "price", "rating", "imageUrl", "category"}
    missing = required - product.keys()
    assert not missing, f"Product missing fields: {missing}"
    assert isinstance(product["price"], dict), "price must be an object"
    assert "amount" in product["price"], "price.amount missing"
    assert "currency" in product["price"], "price.currency missing"
    assert isinstance(product["rating"], (int, float)), "rating must be numeric"
    assert 0 <= product["rating"] <= 5, f"rating out of [0,5]: {product['rating']}"


def assert_catalog_page_shape(body: dict) -> None:
    # Real API shape: { language, currency, categories, items, meta }
    required = {"language", "currency", "categories", "items", "meta"}
    missing = required - body.keys()
    assert not missing, f"CatalogPage missing fields: {missing}"
    assert isinstance(body["items"], list), "items must be a list"
    assert isinstance(body["categories"], list), "categories must be a list"
    meta = body["meta"]
    assert meta["currentPage"] >= 1, "currentPage must be >= 1"
    assert meta["pageSize"] >= 1, "pageSize must be >= 1"
    assert meta["totalPages"] >= 0, "totalPages must be non-negative"
    for product in body["items"]:
        assert_product_shape(product)


def assert_product_details_shape(body: dict) -> None:
    assert "product" in body, "Response missing 'product'"
    assert "reviews" in body, "Response missing 'reviews'"
    assert_product_shape(body["product"])
    assert isinstance(body["reviews"], list), "reviews must be a list"
    for review in body["reviews"]:
        assert_review_shape(review)


def assert_review_shape(review: dict) -> None:
    required = {"id", "rating", "comment", "date", "reviewerName"}
    missing = required - review.keys()
    assert not missing, f"Review missing fields: {missing}"
    assert 1 <= review["rating"] <= 5, f"Review rating out of range: {review['rating']}"


def assert_pagination_consistency(body: dict) -> None:
    """ISTQB boundary check: page × pageSize ≤ totalProducts (within tolerance)."""
    meta = body["meta"]
    page = meta["currentPage"]
    page_size = meta["pageSize"]
    total_pages = meta["totalPages"]
    total_products = meta["totalProducts"]
    actual_count = len(body["items"])

    if total_pages > 0:
        assert page <= total_pages, f"page {page} exceeds totalPages {total_pages}"

    if page < total_pages:
        assert actual_count == page_size, (
            f"Non-last page should have pageSize={page_size} products, got {actual_count}"
        )
    else:
        assert actual_count <= page_size, (
            f"Products count {actual_count} exceeds pageSize {page_size}"
        )

    if total_products == 0:
        assert actual_count == 0, "No products expected when totalProducts=0"
