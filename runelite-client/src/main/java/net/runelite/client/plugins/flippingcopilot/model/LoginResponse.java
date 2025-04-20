package net.runelite.client.plugins.flippingcopilot.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    public boolean error;
    public String message;
    public String jwt;

    @SerializedName("user_id")
    public int userId;
}
