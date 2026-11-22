package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.InventoryUtil;
import astralis.util.MoveUtil;

import net.minecraft.world.phys.AABB;

import pub.astralis.client.module.core.Category;

public class BridgeAssist extends Module {
    private static final double COLLISION_EPSILON = 1.0E-7;
    private final NumberSetting predictionTicks;
    private final BooleanSetting sneakOnJump;
    private final NumberSetting unsneakDelay;
    private final BooleanSetting blocks;
    private final BooleanSetting backwards;
    private final BooleanSetting lookingDown;
    private long lastSneakTime;
    private boolean sneaking;

    public BridgeAssist() {

        super(MODULE_ID, "", "", Category.Misc);

        this.predictionTicks =
                new NumberSetting(PREDICTION_TICKS_SETTING_ID, 1.0F, 1.0F, 5.0F, 1.0F);

        this.sneakOnJump = new BooleanSetting(SNEAK_ON_JUMP_SETTING_ID, false);

        this.unsneakDelay = new NumberSetting(UNSNEAK_DELAY_SETTING_ID, 50.0F, 0.0F, 500.0F, 10.0F);

        this.blocks = new BooleanSetting(BLOCKS_SETTING_ID, true);

        this.backwards = new BooleanSetting(BACKWARDS_SETTING_ID, true);

        this.lookingDown = new BooleanSetting(LOOKING_DOWN_SETTING_ID, true);
        this.lastSneakTime = 0L;
        this.sneaking = false;
        SettingGroup settingGroup =
                new SettingGroup(BRIDGING_GROUP_ID)
                        .addValues(this.predictionTicks, this.sneakOnJump, this.unsneakDelay);
        SettingGroup settingGroup2 =
                new SettingGroup(CHECKS_GROUP_ID)
                        .addValues(this.blocks, this.backwards, this.lookingDown);
        this.addSettings(settingGroup, settingGroup2);
    }

    private boolean isAirBelow(double offsetX, double offsetZ, float checkDepth) {
        AABB boundingBox = mc.player.getBoundingBox();
        AABB boundingBox2 =
                new AABB(
                        boundingBox.minX + 1.0E-7 + offsetX,
                        boundingBox.minY - checkDepth - 1.0E-7,
                        boundingBox.minZ + 1.0E-7 + offsetZ,
                        boundingBox.maxX - 1.0E-7 + offsetX,
                        boundingBox.minY,
                        boundingBox.maxZ - 1.0E-7 + offsetZ);
        return mc.level.noCollision(mc.player, boundingBox2);
    }

    @Override
    public void onDisable() {
        this.lastSneakTime = 0L;
        this.sneaking = false;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (!mc.player.isSpectator() && !mc.player.getAbilities().flying) {
            long currentTime = System.currentTimeMillis();
            if (this.sneaking
                    && this.unsneakDelay.getValue() > 0.0F
                    && (float) (currentTime - this.lastSneakTime) < this.unsneakDelay.getValue()) {
                moveInputEvent.setSneak(true);
            } else if (mc.player.getXRot() < 65.0F && !this.lookingDown.getValue()) {
                this.sneaking = false;
            } else if (this.blocks.getValue() && !InventoryUtil.isHoldingPlaceableBlock()) {
                this.sneaking = false;
            } else if (this.backwards.getValue() && !mc.options.keyDown.isDown()) {
                this.sneaking = false;
            } else {
                if (mc.player.onGround()
                        || this.sneakOnJump.getValue() && moveInputEvent.isJump()) {
                    if (this.shouldSneak()) {
                        moveInputEvent.setSneak(true);
                        this.lastSneakTime = currentTime;
                        this.sneaking = true;
                    } else {
                        this.sneaking = false;
                    }
                }
            }
        }
    }

    private boolean shouldSneak() {
        float stepHeight = mc.player.maxUpStep();
        if (this.isAirBelow(0.0, 0.0, stepHeight)) {
            return true;
        }

        if (this.sneaking && !MoveUtil.isMoving()) {
            double edgeOffset = 0.3;
            if (this.isAirBelow(edgeOffset, 0.0, stepHeight)
                    || this.isAirBelow(-0.3, 0.0, stepHeight)
                    || this.isAirBelow(0.0, edgeOffset, stepHeight)
                    || this.isAirBelow(0.0, -0.3, stepHeight)) {
                return true;
            }
        }

        if (!MoveUtil.isMoving()) {
            return false;
        }

        float directionRad = MoveUtil.getDirectionRad();
        double dirX = -Math.sin(directionRad);
        double dirZ = Math.cos(directionRad);
        double maxDistance = 0.3 * this.predictionTicks.getValue().floatValue();
        double stepSize = 0.05;

        for (double distance = stepSize; distance <= maxDistance; distance += stepSize) {
            if (this.isAirBelow(dirX * distance, dirZ * distance, stepHeight)) {
                return true;
            }
        }

        return false;
    }

    private static final String MODULE_ID = "51092b25234836cfe49446f58396a1f8";
    private static final String PREDICTION_TICKS_SETTING_ID = "44d7456bb79ab56f0d18611a1c6e7cf1";
    private static final String SNEAK_ON_JUMP_SETTING_ID = "3ba6f458fc264f5e7b043bacbe968e2b";
    private static final String UNSNEAK_DELAY_SETTING_ID = "47cc62028c97020641abef0a4c4b530b";
    private static final String BLOCKS_SETTING_ID = "663a6aac94ac9279dca177ea31468466";
    private static final String BACKWARDS_SETTING_ID = "2d3ac654e90e425d6c28d1982fc49854";
    private static final String LOOKING_DOWN_SETTING_ID = "ae42569d6e5b2c4876fbf581c29ef41e";
    private static final String BRIDGING_GROUP_ID = "2e05353e2cc71e616b103b16738544e4";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
