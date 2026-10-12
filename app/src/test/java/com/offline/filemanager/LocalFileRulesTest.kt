package com.offline.filemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFileRulesTest {

    @Test
    fun testValidateName_valid() {
        val (valid, msg) = LocalFileRules.validateName("MyDocument.pdf")
        assertTrue(valid)
        assertEquals("", msg)
    }

    @Test
    fun testValidateName_empty() {
        val (valid, _) = LocalFileRules.validateName("")
        assertFalse(valid)
    }

    @Test
    fun testValidateName_invalidCharacters() {
        val (valid1, _) = LocalFileRules.validateName("invalid/name.txt")
        assertFalse(valid1)

        val (valid2, _) = LocalFileRules.validateName("invalid:name.txt")
        assertFalse(valid2)

        val (valid3, _) = LocalFileRules.validateName("invalid*name.txt")
        assertFalse(valid3)
    }

    @Test
    fun testValidateName_dotNames() {
        val (valid1, _) = LocalFileRules.validateName(".")
        assertFalse(valid1)

        val (valid2, _) = LocalFileRules.validateName("..")
        assertFalse(valid2)
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", LocalFileRules.formatFileSize(0))
        assertEquals("500 B", LocalFileRules.formatFileSize(500))
        assertEquals("1.0 KB", LocalFileRules.formatFileSize(1024))
        assertEquals("1.5 MB", LocalFileRules.formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testGetCategoryForExtension() {
        assertEquals(FileCategory.DOCUMENTS, LocalFileRules.getCategoryForExtension("pdf"))
        assertEquals(FileCategory.DOCUMENTS, LocalFileRules.getCategoryForExtension("txt"))
        assertEquals(FileCategory.IMAGES, LocalFileRules.getCategoryForExtension("jpg"))
        assertEquals(FileCategory.IMAGES, LocalFileRules.getCategoryForExtension("png"))
        assertEquals(FileCategory.AUDIO, LocalFileRules.getCategoryForExtension("mp3"))
        assertEquals(FileCategory.VIDEOS, LocalFileRules.getCategoryForExtension("mp4"))
        assertEquals(FileCategory.ARCHIVES, LocalFileRules.getCategoryForExtension("zip"))
        assertEquals(FileCategory.OTHER, LocalFileRules.getCategoryForExtension("xyz"))
    }

    @Test
    fun testIsEditableText() {
        assertTrue(LocalFileRules.isEditableText("notes.txt"))
        assertTrue(LocalFileRules.isEditableText("README.md"))
        assertTrue(LocalFileRules.isEditableText("config.json"))
        assertFalse(LocalFileRules.isEditableText("photo.jpg"))
        assertFalse(LocalFileRules.isEditableText("archive.zip"))
    }
}
