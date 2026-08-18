package tech.onetap.util.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthManager {
    private static final String API_URL = "https://onetap-auth.wishen92.workers.dev";
    private static final Gson GSON = new Gson();
    
    @Getter
    private String sessionToken;
    @Getter
    private String discordUsername;
    @Getter
    private int uid;
    @Getter
    private String discordId;
    @Getter
    private String discordAvatar;
    @Getter
    private String hwid;
    @Getter
    private String ip;
    
    public boolean authenticate() {
        System.out.println("[Pharaon Auth] Authentication disabled");
        this.sessionToken = "local";
        this.discordUsername = "";
        this.discordId = "";
        this.discordAvatar = null;
        this.uid = 0;
        this.hwid = HWIDUtil.generateHWID();
        this.ip = "local";
        return true;
    }
    
    private void sendDiscordNotRunningNotificationSync() throws Exception {
        System.out.println("[Pharaon Auth] Sending Discord not running notification...");
        
        // Get HWID and IP even without Discord
        String hwid = HWIDUtil.generateHWID();
        String ip = getPublicIP();
        HWIDUtil.HardwareInfo hardware = HWIDUtil.getHardwareInfo();
        
        // Create notification payload
        JsonObject payload = new JsonObject();
        payload.addProperty("reason", "Discord not running");
        payload.addProperty("reasonCode", "DISCORD_NOT_RUNNING");
        payload.addProperty("hwid", hwid);
        payload.addProperty("ip", ip);
        
        JsonObject hardwareObj = new JsonObject();
        hardwareObj.addProperty("cpu", hardware.getCpu());
        hardwareObj.addProperty("gpu", hardware.getGpu());
        hardwareObj.addProperty("ram", hardware.getRam());
        payload.add("hardware", hardwareObj);
        
        // Send to webhook endpoint
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/api/webhook/unauthorized"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .timeout(java.time.Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            System.out.println("[Pharaon Auth] Notification sent successfully");
        } else {
            System.err.println("[Pharaon Auth] Failed to send notification: " + response.statusCode());
        }
    }
    
    private String getPublicIP() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ipify.org"))
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
