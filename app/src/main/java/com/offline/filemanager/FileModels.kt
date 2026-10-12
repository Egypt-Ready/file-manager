package com.offline.filemanager

enum class FileCategory(val label: String) {
    DOCUMENTS("Documents"),
    IMAGES("Images"),
    AUDIO("Audio"),
    VIDEOS("Videos"),
    ARCHIVES("Archives"),
    OTHER("Other")
}

enum class SortField {
    NAME,
    DATE,
    SIZE,
    TYPE
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class ViewMode {
    LIST,
    GRID
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val category: FileCategory,
    val itemCount: Int = 0,
    val mimeType: String = ""
)

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categorySizes: Map<FileCategory, Long> = emptyMap(),
    val totalFileCount: Int = 0
)

data class Clipboard(
    val items: List<FileItem>,
    val isCut: Boolean
)

data class TrashEntry(
    val id: String,
    val originalPath: String,
    val trashPath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val size: Long,
    val deletedAt: Long
)

sealed class ActiveDialog {
    data class CreateFolder(val parentPath: String) : ActiveDialog()
    data class CreateFile(val parentPath: String) : ActiveDialog()
    data class Rename(val item: FileItem) : ActiveDialog()
    data class DeleteConfirm(val items: List<FileItem>, val permanent: Boolean = false) : ActiveDialog()
    data class FileDetails(val item: FileItem, val checksum: String = "") : ActiveDialog()
    data class ZipItems(val items: List<FileItem>, val defaultName: String) : ActiveDialog()
}
