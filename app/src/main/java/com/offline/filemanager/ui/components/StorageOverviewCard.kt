package com.offline.filemanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.filemanager.FileCategory
import com.offline.filemanager.LocalFileRules
import com.offline.filemanager.StorageStats
import com.offline.filemanager.ui.theme.ArchiveEmerald
import com.offline.filemanager.ui.theme.AudioPink
import com.offline.filemanager.ui.theme.DocBlue
import com.offline.filemanager.ui.theme.ImagePurple
import com.offline.filemanager.ui.theme.VideoRed

data class CategoryMeta(
    val category: FileCategory,
    val icon: ImageVector,
    val color: Color,
    val title: String
)

val CATEGORY_METAS = listOf(
    CategoryMeta(FileCategory.DOCUMENTS, Icons.Default.Description, DocBlue, "Docs"),
    CategoryMeta(FileCategory.IMAGES, Icons.Default.Image, ImagePurple, "Images"),
    CategoryMeta(FileCategory.AUDIO, Icons.Default.Audiotrack, AudioPink, "Audio"),
    CategoryMeta(FileCategory.VIDEOS, Icons.Default.Videocam, VideoRed, "Videos"),
    CategoryMeta(FileCategory.ARCHIVES, Icons.Default.Inventory2, ArchiveEmerald, "Archives")
)

@Composable
fun StorageOverviewCard(
    stats: StorageStats,
    selectedCategory: FileCategory?,
    isRecentSelected: Boolean,
    isTrashSelected: Boolean,
    onCategoryClick: (FileCategory?) -> Unit,
    onRecentClick: () -> Unit,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (stats.totalBytes > 0) {
        (stats.usedBytes.toFloat() / stats.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "storage_progress")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Storage",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Internal Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${LocalFileRules.formatFileSize(stats.usedBytes)} / ${LocalFileRules.formatFileSize(stats.totalBytes)} used",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category & Action Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAll = selectedCategory == null && !isRecentSelected && !isTrashSelected
                    FilterChipButton(
                        title = "All Files",
                        icon = null,
                        color = MaterialTheme.colorScheme.primary,
                        isSelected = isAll,
                        onClick = { onCategoryClick(null) },
                        testTag = "filter_chip_all"
                    )
                }

                items(CATEGORY_METAS) { meta ->
                    val isSelected = selectedCategory == meta.category
                    val size = stats.categorySizes[meta.category] ?: 0L
                    FilterChipButton(
                        title = "${meta.title} (${LocalFileRules.formatFileSize(size)})",
                        icon = meta.icon,
                        color = meta.color,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) onCategoryClick(null) else onCategoryClick(meta.category)
                        },
                        testTag = "filter_chip_${meta.category.name.lowercase()}"
                    )
                }

                item {
                    FilterChipButton(
                        title = "Recent",
                        icon = Icons.Default.History,
                        color = MaterialTheme.colorScheme.secondary,
                        isSelected = isRecentSelected,
                        onClick = onRecentClick,
                        testTag = "filter_chip_recent"
                    )
                }

                item {
                    FilterChipButton(
                        title = "Trash",
                        icon = Icons.Default.DeleteSweep,
                        color = VideoRed,
                        isSelected = isTrashSelected,
                        onClick = onTrashClick,
                        testTag = "filter_chip_trash"
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipButton(
    title: String,
    icon: ImageVector?,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bg = if (isSelected) color else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
