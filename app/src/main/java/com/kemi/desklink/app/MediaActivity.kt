package com.kemi.desklink.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.kemi.desklink.platform.DisplayRouter
import com.kemi.desklink.workspace.PlaybackState
import com.kemi.desklink.workspace.WorkspaceCoordinator

/**
 * P0 副屏验证面。P3 会在这里接入 LibVLC；此 Activity 只保留媒体控制职责，
 * 不创建文字输入控件，避免抢走 D0 文档的 KEMI IME 焦点。
 */
class MediaActivity : Activity() {
    private lateinit var stateLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
        Log.i(TAG, "MediaActivity ready on display=${DisplayRouter.currentDisplayId(this)}")
    }

    private fun createContent(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val padding = dp(24)
        setPadding(padding, padding, padding, padding)
        setBackgroundColor(0xFF101820.toInt())

        addView(TextView(context).apply {
            text = "KEMI DeskLink · 副屏媒体验证面"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
        })
        stateLabel = TextView(context).apply {
            text = "D${DisplayRouter.currentDisplayId(this@MediaActivity)} · LibVLC 将在 P3 接入"
            textSize = 16f
            setTextColor(0xFFD0D7DE.toInt())
            setPadding(0, dp(12), 0, dp(12))
        }
        addView(stateLabel)
        addView(Button(context).apply {
            text = "切换媒体播放状态（验证状态同步）"
            setOnClickListener { togglePlayback() }
        })
        addView(Button(context).apply {
            text = "返回主屏"
            setOnClickListener { finish() }
        })
    }

    private fun togglePlayback() {
        val next = WorkspaceCoordinator.update {
            it.copy(playback = if (it.playback == PlaybackState.PLAYING) PlaybackState.PAUSED else PlaybackState.PLAYING)
        }
        stateLabel.text = "D${DisplayRouter.currentDisplayId(this)} · 状态：${next.playback}"
        Log.i(TAG, "Playback state=${next.playback}")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "DeskLink.Media"

        fun newIntent(context: Context): Intent = Intent(context, MediaActivity::class.java)
    }
}

