package com.offline.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun BreadcrumbBar(
    rootPath: String,
    currentPath: String,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = buildSegments(rootPath, currentPath)

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("breadcrumb_bar")
    ) {
        itemsIndexed(segments) { index, segment ->
            val isLast = index == segments.size - 1

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onNavigateTo(segment.path) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                if (index == 0) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = segment.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

private data class PathSegment(val displayName: String, val path: String)

private fun buildSegments(rootPath: String, currentPath: String): List<PathSegment> {
    val list = mutableListOf<PathSegment>()
    list.add(PathSegment("Internal", rootPath))

    if (currentPath == rootPath) {
        return list
    }

    val relative = currentPath.removePrefix(rootPath).trimStart(File.separatorChar)
    if (relative.isEmpty()) return list

    val parts = relative.split(File.separator)
    var accumulated = rootPath
    for (part in parts) {
        accumulated += File.separator + part
        list.add(PathSegment(part, accumulated))
    }
    return list
}
