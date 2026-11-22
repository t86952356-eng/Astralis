package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class XuiClaim {
    @Expose
    @SerializedName("uhs")
    public String userHash;

    private XuiClaim() {}
}
