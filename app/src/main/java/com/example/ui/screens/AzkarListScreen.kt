package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserSettings
import com.example.data.model.AzkarCategory
import com.example.data.model.ZikrItem
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AzkarListScreen(
    categories: List<AzkarCategory>,
    customAzkar: List<ZikrItem>,
    settings: UserSettings,
    onOpenCategory: (String) -> Unit,
    onAddCustomZikr: (String, Int, String) -> Unit,
    onUpdateCustomZikr: (Long, String, Int, String) -> Unit,
    onDeleteCustomZikr: (Long) -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var editingZikr by remember { mutableStateOf<ZikrItem?>(null) }
    var showManageCustomList by remember { mutableStateOf(false) }

    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val completedToday = if (settings.completedAzkarDate == todayKey) {
        settings.completedAzkarCategories
    } else {
        emptySet()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EmeraldOrnamentalHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "حصن المسلم",
                            fontFamily = AmiriFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = NoorTheme.colors.onHeaderForest
                        )
                        Text(
                            text = "الأذكار اليومية والأدعية المأثورة",
                            style = MaterialTheme.typography.bodySmall,
                            color = NoorTheme.colors.goldAccent
                        )
                    }
                    IconButton(
                        onClick = onOpenReminders,
                        modifier = Modifier.testTag("azkar_reminders_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "تنبيهات الأذكار",
                            tint = NoorTheme.colors.goldAccent
                        )
                    }
                }
            }
        }

        // Custom Azkar ("أذكاري") Featured Card
        item {
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("custom_azkar_card"),
                borderColor = NoorTheme.colors.goldAccent
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "أذكاري الخاصة",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                EmeraldPillBadge(
                                    text = "${ArabicTextUtils.toEasternArabicNumerals(customAzkar.size)} ذكر"
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "أضف أدعيتك وأذكارك الخاصة ومارسها بالعد التفاعلي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editingZikr = null
                                showCreateEditDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_custom_zikr_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة ذكر")
                        }
                        if (customAzkar.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { onOpenCategory("custom") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_custom_azkar_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("قراءة")
                            }
                            IconButton(
                                onClick = { showManageCustomList = !showManageCustomList },
                                modifier = Modifier.testTag("manage_custom_azkar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "إدارة أذكاري",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (showManageCustomList && customAzkar.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        customAzkar.forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "التكرار: ${ArabicTextUtils.toEasternArabicNumerals(item.count)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingZikr = item
                                            showCreateEditDialog = true
                                        }) {
                                            Icon(Icons.Filled.Edit, "تعديل", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { onDeleteCustomZikr(item.customDbId) }) {
                                            Icon(Icons.Filled.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hisn Al-Muslim Built-in Categories
        items(categories, key = { it.id }) { category ->
            val isCompleted = completedToday.contains(category.id)
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("azkar_category_${category.id}"),
                onClick = { onOpenCategory(category.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NoorTheme.colors.badgeEmeraldBg,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = mapCategoryIcon(category.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "مكتمل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    EmeraldPillBadge(
                        text = "${ArabicTextUtils.toEasternArabicNumerals(category.items.size)} ذكراً"
                    )
                }
            }
        }

        item {
            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("azkar_hisn_source_card"),
                onClick = { ExternalUrlHelper.openVerificationUrl(context, "https://sunnah.com/hisn") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                text = "المصدر: كتاب حصن المسلم",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "من أذكار الكتاب والسنة — الشيخ سعيد بن وهف القحطاني",
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
                            contentDescription = "فتح مصدر حصن المسلم",
                            tint = NoorTheme.colors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCreateEditDialog) {
        CustomZikrDialog(
            initial = editingZikr,
            onDismiss = { showCreateEditDialog = false },
            onSave = { text, count, fadl ->
                val target = editingZikr
                if (target == null) {
                    onAddCustomZikr(text, count, fadl)
                } else {
                    onUpdateCustomZikr(target.customDbId, text, count, fadl)
                }
                showCreateEditDialog = false
            }
        )
    }
}

@Composable
fun CustomZikrDialog(
    initial: ZikrItem?,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var count by remember { mutableIntStateOf(initial?.count ?: 3) }
    var fadl by remember { mutableStateOf(initial?.fadl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "إضافة ذكر جديد" else "تعديل الذكر",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("نص الذكر أو الدعاء") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_zikr_text_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("عدد التكرار:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { if (count > 1) count-- }) { Text("-") }
                        Text(
                            text = ArabicTextUtils.toEasternArabicNumerals(count),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { count++ }) { Text("+") }
                    }
                }
                OutlinedTextField(
                    value = fadl,
                    onValueChange = { fadl = it },
                    label = { Text("الفضل أو الملاحظة (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onSave(text, count, fadl) },
                modifier = Modifier.testTag("save_custom_zikr_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

private fun mapCategoryIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "wb_sunny" -> Icons.Filled.WbSunny
        "nights_stay" -> Icons.Filled.NightsStay
        "bedtime" -> Icons.Filled.Bedtime
        "alarm" -> Icons.Filled.Alarm
        "mosque" -> Icons.Filled.Mosque
        "verified_user" -> Icons.Filled.VerifiedUser
        "water_drop" -> Icons.Filled.WaterDrop
        else -> Icons.Filled.Explore
    }
}
