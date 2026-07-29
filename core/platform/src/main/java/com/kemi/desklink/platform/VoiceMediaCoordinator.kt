package com.kemi.desklink.platform

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioAttributes
import android.os.Build
import com.kemi.desklink.workspace.PlaybackState
import com.kemi.desklink.workspace.VoiceSessionReducer
import com.kemi.desklink.workspace.VoiceState
import com.kemi.desklink.workspace.WorkspaceCoordinator
import com.kemi.desklink.workspace.WorkspaceSession
import java.util.UUID

/**
 * Bridges the real IME-visible state from D2 to the pure workspace state machine.
 * It pauses only media that was already playing and never creates an editor/IME on D2.
 */
class VoiceMediaCoordinator(
    context: Context,
    private val targetDisplayId: Int,
    private val onSessionChanged: (WorkspaceSession) -> Unit,
) {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { }
        .build()

    private var activeRequestId: String? = null
    private var lastVisible = false

    fun onImeVisibilityChanged(isVisible: Boolean) {
        if (lastVisible == isVisible) return
        lastVisible = isVisible

        if (isVisible) {
            val requestId = UUID.randomUUID().toString()
            activeRequestId = requestId
            val updated = WorkspaceCoordinator.update {
                VoiceSessionReducer.onImeOpened(it, requestId, targetDisplayId)
            }
            if (updated.mediaWasPlayingBeforeVoice) abandonAudioFocus()
            onSessionChanged(updated)
            return
        }

        val requestId = activeRequestId ?: return
        val updated = WorkspaceCoordinator.update { VoiceSessionReducer.onImeClosed(it, requestId) }
        activeRequestId = null
        if (updated.playback == PlaybackState.PLAYING && updated.voice?.state == VoiceState.FINISHED) {
            requestAudioFocus()
        }
        onSessionChanged(updated)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
