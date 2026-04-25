package com.artem.korenyakin.internassignment03.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artem.korenyakin.internassignment03.feature.catalog.components.CatalogFeedbackCard
import com.artem.korenyakin.internassignment03.feature.catalog.resources.Res
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_back
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_description
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_error_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_label
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_loading_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_loading_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_no_reviews_subtitle
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_no_reviews_title
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_reviewed_on
import com.artem.korenyakin.internassignment03.feature.catalog.resources.details_reviews
import com.artem.korenyakin.internassignment03.feature.catalog.resources.retry
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductReview
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProductDetailsContent(
    state: ProductDetailsState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.productDetailsBackground(),
    ) {
        when (state) {
            ProductDetailsState.Hidden -> Unit

            is ProductDetailsState.Loading -> ProductDetailsLoadingState(onBack = onBack)
            is ProductDetailsState.Error -> ProductDetailsErrorState(onBack = onBack, onRetry = onRetry)
            is ProductDetailsState.Content -> ProductDetailsLoadedContent(state = state, onBack = onBack)
        }
    }
}

@Composable
private fun Modifier.productDetailsBackground(): Modifier = fillMaxSize().background(
    brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
        ),
    ),
)

@Composable
private fun ProductDetailsLoadingState(
    onBack: () -> Unit,
) {
    ProductDetailsStatus(
        onBack = onBack,
        title = stringResource(Res.string.details_loading_title),
        subtitle = stringResource(Res.string.details_loading_subtitle),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProductDetailsErrorState(
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    ProductDetailsStatus(
        onBack = onBack,
        title = stringResource(Res.string.details_error_title),
    ) {
        CatalogFeedbackCard(
            label = stringResource(Res.string.details_label),
            title = stringResource(Res.string.details_error_title),
            actionLabel = stringResource(Res.string.retry),
            onAction = onRetry,
        )
    }
}

@Composable
private fun ProductDetailsLoadedContent(
    state: ProductDetailsState.Content,
    onBack: () -> Unit,
) {
    val details = state.details
    val visibleReviews = details.reviews.take(MaxVisibleReviewCards)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "back") {
            BackNavigationButton(onBack = onBack)
        }
        item(key = "hero") {
            ProductDetailsHero(
                product = details.product,
            )
        }
        item(key = "description") {
            ProductDescriptionSection(description = details.product.description)
        }
        addReviewItems(visibleReviews = visibleReviews)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.addReviewItems(
    visibleReviews: List<ProductReview>,
) {
    if (visibleReviews.isNotEmpty()) {
        item(key = "reviews-header") {
            ProductDetailsSectionHeader(
                title = stringResource(Res.string.details_reviews),
            )
        }
        items(
            items = visibleReviews,
            key = { review -> review.id },
        ) { review ->
            ProductReviewCard(review = review)
        }
        return
    }

    item(key = "reviews-empty") {
        CatalogFeedbackCard(
            label = stringResource(Res.string.details_label),
            title = stringResource(Res.string.details_no_reviews_title),
            subtitle = stringResource(Res.string.details_no_reviews_subtitle),
        )
    }
}

@Composable
private fun ProductDescriptionSection(
    description: String,
) {
    ProductDetailsSection(
        title = stringResource(Res.string.details_description),
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProductDetailsStatus(
    onBack: () -> Unit,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackNavigationButton(onBack = onBack)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let { subtitleText ->
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun BackNavigationButton(
    onBack: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
        modifier = Modifier
            .clickable(onClick = onBack)
            .semantics { testTag = "back_button" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "<",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.details_back),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ProductDetailsHero(
    product: Product,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProductDetailsHeroImage(product = product)
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProductDetailsHeroImage(
    product: Product,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            ),
    ) {
        AsyncImage(
            model = product.imageUrl.takeIf { url -> url.isNotBlank() },
            contentDescription = product.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
        )
        ProductDetailsHeroOverlay(product = product)
    }
}

@Composable
private fun ProductDetailsHeroOverlay(
    product: Product,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DetailPill(text = product.category.title)
            DetailPill(
                text = formatRatingValue(formatRatingNumber(product.rating)),
            )
        }
        Text(
            text = formatPrice(product.price),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProductDetailsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            subtitle?.let { subtitleText ->
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun ProductDetailsSectionHeader(
    title: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ProductReviewCard(
    review: ProductReview,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = review.reviewerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            resource = Res.string.details_reviewed_on,
                            formatReviewDate(review.date),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DetailPill(
                    text = formatRatingValue(formatRatingNumber(review.rating)),
                )
            }
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailPill(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

private const val MaxVisibleReviewCards: Int = 3
