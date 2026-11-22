package astralis.command.impl;

import astralis.command.BaseCommand;
import astralis.ui.effect.FlashEffect;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.SharedSuggestionProvider;

public class FlashCommand extends BaseCommand {
    private static final long DEFAULT_DURATION_MILLIS = 3000L;

    public FlashCommand() {

        super("flash", "Try it out.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        LiteralArgumentBuilder rootBuilder =
                (LiteralArgumentBuilder)
                        builder.executes(
                                context -> {
                                    FlashEffect flashEffect = FlashEffect.getInstance();

                                    flashEffect.startFlash(3000L);
                                    return 1;
                                });

        rootBuilder.then(
                argument("duration", LongArgumentType.longArg(100L, 10000L))
                        .executes(
                                context -> {
                                    long duration = LongArgumentType.getLong(context, "duration");
                                    FlashEffect.getInstance().startFlash(duration);
                                    return 1;
                                }));
    }
}
