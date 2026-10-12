package com.offline.filemanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline.filemanager.FileCategory
import com.offline.filemanager.FileItem
import com.offline.filemanager.LocalFileRules
import com.offline.filemanager.ui.theme.ArchiveEmerald
import com.offline.filemanager.ui.theme.AudioPink
import com.offline.filemanager.ui.theme.DocBlue
import com.offline.filemanager.ui.theme.FolderAmber
import com.offline.filemanager.ui.theme.ImagePurple
import com.offline.filemanager.ui.theme.VideoRed

@Composable
fun getFileIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) {
        return Pair(Icons.Default.Folder, FolderAmber)
    }
    return when (item.category) {
        FileCategory.DOCUMENTS -> Pair(Icons.Default.Description, DocBlue)
        FileCategory.IMAGES -> Pair(Icons.Default.Image, ImagePurple)
        FileCategory.AUDIO -> Pair(Icons.Default.Audiotrack, AudioPink)
        FileCategory.VIDEOS -> Pair(Icons.Default.Videocam, VideoRed)
        FileCategory.ARCHIVES -> Pair(Icons.Default.Inventory2, ArchiveEmerald)
        FileCategory.OTHER -> Pair(Icons.Default.Description, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onZip: () -> Unit,
    onExtract: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val (icon, iconColor) = getFileIconAndColor(item)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_item_row_${item.name}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Select",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (item.isDirectory) {
                    "${item.itemCount} items • ${LocalFileRules.formatDate(item.lastModified)}"
                } else {
                    "${LocalFileRules.formatFileSize(item.size)} • ${LocalFileRules.formatDate(item.lastModified)}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontSize = 12.sp
                )
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FileActionDropdownMenu(
                        expanded = menuExpanded,
                        item = item,
                        onDismiss = { menuExpanded = false },
                        onRename = { menuExpanded = false; onRename() },
                        onCopy = { menuExpanded = false; onCopy() },
                        onCut = { menuExpanded = false; onCut() },
                        onDelete = { menuExpanded = false; onDelete() },
                        onDetails = { menuExpanded = false; onDetails() },
                        onShare = { menuExpanded = false; onShare() },
                        onZip = { menuExpanded = false; onZip() },
                        onExtract = { menuExpanded = false; onExtract() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGridCard(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onZip: () -> Unit,
    onExtract: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val (icon, iconColor) = getFileIconAndColor(item)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = modifier
            .aspectRatio(0.95f)
            .testTag("file_item_grid_${item.name}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (item.isDirectory) "${item.itemCount} items" else LocalFileRules.formatFileSize(item.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontSize = 11.sp
                )
            }

            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FileActionDropdownMenu(
                        expanded = menuExpanded,
                        item = item,
                        onDismiss = { menuExpanded = false },
                        onRename = { menuExpanded = false; onRename() },
                        onCopy = { menuExpanded = false; onCopy() },
                        onCut = { menuExpanded = false; onCut() },
                        onDelete = { menuExpanded = false; onDelete() },
                        onDetails = { menuExpanded = false; onDetails() },
                        onShare = { menuExpanded = false; onShare() },
                        onZip = { menuExpanded = false; onZip() },
                        onExtract = { menuExpanded = false; onExtract() }
                    )
                }
            }
        }
    }
}

@Composable
fun FileActionDropdownMenu(
    expanded: Boolean,
    item: FileItem,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onZip: () -> Unit,
    onExtract: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Details") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = onDetails
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = onRename
        )
        DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            onClick = onCopy
        )
        DropdownMenuItem(
            text = { Text("Cut") },
            leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
            onClick = onCut
        )
        if (!item.isDirectory) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = onShare
            )
        }
        if (item.category == FileCategory.ARCHIVES && item.extension.equals("zip", ignoreCase = true)) {
            DropdownMenuItem(
                text = { Text("Extract ZIP") },
                leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) },
                onClick = onExtract
            )
        } else {
            DropdownMenuItem(
                text = { Text("Compress to ZIP") },
                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                onClick = onZip
            )
        }
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = onDelete
        )
    }
}
