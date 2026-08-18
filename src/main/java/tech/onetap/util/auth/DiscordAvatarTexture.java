package tech.onetap.util.auth;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import tech.onetap.Onetap;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

public class DiscordAvatarTexture {
    private static final HttpClient AVATAR_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Identifier textureId;
    private volatile boolean requested;
    private volatile boolean loading;
    private volatile byte[] pendingBytes;
    private NativeImageBackedTexture texture;

    public DiscordAvatarTexture(Identifier textureId) {
        this.textureId = textureId;
    }

    public void resetRequestIfIdle() {
        if (texture == null && !loading) {
            requested = false;
        }
    }

    public void requestLoad() {
        if (requested || loading || texture != null) {
            return;
        }

        String discordId = null;
        String discordAvatar = null;
        try {
            var auth = Onetap.getInstance().getAuthManager();
            if (auth != null) {
                discordId = auth.getDiscordId();
                discordAvatar = auth.getDiscordAvatar();
            }
        } catch (Throwable ignored) {
        }

        if (discordId == null || discordId.isBlank() || discordAvatar == null || discordAvatar.isBlank()) {
            requested = true;
            return;
        }

        final String resolvedDiscordId = discordId;
        final String resolvedDiscordAvatar = discordAvatar;
        loading = true;
        CompletableFuture.runAsync(() -> {
            try {
                byte[] downloadedBytes = downloadAvatarBytes(resolvedDiscordId, resolvedDiscordAvatar);
                if (downloadedBytes != null && downloadedBytes.length > 0) {
                    pendingBytes = downloadedBytes;
                }
            } finally {
                requested = true;
                loading = false;
            }
        });
    }

    public void updateTexture(MinecraftClient client) {
        if (client == null || texture != null || pendingBytes == null) {
            return;
        }

        byte[] avatarBytes = pendingBytes;
        pendingBytes = null;

        try (ByteArrayInputStream stream = new ByteArrayInputStream(avatarBytes)) {
            NativeImage image = NativeImage.read(stream);
            if (image == null) {
                return;
            }

            texture = new NativeImageBackedTexture(image);
            client.getTextureManager().registerTexture(textureId, texture);
        } catch (Throwable ignored) {
            texture = null;
        }
    }

    public int getTextureId(IntSupplier fallbackSupplier) {
        if (texture != null) {
            try {
                return texture.getGlId();
            } catch (Throwable ignored) {
            }
        }
        return fallbackSupplier.getAsInt();
    }

    private byte[] downloadAvatarBytes(String discordId, String discordAvatar) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cdn.discordapp.com/avatars/" + discordId + "/" + discordAvatar + ".png?size=128"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = AVATAR_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
