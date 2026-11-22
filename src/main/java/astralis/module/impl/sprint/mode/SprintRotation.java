package astralis.module.impl.sprint.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.TickEvent;
import astralis.module.impl.Sprint;
import astralis.rotation.RotationManager;
import astralis.setting.Mode;
import astralis.util.RotationUtil;

import net.minecraft.world.phys.Vec3;

public class SprintRotation extends Mode<Sprint> {

    public SprintRotation(Sprint sprint) {
        super("Rotation", sprint);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if ((!mc.options.keyUp.isDown() || mc.options.keyDown.isDown())
                    && !mc.options.keyShift.isDown()) {
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

                if (vector3.lengthSqr() >= 1.0E-4) {
                    float[] rotations = RotationUtil.getRotationsFromDirection(vector3);
                    RotationManager.getInstance()
                            .submit(rotations[0], mc.player.getXRot(), 360.0F, 360.0F);
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        moveInputEvent.setSprint(true);
    }
}
