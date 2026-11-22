package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ContinueAttackEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.module.impl.beddefender.BedDefenderDirectionSwitchMap;
import astralis.module.impl.beddefender.DefenseBlock;
import astralis.module.impl.beddefender.DefenseLayouts;
import astralis.module.impl.beddefender.PlacementCandidate;
import astralis.module.impl.beddefender.PlacementTarget;
import astralis.rotation.MoveFixMode;
import astralis.rotation.Rotation;
import astralis.rotation.RotationManager;
import astralis.rotation.RotationPriority;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.InventoryUtil;
import astralis.util.RotationUtil;
import astralis.util.player.TeamUtil;
import astralis.util.render.RenderUtil;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BedDefender extends Module {
    private static final float PLACE_REACH = 4.5F;
    private static Direction[] placeDirections;
    private final NumberSetting delayAfterSwap;
    private final NumberSetting delayAfterAiming;
    private final NumberSetting sneakHoldTicks;
    private final BooleanSetting render;
    private final ModeSetting defense;
    private boolean bedFound;
    private BlockPos bedPos;
    private Direction bedFacing;
    private BlockPos placementPos;
    private DefenseBlock currentDefenseBlock;
    private BlockPos supportPos;
    private Direction supportFace;
    private float aimYaw;
    private float aimPitch;
    private int swapDelayCounter;
    private int aimDelayCounter;
    private int sneakTicks;

    public BedDefender() {

        super(MODULE_ID, "", "", Category.Misc);

        this.delayAfterSwap =
                new NumberSetting(DELAY_AFTER_SWAP_SETTING_ID, 0.0F, 0.0F, 10.0F, 1.0F);

        this.delayAfterAiming =
                new NumberSetting(DELAY_AFTER_AIMING_SETTING_ID, 0.0F, 0.0F, 10.0F, 1.0F);

        this.sneakHoldTicks =
                new NumberSetting(SNEAK_HOLD_TICKS_SETTING_ID, 5.0F, 0.0F, 20.0F, 1.0F);

        this.render = new BooleanSetting(RENDER_SETTING_ID, true);

        ModeSetting modeSetting = new ModeSetting(DEFENSE_SETTING_ID, this);
        String[] defenseModes = new String[8];

        defenseModes[0] = "EndWoolCorners";

        defenseModes[1] = "EndWoolCorners2";

        defenseModes[2] = "EndGlassNoCorners";

        defenseModes[3] = "WoodWoolCorners";

        defenseModes[4] = "EndGlassWool";

        defenseModes[5] = "Wool1Layer";

        defenseModes[6] = "EndWoodLongCorners";

        defenseModes[7] = "EndWoolLong";
        ModeSetting modeSetting2 = modeSetting.addModes(defenseModes);
        this.defense = modeSetting2.setDefault("EndGlassNoCorners");
        this.addSettings(
                this.defense,
                this.delayAfterSwap,
                this.delayAfterAiming,
                this.sneakHoldTicks,
                this.render);
    }

    private BlockPos getDefenseBlockPos(DefenseBlock defenseBlock) {
        return this.bedPos.offset(
                this.rotateOffset(
                        defenseBlock.getOffsetX(),
                        defenseBlock.getOffsetY(),
                        defenseBlock.getOffsetZ(),
                        this.bedFacing));
    }

    private BlockPos[] getBedParts() {
        BlockState blockState = mc.level.getBlockState(this.bedPos);
        return !(blockState.getBlock() instanceof BedBlock)
                ? new BlockPos[] {this.bedPos}
                : new BlockPos[] {
                    this.bedPos, this.bedPos.relative(BedBlock.getConnectedDirection(blockState))
                };
    }

    private boolean isMaterial(ItemStack itemStack, String materialType) {
        String typeName = materialType;

        return switch (typeName) {
            case "wool" -> itemStack.is(ItemTags.WOOL);
            case "end_stone" -> itemStack.is(Items.END_STONE);
            case "planks" -> itemStack.is(ItemTags.PLANKS);
            case "stained_glass" -> {
                if (itemStack.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() instanceof StainedGlassBlock) {
                    yield true;
                }

                yield false;
            }
            default -> false;
        };
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        if (this.render.getValue() && this.placementPos != null) {
            PoseStack poseStack = render3DEvent.getPoseStack();
            AABB boundingBox = new AABB(this.placementPos).inflate(0.002);

            RenderUtil.drawBox(poseStack, boundingBox, new Color(0, 255, 0, 110));
        }
    }

    private Vec3 getFacePoint(AABB boundingBox, Direction direction, double u, double v) {
        double inset = direction.getAxis() == Axis.Y ? 0.01 : 0.05;

        return switch (BedDefenderDirectionSwitchMap.directionOrdinalMap[direction.ordinal()]) {
            case 1 ->
                    new Vec3(
                            Mth.lerp(u, boundingBox.minX, boundingBox.maxX),
                            boundingBox.maxY - inset,
                            Mth.lerp(v, boundingBox.minZ, boundingBox.maxZ));
            case 2 ->
                    new Vec3(
                            Mth.lerp(u, boundingBox.minX, boundingBox.maxX),
                            boundingBox.minY + inset,
                            Mth.lerp(v, boundingBox.minZ, boundingBox.maxZ));
            case 3 ->
                    new Vec3(
                            Mth.lerp(u, boundingBox.minX, boundingBox.maxX),
                            Mth.lerp(v, boundingBox.minY, boundingBox.maxY),
                            boundingBox.maxZ - inset);
            case 4 ->
                    new Vec3(
                            Mth.lerp(u, boundingBox.minX, boundingBox.maxX),
                            Mth.lerp(v, boundingBox.minY, boundingBox.maxY),
                            boundingBox.minZ + inset);
            case 5 ->
                    new Vec3(
                            boundingBox.maxX - inset,
                            Mth.lerp(u, boundingBox.minY, boundingBox.maxY),
                            Mth.lerp(v, boundingBox.minZ, boundingBox.maxZ));
            case 6 ->
                    new Vec3(
                            boundingBox.minX + inset,
                            Mth.lerp(u, boundingBox.minY, boundingBox.maxY),
                            Mth.lerp(v, boundingBox.minZ, boundingBox.maxZ));
            default -> throw new MatchException(null, null);
        };
    }

    private Vec3 getFaceCenter(BlockPos blockPos, Direction direction) {
        VoxelShape voxelShape = this.getInteractionShape(blockPos);
        AABB boundingBox =
                voxelShape.isEmpty() ? new AABB(blockPos) : voxelShape.bounds().move(blockPos);
        return this.getFacePoint(boundingBox, direction, 0.5, 0.5);
    }

    private boolean isValidPlacementHit(BlockHitResult blockHitResult, BlockPos blockPos) {
        if (blockHitResult.getType() != Type.BLOCK
                || blockPos == null
                || !mc.level.getBlockState(blockPos).isAir()) {
            return false;
        } else {
            return !blockHitResult
                            .getBlockPos()
                            .relative(blockHitResult.getDirection())
                            .equals(blockPos)
                    ? false
                    : !(mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock()
                                    instanceof BedBlock)
                            || blockHitResult.getDirection() == Direction.UP;
        }
    }

    private int findMaterialSlot(String materialType) {
        int slot = 0;

        while (true) {

            if (slot >= 9) {
                return -1;
            }

            ItemStack itemStack = mc.player.getInventory().getItem(slot);
            if (!itemStack.isEmpty() && this.isMaterial(itemStack, materialType)) {
                return slot;
            }

            slot++;
        }
    }

    private boolean updatePlan() {
        Vec3 vector3 = mc.player.position();
        java.util.ArrayList<PlacementCandidate> candidates = new java.util.ArrayList<>();
        boolean foundEmpty = false;
        boolean foundMaterial = false;

        for (DefenseBlock defenseBlock : this.getDefensePattern()) {
            BlockPos blockPos = this.getDefenseBlockPos(defenseBlock);
            if (mc.level.getBlockState(blockPos).isAir()) {
                foundEmpty = true;
                if (this.findMaterialSlot(defenseBlock.getBlockType()) != -1) {
                    foundMaterial = true;
                    if (this.hasPlacementSupport(blockPos)) {
                        candidates.add(
                                new PlacementCandidate(
                                        defenseBlock,
                                        blockPos,
                                        this.distanceToBed(blockPos),
                                        Vec3.atCenterOf(blockPos).distanceToSqr(vector3)));
                    }
                }
            }
        }

        if (foundEmpty && foundMaterial) {
            candidates.sort(
                    Comparator.comparingInt(PlacementCandidate::getBedDistance)
                            .thenComparingDouble(PlacementCandidate::getDistanceSq));
            if (this.placementPos != null && this.currentDefenseBlock != null) {
                for (PlacementCandidate candidate : candidates) {
                    if (candidate.getPosition().equals(this.placementPos)) {
                        PlacementTarget target = this.findPlacement(candidate.getPosition());
                        if (target != null) {
                            this.applyPlacement(target);
                            return true;
                        }
                        break;
                    }
                }
            }

            for (PlacementCandidate candidate : candidates) {
                PlacementTarget target = this.findPlacement(candidate.getPosition());
                if (target != null) {
                    this.selectTarget(candidate.getBlock(), candidate.getPosition(), target);
                    return true;
                }
            }

            if (!candidates.isEmpty()) {
                PlacementCandidate firstCandidate = (PlacementCandidate) candidates.getFirst();
                this.selectTarget(
                        firstCandidate.getBlock(),
                        firstCandidate.getPosition(),
                        this.findFallbackPlacement(firstCandidate.getPosition()));
                return true;
            } else {
                this.clearTarget();
                return true;
            }
        } else {
            this.clearTarget();
            return false;
        }
    }

    private int distanceToBed(BlockPos blockPos) {

        int minDistance = Integer.MAX_VALUE;

        for (BlockPos blockPos2 : this.getBedParts()) {
            minDistance = Math.min(minDistance, blockPos.distManhattan(blockPos2));
        }

        return minDistance;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.sneakTicks > 0
                || this.isAboveBed(this.placementPos)
                || this.supportPos != null
                        && mc.level != null
                        && mc.level.getBlockState(this.supportPos).getBlock() instanceof BedBlock) {
            moveInputEvent.setSneak(true);
        }
    }

    @EventTarget
    public void onStartUseItem(StartUseItemEvent startUseItemEvent) {
        if (this.bedFound) {
            startUseItemEvent.setCancelled(true);
        }
    }

    private void placeBlock() {
        BlockHitResult blockHitResult = this.traceBlock(this.aimYaw, this.aimPitch);
        float yaw = this.aimYaw;
        float pitch = this.aimPitch;
        if (!this.isValidPlacementHit(blockHitResult, this.placementPos)) {
            yaw = RotationManager.getInstance().getYaw();
            pitch = RotationManager.getInstance().getPitch();
            blockHitResult = this.traceBlock(yaw, pitch);
        }

        if (!this.isValidPlacementHit(blockHitResult, this.placementPos)) {
            this.currentDefenseBlock = null;
        } else if (mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock()
                        instanceof BedBlock
                && !mc.player.isShiftKeyDown()) {
            this.sneakTicks = Math.max(1, this.sneakHoldTicks.getValue().intValue());
        } else {
            RotationManager.getInstance().forceRotation(yaw, pitch);
            InteractionResult interactionResult =
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
            if (interactionResult.consumesAction()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
                BlockPlaces.onBlockPlaced(
                        blockHitResult.getBlockPos(), blockHitResult.getDirection());
                this.currentDefenseBlock = null;
            }
        }
    }

    private boolean findBed() {
        BlockPos blockPos = mc.player.blockPosition();
        Vec3 vector3 = mc.player.position();
        BlockPos blockPos2 = null;
        double closestDistance = Double.MAX_VALUE;

        BlockPos blockPos3 = blockPos.offset(-16, -16, -16);

        for (BlockPos blockPos4 : BlockPos.betweenClosed(blockPos3, blockPos.offset(16, 16, 16))) {
            BlockState blockState = mc.level.getBlockState(blockPos4);
            if (blockState.getBlock() instanceof BedBlock bedBlock
                    && Bedwars.isInBedwarsGame()
                    && TeamUtil.isOwnTeamColor(bedBlock.getColor())) {
                double distance = Vec3.atCenterOf(blockPos4).distanceToSqr(vector3);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    blockPos2 = blockPos4.immutable();
                }
            }
        }

        if (blockPos2 == null) {
            return false;
        }

        BlockState blockState2 = mc.level.getBlockState(blockPos2);
        this.bedFacing = blockState2.getValue(BedBlock.FACING);
        this.bedPos =
                blockState2.getValue(BedBlock.PART) == BedPart.FOOT
                        ? blockPos2.relative(this.bedFacing)
                        : blockPos2;
        this.bedFound = true;
        return true;
    }

    private BlockPos rotateOffset(int x, int y, int z, Direction direction) {
        if (direction == null) {
            return new BlockPos(x, y, z);
        }

        return switch (BedDefenderDirectionSwitchMap.directionOrdinalMap[direction.ordinal()]) {
            case 3 -> new BlockPos(-x, y, -z);
            case 4 -> new BlockPos(x, y, z);
            case 5 -> new BlockPos(-z, y, x);
            case 6 -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (mc.player != null && mc.level != null && mc.gameMode != null) {
                if (this.sneakTicks > 0) {
                    this.sneakTicks--;
                }

                if (!this.bedFound && !this.findBed()) {
                    this.setEnabled(false);
                } else if (!this.updatePlan()) {
                    this.setEnabled(false);
                } else if (this.currentDefenseBlock != null
                        && this.supportPos != null
                        && this.supportFace != null) {
                    this.submitAimRotation();
                    int materialSlot =
                            this.findMaterialSlot(this.currentDefenseBlock.getBlockType());
                    if (materialSlot == -1) {
                        this.currentDefenseBlock = null;
                    } else {
                        if (mc.player.getInventory().getSelectedSlot() != materialSlot) {
                            InventoryUtil.setSlot(materialSlot);
                            this.swapDelayCounter = this.delayAfterSwap.getValue().intValue();
                            if (this.swapDelayCounter > 0) {
                                return;
                            }
                        }

                        if (this.swapDelayCounter > 0) {
                            this.swapDelayCounter--;
                        } else {
                            boolean needsSneak =
                                    mc.level.getBlockState(this.supportPos).getBlock()
                                                    instanceof BedBlock
                                            || this.isAboveBed(this.placementPos);
                            if (needsSneak && this.sneakTicks == 0 && !mc.player.isShiftKeyDown()) {
                                int holdTicks = this.sneakHoldTicks.getValue().intValue();
                                if (holdTicks > 0) {
                                    this.sneakTicks = holdTicks;
                                    return;
                                }
                            }

                            if (this.sneakTicks <= 0) {
                                if (this.aimDelayCounter > 0) {
                                    this.aimDelayCounter--;
                                } else {
                                    this.placeBlock();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<String> getRequiredMaterials() {
        HashSet materials = new HashSet();

        for (DefenseBlock defenseBlock : this.getDefensePattern()) {
            materials.add(defenseBlock.getBlockType());
        }

        return materials;
    }

    private List<DefenseBlock> getDefensePattern() {
        List pattern = DefenseLayouts.createLayouts().get(this.defense.getValue().getName());
        return pattern != null ? pattern : List.of();
    }

    @Override
    public void onDisable() {
        this.sneakTicks = 0;
        this.supportPos = null;
        RotationManager.getInstance().reset();
    }

    private boolean isDefensePos(BlockPos blockPos) {

        for (DefenseBlock defenseBlock : this.getDefensePattern()) {
            if (this.getDefenseBlockPos(defenseBlock).equals(blockPos)) {
                return true;
            }
        }

        return false;
    }

    private VoxelShape getInteractionShape(BlockPos blockPos) {
        BlockState blockState = mc.level.getBlockState(blockPos);
        if (blockState.getBlock() instanceof BedBlock) {
            return blockState.getShape(mc.level, blockPos);
        }

        VoxelShape voxelShape = blockState.getCollisionShape(mc.level, blockPos);
        return voxelShape.isEmpty() ? blockState.getShape(mc.level, blockPos) : voxelShape;
    }

    private int findSlotForMaterials(Set<String> materials) {

        for (String material : materials) {
            int slot = this.findMaterialSlot(material);
            if (slot != -1) {
                return slot;
            }
        }

        return -1;
    }

    private float[] getRotationToPoint(Vec3 vector3) {
        Vec3 vector32 = this.getCameraPosition();
        double dx = vector3.x - vector32.x;
        double dy = vector3.y - vector32.y;
        double dz = vector3.z - vector32.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch =
                Mth.clamp(
                        (float) (-Math.toDegrees(Math.atan2(dy, horizontalDistance))),
                        -90.0F,
                        90.0F);
        return new float[] {yaw, pitch};
    }

    private PlacementTarget findPlacement(BlockPos blockPos) {
        PlacementTarget fallback = null;

        for (Direction direction : placeDirections) {
            BlockPos blockPos2 = blockPos.relative(direction.getOpposite());
            if (this.canClickFace(blockPos2, direction)) {
                boolean supportIsBed =
                        mc.level.getBlockState(blockPos2).getBlock() instanceof BedBlock;

                for (Vec3 vector3 : this.getAimPoints(blockPos2, direction)) {
                    float[] rotations = this.getRotationToPoint(vector3);
                    if (this.isValidPlacementHit(
                            this.traceBlock(rotations[0], rotations[1]), blockPos)) {
                        PlacementTarget target =
                                new PlacementTarget(
                                        rotations[0], rotations[1], blockPos2, direction);
                        if (!supportIsBed) {
                            return target;
                        }

                        if (fallback == null) {
                            fallback = target;
                        }
                    }
                }

                if (supportIsBed && direction == Direction.UP) {
                    float[] centerRotations =
                            this.getRotationToPoint(this.getFaceCenter(blockPos2, direction));
                    if (this.isValidPlacementHit(
                                    this.traceBlock(centerRotations[0], 90.0F), blockPos)
                            && fallback == null) {
                        fallback =
                                new PlacementTarget(
                                        centerRotations[0], 90.0F, blockPos2, direction);
                    }
                }
            }
        }

        return fallback;
    }

    private Vec3 getCameraPosition() {
        return mc.getCameraEntity() != null
                ? mc.getCameraEntity().getEyePosition(1.0F)
                : mc.player.getEyePosition(1.0F);
    }

    private BlockHitResult traceBlock(float yaw, float pitch) {
        BlockHitResult blockHitResult = RotationUtil.rayTraceBlock(4.5F, yaw, pitch);
        if (blockHitResult.getType() == Type.BLOCK) {
            return blockHitResult;
        }

        Vec3 vector3 = this.getCameraPosition();
        Vec3 vector32 = RotationUtil.getLookVectorMth(yaw, pitch);
        Vec3 vector33 = vector3.add(vector32.x * 4.5, vector32.y * 4.5, vector32.z * 4.5);
        return mc.level.clip(
                new ClipContext(
                        vector3,
                        vector33,
                        Block.OUTLINE,
                        Fluid.NONE,
                        mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player));
    }

    private boolean isAboveBed(BlockPos blockPos) {
        if (blockPos != null && this.bedPos != null) {
            for (BlockPos blockPos2 : this.getBedParts()) {
                if (blockPos.equals(blockPos2.above())) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @EventTarget
    public void onContinueAttack(ContinueAttackEvent continueAttackEvent) {
        if (this.bedFound) {
            continueAttackEvent.setCancelled(true);
        }
    }

    private void applyPlacement(PlacementTarget target) {
        this.supportPos = target.getSupportPos();
        this.supportFace = target.getFace();
        this.aimYaw = target.getYaw();
        this.aimPitch = target.getPitch();
    }

    private boolean hasPlacementSupport(BlockPos blockPos) {

        for (Direction direction : Direction.values()) {
            BlockPos blockPos2 = blockPos.relative(direction);
            if (this.isBedPart(blockPos2)) {
                return true;
            }

            if (!mc.level.getBlockState(blockPos2).isAir() && this.isDefensePos(blockPos2)) {
                return true;
            }
        }

        return false;
    }

    private void resetState() {
        this.bedFound = false;
        this.bedPos = null;
        this.bedFacing = null;
        this.placementPos = null;
        this.currentDefenseBlock = null;
        this.supportPos = null;
        this.supportFace = null;
        this.aimYaw = 0.0F;
        this.aimPitch = 0.0F;
        this.swapDelayCounter = 0;
        this.aimDelayCounter = 0;
        this.sneakTicks = 0;
    }

    private void clearTarget() {
        this.currentDefenseBlock = null;
        this.supportPos = null;
        this.supportFace = null;
        this.placementPos = null;
    }

    private void selectTarget(
            DefenseBlock defenseBlock, BlockPos blockPos, PlacementTarget target) {
        if (this.placementPos == null || !this.placementPos.equals(blockPos)) {
            this.aimDelayCounter = this.delayAfterAiming.getValue().intValue();
        }

        this.currentDefenseBlock = defenseBlock;
        this.placementPos = blockPos;
        if (target == null) {
            this.supportPos = null;
            this.supportFace = null;
        } else {
            this.applyPlacement(target);
        }
    }

    private boolean canClickFace(BlockPos blockPos, Direction direction) {
        BlockState blockState = mc.level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        } else {
            return blockState.getBlock() instanceof BedBlock && direction != Direction.UP
                    ? false
                    : !this.getInteractionShape(blockPos).isEmpty();
        }
    }

    @Override
    public void onEnable() {
        this.resetState();
        if (this.findSlotForMaterials(this.getRequiredMaterials()) == -1) {
            this.setEnabled(false);
        }
    }

    private List<Vec3> getAimPoints(BlockPos blockPos, Direction direction) {
        VoxelShape voxelShape = this.getInteractionShape(blockPos);
        if (voxelShape.isEmpty()) {
            return List.of();
        }

        AABB boundingBox = voxelShape.bounds().move(blockPos);
        return List.of(
                this.getFacePoint(boundingBox, direction, 0.5, 0.5),
                this.getFacePoint(boundingBox, direction, 0.3, 0.3),
                this.getFacePoint(boundingBox, direction, 0.3, 0.7),
                this.getFacePoint(boundingBox, direction, 0.7, 0.3),
                this.getFacePoint(boundingBox, direction, 0.7, 0.7));
    }

    private boolean isBedPart(BlockPos blockPos) {

        for (BlockPos blockPos2 : this.getBedParts()) {
            if (blockPos2.equals(blockPos)) {
                return true;
            }
        }

        return false;
    }

    private void submitAimRotation() {
        Rotation rotation =
                new Rotation(
                        this.aimYaw,
                        this.aimPitch,
                        360.0F,
                        360.0F,
                        RotationPriority.HIGH,
                        MoveFixMode.SILENT);
        rotation.setTickStepped(true);

        rotation.setTag("Bed Defender");
        RotationManager.getInstance().submit(rotation);
        RotationManager.getInstance().forceRotation(this.aimYaw, this.aimPitch);
    }

    private PlacementTarget findFallbackPlacement(BlockPos blockPos) {
        PlacementTarget fallback = null;

        for (Direction direction : placeDirections) {
            BlockPos blockPos2 = blockPos.relative(direction.getOpposite());
            if (this.canClickFace(blockPos2, direction)) {
                float[] rotations =
                        this.getRotationToPoint(this.getFaceCenter(blockPos2, direction));
                PlacementTarget target =
                        new PlacementTarget(rotations[0], rotations[1], blockPos2, direction);
                if (!(mc.level.getBlockState(blockPos2).getBlock() instanceof BedBlock)) {
                    return target;
                }

                if (fallback == null) {
                    fallback = target;
                }
            }
        }

        return fallback;
    }

    static {
        placeDirections =
                new Direction[] {
                    Direction.UP,
                    Direction.NORTH,
                    Direction.SOUTH,
                    Direction.EAST,
                    Direction.WEST,
                    Direction.DOWN
                };
    }

    private static final String MODULE_ID = "56fb53c4614495bac248464d983fdcc3";
    private static final String DELAY_AFTER_SWAP_SETTING_ID = "db217ada3c0a6fcd5ddcbdb2a9bf1ee4";
    private static final String DELAY_AFTER_AIMING_SETTING_ID =
            "aa705663a692c393d2c8d420af3251df6f8a9817e4ed60e9af593890ab6143a3";
    private static final String SNEAK_HOLD_TICKS_SETTING_ID = "6edbfeca33b19838577a1b1ec3cdc327";
    private static final String RENDER_SETTING_ID = "16db17949516d33d5746372def02cd96";
    private static final String DEFENSE_SETTING_ID = "47929e5082ba8755d4be8bc7b4b221f7";
}
