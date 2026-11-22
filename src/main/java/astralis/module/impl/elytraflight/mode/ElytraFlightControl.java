package astralis.module.impl.elytraflight.mode;

import astralis.event.EventTarget;
import astralis.event.impl.EntityMoveEvent;
import astralis.module.impl.ElytraFlight;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.MoveUtil;

import net.minecraft.world.phys.Vec3;

public class ElytraFlightControl extends Mode<ElytraFlight> {
    private final NumberSetting horizontalSpeed;
    private final NumberSetting upSpeed;
    private final NumberSetting downSpeed;

    public ElytraFlightControl(ElytraFlight elytraFlight) {

        super("Control", elytraFlight);

        this.horizontalSpeed =
                new NumberSetting("2fdfac580962e6e58a5e1de48cabfbc0", 2.0F, 0.1F, 5.0F, 0.1F);
        this.upSpeed =
                new NumberSetting("6b8fa4c7f0c045e08260c37709e1d35a", 1.0F, 0.1F, 5.0F, 0.1F);
        this.downSpeed =
                new NumberSetting("ba66e9a43a21fe068e404e96efbd79e0", 1.0F, 0.1F, 5.0F, 0.1F);
        this.addSettings(this.horizontalSpeed, this.upSpeed, this.downSpeed);
    }

    @EventTarget
    public void onEntityMove(EntityMoveEvent event) {
        if (mc.player.isFallFlying() && event.getEntity() == mc.player) {
            if (MoveUtil.isMoving()) {
                MoveUtil.setSpeed(this.horizontalSpeed.getValue().floatValue());
                event.setMovement(mc.player.getDeltaMovement());
            } else {
                event.setMovement(Vec3.ZERO);
            }

            double verticalSpeed = 0.0;
            if (mc.options.keyJump.isDown()) {
                verticalSpeed = this.upSpeed.getValue().floatValue();
            } else if (mc.options.keyShift.isDown()) {
                verticalSpeed = -this.downSpeed.getValue();
            }

            Vec3 vector3 = event.getMovement();
            event.setMovement(new Vec3(vector3.x, verticalSpeed, vector3.z));
        }
    }
}
