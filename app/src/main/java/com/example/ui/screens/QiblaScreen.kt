package com.example.ui.screens

import android.Manifest
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.qibla.CityPreset
import com.example.qibla.QiblaCalculator
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.KaabaCubeInk
import com.example.ui.theme.NoorTheme
import com.example.ui.viewmodel.QiblaLocationSource
import com.example.ui.viewmodel.QiblaUiState
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper
import com.example.util.HapticHelper
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    onStartSensors: () -> Unit,
    onStopSensors: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onRefreshGps: () -> Unit,
    onSelectCity: (CityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCityPicker by remember { mutableStateOf(false) }
    var wasAlignedBefore by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onPermissionResult(granted)
    }

    // Lifecycle-aware sensor registration (unregisters on pause to preserve battery)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onStartSensors()
                Lifecycle.Event.ON_PAUSE -> onStopSensors()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onStartSensors()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStopSensors()
        }
    }

    // Tactile pulse when locking onto Qibla alignment
    LaunchedEffect(uiState.isAligned) {
        if (uiState.isAligned && !wasAlignedBefore) {
            HapticHelper.tapPulse(context)
        }
        wasAlignedBefore = uiState.isAligned
    }

    val scrollState = rememberScrollState()
    val sensorsAvailable = uiState.sensorReading.hasAccelerometer && uiState.sensorReading.hasMagnetometer

    Column(modifier = modifier.fillMaxSize()) {
        EmeraldOrnamentalHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "اتجاه القبلة",
                        fontFamily = AmiriFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoorTheme.colors.onHeaderForest
                    )
                    Text(
                        text = uiState.locationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = NoorTheme.colors.goldAccent
                    )
                }

                if (uiState.isFetchingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = NoorTheme.colors.goldAccent
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Missing hardware sensor warning
            if (!sensorsAvailable) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "هذا الجهاز لا يحتوي على مستشعر المغناطيسية أو التسارع. تُعرض زاوية القبلة من الشمال الحقيقي فقط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else if (uiState.sensorReading.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                // Figure-8 Calibration Prompt
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_calibration_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "دقة البوصلة منخفضة: حرّك الهاتف على شكل الرقم ٨ في الهواء لمعايرة المستشعر.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Alignment Status Pill
            AnimatedVisibility(visible = uiState.hasLocation && sensorsAvailable) {
                val aligned = uiState.isAligned
                val delta = QiblaCalculator.shortestAngleDiff(
                    uiState.sensorReading.trueAzimuthDeg,
                    uiState.qiblaBearingDeg
                )
                val statusText = when {
                    aligned -> "أنت متجه نحو القبلة الشريفة ✓"
                    delta > 0f -> "أدر الهاتف يميناً بمقدار ${ArabicTextUtils.toEasternArabicNumerals(abs(delta).roundToInt())}°"
                    else -> "أدر الهاتف يساراً بمقدار ${ArabicTextUtils.toEasternArabicNumerals(abs(delta).roundToInt())}°"
                }
                val bgColor by animateColorAsState(
                    targetValue = if (aligned) MaterialTheme.colorScheme.primary else NoorTheme.colors.badgeEmeraldBg,
                    animationSpec = tween(250),
                    label = "alignment_bg"
                )
                val txtColor by animateColorAsState(
                    targetValue = if (aligned) MaterialTheme.colorScheme.onPrimary else NoorTheme.colors.badgeEmeraldText,
                    animationSpec = tween(250),
                    label = "alignment_txt"
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, if (aligned) NoorTheme.colors.goldAccent else NoorTheme.colors.goldBorder),
                    modifier = Modifier.testTag("qibla_alignment_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (aligned) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = txtColor
                        )
                    }
                }
            }

            // Compass Dial
            QiblaCompassDial(
                deviceAzimuthDeg = if (sensorsAvailable) uiState.sensorReading.trueAzimuthDeg else 0f,
                qiblaBearingDeg = uiState.qiblaBearingDeg,
                hasLocation = uiState.hasLocation,
                isAligned = uiState.isAligned,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .testTag("qibla_compass_dial")
            )

            // Telemetry Cards (Bearing & Distance)
            if (uiState.hasLocation) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoorGoldCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "زاوية القبلة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${ArabicTextUtils.toEasternArabicNumerals(uiState.qiblaBearingDeg.roundToInt())}°",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = QiblaCalculator.cardinalDirectionArabic(uiState.qiblaBearingDeg),
                                style = MaterialTheme.typography.labelSmall,
                                color = NoorTheme.colors.goldAccent
                            )
                        }
                    }

                    NoorGoldCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "المسافة للكعبة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${ArabicTextUtils.toEasternArabicNumerals(uiState.distanceKm.roundToInt())} كم",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "مكة المكرمة",
                                style = MaterialTheme.typography.labelSmall,
                                color = NoorTheme.colors.goldAccent
                            )
                        }
                    }
                }
            }

            // Location Action Card (GPS Permission / Refresh / City Picker)
            NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحديد الموقع الجغرافي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val sourceLabel = when (uiState.locationSource) {
                            QiblaLocationSource.GPS -> "GPS مباشر"
                            QiblaLocationSource.MANUAL_CITY -> "اختيار يدوي"
                            QiblaLocationSource.UNSET -> "غير محدد"
                        }
                        EmeraldPillBadge(text = sourceLabel)
                    }

                    if (uiState.latitude != null && uiState.longitude != null) {
                        Text(
                            text = "الإحداثيات: ${String.format(java.util.Locale.US, "%.4f", uiState.latitude)}°, ${String.format(java.util.Locale.US, "%.4f", uiState.longitude)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!uiState.locationError.isNullOrBlank()) {
                        Text(
                            text = uiState.locationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (uiState.hasLocationPermission) {
                                    onRefreshGps()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("qibla_gps_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (uiState.hasLocationPermission) "تحديث GPS" else "تفعيل الموقع")
                        }

                        OutlinedButton(
                            onClick = { showCityPicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("qibla_city_picker_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.LocationCity, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اختر مدينة")
                        }
                    }
                }
            }

            // Scientific Source Attribution Card
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_source_card"),
                onClick = {
                    ExternalUrlHelper.openVerificationUrl(
                        context,
                        "https://www.ngdc.noaa.gov/geomag/WMM/"
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "المرجع العلمي لحساب القبلة",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "إحداثيات الكعبة (21.4225°, 39.8262°) ونموذج WMM المغناطيسي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "تحقق",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NoorTheme.colors.goldAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "فتح المرجع العلمي",
                            tint = NoorTheme.colors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCityPicker) {
        CityPresetPickerDialog(
            onDismiss = { showCityPicker = false },
            onSelectCity = { city ->
                onSelectCity(city)
                showCityPicker = false
            }
        )
    }
}

@Composable
private fun QiblaCompassDial(
    deviceAzimuthDeg: Float,
    qiblaBearingDeg: Float,
    hasLocation: Boolean,
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val cardColor = NoorTheme.colors.cardElevated
    val emerald = MaterialTheme.colorScheme.primary
    val gold = NoorTheme.colors.goldAccent
    val onSurface = MaterialTheme.colorScheme.onSurface
    val subtleText = MaterialTheme.colorScheme.onSurfaceVariant
    val errorRed = MaterialTheme.colorScheme.error
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 10.dp.toPx()

            // Outer ambient glow ring when aligned
            if (isAligned) {
                drawCircle(
                    color = gold.copy(alpha = 0.25f),
                    radius = outerRadius + 8.dp.toPx()
                )
            }

            // Dial parchment plate
            drawCircle(
                color = cardColor,
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = if (isAligned) gold else gold.copy(alpha = 0.55f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = if (isAligned) 3.5.dp.toPx() else 2.dp.toPx())
            )
            drawCircle(
                color = emerald.copy(alpha = 0.14f),
                radius = outerRadius * 0.78f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Fixed top device-heading lubber mark
            val lubberPath = Path().apply {
                moveTo(center.x, center.y - outerRadius - 6.dp.toPx())
                lineTo(center.x - 7.dp.toPx(), center.y - outerRadius + 4.dp.toPx())
                lineTo(center.x + 7.dp.toPx(), center.y - outerRadius + 4.dp.toPx())
                close()
            }
            drawPath(lubberPath, color = if (isAligned) gold else emerald)

            // Rotating compass rose (-deviceAzimuthDeg so North aligns with real North)
            rotate(degrees = -deviceAzimuthDeg, pivot = center) {
                for (deg in 0 until 360 step 5) {
                    val isMajor = deg % 30 == 0
                    val isMedium = deg % 15 == 0
                    val tickLen = when {
                        isMajor -> 12.dp.toPx()
                        isMedium -> 8.dp.toPx()
                        else -> 4.dp.toPx()
                    }
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val startR = outerRadius - 6.dp.toPx()
                    val endR = startR - tickLen
                    val start = Offset(
                        center.x + (startR * cos(rad)).toFloat(),
                        center.y + (startR * sin(rad)).toFloat()
                    )
                    val end = Offset(
                        center.x + (endR * cos(rad)).toFloat(),
                        center.y + (endR * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = if (deg == 0) errorRed else if (isMajor) emerald else subtleText.copy(alpha = 0.45f),
                        start = start,
                        end = end,
                        strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Cardinal markers (N / E / S / W in Arabic)
                val cardinals = listOf(
                    0f to "ش",
                    90f to "ق",
                    180f to "ج",
                    270f to "غ"
                )
                val labelRadius = outerRadius * 0.65f
                cardinals.forEach { (angleDeg, label) ->
                    val rad = Math.toRadians((angleDeg - 90).toDouble())
                    val lx = center.x + (labelRadius * cos(rad)).toFloat()
                    val ly = center.y + (labelRadius * sin(rad)).toFloat()
                    val measured = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (angleDeg == 0f) errorRed else onSurface
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            lx - measured.size.width / 2f,
                            ly - measured.size.height / 2f
                        )
                    )
                }
            }

            // Qibla Pointer Needle (points to qiblaBearingDeg relative to True North)
            if (hasLocation) {
                val needleRotation = qiblaBearingDeg - deviceAzimuthDeg
                rotate(degrees = needleRotation, pivot = center) {
                    val needleLen = outerRadius * 0.72f
                    // Tail
                    drawLine(
                        color = subtleText.copy(alpha = 0.35f),
                        start = center,
                        end = Offset(center.x, center.y + needleLen * 0.38f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Golden / Emerald Qibla shaft
                    drawLine(
                        color = if (isAligned) gold else emerald,
                        start = center,
                        end = Offset(center.x, center.y - needleLen + 14.dp.toPx()),
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Arrow tip
                    val arrowTip = Path().apply {
                        moveTo(center.x, center.y - needleLen - 10.dp.toPx())
                        lineTo(center.x - 10.dp.toPx(), center.y - needleLen + 10.dp.toPx())
                        lineTo(center.x + 10.dp.toPx(), center.y - needleLen + 10.dp.toPx())
                        close()
                    }
                    drawPath(arrowTip, color = gold)

                    // Kaaba cube icon along the pointer
                    val kaabaSize = 22.dp.toPx()
                    val kaabaTopLeft = Offset(
                        center.x - kaabaSize / 2f,
                        center.y - needleLen * 0.56f - kaabaSize / 2f
                    )
                    drawRoundRect(
                        color = KaabaCubeInk,
                        topLeft = kaabaTopLeft,
                        size = Size(kaabaSize, kaabaSize),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    // Golden Kiswah band on the Kaaba icon
                    drawRect(
                        color = gold,
                        topLeft = Offset(kaabaTopLeft.x, kaabaTopLeft.y + kaabaSize * 0.25f),
                        size = Size(kaabaSize, kaabaSize * 0.18f)
                    )
                }
            }

            // Center Pivot Medallion
            drawCircle(color = emerald, radius = 9.dp.toPx(), center = center)
            drawCircle(color = gold, radius = 4.5.dp.toPx(), center = center)
        }

        if (!hasLocation) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder)
            ) {
                Text(
                    text = "حدّد موقعك لعرض\nاتجاه الكعبة",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CityPresetPickerDialog(
    onDismiss: () -> Unit,
    onSelectCity: (CityPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر مدينتك",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(320.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(QiblaCalculator.cityPresets) { city ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(city) }
                            .testTag("city_preset_${city.nameArabic}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = city.nameArabic,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = city.countryArabic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
