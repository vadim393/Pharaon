package tech.onetap.util.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;


public class DiscordUtil {
    private static final long APP_ID = 1459904567465476160L;
    private static final long FALLBACK_APP_ID = 1469299305268645991L;
    private static final Gson GSON = new Gson();


    public static DiscordInfo getDiscordInfo() {
        System.out.println("[OneTap Auth] Connecting to Discord IPC...");

        DiscordInfo info = tryNamedPipeFallback(APP_ID);
        if (info != null) return info;

        info = tryNamedPipeFallback(FALLBACK_APP_ID);
        if (info != null) return info;

        System.err.println("[OneTap Auth] ✗ Failed to connect to Discord!");
        System.err.println("[OneTap Auth]");
        System.err.println("[OneTap Auth] Please make sure:");
        System.err.println("[OneTap Auth] 1. Discord is running");
        System.err.println("[OneTap Auth] 2. You are logged in to Discord");
        System.err.println("[OneTap Auth] 3. Discord is not running in browser (desktop app required)");
        System.err.println("[OneTap Auth]");
        System.err.println("[OneTap Auth] Then restart OneTap client.");

        return null;
    }

    private static DiscordInfo tryNamedPipeFallback(long appId) {
        String[] prefixes = {"\\\\.\\pipe\\", "\\\\?\\pipe\\"};

        for (String prefix : prefixes) {
            for (int i = 0; i < 10; i++) {
                String pipePath = prefix + "discord-ipc-" + i;
                try (RandomAccessFile pipe = new RandomAccessFile(pipePath, "rw")) {
                    writeHandshake(pipe, appId);
                    DiscordInfo info = readReadyUser(pipe);
                    if (info != null) {
                        System.out.println("[OneTap Auth] ✓ Discord IPC connected (pipe fallback: " + pipePath + ")");
                        System.out.println("[OneTap Auth] - ID: " + info.id);
                        System.out.println("[OneTap Auth] - Username: " + info.username);
                        return info;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private static void writeHandshake(RandomAccessFile pipe, long appId) throws Exception {
        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", 1);
        handshake.addProperty("client_id", Long.toString(appId));

        byte[] payload = handshake.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0); // Handshake opcode
        header.putInt(payload.length);
        pipe.write(header.array());
        pipe.write(payload);
    }

    private static DiscordInfo readReadyUser(RandomAccessFile pipe) throws Exception {
        long deadline = System.currentTimeMillis() + 8000L;
        byte[] intBuf = new byte[4];

        while (System.currentTimeMillis() < deadline) {
            if (!readFully(pipe, intBuf, deadline)) {
                return null;
            }
            int opcode = ByteBuffer.wrap(intBuf).order(ByteOrder.LITTLE_ENDIAN).getInt();

            if (!readFully(pipe, intBuf, deadline)) {
                return null;
            }
            int length = ByteBuffer.wrap(intBuf).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (length <= 0 || length > 1024 * 1024) {
                return null;
            }

            byte[] payload = new byte[length];
            if (!readFully(pipe, payload, deadline)) {
                return null;
            }

            if (opcode != 1) continue; // Not a frame packet.

            JsonObject packet = GSON.fromJson(new String(payload, StandardCharsets.UTF_8), JsonObject.class);
            if (packet == null || !packet.has("evt") || !"READY".equals(packet.get("evt").getAsString())) {
                continue;
            }

            JsonObject data = packet.has("data") ? packet.getAsJsonObject("data") : null;
            JsonObject user = data != null && data.has("user") ? data.getAsJsonObject("user") : null;
            if (user == null || !user.has("id")) return null;

            String id = user.get("id").getAsString();
            String username = user.has("username") ? user.get("username").getAsString() : "unknown";
            String avatar = user.has("avatar") && !user.get("avatar").isJsonNull()
                    ? user.get("avatar").getAsString()
                    : null;

            return new DiscordInfo(id, username, avatar);
        }

        return null;
    }

    private static boolean readFully(RandomAccessFile file, byte[] buffer, long deadline) throws Exception {
        int read = 0;
        while (read < buffer.length) {
            int n = file.read(buffer, read, buffer.length - read);
            if (n > 0) {
                read += n;
                continue;
            }

            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(25);
        }
        return true;
    }

    @Data
    public static class DiscordInfo {
        private final String id;
        private final String username;
        private final String avatar;
    }
}