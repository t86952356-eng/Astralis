package astralis.module.impl.elytraflight.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.ElytraFlight;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.util.RotationUtil;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

public class ElytraFlightGrim extends Mode<ElytraFlight> {
    private final BooleanSetting useRockets;
    private double rocketDuration;
    private long lastRocketTime;
    private boolean rotationFlipped;

    public ElytraFlightGrim(ElytraFlight elytraFlight) {

        super("Grim", elytraFlight);

        this.useRockets = new BooleanSetting("fd80424304c5bad7a02793e120798149", true);
        this.rocketDuration = 1.0;
        this.lastRocketTime = 0L;
        this.rotationFlipped = false;
        this.addSettings(this.useRockets);
    }

    private InteractionHand findRocketHand() {
        if (mc.player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            return InteractionHand.MAIN_HAND;
        } else {
            return mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET)
                    ? InteractionHand.OFF_HAND
                    : null;
        }
    }

    private void useRocket(InteractionHand interactionHand) {
        Fireworks fireworks =
                mc.player.getItemInHand(interactionHand).get(DataComponents.FIREWORKS);
        int flightDuration = fireworks != null ? fireworks.flightDuration() : 1;
        this.rocketDuration = flightDuration * 0.5 + 0.5;
        mc.gameMode.useItem(mc.player, interactionHand);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (mc.player.isFallFlying() && tickEvent.getStage() == EventStage.PRE) {
            if (this.useRockets.getValue()
                    && System.currentTimeMillis() - this.lastRocketTime
                            >= this.rocketDuration * 1000.0) {
                InteractionHand interactionHand = this.findRocketHand();
                if (interactionHand != null) {
                    this.useRocket(interactionHand);
                    this.lastRocketTime = System.currentTimeMillis();
                }
            }

            Vec3 vector3 = Vec3.ZERO;
            float yaw = mc.player.getYRot();
            if (mc.options.keyUp.isDown()) {
                vector3 = vector3.add(RotationUtil.directionFromRotation(0.0F, yaw));
            }

            if (mc.options.keyDown.isDown()) {
                vector3 = vector3.add(RotationUtil.directionFromRotation(0.0F, yaw + 180.0F));
            }

            if (mc.options.keyLeft.isDown()) {
                vector3 = vector3.add(RotationUtil.directionFromRotation(0.0F, yaw - 90.0F));
            }

            if (mc.options.keyRight.isDown()) {
                vector3 = vector3.add(RotationUtil.directionFromRotation(0.0F, yaw + 90.0F));
            }

            if (mc.options.keyJump.isDown()) {
                vector3 = vector3.add(new Vec3(0.0, 1.0, 0.0));
            }

            if (mc.options.keyShift.isDown()) {
                vector3 = vector3.add(new Vec3(0.0, -1.0, 0.0));
            }

            if (vector3.lengthSqr() < 1.0E-4) {
                if (this.findRocketHand() != null) {
                    if (this.rotationFlipped) {
                        RotationManager.getInstance().submit(0.0F, 0.0F, 360.0F, 360.0F);
                    } else {
                        RotationManager.getInstance().submit(180.0F, 0.0F, 360.0F, 360.0F);
                    }

                    this.rotationFlipped = !this.rotationFlipped;
                }
            } else {
                float[] rotations = RotationUtil.getRotationsFromDirection(vector3);
                RotationManager.getInstance().submit(rotations[0], rotations[1], 360.0F, 360.0F);
            }
        }
    }
}
