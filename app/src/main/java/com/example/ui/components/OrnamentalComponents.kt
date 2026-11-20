package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils

/**
 * 8-pointed Islamic Khatam Star badge used for Surah numbers and Ayah markers.
 */
@Composable
fun KhatamStarBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    fillColor: Color = NoorTheme.colors.badgeEmeraldBg,
    strokeColor: Color = NoorTheme.colors.goldAccent,
    textColor: Color = NoorTheme.colors.badgeEmeraldText
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = this.size.minDimension * 0.68f
            val left = (this.size.width - side) / 2f
            val top = (this.size.height - side) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Draw two overlapping rotated squares to form an 8-point Khatam star
            for (angle in listOf(0f, 45f)) {
                rotate(degrees = angle, pivot = center) {
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(side, side)
                    )
                    drawRect(
                        color = strokeColor.copy(alpha = 0.75f),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(side, side),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }
        }
        Text(
            text = ArabicTextUtils.toEasternArabicNumerals(number),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Deep Forest Emerald header container with subtle gold geometric lattice pattern and rounded bottom.
 */
@Composable
fun EmeraldOrnamentalHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val forestColor = NoorTheme.colors.headerForest
    val goldColor = NoorTheme.colors.goldAccent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        forestColor,
                        forestColor.copy(alpha = 0.94f)
                    )
                )
            )
    ) {
        // Subtle geometric rosette watermark
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.14f, h * 0.48f)
            val radius = h * 0.55f
            for (i in 0 until 8) {
                rotate(degrees = i * 45f, pivot = center) {
                    drawCircle(
                        color = goldColor.copy(alpha = 0.06f),
                        radius = radius * 0.7f,
                        center = Offset(center.x + radius * 0.25f, center.y),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
            // Fine bottom gold accent hairline
            drawLine(
                color = goldColor.copy(alpha = 0.45f),
                start = Offset(24.dp.toPx(), h - 1.dp.toPx()),
                end = Offset(w - 24.dp.toPx(), h - 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            content = content
        )
    }
}

/**
 * Elevated parchment/card surface with fine 1dp gold hairline border.
 */
@Composable
fun NoorGoldCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = NoorTheme.colors.cardElevated,
    borderColor: Color = NoorTheme.colors.goldBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            content = content
        )
    }
}

/**
 * Traditional Mushaf Surah Title Ornament Banner.
 */
@Composable
fun SurahOrnamentBanner(
    surahName: String,
    revelationType: String,
    ayahCount: Int,
    modifier: Modifier = Modifier
) {
    val gold = NoorTheme.colors.goldAccent
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NoorTheme.colors.cardElevated,
        border = BorderStroke(1.2.dp, gold.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val r = 10.dp.toPx()
                val stroke = Stroke(width = 1.dp.toPx())
                // Decorative corner arcs
                drawCircle(color = gold.copy(alpha = 0.28f), radius = r, center = Offset(0f, 0f), style = stroke)
                drawCircle(color = gold.copy(alpha = 0.28f), radius = r, center = Offset(size.width, 0f), style = stroke)
                drawCircle(color = gold.copy(alpha = 0.28f), radius = r, center = Offset(0f, size.height), style = stroke)
                drawCircle(color = gold.copy(alpha = 0.28f), radius = r, center = Offset(size.width, size.height), style = stroke)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "سورة $surahName",
                    fontFamily = AmiriFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = revelationType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        color = gold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${ArabicTextUtils.toEasternArabicNumerals(ayahCount)} آية",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Pill badge in 10% Emerald tint.
 */
@Composable
fun EmeraldPillBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = NoorTheme.colors.badgeEmeraldBg
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = NoorTheme.colors.badgeEmeraldText
        )
    }
}
