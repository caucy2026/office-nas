package com.kemi.desklink.platform;

import android.content.Context;

import com.kemi.desklink.workspace.MediaHistoryEntry;
import com.kemi.desklink.workspace.MediaHistoryPolicy;
import com.kemi.desklink.workspace.MediaRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small, credential-free persisted media library. It stores only already-sanitized MediaRef data,
 * playback progress, and a favorite flag; NAS passwords belong in a future Keystore provider.
 */
public final class MediaLibraryRepository {
    private static final String PREFS = "desklink_media_library";
    private static final String KEY_ENTRIES = "entries";

    private final android.content.SharedPreferences preferences;

    public MediaLibraryRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<MediaHistoryEntry> load() {
        String encoded = preferences.getString(KEY_ENTRIES, "[]");
        if (encoded == null) return Collections.emptyList();
        try {
            JSONArray array = new JSONArray(encoded);
            List<MediaHistoryEntry> entries = new ArrayList<>();
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) continue;
                String provider = item.optString("provider", "");
                String assetId = item.optString("assetId", "");
                String uri = item.optString("uri", "");
                if (provider.isEmpty() || assetId.isEmpty() || uri.isEmpty()) continue;
                entries.add(new MediaHistoryEntry(
                        new MediaRef(provider, assetId, item.optString("displayName", assetId), uri),
                        Math.max(0L, item.optLong("positionMs", 0L)),
                        Math.max(0L, item.optLong("durationMs", 0L)),
                        Math.max(0L, item.optLong("lastPlayedAtEpochMs", 0L)),
                        item.optBoolean("isFavorite", false)
                ));
            }
            return entries;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    public List<MediaHistoryEntry> record(
            MediaRef media,
            long positionMs,
            long durationMs
    ) {
        List<MediaHistoryEntry> updated = MediaHistoryPolicy.INSTANCE.record(
                load(), media, positionMs, durationMs, System.currentTimeMillis());
        save(updated);
        return updated;
    }

    public List<MediaHistoryEntry> toggleFavorite(MediaRef media) {
        List<MediaHistoryEntry> updated = MediaHistoryPolicy.INSTANCE.toggleFavorite(
                load(), media, System.currentTimeMillis());
        save(updated);
        return updated;
    }

    public MediaHistoryEntry find(MediaRef media) {
        for (MediaHistoryEntry entry : load()) {
            MediaRef candidate = entry.getMedia();
            if (candidate.getProvider().equals(media.getProvider()) && candidate.getAssetId().equals(media.getAssetId())) {
                return entry;
            }
        }
        return null;
    }

    public boolean isFavorite(MediaRef media) {
        MediaHistoryEntry entry = find(media);
        return entry != null && entry.isFavorite();
    }

    private void save(List<MediaHistoryEntry> entries) {
        JSONArray array = new JSONArray();
        for (MediaHistoryEntry entry : entries) {
            MediaRef media = entry.getMedia();
            JSONObject item = new JSONObject();
            try {
                item.put("provider", media.getProvider());
                item.put("assetId", media.getAssetId());
                item.put("displayName", media.getDisplayName());
                item.put("uri", media.getUri());
                item.put("positionMs", entry.getPositionMs());
                item.put("durationMs", entry.getDurationMs());
                item.put("lastPlayedAtEpochMs", entry.getLastPlayedAtEpochMs());
                item.put("isFavorite", entry.isFavorite());
                array.put(item);
            } catch (Exception ignored) {
                // JSON values are primitives; skip only a malformed in-memory entry.
            }
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply();
    }
}
