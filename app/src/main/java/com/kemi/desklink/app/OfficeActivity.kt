package com.kemi.desklink.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.kemi.desklink.platform.DisplayRouter
import com.kemi.desklink.workspace.WorkspaceCoordinator

/**
 * P0 主屏入口。当前的 EditText 是 ONLYOFFICE 接入前的输入法验证面；
 * 它必须保持主屏焦点，让 KEMI 的跨屏语音 IME 将 commitText 写回这里。
 */
class OfficeActivity : Activity() {
    private lateinit var editor: EditText
    private lateinit var displayStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.getBooleanExtra(EXTRA_MAIN_DISPLAY_REDIRECT, false) &&
            DisplayRouter.currentDisplayId(this) != DisplayRouter.MAIN_DISPLAY_ID
        ) {
            Log.i(TAG, "OfficeActivity launched outside D0; redirecting to D0")
            val redirect = intent.setClass(this, OfficeActivity::class.java)
                .putExtra(EXTRA_MAIN_DISPLAY_REDIRECT, true)
            DisplayRouter.launchOnDisplay(this, redirect, DisplayRouter.MAIN_DISPLAY_ID)
            finish()
            return
        }

        setContentView(createContent())
        Log.i(TAG, "OfficeActivity ready on display=${DisplayRouter.currentDisplayId(this)}")
    }

    override fun onPause() {
        super.onPause()
        if (::editor.isInitialized) {
            WorkspaceCoordinator.update {
                it.copy(
                    draftText = editor.text.toString(),
                    selectionVersion = it.selectionVersion + 1,
                )
            }
        }
    }

    private fun createContent(): View {
        val padding = dp(20)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(0xFFF6F8FC.toInt())

            addView(title("KEMI DeskLink · P0 双屏验证"))
            displayStatus = TextView(context).apply {
                text = "主屏 D${DisplayRouter.currentDisplayId(this@OfficeActivity)} · 编辑区已准备 KEMI 语音输入"
                textSize = 15f
                setPadding(0, dp(8), 0, dp(12))
            }
            addView(displayStatus)

            editor = EditText(context).apply {
                hint = "点击此处后，使用 KEMI 跨屏语音输入法说一句测试文本"
                setText(WorkspaceCoordinator.snapshot().draftText)
                minLines = 8
                gravity = Gravity.TOP or Gravity.START
                setBackgroundColor(0xFFFFFFFF.toInt())
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }
            addView(editor, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ))

            addView(Button(context).apply {
                text = "在副屏打开媒体验证面"
                setOnClickListener { openMediaOnSecondaryDisplay() }
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(12) })
        }
    }

    private fun openMediaOnSecondaryDisplay() {
        val secondaryId = DisplayRouter.findSecondaryDisplayId(this)
        if (secondaryId == null) {
            displayStatus.text = "未发现已点亮的副屏；请确认 Display 2 已连接"
            Log.w(TAG, "No active secondary display")
            return
        }
        Log.i(TAG, "Launching MediaActivity on display=$secondaryId")
        DisplayRouter.launchOnDisplay(this, MediaActivity.newIntent(this), secondaryId)
    }

    private fun title(value: String) = TextView(this).apply {
        text = value
        textSize = 24f
        setTextColor(0xFF202124.toInt())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "DeskLink.Office"
        const val EXTRA_MAIN_DISPLAY_REDIRECT = "main_display_redirect"
    }
}

