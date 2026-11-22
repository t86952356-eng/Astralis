package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.BlockEntityRenderEvent;
import astralis.event.impl.Render3DEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.util.render.RenderUtil;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

public class StorageESP extends Module {
    private final BooleanSetting chests;
    private final ColorSetting chestColor;
    private final BooleanSetting shulkerBox;
    private final ColorSetting shulkerBoxColor;
    private final List<BlockEntity> pendingBlockEntities;

    public StorageESP() {

        super(MODULE_ID, "", "", Category.Misc);

        this.chests = new BooleanSetting(CHESTS_SETTING_ID, true);

        this.chestColor = new ColorSetting(CHEST_COLOR_SETTING_ID, Color.BLUE);

        this.shulkerBox = new BooleanSetting(SHULKER_BOX_SETTING_ID, true);

        this.shulkerBoxColor = new ColorSetting(SHULKER_BOX_COLOR_SETTING_ID, Color.PINK);
        this.pendingBlockEntities = new java.util.ArrayList<>();
        this.shulkerBoxColor.hideIf(
                () -> {
                    return !this.shulkerBox.getValue();
                });
        this.chestColor.hideIf(
                () -> {
                    return !this.chests.getValue();
                });
        this.addSettings(this.chests, this.chestColor, this.shulkerBox, this.shulkerBoxColor);
    }

    private AABB getStorageBounds(BlockEntity blockEntity) {
        BlockState blockState = mc.level.getBlockState(blockEntity.getBlockPos());
        VoxelShape voxelShape = blockState.getShape(mc.level, blockEntity.getBlockPos());
        if (voxelShape.isEmpty()) {
            return null;
        }

        if (!blockState.hasProperty(ChestBlock.TYPE)
                || blockState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return voxelShape.bounds().move(blockEntity.getBlockPos());
        }

        if (blockState.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
            return null;
        }

        BlockPos blockPos = ChestBlock.getConnectedBlockPos(blockEntity.getBlockPos(), blockState);
        VoxelShape voxelShape2 = mc.level.getBlockState(blockPos).getShape(mc.level, blockPos);
        return voxelShape2.isEmpty()
                ? null
                : voxelShape
                        .bounds()
                        .move(blockEntity.getBlockPos())
                        .minmax(voxelShape2.bounds().move(blockPos));
    }

    @EventTarget
    public void onRender3d(Render3DEvent render3DEvent) {
        PoseStack poseStack = render3DEvent.getPoseStack();
        this.pendingBlockEntities.forEach(
                blockEntity -> {
                    BlockEntity blockEntity2 = blockEntity;
                    byte caseStart = 0;

                    ColorSetting colorSetting =
                            switch (typeSwitch0(blockEntity2, caseStart)) {
                                case 0 -> {
                                    ChestBlockEntity chestBlockEntity =
                                            (ChestBlockEntity) blockEntity2;
                                    yield this.chestColor;
                                }
                                case 1 -> {
                                    ShulkerBoxBlockEntity shulkerBoxBlockEntity =
                                            (ShulkerBoxBlockEntity) blockEntity2;
                                    yield this.shulkerBoxColor;
                                }
                                default -> null;
                            };
                    AABB boundingBox;
                    if (colorSetting != null
                            && (boundingBox = this.getStorageBounds(blockEntity)) != null) {
                        RenderUtil.drawSettingBox(poseStack, boundingBox, colorSetting);
                    }
                });
        this.pendingBlockEntities.clear();
    }

    @EventTarget
    public void onBlockEntityRender(BlockEntityRenderEvent event) {
        BlockEntity blockEntity = event.getBlockEntity();
        if (blockEntity instanceof ChestBlockEntity && this.chests.getValue()
                || blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBox.getValue()) {
            this.pendingBlockEntities.add(blockEntity);
        }
    }

    private static int typeSwitch0(BlockEntity blockEntity, int startIndex) {
        Objects.checkIndex(startIndex, 3);
        if (blockEntity == null) {
            return -1;
        } else if (startIndex <= 0 && blockEntity instanceof ChestBlockEntity) {
            return 0;
        } else {
            return startIndex <= 1 && blockEntity instanceof ShulkerBoxBlockEntity ? 1 : 2;
        }
    }

    private static final String MODULE_ID = "8d7a36811538a85644158e9f01abc0c7";
    private static final String CHESTS_SETTING_ID = "de085432cede77271cd675a5e562befc";
    private static final String CHEST_COLOR_SETTING_ID = "2c35c12f54e9b566ba4c1e0729c6f20c";
    private static final String SHULKER_BOX_SETTING_ID = "eab0449626a033893bba94a9eea5abdc";
    private static final String SHULKER_BOX_COLOR_SETTING_ID =
            "f642b4b89bcbc2d5d9af3bbccbe75f19c8da9162bde5b1f4280ff549af70a03d";
}
