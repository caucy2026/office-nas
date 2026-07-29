package com.kemi.desklink.platform

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display

/** Centralizes display routing so feature modules do not contain device-specific launch logic. */
object DisplayRouter {
    const val MAIN_DISPLAY_ID: Int = Display.DEFAULT_DISPLAY

    fun currentDisplayId(activity: Activity): Int = activity.display?.displayId ?: MAIN_DISPLAY_ID

    fun findSecondaryDisplayId(context: Context): Int? {
        val manager = context.getSystemService(DisplayManager::class.java) ?: return null
        return manager.displays
            .asSequence()
            .filter { it.displayId != MAIN_DISPLAY_ID }
            .filter { it.state == Display.STATE_ON }
            .map { it.displayId }
            .firstOrNull()
    }

    fun launchOnDisplay(activity: Activity, intent: Intent, displayId: Int) {
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(displayId)
            .toBundle()
        activity.startActivity(intent, options)
    }
}

