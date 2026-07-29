package com.kemi.desklink.reference

import com.kemi.desklink.workspace.MediaRef
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

/** A portable video bookmark. The media source is intentionally credential-free. */
data class MediaTimeReference(
    val provider: String,
    val assetId: String,
    val positionMs: Long,
)

/**
 * Stable, versionless links used by the Office adapter and the temporary D0 editor.
 * Provider and asset id are URL-safe Base64 path segments, so a local document URI
 * cannot be confused with a deep-link path delimiter.
 */
object ReferenceService {
    private const val SCHEME = "kemi-desklink"
    private const val AUTHORITY = "media"

    fun create(media: MediaRef, positionMs: Long): MediaTimeReference {
        require(media.provider.isNotBlank()) { "media provider is required" }
        require(media.assetId.isNotBlank()) { "media asset id is required" }
        return MediaTimeReference(
            provider = media.provider,
            assetId = media.assetId,
            positionMs = positionMs.coerceAtLeast(0L),
        )
    }

    fun toUri(reference: MediaTimeReference): String {
        require(reference.provider.isNotBlank()) { "media provider is required" }
        require(reference.assetId.isNotBlank()) { "media asset id is required" }
        return "$SCHEME://$AUTHORITY/${encode(reference.provider)}/${encode(reference.assetId)}?t=${reference.positionMs.coerceAtLeast(0L)}"
    }

    fun parse(rawUri: String): MediaTimeReference? = runCatching {
        val uri = URI(rawUri)
        require(uri.scheme == SCHEME && uri.host == AUTHORITY) { "not a DeskLink media reference" }
        val segments = uri.rawPath.trim('/').split('/')
        require(segments.size == 2) { "invalid media reference path" }
        val position = uri.rawQuery
            ?.split('&')
            ?.firstOrNull { it.startsWith("t=") }
            ?.substringAfter('=')
            ?.toLongOrNull()
            ?.takeIf { it >= 0L }
            ?: error("invalid media reference time")
        MediaTimeReference(decode(segments[0]), decode(segments[1]), position)
    }.getOrNull()

    fun displayText(media: MediaRef, positionMs: Long): String =
        "《${media.displayName}》 ${formatTime(positionMs)}"

    fun markdownLink(media: MediaRef, positionMs: Long): String =
        "[${escapeMarkdown(displayText(media, positionMs))}](${toUri(create(media, positionMs))})"

    fun appendToDraft(draft: String, referenceMarkdown: String): String = when {
        draft.isBlank() -> referenceMarkdown
        draft.endsWith('\n') -> draft + referenceMarkdown
        else -> "$draft\n$referenceMarkdown"
    }

    fun formatTime(positionMs: Long): String {
        val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private fun escapeMarkdown(value: String): String = value
        .replace("\\", "\\\\")
        .replace("[", "\\[")
        .replace("]", "\\]")
}
