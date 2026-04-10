import { afterEach, beforeEach, describe, expect, test } from "@jest/globals";
import { DummyJsonClient } from "../../../../../src/modules/catalog/infrastructure/dummy-json.client";

describe("DummyJsonClient", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn() as unknown as typeof fetch;
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  test("maps products with reviews and fallback values", async () => {
    const fetchMock = global.fetch as unknown as jest.MockedFunction<typeof fetch>;
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          products: [
            {
              id: 10,
              title: "Night Serum",
              description: "A brightening serum.",
              category: "beauty",
              price: 19.99,
              images: ["https://example.com/fallback.webp"],
              thumbnail: "https://example.com/thumb.webp",
              reviews: [
                {
                  rating: 5,
                  comment: "Highly recommended!",
                  date: "2026-04-10T10:15:00.000Z",
                  reviewerName: "Emma Wilson",
                },
                {},
              ],
            },
          ],
        }),
        { status: 200 },
      ),
    );

    const client = new DummyJsonClient("https://dummyjson.com");
    const products = await client.fetchProducts();

    expect(products).toEqual([
      {
        externalId: 10,
        title: "Night Serum",
        description: "A brightening serum.",
        price: 19.99,
        rating: 0,
        imageUrl: "https://example.com/thumb.webp",
        categorySlug: "beauty",
        reviews: [
          {
            rating: 5,
            comment: "Highly recommended!",
            date: "2026-04-10T10:15:00.000Z",
            reviewerName: "Emma Wilson",
          },
          {
            rating: 0,
            comment: "",
            date: "",
            reviewerName: "Anonymous",
          },
        ],
      },
    ]);
  });

  test("humanizes category title when DummyJSON category name is empty", async () => {
    const fetchMock = global.fetch as unknown as jest.MockedFunction<typeof fetch>;
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            slug: "skin-care",
            name: "",
          },
        ]),
        { status: 200 },
      ),
    );

    const client = new DummyJsonClient("https://dummyjson.com");
    const categories = await client.fetchCategories();

    expect(categories).toEqual([
      {
        slug: "skin-care",
        title: "Skin Care",
      },
    ]);
  });

  test("throws app error when product request fails", async () => {
    const fetchMock = global.fetch as unknown as jest.MockedFunction<typeof fetch>;
    fetchMock.mockResolvedValue(new Response("upstream failed", { status: 502 }));

    const client = new DummyJsonClient("https://dummyjson.com");

    await expect(client.fetchProducts()).rejects.toMatchObject({
      code: "DUMMYJSON_PRODUCTS_REQUEST_FAILED",
      statusCode: 502,
    });
  });
});
