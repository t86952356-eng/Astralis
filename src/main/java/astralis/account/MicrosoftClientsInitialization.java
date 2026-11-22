package astralis.account;

import astralis.social.model.MicrosoftClient;

import net.minecraft.ChatFormatting;

public final class MicrosoftClientsInitialization {
    public static MicrosoftClient iasClient =
            new MicrosoftClient("54fd49e4-2103-4044-9603-2b028c814ec3", "IAS", ChatFormatting.RED);
    public static MicrosoftClient launcherClient =
            new MicrosoftClient("00000000402b5328", "Minecraft Launcher", ChatFormatting.GREEN);
}
