package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ShaarawyPlayerState
import com.example.data.model.ShaarawyLectureItem
import com.example.ui.components.EmeraldOrnamentalHeader
import com.example.ui.components.EmeraldPillBadge
import com.example.ui.components.NoorGoldCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.NoorTheme
import com.example.util.ExternalUrlHelper

@Composable
fun ShaarawyScreen(
    lectures: List<ShaarawyLectureItem>,
    categories: List<String>,
    selectedCategory: String,
    playerState: ShaarawyPlayerState,
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onPlayLecture: (ShaarawyLectureItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStopAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (playerState.isActive) {
                ShaarawyPlayerBar(
                    playerState = playerState,
                    onTogglePlayPause = onTogglePlayPause,
                    onStop = onStopAudio
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EmeraldOrnamentalHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("shaarawy_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = NoorTheme.colors.onHeaderForest
                            )
                        }
                        Column {
                            Text(
                                text = "خواطر الشيخ الشعراوي",
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NoorTheme.colors.onHeaderForest
                            )
                            Text(
                                text = "الخطب والخواطر القرآنية الإيمانية",
                                style = MaterialTheme.typography.bodySmall,
                                color = NoorTheme.colors.goldAccent
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NoorTheme.colors.goldAccent,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = NoorTheme.colors.headerForest,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
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
                        modifier = Modifier.testTag("shaarawy_cat_$category")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lectures, key = { it.id }) { lecture ->
                    val isPlayingThis = playerState.isActive && playerState.lectureTitle == lecture.title
                    ShaarawyLectureCard(
                        lecture = lecture,
                        isPlaying = isPlayingThis,
                        onPlay = { onPlayLecture(lecture) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ShaarawyLectureCard(
    lecture: ShaarawyLectureItem,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    NoorGoldCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shaarawy_card_${lecture.id}"),
        onClick = onPlay,
        containerColor = if (isPlaying) NoorTheme.colors.activeAyahHighlight else NoorTheme.colors.cardElevated,
        borderColor = if (isPlaying) NoorTheme.colors.goldAccent else NoorTheme.colors.goldBorder
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmeraldPillBadge(text = lecture.surahName)
                    EmeraldPillBadge(text = lecture.duration)
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = lecture.speaker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NoorTheme.colors.badgeEmeraldBg,
                border = BorderStroke(1.dp, NoorTheme.colors.goldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "المصدر: ${lecture.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ShaarawyPlayerBar(
    playerState: ShaarawyPlayerState,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = NoorTheme.colors.headerForest,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, NoorTheme.colors.goldAccent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.lectureTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = NoorTheme.colors.onHeaderForest
                    )
                    Text(
                        text = playerState.lectureSource,
                        style = MaterialTheme.typography.labelSmall,
                        color = NoorTheme.colors.goldAccent
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NoorTheme.colors.goldAccent,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(onClick = onTogglePlayPause) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = NoorTheme.colors.headerForest
                            )
                        }
                    }
                    IconButton(onClick = onStop) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = NoorTheme.colors.onHeaderForest)
                    }
                }
            }
        }
    }
}
