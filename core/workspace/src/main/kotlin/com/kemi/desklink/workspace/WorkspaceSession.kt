package com.kemi.desklink.workspace

/**
 * P0 的跨屏会话模型。P1 将在不改变这个公共模型的前提下增加磁盘持久化。
 */
data class WorkspaceSession(
    val sessionId: String = "default",
    val documentTitle: String? = null,
    val draftText: String = "",
    val selectionVersion: Long = 0L,
    val media: MediaRef? = null,
    val playback: PlaybackState = PlaybackState.IDLE,
    /** Position is intentionally separate from [MediaRef]; a shared URI is not a bookmark. */
    val mediaPositionMs: Long = 0L,
    val mediaDurationMs: Long = 0L,
    val mediaWasPlayingBeforeVoice: Boolean = false,
    val voice: VoiceSession? = null,
)

data class MediaRef(
    val provider: String,
    val assetId: String,
    val displayName: String,
    val uri: String,
)

data class VoiceSession(
    val requestId: String,
    val targetDisplayId: Int,
    val targetSelectionVersion: Long,
    val state: VoiceState,
)

enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
}

enum class VoiceState {
    REQUESTED,
    LISTENING,
    FINISHED,
}

/**
 * 唯一状态源。它不依赖 Android，因此 P0 即可对状态转换做 JVM 单测。
 */
object WorkspaceCoordinator {
    @Volatile
    private var current = WorkspaceSession()

    @Synchronized
    fun snapshot(): WorkspaceSession = current

    @Synchronized
    fun update(transform: (WorkspaceSession) -> WorkspaceSession): WorkspaceSession {
        current = transform(current)
        return current
    }

    @Synchronized
    fun restore(session: WorkspaceSession): WorkspaceSession {
        current = session
        return current
    }

    @Synchronized
    fun resetForTest() {
        current = WorkspaceSession()
    }
}

/** Pure transition rules that can be tested without Android or a real media engine. */
object VoiceSessionReducer {
    fun onImeOpened(
        session: WorkspaceSession,
        requestId: String,
        targetDisplayId: Int,
    ): WorkspaceSession {
        if (session.voice?.state == VoiceState.LISTENING) return session

        val wasPlaying = session.playback == PlaybackState.PLAYING
        return session.copy(
            playback = if (wasPlaying) PlaybackState.PAUSED else session.playback,
            mediaWasPlayingBeforeVoice = wasPlaying,
            voice = VoiceSession(
                requestId = requestId,
                targetDisplayId = targetDisplayId,
                targetSelectionVersion = session.selectionVersion,
                state = VoiceState.LISTENING,
            ),
        )
    }

    fun onImeClosed(session: WorkspaceSession, requestId: String): WorkspaceSession {
        val voice = session.voice ?: return session
        if (voice.requestId != requestId || voice.state != VoiceState.LISTENING) return session

        return session.copy(
            playback = if (session.mediaWasPlayingBeforeVoice) PlaybackState.PLAYING else session.playback,
            mediaWasPlayingBeforeVoice = false,
            voice = voice.copy(state = VoiceState.FINISHED),
        )
    }
}
