package com.offline.filemanager

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalFileRules {

    private val INVALID_CHARACTERS = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')

    /**
     * Validates a file or directory name.
     * Returns a pair of (isValid, errorMessage).
     */
    fun validateName(name: String?): Pair<Boolean, String> {
        if (name.isNullOrBlank()) {
            return Pair(false, "Name cannot be empty")
        }
        val trimmed = name.trim()
        if (trimmed == "." || trimmed == "..") {
            return Pair(false, "Name cannot be '.' or '..'")
        }
        for (char in INVALID_CHARACTERS) {
            if (trimmed.contains(char)) {
                return Pair(false, "Name cannot contain character: '$char'")
            }
        }
        if (trimmed.length > 255) {
            return Pair(false, "Name exceeds maximum length of 255 characters")
        }
        return Pair(true, "")
    }

    /**
     * Prevents directory traversal attacks by checking if the target path is inside the allowed root.
     */
    fun isPathSafe(rootPath: String, targetPath: String): Boolean {
        val rootCanonical = File(rootPath).canonicalFile
        val targetCanonical = File(targetPath).canonicalFile
        return targetCanonical.path.startsWith(rootCanonical.path)
    }

    /**
     * Generates a non-conflicting unique name for copied or new files.
     */
    fun generateUniqueName(parentDir: File, originalName: String): String {
        val destinationFile = File(parentDir, originalName)
        if (!destinationFile.exists()) {
            return originalName
        }

        val dotIndex = originalName.lastIndexOf('.')
        val (baseName, extension) = if (dotIndex > 0 && dotIndex < originalName.length - 1) {
            Pair(originalName.substring(0, dotIndex), originalName.substring(dotIndex))
        } else {
            Pair(originalName, "")
        }

        var counter = 1
        var candidateName = "$baseName ($counter)$extension"
        while (File(parentDir, candidateName).exists()) {
            counter++
            candidateName = "$baseName ($counter)$extension"
        }
        return candidateName
    }

    /**
     * Formats bytes into a human readable string.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) {
            String.format(Locale.US, "%d %s", bytes, units[index])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[index])
        }
    }

    /**
     * Formats a timestamp into a friendly date string.
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Detects category by file extension.
     */
    fun getCategoryForExtension(extension: String): FileCategory {
        val ext = extension.lowercase().removePrefix(".")
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico" -> FileCategory.IMAGES
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "wmv" -> FileCategory.VIDEOS
            "mp3", "wav", "m4a", "aac", "ogg", "flac", "wma", "opus" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "rtf", "odt" -> FileCategory.DOCUMENTS
            "zip", "tar", "gz", "rar", "7z", "bz2", "xz" -> FileCategory.ARCHIVES
            else -> FileCategory.OTHER
        }
    }

    /**
     * Detects MIME type for a given filename.
     */
    fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html", "htm" -> "text/html"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "csv" -> "text/csv"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "zip" -> "application/zip"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            else -> "*/*"
        }
    }

    /**
     * Checks if a file is an editable text file.
     */
    fun isEditableText(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return listOf("txt", "md", "json", "xml", "html", "htm", "css", "js", "ts", "kt", "java", "py", "sh", "csv", "log", "properties", "gradle").contains(ext)
    }

    /**
     * Calculates MD5 or SHA-256 hash of a file.
     */
    fun calculateChecksum(file: File, algorithm: String = "SHA-256"): String {
        if (!file.exists() || file.isDirectory) return ""
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
