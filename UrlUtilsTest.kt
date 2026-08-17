package com.netly.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlUtilsTest {

    @Test
    fun testValidYouTubeLiveUrls() {
        val live1 = "https://www.youtube.com/live/vhwA3fs7l78"
        assertEquals("vhwA3fs7l78", UrlUtils.extractVideoId(live1))
        assertEquals("https://www.youtube.com/watch?v=vhwA3fs7l78", UrlUtils.extractYouTubeUrl(live1))
        assertTrue(UrlUtils.isValidYouTubeUrl(live1))

        val live2 = "https://youtube.com/live/vhwA3fs7l78"
        assertEquals("vhwA3fs7l78", UrlUtils.extractVideoId(live2))
        assertTrue(UrlUtils.isValidYouTubeUrl(live2))

        val live3 = "https://m.youtube.com/live/vhwA3fs7l78?si=9kAbcD123"
        assertEquals("vhwA3fs7l78", UrlUtils.extractVideoId(live3))
        assertTrue(UrlUtils.isValidYouTubeUrl(live3))

        val live4 = "http://www.youtube.com/live/VIDEO_ID_11"
        assertEquals("VIDEO_ID_11", UrlUtils.extractVideoId(live4))
        assertTrue(UrlUtils.isValidYouTubeUrl(live4))
    }

    @Test
    fun testValidYouTubeWatchUrls() {
        val watch1 = "https://www.youtube.com/watch?v=hlRTxA10eRA"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(watch1))
        assertTrue(UrlUtils.isValidYouTubeUrl(watch1))

        val watch2 = "https://youtube.com/watch?v=hlRTxA10eRA"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(watch2))
        assertTrue(UrlUtils.isValidYouTubeUrl(watch2))

        val watch3 = "https://m.youtube.com/watch?v=hlRTxA10eRA"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(watch3))
        assertTrue(UrlUtils.isValidYouTubeUrl(watch3))

        val watchWithParams = "https://www.youtube.com/watch?feature=share&v=hlRTxA10eRA&si=abc12345"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(watchWithParams))
        assertTrue(UrlUtils.isValidYouTubeUrl(watchWithParams))
    }

    @Test
    fun testValidYouTubeShortUrls() {
        val youtuBe1 = "https://youtu.be/hlRTxA10eRA"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(youtuBe1))
        assertTrue(UrlUtils.isValidYouTubeUrl(youtuBe1))

        val youtuBeWithQuery = "https://youtu.be/hlRTxA10eRA?si=p8YtD-1"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(youtuBeWithQuery))
        assertTrue(UrlUtils.isValidYouTubeUrl(youtuBeWithQuery))
    }

    @Test
    fun testValidYouTubeShortsAndEmbedUrls() {
        val shorts1 = "https://www.youtube.com/shorts/hlRTxA10eRA"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(shorts1))
        assertTrue(UrlUtils.isValidYouTubeUrl(shorts1))

        val shorts2 = "https://youtube.com/shorts/hlRTxA10eRA?feature=share"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(shorts2))
        assertTrue(UrlUtils.isValidYouTubeUrl(shorts2))

        val embed = "https://www.youtube.com/embed/hlRTxA10eRA"
        assertEquals("hlRTxA10eRA", UrlUtils.extractVideoId(embed))
        assertTrue(UrlUtils.isValidYouTubeUrl(embed))
    }

    @Test
    fun testValidStandaloneId() {
        val rawId = "vhwA3fs7l78"
        assertEquals("vhwA3fs7l78", UrlUtils.extractVideoId(rawId))
        assertTrue(UrlUtils.isValidYouTubeUrl(rawId))
    }

    @Test
    fun testInvalidUrls() {
        assertNull(UrlUtils.extractVideoId("https://example.com/video"))
        assertFalse(UrlUtils.isValidYouTubeUrl("https://example.com/video"))

        assertNull(UrlUtils.extractVideoId("https://google.com"))
        assertFalse(UrlUtils.isValidYouTubeUrl("https://google.com"))

        assertNull(UrlUtils.extractVideoId("random text"))
        assertFalse(UrlUtils.isValidYouTubeUrl("random text"))

        assertNull(UrlUtils.extractVideoId("youtube.com"))
        assertFalse(UrlUtils.isValidYouTubeUrl("youtube.com"))

        assertNull(UrlUtils.extractVideoId("https://youtu.be/"))
        assertFalse(UrlUtils.isValidYouTubeUrl("https://youtu.be/"))

        assertNull(UrlUtils.extractVideoId("https://example.com/watch?v=vhwA3fs7l78"))
        assertFalse(UrlUtils.isValidYouTubeUrl("https://example.com/watch?v=vhwA3fs7l78"))

        assertNull(UrlUtils.extractVideoId("https://example.com/live/vhwA3fs7l78"))
        assertFalse(UrlUtils.isValidYouTubeUrl("https://example.com/live/vhwA3fs7l78"))

        assertNull(UrlUtils.extractVideoId(""))
        assertNull(UrlUtils.extractVideoId(null))
    }
}
