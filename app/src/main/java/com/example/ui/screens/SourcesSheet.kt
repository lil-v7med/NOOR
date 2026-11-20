package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SourcesCatalog
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.NoorTheme
import com.example.util.ExternalUrlHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = NoorTheme.colors.goldAccent
                )
                Text(
                    text = "المصادر والمراجع المعتمدة",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "نلتزم بتوثيق كافة النصوص القرآنية والتفسير والأذكار والأحاديث والتلاوات مع روابط مباشرة للتحقق من صحتها.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SourcesCatalog.references.forEachIndexed { index, ref ->
                NoorGoldCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("source_ref_card_$index"),
                    onClick = { ExternalUrlHelper.openVerificationUrl(context, ref.url) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ref.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "فتح الرابط",
                                tint = NoorTheme.colors.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        EmeraldPillBadge(text = ref.publisher)
                        Text(
                            text = ref.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { ExternalUrlHelper.openVerificationUrl(context, ref.url) }
                        ) {
                            Text("فتح المصدر للتحقق")
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            NoorGoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_security_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "الخصوصية والأمان ومعلومات النشر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    EmeraldPillBadge(text = "خصوصية كاملة ١٠٠٪ • بدون إعلانات")
                    Text(
                        text = "• تُحفظ بياناتك (العلامات المرجعية، الأذكار المخصصة، مفضلة الأحاديث، وسجل الصلوات والختمة) محلياً بالكامل على جهازك باستخدام Room وDataStore.\n" +
                            "• لا يجمع التطبيق أي بيانات شخصية ولا يتضمن إعلانات أو تتبعاً.\n" +
                            "• النسخ الاحتياطي يتم عبر مستكشف النظام الآمن (SAF) دون طلب أذونات وصول شاملة للتخزين.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
