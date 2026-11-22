package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class XboxAuthResponse {
    @Expose
    @SerializedName("Token")
    public String token;

    @Expose
    @SerializedName("DisplayClaims")
    public DisplayClaims displayClaims;
}
