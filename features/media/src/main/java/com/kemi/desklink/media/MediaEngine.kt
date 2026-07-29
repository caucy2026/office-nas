package com.kemi.desklink.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface

/**
 * Playback seam. P3a uses the platform player for local files; the P3 LibVLC adapter
 * will implement this same interface for SMB, NFS and UPnP without touching D2 UI.
 */
interface MediaEngine {
    val isPrepared: Boolean

    fun attach(surface: Surface?)

    fun load(uri: Uri)

    fun play()

    fun pause()

    fun release()
}

class PlatformMediaEngine(
    private val context: Context,
    private val onPrepared: () -> Unit,
    private val onCompleted: () -> Unit,
    private val onError: (String) -> Unit,
) : MediaEngine {
    private val player = MediaPlayer()
    private var attachedSurface: Surface? = null

    override var isPrepared: Boolean = false
        private set

    init {
        player.setOnPreparedListener {
            isPrepared = true
            attachedSurface?.let(player::setSurface)
            onPrepared()
        }
        player.setOnCompletionListener {
            onCompleted()
        }
        player.setOnErrorListener { _, what, extra ->
            isPrepared = false
            onError("MediaPlayer error what=$what extra=$extra")
            true
        }
    }

    override fun attach(surface: Surface?) {
        attachedSurface = surface
        player.setSurface(surface)
    }

    override fun load(uri: Uri) {
        isPrepared = false
        player.reset()
        attachedSurface?.let(player::setSurface)
        try {
            player.setDataSource(context, uri)
            player.prepareAsync()
        } catch (error: Exception) {
            onError(error.message ?: error.javaClass.simpleName)
        }
    }

    override fun play() {
        if (isPrepared && !player.isPlaying) player.start()
    }

    override fun pause() {
        if (isPrepared && player.isPlaying) player.pause()
    }

    override fun release() {
        isPrepared = false
        player.release()
    }
}

