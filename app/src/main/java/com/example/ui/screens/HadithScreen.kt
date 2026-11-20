package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HadithItem
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.ui.viewmodel.NawawiSyncUiState
import com.example.util.ArabicTextUtils
import com.example.util.ExternalUrlHelper

@Composable
fun HadithScreen(
    searchQuery: String,
    selectedCategory: String,
    categories: List<String>,
    hadiths: List<HadithItem>,
    favoriteIds: Set<String>,
    nawawiSyncState: NawawiSyncUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSyncNawawiFullTexts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedHadithForModal by remember { mutableStateOf<HadithItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        EmeraldOrnamentalHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الأحاديث النبوية والأربعون النووية",
                        fontFamily = AmiriFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoorTheme.colors.onHeaderForest
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "مجموعة مختارة وموثقة من الصحاح والأربعين النووية بروابط Sunnah.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoorTheme.colors.goldAccent
                    )
                }
                if (favoriteIds.isNotEmpty()) {
                    EmeraldPillBadge(
                        text = "المفضلة ${ArabicTextUtils.toEasternArabicNumerals(favoriteIds.size)}"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hadith_search_input"),
                placeholder = {
                    Text(
                        text = "ابحث في الأحاديث أو الراوي أو المصدر...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = NoorTheme.colors.goldAccent,
                    unfocusedBorderColor = NoorTheme.colors.goldBorder
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("hadith_cat_$category")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!nawawiSyncState.isSynced && (selectedCategory == "الكل" || selectedCategory == "الأربعون النووية")) {
                item(key = "nawawi_sync_banner") {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nawawi_sync_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الأربعون النووية (٤٢ حديثاً)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                EmeraldPillBadge(text = "مصدر موثق")
                            }
                            Text(
                                text = "يتضمن الفهرس الكامل للأحاديث الـ ٤٢ مع الراوي والتخريج؛ ويمكنك تنزيل المتن الكامل لجميع الأحاديث للاستخدام بدون إنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!nawawiSyncState.errorMessage.isNullOrBlank()) {
                                Text(
                                    text = nawawiSyncState.errorMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                onClick = onSyncNawawiFullTexts,
                                enabled = !nawawiSyncState.isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sync_nawawi_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (nawawiSyncState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("جارٍ تحميل المتن الكامل للأربعين النووية...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("تحميل النص الكامل للأربعين النووية بدون نت")
                                }
                            }
                        }
                    }
                }
            } else if (nawawiSyncState.isSynced && selectedCategory == "الأربعون النووية") {
                item(key = "nawawi_synced_badge") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NoorTheme.colors.badgeEmeraldBg,
                        border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "الأربعون النووية محفوظة بالنص الكامل للعمل بدون إنترنت ✓",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (hadiths.isEmpty()) {
                item {
                    NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (selectedCategory == "المفضلة") {
                                    "لا توجد أحاديث في المفضلة بعد"
                                } else {
                                    "لم يتم العثور على أحاديث مطابقة"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            } else {
                items(hadiths, key = { it.id }) { hadith ->
                    val isFav = favoriteIds.contains(hadith.id)
                    HadithCard(
                        hadith = hadith,
                        isFavorite = isFav,
                        onClick = { selectedHadithForModal = hadith },
                        onToggleFavorite = { onToggleFavorite(hadith.id) },
                        onOpenSource = {
                            ExternalUrlHelper.openVerificationUrl(context, hadith.sourceUrl)
                        },
                        onCopy = {
                            val bodyText = hadith.text.ifBlank { hadith.explanation }
                            val copyText = "${hadith.title}\n${hadith.narrator}:\n«$bodyText»\n[${hadith.source}]\n${hadith.sourceUrl}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hadith", copyText))
                            Toast.makeText(context, "تم نسخ الحديث والمصدر", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            val bodyText = hadith.text.ifBlank { hadith.explanation }
                            val shareText = "${hadith.title}\n${hadith.narrator}:\n«$bodyText»\n\nالمصدر: ${hadith.source}\nللتحقق: ${hadith.sourceUrl}"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الحديث الشريف"))
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    val activeModalHadith = selectedHadithForModal
    if (activeModalHadith != null) {
        val isFav = favoriteIds.contains(activeModalHadith.id)
        HadithExplanationBottomSheet(
            hadith = activeModalHadith,
            isFavorite = isFav,
            onToggleFavorite = { onToggleFavorite(activeModalHadith.id) },
            onDismiss = { selectedHadithForModal = null },
            onOpenSource = {
                ExternalUrlHelper.openVerificationUrl(context, activeModalHadith.sourceUrl)
            },
            onCopy = {
                val bodyText = activeModalHadith.text.ifBlank { activeModalHadith.explanation }
                val copyText = "${activeModalHadith.title}\n${activeModalHadith.narrator}:\n«$bodyText»\n[${activeModalHadith.source}]\n${activeModalHadith.sourceUrl}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Hadith", copyText))
                Toast.makeText(context, "تم نسخ الحديث والمصدر", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val bodyText = activeModalHadith.text.ifBlank { activeModalHadith.explanation }
                val shareText = "${activeModalHadith.title}\n${activeModalHadith.narrator}:\n«$bodyText»\n\nالمصدر: ${activeModalHadith.source}\nللتحقق: ${activeModalHadith.sourceUrl}"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الحديث الشريف"))
            }
        )
    }
}

@Composable
private fun HadithCard(
    hadith: HadithItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSource: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    NoorGoldCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hadith_card_${hadith.id}"),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hadith.hadithNumber != null) {
                        EmeraldPillBadge(
                            text = "حديث ${ArabicTextUtils.toEasternArabicNumerals(hadith.hadithNumber)}"
                        )
                    } else {
                        EmeraldPillBadge(text = hadith.category)
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = hadith.grade,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("hadith_fav_${hadith.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "حفظ في المفضلة",
                        tint = if (isFavorite) NoorTheme.colors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = hadith.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = hadith.narrator,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hadith.text.isNotBlank()) {
                Text(
                    text = "«${hadith.text}»",
                    fontFamily = AmiriFontFamily,
                    fontSize = 21.sp,
                    lineHeight = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الموضوع / الفائدة: ${hadith.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = hadith.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = NoorTheme.colors.goldBorder)

            // Source + Verification Link Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NoorTheme.colors.badgeEmeraldBg,
                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                onClick = onOpenSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hadith_source_link_${hadith.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المصدر والتخريج",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = hadith.source,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "تحقق",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "فتح رابط المصدر",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نسخ")
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithExplanationBottomSheet(
    hadith: HadithItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSource: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شرح الحديث والتخريج",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) NoorTheme.colors.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = hadith.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = hadith.narrator,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hadith.text.isNotBlank()) {
                NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "«${hadith.text}»",
                        modifier = Modifier.padding(16.dp),
                        fontFamily = AmiriFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }

            Text(
                text = "شرح الحديث والفائدة:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = hadith.explanation,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NoorTheme.colors.badgeEmeraldBg,
                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                onClick = onOpenSource,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "المصدر والدرجة: ${hadith.grade}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hadith.source,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("نسخ")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مشاركة")
                }
            }
        }
    }
}
