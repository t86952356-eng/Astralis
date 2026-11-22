package astralis.module.impl.combat.velocity;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.combat.Velocity;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Arms a jump reset from upward knockback and consumes it through movement input. */
public class VelocityLegit extends Mode<Velocity> {
    private final NumberSetting jumpResetChance;
    private final NumberSetting hurtTimeThreshold;
    private final BooleanSetting smartFov;
    private final BooleanSetting rotateToVelocityOrigin;
    private boolean jumpResetPending;

    public VelocityLegit(Velocity parent) {

        super("Legit", parent);

        this.jumpResetChance =
                new NumberSetting(JUMP_RESET_CHANCE_SETTING_ID, 100.0F, 1.0F, 100.0F, 1.0F);

        this.hurtTimeThreshold =
                new NumberSetting(HURT_TIME_THRESHOLD_SETTING_ID, 10.0F, 1.0F, 10.0F, 1.0F);

        this.smartFov = new BooleanSetting(SMART_FOV_SETTING_ID, true);

        this.rotateToVelocityOrigin =
                new BooleanSetting(ROTATE_TO_VELOCITY_ORIGIN_SETTING_ID, false);
        this.jumpResetPending = false;
        this.jumpResetChance.hideIf(
                () -> {
                    ModeSetting parentMode = this.getParent().getMode();

                    return !parentMode.is("Legit");
                });
        this.hurtTimeThreshold.hideIf(
                () -> {
                    ModeSetting parentMode = this.getParent().getMode();

                    return !parentMode.is("Legit");
                });
        this.addSettings(
                this.jumpResetChance,
                this.hurtTimeThreshold,
                this.rotateToVelocityOrigin,
                this.smartFov);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        // Keep requesting the jump until the airborne damage window has passed.
        if (this.jumpResetPending
                && mc.player.hurtTime >= this.hurtTimeThreshold.getValue().intValue()
                && !event.isJump()) {
            event.setJump(true);
        }

        if (!mc.player.onGround()
                && this.jumpResetPending
                && mc.player.hurtTime < this.hurtTimeThreshold.getValue().intValue() - 1) {
            this.jumpResetPending = false;
        }
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (event.getY() > 0.0 && this.getParent().shouldApply()) {
            if (this.jumpResetChance.getValue() != 100.0F
                    && Math.random() >= this.jumpResetChance.getValue() / 100.0F) {
                return;
            }

            // Reject knockback whose origin is outside the forward-facing half of the view.
            if (this.smartFov.getValue()) {
                Vec3 horizontalKnockback = new Vec3(event.getX(), 0.0, event.getZ());
                if (horizontalKnockback.lengthSqr() > 0.0) {
                    float knockbackOriginYaw =
                            (float)
                                    (Math.toDegrees(Math.atan2(-event.getZ(), -event.getX()))
                                            - 90.0);
                    float yawDifference =
                            Math.abs(Mth.wrapDegrees(knockbackOriginYaw - mc.player.getYRot()));
                    if (yawDifference > 90.0F) {
                        return;
                    }
                }
            }

            if (this.rotateToVelocityOrigin.getValue()) {
                double knockbackX = event.getX();
                double knockbackZ = event.getZ();
                if (knockbackX != 0.0 || knockbackZ != 0.0) {
                    float knockbackOriginYaw =
                            (float) (Math.toDegrees(Math.atan2(-knockbackZ, -knockbackX)) - 90.0);
                    float knockbackOriginPitch =
                            (float)
                                    (-Math.toDegrees(
                                            Math.atan2(
                                                    -event.getY(),
                                                    Math.sqrt(
                                                            knockbackX * knockbackX
                                                                    + knockbackZ * knockbackZ))));
                    RotationManager.getInstance()
                            .submit(knockbackOriginYaw, knockbackOriginPitch, 360.0F, 360.0F);
                }
            }

            if (mc.player.onGround() || mc.player.fallDistance < 0.1F) {
                this.jumpResetPending = true;
            }
        }
    }

    private static final String JUMP_RESET_CHANCE_SETTING_ID = "972fc2a5a12fd5593e3a52b7a6c6ab8c";
    private static final String HURT_TIME_THRESHOLD_SETTING_ID =
            "c0308de6d5511bc615907b45104b38aa963cac39af0ddabc3b99013a95c8441c";
    private static final String SMART_FOV_SETTING_ID = "feceacc294e2496ea8827bf475d6e123";
    private static final String ROTATE_TO_VELOCITY_ORIGIN_SETTING_ID =
            "08c1eef7c9f2a0daeed0fbdd9df527b80655f63babef2c88ed26bc6d3c6d986f";
}
