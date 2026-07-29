package com.kemi.desklink.workspace

/**
 * A credential-free media library entry. Provider credentials must never be added to [MediaRef]
 * or this history; network sources use their existing sanitized URI representation.
 */
data class MediaHistoryEntry(
    val media: MediaRef,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAtEpochMs: Long,
    val isFavorite: Boolean = false,
)

/** Pure history policy so ordering, resume, and favorite behavior stay testable outside Android. */
object MediaHistoryPolicy {
    const val MAX_RECENTS = 30
    private const val RESUME_MIN_MS = 5_000L
    private const val END_GUARD_MS = 5_000L

    fun record(
        existing: List<MediaHistoryEntry>,
        media: MediaRef,
        positionMs: Long,
        durationMs: Long,
        nowEpochMs: Long,
    ): List<MediaHistoryEntry> {
        val previous = existing.firstOrNull { it.media.sameSourceAs(media) }
        val updated = MediaHistoryEntry(
            media = media,
            positionMs = normalizedPosition(positionMs, durationMs),
            durationMs = durationMs.coerceAtLeast(0L),
            lastPlayedAtEpochMs = nowEpochMs,
            isFavorite = previous?.isFavorite ?: false,
        )
        return buildList {
            add(updated)
            addAll(existing.filterNot { it.media.sameSourceAs(media) })
        }.take(MAX_RECENTS)
    }

    fun toggleFavorite(
        existing: List<MediaHistoryEntry>,
        media: MediaRef,
        nowEpochMs: Long,
    ): List<MediaHistoryEntry> {
        val previous = existing.firstOrNull { it.media.sameSourceAs(media) }
        return if (previous == null) {
            record(existing, media, positionMs = 0L, durationMs = 0L, nowEpochMs = nowEpochMs)
                .map { entry -> if (entry.media.sameSourceAs(media)) entry.copy(isFavorite = true) else entry }
        } else {
            existing.map { entry ->
                if (entry.media.sameSourceAs(media)) entry.copy(isFavorite = !entry.isFavorite) else entry
            }
        }
    }

    fun resumablePosition(entry: MediaHistoryEntry?): Long {
        entry ?: return 0L
        val position = entry.positionMs.coerceAtLeast(0L)
        if (position < RESUME_MIN_MS) return 0L
        if (entry.durationMs > 0L && position >= entry.durationMs - END_GUARD_MS) return 0L
        return position
    }

    private fun normalizedPosition(positionMs: Long, durationMs: Long): Long {
        val position = positionMs.coerceAtLeast(0L)
        return if (durationMs > 0L) position.coerceAtMost(durationMs) else position
    }

    private fun MediaRef.sameSourceAs(other: MediaRef): Boolean =
        provider == other.provider && assetId == other.assetId
}
