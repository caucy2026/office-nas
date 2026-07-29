package com.kemi.desklink.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.kemi.desklink.media.LibVlcMediaEngine
import com.kemi.desklink.media.MediaEngine
import com.kemi.desklink.media.PlatformMediaEngine
import com.kemi.desklink.platform.DisplayRouter
import com.kemi.desklink.platform.VoiceMediaCoordinator
import com.kemi.desklink.platform.WorkspaceRepository
import com.kemi.desklink.workspace.MediaRef
import com.kemi.desklink.workspace.PlaybackState
import com.kemi.desklink.workspace.WorkspaceCoordinator
import com.kemi.desklink.workspace.WorkspaceSession
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * D2 media workspace. LibVLC receives both local documents and credential-free
 * network media URIs selected on D0; provider browsing remains a later boundary.
 */
class MediaActivity : Activity() {
    private lateinit var stateLabel: TextView
    private lateinit var sourceLabel: TextView
    private lateinit var playButton: Button
    private lateinit var videoContainer: FrameLayout
    private var vlcVideoLayout: VLCVideoLayout? = null
    private lateinit var repository: WorkspaceRepository
    private lateinit var voiceMediaCoordinator: VoiceMediaCoordinator
    private lateinit var mediaEngine: MediaEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        repository = WorkspaceRepository(this)
        WorkspaceCoordinator.restore(repository.load())
        voiceMediaCoordinator = VoiceMediaCoordinator(
            context = this,
            targetDisplayId = DisplayRouter.currentDisplayId(this),
            onSessionChanged = ::persistAndRender,
        )
        mediaEngine = createMediaEngine()
        setContentView(createContent())
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                voiceMediaCoordinator.onImeVisibilityChanged(insets.isVisible(WindowInsets.Type.ime()))
            }
            view.onApplyWindowInsets(insets)
        }
        videoContainer.post {
            vlcVideoLayout?.let(mediaEngine::attachVideoLayout)
            restoreMediaIfPresent()
        }
        Log.i(TAG, "MediaActivity ready on display=${DisplayRouter.currentDisplayId(this)}")
    }

    override fun onDestroy() {
        mediaEngine.release()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WorkspaceCoordinator.restore(repository.load())
        restoreMediaIfPresent()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_LOCAL_VIDEO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
        selectLocalMedia(uri)
    }

    private fun createContent(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val padding = dp(20)
        setPadding(padding, padding, padding, padding)
        setBackgroundColor(0xFF101820.toInt())

        addView(TextView(context).apply {
            text = "KEMI DeskLink · 副屏媒体 P3b"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
        })
        sourceLabel = TextView(context).apply {
            text = "请选择本地视频，或从 D0 发送 SMB/NFS/UPnP/HTTP/RTSP URI"
            textSize = 14f
            setTextColor(0xFFD0D7DE.toInt())
            setPadding(0, dp(8), 0, dp(8))
        }
        addView(sourceLabel)
        videoContainer = FrameLayout(context).apply {
            setBackgroundColor(0xFF000000.toInt())
            if (mediaEngine.usesVlcVideoLayout) {
                VLCVideoLayout(context).also { layout ->
                    vlcVideoLayout = layout
                    addView(layout, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ))
                }
            } else {
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            mediaEngine.attach(holder.surface)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            mediaEngine.attach(null)
                        }
                    })
                    addView(this, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ))
                }
            }
        }
        addView(videoContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        stateLabel = TextView(context).apply {
            text = "D${DisplayRouter.currentDisplayId(this@MediaActivity)} · 媒体：${WorkspaceCoordinator.snapshot().playback}"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, dp(10), 0, dp(6))
        }
        addView(stateLabel)
        addView(Button(context).apply {
            text = "选择本地视频"
            setOnClickListener(::pickLocalVideo)
        })
        playButton = Button(context).apply {
            text = "播放"
            setOnClickListener { togglePlayback() }
        }
        addView(playButton)
    }

    private fun pickLocalVideo(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_PICK_LOCAL_VIDEO)
    }

    private fun selectLocalMedia(uri: Uri) {
        val title = uri.lastPathSegment?.substringAfterLast('/') ?: "本地视频"
        val session = WorkspaceCoordinator.update {
            it.copy(
                media = MediaRef(
                    provider = PROVIDER_LOCAL,
                    assetId = uri.toString(),
                    displayName = title,
                    uri = uri.toString(),
                ),
                playback = PlaybackState.IDLE,
            )
        }
        persistAndRender(session)
        sourceLabel.text = "正在准备：$title"
        mediaEngine.load(uri)
    }

    private fun restoreMediaIfPresent() {
        val media = WorkspaceCoordinator.snapshot().media ?: return
        val uri = Uri.parse(media.uri)
        if (media.provider == PROVIDER_LOCAL && !isReadable(uri)) {
            clearMedia("上次本地视频已不可访问，请重新选择")
            return
        }
        sourceLabel.text = "恢复：${media.displayName}"
        mediaEngine.load(uri)
    }

    private fun onMediaPrepared() {
        sourceLabel.text = "已准备：${WorkspaceCoordinator.snapshot().media?.displayName ?: "本地视频"}"
        val session = WorkspaceCoordinator.update { it.copy(playback = PlaybackState.PAUSED) }
        persistAndRender(session)
    }

    private fun onMediaCompleted() {
        persistAndRender(WorkspaceCoordinator.update { it.copy(playback = PlaybackState.PAUSED) })
    }

    private fun showPlaybackError(message: String) {
        Log.e(TAG, message)
        clearMedia("无法播放该视频，请重新选择")
    }

    private fun clearMedia(message: String) {
        sourceLabel.text = message
        val session = WorkspaceCoordinator.update {
            if (it.media != null) {
                it.copy(media = null, playback = PlaybackState.IDLE)
            } else {
                it.copy(playback = PlaybackState.IDLE)
            }
        }
        persistAndRender(session)
    }

    private fun isReadable(uri: Uri): Boolean = runCatching {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun createMediaEngine(): MediaEngine {
        val prepared = { runOnUiThread(::onMediaPrepared) }
        val completed = { runOnUiThread(::onMediaCompleted) }
        val error = { message: String -> runOnUiThread { showPlaybackError(message) } }
        return runCatching { LibVlcMediaEngine(this, prepared, completed, error) }
            .onFailure { Log.w(TAG, "LibVLC unavailable; using platform local fallback", it) }
            .getOrElse { PlatformMediaEngine(this, prepared, completed, error) }
    }

    private fun togglePlayback() {
        if (!mediaEngine.isPrepared) {
            sourceLabel.text = "请先选择可播放的本地视频"
            return
        }
        val next = WorkspaceCoordinator.update {
            it.copy(playback = if (it.playback == PlaybackState.PLAYING) PlaybackState.PAUSED else PlaybackState.PLAYING)
        }
        persistAndRender(next)
    }

    private fun persistAndRender(session: WorkspaceSession) {
        repository.save(session)
        if (::mediaEngine.isInitialized) {
            when (session.playback) {
                PlaybackState.PLAYING -> mediaEngine.play()
                PlaybackState.PAUSED, PlaybackState.IDLE -> mediaEngine.pause()
            }
        }
        if (::stateLabel.isInitialized) {
            val voice = session.voice?.state?.name ?: "IDLE"
            stateLabel.text = "D${DisplayRouter.currentDisplayId(this)} · 媒体：${session.playback} · 语音：$voice"
            playButton.text = if (session.playback == PlaybackState.PLAYING) "暂停" else "播放"
        }
        Log.i(TAG, "Workspace playback=${session.playback}, voice=${session.voice?.state}")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "DeskLink.Media"
        private const val REQUEST_PICK_LOCAL_VIDEO = 20
        private const val PROVIDER_LOCAL = "local"

        fun newIntent(context: Context): Intent = Intent(context, MediaActivity::class.java)
    }
}
