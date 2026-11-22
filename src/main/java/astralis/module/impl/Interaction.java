package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.mixin.MinecraftAccessor;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;
import astralis.util.InventoryUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.util.List;

public class Interaction extends Module {
    private final BooleanSetting fastPlace;
    private final NumberSetting delay;
    private final BooleanSetting onlyOnBlocks;
    private final BooleanSetting throughWallInteract;
    private final NumberSetting reach;
    private final SettingGroup fastPlaceSettings;
    private final SettingGroup throughWallInteractSettings;
    private final List<BlockPos> rayPositions;

    public Interaction() {

        super(MODULE_ID, "", "", Category.Misc);

        this.fastPlace = new BooleanSetting(FAST_PLACE_SETTING_ID, false);

        this.delay = new NumberSetting(DELAY_SETTING_ID, 1.0F, 0.0F, 4.0F, 1.0F);

        this.onlyOnBlocks = new BooleanSetting(ONLY_ON_BLOCKS_SETTING_ID, true);

        this.throughWallInteract = new BooleanSetting(THROUGH_WALL_INTERACT_SETTING_ID, false);

        this.reach = new NumberSetting(REACH_SETTING_ID, 4.5F, 1.0F, 6.0F, 0.1F);

        this.fastPlaceSettings =
                new SettingGroup(FAST_PLACE_SETTINGS_GROUP_ID)
                        .addValues(this.delay, this.onlyOnBlocks);

        this.throughWallInteractSettings =
                new SettingGroup(THROUGH_WALL_INTERACT_SETTINGS_GROUP_ID).addValues(this.reach);
        this.rayPositions = new java.util.ArrayList<>();
        this.fastPlaceSettings.hideIf(() -> !this.fastPlace.getValue());
        this.throughWallInteractSettings.hideIf(() -> !this.throughWallInteract.getValue());
        this.addSettings(
                this.fastPlace,
                this.fastPlaceSettings,
                this.throughWallInteract,
                this.throughWallInteractSettings);
    }

    private void handleWallInteract() {
        if (!mc.player.isShiftKeyDown() && mc.options.keyUse.isDown()) {
            float range = this.reach.getValue();
            Vec3 vector3 =
                    mc.player.getEyePosition(
                            mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            java.util.ArrayList<BlockEntity> blockEntities = new java.util.ArrayList<>();
            int scanRadius = Mth.ceil(range);
            BlockPos blockPos = mc.player.blockPosition();
            BlockPos.betweenClosed(
                            blockPos.offset(-scanRadius, -scanRadius, -scanRadius),
                            blockPos.offset(scanRadius, scanRadius, scanRadius))
                    .forEach(
                            blockPos2 -> {
                                BlockEntity blockEntity = mc.level.getBlockEntity(blockPos2);
                                if (blockEntity != null) {
                                    blockEntities.add(blockEntity);
                                }
                            });
            HitResult hitResult =
                    mc.player.pick(
                            range, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
            if (hitResult.getType() != Type.BLOCK
                    || !blockEntities.contains(
                            mc.level.getBlockEntity(((BlockHitResult) hitResult).getBlockPos()))) {
                this.rayPositions.clear();
                float pitchRad = (float) Math.toRadians(-mc.player.getXRot());
                float yawRad = (float) Math.toRadians(-mc.player.getYRot());
                Vec3 vector32 = new Vec3(0.0, 0.0, 0.1).xRot(pitchRad).yRot(yawRad);

                for (int step = 1; step < range * 10.0F; step++) {
                    this.rayPositions.add(
                            BlockPos.containing(vector3.add(vector32.multiply(step, step, step))));
                }

                BlockEntity blockEntity2 = null;
                Vec3 vector33 = null;
                double closestDistance = Double.MAX_VALUE;

                for (BlockEntity blockEntity3 : blockEntities) {
                    if (this.rayPositions.contains(blockEntity3.getBlockPos())) {
                        Vec3 vector34 = Vec3.atCenterOf(blockEntity3.getBlockPos());
                        if (vector34.distanceTo(vector3) < closestDistance) {
                            blockEntity2 = blockEntity3;
                            vector33 = vector34;
                            closestDistance = vector34.distanceTo(vector3);
                        }
                    }
                }

                if (blockEntity2 != null) {
                    mc.gameMode.useItemOn(
                            mc.player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    vector33, Direction.UP, blockEntity2.getBlockPos(), true));
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (this.fastPlace.getValue()
                    && (!this.onlyOnBlocks.getValue() || InventoryUtil.isHoldingPlaceableBlock())) {
                MinecraftAccessor accessor = (MinecraftAccessor) mc;
                int delayTicks = this.delay.getValue().intValue();
                if (accessor.getItemUseCooldown() > delayTicks) {
                    accessor.setItemUseCooldown(delayTicks);
                }
            }

            if (this.throughWallInteract.getValue()) {
                this.handleWallInteract();
            }
        }
    }

    private static final String MODULE_ID = "6ae3d57aa8f83f1a396cdc84d79a9deb";
    private static final String FAST_PLACE_SETTING_ID = "e17de88f73ae29c07f48a4bcae8bcbd5";
    private static final String DELAY_SETTING_ID = "960f91ff141f21b3427418599a8603ec";
    private static final String ONLY_ON_BLOCKS_SETTING_ID = "485a24ea6d0579900bffa97a72474b19";
    private static final String THROUGH_WALL_INTERACT_SETTING_ID =
            "f3ff57fe0f94dfe3349c7687875c0a801321a76b1e97038db427a67993b69627";
    private static final String REACH_SETTING_ID = "6ca426030e3215abae95fef1bc326538";
    private static final String FAST_PLACE_SETTINGS_GROUP_ID =
            "3405dc696a80f0e729fa7b02fe41fe9f08314606adcffd3c08cd228bcdf298d0";
    private static final String THROUGH_WALL_INTERACT_SETTINGS_GROUP_ID =
            "f3ff57fe0f94dfe3349c7687875c0a8072c1e8123bf3021d918692788ebcca32";
}
