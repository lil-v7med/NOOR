package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserSettings
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper
import com.example.util.HapticHelper

@Composable
fun TasbeehScreen(
    settings: UserSettings,
    presetPhrases: List<String>,
    targetPresets: List<Int>,
    onIncrement: () -> Unit,
    onResetCycle: () -> Unit,
    onSelectTarget: (Int) -> Unit,
    onSelectPhrase: (String) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pressedBounce by remember { mutableStateOf(false) }
    var showCustomPhraseDialog by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressedBounce) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        finishedListener = { pressedBounce = false },
        label = "tasbeeh_bounce"
    )

    val target = settings.tasbeehTarget
    val current = settings.tasbeehCurrentCount
    val progress = if (target > 0) {
        ((current % target).let { if (it == 0 && current > 0) target else it }).toFloat() / target.toFloat()
    } else {
        ((current % 100).toFloat() / 100f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(220),
        label = "tasbeeh_arc"
    )

    Column(modifier = modifier.fillMaxSize()) {
        EmeraldOrnamentalHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المسبحة الإلكترونية",
                        fontFamily = AmiriFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoorTheme.colors.onHeaderForest
                    )
                    Text(
                        text = "الإجمالي الكلي: ${ArabicTextUtils.toEasternArabicNumerals(settings.lifetimeTasbeeh)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoorTheme.colors.goldAccent
                    )
                }
                IconButton(
                    onClick = { onToggleHaptics(!settings.tasbeehHapticsEnabled) },
                    modifier = Modifier.testTag("tasbeeh_haptic_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Vibration,
                        contentDescription = "تبديل الاهتزاز",
                        tint = if (settings.tasbeehHapticsEnabled) NoorTheme.colors.goldAccent else NoorTheme.colors.onHeaderForest.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Phrase Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetPhrases.forEach { phrase ->
                        FilterChip(
                            selected = settings.tasbeehPhrase == phrase,
                            onClick = { onSelectPhrase(phrase) },
                            label = { Text(phrase, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showCustomPhraseDialog = true },
                        leadingIcon = { Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("ذكر مخصص") }
                    )
                }

                // Target Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    targetPresets.forEach { preset ->
                        val label = if (preset == 0) "مفتوح" else ArabicTextUtils.toEasternArabicNumerals(preset)
                        FilterChip(
                            selected = settings.tasbeehTarget == preset,
                            onClick = { onSelectTarget(preset) },
                            label = { Text(label) },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("tasbeeh_target_$preset")
                        )
                    }
                }
            }

            // Active Phrase Display
            Text(
                text = settings.tasbeehPhrase,
                fontFamily = AmiriFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Interactive Circular Counter Medallion
            val gold = NoorTheme.colors.goldAccent
            val primary = MaterialTheme.colorScheme.primary
            val trackColor = gold.copy(alpha = 0.16f)

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(bounded = true, color = gold)
                    ) {
                        pressedBounce = true
                        val nextCount = current + 1
                        if (settings.tasbeehHapticsEnabled) {
                            if (target > 0 && nextCount % target == 0) {
                                HapticHelper.completionPulse(context)
                            } else {
                                HapticHelper.tapPulse(context)
                            }
                        }
                        onIncrement()
                    }
                    .testTag("tasbeeh_tap_circle"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = NoorTheme.colors.cardElevated,
                    shadowElevation = 6.dp
                ) {}

                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Outer subtle ring
                    drawCircle(
                        color = gold.copy(alpha = 0.25f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Progress track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Active gold sweep
                    drawArc(
                        color = gold,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ArabicTextUtils.toEasternArabicNumerals(current),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val targetText = if (target > 0) {
                        "الهدف: ${ArabicTextUtils.toEasternArabicNumerals(target)}"
                    } else {
                        "تسبيح مفتوح"
                    }
                    EmeraldPillBadge(text = targetText)
                }
            }

            // Bottom Reset Row + Source Link
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetCycle,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("tasbeeh_reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تصفير الدورة")
                }

                Surface(
                    onClick = { ExternalUrlHelper.openVerificationUrl(context, "https://sunnah.com/hisn") },
                    shape = RoundedCornerShape(999.dp),
                    color = NoorTheme.colors.badgeEmeraldBg.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                    modifier = Modifier.testTag("tasbeeh_source_link")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "المصدر: صيغ التسبيح المأثورة — حصن المسلم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "فتح المصدر للتحقق",
                            tint = NoorTheme.colors.goldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomPhraseDialog) {
        var customPhrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomPhraseDialog = false },
            title = { Text("تخصيص الذكر") },
            text = {
                OutlinedTextField(
                    value = customPhrase,
                    onValueChange = { customPhrase = it },
                    label = { Text("اكتب صيغة الذكر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (customPhrase.isNotBlank()) onSelectPhrase(customPhrase)
                    showCustomPhraseDialog = false
                }) {
                    Text("اعتماد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPhraseDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
