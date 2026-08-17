package com.netly.app.updater

import com.netly.app.data.updater.util.MarkdownUtils
import com.netly.app.data.updater.util.SemanticVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun testVersionComparison() {
        // 1.9.0 < 1.10.0
        assertTrue(SemanticVersion.isNewer("1.10.0", "1.9.0"))
        assertTrue(SemanticVersion.isNewer("v1.10.0", "1.9.0"))
        assertFalse(SemanticVersion.isNewer("1.9.0", "1.10.0"))

        // v1.1.0 > 1.0.1
        assertTrue(SemanticVersion.isNewer("v1.1.0", "1.0.1"))
        assertTrue(SemanticVersion.isNewer("1.1.0", "1.0.1"))
        assertTrue(SemanticVersion.isNewer("v1.1", "1.0.1"))
        assertTrue(SemanticVersion.isNewer("1.1", "1.0"))
        assertTrue(SemanticVersion.isNewer("v2.0.0", "1.9.9"))
        assertTrue(SemanticVersion.isNewer("v1.0.2", "1.0.1"))
        assertTrue(SemanticVersion.isNewer("1.0.0.2", "1.0.0.1"))

        // Same versions or older
        assertFalse(SemanticVersion.isNewer("v1.0.0", "1.0.0"))
        assertFalse(SemanticVersion.isNewer("v1.0", "1.0"))
        assertFalse(SemanticVersion.isNewer("1.0", "1.0.0"))
        assertFalse(SemanticVersion.isNewer("v1.0.0", "1.0.1"))
        assertFalse(SemanticVersion.isNewer("1.0", "1.1"))
        assertFalse(SemanticVersion.isNewer("v0.9.0", "1.0.0"))
    }

    @Test
    fun testPreReleaseHandling() {
        // Release is newer than pre-release of same version
        val release = SemanticVersion.parse("v1.0.0")
        val beta = SemanticVersion.parse("v1.0.0-beta")
        assertTrue(release > beta)
    }

    @Test
    fun testMarkdownSanitizer() {
        val rawMarkdown = """
            ## What's Changed
            * Fixed video stream extraction by @sumit in https://github.com/sumit/Netly/pull/1
            * **New Feature:** Added in-app updater
            - Improved download speed
            <br>
            Check out the full release on [GitHub](https://github.com/...)
        """.trimIndent()

        val cleaned = MarkdownUtils.sanitizeMarkdown(rawMarkdown)

        assertTrue(cleaned != null)
        assertFalse(cleaned!!.contains("##"))
        assertFalse(cleaned.contains("**"))
        assertFalse(cleaned.contains("<br>"))
        assertTrue(cleaned.contains("• Fixed video stream extraction"))
        assertTrue(cleaned.contains("• New Feature: Added in-app updater"))
        assertTrue(cleaned.contains("• Improved download speed"))
        assertTrue(cleaned.contains("Check out the full release on GitHub"))
    }

    @Test
    fun testEmptyMarkdownReturnsNull() {
        assertNull(MarkdownUtils.sanitizeMarkdown(""))
        assertNull(MarkdownUtils.sanitizeMarkdown("   "))
        assertNull(MarkdownUtils.sanitizeMarkdown(null))
    }
}
