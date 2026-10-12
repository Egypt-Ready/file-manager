package com.offline.filemanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileManagerRepository(application)

    private val _currentPath = MutableStateFlow(repository.rootDir.absolutePath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _pathHistory = MutableStateFlow<List<String>>(listOf(repository.rootDir.absolutePath))
    val pathHistory: StateFlow<List<String>> = _pathHistory.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _isRecentMode = MutableStateFlow(false)
    val isRecentMode: StateFlow<Boolean> = _isRecentMode.asStateFlow()

    private val _isTrashMode = MutableStateFlow(false)
    val isTrashMode: StateFlow<Boolean> = _isTrashMode.asStateFlow()

    private val _sortField = MutableStateFlow(SortField.NAME)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedItems: StateFlow<Set<FileItem>> = _selectedItems.asStateFlow()

    private val _clipboard = MutableStateFlow<Clipboard?>(null)
    val clipboard: StateFlow<Clipboard?> = _clipboard.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _trashEntries = MutableStateFlow<List<TrashEntry>>(emptyList())
    val trashEntries: StateFlow<List<TrashEntry>> = _trashEntries.asStateFlow()

    private val _activeDialog = MutableStateFlow<ActiveDialog?>(null)
    val activeDialog: StateFlow<ActiveDialog?> = _activeDialog.asStateFlow()

    // Built-in viewers
    private val _editingFile = MutableStateFlow<FileItem?>(null)
    val editingFile: StateFlow<FileItem?> = _editingFile.asStateFlow()

    private val _editingTextContent = MutableStateFlow("")
    val editingTextContent: StateFlow<String> = _editingTextContent.asStateFlow()

    private val _previewImage = MutableStateFlow<FileItem?>(null)
    val previewImage: StateFlow<FileItem?> = _previewImage.asStateFlow()

    private val _previewAudio = MutableStateFlow<FileItem?>(null)
    val previewAudio: StateFlow<FileItem?> = _previewAudio.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshCurrentView()
        refreshStorageStats()
    }

    fun refreshCurrentView() {
        viewModelScope.launch {
            when {
                _isTrashMode.value -> {
                    _trashEntries.value = repository.getTrashEntries()
                }
                _isRecentMode.value -> {
                    val raw = repository.getRecentFiles()
                    _files.value = sortAndFilter(raw)
                }
                _selectedCategory.value != null -> {
                    val raw = repository.getFilesByCategory(_selectedCategory.value!!)
                    _files.value = sortAndFilter(raw)
                }
                else -> {
                    val raw = repository.getFiles(_currentPath.value)
                    _files.value = sortAndFilter(raw)
                }
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.getStorageStats()
        }
    }

    private fun sortAndFilter(list: List<FileItem>): List<FileItem> {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            list
        } else {
            list.filter { it.name.lowercase().contains(query) }
        }

        val sorted = when (_sortField.value) {
            SortField.NAME -> filtered.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
            SortField.DATE -> filtered.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.lastModified }
            )
            SortField.SIZE -> filtered.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.size }
            )
            SortField.TYPE -> filtered.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.extension }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        return if (_sortOrder.value == SortOrder.DESCENDING) {
            // Keep directories on top even when descending
            val dirs = sorted.filter { it.isDirectory }.reversed()
            val regular = sorted.filter { !it.isDirectory }.reversed()
            dirs + regular
        } else {
            sorted
        }
    }

    fun navigateTo(path: String) {
        _isRecentMode.value = false
        _isTrashMode.value = false
        _selectedCategory.value = null
        _selectedItems.value = emptySet()
        _currentPath.value = path

        val history = _pathHistory.value.toMutableList()
        val existingIndex = history.indexOf(path)
        if (existingIndex >= 0) {
            _pathHistory.value = history.subList(0, existingIndex + 1)
        } else {
            history.add(path)
            _pathHistory.value = history
        }
        refreshCurrentView()
    }

    fun navigateUp(): Boolean {
        if (_isTrashMode.value || _isRecentMode.value || _selectedCategory.value != null) {
            _isTrashMode.value = false
            _isRecentMode.value = false
            _selectedCategory.value = null
            refreshCurrentView()
            return true
        }

        val current = File(_currentPath.value)
        val parent = current.parentFile
        if (parent != null && LocalFileRules.isPathSafe(repository.rootDir.absolutePath, parent.absolutePath)) {
            navigateTo(parent.absolutePath)
            return true
        }
        return false
    }

    fun selectCategory(category: FileCategory?) {
        _selectedCategory.value = category
        _isRecentMode.value = false
        _isTrashMode.value = false
        _selectedItems.value = emptySet()
        refreshCurrentView()
    }

    fun showRecent() {
        _isRecentMode.value = true
        _isTrashMode.value = false
        _selectedCategory.value = null
        _selectedItems.value = emptySet()
        refreshCurrentView()
    }

    fun showTrash() {
        _isTrashMode.value = true
        _isRecentMode.value = false
        _selectedCategory.value = null
        _selectedItems.value = emptySet()
        refreshCurrentView()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        refreshCurrentView()
    }

    fun setSort(field: SortField) {
        if (_sortField.value == field) {
            _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        } else {
            _sortField.value = field
            _sortOrder.value = SortOrder.ASCENDING
        }
        refreshCurrentView()
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun toggleSelectItem(item: FileItem) {
        _selectedItems.update { current ->
            if (current.contains(item)) current - item else current + item
        }
    }

    fun selectAll() {
        _selectedItems.value = _files.value.toSet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun showDialog(dialog: ActiveDialog?) {
        _activeDialog.value = dialog
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = repository.createFolder(_currentPath.value, name)
            result.fold(
                onSuccess = {
                    _userMessage.value = "Folder created: ${it.name}"
                    _activeDialog.value = null
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Error: ${it.message}"
                }
            )
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val result = repository.createFile(_currentPath.value, name)
            result.fold(
                onSuccess = {
                    _userMessage.value = "File created: ${it.name}"
                    _activeDialog.value = null
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Error: ${it.message}"
                }
            )
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = repository.rename(item.path, newName)
            result.fold(
                onSuccess = {
                    _userMessage.value = "Renamed to ${it.name}"
                    _activeDialog.value = null
                    clearSelection()
                    refreshCurrentView()
                },
                onFailure = {
                    _userMessage.value = "Error: ${it.message}"
                }
            )
        }
    }

    fun copySelected() {
        val selected = _selectedItems.value.toList()
        if (selected.isNotEmpty()) {
            _clipboard.value = Clipboard(items = selected, isCut = false)
            _userMessage.value = "${selected.size} items copied"
            clearSelection()
        }
    }

    fun cutSelected() {
        val selected = _selectedItems.value.toList()
        if (selected.isNotEmpty()) {
            _clipboard.value = Clipboard(items = selected, isCut = true)
            _userMessage.value = "${selected.size} items cut"
            clearSelection()
        }
    }

    fun pasteClipboard() {
        val clip = _clipboard.value ?: return
        viewModelScope.launch {
            val result = if (clip.isCut) {
                repository.moveItems(clip.items, _currentPath.value)
            } else {
                repository.copyItems(clip.items, _currentPath.value)
            }

            result.fold(
                onSuccess = { count ->
                    _userMessage.value = "$count items pasted successfully"
                    _clipboard.value = null
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Paste failed: ${it.message}"
                }
            )
        }
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun deleteItems(items: List<FileItem>, permanent: Boolean = false) {
        viewModelScope.launch {
            val result = if (permanent) {
                repository.deletePermanently(items)
            } else {
                repository.moveToTrash(items)
            }

            result.fold(
                onSuccess = { count ->
                    _userMessage.value = if (permanent) "$count items deleted permanently" else "$count items moved to Trash"
                    _activeDialog.value = null
                    clearSelection()
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Delete failed: ${it.message}"
                }
            )
        }
    }

    fun restoreTrashEntry(entry: TrashEntry) {
        viewModelScope.launch {
            repository.restoreTrashEntry(entry).fold(
                onSuccess = {
                    _userMessage.value = "Restored ${entry.fileName}"
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Restore failed: ${it.message}"
                }
            )
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash().fold(
                onSuccess = { count ->
                    _userMessage.value = "Trash emptied ($count items deleted)"
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Failed: ${it.message}"
                }
            )
        }
    }

    fun openItem(item: FileItem) {
        if (item.isDirectory) {
            navigateTo(item.path)
        } else {
            when {
                LocalFileRules.isEditableText(item.name) -> {
                    openTextEditor(item)
                }
                item.category == FileCategory.IMAGES -> {
                    _previewImage.value = item
                }
                item.category == FileCategory.AUDIO -> {
                    _previewAudio.value = item
                    _isAudioPlaying.value = true
                }
                item.category == FileCategory.ARCHIVES && item.extension.equals("zip", ignoreCase = true) -> {
                    extractZip(item)
                }
                else -> {
                    showDetails(item)
                }
            }
        }
    }

    private fun openTextEditor(item: FileItem) {
        viewModelScope.launch {
            val content = repository.readTextFile(item.path)
            _editingTextContent.value = content
            _editingFile.value = item
        }
    }

    fun updateEditingContent(text: String) {
        _editingTextContent.value = text
    }

    fun saveEditingFile() {
        val file = _editingFile.value ?: return
        viewModelScope.launch {
            repository.saveTextFile(file.path, _editingTextContent.value).fold(
                onSuccess = {
                    _userMessage.value = "File saved"
                    _editingFile.value = null
                    refreshCurrentView()
                },
                onFailure = {
                    _userMessage.value = "Failed to save: ${it.message}"
                }
            )
        }
    }

    fun closeTextEditor() {
        _editingFile.value = null
    }

    fun closeImagePreview() {
        _previewImage.value = null
    }

    fun closeAudioPreview() {
        _previewAudio.value = null
        _isAudioPlaying.value = false
    }

    fun toggleAudioPlayback() {
        _isAudioPlaying.value = !_isAudioPlaying.value
    }

    fun showDetails(item: FileItem) {
        viewModelScope.launch {
            val checksum = if (!item.isDirectory) {
                LocalFileRules.calculateChecksum(File(item.path))
            } else ""
            _activeDialog.value = ActiveDialog.FileDetails(item, checksum)
        }
    }

    fun zipSelected(zipName: String) {
        val items = _selectedItems.value.toList()
        if (items.isEmpty()) return
        viewModelScope.launch {
            val result = repository.compressToZip(items, zipName, _currentPath.value)
            result.fold(
                onSuccess = {
                    _userMessage.value = "Compressed to ${it.name}"
                    _activeDialog.value = null
                    clearSelection()
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Zip error: ${it.message}"
                }
            )
        }
    }

    fun extractZip(item: FileItem) {
        viewModelScope.launch {
            val destName = item.name.removeSuffix(".zip")
            val destFolder = File(File(item.path).parentFile ?: repository.rootDir, destName).absolutePath
            val result = repository.extractZip(item.path, destFolder)
            result.fold(
                onSuccess = { count ->
                    _userMessage.value = "Extracted $count files to $destName"
                    refreshCurrentView()
                    refreshStorageStats()
                },
                onFailure = {
                    _userMessage.value = "Extract error: ${it.message}"
                }
            )
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
