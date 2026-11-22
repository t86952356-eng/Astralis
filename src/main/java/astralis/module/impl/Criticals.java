package astralis.module.impl;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.criticals.mode.CriticalsJump;
import astralis.module.impl.criticals.mode.CriticalsLag;
import astralis.module.impl.criticals.mode.CriticalsOffGround;
import astralis.module.impl.criticals.mode.CriticalsSentinel;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.SettingGroup;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;
import astralis.util.MoveUtil;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.WebBlock;

import pub.astralis.client.module.core.Category;

public class Criticals extends Module {
    private final ModeSetting mode;
    private final BooleanSetting requireKaTarget;
    private final BooleanSetting requireWeapon;
    private final BooleanSetting liquid;

    public Criticals() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new CriticalsSentinel(this))
                        .addValues(new CriticalsOffGround(this))
                        .addValues(new CriticalsLag(this))
                        .addValues(new CriticalsJump(this));
        this.mode = modeSetting.setDefault("Jump");

        this.requireKaTarget = new BooleanSetting(REQUIRE_KA_TARGET_SETTING_ID, true);

        this.requireWeapon = new BooleanSetting(REQUIRE_WEAPON_SETTING_ID, true);

        this.liquid = new BooleanSetting(LIQUID_SETTING_ID, true);
        SettingGroup settingGroup =
                new SettingGroup(CHECKS_GROUP_ID)
                        .addValues(this.requireKaTarget, this.requireWeapon, this.liquid);
        this.addSettings(this.mode, settingGroup);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public boolean isRequireKaTarget() {
        return this.requireKaTarget.getValue();
    }

    public boolean isJumpAlignedWithCooldown() {

        if (!this.canCrit(true)) {
            return false;
        }

        float remainingCooldown = BreakUtil.getRemainingCooldownTicks();
        double jumpVelocity = mc.player.getDeltaMovement().y + 0.42F;

        int ticksUntilLanding = MoveUtil.ticksUntilGroundCollision(jumpVelocity, 15);
        if (ticksUntilLanding < 0) {

            ticksUntilLanding = 10;
        }

        return ticksUntilLanding + 5.25F >= remainingCooldown && 6.25F < remainingCooldown;
    }

    public boolean canCrit(boolean allowOnGround) {
        return !mc.player.isInLiquid()
                && !mc.player.isPassenger()
                && !(mc.player.getInBlockState().getBlock() instanceof WebBlock)
                && !mc.player.hasEffect(MobEffects.LEVITATION)
                && !mc.player.hasEffect(MobEffects.BLINDNESS)
                && !mc.player.hasEffect(MobEffects.SLOW_FALLING)
                && !mc.player.onClimbable()
                && !mc.player.isNoGravity()
                && !mc.player.isHandsBusy()
                && !mc.player.getAbilities().flying
                && (!mc.player.onGround() || allowOnGround);
    }

    public boolean isCritReady(boolean allowOnGround, boolean allowSprint) {
        return !allowSprint && mc.player.isSprinting()
                ? false
                : this.canCrit(allowOnGround) && mc.player.getAttackStrengthScale(0.5F) > 0.9F;
    }

    public boolean shouldSkipCrit() {
        if (this.requireKaTarget.getValue()) {
            KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
            if (!killAura.isEnabled() || killAura.target == null) {
                return true;
            }
        }

        return this.requireWeapon.getValue() && !InventoryUtil.isHoldingWeapon()
                ? true
                : this.liquid.getValue() && mc.player.isInLiquid();
    }

    public boolean shouldWaitForCrit(LivingEntity livingEntity) {
        if (livingEntity == null || livingEntity.isDeadOrDying() || mc.player.isFallFlying()) {
            return false;
        } else if (this.canCrit(false) && !(mc.player.getDeltaMovement().y < -0.08)) {
            float cooldownTicks =
                    Math.max(
                            (float) (mc.player.getDeltaMovement().y / 0.08),
                            BreakUtil.getRemainingCooldownTicks());
            return BreakUtil.getCooldownDamageScale(cooldownTicks) >= 0.375F
                    ? false
                    : MoveUtil.ticksUntilGroundCollision(
                                    mc.player.getDeltaMovement().y, (int) (cooldownTicks * 1.3F))
                            < 0;
        } else {
            return false;
        }
    }

    public boolean isFallingCritReady(boolean allowSprint) {
        return this.isCritReady(false, allowSprint) && mc.player.fallDistance > 0.0;
    }

    private static final String MODULE_ID = "6daf5743b3d2bbda5575b39e21ab6b9e";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String REQUIRE_KA_TARGET_SETTING_ID =
            "53a6f4313355b0dc18122657b6c1979c11c112b280ff987bd5e1a261d0ca8ddc";
    private static final String REQUIRE_WEAPON_SETTING_ID = "1399ddaa937dd384383b72ec9216b783";
    private static final String LIQUID_SETTING_ID = "9c6d2395bd6cd0575bfdff00d7ddee31";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
