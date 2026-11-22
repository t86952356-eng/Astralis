package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.Render3DEvent;
import astralis.module.Module;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.render.ColorUtil;
import astralis.util.render.RenderUtil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer.Draw;
import net.minecraft.client.renderer.StagedVertexBuffer.ExecuteInfo;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class BlockPlaces extends Module {
    private final NumberSetting duration;
    private final ModeSetting mode;
    private final NumberSetting outlineWidth;
    private final ColorSetting colors;
    private final ConcurrentHashMap<BlockPos, Long> placedBlocks;

    public BlockPlaces() {

        super(MODULE_ID, "", "", Category.Misc);

        this.duration = new NumberSetting(DURATION_SETTING_ID, 1000.0F, 100.0F, 5000.0F, 50.0F);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modeNames = new String[2];

        modeNames[0] = "Fill";

        modeNames[1] = "Outline";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.mode = modeSetting2.setDefault("Outline");

        this.outlineWidth = new NumberSetting(OUTLINE_WIDTH_SETTING_ID, 1.0F, 1.0F, 10.0F, 1.0F);

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);
        this.placedBlocks = new ConcurrentHashMap<>();
        this.outlineWidth.hideIf(
                () -> {
                    return !this.mode.is("Outline");
                });
        ModeSetting modeSetting3 = this.colors.getColorMode();

        modeSetting3.setDefault("Client");
        this.addSettings(this.duration, this.mode, this.outlineWidth, this.colors);
    }

    public static void onBlockPlaced(BlockPos blockPos, Direction direction) {
        BlockPlaces blockPlaces =
                Astralis.getInstance().getModuleManager().getModule(BlockPlaces.class);
        if (blockPlaces != null && blockPlaces.isEnabled()) {
            blockPlaces.trackPlacement(blockPos.relative(direction));
        }
    }

    public void trackPlacement(BlockPos blockPos) {
        if (mc.level != null) {
            this.placedBlocks.put(blockPos, System.currentTimeMillis());
        }
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.SEND) {
            if (packetEvent.getPacket()
                    instanceof ServerboundUseItemOnPacket serverboundUseItemOnPacket) {
                if (mc.player != null && mc.level != null) {
                    BlockPos blockPos = serverboundUseItemOnPacket.getHitResult().getBlockPos();
                    BlockPos blockPos2 =
                            blockPos.relative(
                                    serverboundUseItemOnPacket.getHitResult().getDirection());
                    this.placedBlocks.put(blockPos2, System.currentTimeMillis());
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        if (mc.player != null && mc.level != null) {
            long currentTime = System.currentTimeMillis();
            long durationMs = (long) this.duration.getValue().floatValue();
            Iterator iterator = this.placedBlocks.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                long elapsed = currentTime - (Long) entry.getValue();
                if (elapsed >= durationMs) {
                    iterator.remove();
                } else {
                    float fade = 1.0F - (float) elapsed / (float) durationMs;

                    if (this.mode.is("Fill")) {
                        this.drawBlockFill(
                                render3DEvent.getPoseStack(), (BlockPos) entry.getKey(), fade);
                    } else {
                        this.drawBlockOutline(
                                render3DEvent.getPoseStack(), (BlockPos) entry.getKey(), fade);
                    }
                }
            }
        }
    }

    private static int scaleAlpha(int color, float scale) {

        int alpha = color >> 24;

        int scaledAlpha = Math.max(0, Math.min(255, Math.round((alpha & 0xFF) * scale)));

        int rgb = color & 16777215;
        return rgb | scaledAlpha << 24;
    }

    @Override
    public void onDisable() {
        this.placedBlocks.clear();
    }

    private void drawBlockFill(PoseStack poseStack, BlockPos blockPos, float fade) {
        if (mc.level != null) {
            BlockState blockState = mc.level.getBlockState(blockPos);
            VoxelShape voxelShape = blockState.getShape(mc.level, blockPos);
            AABB boundingBox =
                    (voxelShape.isEmpty() ? new AABB(blockPos) : voxelShape.bounds().move(blockPos))
                            .inflate(0.002);
            Color[] blendColors = this.colors.getColors();
            int blendedColor =
                    ColorUtil.waveBlend(
                            blockPos.getX(), blendColors[0].getRGB(), blendColors[1].getRGB());

            int green;

            int shiftedAlpha = blendedColor >> 24;

            int scaledAlpha = Math.max(0, Math.min(255, Math.round((shiftedAlpha & 0xFF) * fade)));

            int shiftedRed = blendedColor >> 16;

            int red = shiftedRed & 0xFF;

            green = blendedColor >> 8;

            green &= 255;

            Color color = new Color(red, green, blendedColor & 0xFF, scaledAlpha);
            RenderUtil.drawBox(poseStack, boundingBox, color);
        }
    }

    private void drawBlockOutline(PoseStack poseStack, BlockPos blockPos, float fade) {
        if (mc.level != null) {
            BlockState blockState = mc.level.getBlockState(blockPos);
            VoxelShape voxelShape = blockState.getShape(mc.level, blockPos);
            if (!voxelShape.isEmpty()) {
                Color[] blendColors = this.colors.getColors();
                int edgeColor =
                        scaleAlpha(
                                ColorUtil.waveBlend(
                                        blockPos.getX(),
                                        blendColors[0].getRGB(),
                                        blendColors[1].getRGB()),
                                fade);
                Vec3 vector3 = mc.gameRenderer.mainCamera().position();
                StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
                RenderType renderType = RenderTypes.lines();
                Draw draw =
                        stagedVertexBuffer.appendDraw(
                                renderType.format(), renderType.primitiveTopology());
                VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
                poseStack.pushPose();
                poseStack.translate(
                        blockPos.getX() - vector3.x,
                        blockPos.getY() - vector3.y,
                        blockPos.getZ() - vector3.z);
                Pose pose = poseStack.last();
                Vector3f vector3f = new Vector3f();
                voxelShape.forAllEdges(
                        (x1, y1, z1, x2, y2, z2) -> {
                            vector3f.set((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1))
                                    .normalize();
                            vertexConsumer
                                    .addVertex(pose, (float) x1, (float) y1, (float) z1)
                                    .setColor(edgeColor)
                                    .setNormal(pose, vector3f)
                                    .setLineWidth(this.outlineWidth.getValue());
                            vertexConsumer
                                    .addVertex(pose, (float) x2, (float) y2, (float) z2)
                                    .setColor(edgeColor)
                                    .setNormal(pose, vector3f)
                                    .setLineWidth(this.outlineWidth.getValue());
                        });
                stagedVertexBuffer.upload();
                PreparedRenderType preparedRenderType = renderType.prepare();
                ExecuteInfo executeInfo = stagedVertexBuffer.getExecuteInfo(draw);
                if (executeInfo != null) {
                    preparedRenderType.drawFromBuffer(executeInfo);
                }

                stagedVertexBuffer.endDraw();
                poseStack.popPose();
            }
        }
    }

    private static final String MODULE_ID = "7087c60c8c60ee84af3d651b16cdeb3b";
    private static final String DURATION_SETTING_ID = "931fabbd28f64b0ae4479a5942eb13fb";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String OUTLINE_WIDTH_SETTING_ID = "b93cac17b61546079510f14a96debf74";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
