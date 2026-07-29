package com.kemi.desklink.platform

import android.content.Context
import com.kemi.desklink.workspace.MediaRef
import com.kemi.desklink.workspace.PlaybackState
import com.kemi.desklink.workspace.WorkspaceSession

/**
 * P1 persistence for non-sensitive workspace state. Credentials are deliberately excluded;
 * future NAS providers must store those separately in Android Keystore.
 */
class WorkspaceRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): WorkspaceSession {
        val media = preferences.getString(KEY_MEDIA_PROVIDER, null)?.let { provider ->
            MediaRef(
                provider = provider,
                assetId = preferences.getString(KEY_MEDIA_ASSET_ID, "") ?: "",
                displayName = preferences.getString(KEY_MEDIA_NAME, "") ?: "",
                uri = preferences.getString(KEY_MEDIA_URI, "") ?: "",
            )
        }
        return WorkspaceSession(
            documentTitle = preferences.getString(KEY_DOCUMENT_TITLE, null),
            draftText = preferences.getString(KEY_DRAFT_TEXT, "") ?: "",
            selectionVersion = preferences.getLong(KEY_SELECTION_VERSION, 0L),
            media = media,
            playback = preferences.getString(KEY_PLAYBACK, PlaybackState.IDLE.name)
                ?.let { runCatching { PlaybackState.valueOf(it) }.getOrDefault(PlaybackState.IDLE) }
                ?: PlaybackState.IDLE,
        )
    }

    fun save(session: WorkspaceSession) {
        preferences.edit()
            .putString(KEY_DOCUMENT_TITLE, session.documentTitle)
            .putString(KEY_DRAFT_TEXT, session.draftText)
            .putLong(KEY_SELECTION_VERSION, session.selectionVersion)
            .putString(KEY_PLAYBACK, session.playback.name)
            .putString(KEY_MEDIA_PROVIDER, session.media?.provider)
            .putString(KEY_MEDIA_ASSET_ID, session.media?.assetId)
            .putString(KEY_MEDIA_NAME, session.media?.displayName)
            .putString(KEY_MEDIA_URI, session.media?.uri)
            .apply()
    }

    private companion object {
        const val PREFS = "desklink_workspace"
        const val KEY_DOCUMENT_TITLE = "document_title"
        const val KEY_DRAFT_TEXT = "draft_text"
        const val KEY_SELECTION_VERSION = "selection_version"
        const val KEY_PLAYBACK = "playback"
        const val KEY_MEDIA_PROVIDER = "media_provider"
        const val KEY_MEDIA_ASSET_ID = "media_asset_id"
        const val KEY_MEDIA_NAME = "media_name"
        const val KEY_MEDIA_URI = "media_uri"
    }
}

