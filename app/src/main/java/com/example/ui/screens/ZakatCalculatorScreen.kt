package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ExternalUrlHelper
import com.example.zakat.ZakatInput
import com.example.zakat.ZakatResult
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ZakatCalculatorScreen(
    input: ZakatInput,
    result: ZakatResult,
    onUpdateInput: ((ZakatInput) -> ZakatInput) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cashText by remember { mutableStateOf("") }
    var gold24Text by remember { mutableStateOf("") }
    var gold21Text by remember { mutableStateOf("") }
    var gold18Text by remember { mutableStateOf("") }
    var silverText by remember { mutableStateOf("") }
    var investText by remember { mutableStateOf("") }
    var debtText by remember { mutableStateOf("") }
    var goldPriceText by remember { mutableStateOf(input.gold24kPricePerGram.toInt().toString()) }
    var silverPriceText by remember { mutableStateOf(input.silverPricePerGram.toInt().toString()) }

    val fmt = remember { NumberFormat.getNumberInstance(Locale("ar", "EG")).apply { maximumFractionDigits = 2 } }

    fun buildShareSummary(): String {
        return buildString {
            appendLine("حساب زكاة المال — محراب النور")
            appendLine("إجمالي الممتلكات: ${fmt.format(result.totalWealth)}")
            appendLine("الوعاء الزكوي الصافي: ${fmt.format(result.netZakatableWealth)}")
            appendLine("قيمة النصاب الشرعي: ${fmt.format(result.nisabThreshold)}")
            if (result.isZakatDue) {
                appendLine("مقدار الزكاة الواجبة (٢.٥٪): ${fmt.format(result.zakatAmountDue)}")
            } else {
                appendLine("الحكم: لم يبلغ المال النصاب الشرعي بعد.")
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            EmeraldOrnamentalHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("zakat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = NoorTheme.colors.onHeaderForest
                            )
                        }
                        Column {
                            Text(
                                text = "حاسبة الزكاة",
                                fontFamily = AmiriFontFamily,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = NoorTheme.colors.onHeaderForest
                            )
                            Text(
                                text = "النقد والذهب والفضة والاستثمارات (٢.٥٪)",
                                style = MaterialTheme.typography.labelSmall,
                                color = NoorTheme.colors.goldAccent
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            cashText = ""
                            gold24Text = ""
                            gold21Text = ""
                            gold18Text = ""
                            silverText = ""
                            investText = ""
                            debtText = ""
                            onReset()
                        },
                        modifier = Modifier.testTag("zakat_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "تصفير الحقول",
                            tint = NoorTheme.colors.onHeaderForest
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Result Summary Card
                item {
                    NoorGoldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zakat_result_card"),
                        containerColor = if (result.isZakatDue) {
                            NoorTheme.colors.badgeEmeraldBg
                        } else {
                            NoorTheme.colors.cardElevated
                        },
                        borderColor = if (result.isZakatDue) {
                            NoorTheme.colors.goldAccent
                        } else {
                            NoorTheme.colors.goldBorder
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmeraldPillBadge(
                                    text = if (result.isZakatDue) "بلغ النصاب — تجب الزكاة ✓" else "لم يبلغ النصاب"
                                )
                                Icon(
                                    imageVector = Icons.Filled.Calculate,
                                    contentDescription = null,
                                    tint = NoorTheme.colors.goldAccent
                                )
                            }

                            Text(
                                text = "الزكاة الواجبة: ${fmt.format(result.zakatAmountDue)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(color = NoorTheme.colors.goldBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "صافي الوعاء الزكوي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        fmt.format(result.netZakatableWealth),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "قيمة النصاب الشرعي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        fmt.format(result.nisabThreshold),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Zakat", buildShareSummary()))
                                        Toast.makeText(context, "تم نسخ نتيجة الحساب", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("نسخ")
                                }
                                Button(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, buildShareSummary())
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة حساب الزكاة"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("مشاركة")
                                }
                            }
                        }
                    }
                }

                // Nisab basis & gram prices
                item {
                    NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "معيار النصاب وأسعار الجرام (بعملتك المحلية)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = input.nisabBasisIsGold,
                                    onClick = { onUpdateInput { it.copy(nisabBasisIsGold = true) } },
                                    label = { Text("نصاب الذهب (٨٥ جم)") },
                                    modifier = Modifier.testTag("zakat_nisab_gold_chip")
                                )
                                FilterChip(
                                    selected = !input.nisabBasisIsGold,
                                    onClick = { onUpdateInput { it.copy(nisabBasisIsGold = false) } },
                                    label = { Text("نصاب الفضة (٥٩٥ جم)") },
                                    modifier = Modifier.testTag("zakat_nisab_silver_chip")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ZakatNumberField(
                                    label = "سعر جرام الذهب ٢٤",
                                    value = goldPriceText,
                                    onValueChange = { txt ->
                                        goldPriceText = txt
                                        val parsed = txt.toDoubleOrNull() ?: 0.0
                                        onUpdateInput { it.copy(gold24kPricePerGram = parsed) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ZakatNumberField(
                                    label = "سعر جرام الفضة",
                                    value = silverPriceText,
                                    onValueChange = { txt ->
                                        silverPriceText = txt
                                        val parsed = txt.toDoubleOrNull() ?: 0.0
                                        onUpdateInput { it.copy(silverPricePerGram = parsed) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Wealth inputs
                item {
                    NoorGoldCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "بيانات الممتلكات والمدخرات (حول كامل)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ZakatNumberField(
                                label = "النقد والمدخرات البنكية",
                                value = cashText,
                                onValueChange = { txt ->
                                    cashText = txt
                                    val parsed = txt.toDoubleOrNull() ?: 0.0
                                    onUpdateInput { it.copy(cashAndSavings = parsed) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("zakat_cash_field")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ZakatNumberField(
                                    label = "ذهب عيار ٢٤ (جم)",
                                    value = gold24Text,
                                    onValueChange = { txt ->
                                        gold24Text = txt
                                        val parsed = txt.toDoubleOrNull() ?: 0.0
                                        onUpdateInput { it.copy(gold24kGrams = parsed) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ZakatNumberField(
                                    label = "ذهب عيار ٢١ (جم)",
                                    value = gold21Text,
                                    onValueChange = { txt ->
                                        gold21Text = txt
                                        val parsed = txt.toDoubleOrNull() ?: 0.0
                                        onUpdateInput { it.copy(gold21kGrams = parsed) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ZakatNumberField(
                                    label = "ذهب عيار ١٨ (جم)",
                                    value = gold18Text,
                                    onValueChange = { txt ->
                                        gold18Text = txt
                                        val parsed = txt.toDoubleOrNull() ?: 0.0
                                        onUpdateInput { it.copy(gold18kGrams = parsed) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ZakatNumberField(
                                    label = "الفضة (جم)",
                                    value = silverText,
                                    onValueChange = { txt ->
                                        silverText = txt
                                        val parsed = txt.toDoubleOrNull() ?: 0.0
                                        onUpdateInput { it.copy(silverGrams = parsed) }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ZakatNumberField(
                                label = "الأسهم وعروض التجارة والاستثمارات",
                                value = investText,
                                onValueChange = { txt ->
                                    investText = txt
                                    val parsed = txt.toDoubleOrNull() ?: 0.0
                                    onUpdateInput { it.copy(investmentsValue = parsed) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ZakatNumberField(
                                label = "يُخصم: الديون والالتزامات الحالة",
                                value = debtText,
                                onValueChange = { txt ->
                                    debtText = txt
                                    val parsed = txt.toDoubleOrNull() ?: 0.0
                                    onUpdateInput { it.copy(immediateDebts = parsed) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Fiqh Reference Verification Link
                item {
                    Surface(
                        onClick = {
                            ExternalUrlHelper.openVerificationUrl(
                                context,
                                "https://www.islamweb.net/ar/fatwa/2055/"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = NoorTheme.colors.badgeEmeraldBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zakat_source_link")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                Text(
                                    text = "المرجع الشرعي: أحكام نصاب الذهب والفضة (إسلام ويب)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "فتح المرجع",
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZakatNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' }
            onValueChange(filtered)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}
