package com.kemi.desklink.workspace

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkspaceCoordinatorTest {
    @AfterTest
    fun reset() {
        WorkspaceCoordinator.resetForTest()
    }

    @Test
    fun updateKeepsOneAuthoritativeSession() {
        val updated = WorkspaceCoordinator.update {
            it.copy(draftText = "KEMI 语音提交", selectionVersion = it.selectionVersion + 1)
        }

        assertEquals("KEMI 语音提交", updated.draftText)
        assertEquals(updated, WorkspaceCoordinator.snapshot())
        assertEquals(1L, updated.selectionVersion)
    }

    @Test
    fun playingMediaPausesForVoiceAndResumesOnlyForSameRequest() {
        val playing = WorkspaceSession(playback = PlaybackState.PLAYING, selectionVersion = 4)
        val listening = VoiceSessionReducer.onImeOpened(playing, "voice-1", 2)

        assertEquals(PlaybackState.PAUSED, listening.playback)
        assertEquals(true, listening.mediaWasPlayingBeforeVoice)
        assertEquals(4L, listening.voice?.targetSelectionVersion)

        val unrelatedClose = VoiceSessionReducer.onImeClosed(listening, "voice-2")
        assertEquals(listening, unrelatedClose)

        val resumed = VoiceSessionReducer.onImeClosed(listening, "voice-1")
        assertEquals(PlaybackState.PLAYING, resumed.playback)
        assertEquals(VoiceState.FINISHED, resumed.voice?.state)
    }

    @Test
    fun pausedMediaStaysPausedAfterVoice() {
        val paused = WorkspaceSession(playback = PlaybackState.PAUSED)
        val listening = VoiceSessionReducer.onImeOpened(paused, "voice-1", 2)
        val closed = VoiceSessionReducer.onImeClosed(listening, "voice-1")

        assertEquals(PlaybackState.PAUSED, closed.playback)
        assertEquals(false, closed.mediaWasPlayingBeforeVoice)
        assertNull(WorkspaceCoordinator.snapshot().voice)
    }
}
