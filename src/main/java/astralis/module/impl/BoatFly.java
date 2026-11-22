package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.EntityMoveEvent;
import astralis.module.Module;
import astralis.setting.NumberSetting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

public class BoatFly extends Module {
    private final NumberSetting horizontalSpeed;
    private final NumberSetting upSpeed;
    private final NumberSetting gravity;

    public BoatFly() {

        super(MODULE_ID, "", "", Category.Misc);

        this.horizontalSpeed =
                new NumberSetting(HORIZONTAL_SPEED_SETTING_ID, 2.0F, 0.1F, 10.0F, 0.1F);

        this.upSpeed = new NumberSetting(UP_SPEED_SETTING_ID, 1.0F, 0.1F, 10.0F, 0.1F);

        this.gravity = new NumberSetting(GRAVITY_SETTING_ID, 0.0F, -5.0F, 5.0F, 0.1F);
        this.addSettings(this.horizontalSpeed, this.upSpeed, this.gravity);
    }

    @EventTarget
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity2 = mc.player.getVehicle();
        if (entity2 instanceof Boat boat2 && event.getEntity() == boat2) {
            double motionX = event.getMovement().x * this.horizontalSpeed.getValue().floatValue();
            double motionZ = event.getMovement().z * this.horizontalSpeed.getValue().floatValue();
            double motionY = this.gravity.getValue().floatValue();
            if (mc.options.keyJump.isDown()) {
                motionY = this.upSpeed.getValue().floatValue();
            }

            event.setMovement(new Vec3(motionX, motionY, motionZ));
        }
    }

    private static final String MODULE_ID = "8676f53d0af143c63be74e743ac8f29d";
    private static final String HORIZONTAL_SPEED_SETTING_ID = "2fdfac580962e6e58a5e1de48cabfbc0";
    private static final String UP_SPEED_SETTING_ID = "6b8fa4c7f0c045e08260c37709e1d35a";
    private static final String GRAVITY_SETTING_ID = "36ac7882996b766497e805d27d7db577";
}
