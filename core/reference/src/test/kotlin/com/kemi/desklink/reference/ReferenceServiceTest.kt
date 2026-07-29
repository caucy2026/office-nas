package com.kemi.desklink.reference

import com.kemi.desklink.workspace.MediaRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReferenceServiceTest {
    private val media = MediaRef(
        provider = "local",
        assetId = "content://downloads/document/维修 演示/42",
        displayName = "维修[演示].mp4",
        uri = "content://downloads/document/维修 演示/42",
    )

    @Test
    fun `round trips a media reference without path ambiguity`() {
        val reference = ReferenceService.create(media, 763_000L)

        assertEquals(reference, ReferenceService.parse(ReferenceService.toUri(reference)))
    }

    @Test
    fun `formats a readable markdown reference and appends it on a new line`() {
        val markdown = ReferenceService.markdownLink(media, 6_000L)

        assertTrue(markdown.startsWith("[《维修\\[演示\\].mp4》 0:06](kemi-desklink://media/"))
        assertEquals(
            ReferenceService.create(media, 6_000L),
            ReferenceService.parse(markdown.substringAfter("](").removeSuffix(")")),
        )
        assertEquals("会议记录\n$markdown", ReferenceService.appendToDraft("会议记录", markdown))
    }

    @Test
    fun `rejects foreign and malformed deep links`() {
        assertNull(ReferenceService.parse("https://example.com/media/local/asset?t=1"))
        assertNull(ReferenceService.parse("kemi-desklink://media/not-base64?t=-1"))
    }
}
