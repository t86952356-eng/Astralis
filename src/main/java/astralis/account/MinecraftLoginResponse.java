package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MinecraftLoginResponse {
    @Expose
    @SerializedName("access_token")
    public String accessToken;
}
