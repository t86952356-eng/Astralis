package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EntitlementItem {
    @Expose
    @SerializedName("name")
    public String name;

    private EntitlementItem() {}
}
