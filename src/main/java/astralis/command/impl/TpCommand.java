package astralis.command.impl;

import astralis.command.BaseCommand;

import com.astralis.scripting.InstanceAccess;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Locale;

public class TpCommand extends BaseCommand implements InstanceAccess {
    public TpCommand() {

        String[] aliases = new String[1];

        aliases[0] = "teleport";
        super("tp", "Teleport to coordinates.", aliases);
    }

    private int teleport(CommandContext<SharedSuggestionProvider> context) {
        double x = this.getDoubleArgument(context, "x");
        double y = this.getDoubleArgument(context, "y");
        double z = this.getDoubleArgument(context, "z");
        mc.player.setPos(x, y, z);
        String leadingColor = String.valueOf(ChatFormatting.GRAY);
        String xColor = String.valueOf(ChatFormatting.WHITE);

        String formattedX = String.format(Locale.ROOT, "%.2f", x);
        String yColor = String.valueOf(ChatFormatting.WHITE);

        String formattedY = String.format(Locale.ROOT, "%.2f", y);
        String zColor = String.valueOf(ChatFormatting.WHITE);

        String formattedZ = String.format(Locale.ROOT, "%.2f", z);
        String zPrefix = zColor;
        String yText = formattedY;
        String yPrefix = yColor;
        String xText = formattedX;
        String xPrefix = xColor;
        this.printMessage(
                leadingColor
                        + "Teleported to "
                        + xPrefix
                        + xText
                        + " "
                        + yPrefix
                        + yText
                        + " "
                        + zPrefix
                        + formattedZ);
        return 1;
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(
                argument("x", DoubleArgumentType.doubleArg())
                        .suggests(
                                (context, suggestionsBuilder) -> {
                                    suggestionsBuilder.suggest(
                                            String.format(Locale.ROOT, "%.2f", mc.player.getX()));
                                    return suggestionsBuilder.buildFuture();
                                })
                        .then(
                                argument("y", DoubleArgumentType.doubleArg())
                                        .suggests(
                                                (context, suggestionsBuilder) -> {
                                                    suggestionsBuilder.suggest(
                                                            String.format(
                                                                    Locale.ROOT,
                                                                    "%.2f",
                                                                    mc.player.getY()));
                                                    return suggestionsBuilder.buildFuture();
                                                })
                                        .then(
                                                argument("z", DoubleArgumentType.doubleArg())
                                                        .suggests(
                                                                (context, suggestionsBuilder) -> {
                                                                    suggestionsBuilder.suggest(
                                                                            String.format(
                                                                                    Locale.ROOT,
                                                                                    "%.2f",
                                                                                    mc.player
                                                                                            .getZ()));
                                                                    return suggestionsBuilder
                                                                            .buildFuture();
                                                                })
                                                        .executes(this::teleport))));
    }
}
