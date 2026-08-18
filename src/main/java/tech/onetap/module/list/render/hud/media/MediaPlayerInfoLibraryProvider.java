package tech.onetap.module.list.render.hud.media;

import net.minecraft.client.MinecraftClient;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


public class MediaPlayerInfoLibraryProvider implements MediaInfoProvider {
    private static final long POLL_INTERVAL_MS = 1000;
    private final AtomicReference<MediaInfo> cached = new AtomicReference<>(MediaInfo.empty());
    private volatile long lastPollTime = 0;
    private volatile boolean isPolling = false;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public MediaInfo getCurrentMedia() {
        if (isPolling) return cached.get();
        if (System.currentTimeMillis() - lastPollTime < POLL_INTERVAL_MS) return cached.get();

        lastPollTime = System.currentTimeMillis();
        isPolling = true;
        Thread.ofVirtual().name("Media-Polling").start(() -> {
            try {
                updateMediaInfo();
            } catch (Throwable ignored) {
            } finally {
                isPolling = false;
            }
        });
        return cached.get();
    }

    @SuppressWarnings("unchecked")
    private void updateMediaInfo() {
        try {
            List<?> sessions = null;
            try {
                sessions = dev.redstones.mediaplayerinfo.MediaPlayerInfo.Instance.getMediaSessions();
            } catch (Throwable t) {
                try {
                    Object instance = dev.redstones.mediaplayerinfo.MediaPlayerInfo.class
                            .getField("Instance").get(null);
                    Method m = instance.getClass().getMethod("getMediaSessions");
                    sessions = (List<?>) m.invoke(instance);
                } catch (Throwable ignored) {
                }
            }

            if (sessions == null || sessions.isEmpty()) {
                clearThumbnail();
                cached.set(MediaInfo.empty());
                return;
            }

            dev.redstones.mediaplayerinfo.IMediaSession best = null;
            for (Object s : sessions) {
                if (s == null) continue;
                try {
                    Object status = s.getClass().getMethod("getPlaybackStatus").invoke(s);
                    if (status != null && status.toString().toUpperCase().contains("PLAYING")) {
                        dev.redstones.mediaplayerinfo.MediaInfo mi = ((dev.redstones.mediaplayerinfo.IMediaSession) s).getMedia();
                        if (mi != null && mi.getTitle() != null && !mi.getTitle().isEmpty()) {
                            best = (dev.redstones.mediaplayerinfo.IMediaSession) s;
                            break;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            if (best == null) {
                for (Object s : sessions) {
                    if (s == null) continue;
                    try {
                        dev.redstones.mediaplayerinfo.MediaInfo mi = ((dev.redstones.mediaplayerinfo.IMediaSession) s).getMedia();
                        if (mi != null && mi.getTitle() != null && !mi.getTitle().isEmpty()) {
                            best = (dev.redstones.mediaplayerinfo.IMediaSession) s;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            if (best != null) {
                dev.redstones.mediaplayerinfo.MediaInfo info = best.getMedia();
                String title = info.getTitle();
                String artist = info.getArtist() != null ? info.getArtist() : "";

                if (artist.isEmpty() || artist.equalsIgnoreCase("Unknown Artist")) {
                    if (title != null && title.contains(" - ")) {
                        String[] parts = title.split(" - ", 2);
                        artist = parts[0].trim();
                        title = parts[1].trim();
                    }
                }

                String cleanTitle = (title != null ? title : "")
                        .replace(" - SoundCloud", "").replace(" | SoundCloud", "")
                        .replace(" - YouTube Music", "").replace(" - YouTube", "").trim();
                String cleanArtist = artist.isEmpty() ? "Unknown Artist" :
                        artist.replace("SoundCloud", "").replace("YouTube", "").trim();

                long dur = 0, pos = 0;
                try {
                    dur = info.getDuration();
                    pos = info.getPosition();
                } catch (Throwable ignored) {
                }

                if (dur > 0) {
                    if (dur < 10000) {
                        dur *= 1000;
                        pos *= 1000;
                    } else if (dur > 100000000) {
                        dur /= 10000;
                        pos /= 10000;
                    }
                } else {
                    dur = 0;
                    pos = 0;
                }

                boolean playing = false;
                try {
                    Object status = best.getClass().getMethod("getPlaybackStatus").invoke(best);
                    playing = status != null && status.toString().toUpperCase().contains("PLAYING");
                } catch (Throwable ignored) {
                }

                byte[] artwork = info.getArtworkPng();
                Optional<byte[]> coverOpt = (artwork != null && artwork.length > 0) ? Optional.of(artwork) : Optional.empty();

                Optional<Integer> durSec = dur > 0 ? Optional.of((int) (dur / 1000)) : Optional.empty();
                Optional<Long> durMs = dur > 0 ? Optional.of(dur) : Optional.empty();
                Optional<Long> posMs = dur > 0 ? Optional.of(Math.max(0, pos)) : Optional.empty();

                MediaInfo result = new MediaInfo(cleanTitle, cleanArtist, durSec, coverOpt,
                        durMs, posMs, playing, System.currentTimeMillis());
                cached.set(result);

                if (coverOpt.isPresent()) {
                    byte[] bytes = coverOpt.get();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.execute(() -> updateThumbnail(bytes));
                    }
                }
            } else {
                clearThumbnail();
                cached.set(MediaInfo.empty());
            }
        } catch (Throwable ignored) {
            clearThumbnail();
            cached.set(MediaInfo.empty());
        }
    }

    private void clearThumbnail() {
        net.minecraft.util.Identifier id = lastThumbnailId;
        lastThumbnailId = null;
        lastThumbnailBytes = null;
        if (id != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    try { mc.getTextureManager().destroyTexture(id); } catch (Throwable ignored) {}
                });
            }
        }
    }

    private byte[] lastThumbnailBytes;
    private net.minecraft.util.Identifier lastThumbnailId;

    private void updateThumbnail(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        if (Arrays.equals(bytes, lastThumbnailBytes)) return;
        lastThumbnailBytes = bytes;
        try {
            net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(new ByteArrayInputStream(bytes));
            if (img == null) return;
            net.minecraft.client.texture.NativeImageBackedTexture tex = new net.minecraft.client.texture.NativeImageBackedTexture(img);
            net.minecraft.util.Identifier id = net.minecraft.util.Identifier.of("onetap", "media/thumb_" + System.currentTimeMillis());
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && lastThumbnailId != null) {
                try {
                    mc.getTextureManager().destroyTexture(lastThumbnailId);
                } catch (Throwable ignored) {
                }
            }
            if (mc != null) {
                mc.getTextureManager().registerTexture(id, tex);
                lastThumbnailId = id;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Optional<net.minecraft.util.Identifier> getCoverTextureId() {
        return Optional.ofNullable(lastThumbnailId);
    }
}
