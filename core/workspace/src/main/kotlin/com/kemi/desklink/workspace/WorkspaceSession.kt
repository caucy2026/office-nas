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
    fun resetForTest() {
        current = WorkspaceSession()
    }
}

