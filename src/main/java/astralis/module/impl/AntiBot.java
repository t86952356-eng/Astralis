package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import pub.astralis.client.module.core.Category;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public class AntiBot extends Module {
    private static BooleanSetting isInTablist;
    private static BooleanSetting validName;
    private static BooleanSetting nameStartsWith;
    private static BooleanSetting age;
    private static BooleanSetting latency;
    private static BooleanSetting neverTouchedGround;
    private static BooleanSetting scale;
    private static BooleanSetting checkMonsters;
    private static BooleanSetting checkAnimals;
    private static BooleanSetting checkAllMobs;
    private final SettingGroup playerFiltersGroup;
    private final SettingGroup entityFiltersGroup;
    private static Pattern validNamePattern;
    private static Set<LivingEntity> legitimateEntities;

    public AntiBot() {

        super(MODULE_ID, "", "", Category.Misc);

        SettingGroup settingGroup = new SettingGroup(PLAYER_FILTERS_GROUP_ID);
        Setting[] playerFilters =
                new Setting[] {
                    isInTablist, validName, nameStartsWith, age, latency, neverTouchedGround, null
                };

        playerFilters[6] = scale;
        this.playerFiltersGroup = settingGroup.addValues(playerFilters);

        this.entityFiltersGroup =
                new SettingGroup(ENTITY_FILTERS_GROUP_ID)
                        .addValues(checkMonsters, checkAnimals, checkAllMobs);
        this.addSettings(this.playerFiltersGroup, this.entityFiltersGroup);
    }

    private static boolean isBotPlayer(Player player2) {
        PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player2.getUUID());
        if (age.getValue()) {

            if (player2.tickCount < 20) {
                return true;
            }
        }

        if (!latency.getValue() || playerInfo != null && playerInfo.getLatency() > 0) {
            if (scale.getValue() && player2.getScale() != mc.player.getScale()) {
                return true;
            }

            if (neverTouchedGround.getValue()) {

                if (player2.tickCount > 20
                        && !player2.onGround()
                        && player2.fallDistance == 0.0
                        && player2.getDeltaMovement().y == 0.0) {
                    return true;
                }
            }

            if (isInTablist.getValue() && !isInTablist(player2)) {
                return true;
            }

            if (validName.getValue()) {
                String playerName = player2.getGameProfile().name();
                if (hasInvalidName(playerName)) {
                    return true;
                }

                if (nameStartsWith.getValue()) {

                    if (playerName.startsWith("[NPC]")) {
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static boolean isBot(LivingEntity livingEntity) {
        AntiBot antiBot = Astralis.getInstance().getModuleManager().getModule(AntiBot.class);
        if (antiBot == null
                || !antiBot.isEnabled()
                || livingEntity == null
                || livingEntity == mc.player) {
            return false;
        }

        if (legitimateEntities.contains(livingEntity)) {
            return false;
        }

        boolean bot =
                livingEntity instanceof Player player2
                        ? isBotPlayer(player2)
                        : isBotMob(livingEntity);
        if (!bot) {
            legitimateEntities.add(livingEntity);
        }

        return bot;
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        legitimateEntities.clear();
    }

    private static boolean isBotMob(LivingEntity livingEntity) {
        boolean shouldCheck =
                livingEntity instanceof Monster && checkMonsters.getValue()
                        || livingEntity instanceof Animal && checkAnimals.getValue()
                        || checkAllMobs.getValue();
        return !shouldCheck ? false : livingEntity.isDeadOrDying();
    }

    @Override
    public void onDisable() {
        legitimateEntities.clear();
    }

    private static boolean hasInvalidName(String name) {
        return name == null
                || !validNamePattern.matcher(name).matches()
                || name.contains(" ")
                || name.contains("§");
    }

    private static boolean isInTablist(Player player2) {
        if (mc.getConnection() == null) {
            return false;
        }

        PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player2.getUUID());
        return playerInfo != null
                && mc.getConnection().getListedOnlinePlayers().contains(playerInfo);
    }

    static {
        isInTablist = new BooleanSetting(AntiBot.IS_IN_TABLIST_SETTING_ID, true);
        validName = new BooleanSetting(AntiBot.VALID_NAME_SETTING_ID, true);
        nameStartsWith = new BooleanSetting(AntiBot.NAME_STARTS_WITH_SETTING_ID, true);
        age = new BooleanSetting(AntiBot.AGE_SETTING_ID, true);
        latency = new BooleanSetting(AntiBot.LATENCY_SETTING_ID, true);
        neverTouchedGround = new BooleanSetting(AntiBot.NEVER_TOUCHED_GROUND_SETTING_ID, true);
        scale = new BooleanSetting(AntiBot.SCALE_SETTING_ID, true);
        checkMonsters = new BooleanSetting(AntiBot.CHECK_MONSTERS_SETTING_ID, true);
        checkAnimals = new BooleanSetting(AntiBot.CHECK_ANIMALS_SETTING_ID, false);
        checkAllMobs = new BooleanSetting(AntiBot.CHECK_ALL_MOBS_SETTING_ID, false);
        validNamePattern = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
        legitimateEntities = Collections.newSetFromMap(new WeakHashMap<>());
    }

    private static final String MODULE_ID = "568c0d4230e37663cb609ec9eadd3d23";
    private static final String PLAYER_FILTERS_GROUP_ID = "0303dcc33b69494367dc9212b7a8bd1d";
    private static final String ENTITY_FILTERS_GROUP_ID = "1c605406b8f35b1a635da4f9a7b8b565";
    private static final String IS_IN_TABLIST_SETTING_ID = "f910c6565948a263cfa72a2ae4e5f52b";
    private static final String VALID_NAME_SETTING_ID = "fb98453e0812b1696e0f44fde0b30adc";
    private static final String NAME_STARTS_WITH_SETTING_ID = "8f3dccb57ecba03f7ae260496b6fc2f0";
    private static final String AGE_SETTING_ID = "31c8e300e2cbe84762247c77b9400540";
    private static final String LATENCY_SETTING_ID = "022005c43b68a6652247ae2c7ffb958d";
    private static final String NEVER_TOUCHED_GROUND_SETTING_ID =
            "e10378302009b827eae4b2c8f6b12e9f266d3a278d84aed91e024937b87f63c2";
    private static final String SCALE_SETTING_ID = "137a749a1d2ba133748fc087474416fc";
    private static final String CHECK_MONSTERS_SETTING_ID = "4cb9e778dc7c685b68d02600c9a76336";
    private static final String CHECK_ANIMALS_SETTING_ID = "605ae12b558a79b5d6a4cf4eba69f534";
    private static final String CHECK_ALL_MOBS_SETTING_ID = "c545e76cc4c2ca5efc623121718bd01c";
}
