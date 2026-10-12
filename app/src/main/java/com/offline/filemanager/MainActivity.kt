package com.offline.filemanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offline.filemanager.ActiveDialog
import com.offline.filemanager.FileItem
import com.offline.filemanager.LocalFileRules
import com.offline.filemanager.SortField
import com.offline.filemanager.SortOrder
import com.offline.filemanager.ViewMode
import com.offline.filemanager.ui.components.AudioPreviewDialog
import com.offline.filemanager.ui.components.BreadcrumbBar
import com.offline.filemanager.ui.components.FileActionDialogHost
import com.offline.filemanager.ui.components.FileItemGridCard
import com.offline.filemanager.ui.components.FileItemRow
import com.offline.filemanager.ui.components.ImagePreviewDialog
import com.offline.filemanager.ui.components.StorageOverviewCard
import com.offline.filemanager.ui.components.TextEditorScreen
import com.offline.filemanager.ui.components.TrashBinContent
import com.offline.filemanager.ui.theme.FileManagerTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FileManagerTheme {
                FileManagerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FileManagerApp(viewModel: FileManagerViewModel) {
    val context = LocalContext.current
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isRecentMode by viewModel.isRecentMode.collectAsStateWithLifecycle()
    val isTrashMode by viewModel.isTrashMode.collectAsStateWithLifecycle()
    val sortField by viewModel.sortField.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val trashEntries by viewModel.trashEntries.collectAsStateWithLifecycle()
    val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()

    val editingFile by viewModel.editingFile.collectAsStateWithLifecycle()
    val editingTextContent by viewModel.editingTextContent.collectAsStateWithLifecycle()
    val previewImage by viewModel.previewImage.collectAsStateWithLifecycle()
    val previewAudio by viewModel.previewAudio.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Handle back button
    BackHandler(enabled = true) {
        if (selectedItems.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (editingFile != null) {
            viewModel.closeTextEditor()
        } else if (previewImage != null) {
            viewModel.closeImagePreview()
        } else if (previewAudio != null) {
            viewModel.closeAudioPreview()
        } else {
            val handled = viewModel.navigateUp()
            if (!handled) {
                // Exit app or let system handle
            }
        }
    }

    // Fullscreen Text Editor view if active
    if (editingFile != null) {
        TextEditorScreen(
            fileItem = editingFile!!,
            textContent = editingTextContent,
            onContentChange = { viewModel.updateEditingContent(it) },
            onSave = { viewModel.saveEditingFile() },
            onClose = { viewModel.closeTextEditor() }
        )
        return
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isAddMenuOpen by remember { mutableStateOf(false) }

    val shareFile: (FileItem) -> Unit = { item ->
        try {
            val file = File(item.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifEmpty { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${item.name}"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!isTrashMode && selectedItems.isEmpty()) {
                Box {
                    FloatingActionButton(
                        onClick = { isAddMenuOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }

                    DropdownMenu(
                        expanded = isAddMenuOpen,
                        onDismissRequest = { isAddMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            onClick = {
                                isAddMenuOpen = false
                                viewModel.showDialog(ActiveDialog.CreateFolder(currentPath))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New File") },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            onClick = {
                                isAddMenuOpen = false
                                viewModel.showDialog(ActiveDialog.CreateFile(currentPath))
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header / App Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "File Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Search toggle
                            IconButton(
                                onClick = {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) viewModel.setSearchQuery("")
                                },
                                modifier = Modifier.testTag("toggle_search_btn")
                            ) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }

                            // View mode toggle (List / Grid)
                            IconButton(
                                onClick = { viewModel.toggleViewMode() },
                                modifier = Modifier.testTag("toggle_view_mode_btn")
                            ) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = "Toggle View"
                                )
                            }

                            // Sort button
                            Box {
                                IconButton(
                                    onClick = { isSortMenuOpen = true },
                                    modifier = Modifier.testTag("sort_menu_btn")
                                ) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }

                                DropdownMenu(
                                    expanded = isSortMenuOpen,
                                    onDismissRequest = { isSortMenuOpen = false }
                                ) {
                                    SortMenuItem("Name", SortField.NAME, sortField, sortOrder) {
                                        isSortMenuOpen = false
                                        viewModel.setSort(SortField.NAME)
                                    }
                                    SortMenuItem("Date", SortField.DATE, sortField, sortOrder) {
                                        isSortMenuOpen = false
                                        viewModel.setSort(SortField.DATE)
                                    }
                                    SortMenuItem("Size", SortField.SIZE, sortField, sortOrder) {
                                        isSortMenuOpen = false
                                        viewModel.setSort(SortField.SIZE)
                                    }
                                    SortMenuItem("Type", SortField.TYPE, sortField, sortOrder) {
                                        isSortMenuOpen = false
                                        viewModel.setSort(SortField.TYPE)
                                    }
                                }
                            }
                        }
                    }

                    // Search input field when active
                    androidx.compose.animation.AnimatedVisibility(visible = isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search files & folders...") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("search_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                }
            }

            // Storage Overview Card
            StorageOverviewCard(
                stats = storageStats,
                selectedCategory = selectedCategory,
                isRecentSelected = isRecentMode,
                isTrashSelected = isTrashMode,
                onCategoryClick = { viewModel.selectCategory(it) },
                onRecentClick = { viewModel.showRecent() },
                onTrashClick = { viewModel.showTrash() },
                modifier = Modifier.padding(12.dp)
            )

            // Breadcrumb path navigation (hidden in Trash/Recent/Category mode)
            if (!isTrashMode && !isRecentMode && selectedCategory == null) {
                BreadcrumbBar(
                    rootPath = File(context.filesDir, "storage").absolutePath,
                    currentPath = currentPath,
                    onNavigateTo = { viewModel.navigateTo(it) }
                )
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isTrashMode -> {
                        TrashBinContent(
                            entries = trashEntries,
                            onRestore = { viewModel.restoreTrashEntry(it) },
                            onEmptyTrash = { viewModel.emptyTrash() },
                            onExitTrash = { viewModel.selectCategory(null) }
                        )
                    }
                    files.isEmpty() -> {
                        EmptyStateView(
                            searchQuery = searchQuery,
                            onCreateFolder = { viewModel.showDialog(ActiveDialog.CreateFolder(currentPath)) },
                            onCreateFile = { viewModel.showDialog(ActiveDialog.CreateFile(currentPath)) }
                        )
                    }
                    viewMode == ViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("file_list_column")
                        ) {
                            items(files, key = { it.path }) { item ->
                                val isSelected = selectedItems.contains(item)
                                FileItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = selectedItems.isNotEmpty(),
                                    onClick = {
                                        if (selectedItems.isNotEmpty()) {
                                            viewModel.toggleSelectItem(item)
                                        } else {
                                            viewModel.openItem(item)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelectItem(item) },
                                    onRename = { viewModel.showDialog(ActiveDialog.Rename(item)) },
                                    onCopy = {
                                        viewModel.toggleSelectItem(item)
                                        viewModel.copySelected()
                                    },
                                    onCut = {
                                        viewModel.toggleSelectItem(item)
                                        viewModel.cutSelected()
                                    },
                                    onDelete = { viewModel.showDialog(ActiveDialog.DeleteConfirm(listOf(item))) },
                                    onDetails = { viewModel.showDetails(item) },
                                    onShare = { shareFile(item) },
                                    onZip = {
                                        viewModel.showDialog(
                                            ActiveDialog.ZipItems(listOf(item), "${item.name.substringBeforeLast('.')}.zip")
                                        )
                                    },
                                    onExtract = { viewModel.extractZip(item) }
                                )
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("file_grid_view")
                        ) {
                            items(files, key = { it.path }) { item ->
                                val isSelected = selectedItems.contains(item)
                                FileItemGridCard(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = selectedItems.isNotEmpty(),
                                    onClick = {
                                        if (selectedItems.isNotEmpty()) {
                                            viewModel.toggleSelectItem(item)
                                        } else {
                                            viewModel.openItem(item)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelectItem(item) },
                                    onRename = { viewModel.showDialog(ActiveDialog.Rename(item)) },
                                    onCopy = {
                                        viewModel.toggleSelectItem(item)
                                        viewModel.copySelected()
                                    },
                                    onCut = {
                                        viewModel.toggleSelectItem(item)
                                        viewModel.cutSelected()
                                    },
                                    onDelete = { viewModel.showDialog(ActiveDialog.DeleteConfirm(listOf(item))) },
                                    onDetails = { viewModel.showDetails(item) },
                                    onShare = { shareFile(item) },
                                    onZip = {
                                        viewModel.showDialog(
                                            ActiveDialog.ZipItems(listOf(item), "${item.name.substringBeforeLast('.')}.zip")
                                        )
                                    },
                                    onExtract = { viewModel.extractZip(item) }
                                )
                            }
                        }
                    }
                }

                // Multi-select Bottom Action Bar
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedItems.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("multi_select_action_bar")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedItems.size} selected",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row {
                                        TextButton(onClick = { viewModel.selectAll() }) {
                                            Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Select All")
                                        }
                                        TextButton(onClick = { viewModel.clearSelection() }) {
                                            Text("Clear")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MultiActionButton("Copy", Icons.Default.ContentCopy) { viewModel.copySelected() }
                                    MultiActionButton("Cut", Icons.Default.ContentCut) { viewModel.cutSelected() }
                                    MultiActionButton("ZIP", Icons.Default.Inventory2) {
                                        viewModel.showDialog(ActiveDialog.ZipItems(selectedItems.toList(), "Archive.zip"))
                                    }
                                    MultiActionButton("Delete", Icons.Default.Delete, isDestructive = true) {
                                        viewModel.showDialog(ActiveDialog.DeleteConfirm(selectedItems.toList()))
                                    }
                                }
                            }
                        }
                    }
                }

                // Clipboard Paste Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = clipboard != null && selectedItems.isEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        clipboard?.let { clip ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("clipboard_paste_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (clip.isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${clip.items.size} item(s) ready",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Row {
                                        TextButton(onClick = { viewModel.clearClipboard() }) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = { viewModel.pasteClipboard() },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("paste_confirm_btn")
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paste")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs Host
    FileActionDialogHost(
        activeDialog = activeDialog,
        onDismiss = { viewModel.showDialog(null) },
        onCreateFolder = { viewModel.createFolder(it) },
        onCreateFile = { viewModel.createFile(it) },
        onRename = { item, newName -> viewModel.renameItem(item, newName) },
        onDelete = { items, permanent -> viewModel.deleteItems(items, permanent) },
        onZip = { viewModel.zipSelected(it) }
    )

    // Image Preview Dialog
    previewImage?.let { imgItem ->
        ImagePreviewDialog(
            fileItem = imgItem,
            onClose = { viewModel.closeImagePreview() }
        )
    }

    // Audio Preview Dialog
    previewAudio?.let { audioItem ->
        AudioPreviewDialog(
            fileItem = audioItem,
            isPlaying = isAudioPlaying,
            onTogglePlay = { viewModel.toggleAudioPlayback() },
            onClose = { viewModel.closeAudioPreview() }
        )
    }
}

@Composable
fun MultiActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun SortMenuItem(
    label: String,
    field: SortField,
    currentField: SortField,
    currentOrder: SortOrder,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontWeight = if (field == currentField) FontWeight.Bold else FontWeight.Normal)
                if (field == currentField) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (currentOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
fun EmptyStateView(
    searchQuery: String,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CreateNewFolder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "No files found for \"$searchQuery\"" else "This folder is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (searchQuery.isNotEmpty()) "Try a different search query" else "Create a new folder or file to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    OutlinedButton(onClick = onCreateFolder) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Folder")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(onClick = onCreateFile) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New File")
                    }
                }
            }
        }
    }
}
