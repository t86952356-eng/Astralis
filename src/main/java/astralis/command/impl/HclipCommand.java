package astralis.command.impl;

import astralis.command.BaseCommand;

import com.astralis.scripting.InstanceAccess;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Locale;

public class HclipCommand extends BaseCommand implements InstanceAccess {
    public HclipCommand() {

        String[] aliases = new String[1];

        aliases[0] = "hc";
        super("hclip", "Horizontal teleport.", aliases);
    }

    private int horizontalClip(CommandContext<SharedSuggestionProvider> context) {

        double distance = this.getDoubleArgument(context, "distance");
        double yawRadians = Math.toRadians(mc.player.getYRot());
        double newX = mc.player.getX() - Math.sin(yawRadians) * distance;
        double newZ = mc.player.getZ() + Math.cos(yawRadians) * distance;
        mc.player.setPos(newX, mc.player.getY(), newZ);
        String primaryColor = String.valueOf(ChatFormatting.GRAY);
        String whiteColor = String.valueOf(ChatFormatting.WHITE);

        String formattedDistance = String.format(Locale.ROOT, "%.2f", distance);
        String trailingColor = String.valueOf(ChatFormatting.GRAY);
        String distanceText = formattedDistance;
        String highlightColor = whiteColor;

        this.printMessage(
                primaryColor
                        + "H-Clipped "
                        + highlightColor
                        + distanceText
                        + trailingColor
                        + " blocks.");
        return 1;
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        builder.then(
                argument("distance", DoubleArgumentType.doubleArg())
                        .executes(this::horizontalClip));
    }
}
