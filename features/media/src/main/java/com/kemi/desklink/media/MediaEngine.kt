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

    fun attach(surface: Surface?)

    /** LibVLC's supported output path; it owns the inner video Surface lifecycle. */
    fun attachVideoLayout(layout: VLCVideoLayout) = Unit

    fun load(uri: Uri)

    fun play()

    fun pause()

    fun release()
}

/**
 * LibVLC-backed implementation used for local files and network URI schemes such as
 * smb://, nfs://, upnp://, http(s):// and rtsp://. It uses LibVLC's managed
 * VLCVideoLayout because a raw SurfaceView is not sufficient on all Android displays.
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
