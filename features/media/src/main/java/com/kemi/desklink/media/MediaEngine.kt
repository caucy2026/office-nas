package com.kemi.desklink.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer as VlcMediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Playback seam. The raw [Surface] path keeps the platform fallback simple; LibVLC owns
 * its nested output surface through [VLCVideoLayout].
 */
interface MediaEngine {
    val isPrepared: Boolean
    val usesVlcVideoLayout: Boolean get() = false
    val currentPositionMs: Long get() = 0L
    val durationMs: Long get() = 0L

    fun attach(surface: Surface?)

    /** LibVLC's supported output path; it owns the inner video Surface lifecycle. */
    fun attachVideoLayout(layout: VLCVideoLayout) = Unit

    fun load(uri: Uri)

    /** Best-effort seek used for persisted resume points. */
    fun seekTo(positionMs: Long) = Unit

    fun play()

    fun pause()

    fun release()
}

/**
 * LibVLC-backed implementation for network URI schemes such as smb://, nfs://,
 * upnp://, http(s):// and rtsp://. Android document-provider content URIs use
 * [PlatformMediaEngine], whose Context data-source path preserves their grants.
 */
class LibVlcMediaEngine(
    context: Context,
    private val onPrepared: () -> Unit,
    private val onCompleted: () -> Unit,
    private val onError: (String) -> Unit,
) : MediaEngine {
    private val libVlc = LibVLC(
        context.applicationContext,
        arrayListOf(
            "--avcodec-hw=any",
            "--network-caching=300",
            "--file-caching=150",
            "--audio-time-stretch",
        ),
    )
    private val player = VlcMediaPlayer(libVlc)
    private var videoLayoutAttached = false

    override var isPrepared: Boolean = false
        private set
    override val usesVlcVideoLayout: Boolean = true
    override val currentPositionMs: Long
        get() = if (isPrepared) player.time.coerceAtLeast(0L) else 0L
    override val durationMs: Long
        get() = if (isPrepared) player.length.coerceAtLeast(0L) else 0L

    init {
        player.setEventListener { event ->
            when (event.type) {
                VlcMediaPlayer.Event.Vout -> player.updateVideoSurfaces()
                VlcMediaPlayer.Event.EndReached -> onCompleted()
                VlcMediaPlayer.Event.EncounteredError -> {
                    isPrepared = false
                    onError("LibVLC 无法打开或解码该媒体")
                }
            }
        }
    }

    override fun attach(surface: Surface?) = Unit

    override fun attachVideoLayout(layout: VLCVideoLayout) {
        if (videoLayoutAttached) player.detachViews()
        player.attachViews(
            layout,
            null,
            false,
            false,
        )
        player.videoScale = VlcMediaPlayer.ScaleType.SURFACE_BEST_FIT
        videoLayoutAttached = true
    }

    override fun load(uri: Uri) {
        isPrepared = false
        val media = Media(libVlc, uri)
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=300")
        player.media = media
        media.release()
        isPrepared = true
        onPrepared()
    }

    override fun seekTo(positionMs: Long) {
        if (isPrepared) player.time = positionMs.coerceAtLeast(0L)
    }

    override fun play() {
        if (isPrepared && !player.isPlaying) player.play()
    }

    override fun pause() {
        if (isPrepared && player.isPlaying) player.pause()
    }

    override fun release() {
        isPrepared = false
        if (videoLayoutAttached) {
            player.detachViews()
            videoLayoutAttached = false
        }
        player.release()
        libVlc.release()
    }
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
    override val currentPositionMs: Long
        get() = if (isPrepared) player.currentPosition.toLong().coerceAtLeast(0L) else 0L
    override val durationMs: Long
        get() = if (isPrepared) player.duration.toLong().coerceAtLeast(0L) else 0L

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

    override fun seekTo(positionMs: Long) {
        if (isPrepared) player.seekTo(positionMs.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
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
