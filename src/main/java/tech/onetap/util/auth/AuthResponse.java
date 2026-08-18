package tech.onetap.util.auth;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AuthResponse {
    @SerializedName("authorized")
    private boolean authorized;
    @SerializedName("uid")
    private int uid;
    @SerializedName("discord_username")
    private String discord_username;
    @SerializedName("token")
    private String token;
    @SerializedName("expires_at")
    private long expires_at;
    @SerializedName("hwid_resets_left")
    private int hwid_resets_left;
    @SerializedName("client_version")
    private String client_version;
    @SerializedName("reason")
    private String reason;
    @SerializedName("message")
    private String message;
    @SerializedName("allowed_versions")
    private String[] allowed_versions;
}
