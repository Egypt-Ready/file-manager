package com.offline.filemanager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileManagerRepository(private val context: Context) {

    val rootDir: File = File(context.filesDir, "storage").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val trashDir: File = File(context.filesDir, ".trash").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val trashMetadataFile: File = File(context.filesDir, ".trash_meta.csv")

    init {
        ensureInitialSeed()
    }

    private fun ensureInitialSeed() {
        val seededMarker = File(context.filesDir, ".seeded")
        if (!seededMarker.exists()) {
            val docs = File(rootDir, "Documents").apply { mkdirs() }
            val images = File(rootDir, "Pictures").apply { mkdirs() }
            val audio = File(rootDir, "Music").apply { mkdirs() }
            val downloads = File(rootDir, "Downloads").apply { mkdirs() }
            val archives = File(rootDir, "Archives").apply { mkdirs() }

            File(docs, "Project_Overview.md").writeText(
                "# Project Overview\n\nWelcome to your modern Android File Manager!\n\n## Features\n- Fast file browsing\n- Media previews & Text Editor\n- ZIP compression & extraction\n- Safe trash bin recovery\n- Storage analysis & sorting\n"
            )
            File(docs, "Notes.txt").writeText(
                "Tasks for today:\n1. Check storage usage\n2. Backup important documents to ZIP\n3. Clean temporary files\n"
            )
            File(downloads, "User_Manual.txt").writeText(
                "Offline File Manager Quick Manual\nTap on any folder to open.\nUse long press or multi-select to copy, cut, or delete.\nTap on text files to open the built-in editor.\n"
            )

            // Create a small placeholder sample file in Pictures
            File(images, "Sample_Photo.jpg").writeBytes(createSampleJpegBytes())
            // Create a small placeholder sample audio file
            File(audio, "Sample_Audio.mp3").writeBytes(createSampleAudioBytes())
            // Create a sample zip file in Archives
            createInitialZip(File(archives, "Backup_Starter.zip"))

            seededMarker.createNewFile()
        }
    }

    private fun createSampleJpegBytes(): ByteArray {
        // Minimal valid JPEG stream header
        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(),
            'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(),
            0x43.toByte(), 0x00.toByte(), 0x08.toByte(), 0x06.toByte(), 0x06.toByte(), 0x07.toByte(),
            0x06.toByte(), 0x05.toByte(), 0x08.toByte(), 0x07.toByte(), 0x07.toByte(), 0x07.toByte(),
            0x09.toByte(), 0x09.toByte(), 0x08.toByte(), 0x0A.toByte(), 0x0C.toByte(), 0x14.toByte(),
            0x0D.toByte(), 0x0C.toByte(), 0x0B.toByte(), 0x0B.toByte(), 0x0C.toByte(), 0x19.toByte(),
            0x12.toByte(), 0x13.toByte(), 0x0F.toByte(), 0x14.toByte(), 0x1D.toByte(), 0x1A.toByte(),
            0x1F.toByte(), 0x1E.toByte(), 0x1D.toByte(), 0x1A.toByte(), 0x1C.toByte(), 0x1C.toByte(),
            0x20.toByte(), 0x24.toByte(), 0x2E.toByte(), 0x27.toByte(), 0x20.toByte(), 0x22.toByte(),
            0x2C.toByte(), 0x23.toByte(), 0x1C.toByte(), 0x1C.toByte(), 0x28.toByte(), 0x37.toByte(),
            0x29.toByte(), 0x2C.toByte(), 0x30.toByte(), 0x31.toByte(), 0x34.toByte(), 0x34.toByte(),
            0x34.toByte(), 0x1F.toByte(), 0x27.toByte(), 0x39.toByte(), 0x3D.toByte(), 0x38.toByte(),
            0x32.toByte(), 0x3C.toByte(), 0x2E.toByte(), 0x33.toByte(), 0x34.toByte(), 0x32.toByte(),
            0xFF.toByte(), 0xD9.toByte()
        )
    }

    private fun createSampleAudioBytes(): ByteArray {
        // Minimal mock audio byte buffer
        return ByteArray(1024) { 0x55 }
    }

    private fun createInitialZip(targetZip: File) {
        try {
            ZipOutputStream(FileOutputStream(targetZip)).use { zos ->
                val entry = ZipEntry("welcome.txt")
                zos.putNextEntry(entry)
                zos.write("Thank you for using Offline File Manager!".toByteArray())
                zos.closeEntry()
            }
        } catch (_: Exception) {}
    }

    suspend fun getFiles(directoryPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val list = dir.listFiles() ?: return@withContext emptyList()
        list.map { toFileItem(it) }
    }

    suspend fun getAllFilesRecursively(fromDir: File = rootDir): List<FileItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FileItem>()
        fun scan(file: File) {
            val children = file.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                result.add(toFileItem(child))
                if (child.isDirectory) {
                    scan(child)
                }
            }
        }
        scan(fromDir)
        result
    }

    suspend fun getFilesByCategory(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val all = getAllFilesRecursively(rootDir)
        all.filter { !it.isDirectory && it.category == category }
    }

    suspend fun getRecentFiles(limit: Int = 30): List<FileItem> = withContext(Dispatchers.IO) {
        val all = getAllFilesRecursively(rootDir)
        all.filter { !it.isDirectory }
            .sortedByDescending { it.lastModified }
            .take(limit)
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val all = getAllFilesRecursively(rootDir)
        val categorySizes = mutableMapOf<FileCategory, Long>()
        var totalUsed = 0L

        for (item in all) {
            if (!item.isDirectory) {
                totalUsed += item.size
                categorySizes[item.category] = (categorySizes[item.category] ?: 0L) + item.size
            }
        }

        // Realistic sandbox storage allocation (e.g. 512 MB total app quota)
        val totalQuota = 512L * 1024L * 1024L
        val freeQuota = (totalQuota - totalUsed).coerceAtLeast(0L)

        StorageStats(
            totalBytes = totalQuota,
            usedBytes = totalUsed,
            freeBytes = freeQuota,
            categorySizes = categorySizes,
            totalFileCount = all.count { !it.isDirectory }
        )
    }

    suspend fun createFolder(parentPath: String, folderName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        val (isValid, err) = LocalFileRules.validateName(folderName)
        if (!isValid) return@withContext Result.failure(IllegalArgumentException(err))

        val parent = File(parentPath)
        val target = File(parent, folderName.trim())
        if (target.exists()) {
            return@withContext Result.failure(IllegalStateException("A folder or file with this name already exists"))
        }

        if (target.mkdirs()) {
            Result.success(toFileItem(target))
        } else {
            Result.failure(IOException("Failed to create folder"))
        }
    }

    suspend fun createFile(parentPath: String, fileName: String, initialContent: String = ""): Result<FileItem> = withContext(Dispatchers.IO) {
        val (isValid, err) = LocalFileRules.validateName(fileName)
        if (!isValid) return@withContext Result.failure(IllegalArgumentException(err))

        val parent = File(parentPath)
        val target = File(parent, fileName.trim())
        if (target.exists()) {
            return@withContext Result.failure(IllegalStateException("A file with this name already exists"))
        }

        try {
            if (target.createNewFile()) {
                if (initialContent.isNotEmpty()) {
                    target.writeText(initialContent)
                }
                Result.success(toFileItem(target))
            } else {
                Result.failure(IOException("Failed to create file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(oldPath: String, newName: String): Result<FileItem> = withContext(Dispatchers.IO) {
        val (isValid, err) = LocalFileRules.validateName(newName)
        if (!isValid) return@withContext Result.failure(IllegalArgumentException(err))

        val src = File(oldPath)
        if (!src.exists()) return@withContext Result.failure(IOException("Source file not found"))

        val target = File(src.parentFile, newName.trim())
        if (target.exists()) {
            return@withContext Result.failure(IllegalStateException("A file or folder with this name already exists"))
        }

        if (src.renameTo(target)) {
            Result.success(toFileItem(target))
        } else {
            Result.failure(IOException("Failed to rename file"))
        }
    }

    suspend fun copyItems(items: List<FileItem>, destinationDir: String): Result<Int> = withContext(Dispatchers.IO) {
        val targetDir = File(destinationDir)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext Result.failure(IOException("Target directory does not exist"))
        }

        var copiedCount = 0
        for (item in items) {
            val src = File(item.path)
            if (!src.exists()) continue
            val uniqueName = LocalFileRules.generateUniqueName(targetDir, src.name)
            val dst = File(targetDir, uniqueName)

            if (src.isDirectory) {
                src.copyRecursively(dst, overwrite = false)
            } else {
                src.copyTo(dst, overwrite = false)
            }
            copiedCount++
        }
        Result.success(copiedCount)
    }

    suspend fun moveItems(items: List<FileItem>, destinationDir: String): Result<Int> = withContext(Dispatchers.IO) {
        val targetDir = File(destinationDir)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext Result.failure(IOException("Target directory does not exist"))
        }

        var movedCount = 0
        for (item in items) {
            val src = File(item.path)
            if (!src.exists()) continue
            val uniqueName = LocalFileRules.generateUniqueName(targetDir, src.name)
            val dst = File(targetDir, uniqueName)

            if (src.renameTo(dst)) {
                movedCount++
            } else {
                // Fallback copy then delete
                if (src.isDirectory) {
                    if (src.copyRecursively(dst, overwrite = false)) {
                        src.deleteRecursively()
                        movedCount++
                    }
                } else {
                    src.copyTo(dst, overwrite = false)
                    src.delete()
                    movedCount++
                }
            }
        }
        Result.success(movedCount)
    }

    suspend fun moveToTrash(items: List<FileItem>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        for (item in items) {
            val src = File(item.path)
            if (!src.exists()) continue
            val id = UUID.randomUUID().toString()
            val trashTarget = File(trashDir, id)

            if (src.renameTo(trashTarget)) {
                appendTrashMeta(
                    TrashEntry(
                        id = id,
                        originalPath = item.path,
                        trashPath = trashTarget.absolutePath,
                        fileName = item.name,
                        isDirectory = item.isDirectory,
                        size = item.size,
                        deletedAt = System.currentTimeMillis()
                    )
                )
                count++
            }
        }
        Result.success(count)
    }

    suspend fun getTrashEntries(): List<TrashEntry> = withContext(Dispatchers.IO) {
        if (!trashMetadataFile.exists()) return@withContext emptyList()
        val entries = mutableListOf<TrashEntry>()
        trashMetadataFile.forEachLine { line ->
            val parts = line.split("|")
            if (parts.size >= 7) {
                val trashFile = File(parts[2])
                if (trashFile.exists()) {
                    entries.add(
                        TrashEntry(
                            id = parts[0],
                            originalPath = parts[1],
                            trashPath = parts[2],
                            fileName = parts[3],
                            isDirectory = parts[4].toBoolean(),
                            size = parts[5].toLongOrNull() ?: 0L,
                            deletedAt = parts[6].toLongOrNull() ?: 0L
                        )
                    )
                }
            }
        }
        entries.sortedByDescending { it.deletedAt }
    }

    suspend fun restoreTrashEntry(entry: TrashEntry): Result<Boolean> = withContext(Dispatchers.IO) {
        val trashFile = File(entry.trashPath)
        if (!trashFile.exists()) return@withContext Result.failure(IOException("File no longer in trash"))

        val origFile = File(entry.originalPath)
        val targetParent = origFile.parentFile ?: rootDir
        if (!targetParent.exists()) {
            targetParent.mkdirs()
        }
        val safeName = LocalFileRules.generateUniqueName(targetParent, entry.fileName)
        val restoredTarget = File(targetParent, safeName)

        if (trashFile.renameTo(restoredTarget)) {
            removeTrashMeta(entry.id)
            Result.success(true)
        } else {
            Result.failure(IOException("Failed to restore item"))
        }
    }

    suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        val entries = getTrashEntries()
        var deletedCount = 0
        for (e in entries) {
            val file = File(e.trashPath)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
            deletedCount++
        }
        trashMetadataFile.delete()
        Result.success(deletedCount)
    }

    suspend fun deletePermanently(items: List<FileItem>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        for (item in items) {
            val f = File(item.path)
            if (f.isDirectory) {
                if (f.deleteRecursively()) count++
            } else {
                if (f.delete()) count++
            }
        }
        Result.success(count)
    }

    suspend fun readTextFile(path: String): String = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists() && file.isFile) {
            file.readText()
        } else {
            ""
        }
    }

    suspend fun saveTextFile(path: String, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.writeText(content)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun compressToZip(items: List<FileItem>, outputZipName: String, destinationDir: String): Result<FileItem> = withContext(Dispatchers.IO) {
        val dest = File(destinationDir)
        val safeZipName = if (outputZipName.endsWith(".zip", ignoreCase = true)) outputZipName else "$outputZipName.zip"
        val uniqueName = LocalFileRules.generateUniqueName(dest, safeZipName)
        val targetZip = File(dest, uniqueName)

        try {
            ZipOutputStream(FileOutputStream(targetZip)).use { zos ->
                for (item in items) {
                    val file = File(item.path)
                    addToZip(file, file.name, zos)
                }
            }
            Result.success(toFileItem(targetZip))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addToZip(fileToZip: File, fileName: String, zos: ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val entryName = if (fileName.endsWith("/")) fileName else "$fileName/"
            zos.putNextEntry(ZipEntry(entryName))
            zos.closeEntry()
            fileToZip.listFiles()?.forEach { child ->
                addToZip(child, "$entryName${child.name}", zos)
            }
            return
        }

        FileInputStream(fileToZip).use { fis ->
            val zipEntry = ZipEntry(fileName)
            zos.putNextEntry(zipEntry)
            val bytes = ByteArray(8192)
            var length: Int
            while (fis.read(bytes).also { length = it } >= 0) {
                zos.write(bytes, 0, length)
            }
            zos.closeEntry()
        }
    }

    suspend fun extractZip(zipPath: String, targetFolder: String): Result<Int> = withContext(Dispatchers.IO) {
        val zipFile = File(zipPath)
        if (!zipFile.exists()) return@withContext Result.failure(IOException("Zip file does not exist"))

        val targetDir = File(targetFolder)
        if (!targetDir.exists()) targetDir.mkdirs()

        var extractedCount = 0
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    // Guard against zip slip
                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Zip entry is outside of target dir: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        extractedCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(extractedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun appendTrashMeta(entry: TrashEntry) {
        val line = "${entry.id}|${entry.originalPath}|${entry.trashPath}|${entry.fileName}|${entry.isDirectory}|${entry.size}|${entry.deletedAt}\n"
        trashMetadataFile.appendText(line)
    }

    private fun removeTrashMeta(id: String) {
        if (!trashMetadataFile.exists()) return
        val remaining = trashMetadataFile.readLines().filterNot { it.startsWith("$id|") }
        trashMetadataFile.writeText(remaining.joinToString("\n") + if (remaining.isNotEmpty()) "\n" else "")
    }

    private fun toFileItem(file: File): FileItem {
        val isDir = file.isDirectory
        val ext = if (isDir) "" else file.extension
        val size = if (isDir) calculateFolderSize(file) else file.length()
        val count = if (isDir) (file.listFiles()?.size ?: 0) else 0
        val category = if (isDir) FileCategory.OTHER else LocalFileRules.getCategoryForExtension(ext)
        val mimeType = if (isDir) "resource/folder" else LocalFileRules.getMimeType(file.name)

        return FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = isDir,
            size = size,
            lastModified = file.lastModified(),
            extension = ext,
            category = category,
            itemCount = count,
            mimeType = mimeType
        )
    }

    private fun calculateFolderSize(folder: File): Long {
        var length: Long = 0
        val files = folder.listFiles() ?: return 0
        for (file in files) {
            length += if (file.isFile) file.length() else calculateFolderSize(file)
        }
        return length
    }
}
