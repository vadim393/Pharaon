package tech.onetap.util.friend;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Friend {
    @SerializedName("name")
    String name;

    public String name() {
        return name;
    }
}