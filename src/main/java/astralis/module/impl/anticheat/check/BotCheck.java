package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiBot;
import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;

import net.minecraft.world.entity.player.Player;

/** Identifies AntiBot matches, with optional reporting through the AntiCheat flag sink. */
public class BotCheck extends Check {
    public BotCheck(AntiCheat antiCheat, BooleanSetting enabledSetting) {
        super(antiCheat, CheckType.BOT, enabledSetting);
    }

    /** Returns true for a bot even when this check is disabled; only reporting is conditional. */
    public boolean isBot(Player player, PlayerCheckState state) {
        if (!AntiBot.isBot(player)) {
            return false;
        }

        if (!this.isCheckEnabled()) {
            return true;
        }

        this.flag(player, state, "npc/movement profile");
        return true;
    }
}
