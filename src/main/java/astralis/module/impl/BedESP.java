package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.Render3DEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.util.OwnBedTracker;
import astralis.util.render.RenderUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class BedESP extends Module {
    private static Color obsidianColor = new Color(165, 80, 255, 110);
    private static Direction[] adjacentDirections = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private final ColorSetting color;
    private final BooleanSetting obsidianProtection;
    private final Set<BlockPos> visitedBedPositions;

    public BedESP() {

        super(MODULE_ID, "", "", Category.Misc);

        this.color = new ColorSetting(COLOR_SETTING_ID, Color.CYAN);

        this.obsidianProtection = new BooleanSetting(OBSIDIAN_PROTECTION_SETTING_ID, true);
        this.visitedBedPositions = new HashSet<>();
        this.addSettings(this.color, this.obsidianProtection);
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        this.visitedBedPositions.clear();

        for (BlockPos blockPos : OwnBedTracker.getPositions()) {
            if (this.visitedBedPositions.add(blockPos)) {
                BlockState blockState = mc.level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BedBlock) {
                    BlockPos blockPos2 =
                            blockPos.relative(BedBlock.getConnectedDirection(blockState));
                    this.visitedBedPositions.add(blockPos2);
                    if (this.obsidianProtection.getValue()) {
                        this.drawObsidianProtection(render3DEvent, blockPos);
                        this.drawObsidianProtection(render3DEvent, blockPos2);
                    }

                    BlockState blockState2 = mc.level.getBlockState(blockPos2);
                    VoxelShape voxelShape = blockState.getShape(mc.level, blockPos);
                    VoxelShape voxelShape2 = blockState2.getShape(mc.level, blockPos2);
                    if (!voxelShape.isEmpty() || !voxelShape2.isEmpty()) {
                        AABB boundingBox =
                                voxelShape.isEmpty()
                                        ? voxelShape2.bounds().move(blockPos2)
                                        : (voxelShape2.isEmpty()
                                                ? voxelShape.bounds().move(blockPos)
                                                : voxelShape
                                                        .bounds()
                                                        .move(blockPos)
                                                        .minmax(
                                                                voxelShape2
                                                                        .bounds()
                                                                        .move(blockPos2)));
                        RenderUtil.drawBox(
                                render3DEvent.getPoseStack(),
                                boundingBox.inflate(0.002),
                                this.color.getValue());
                    }
                }
            }
        }
    }

    private void drawObsidianProtection(Render3DEvent render3DEvent, BlockPos blockPos) {

        for (Direction direction : adjacentDirections) {
            BlockPos blockPos2 = blockPos.relative(direction);
            if (mc.level.getBlockState(blockPos2).getBlock() == Blocks.OBSIDIAN) {
                RenderUtil.drawBox(
                        render3DEvent.getPoseStack(),
                        new AABB(blockPos2).inflate(0.002),
                        obsidianColor);
            }
        }
    }

    private static final String MODULE_ID = "e97ae31b21e1c88d5263fa5e6368c1a6";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String OBSIDIAN_PROTECTION_SETTING_ID =
            "7154142b1462c893d0d170e24998f087637299a6db656549a7fd2d4b79d5ab94";
}
