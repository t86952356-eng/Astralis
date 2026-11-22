package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.module.impl.misc.HypixelStats;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.SharedSuggestionProvider;

public class StatsCommand extends BaseCommand {
    public StatsCommand() {

        super("stats", "Look up Hypixel Bedwars stats for a player");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        builder.then(
                        argument("player", StringArgumentType.string())
                                .executes(
                                        context -> {
                                            String playerName =
                                                    StringArgumentType.getString(context, "player");
                                            HypixelStats hypixelStats =
                                                    Astralis.getInstance()
                                                            .getModuleManager()
                                                            .getModule(HypixelStats.class);
                                            hypixelStats.printPlayerStats(playerName);
                                            return 1;
                                        }))
                .executes(
                        usageContext -> {
                            this.printMessage("§cUsage: .stats <player>");
                            return 1;
                        });
    }
}
