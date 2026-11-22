package astralis.scripting;

import net.minecraft.ChatFormatting;

public record PendingScriptMessage(
        String name, String message, String copyText, ChatFormatting color) {}
