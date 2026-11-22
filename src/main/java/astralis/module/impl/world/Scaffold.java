package astralis.module.impl.world;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ContinueAttackEvent;
import astralis.event.impl.MouseScrollEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.RenderHeldItemEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.module.impl.BlockPlaces;
import astralis.module.impl.world.scaffold.BlockCounterRenderer;
import astralis.module.impl.world.scaffold.DirectionSwitchMap;
import astralis.rotation.Rotation;
import astralis.rotation.RotationManager;
import astralis.rotation.RotationPriority;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.Blink;
import astralis.util.BlockData;
import astralis.util.InventoryUtil;
import astralis.util.LocalProxy;
import astralis.util.MathUtil;
import astralis.util.MoveUtil;
import astralis.util.PacketUtil;
import astralis.util.PlaceRotation;
import astralis.util.RotationUtil;
import astralis.util.Timer;
import astralis.util.inventory.DelayedSlotRestorer;
import astralis.util.render.RenderUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;

public class Scaffold extends Module {
    private final BooleanSetting keepY;
    private final ModeSetting swingMode;
    private final BooleanSetting tower;
    private final BooleanSetting jump;
    private final BooleanSetting noDiagonalJump;
    private final NumberSetting jumpFrequency;
    private final BooleanSetting sneak;
    private final NumberSetting sneakXBlocks;
    private final NumberSetting sneakTicks;
    private final BooleanSetting expand;
    private final NumberSetting expandDistance;
    private final BooleanSetting onlyOnRightClick;
    private final SettingGroup movementSettings;
    private final ModeSetting rotationType;
    private final BooleanSetting extraBlock;
    private final BooleanSetting forwardOnGround;
    private final BooleanSetting snapRotations;
    private final ModeSetting rayCastMode;
    private final BooleanSetting watchdogNoise;
    private final BooleanSetting slow;
    private final NumberSetting slowDelay;
    private final BooleanSetting organic;
    private final BooleanSetting rotateOnFrame;
    private final RangeSetting yawSpeed;
    private final RangeSetting pitchSpeed;
    private final RangeSetting watchdogSpeed;
    private final SettingGroup rotationSettings;
    private final BooleanSetting hideJump;
    private final BooleanSetting blockESP;
    private final ColorSetting blockHighlightColor;
    private final ModeSetting blockCounter;
    private final ColorSetting counterColors;
    private final ModeSetting silentRotations;
    private final SettingGroup visualSettings;
    private BlockData placementTarget;
    private float targetYaw;
    private float targetPitch;
    private float snapYaw;
    private float snapPitch;
    private float placeSideSign;
    private boolean snapPathActive;
    private int snapPlaceCountdown;
    private boolean snapInitialized;
    private PlaceRotation placeRotation;
    private double heightAnchor;
    private double hideJumpEyeY;
    private int originalHotbarSlot;
    private int jumpPlacementCount;
    private int sneakPlacementCount;
    private int sneakTicksLeft;
    private boolean extraBlockPlaced;
    private Timer timeSinceHurt;
    private float watchdogNoiseOffset;
    private int watchdogNoiseTicks;
    private float visualYaw;
    private BlockPos lastPlacedPos;

    public Scaffold() {

        super(MODULE_ID, "", "", Category.Misc);

        this.keepY = new BooleanSetting(KEEP_Y_SETTING_ID, false);

        ModeSetting swingModeSetting = new ModeSetting(SWING_MODE_SETTING_ID);
        String[] swingModes = new String[2];

        swingModes[0] = "Normal";

        swingModes[1] = "Silent";
        ModeSetting configuredSwingMode = swingModeSetting.addModes(swingModes);
        this.swingMode = configuredSwingMode.setDefault("Normal");

        this.tower = new BooleanSetting(TOWER_SETTING_ID, false);

        this.jump = new BooleanSetting(JUMP_SETTING_ID, false);

        this.noDiagonalJump = new BooleanSetting(NO_DIAGONAL_JUMP_SETTING_ID, false);

        this.jumpFrequency = new NumberSetting(JUMP_FREQUENCY_SETTING_ID, 0.0F, 0.0F, 10.0F, 1.0F);

        this.sneak = new BooleanSetting(SNEAK_SETTING_ID, false);

        this.sneakXBlocks = new NumberSetting(SNEAK_X_BLOCKS_SETTING_ID, 1.0F, 1.0F, 10.0F, 1.0F);

        this.sneakTicks = new NumberSetting(SNEAK_TICKS_SETTING_ID, 1.0F, 1.0F, 20.0F, 1.0F);

        this.expand = new BooleanSetting(EXPAND_SETTING_ID, false);

        this.expandDistance = new NumberSetting(EXPAND_DISTANCE_SETTING_ID, 1.0F, 1.0F, 6.0F, 1.0F);

        this.onlyOnRightClick = new BooleanSetting(ONLY_ON_RIGHT_CLICK_SETTING_ID, false);

        SettingGroup movementGroup = new SettingGroup(MOVEMENT_GROUP_ID);
        Setting[] movementValues =
                new Setting[] {
                    this.tower,
                    this.jump,
                    this.noDiagonalJump,
                    this.jumpFrequency,
                    this.sneak,
                    this.sneakXBlocks,
                    null,
                    null,
                    null,
                    null
                };
        movementValues[6] = this.sneakTicks;
        movementValues[7] = this.expand;
        movementValues[8] = this.expandDistance;
        movementValues[9] = this.onlyOnRightClick;
        this.movementSettings = movementGroup.addValues(movementValues);

        ModeSetting rotationModeSetting = new ModeSetting(ROTATION_TYPE_SETTING_ID);
        String[] rotationModes = new String[6];

        rotationModes[0] = "Perfect";

        rotationModes[1] = "Perfect Brute";

        rotationModes[2] = "Watchdog";

        rotationModes[3] = "Watchdog 2";

        rotationModes[4] = "Backwards";

        rotationModes[5] = "None";
        ModeSetting configuredRotationMode = rotationModeSetting.addModes(rotationModes);
        this.rotationType = configuredRotationMode.setDefault("Perfect");

        this.extraBlock = new BooleanSetting(EXTRA_BLOCK_SETTING_ID, false);

        this.forwardOnGround = new BooleanSetting(FORWARD_ON_GROUND_SETTING_ID, false);

        this.snapRotations = new BooleanSetting(SNAP_ROTATIONS_SETTING_ID, false);

        ModeSetting rayCastSetting = new ModeSetting(RAY_CAST_MODE_SETTING_ID);
        String[] rayCastModes = new String[3];

        rayCastModes[0] = "Standard";

        rayCastModes[1] = "Strict";

        rayCastModes[2] = "Disabled";
        ModeSetting configuredRayCastMode = rayCastSetting.addModes(rayCastModes);
        this.rayCastMode = configuredRayCastMode.setDefault("Standard");

        this.watchdogNoise = new BooleanSetting(WATCHDOG_NOISE_SETTING_ID, true);

        this.slow = new BooleanSetting(SLOW_SETTING_ID, false);
        this.slowDelay = new NumberSetting(SLOW_DELAY_SETTING_ID, 1.0F, 1.0F, 4.0F, 1.0F);
        this.organic = new BooleanSetting(ORGANIC_SETTING_ID, false);
        this.rotateOnFrame = new BooleanSetting(ROTATE_ON_FRAME_SETTING_ID, true);
        this.yawSpeed = new RangeSetting(YAW_SPEED_SETTING_ID, 90.0F, 90.0F, 5.0F, 360.0F, 5.0F);
        this.pitchSpeed =
                new RangeSetting(PITCH_SPEED_SETTING_ID, 90.0F, 90.0F, 5.0F, 360.0F, 5.0F);
        this.watchdogSpeed =
                new RangeSetting(WATCHDOG_SPEED_SETTING_ID, 110.0F, 120.0F, 40.0F, 130.0F, 1.0F);
        SettingGroup rotationGroup = new SettingGroup(ROTATIONS_GROUP_ID);

        Setting[] rotationValues =
                new Setting[] {
                    this.rotationType,
                    this.extraBlock,
                    this.forwardOnGround,
                    this.snapRotations,
                    this.rotateOnFrame,
                    this.rayCastMode,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        rotationValues[6] = this.watchdogNoise;

        rotationValues[7] = this.slow;

        rotationValues[8] = this.slowDelay;

        rotationValues[9] = this.organic;

        rotationValues[10] = this.watchdogSpeed;

        rotationValues[11] = this.yawSpeed;

        rotationValues[12] = this.pitchSpeed;
        this.rotationSettings = rotationGroup.addValues(rotationValues);
        this.hideJump = new BooleanSetting(HIDE_JUMP_SETTING_ID, false);
        this.blockESP = new BooleanSetting(BLOCK_ESP_SETTING_ID, false);
        this.blockHighlightColor = new ColorSetting(BLOCK_HIGHLIGHT_COLOR_SETTING_ID, Color.CYAN);
        ModeSetting blockCounterSetting = new ModeSetting(BLOCK_COUNTER_SETTING_ID);
        String[] blockCounterModes = new String[3];

        blockCounterModes[0] = "Off";

        blockCounterModes[1] = "Simple";

        blockCounterModes[2] = "Fancy";
        ModeSetting configuredBlockCounter = blockCounterSetting.addModes(blockCounterModes);

        this.blockCounter = configuredBlockCounter.setDefault("Fancy");
        this.counterColors = new ColorSetting(COUNTER_COLORS_SETTING_ID, Color.WHITE);
        ModeSetting visualRotationSetting = new ModeSetting(SILENT_ROTATIONS_SETTING_ID);
        String[] visualRotationModes = new String[3];

        visualRotationModes[0] = "Off";

        visualRotationModes[1] = "Normal";

        visualRotationModes[2] = "Sideways";
        ModeSetting configuredVisualRotation = visualRotationSetting.addModes(visualRotationModes);

        this.silentRotations = configuredVisualRotation.setDefault("Normal");
        SettingGroup visualGroup = new SettingGroup(VISUALS_GROUP_ID);

        Setting[] visualValues =
                new Setting[] {
                    this.swingMode,
                    this.hideJump,
                    this.silentRotations,
                    this.blockESP,
                    this.blockHighlightColor,
                    this.blockCounter,
                    null
                };

        visualValues[6] = this.counterColors;
        this.visualSettings = visualGroup.addValues(visualValues);
        this.placeSideSign = 1.0F;
        this.timeSinceHurt = new Timer();
        this.watchdogNoiseOffset = 0.0F;
        this.watchdogNoiseTicks = 0;
        this.organic.hideIf(
                () -> {
                    return !this.rayCastMode.is("Disabled");
                });
        this.organic.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return !rotationModeName.contains("Watchdog");
                });
        this.watchdogSpeed.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return !rotationModeName.contains("Watchdog");
                });
        this.slow.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return !rotationModeName.contains("Watchdog");
                });
        this.slowDelay.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return !rotationModeName.contains("Watchdog");
                });
        this.slowDelay.hideIf(
                () -> {
                    return !this.slow.getValue();
                });
        this.rotateOnFrame.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return rotationModeName.contains("Watchdog");
                });
        this.yawSpeed.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return rotationModeName.contains("Watchdog");
                });
        this.pitchSpeed.hideIf(
                () -> {
                    String rotationModeName = this.rotationType.getValue().getName();

                    return rotationModeName.contains("Watchdog");
                });
        this.jumpFrequency.hideIf(
                () -> {
                    return !this.jump.getValue();
                });
        this.noDiagonalJump.hideIf(
                () -> {
                    return !this.jump.getValue();
                });
        this.sneakXBlocks.hideIf(
                () -> {
                    return !this.sneak.getValue();
                });
        this.sneakTicks.hideIf(
                () -> {
                    return !this.sneak.getValue();
                });
        this.expandDistance.hideIf(
                () -> {
                    return !this.expand.getValue();
                });
        this.onlyOnRightClick.hideIf(
                () -> {
                    return !this.jump.getValue()
                            && !this.sneak.getValue()
                            && !this.keepY.getValue()
                            && !this.forwardOnGround.getValue();
                });
        this.blockHighlightColor.hideIf(
                () -> {
                    return !this.blockESP.getValue();
                });
        ModeSetting counterColorMode = this.counterColors.getColorMode();

        counterColorMode.setDefault("Client");
        this.addSettings(
                this.keepY, this.movementSettings, this.rotationSettings, this.visualSettings);
        new BlockCounterRenderer(this, this.blockCounter, this.counterColors);
    }

    private String getRayCastMode() {
        return this.rayCastMode.getValue().getName();
    }

    /** Maintains snap placement and its visual rotation while the jump path is active. */
    private void handleSnapPath() {
        float movementYaw = MoveUtil.getDirection(mc.player.getYRot());
        if (this.snapPlaceCountdown > 0) {
            this.snapPlaceCountdown--;
        }

        this.snapBruteForceAndPlace(movementYaw);
        float diagonalYaw = movementYaw + 45.0F;
        int placementCountdown = this.snapPlaceCountdown;
        byte yawUpdateCountdown;
        if (Math.abs(Mth.wrapDegrees(diagonalYaw - this.snapYaw)) > 60.0F) {

            yawUpdateCountdown = 8;
        } else {

            yawUpdateCountdown = 9;
        }

        if (placementCountdown == yawUpdateCountdown
                && !mc.options.keyJump.isDown()
                && this.snapInitialized) {
            this.snapYaw = diagonalYaw;
        }

        if (mc.player.onGround() && mc.options.keyJump.isDown()) {
            this.snapYaw = movementYaw;
        }

        this.targetYaw = this.snapYaw;
        this.targetPitch = this.snapPitch;
        Rotation snapRotation =
                new Rotation(this.snapYaw, this.snapPitch, 360.0F, 360.0F, RotationPriority.HIGH);
        snapRotation.setTickStepped(true);
        RotationManager.getInstance().submit(snapRotation);
        RotationManager.getInstance().forceRotation(this.snapYaw, this.snapPitch);
        Rotation activeRotation = RotationManager.getInstance().getCurrent();
        float visualMoveYaw = MoveUtil.isMoving() ? MoveUtil.getMoveYaw() : mc.player.getYRot();

        activeRotation.setVisualPitchFollows(!this.silentRotations.is("Sideways"));
        activeRotation.setCustomVisual(true);
        activeRotation.setVisualPitch(activeRotation.getPitch());
        if (this.silentRotations.is("Sideways")) {
            activeRotation.setVisualYaw(
                    MoveUtil.isMovingDiagonally() ? visualMoveYaw - 135.0F : visualMoveYaw - 90.0F);
            activeRotation.setVisualPitch(90.0F);
        } else {
            if (this.silentRotations.is("Normal")) {
                this.visualYaw =
                        RotationUtil.rotateTowards(this.visualYaw, visualMoveYaw - 180.0F, 60.0F);
                activeRotation.setVisualYaw(this.visualYaw);
            } else {
                activeRotation.setVisualYaw(visualMoveYaw - 180.0F);
            }
        }

        this.snapInitialized = true;
    }

    private void placeBlock(
            InteractionHand hand, int blockSlot, Vec3 hitVec, float yaw, float pitch) {
        if (hand == InteractionHand.MAIN_HAND) {
            InventoryUtil.setSlot(blockSlot);
        }

        BlockHitResult hitResult =
                new BlockHitResult(
                        hitVec, this.placementTarget.face(), this.placementTarget.pos(), false);
        this.lastPlacedPos = this.placementTarget.pos().relative(this.placementTarget.face());
        String rotationMode = this.getRotationMode();

        if (!rotationMode.equals("None")) {
            RotationManager.getInstance().forceRotation(yaw, pitch);
        }

        this.useItemAndSwing(hand, hitResult);
        this.jumpPlacementCount++;
        if (this.sneak.getValue()) {
            this.sneakPlacementCount++;
        }
    }

    @Override
    public String getSuffix() {
        return this.getRotationMode();
    }

    private Vec3 getFaceAimPoint(BlockData target) {
        VoxelShape collisionShape =
                mc.level.getBlockState(target.pos()).getCollisionShape(mc.level, target.pos());
        AABB bounds =
                collisionShape.isEmpty()
                        ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                        : collisionShape.bounds();
        Vec3 center = bounds.getCenter();
        Vec3 faceOffset = new Vec3(target.face().getUnitVec3i()).scale(0.25);
        return new Vec3(
                        target.pos().getX() + center.x,
                        target.pos().getY() + center.y,
                        target.pos().getZ() + center.z)
                .add(faceOffset);
    }

    @EventTarget
    public void onContinueAttack(ContinueAttackEvent event) {
        if (mc.hitResult instanceof BlockHitResult) {
            event.setCancelled(true);
        }
    }

    public double getHideJumpEyeY() {
        return this.hideJumpEyeY;
    }

    public boolean isHideJumpActive() {
        return this.isEnabled() && this.hideJump.getValue();
    }

    public boolean isValidBlock(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockState defaultState = block.defaultBlockState();
            if (!defaultState.isCollisionShapeFullBlock(mc.level, mc.player.blockPosition())) {
                return false;
            } else if (block instanceof Fallable) {
                return false;
            } else if (block instanceof TntBlock) {
                return false;
            } else {
                return defaultState.hasBlockEntity()
                        ? false
                        : defaultState.getMenuProvider(mc.level, mc.player.blockPosition()) == null;
            }
        } else {
            return false;
        }
    }

    /** Organic placement may bypass ray casting after the hurt delay; all other cases test the target. */
    private boolean canPlaceWithRotation(BlockData target, float yaw, float pitch) {
        if (this.timeSinceHurt.hasPassed(1000L) && this.organic.getValue()) {
            String rayCastMode = this.getRayCastMode();

            if (rayCastMode.equals("Disabled")) {
                return true;
            }
        }

        return this.rayCastHitsTarget(target, yaw, pitch);
    }

    @Override
    public void onDisable() {
        DelayedSlotRestorer.scheduleSlotRestore(this.originalHotbarSlot);
        if (Blink.isActive()) {
            Blink.stop();
        }

        RotationManager.getInstance().reset();
        this.originalHotbarSlot = -1;
    }

    private String getRotationMode() {

        if (this.onlyOnRightClick.getValue() && !mc.options.keyUse.isDown()) {

            return "Perfect Brute";
        } else {
            return this.rotationType.getValue().getName();
        }
    }

    private Vec3 getRandomFaceHitVec(BlockData target) {
        if (target == null) {
            return null;
        }

        BlockPos blockPos = target.pos();
        VoxelShape collisionShape =
                mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos);
        AABB bounds =
                collisionShape.isEmpty()
                        ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
                        : collisionShape.bounds();
        double hitX = blockPos.getX() + bounds.minX + Math.random() * (bounds.maxX - bounds.minX);
        double hitY = blockPos.getY() + bounds.minY + Math.random() * (bounds.maxY - bounds.minY);
        double hitZ = blockPos.getZ() + bounds.minZ + Math.random() * (bounds.maxZ - bounds.minZ);
        switch (DirectionSwitchMap.switchMapDirection[target.face().ordinal()]) {
            case 1:
                hitY = blockPos.getY() + bounds.minY;
                break;
            case 2:
                hitY = blockPos.getY() + bounds.maxY;
                break;
            case 3:
                hitZ = blockPos.getZ() + bounds.minZ;
                break;
            case 4:
                hitZ = blockPos.getZ() + bounds.maxZ;
                break;
            case 5:
                hitX = blockPos.getX() + bounds.minX;
                break;
            case 6:
                hitX = blockPos.getX() + bounds.maxX;
        }

        return new Vec3(hitX, hitY, hitZ);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getStage() != EventStage.POST) {
            if (mc.player.hurtTime != 0) {
                this.timeSinceHurt.reset();
            }

            if (mc.player.onGround()) {
                this.extraBlockPlaced = false;
            }

            BlockPos playerBlockPos = mc.player.blockPosition();
            if (mc.options.keyJump.isDown()) {
                this.heightAnchor = playerBlockPos.getY();
                if (this.hideJump.getValue()) {
                    this.hideJumpEyeY = mc.player.getEyeY();
                }
            }

            if (this.hideJump.getValue() && mc.player.onGround()) {
                this.hideJumpEyeY = mc.player.getEyeY();
            }

            boolean placementInputActive =
                    !this.onlyOnRightClick.getValue() || mc.options.keyUse.isDown();
            double movementRadians = Math.toRadians(MoveUtil.getDirection(mc.player.getYRot()));
            this.placementTarget = null;

            for (int expandOffset =
                            MoveUtil.isMoving() && this.expand.getValue()
                                    ? this.expandDistance.getValue().intValue()
                                    : 0;
                    expandOffset >= 0 && this.placementTarget == null;
                    expandOffset--) {
                this.placementTarget =
                        this.getBlockData(
                                BlockPos.containing(
                                        mc.player.getX() - Math.sin(movementRadians) * expandOffset,
                                        (this.keepY.getValue() && placementInputActive
                                                        ? (int) this.heightAnchor
                                                        : playerBlockPos.getY())
                                                - 1,
                                        mc.player.getZ()
                                                + Math.cos(movementRadians) * expandOffset));
            }

            if (this.isSnapActive()
                    && Astralis.getInstance().hasHypixelLocation()
                    && !MoveUtil.isMovingDiagonally()) {
                String snapRotationMode = this.getRotationMode();

                if (!snapRotationMode.equals("None")
                        && (mc.options.keyJump.isDown()
                                || this.jump.getValue() && placementInputActive
                                || this.snapPathActive && !mc.player.onGround())) {
                    if (!this.snapPathActive) {
                        this.snapYaw = RotationManager.getInstance().getYaw();
                        this.snapPitch = RotationManager.getInstance().getPitch();
                        this.snapPlaceCountdown = 0;
                        this.snapInitialized = false;
                    }

                    this.snapPathActive = true;
                    this.handleSnapPath();
                    return;
                }
            }

            this.snapPathActive = false;
            this.updateRotations();
            String yawRotationMode = this.getRotationMode();

            float placementYaw =
                    yawRotationMode.equals("None")
                            ? mc.player.getYRot()
                            : RotationManager.getInstance().getYaw();
            String pitchRotationMode = this.getRotationMode();

            float placementPitch =
                    pitchRotationMode.equals("None")
                            ? mc.player.getXRot()
                            : RotationManager.getInstance().getPitch();
            int blockSlot = this.findBlockSlot();
            if (blockSlot != -1) {

                InteractionHand hand =
                        blockSlot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                if (this.extraBlock.getValue()
                        && !this.extraBlockPlaced
                        && !mc.player.onGround()
                        && mc.player.getDeltaMovement().y < 0.0
                        && !mc.options.keyJump.isDown()
                        && mc.player.getY() > this.heightAnchor
                        && mc.player.getY()
                                        + MoveUtil.predictMotionY(mc.player.getDeltaMovement().y, 3)
                                < this.heightAnchor + 1.0
                        && mc.hitResult instanceof BlockHitResult extraBlockHit
                        && extraBlockHit.getType() == Type.BLOCK
                        && !mc.level.getBlockState(extraBlockHit.getBlockPos()).isAir()) {
                    if (hand == InteractionHand.MAIN_HAND) {
                        InventoryUtil.setSlot(blockSlot);
                    }

                    this.useItemAndSwing(hand, extraBlockHit);
                    this.extraBlockPlaced = true;
                } else if (this.placementTarget != null) {
                    brutePlacement:
                    {
                        String placementRotationMode = this.getRotationMode();

                        if (!placementRotationMode.equals("Watchdog")) {

                            if (!placementRotationMode.equals("Perfect Brute")) {
                                break brutePlacement;
                            }
                        }

                        if (this.placeRotation != null) {
                            this.placeBlock(
                                    hand,
                                    blockSlot,
                                    this.placeRotation.getHitVec(),
                                    this.targetYaw,
                                    this.targetPitch);
                            return;
                        }
                    }

                    if (!this.canPlaceWithRotation(
                            this.placementTarget, placementYaw, placementPitch)) {
                        String failedRayCastMode = this.getRayCastMode();

                        if (!failedRayCastMode.equals("Disabled") || this.organic.getValue()) {
                            return;
                        }
                    }

                    String airRotationMode = this.getRotationMode();

                    if (airRotationMode.contains("Watchdog") && MoveUtil.airTicks <= 1) {
                        String earlyAirRayCastMode = this.getRayCastMode();

                        if (earlyAirRayCastMode.equals("Disabled")) {
                            return;
                        }
                    }

                    this.placeBlock(
                            hand,
                            blockSlot,
                            this.getRandomFaceHitVec(this.placementTarget),
                            placementYaw,
                            placementPitch);
                }
            }
        }
    }

    /** Selects the largest valid stack in the hotbar or offhand (slot 40). */
    private int findBlockSlot() {

        int bestSlot = -1;
        int largestStackCount = 0;
        int slot = 0;

        while (true) {

            if (slot >= 9) {
                ItemStack offhandStack = mc.player.getOffhandItem();
                if (this.isValidBlock(offhandStack)
                        && offhandStack.getCount() > largestStackCount) {

                    bestSlot = 40;
                }

                return bestSlot;
            }

            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (this.isValidBlock(stack) && stack.getCount() > largestStackCount) {
                largestStackCount = stack.getCount();
                bestSlot = slot;
            }

            slot++;
        }
    }

    @EventTarget
    public void onRenderHeldItem(RenderHeldItemEvent event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && this.originalHotbarSlot != -1) {
            event.setStack(mc.player.getInventory().getItem(this.originalHotbarSlot));
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.blockESP.getValue() && this.lastPlacedPos != null) {
            BlockState blockState = mc.level.getBlockState(this.lastPlacedPos);
            VoxelShape shape =
                    blockState.isAir() ? null : blockState.getShape(mc.level, this.lastPlacedPos);
            AABB bounds =
                    shape != null && !shape.isEmpty()
                            ? shape.bounds().move(this.lastPlacedPos)
                            : new AABB(this.lastPlacedPos);
            RenderUtil.drawSettingBox(
                    event.getPoseStack(), bounds.inflate(0.002), this.blockHighlightColor);
        }
    }

    private boolean rayCastHitsTarget(BlockData target, float yaw, float pitch) {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        Vec3 lookDirection = RotationUtil.getLookVector(pitch, yaw);
        double reach = 4.5;
        Vec3 rayEnd = eyePosition.add(lookDirection.scale(reach));
        BlockHitResult hitResult =
                mc.level.clip(
                        new ClipContext(
                                eyePosition,
                                rayEnd,
                                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                                Fluid.NONE,
                                mc.player));
        if (hitResult.getType() == Type.MISS) {
            return false;
        }

        String rayCastMode = this.getRayCastMode();

        return !rayCastMode.equals("Strict")
                ? hitResult.getBlockPos().equals(target.pos())
                : hitResult.getBlockPos().equals(target.pos())
                        && hitResult.getDirection() == target.face();
    }

    /** Finds support faces near a replaceable position, then tries the closest placements first. */
    private BlockData getBlockData(BlockPos targetPos) {
        if (mc.level.getBlockState(targetPos).canBeReplaced()) {
            MutableBlockPos searchPos = new MutableBlockPos();
            ArrayList<BlockData> candidates = new ArrayList<>();
            byte searchExtent = 4;

            for (int verticalOffset = 0; verticalOffset > -4; verticalOffset--) {
                for (int offsetX = 0; offsetX < searchExtent; offsetX++) {
                    for (int offsetZ = 0; offsetZ < searchExtent; offsetZ++) {
                        for (int offsetSign : new int[] {1, -1}) {
                            searchPos.set(
                                    targetPos.getX() + offsetX * offsetSign,
                                    targetPos.getY() + verticalOffset * offsetSign,
                                    targetPos.getZ() + offsetZ * offsetSign);
                            if (mc.level.getBlockState(searchPos).canBeReplaced()) {
                                for (Direction direction : Direction.values()) {
                                    BlockPos supportPos = searchPos.relative(direction);
                                    if (!mc.level.getBlockState(supportPos).canBeReplaced()) {
                                        candidates.add(
                                                new BlockData(supportPos, direction.getOpposite()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                return null;
            }

            candidates.sort(
                    Comparator.comparingDouble(
                            candidate ->
                                    candidate.pos().relative(candidate.face()).distSqr(targetPos)));

            for (BlockData candidate : candidates) {
                double distanceSquared =
                        mc.player
                                .position()
                                .distanceToSqr(
                                        new Vec3(
                                                candidate.pos().getX() + 0.5,
                                                candidate.pos().getY() + 0.5,
                                                candidate.pos().getZ() + 0.5));
                if (distanceSquared <= 20.25) {
                    return new BlockData(candidate.pos(), candidate.face());
                }
            }
        }

        return null;
    }

    @Override
    public void onEnable() {
        this.heightAnchor = mc.player.getY();
        this.originalHotbarSlot = mc.player.getInventory().getSelectedSlot();
        this.hideJumpEyeY = mc.player.getEyeY();
        this.targetYaw = MoveUtil.getMoveYaw() - 180.0F;
        this.targetPitch = 81.5F;
        this.placeRotation = null;
        this.snapYaw = mc.player.getYRot();
        this.snapPitch = 83.0F;
        this.snapPlaceCountdown = 0;
        this.snapInitialized = false;
        this.snapPathActive = false;
        this.placeSideSign = 1.0F;
        this.visualYaw = mc.player.getYRot();
        this.hideJumpEyeY = mc.player.getEyeY();
        this.extraBlockPlaced = false;
        this.timeSinceHurt.reset();
    }

    @EventTarget
    public void onStartUseItem(StartUseItemEvent event) {
        if (mc.hitResult instanceof BlockHitResult) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.jump.getValue()
                && (!this.onlyOnRightClick.getValue() || mc.options.keyUse.isDown())) {
            if (this.jumpFrequency.getValue() == 0.0F) {
                event.setJump(true);
            } else if (this.jumpPlacementCount % this.jumpFrequency.getValue() == 0.0F
                    && (!MoveUtil.isMovingDiagonally() || !this.noDiagonalJump.getValue())) {
                event.setJump(true);
                this.jumpPlacementCount = 0;
            }
        }

        if (this.sneak.getValue()
                && (!this.onlyOnRightClick.getValue() || mc.options.keyUse.isDown())) {
            if (this.sneakTicksLeft > 0) {
                event.setSneak(true);
                this.sneakTicksLeft--;
            } else if (this.sneakPlacementCount >= this.sneakXBlocks.getValue().intValue()) {
                event.setSneak(true);
                this.sneakPlacementCount = 0;
                this.sneakTicksLeft = this.sneakTicks.getValue().intValue() - 1;
            }
        } else {
            this.sneakPlacementCount = 0;
            this.sneakTicksLeft = 0;
        }

        if (this.slow.getValue()
                && MoveUtil.airTicks >= 4
                && MoveUtil.airTicks <= 3 + this.slowDelay.getValue().intValue()) {
            String rotationMode = this.getRotationMode();

            if (rotationMode.contains("Watchdog") && MoveUtil.isMoving()) {
                event.setForward(true);
            }
        }
    }

    private boolean isSnapActive() {
        return this.snapRotations.getValue()
                || this.onlyOnRightClick.getValue() && !mc.options.keyUse.isDown();
    }

    private void useItemAndSwing(InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult interactionResult = mc.gameMode.useItemOn(mc.player, hand, hitResult);
        if (!LocalProxy.isConnectedViaProxy() || interactionResult instanceof Success) {
            BlockPlaces.onBlockPlaced(hitResult.getBlockPos(), hitResult.getDirection());
            switch (this.swingMode.getValue().getName()) {
                case "Normal":
                    mc.player.swing(hand);
                    break;
                case "Silent":
                    PacketUtil.send(new ServerboundSwingPacket(hand));
            }
        }
    }

    private void updateRotations() {
        float backwardsYaw = MoveUtil.getMoveYaw() - 180.0F;
        String rotationMode = this.getRotationMode();
        this.placeRotation = null;

        if (!rotationMode.equals("None")) {
            if (this.isSnapActive()
                    && Astralis.getInstance().hasHypixelLocation()
                    && this.placementTarget != null) {
                double playerYawRadians = Math.toRadians(mc.player.getYRot());
                double targetSideOffset =
                        -(this.placementTarget.pos().getX() + 0.5 - mc.player.getX())
                                        * Math.cos(playerYawRadians)
                                - (this.placementTarget.pos().getZ() + 0.5 - mc.player.getZ())
                                        * Math.sin(playerYawRadians);
                if (Math.abs(targetSideOffset) > 0.001) {
                    this.placeSideSign = targetSideOffset > 0.0 ? 1.0F : -1.0F;
                }
            }

            if (this.forwardOnGround.getValue()
                    && (!this.onlyOnRightClick.getValue() || mc.options.keyUse.isDown())
                    && MoveUtil.isMoving()
                    && MoveUtil.airTicks < 1) {
                this.targetYaw = MoveUtil.getMoveYaw();
                this.targetPitch = 81.5F;
            } else {
                switch (rotationMode) {
                    case "Perfect":
                    case "Watchdog 2":
                        if (this.placementTarget != null) {
                            float[] faceRotations =
                                    RotationUtil.getRotationsTo(
                                            this.getFaceAimPoint(this.placementTarget));
                            this.targetYaw = faceRotations[0];
                            this.targetPitch = faceRotations[1];
                        } else {
                            this.targetYaw = backwardsYaw;
                        }
                        break;
                    case "Watchdog":
                    case "Perfect Brute":
                        if (this.placementTarget != null) {
                            if (!RotationUtil.rayTraceHitsBlock(
                                    this.placementTarget.face(),
                                    this.placementTarget.pos(),
                                    true,
                                    4.5F,
                                    this.targetYaw,
                                    this.targetPitch)) {
                                BlockPos pitchScanPos = this.placementTarget.pos();
                                Direction pitchScanFace = this.placementTarget.face();

                                this.targetPitch =
                                        RotationUtil.scanPitch(
                                                pitchScanPos,
                                                pitchScanFace,
                                                this.targetYaw,
                                                this.targetPitch,
                                                90);
                                if ((!MoveUtil.isMovingDiagonally() || MoveUtil.airTicks < 5)
                                        && !RotationUtil.rayTraceHitsBlock(
                                                this.placementTarget.face(),
                                                this.placementTarget.pos(),
                                                true,
                                                4.5F,
                                                this.targetYaw,
                                                this.targetPitch)) {
                                    this.targetYaw = MoveUtil.getMoveYaw() - 180.0F;
                                    if (!RotationUtil.rayTraceHitsBlock(
                                            this.placementTarget.face(),
                                            this.placementTarget.pos(),
                                            false,
                                            4.5F,
                                            this.targetYaw,
                                            this.targetPitch)) {
                                        BlockPos fallbackScanPos = this.placementTarget.pos();
                                        Direction fallbackScanFace = this.placementTarget.face();

                                        this.targetPitch =
                                                RotationUtil.scanPitch(
                                                        fallbackScanPos,
                                                        fallbackScanFace,
                                                        this.targetYaw,
                                                        this.targetPitch,
                                                        90);
                                    }
                                }
                            }

                            this.placeRotation =
                                    RotationUtil.findClosestPlaceRotation(
                                            this.placementTarget.pos(),
                                            this.placementTarget.face(),
                                            this.targetYaw,
                                            this.targetPitch,
                                            4.5F);
                            if (this.placeRotation != null) {
                                this.targetYaw = this.placeRotation.getYaw();
                                this.targetPitch = this.placeRotation.getPitch();
                            }
                        } else {
                            this.targetYaw = backwardsYaw;
                        }
                        break;
                    case "Backwards":
                        this.targetYaw =
                                MoveUtil.isMoving()
                                        ? MoveUtil.getMoveYaw() - 180.0F
                                        : mc.player.getYRot() - 180.0F;
                        this.targetPitch = 81.5F;
                        break;
                    case "Sideways":
                        this.targetYaw =
                                MoveUtil.isMoving()
                                        ? MoveUtil.getMoveYaw() - 180.0F
                                        : mc.player.getYRot() - 180.0F;
                        this.targetPitch = MoveUtil.isMovingDiagonally() ? 45.0F : 65.0F;
                }
            }

            if (this.isSnapActive()
                    && (this.placementTarget == null
                            || this.rayCastHitsTarget(
                                    this.placementTarget,
                                    mc.player.getYRot(),
                                    mc.player.getXRot()))) {
                if (Astralis.getInstance().getHypixelApi().isConnected()) {
                    this.targetYaw = mc.player.getYRot() + 45.0F * this.placeSideSign;
                } else {
                    this.targetYaw = mc.player.getYRot();
                    this.targetPitch = mc.player.getXRot();
                    this.placeRotation = null;
                }
            } else if (this.isSnapActive()
                    && Astralis.getInstance().getHypixelApi().isConnected()
                    && this.placementTarget != null) {
                PlaceRotation snapPlacement =
                        RotationUtil.bruteForcePlaceRotation(
                                this.placementTarget.pos(),
                                this.placementTarget.face(),
                                mc.player.getYRot(),
                                false,
                                this.placeSideSign);
                if (snapPlacement != null) {
                    this.targetYaw = snapPlacement.getYaw();
                    this.targetPitch = snapPlacement.getPitch();
                    this.placeRotation = snapPlacement;
                }
            }

            if (this.watchdogNoise.getValue()) {
                if (this.watchdogNoiseTicks <= 0) {
                    this.watchdogNoiseOffset = MathUtil.randomFloat(-2.5F, 0.0F);

                    this.watchdogNoiseTicks = MathUtil.randomInt(15, 18);
                } else {
                    this.watchdogNoiseTicks--;
                }
            } else {
                this.watchdogNoiseOffset = 0.0F;
            }

            Rotation requestedRotation;
            boolean tickStepped;
            rotationStepMode:
            {
                float watchdogYawSpeed =
                        (mc.player.onGround()
                                        ? 180.0F
                                        : (MoveUtil.airTicks < 2
                                                ? MathUtil.randomFloat(
                                                        this.watchdogSpeed.getLow(),
                                                        this.watchdogSpeed.getHigh())
                                                : 40.0F))
                                + this.watchdogNoiseOffset;

                requestedRotation =
                        new Rotation(
                                this.targetYaw,
                                this.targetPitch,
                                rotationMode.contains("Watchdog")
                                        ? watchdogYawSpeed
                                        : MathUtil.randomFloat(
                                                this.yawSpeed.getLow(), this.yawSpeed.getHigh()),
                                MathUtil.randomFloat(
                                        this.pitchSpeed.getLow(), this.pitchSpeed.getHigh()),
                                RotationPriority.HIGH);
                if (this.rotateOnFrame.getValue()) {

                    if (!rotationMode.contains("Watchdog")) {
                        tickStepped = false;
                        break rotationStepMode;
                    }
                }

                tickStepped = true;
            }

            float visualMoveYaw;
            boolean hypixelSnapActive;
            Rotation activeRotation;
            boolean customVisual;
            visualRotationMode:
            {
                requestedRotation.setTickStepped(tickStepped);
                RotationManager.getInstance().submit(requestedRotation);
                activeRotation = RotationManager.getInstance().getCurrent();
                visualMoveYaw = MoveUtil.isMoving() ? MoveUtil.getMoveYaw() : mc.player.getYRot();
                hypixelSnapActive =
                        this.isSnapActive() && Astralis.getInstance().getHypixelApi().isConnected();

                activeRotation.setVisualPitchFollows(!this.silentRotations.is("Sideways"));
                if (!hypixelSnapActive) {

                    if (this.silentRotations.is("Off")) {
                        customVisual = false;
                        break visualRotationMode;
                    }
                }

                customVisual = true;
            }

            activeRotation.setCustomVisual(customVisual);
            activeRotation.setVisualPitch(activeRotation.getPitch());

            if (this.silentRotations.is("Sideways")) {
                activeRotation.setVisualYaw(
                        MoveUtil.isMovingDiagonally()
                                ? visualMoveYaw - 135.0F
                                : visualMoveYaw - 90.0F);
                activeRotation.setVisualPitch(90.0F);
            } else {

                if (this.silentRotations.is("Normal")) {
                    this.visualYaw =
                            RotationUtil.rotateTowards(
                                    this.visualYaw, visualMoveYaw - 180.0F, 60.0F);
                    activeRotation.setVisualYaw(this.visualYaw);
                } else if (hypixelSnapActive) {
                    activeRotation.setVisualYaw(visualMoveYaw - 180.0F);
                } else {
                    activeRotation.setVisualYaw(activeRotation.getYaw());
                }
            }
        }
    }

    @EventTarget
    public void onMouseScroll(MouseScrollEvent event) {
        if (mc.options.getCameraType().isFirstPerson() && mc.gui.screen() == null) {
            event.setCancelled(true);
        }
    }

    private void snapBruteForceAndPlace(float movementYaw) {
        this.placeRotation = null;
        if (this.placementTarget != null) {
            int blockSlot = this.findBlockSlot();
            if (blockSlot != -1) {
                this.placeRotation =
                        RotationUtil.bruteForcePlaceRotation(
                                this.placementTarget.pos(),
                                this.placementTarget.face(),
                                movementYaw + 90.0F,
                                true,
                                this.placeSideSign);
                if (this.placeRotation != null) {
                    float maxRotationStep = (float) (200.0 - Math.random());
                    this.snapYaw =
                            this.snapYaw
                                    + Math.clamp(
                                            Mth.wrapDegrees(
                                                    this.placeRotation.getYaw() - this.snapYaw),
                                            -maxRotationStep,
                                            maxRotationStep);
                    this.snapYaw =
                            mc.player.getYRot()
                                    + Mth.wrapDegrees(this.snapYaw - mc.player.getYRot());
                    this.snapPitch =
                            this.snapPitch
                                    + Math.clamp(
                                            Mth.wrapDegrees(
                                                    this.placeRotation.getPitch() - this.snapPitch),
                                            -maxRotationStep,
                                            maxRotationStep);
                    if (!(Math.abs(Mth.wrapDegrees(this.snapYaw - this.placeRotation.getYaw()))
                                    > 0.1F)
                            && !(Math.abs(
                                            Mth.wrapDegrees(
                                                    this.snapPitch - this.placeRotation.getPitch()))
                                    > 0.1F)
                            && (!mc.player.onGround() || !mc.options.keyJump.isDown())) {

                        this.placeBlock(
                                blockSlot == 40
                                        ? InteractionHand.OFF_HAND
                                        : InteractionHand.MAIN_HAND,
                                blockSlot,
                                this.placeRotation.getHitVec(),
                                this.snapYaw,
                                this.snapPitch);
                        this.snapPlaceCountdown = 10;
                    }
                }
            }
        }
    }

    private static final String MODULE_ID = "074047925307c6c6571bff82fc8050fd";
    private static final String KEEP_Y_SETTING_ID = "0b53b3202d3862265e037d9fc80de409";
    private static final String SWING_MODE_SETTING_ID = "98ea31083df0c406e4d7c0c13ebe064d";
    private static final String TOWER_SETTING_ID = "d85a9695fdf6cff3b4207b8232260fd9";
    private static final String JUMP_SETTING_ID = "c58f91608c605292cc4dc60de7702c85";
    private static final String NO_DIAGONAL_JUMP_SETTING_ID = "43aefc59c8f1f3b846bbf1ec4f2c3045";
    private static final String JUMP_FREQUENCY_SETTING_ID = "e6381baffdd414cff09e4f92ed49749b";
    private static final String SNEAK_SETTING_ID = "90a3cd2f703d471ec3f30083d9d352b6";
    private static final String SNEAK_X_BLOCKS_SETTING_ID = "b94ca93fdb111c06926dc446f60b9363";
    private static final String SNEAK_TICKS_SETTING_ID = "44103af04f0bb58fc5c9c0a83ea9c927";
    private static final String EXPAND_SETTING_ID = "2fcd82b870f59b2b7228a3c0711bbcce";
    private static final String EXPAND_DISTANCE_SETTING_ID = "5b646d9fffb7c027fd013b224ef7bcb5";
    private static final String ONLY_ON_RIGHT_CLICK_SETTING_ID =
            "a3a659ba8b8a8c5c0c4b708c2999c59ed6e107aeebfd16f5ff018bc64a04d385";
    private static final String MOVEMENT_GROUP_ID = "078a6b9d4debc827e38ed6decb26f0d3";
    private static final String ROTATION_TYPE_SETTING_ID = "80ff86e1d8c41c87bbde758beb50d2cc";
    private static final String EXTRA_BLOCK_SETTING_ID = "b7227b21e7b4cade8e81f0b4f04d1fe3";
    private static final String FORWARD_ON_GROUND_SETTING_ID =
            "d7fb20a55d702ad53b48bc747a30a62f44128b37a79f925fc5de37abc848976e";
    private static final String SNAP_ROTATIONS_SETTING_ID = "81ef136084dc71005a530076cdbfdb65";
    private static final String RAY_CAST_MODE_SETTING_ID = "4f0cf329248bc83c0fba96037b020798";
    private static final String WATCHDOG_NOISE_SETTING_ID = "238e1cfba2feaeadb7b18aead1b38fe5";
    private static final String SLOW_SETTING_ID = "6a55678f0f31668a16a78a4c8a29c961";
    private static final String SLOW_DELAY_SETTING_ID = "f2f2bc485828b3697bb775b4aa960f06";
    private static final String ORGANIC_SETTING_ID = "32f6d06f8c6a413f1f26d6f97058745d";
    private static final String ROTATE_ON_FRAME_SETTING_ID = "684de05b53fd93607d33860c2d757998";
    private static final String YAW_SPEED_SETTING_ID = "7dafa637ea670fc815c9274ad2e35e7f";
    private static final String PITCH_SPEED_SETTING_ID = "7178b99f9c8ed1c6338486b30f7e0f8b";
    private static final String WATCHDOG_SPEED_SETTING_ID = "7ac7b9d6baa37fa6a0222931a27d554a";
    private static final String ROTATIONS_GROUP_ID = "b64d2022fbf8a68f2bcf0b4bf8376a1f";
    private static final String HIDE_JUMP_SETTING_ID = "26a93874f336c0b0ca25773459d943a9";
    private static final String BLOCK_ESP_SETTING_ID = "e102e481e35375b0b1201b5f467ae21f";
    private static final String BLOCK_HIGHLIGHT_COLOR_SETTING_ID =
            "bedea323e5b25a09af1e8e6244a33119";
    private static final String BLOCK_COUNTER_SETTING_ID = "b4ebeed1f591eccc1804609bac5749d2";
    private static final String COUNTER_COLORS_SETTING_ID = "b5840e8097ccb76aea8fed6fc8ab8a9e";
    private static final String SILENT_ROTATIONS_SETTING_ID = "1e5b6c0ea6d02a2d2a49bea94939fc0c";
    private static final String VISUALS_GROUP_ID = "087bb197a9ad627bd347a839d7f02549";
}
