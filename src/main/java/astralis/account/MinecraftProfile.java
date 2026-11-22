package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MinecraftProfile {
    @Expose
    @SerializedName("id")
    public String id;

    @Expose
    @SerializedName("name")
    public String name;
}
