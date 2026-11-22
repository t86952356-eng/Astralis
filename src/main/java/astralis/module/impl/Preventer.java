package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.ContinueAttackEvent;
import astralis.event.impl.StartAttackEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.SettingGroup;
import astralis.util.InventoryUtil;
import astralis.util.player.TeamUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

import pub.astralis.client.module.core.Category;

public class Preventer extends Module {
    private final BooleanSetting cancelTeamAttacks;
    private final BooleanSetting cancelBotAttacks;
    private final BooleanSetting cancelObiPunch;
    private final BooleanSetting cancelGlowstoneMisplace;
    private final BooleanSetting cancelAttackOnMiss;
    private final BooleanSetting allowBlockHit;
    private final BooleanSetting allowAirHit;
    private final BooleanSetting weaponOnly;

    public Preventer() {

        super(MODULE_ID, "", "", Category.Misc);

        this.cancelTeamAttacks = new BooleanSetting(CANCEL_TEAM_ATTACKS_SETTING_ID, true);

        this.cancelBotAttacks = new BooleanSetting(CANCEL_BOT_ATTACKS_SETTING_ID, false);

        this.cancelObiPunch = new BooleanSetting(CANCEL_OBI_PUNCH_SETTING_ID, false);

        this.cancelGlowstoneMisplace =
                new BooleanSetting(CANCEL_GLOWSTONE_MISPLACE_SETTING_ID, false);

        this.cancelAttackOnMiss = new BooleanSetting(CANCEL_ATTACK_ON_MISS_SETTING_ID, false);

        this.allowBlockHit = new BooleanSetting(ALLOW_BLOCK_HIT_SETTING_ID, false);

        this.allowAirHit = new BooleanSetting(ALLOW_AIR_HIT_SETTING_ID, false);

        this.weaponOnly = new BooleanSetting(WEAPON_ONLY_SETTING_ID, false);
        SettingGroup settingGroup =
                new SettingGroup(ATTACKS_TO_CANCEL_GROUP_ID)
                        .addValues(
                                this.cancelTeamAttacks, this.cancelBotAttacks, this.cancelObiPunch);
        SettingGroup settingGroup2 =
                new SettingGroup(ATTACK_ON_MISS_SETTINGS_GROUP_ID)
                        .addValues(
                                this.cancelAttackOnMiss,
                                this.allowBlockHit,
                                this.allowAirHit,
                                this.weaponOnly);
        SettingGroup settingGroup3 =
                new SettingGroup(MISSES_TO_CANCEL_GROUP_ID)
                        .addValues(settingGroup2, this.cancelGlowstoneMisplace);
        this.allowBlockHit.hideIf(() -> !this.cancelAttackOnMiss.getValue());
        this.allowAirHit.hideIf(() -> !this.cancelAttackOnMiss.getValue());
        this.weaponOnly.hideIf(() -> !this.cancelAttackOnMiss.getValue());
        this.addSettings(settingGroup, settingGroup3);
    }

    @EventTarget
    public void onContinueAttack(ContinueAttackEvent continueAttackEvent) {
        if (this.cancelObiPunch.getValue()) {
            if (mc.hitResult instanceof BlockHitResult blockHitResult
                    && mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock()
                            == Blocks.OBSIDIAN) {
                continueAttackEvent.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        Entity entity2 = attackEvent.getTarget();
        if (entity2 instanceof LivingEntity livingEntity) {
            if (this.cancelTeamAttacks.getValue() && TeamUtil.isTeammate(livingEntity)) {
                attackEvent.setCancelled(true);
            }

            if (this.cancelBotAttacks.getValue() && AntiBot.isBot(livingEntity)) {
                attackEvent.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onStartAttack(StartAttackEvent event) {
        if (this.cancelAttackOnMiss.getValue() && mc.hitResult != null) {
            if (!this.weaponOnly.getValue() || InventoryUtil.isHoldingWeapon()) {
                if (this.cancelAttackOnMiss.getValue()
                        && (mc.hitResult.getType() == Type.MISS && !this.allowAirHit.getValue()
                                || mc.hitResult.getType() == Type.BLOCK
                                        && !this.allowBlockHit.getValue())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onStartUseItem(StartUseItemEvent startUseItemEvent) {
        if (mc.hitResult != null) {
            if (mc.hitResult instanceof BlockHitResult blockHitResult) {
                BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());
                if (this.cancelGlowstoneMisplace.getValue()
                        && mc.player.getMainHandItem().getItem() == Items.GLOWSTONE
                        && blockState.getBlock() != Blocks.RESPAWN_ANCHOR) {
                    startUseItemEvent.setCancelled(true);
                }
            }
        }
    }

    private static final String MODULE_ID = "4fb069d8fe9eeaf7630fd6e8a2a2d8c0";
    private static final String CANCEL_TEAM_ATTACKS_SETTING_ID =
            "26e7a2b59e64cc1c10b547519bfdb11ce26c8afdd918e63cf87c78f70a1c33e4";
    private static final String CANCEL_BOT_ATTACKS_SETTING_ID =
            "7beccb851989dabfb3f985a81f73131375f5d12ac6196a8f66b4a16733722abd";
    private static final String CANCEL_OBI_PUNCH_SETTING_ID = "3fd8b933e30bc0d8761fe2412b2835d3";
    private static final String CANCEL_GLOWSTONE_MISPLACE_SETTING_ID =
            "e93910146cc6fa8911ae85e925750f58829c8c76108f1659d5b498be1cac24cc";
    private static final String CANCEL_ATTACK_ON_MISS_SETTING_ID =
            "54ae759ab58e530d9ec9248aaad696271106800ff16f286015b2d73eedcc065d";
    private static final String ALLOW_BLOCK_HIT_SETTING_ID = "935e792508f706a3ba71b5b356277417";
    private static final String ALLOW_AIR_HIT_SETTING_ID = "a0e29b17420feeaa4f87df7367f50a90";
    private static final String WEAPON_ONLY_SETTING_ID = "a200b0503e600f347f20828ac59f6bbe";
    private static final String ATTACKS_TO_CANCEL_GROUP_ID =
            "fd0379a84f7fcbb2f4c63234e3c0c62fb63afc919cb8275e763fe423dc8eed67";
    private static final String ATTACK_ON_MISS_SETTINGS_GROUP_ID =
            "ee8fae45fcf17a09c6429979649ceba88fc5f62f9a006967c29eace9fdbca7ad";
    private static final String MISSES_TO_CANCEL_GROUP_ID = "1552f88b870eccfdda80322da510e169";
}
