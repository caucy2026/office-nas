package com.kemi.desklink.workspace

/**
 * Selects the Android-native path for document-provider URIs and keeps LibVLC for
 * network protocols that the platform player does not consistently implement.
 */
enum class MediaPlaybackEngine {
    PLATFORM,
    LIB_VLC,
}

object MediaPlaybackEnginePolicy {
    fun select(media: MediaRef?): MediaPlaybackEngine =
        if (media?.provider == LOCAL_PROVIDER) MediaPlaybackEngine.PLATFORM else MediaPlaybackEngine.LIB_VLC

    private const val LOCAL_PROVIDER = "local"
}
