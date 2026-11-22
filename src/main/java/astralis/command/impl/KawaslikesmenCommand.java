package astralis.command.impl;

import astralis.Astralis;
import astralis.command.BaseCommand;
import astralis.event.EventTarget;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.util.render.RenderUtil;
import astralis.util.render.SparkParticle;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer.Draw;
import net.minecraft.client.renderer.StagedVertexBuffer.ExecuteInfo;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class KawaslikesmenCommand extends BaseCommand implements InstanceAccess {
    private static Identifier primaryTexture;
    private static FloatingImage[] floatingImages;
    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final float NEARBY_RANGE = 5.0F;
    private static final float CLOSE_RANGE = 1.95F;
    private static boolean active;
    private static long startMillis;
    private final List<SparkParticle> sparkles;
    private float nearbyProgress;
    private float closeProgress;
    private long lastFrameMillis;
    private static String[] phrases;

    public KawaslikesmenCommand() {

        super("kawaslikesmen:3", "Definitely a normal command.");
        this.sparkles = new ArrayList<>();
        Astralis.getInstance().getEventBus().register(this);
    }

    private void renderWand(
            PoseStack poseStack,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            float length,
            float thickness) {
        poseStack.pushPose();
        Vec3 vector3 = mc.gameRenderer.mainCamera().position();
        poseStack.translate(x - vector3.x, y - vector3.y, z - vector3.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.translate(0.0F, 0.72F, -0.12F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        float handleSize = 0.055F + this.nearbyProgress * 0.012F;
        float minX = -handleSize * 2.15F;
        float minY = -handleSize;
        float minZ = -handleSize * 0.7F;
        float maxZ = handleSize * 0.7F;

        this.drawBox(poseStack, minX, minY, minZ, -0.004F, handleSize, maxZ, -220159832);
        minY = -handleSize;
        minZ = -handleSize * 0.7F;
        float maxX = handleSize * 2.15F;
        maxZ = handleSize * 0.7F;

        this.drawBox(poseStack, 0.004F, minY, minZ, maxX, handleSize, maxZ, -220159832);
        minX = -thickness;
        minY = -thickness;

        this.drawBox(poseStack, minX, minY, 0.0F, thickness, thickness, length, -219633488);
        float glowRadius = thickness + 0.018F;
        minX = -glowRadius;
        minY = -glowRadius;
        maxZ = length + 0.09F + this.nearbyProgress * 0.04F;

        this.drawBox(poseStack, minX, minY, length, glowRadius, glowRadius, maxZ, -170628488);
        poseStack.popPose();
    }

    private void renderBackgroundHearts(GuiGraphicsExtractor guiGraphicsExtractor, float time) {

        float screenWidth = guiGraphicsExtractor.guiWidth();
        float screenHeight = guiGraphicsExtractor.guiHeight();

        int goldColor = withAlpha(-11930, 160);

        int cyanColor = withAlpha(-7607811, 170);

        int pinkColor = withAlpha(-41315, 180);
        int i = 0;

        while (true) {

            if (i >= 10) {
                float baseX =
                        this.pingPong(time * 70.0F, Math.max(0.0F, screenWidth - 120.0F), 0.2F)
                                + 20.0F;
                this.drawHeart(guiGraphicsExtractor, baseX, screenHeight - 28.0F, 1.1F, pinkColor);
                this.drawHeart(
                        guiGraphicsExtractor, baseX + 16.0F, screenHeight - 22.0F, 0.8F, goldColor);
                this.drawHeart(
                        guiGraphicsExtractor, baseX + 32.0F, screenHeight - 28.0F, 1.1F, cyanColor);
                return;
            }

            float heartY = (time * (48.0F + i * 7.0F) + i * 37.0F) % (screenHeight + 40.0F) - 20.0F;
            float heartX =
                    16.0F
                            + i % 5 * ((screenWidth - 32.0F) / 4.0F)
                            + Mth.sin(time * 2.0F + i) * 8.0F;
            this.drawHeart(
                    guiGraphicsExtractor,
                    heartX,
                    heartY,
                    0.55F + i % 3 * 0.15F,
                    i % 3 == 0 ? goldColor : (i % 3 == 1 ? cyanColor : pinkColor));
            i++;
        }
    }

    private Vec3 getInterpolatedPosition(Player player2, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, player2.xOld, player2.getX()),
                Mth.lerp(partialTick, player2.yOld, player2.getY()),
                Mth.lerp(partialTick, player2.zOld, player2.getZ()));
    }

    private void renderOrbitingCubes(PoseStack poseStack, PlayerPose pose, long timeMillis) {
        float phase = (float) timeMillis / 220.0F;
        int i = 0;

        while (true) {

            if (i >= 6) {
                return;
            }

            float angle = phase + i * (float) (Math.PI * 2) / 6.0F;
            float radius = 0.85F + 0.08F * Mth.sin(phase * 2.0F + i);
            float offsetX = Mth.cos(angle) * radius;
            float offsetZ = Mth.sin(angle) * radius;
            float offsetY = 0.35F + 0.12F * Mth.sin(phase * 3.0F + i);
            poseStack.pushPose();
            Vec3 vector3 = mc.gameRenderer.mainCamera().position();
            poseStack.translate(
                    pose.x() - vector3.x + offsetX,
                    pose.y() - vector3.y + offsetY,
                    pose.z() - vector3.z + offsetZ);
            float halfSize = 0.045F + 0.01F * Mth.sin(phase * 4.0F + i);
            float minX = -halfSize;
            float minY = -halfSize;
            float minZ = -halfSize;
            short cubeColor;
            if (i % 2 == 0) {

                cubeColor = -8054;
            } else {

                cubeColor = -11930;
            }

            this.drawBox(poseStack, minX, minY, minZ, halfSize, halfSize, halfSize, cubeColor);
            poseStack.popPose();
            i++;
        }
    }

    private float pingPong(float travel, float range, float phaseOffset) {
        if (range <= 0.0F) {
            return 0.0F;
        }

        float wrapped = (travel + phaseOffset * range * 2.0F) % (range * 2.0F);
        return wrapped <= range ? wrapped : range * 2.0F - wrapped;
    }

    private static Identifier createTextureId(String path) {

        return Identifier.fromNamespaceAndPath("astralis-recode", path);
    }

    private void spawnSparkles(Vec3 vector3, Vec3 vector32, float closeAmount, float deltaTime) {
        int count =
                Math.max(1, Math.round((4.0F + this.nearbyProgress * 6.0F) * deltaTime * 20.0F));
        Vec3 vector33 = vector32.subtract(vector3);
        if (!(vector33.lengthSqr() < 1.0E-6)) {
            Vec3 vector34 = vector33.normalize();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            boolean converging = closeAmount > 0.35F;

            for (int i = 0; i < count; i++) {
                int currentCount = this.sparkles.size();

                if (currentCount >= 96) {
                    break;
                }

                double speed =
                        converging
                                ? 0.18 + random.nextDouble() * 0.28
                                : 0.85 + random.nextDouble() * 1.15;
                this.sparkles.add(
                        new SparkParticle(
                                vector3.x + (random.nextDouble() - 0.5) * 0.03,
                                vector3.y + (random.nextDouble() - 0.5) * 0.02,
                                vector3.z + (random.nextDouble() - 0.5) * 0.03,
                                vector34.x * speed * (converging ? -0.15 : 1.0)
                                        + (random.nextDouble() - 0.5) * (converging ? 0.08 : 0.22),
                                converging
                                        ? -0.08 - random.nextDouble() * 0.18
                                        : vector34.y * speed * 0.28
                                                + 0.12
                                                + random.nextDouble() * 0.18,
                                vector34.z * speed * (converging ? -0.15 : 1.0)
                                        + (random.nextDouble() - 0.5) * (converging ? 0.08 : 0.22),
                                0.38F + random.nextFloat() * 0.32F));
            }
        }
    }

    public static boolean isAffectedPlayer(int entityId) {
        return active && mc.player != null && mc.level != null && entityId != mc.player.getId()
                ? mc.level.getEntity(entityId) instanceof Player player2
                        && player2.isAlive()
                        && !player2.isSpectator()
                        && mc.player.distanceToSqr(player2) <= 25.0
                : false;
    }

    private void renderTopEmblem(GuiGraphicsExtractor guiGraphicsExtractor, float time) {

        float pulse = 0.75F + 0.25F * Mth.sin(time * 3.7F);

        int paleGoldColor = withAlpha(-8054, 180 + Math.round(60.0F * pulse));

        int goldColor = withAlpha(-11930, 220);
        int i = 0;

        while (true) {

            if (i >= 8) {
                guiGraphicsExtractor.pose().pushMatrix();
                guiGraphicsExtractor
                        .pose()
                        .translate(
                                getScreenCenter(guiGraphicsExtractor, true),
                                26.0F + Mth.sin(time * 2.0F) * 3.0F);
                guiGraphicsExtractor.pose().scale(pulse, pulse);

                guiGraphicsExtractor.fill(-10, -2, 10, 2, goldColor);

                guiGraphicsExtractor.fill(-2, -10, 2, 10, goldColor);

                guiGraphicsExtractor.fill(-7, -7, 7, 7, paleGoldColor);
                guiGraphicsExtractor.pose().popMatrix();
                return;
            }

            float angle = i * (float) (Math.PI * 2) / 8.0F + time * 0.9F;
            float radius = 54.0F + Mth.sin(time * 2.2F + i) * 6.0F;
            this.drawHeart(
                    guiGraphicsExtractor,
                    getScreenCenter(guiGraphicsExtractor, true) + Mth.cos(angle) * radius,
                    28.0F + Mth.sin(time * 1.4F) * 4.0F,
                    0.7F + pulse * 0.25F,
                    i % 2 == 0 ? paleGoldColor : goldColor);
            i++;
        }
    }

    @EventTarget
    public void onVanillaHudRender(VanillaHudRenderEvent event) {

        if (active) {
            GuiGraphicsExtractor guiGraphicsExtractor = event.getGraphics();
            long elapsedMillis = Math.max(0L, Util.getMillis() - startMillis);

            float time = (float) elapsedMillis / 1000.0F;
            float animatedValue = time * 92.0F;
            int screenExtent = guiGraphicsExtractor.guiWidth();
            int heartColor;

            float centerX =
                    89.0F
                            + this.pingPong(
                                    animatedValue,
                                    Math.max(0.0F, screenExtent - 158 - 20.0F),
                                    0.0F);
            animatedValue = time * 61.0F;
            screenExtent = guiGraphicsExtractor.guiHeight();

            float centerY =
                    43.5F
                            + this.pingPong(
                                    animatedValue,
                                    Math.max(0.0F, screenExtent - 67 - 20.0F),
                                    0.35F);
            this.renderBursts(guiGraphicsExtractor, centerX, centerY, elapsedMillis);
            int i = 0;

            while (true) {
                if (i >= 12) {
                    this.renderFloatingImages(guiGraphicsExtractor, time);
                    animatedValue = centerX - 8.0F;
                    float cornerHeartY = centerY - 33.5F - 22.0F;

                    this.drawHeart(guiGraphicsExtractor, animatedValue, cornerHeartY, 1.2F, -59580);
                    animatedValue = centerX + 79.0F + 22.0F;
                    cornerHeartY = centerY + 33.5F + 15.0F;

                    this.drawHeart(guiGraphicsExtractor, animatedValue, cornerHeartY, 0.9F, -30028);
                    this.renderTextOverlay(guiGraphicsExtractor, time);
                    this.renderTopEmblem(guiGraphicsExtractor, time);
                    this.renderBackgroundHearts(guiGraphicsExtractor, time);
                    return;
                }

                float angle = i * (float) (Math.PI * 2) / 12.0F + time * 0.55F;
                float radiusX = 111.0F + Mth.sin(time * 2.0F + i) * 4.0F;
                float radiusY = 59.5F + Mth.cos(time * 1.7F + i) * 4.0F;
                float scale = 0.75F + Mth.sin(time * 4.0F + i * 0.8F) * 0.15F;
                animatedValue = centerX + Mth.cos(angle) * radiusX;
                float orbitHeartY = centerY + Mth.sin(angle) * radiusY;
                if (i % 3 == 0) {

                    heartColor = -36946;
                } else {

                    heartColor = -52897;
                }

                this.drawHeart(guiGraphicsExtractor, animatedValue, orbitHeartY, scale, heartColor);
                i++;
            }
        }
    }

    private void drawBox(
            PoseStack poseStack,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            int color) {
        Matrix4f matrix4f = poseStack.last().pose();
        StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
        RenderType renderType = RenderUtil.filledNoDepthRenderType;
        Draw draw =
                stagedVertexBuffer.appendDraw(renderType.format(), renderType.primitiveTopology());
        VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
        addQuad(
                matrix4f,
                vertexConsumer,
                color,
                minX,
                minY,
                maxZ,
                maxX,
                minY,
                maxZ,
                maxX,
                minY,
                minZ,
                minX,
                minY,
                minZ);
        addQuad(
                matrix4f,
                vertexConsumer,
                color,
                minX,
                maxY,
                minZ,
                maxX,
                maxY,
                minZ,
                maxX,
                maxY,
                maxZ,
                minX,
                maxY,
                maxZ);
        addQuad(
                matrix4f,
                vertexConsumer,
                color,
                minX,
                minY,
                minZ,
                minX,
                minY,
                maxZ,
                minX,
                maxY,
                maxZ,
                minX,
                maxY,
                minZ);
        addQuad(
                matrix4f,
                vertexConsumer,
                color,
                maxX,
                minY,
                maxZ,
                maxX,
                minY,
                minZ,
                maxX,
                maxY,
                minZ,
                maxX,
                maxY,
                maxZ);
        addQuad(
                matrix4f,
                vertexConsumer,
                color,
                maxX,
                minY,
                minZ,
                minX,
                minY,
                minZ,
                minX,
                maxY,
                minZ,
                maxX,
                maxY,
                minZ);
        addQuad(
                matrix4f,
                vertexConsumer,
                color,
                minX,
                minY,
                maxZ,
                maxX,
                minY,
                maxZ,
                maxX,
                maxY,
                maxZ,
                minX,
                maxY,
                maxZ);
        stagedVertexBuffer.upload();
        PreparedRenderType preparedRenderType = renderType.prepare();
        ExecuteInfo executeInfo = stagedVertexBuffer.getExecuteInfo(draw);
        if (executeInfo != null) {
            preparedRenderType.drawFromBuffer(executeInfo);
        }

        stagedVertexBuffer.endDraw();
    }

    private static float getScreenCenter(
            GuiGraphicsExtractor guiGraphicsExtractor, boolean horizontal) {
        return horizontal
                ? guiGraphicsExtractor.guiWidth() / 2.0F
                : guiGraphicsExtractor.guiHeight() / 2.0F;
    }

    private Vec3 getTargetMarkerPosition(Player player2, PlayerPose pose, float partialTick) {
        Vec3 vector3 = this.getInterpolatedPosition(player2, partialTick);
        double directionX = pose.x() - vector3.x;
        double directionZ = pose.z() - vector3.z;
        double horizontalDistance = Math.hypot(directionX, directionZ);
        if (horizontalDistance > 1.0E-4) {
            directionX /= horizontalDistance;
            directionZ /= horizontalDistance;
        } else {
            directionX = 0.0;
            directionZ = 1.0;
        }

        return new Vec3(
                vector3.x + directionX * 0.16, vector3.y + 0.78, vector3.z + directionZ * 0.16);
    }

    private Player findNearestPlayer() {
        Player player2 = null;
        double nearestDistanceSq = 25.0;

        for (Player player3 : mc.level.players()) {
            if (player3 != mc.player && player3.isAlive() && !player3.isSpectator()) {
                double distanceSq = mc.player.distanceToSqr(player3);
                if (!(distanceSq > nearestDistanceSq)) {
                    player2 = player3;
                    nearestDistanceSq = distanceSq;
                }
            }
        }

        return player2;
    }

    private static void addQuad(
            Matrix4f matrix4f,
            VertexConsumer vertexConsumer,
            int color,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4) {
        vertexConsumer.addVertex(matrix4f, x1, y1, z1).setColor(color);
        vertexConsumer.addVertex(matrix4f, x2, y2, z2).setColor(color);
        vertexConsumer.addVertex(matrix4f, x3, y3, z3).setColor(color);
        vertexConsumer.addVertex(matrix4f, x4, y4, z4).setColor(color);
    }

    private void renderFloatingImages(GuiGraphicsExtractor guiGraphicsExtractor, float time) {
        float screenWidth = guiGraphicsExtractor.guiWidth();
        float screenHeight = guiGraphicsExtractor.guiHeight();

        for (int i = 0; i < floatingImages.length; i++) {
            FloatingImage image = floatingImages[i];
            float centerX =
                    8.0F
                            + image.width() / 2.0F
                            + this.pingPong(
                                    time * image.speedX(),
                                    Math.max(0.0F, screenWidth - image.width() - 16.0F),
                                    image.phase());
            float centerY =
                    8.0F
                            + image.height() / 2.0F
                            + this.pingPong(
                                    time * image.speedY(),
                                    Math.max(0.0F, screenHeight - image.height() - 16.0F),
                                    image.phase() + 0.31F);
            guiGraphicsExtractor.pose().pushMatrix();
            guiGraphicsExtractor.pose().translate(centerX, centerY);
            guiGraphicsExtractor
                    .pose()
                    .rotate(time * (0.35F + i * 0.11F) * (i % 2 == 0 ? 1.0F : -1.0F));
            guiGraphicsExtractor.pose().translate(-image.width() / 2.0F, -image.height() / 2.0F);
            guiGraphicsExtractor.blit(
                    RenderPipelines.GUI_TEXTURED,
                    image.texture(),
                    0,
                    0,
                    0.0F,
                    0.0F,
                    image.width(),
                    image.height(),
                    image.textureWidth(),
                    image.textureHeight(),
                    image.textureWidth(),
                    image.textureHeight());
            guiGraphicsExtractor.pose().popMatrix();
        }
    }

    private static float calculateYaw(double deltaX, double deltaZ) {
        return (float) (Mth.atan2(-deltaX, deltaZ) * (180.0 / Math.PI));
    }

    private void renderBursts(
            GuiGraphicsExtractor guiGraphicsExtractor,
            float centerX,
            float centerY,
            long elapsedMillis) {

        long cycleMillis = elapsedMillis % 2600L;
        float firstBurstX = centerX - 112.0F;
        float burstY = centerY - 72.0F;

        this.drawBurst(guiGraphicsExtractor, firstBurstX, burstY, cycleMillis, 100L, -41315);
        float secondBurstX = centerX + 116.0F;
        burstY = centerY + 62.0F;

        this.drawBurst(guiGraphicsExtractor, secondBurstX, burstY, cycleMillis, 1300L, -11930);
        float thirdBurstX = centerX + 130.0F;
        burstY = centerY - 75.0F;

        this.drawBurst(guiGraphicsExtractor, thirdBurstX, burstY, cycleMillis, 700L, -7607811);
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        if (active && mc.player != null && mc.level != null) {
            long nowMillis = Util.getMillis();
            float deltaSeconds =
                    this.lastFrameMillis == 0L
                            ? 0.016F
                            : Mth.clamp(
                                    (float) (nowMillis - this.lastFrameMillis) / 1000.0F,
                                    0.0F,
                                    0.05F);
            this.lastFrameMillis = nowMillis;
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Player player2 = this.findNearestPlayer();
            boolean hasTarget = player2 != null;
            boolean inCloseRange =
                    player2 != null && mc.player.distanceToSqr(player2) <= 3.8025002F;
            this.nearbyProgress =
                    Mth.clamp(
                            this.nearbyProgress + (hasTarget ? 2.4F : -1.6F) * deltaSeconds,
                            0.0F,
                            1.0F);
            this.closeProgress =
                    Mth.clamp(
                            this.closeProgress
                                    + (inCloseRange && this.nearbyProgress > 0.45F ? 3.6F : -2.8F)
                                            * deltaSeconds,
                            0.0F,
                            1.0F);
            PlayerPose pose = this.getPlayerPose(partialTick);
            if (hasTarget && this.nearbyProgress > 0.65F) {
                this.renderOrbitingCubes(render3DEvent.getPoseStack(), pose, nowMillis);
            }

            float wandLength = 0.18F + this.nearbyProgress * 0.42F;
            float wandThickness = 0.045F + this.nearbyProgress * 0.018F;
            float wandYaw = pose.yaw();
            float wandPitch = this.nearbyProgress * 78.0F;
            Vec3 vector3 = this.getWandTipPosition(pose, wandLength, wandPitch);
            Vec3 vector32 =
                    player2 == null
                            ? null
                            : this.getTargetMarkerPosition(player2, pose, partialTick);
            float pulse = 0.55F + 0.45F * Mth.sin((float) nowMillis / 88.0F);
            if (vector32 != null) {
                this.renderTargetMarker(
                        render3DEvent.getPoseStack(),
                        vector32,
                        calculateYaw(pose.x() - vector32.x, pose.z() - vector32.z),
                        Math.max(0.35F, this.closeProgress) * (0.85F + pulse * 0.2F));
            }

            if (this.closeProgress > 0.0F && vector32 != null) {
                double yawRadians = Math.toRadians(pose.yaw());
                Vec3 vector33 =
                        new Vec3(
                                pose.x() + Math.sin(yawRadians) * 0.12F,
                                pose.y() + 0.72F,
                                pose.z() - Math.cos(yawRadians) * 0.12F);
                Vec3 vector34 = vector32.subtract(vector33);
                if (vector34.lengthSqr() > 1.0E-6) {
                    Vec3 vector35 = vector32.add(vector34.normalize().scale(0.28));
                    Vec3 vector36 = vector35.subtract(vector33);
                    float horizontalDistance = (float) Math.hypot(vector36.x, vector36.z);
                    float targetYaw =
                            (float) (Mth.atan2(-vector36.x, vector36.z) * (180.0 / Math.PI));
                    float targetPitch =
                            (float) (Mth.atan2(vector36.y, horizontalDistance) * (180.0 / Math.PI));
                    float targetLength = (float) vector36.length() + pulse * 0.18F;
                    wandYaw = Mth.rotLerp(this.closeProgress, wandYaw, targetYaw);
                    wandPitch = Mth.lerp(this.closeProgress, wandPitch, targetPitch);
                    wandLength = Mth.lerp(this.closeProgress, wandLength, targetLength);
                    wandThickness += this.closeProgress * 0.014F;
                    vector3 =
                            this.getWandTipPosition(
                                    new PlayerPose(pose.x(), pose.y(), pose.z(), wandYaw),
                                    wandLength,
                                    wandPitch);
                }
            }

            float scalePulse =
                    1.0F + Mth.sin((float) nowMillis / 110.0F) * this.nearbyProgress * 0.06F;
            this.renderWand(
                    render3DEvent.getPoseStack(),
                    pose.x(),
                    pose.y(),
                    pose.z(),
                    wandYaw,
                    wandPitch,
                    wandLength,
                    wandThickness * scalePulse);
            if (hasTarget && this.nearbyProgress > 0.55F) {
                this.spawnSparkles(
                        vector3,
                        this.closeProgress > 0.35F && vector32 != null
                                ? vector32
                                : player2.getEyePosition(partialTick),
                        this.closeProgress,
                        deltaSeconds);
            }

            this.renderSparkles(render3DEvent.getPoseStack(), deltaSeconds);
        }
    }

    private static int withAlpha(int color, int alpha) {

        int clampedAlpha = Math.max(0, Math.min(255, alpha));

        int alphaBits = clampedAlpha << 24;

        return alphaBits | color & 16777215;
    }

    private void drawHeart(
            GuiGraphicsExtractor guiGraphicsExtractor, float x, float y, float scale, int color) {
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor.pose().translate(x - 4.0F * scale, y - 4.0F * scale);
        guiGraphicsExtractor.pose().scale(scale, scale);
        guiGraphicsExtractor.fill(1, 0, 3, 2, color);

        guiGraphicsExtractor.fill(5, 0, 7, 2, color);

        guiGraphicsExtractor.fill(0, 1, 8, 4, color);

        guiGraphicsExtractor.fill(1, 4, 7, 5, color);

        guiGraphicsExtractor.fill(2, 5, 6, 6, color);

        guiGraphicsExtractor.fill(3, 6, 5, 7, color);
        guiGraphicsExtractor.pose().popMatrix();
    }

    private PlayerPose getPlayerPose(float partialTick) {
        return new PlayerPose(
                Mth.lerp(partialTick, mc.player.xOld, mc.player.getX()),
                Mth.lerp(partialTick, mc.player.yOld, mc.player.getY()),
                Mth.lerp(partialTick, mc.player.zOld, mc.player.getZ()),
                Mth.rotLerp(partialTick, mc.player.yBodyRotO, mc.player.yBodyRot));
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {}

    private void renderTextOverlay(GuiGraphicsExtractor guiGraphicsExtractor, float time) {

        float screenWidth = guiGraphicsExtractor.guiWidth();
        float screenHeight = guiGraphicsExtractor.guiHeight();

        int goldColor = withAlpha(-11930, 230);

        int creamColor = withAlpha(-2872, 235);

        int paleGoldColor = withAlpha(-8054, 220);
        int phraseIndex = (int) (time / 2.4F) % phrases.length;
        float scale = 0.92F + 0.08F * Mth.sin(time * 3.2F);
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor
                .pose()
                .translate(screenWidth / 2.0F, 16.0F + Mth.sin(time * 1.6F) * 3.0F);
        guiGraphicsExtractor.pose().scale(scale, scale);
        guiGraphicsExtractor.centeredText(mc.font, phrases[phraseIndex], 0, 0, creamColor);
        String nextPhrase = phrases[(phraseIndex + 1) % phrases.length];
        int heightInt;

        guiGraphicsExtractor.centeredText(mc.font, nextPhrase, 0, 12, goldColor);
        guiGraphicsExtractor.pose().popMatrix();

        guiGraphicsExtractor.centeredText(
                mc.font, "حَلَال", 22, (int) (screenHeight / 2.0F - 48.0F), paleGoldColor);

        int rightEdgeX = (int) screenWidth;

        guiGraphicsExtractor.centeredText(
                mc.font,
                "اللَّهُ أَكْبَرُ",
                rightEdgeX - 22,
                (int) (screenHeight / 2.0F - 48.0F),
                goldColor);

        int centerXInt = (int) (screenWidth / 2.0F);
        heightInt = (int) screenHeight;

        guiGraphicsExtractor.centeredText(
                mc.font, "سُبْحَانَ اللَّهِ", centerXInt, heightInt - 18, creamColor);

        heightInt = (int) screenHeight;

        guiGraphicsExtractor.centeredText(
                mc.font, "الْحَمْدُ لِلَّهِ", 70, heightInt - 34, goldColor);

        int rightEdge = (int) screenWidth;
        int bottomEdge;

        int rightLabelX = rightEdge - 80;
        bottomEdge = (int) screenHeight;

        guiGraphicsExtractor.centeredText(
                mc.font, "لَا إِلَٰهَ إِلَّا اللَّهُ", rightLabelX, bottomEdge - 34, paleGoldColor);

        for (int i = 0; i < phrases.length; i++) {
            float driftX =
                    this.pingPong(
                                    time * (38.0F + i * 9.0F),
                                    Math.max(0.0F, screenWidth - 40.0F),
                                    i * 0.13F)
                            + 20.0F;
            float driftY =
                    this.pingPong(
                                    time * (27.0F + i * 7.0F),
                                    Math.max(0.0F, screenHeight - 28.0F),
                                    i * 0.21F + 0.4F)
                            + 14.0F;
            guiGraphicsExtractor.centeredText(
                    mc.font,
                    phrases[i],
                    (int) driftX,
                    (int) driftY,
                    i % 2 == 0 ? goldColor : creamColor);
        }

        int ringIndex = 0;

        while (true) {

            if (ringIndex >= 10) {
                return;
            }

            float angle = ringIndex * (float) (Math.PI * 2) / 10.0F + time * 0.45F;
            float radiusX = screenWidth * 0.38F;
            float radiusY = screenHeight * 0.32F;
            float ringX = screenWidth / 2.0F + Mth.cos(angle) * radiusX;
            float ringY = screenHeight / 2.0F + Mth.sin(angle) * radiusY;
            guiGraphicsExtractor.centeredText(
                    mc.font,
                    phrases[(ringIndex + phraseIndex) % phrases.length],
                    (int) ringX,
                    (int) ringY,
                    ringIndex % 2 == 0 ? paleGoldColor : goldColor);
            ringIndex++;
        }
    }

    public void toggle() {
        active = !active;
        if (active) {
            startMillis = Util.getMillis();
        } else {
            this.nearbyProgress = 0.0F;
            this.closeProgress = 0.0F;
            this.lastFrameMillis = 0L;
            this.sparkles.clear();
        }

        String message;
        if (active) {

            message = "§d♥ kawaslikesmen:3 enabled ♥";
        } else {

            message = "§7kawaslikesmen:3 disabled";
        }

        this.printMessage(message);
    }

    private Vec3 getWandTipPosition(PlayerPose pose, float length, float pitch) {
        double yawRadians = Math.toRadians(pose.yaw());
        double pitchRadians = Math.toRadians(pitch);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double reach = length + 0.08F;
        return new Vec3(
                pose.x() + forwardX * (-0.12F + Math.cos(pitchRadians) * reach),
                pose.y() + 0.72F + Math.sin(pitchRadians) * reach,
                pose.z() + forwardZ * (-0.12F + Math.cos(pitchRadians) * reach));
    }

    private void renderSparkles(PoseStack poseStack, float deltaSeconds) {
        if (!this.sparkles.isEmpty()) {
            poseStack.pushPose();
            Vec3 vector3 = mc.gameRenderer.mainCamera().position();
            poseStack.translate(-vector3.x, -vector3.y, -vector3.z);
            Matrix4f matrix4f = poseStack.last().pose();
            StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
            RenderType renderType = RenderUtil.filledNoDepthRenderType;
            Draw draw =
                    stagedVertexBuffer.appendDraw(
                            renderType.format(), renderType.primitiveTopology());
            VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
            boolean anyDrawn = false;
            Iterator iterator = this.sparkles.iterator();

            while (iterator.hasNext()) {
                SparkParticle sparkle = (SparkParticle) iterator.next();
                sparkle.remainingLife -= deltaSeconds;
                if (sparkle.remainingLife <= 0.0F) {
                    iterator.remove();
                } else {
                    sparkle.x = sparkle.x + sparkle.velocityX * deltaSeconds;
                    sparkle.y = sparkle.y + sparkle.velocityY * deltaSeconds;
                    sparkle.z = sparkle.z + sparkle.velocityZ * deltaSeconds;
                    sparkle.velocityY -= 2.4 * deltaSeconds;
                    float halfSize = 0.022F + sparkle.remainingLife * 0.035F;
                    int alpha =
                            Math.round(
                                    235.0F * Mth.clamp(sparkle.remainingLife / 0.5F, 0.0F, 1.0F));

                    int alphaBits = alpha << 24;

                    int color = alphaBits | 16774884;
                    float minX = (float) (sparkle.x - halfSize);
                    float minY = (float) (sparkle.y - halfSize);
                    float minZ = (float) (sparkle.z - halfSize);
                    float maxX = (float) (sparkle.x + halfSize);
                    float maxY = (float) (sparkle.y + halfSize);
                    float maxZ = (float) (sparkle.z + halfSize);
                    addQuad(
                            matrix4f,
                            vertexConsumer,
                            color,
                            minX,
                            minY,
                            maxZ,
                            maxX,
                            minY,
                            maxZ,
                            maxX,
                            minY,
                            minZ,
                            minX,
                            minY,
                            minZ);
                    addQuad(
                            matrix4f,
                            vertexConsumer,
                            color,
                            minX,
                            maxY,
                            minZ,
                            maxX,
                            maxY,
                            minZ,
                            maxX,
                            maxY,
                            maxZ,
                            minX,
                            maxY,
                            maxZ);
                    addQuad(
                            matrix4f,
                            vertexConsumer,
                            color,
                            minX,
                            minY,
                            minZ,
                            minX,
                            minY,
                            maxZ,
                            minX,
                            maxY,
                            maxZ,
                            minX,
                            maxY,
                            minZ);
                    addQuad(
                            matrix4f,
                            vertexConsumer,
                            color,
                            maxX,
                            minY,
                            maxZ,
                            maxX,
                            minY,
                            minZ,
                            maxX,
                            maxY,
                            minZ,
                            maxX,
                            maxY,
                            maxZ);
                    addQuad(
                            matrix4f,
                            vertexConsumer,
                            color,
                            maxX,
                            minY,
                            minZ,
                            minX,
                            minY,
                            minZ,
                            minX,
                            maxY,
                            minZ,
                            maxX,
                            maxY,
                            minZ);
                    addQuad(
                            matrix4f,
                            vertexConsumer,
                            color,
                            minX,
                            minY,
                            maxZ,
                            maxX,
                            minY,
                            maxZ,
                            maxX,
                            maxY,
                            maxZ,
                            minX,
                            maxY,
                            maxZ);
                    anyDrawn = true;
                }
            }

            if (anyDrawn) {
                stagedVertexBuffer.upload();
                PreparedRenderType preparedRenderType = renderType.prepare();
                ExecuteInfo executeInfo = stagedVertexBuffer.getExecuteInfo(draw);
                if (executeInfo != null) {
                    preparedRenderType.drawFromBuffer(executeInfo);
                }
            }

            stagedVertexBuffer.endDraw();
            poseStack.popPose();
        }
    }

    private void renderTargetMarker(PoseStack poseStack, Vec3 vector3, float yaw, float intensity) {
        poseStack.pushPose();
        Vec3 vector32 = mc.gameRenderer.mainCamera().position();
        poseStack.translate(vector3.x - vector32.x, vector3.y - vector32.y, vector3.z - vector32.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        float halfWidth = 0.08F + intensity * 0.045F;
        float halfHeight = 0.055F + intensity * 0.018F;
        float minX = -halfWidth;
        float minY = -halfHeight;

        this.drawBox(poseStack, minX, minY, -0.018F, -0.008F, halfHeight, 0.028F, -222012814);
        minY = -halfHeight;

        this.drawBox(poseStack, 0.008F, minY, -0.018F, halfWidth, halfHeight, 0.028F, -222012814);
        minY = -halfHeight * 0.72F;
        float maxY = halfHeight * 0.72F;

        this.drawBox(poseStack, -0.012F, minY, -0.006F, 0.012F, maxY, 0.042F, -393596856);
        poseStack.popPose();
    }

    private void drawBurst(
            GuiGraphicsExtractor guiGraphicsExtractor,
            float x,
            float y,
            long cycleMillis,
            long triggerMillis,
            int color) {
        long ageMillis = cycleMillis - triggerMillis;
        if (ageMillis >= 0L) {

            if (ageMillis <= 900L) {
                float progress = (float) ageMillis / 900.0F;
                float radius = 8.0F + progress * 42.0F;
                int alpha = Math.round(255.0F * (1.0F - progress));

                int alphaBits = alpha << 24;

                int fadedColor = alphaBits | color & 16777215;
                int i = 0;

                while (true) {

                    if (i >= 12) {
                        this.drawHeart(
                                guiGraphicsExtractor,
                                x,
                                y,
                                0.55F + (1.0F - progress) * 0.4F,
                                fadedColor);
                        return;
                    }

                    float angle = i * (float) (Math.PI * 2) / 12.0F;
                    float rayX = x + Mth.cos(angle) * radius;
                    float rayY = y + Mth.sin(angle) * radius;
                    guiGraphicsExtractor.pose().pushMatrix();
                    guiGraphicsExtractor.pose().translate(rayX, rayY);
                    guiGraphicsExtractor.pose().rotate(angle);

                    guiGraphicsExtractor.fill(0, -1, 9, 1, fadedColor);
                    guiGraphicsExtractor.pose().popMatrix();
                    i++;
                }
            }
        }
    }

    static {
        primaryTexture =
                Identifier.fromNamespaceAndPath("astralis-recode", "textures/kawaslikesmen3.png");

        FloatingImage[] images = new FloatingImage[9];
        images[0] = new FloatingImage(primaryTexture, 158, 67, 316, 134, 92.0F, 61.0F, 0.0F);

        Identifier attachmentTexture = createTextureId("textures/attachment.png");
        images[1] = new FloatingImage(attachmentTexture, 142, 90, 284, 179, 71.0F, 49.0F, 0.18F);

        Identifier toGifTexture1 = createTextureId("textures/togif1.png");
        images[2] = new FloatingImage(toGifTexture1, 160, 37, 319, 74, 58.0F, 83.0F, 0.36F);

        Identifier toGifTexture2 = createTextureId("textures/togif2.png");
        images[3] = new FloatingImage(toGifTexture2, 123, 38, 205, 63, 84.0F, 39.0F, 0.54F);

        Identifier toGifTexture3 = createTextureId("textures/togif3.png");
        images[4] = new FloatingImage(toGifTexture3, 150, 115, 500, 384, 47.0F, 67.0F, 0.72F);

        Identifier extraTexture1 = createTextureId("textures/kawas_extra1.png");
        images[5] = new FloatingImage(extraTexture1, 113, 71, 284, 179, 63.0F, 91.0F, 0.27F);

        Identifier extraTexture2 = createTextureId("textures/kawas_extra2.png");
        images[6] = new FloatingImage(extraTexture2, 128, 30, 319, 74, 79.0F, 44.0F, 0.63F);

        Identifier extraTexture3 = createTextureId("textures/kawas_extra3.png");
        images[7] = new FloatingImage(extraTexture3, 103, 32, 205, 63, 54.0F, 76.0F, 0.81F);

        Identifier extraTexture4 = createTextureId("textures/kawas_extra4.png");
        images[8] = new FloatingImage(extraTexture4, 125, 96, 500, 384, 66.0F, 52.0F, 0.09F);

        floatingImages = images;

        String[] phraseList = new String[8];
        phraseList[0] = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ";
        phraseList[1] = "قُلْ هُوَ اللَّهُ أَحَدٌ";
        phraseList[2] = "اللَّهُ الصَّمَدُ";
        phraseList[3] = "لَمْ يَلِدْ وَلَمْ يُولَدْ";
        phraseList[4] = "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ";
        phraseList[5] = "لَا إِلَٰهَ إِلَّا اللَّهُ";
        phraseList[6] = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ";
        phraseList[7] = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ";
        phrases = phraseList;
    }
}
