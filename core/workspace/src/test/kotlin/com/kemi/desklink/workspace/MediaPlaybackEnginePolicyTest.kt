package com.kemi.desklink.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPlaybackEnginePolicyTest {
    @Test
    fun `uses platform player for a local document provider uri`() {
        val local = MediaRef(
            provider = "local",
            assetId = "content://downloads/42",
            displayName = "recording.mp4",
            uri = "content://downloads/42",
        )

        assertEquals(MediaPlaybackEngine.PLATFORM, MediaPlaybackEnginePolicy.select(local))
    }

    @Test
    fun `uses LibVLC for network and empty selections`() {
        val network = MediaRef(
            provider = "smb",
            assetId = "smb://nas/video.mp4",
            displayName = "video.mp4",
            uri = "smb://nas/video.mp4",
        )

        assertEquals(MediaPlaybackEngine.LIB_VLC, MediaPlaybackEnginePolicy.select(network))
        assertEquals(MediaPlaybackEngine.LIB_VLC, MediaPlaybackEnginePolicy.select(null))
    }
}
