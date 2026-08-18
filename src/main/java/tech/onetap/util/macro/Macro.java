package tech.onetap.util.macro;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Macro {
    @SerializedName("message")
    String message;
    @SerializedName("key")
    int key;

    public String message() {
        return message;
    }

    public int key() {
        return key;
    }
}