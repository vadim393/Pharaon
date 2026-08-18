package tech.onetap.module.list.render.hud.media;

import java.util.Optional;

/**
 * Информация о текущем воспроизводимом треке.
 */
public record MediaInfo(
        String title,
        String artist,
        Optional<Integer> durationSeconds,
        Optional<byte[]> coverImage,
        Optional<Long> durationMs,
        Optional<Long> positionMs,
        boolean isPlaying,
        long lastUpdateTime
) {
    public static MediaInfo empty() {
        return new MediaInfo("", "", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), false, 0L);
    }

    public boolean isEmpty() {
        return (title == null || title.isBlank()) && (artist == null || artist.isBlank());
    }

    public String getDisplayTitle() {
        return title != null && !title.isBlank() ? title : "—";
    }

    public String getDisplayArtist() {
        return artist != null && !artist.isBlank() ? artist : "—";
    }

    public String getFormattedDuration() {
        return durationSeconds.map(sec -> {
            int m = sec / 60;
            int s = sec % 60;
            return String.format("%d:%02d", m, s);
        }).orElse("—");
    }

    /** Текущая позиция в мс с учётом воспроизведения */
    public long getEffectivePositionMs() {
        if (!isPlaying || durationMs.isEmpty() || positionMs.isEmpty()) {
            return positionMs.orElse(0L);
        }
        long pos = positionMs.get();
        long dur = durationMs.get();
        if (dur <= 0) return pos;
        long elapsed = System.currentTimeMillis() - lastUpdateTime;
        if (elapsed > 0 && elapsed < 2000) {
            pos = Math.min(pos + elapsed, dur);
        }
        return pos;
    }

    public float getProgress() {
        if (durationMs.isEmpty() || positionMs.isEmpty()) return 0f;
        long dur = durationMs.get();
        if (dur <= 0) return 0f;
        long pos = getEffectivePositionMs();
        return Math.max(0f, Math.min(1f, (float) pos / dur));
    }
}
