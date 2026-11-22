package astralis.command.impl;

import astralis.command.BaseCommand;

import com.astralis.scripting.InstanceAccess;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Locale;

public class VclipCommand extends BaseCommand implements InstanceAccess {

    public VclipCommand() {

        String[] aliases = new String[1];

        aliases[0] = "vc";
        super("vclip", "Vertical teleport.", aliases);
    }

    private int verticalClip(CommandContext<SharedSuggestionProvider> context) {

        double distance = this.getDoubleArgument(context, "distance");
        mc.player.setPos(mc.player.getX(), mc.player.getY() + distance, mc.player.getZ());
        String primaryColor = String.valueOf(ChatFormatting.GRAY);
        String whiteColor = String.valueOf(ChatFormatting.WHITE);

        String formattedDistance = String.format(Locale.ROOT, "%.2f", distance);
        String trailingColor = String.valueOf(ChatFormatting.GRAY);
        String distanceText = formattedDistance;
        String highlightColor = whiteColor;

        this.printMessage(
                primaryColor
                        + "V-Clipped "
                        + highlightColor
                        + distanceText
                        + trailingColor
                        + " blocks.");
        return 1;
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        builder.then(
                argument("distance", DoubleArgumentType.doubleArg()).executes(this::verticalClip));
    }
}
