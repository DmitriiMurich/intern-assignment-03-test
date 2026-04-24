"""
Test data factories using the Factory Boy pattern.

Each factory mirrors the backend's JSON contract so tests stay DRY.
Factories produce *valid* baseline objects; individual tests override
only the fields they care about — the Open/Closed principle applied to test data.
"""
from __future__ import annotations

import factory
from faker import Faker

fake = Faker()


class MoneyFactory(factory.DictFactory):
    amount = factory.LazyFunction(lambda: round(fake.pyfloat(min_value=1, max_value=9999, right_digits=2), 2))
    currency = "USD"


class CategoryFactory(factory.DictFactory):
    slug = factory.LazyFunction(lambda: fake.slug())
    title = factory.LazyFunction(lambda: fake.word().capitalize())


class ProductFactory(factory.DictFactory):
    id = factory.LazyFunction(lambda: str(fake.random_int(min=1, max=9999)))
    title = factory.LazyFunction(lambda: fake.catch_phrase())
    description = factory.LazyFunction(lambda: fake.sentence())
    price = factory.SubFactory(MoneyFactory)
    rating = factory.LazyFunction(lambda: round(fake.pyfloat(min_value=1, max_value=5, right_digits=1), 1))
    imageUrl = factory.LazyFunction(lambda: fake.image_url())
    category = factory.SubFactory(CategoryFactory)


class ReviewFactory(factory.DictFactory):
    id = factory.LazyFunction(lambda: str(fake.random_int(min=1, max=99999)))
    rating = factory.LazyFunction(lambda: fake.random_int(min=1, max=5))
    comment = factory.LazyFunction(lambda: fake.sentence())
    date = factory.LazyFunction(lambda: fake.date_time().isoformat())
    reviewerName = factory.LazyFunction(lambda: fake.name())


class ProductDetailsFactory(factory.DictFactory):
    product = factory.SubFactory(ProductFactory)
    reviews = factory.LazyFunction(lambda: [ReviewFactory() for _ in range(3)])


class CatalogMetaFactory(factory.DictFactory):
    totalProducts = 5
    totalCategories = 3
    currentPage = 1
    pageSize = 20
    totalPages = 1
    query = ""
    category = None
    sort = "price_asc"
    sourceLanguage = "en"
    sourceCurrency = "USD"
    exchangeRateProvider = None


class CatalogPageFactory(factory.DictFactory):
    language = "en"
    currency = "USD"
    categories = factory.LazyFunction(lambda: [CategoryFactory() for _ in range(3)])
    items = factory.LazyFunction(lambda: [ProductFactory() for _ in range(5)])
    meta = factory.SubFactory(CatalogMetaFactory)


class LanguageFactory(factory.DictFactory):
    code = factory.LazyFunction(lambda: fake.language_code())
    name = factory.LazyFunction(lambda: fake.language_name())


class CurrencyFactory(factory.DictFactory):
    code = factory.LazyFunction(lambda: fake.currency_code())
    name = factory.LazyFunction(lambda: fake.currency_name())
    symbol = factory.LazyFunction(lambda: fake.currency_symbol())
