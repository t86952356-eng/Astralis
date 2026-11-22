package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MicrosoftTokenResponse {
    @Expose
    @SerializedName("access_token")
    public String accessToken;

    @Expose
    @SerializedName("refresh_token")
    public String refreshToken;

    @Expose
    @SerializedName("error")
    public String error;

    @Expose
    @SerializedName("error_description")
    public String errorDescription;
}
