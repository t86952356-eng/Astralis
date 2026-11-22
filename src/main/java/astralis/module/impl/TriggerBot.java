package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WebSlowdownEvent;
import astralis.manager.AttackManager;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;
import astralis.util.SwingType;
import astralis.util.target.TargetSortOrder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

import pub.astralis.client.module.core.Category;

import java.util.List;

public class TriggerBot extends Module {
    private final NumberSetting percentage;
    private final BooleanSetting waitForCrits;
    private final BooleanSetting weaponsOnly;
    private final BooleanSetting hitWhileUsing;
    private final BooleanSetting ignoreIfTargetBlocking;
    private final BooleanSetting monsters;
    private final BooleanSetting friendly;
    private final BooleanSetting invisible;
    private final BooleanSetting teams;
    private final SettingGroup targets;
    private boolean pauseMovement;
    boolean inWeb;

    public TriggerBot() {

        super(MODULE_ID, "", "", Category.Misc);

        this.percentage = new NumberSetting(PERCENTAGE_SETTING_ID, 1.0F, 0.1F, 1.0F, 0.05F);

        this.waitForCrits = new BooleanSetting(WAIT_FOR_CRITS_SETTING_ID, true);

        this.weaponsOnly = new BooleanSetting(WEAPONS_ONLY_SETTING_ID, true);

        this.hitWhileUsing = new BooleanSetting(HIT_WHILE_USING_SETTING_ID, false);

        this.ignoreIfTargetBlocking =
                new BooleanSetting(IGNORE_IF_TARGET_BLOCKING_SETTING_ID, false);

        this.monsters = new BooleanSetting(MONSTERS_SETTING_ID, false);

        this.friendly = new BooleanSetting(FRIENDLY_SETTING_ID, false);

        this.invisible = new BooleanSetting(INVISIBLE_SETTING_ID, false);

        this.teams = new BooleanSetting(TEAMS_SETTING_ID, false);

        this.targets =
                new SettingGroup(TARGETS_GROUP_ID)
                        .addValues(this.monsters, this.friendly, this.invisible, this.teams);

        this.addSettings(
                this.percentage,
                this.waitForCrits,
                this.weaponsOnly,
                this.hitWhileUsing,
                this.ignoreIfTargetBlocking,
                this.targets);
    }

    @EventTarget
    public void onWebSlowdown(WebSlowdownEvent webSlowdownEvent) {
        this.inWeb = true;
    }

    @Override
    public String getSuffix() {
        return this.percentage.getValue().toString();
    }

    @EventTarget(4)
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.pauseMovement) {
            moveInputEvent.setForward(false);
            moveInputEvent.setSprint(false);
            this.pauseMovement = false;
        }
    }

    private boolean canAttack(LivingEntity livingEntity) {
        if (livingEntity == null || livingEntity.isDeadOrDying()) {
            return false;
        }

        if (mc.gui.screen() != null) {
            return false;
        }

        if (this.ignoreIfTargetBlocking.getValue() && livingEntity.isBlocking()) {
            return false;
        }

        if (!this.hitWhileUsing.getValue() && mc.player.isUsingItem()) {
            return false;
        }

        if (this.weaponsOnly.getValue() && !InventoryUtil.isHoldingWeapon()) {
            return false;
        }

        if (mc.player.getAttackStrengthScale(0.5F) < this.percentage.getValue()) {
            return false;
        }

        if (!this.passesCritCheck(livingEntity)) {
            return false;
        }

        List validTargets =
                BreakUtil.getTargets(
                        this.friendly.getValue(),
                        this.monsters.getValue(),
                        this.teams.getValue(),
                        this.invisible.getValue(),
                        false,
                        6.0F,
                        TargetSortOrder.HEALTH,
                        true);
        return validTargets.contains(livingEntity);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            this.inWeb = false;
            if (mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
                EntityHitResult entityHitResult = (EntityHitResult) mc.hitResult;
                if (entityHitResult.getEntity() instanceof LivingEntity livingEntity
                        && this.canAttack(livingEntity)) {
                    AttackManager.attackIfCooldownReady(
                            SwingType.CLICK,
                            livingEntity,
                            this.percentage.getValue().doubleValue());
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.pauseMovement = false;
    }

    private boolean passesCritCheck(LivingEntity livingEntity) {
        if (!this.waitForCrits.getValue()) {
            return true;
        }

        Criticals criticals = Astralis.getInstance().getModuleManager().getModule(Criticals.class);
        if (!mc.player.onGround() && criticals.canCrit(false) && !this.inWeb) {
            boolean shouldWait = criticals.shouldWaitForCrit(livingEntity);
            if (!shouldWait && !criticals.isFallingCritReady(true)) {
                this.pauseMovement = false;
                return true;
            } else {
                this.pauseMovement = true;
                return !shouldWait && criticals.isFallingCritReady(false);
            }
        } else {
            this.pauseMovement = false;
            return true;
        }
    }

    private static final String MODULE_ID = "b2e68757083f533912e956b49c66bbfb";
    private static final String PERCENTAGE_SETTING_ID = "d244231a8d0002c5180823fe2ca00f4d";
    private static final String WAIT_FOR_CRITS_SETTING_ID = "3c48e2cbba15f969d71983922fc17021";
    private static final String WEAPONS_ONLY_SETTING_ID = "4eb1bdc15306b6e3ba66bf2f0f432243";
    private static final String HIT_WHILE_USING_SETTING_ID = "42ce467a01f034028490f34f9f1fff8a";
    private static final String IGNORE_IF_TARGET_BLOCKING_SETTING_ID =
            "10de3ad40a46852256bb4837b0a913c9df59d1d98c66914982d429137279f857";
    private static final String MONSTERS_SETTING_ID = "b2e39a7090da277f100b28beac526e55";
    private static final String FRIENDLY_SETTING_ID = "3591a85a0cad9ff80c1df14006606ca8";
    private static final String INVISIBLE_SETTING_ID = "a63ea6c796b6ef2ae55074f6ee3726db";
    private static final String TEAMS_SETTING_ID = "46334632fc765d791744b4e27384ec4d";
    private static final String TARGETS_GROUP_ID = "6ef0e612f8bb85c79eef6a9949fb6be4";
}
