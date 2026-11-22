package astralis.account;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EntitlementsResponse {
    @Expose
    @SerializedName("items")
    private EntitlementItem[] items;

    private boolean ownsMinecraft() {
        boolean hasProduct = false;
        boolean hasGame = false;
        EntitlementItem[] entitlements = this.items;
        int length = entitlements.length;

        for (int i = 0; i < length; i++) {
            EntitlementItem item = entitlements[i];

            if (item.name.equals("product_minecraft")) {
                hasProduct = true;
            } else {

                if (item.name.equals("game_minecraft")) {
                    hasGame = true;
                }
            }
        }

        return hasProduct && hasGame;
    }
}
