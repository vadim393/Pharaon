package tech.onetap.ui.punch.pages.friends;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class FriendSkinCache {
    private static final ConcurrentHashMap<String, Supplier<Identifier>> SKINS = new ConcurrentHashMap<>();
    private static final Set<String> RESOLVING = ConcurrentHashMap.newKeySet();

    private FriendSkinCache() {
    }

    static Identifier texture(MinecraftClient minecraft, String name) {
        PlayerListEntry online = onlineInfo(minecraft, name);
        String key = normalize(name);
        if (online != null) {
            Supplier<Identifier> skin = () -> online.getSkinTextures().texture();
            SKINS.put(key, skin);
            return online.getSkinTextures().texture();
        }

        Supplier<Identifier> fallback = SKINS.computeIfAbsent(key, ignored -> defaultSkin(name));
        resolve(minecraft, name, key);
        return fallback.get();
    }

    static boolean isOnline(MinecraftClient minecraft, String name) {
        return onlineInfo(minecraft, name) != null;
    }

    private static PlayerListEntry onlineInfo(MinecraftClient minecraft, String name) {
        if (minecraft.getNetworkHandler() == null) {
            return null;
        }
        return minecraft.getNetworkHandler().getPlayerList().stream()
                .filter(info -> info.getProfile().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private static void resolve(MinecraftClient minecraft, String name, String key) {
        if (!RESOLVING.add(key)) {
            return;
        }
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, name);
        CompletableFuture
                .supplyAsync(() -> minecraft.getSkinProvider().getSkinTextures(profile), Util.getIoWorkerExecutor())
                .thenAccept(skinTextures -> minecraft.execute(() ->
                        SKINS.put(key, () -> skinTextures.texture())))
                .whenComplete((unused, throwable) -> RESOLVING.remove(key));
    }

    private static Supplier<Identifier> defaultSkin(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return () -> DefaultSkinHelper.getSkinTextures(uuid).texture();
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}