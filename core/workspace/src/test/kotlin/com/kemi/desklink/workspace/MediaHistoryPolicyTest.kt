package com.kemi.desklink.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaHistoryPolicyTest {
    private val movie = MediaRef("smb", "movie-a", "Movie A", "smb://nas.example/media/movie-a.mkv")
    private val trailer = MediaRef("local", "trailer-b", "Trailer B", "content://video/trailer-b")

    @Test
    fun recordMovesOneSourceToFrontAndKeepsFavorite() {
        val initial = MediaHistoryPolicy.record(emptyList(), movie, 12_000L, 60_000L, nowEpochMs = 100L)
        val favorite = MediaHistoryPolicy.toggleFavorite(initial, movie, nowEpochMs = 101L)
        val withTrailer = MediaHistoryPolicy.record(favorite, trailer, 2_000L, 30_000L, nowEpochMs = 102L)
        val updatedMovie = MediaHistoryPolicy.record(withTrailer, movie, 20_000L, 60_000L, nowEpochMs = 103L)

        assertEquals(listOf(movie, trailer), updatedMovie.map { it.media })
        assertEquals(20_000L, updatedMovie.first().positionMs)
        assertTrue(updatedMovie.first().isFavorite)
    }

    @Test
    fun resumeSkipsNearStartAndCompletedEntries() {
        val nearStart = MediaHistoryEntry(movie, positionMs = 4_999L, durationMs = 60_000L, lastPlayedAtEpochMs = 1L)
        val middle = nearStart.copy(positionMs = 12_000L)
        val completed = nearStart.copy(positionMs = 56_000L)

        assertEquals(0L, MediaHistoryPolicy.resumablePosition(nearStart))
        assertEquals(12_000L, MediaHistoryPolicy.resumablePosition(middle))
        assertEquals(0L, MediaHistoryPolicy.resumablePosition(completed))
    }

    @Test
    fun toggleFavoriteCreatesOneCredentialFreeHistoryEntry() {
        val entries = MediaHistoryPolicy.toggleFavorite(emptyList(), movie, nowEpochMs = 1L)
        assertEquals(1, entries.size)
        assertTrue(entries.single().isFavorite)

        val removed = MediaHistoryPolicy.toggleFavorite(entries, movie, nowEpochMs = 2L)
        assertFalse(removed.single().isFavorite)
    }
}
