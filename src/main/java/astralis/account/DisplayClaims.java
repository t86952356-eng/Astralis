package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DisplayClaims {
    @Expose
    @SerializedName("xui")
    public XuiClaim[] xui;

    private DisplayClaims() {}
}
