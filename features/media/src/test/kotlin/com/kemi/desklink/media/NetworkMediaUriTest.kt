package com.kemi.desklink.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkMediaUriTest {
    @Test
    fun parsesCredentialFreeNetworkSources() {
        val source = NetworkMediaUri.parse("smb://nas.local/videos/demo-8k.mp4").getOrThrow()

        assertEquals("smb", source.provider)
        assertEquals("demo-8k.mp4", source.displayName)
        assertEquals("smb://nas.local/videos/demo-8k.mp4", source.uri)
    }

    @Test
    fun allowsHttpHlsAndUsesHostWhenPathIsEmpty() {
        val source = NetworkMediaUri.parse("https://stream.example.test").getOrThrow()

        assertEquals("https", source.provider)
        assertEquals("stream.example.test", source.displayName)
    }

    @Test
    fun rejectsUnsupportedSchemesAndPersistedCredentials() {
        assertFailsWith<IllegalArgumentException> {
            NetworkMediaUri.parse("file:///storage/emulated/0/movie.mp4").getOrThrow()
        }
        assertFailsWith<IllegalArgumentException> {
            NetworkMediaUri.parse("smb://user:secret@nas.local/share/movie.mp4").getOrThrow()
        }
    }
}
