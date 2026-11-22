package astralis;

import astralis.util.ChatUtil;

import net.minecraft.client.Minecraft;

/** User-facing status for features that require the removed client backend. */
public final class OfflineServices {
    private OfflineServices() {}

    public static String profileName() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static void unavailable(String featureName) {
        ChatUtil.print(featureName + " is unavailable without the Astralis backend.");
    }
}
