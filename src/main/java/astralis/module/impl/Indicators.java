package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.mixin.AbstractArrowInvoker;
import astralis.module.Module;
import astralis.module.impl.indicators.ProjectileIndicator;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.RotationUtil;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.ScreenPoint;
import astralis.util.render.ScreenProjector;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class Indicators extends Module {
    private final BooleanSetting renderArrows;
    private final BooleanSetting renderEnderPearls;
    private final BooleanSetting renderFireballs;
    private final BooleanSetting renderEggs;
    private final BooleanSetting renderSnowballs;
    private final SettingGroup itemsGroup;
    private final NumberSetting radius;
    private final ModeSetting font;
    private final BooleanSetting itemColors;
    private final BooleanSetting renderItem;
    private final BooleanSetting renderDistance;
    private final BooleanSetting onlyWhenApproaching;
    private final BooleanSetting renderOnlyOffscreen;
    private final List<Entity> trackedProjectiles;
    private final IdentityHashMap<Entity, Vec3> previousPositions;
    private int tickCounter;
    private static float[][] arrowOutline;
    private static Identifier arrowTextureId;
    private GpuTexture arrowTexture;
    private float arrowBoundsX;
    private float arrowBoundsY;
    private float arrowBoundsWidth;
    private float arrowBoundsHeight;

    public Indicators() {

        super(MODULE_ID, "", "", Category.Misc);

        this.renderArrows = new BooleanSetting(RENDER_ARROWS_SETTING_ID, true);

        this.renderEnderPearls = new BooleanSetting(RENDER_ENDER_PEARLS_SETTING_ID, true);

        this.renderFireballs = new BooleanSetting(RENDER_FIREBALLS_SETTING_ID, true);

        this.renderEggs = new BooleanSetting(RENDER_EGGS_SETTING_ID, false);

        this.renderSnowballs = new BooleanSetting(RENDER_SNOWBALLS_SETTING_ID, false);

        this.itemsGroup =
                new SettingGroup(ITEMS_GROUP_ID)
                        .collapsed()
                        .addValues(
                                this.renderArrows,
                                this.renderEnderPearls,
                                this.renderFireballs,
                                this.renderEggs,
                                this.renderSnowballs);

        this.radius = new NumberSetting(RADIUS_SETTING_ID, 50.0F, 30.0F, 200.0F, 5.0F);

        ModeSetting modeSetting = new ModeSetting(FONT_SETTING_ID);
        String[] fontNames = new String[7];

        fontNames[0] = "Product Sans";

        fontNames[1] = "Inter";

        fontNames[2] = "SF Pro";

        fontNames[3] = "Tahoma";

        fontNames[4] = "Geist";

        fontNames[5] = "Roboto";

        fontNames[6] = "IBM Plex Sans";
        ModeSetting modeSetting2 = modeSetting.addModes(fontNames);
        this.font = modeSetting2.setDefault("Product Sans");

        this.itemColors = new BooleanSetting(ITEM_COLORS_SETTING_ID, true);

        this.renderItem = new BooleanSetting(RENDER_ITEM_SETTING_ID, true);

        this.renderDistance = new BooleanSetting(RENDER_DISTANCE_SETTING_ID, true);

        this.onlyWhenApproaching = new BooleanSetting(ONLY_WHEN_APPROACHING_SETTING_ID, false);

        this.renderOnlyOffscreen = new BooleanSetting(RENDER_ONLY_OFFSCREEN_SETTING_ID, false);
        this.trackedProjectiles = new java.util.ArrayList<>();
        this.previousPositions = new IdentityHashMap<>();

        Setting[] allSettings =
                new Setting[] {
                    this.itemsGroup,
                    this.radius,
                    this.font,
                    this.itemColors,
                    this.renderItem,
                    this.renderDistance,
                    null,
                    null
                };

        allSettings[6] = this.onlyWhenApproaching;

        allSettings[7] = this.renderOnlyOffscreen;
        this.addSettings(allSettings);
    }

    private ProjectileIndicator computeIndicator(Entity entity2) {

        if (mc.gui.screen() != null) {
            return null;
        }

        if (mc.player == null || mc.level == null) {
            return null;
        }

        if (entity2.isRemoved()) {
            return null;
        }

        ItemStack itemStack = this.getProjectileIcon(entity2);
        if (itemStack == null) {
            return null;
        }

        double impactSeconds =
                entity2 instanceof LargeFireball largeFireball
                        ? this.estimateImpactSeconds(largeFireball)
                        : -1.0;
        int fadeAlpha = this.computeFadeAlpha(impactSeconds);
        if (fadeAlpha == 0) {
            return null;
        }

        if (this.renderOnlyOffscreen.getValue()) {
            Frustum frustum =
                    mc.gameRenderer.mainCamera() == null
                            ? null
                            : mc.gameRenderer.mainCamera().getCullFrustum();
            if (frustum != null && frustum.isVisible(entity2.getBoundingBox())) {
                return null;
            }
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double entityX = entity2.xo + (entity2.getX() - entity2.xo) * partialTick;
        double entityY =
                entity2.yo
                        + (entity2.getY() - entity2.yo) * partialTick
                        + entity2.getBbHeight() / 2.0;
        double entityZ = entity2.zo + (entity2.getZ() - entity2.zo) * partialTick;
        ScreenPoint screenPoint =
                ScreenProjector.projectToScreen(new Vec3(entityX, entityY, entityZ));
        if (!screenPoint.isValid()) {
            return null;
        }

        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0F;
        float centerY = mc.getWindow().getGuiScaledHeight() / 2.0F;
        float offsetX = screenPoint.getScreenX() - centerX;
        float offsetY = screenPoint.getScreenY() - centerY;
        if (screenPoint.isBehindCamera()) {
            offsetX = -offsetX;
            offsetY = -offsetY;
        }

        float distance = (float) Math.hypot(offsetX, offsetY);
        if (distance < 1.0E-6F) {
            return null;
        }

        float ringRadius = this.radius.getValue() + (this.renderItem.getValue() ? 20.0F : 0.0F);
        if (!screenPoint.isBehindCamera() && distance < ringRadius + 15.0F) {
            return null;
        }

        float angle = (float) Math.atan2(offsetX, offsetY);
        float directionX = (float) Math.sin(angle);

        float directionY = (float) Math.cos(angle);
        float markerX = centerX + ringRadius * directionX;
        float markerY = centerY + ringRadius * directionY;
        float rotationDegrees = (float) (Math.atan2(offsetY, offsetX) * 180.0 / Math.PI + 90.0);
        short red = 255;

        short green = 255;

        short blue = 255;
        if (this.itemColors.getValue()) {
            Item item2 = itemStack.getItem();
            if (item2 == Items.ENDER_PEARL) {

                red = 210;
                green = 0;

                blue = 255;
            } else if (item2 == Items.FIRE_CHARGE) {

                red = 255;

                green = 150;
                blue = 0;
            } else if (item2 == Items.EGG) {

                red = 255;

                green = 238;

                blue = 154;
            }
        }

        int argb = ColorUtil.withAlpha(new Color(red, green, blue), fadeAlpha).getRGB();
        return new ProjectileIndicator(
                itemStack,
                centerX,
                centerY,
                directionX,
                directionY,
                ringRadius,
                markerX,
                markerY,
                rotationDegrees,
                fadeAlpha,
                argb,
                impactSeconds);
    }

    private static float distanceToPolygonEdge(float pointX, float pointY, float[][] polygon) {
        float minDistance = Float.POSITIVE_INFINITY;
        int currentIndex = 0;

        for (int previousIndex = polygon.length - 1;
                currentIndex < polygon.length;
                previousIndex = currentIndex++) {
            float prevX = polygon[previousIndex][0];
            float prevY = polygon[previousIndex][1];
            float currX = polygon[currentIndex][0];
            float currY = polygon[currentIndex][1];
            float edgeDx = currX - prevX;
            float edgeDy = currY - prevY;
            float edgeLengthSq = edgeDx * edgeDx + edgeDy * edgeDy;
            float projection =
                    edgeLengthSq < 1.0E-12F
                            ? 0.0F
                            : Math.clamp(
                                    ((pointX - prevX) * edgeDx + (pointY - prevY) * edgeDy)
                                            / edgeLengthSq,
                                    0.0F,
                                    1.0F);
            float closestX = prevX + projection * edgeDx;
            float closestY = prevY + projection * edgeDy;
            minDistance =
                    Math.min(minDistance, (float) Math.hypot(pointX - closestX, pointY - closestY));
        }

        return minDistance;
    }

    private String formatSeconds(double seconds) {
        int tenths = Math.max(1, (int) Math.ceil(seconds * 10.0));

        if (tenths >= 100) {
            return (int) Math.ceil(seconds) + "s";
        } else {

            if (tenths % 10 == 0) {

                return tenths / 10 + "s";
            } else {

                int wholeSeconds = tenths / 10;

                return wholeSeconds + "." + tenths % 10 + "s";
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST) {
            if (mc.player != null && mc.level != null) {
                this.tickCounter++;
                if (this.tickCounter % 5 == 0) {
                    this.trackedProjectiles.clear();
                    Set liveProjectiles = Collections.newSetFromMap(new IdentityHashMap());

                    for (Entity entity2 : mc.level.entitiesForRendering()) {
                        if (this.getProjectileIcon(entity2) != null
                                && this.isProjectileEnabled(entity2)) {
                            liveProjectiles.add(entity2);
                            if (this.onlyWhenApproaching.getValue()) {
                                Vec3 vector3 = this.previousPositions.get(entity2);
                                if (vector3 == null) {
                                    this.previousPositions.put(entity2, entity2.position());
                                    continue;
                                }

                                if (!this.isApproaching(
                                        vector3,
                                        entity2.position(),
                                        entity2.getBbHeight(),
                                        mc.player)) {
                                    this.previousPositions.put(entity2, entity2.position());
                                    continue;
                                }
                            }

                            this.trackedProjectiles.add(entity2);
                            this.previousPositions.put(entity2, entity2.position());
                        }
                    }

                    this.previousPositions.keySet().retainAll(liveProjectiles);
                }
            } else {
                this.trackedProjectiles.clear();
                this.previousPositions.clear();
            }
        }
    }

    private int computeFadeAlpha(double impactSeconds) {

        if (impactSeconds < 0.0) {

            return 255;
        }

        if (impactSeconds < 0.2) {
            return (int) Math.round(255.0 * impactSeconds / 0.2);
        }

        return 255;
    }

    private static boolean isPointInPolygon(float pointX, float pointY, float[][] polygon) {
        boolean inside = false;
        int currentIndex = 0;

        for (int previousIndex = polygon.length - 1;
                currentIndex < polygon.length;
                previousIndex = currentIndex++) {
            float currY = polygon[currentIndex][1];
            float prevY = polygon[previousIndex][1];
            float currX = polygon[currentIndex][0];
            float prevX = polygon[previousIndex][0];
            if (currY > pointY != prevY > pointY
                    && pointX < (prevX - currX) * (pointY - currY) / (prevY - currY) + currX) {
                inside = !inside;
            }
        }

        return inside;
    }

    private String resolveFontName() {

        if (this.font.is("Inter")) {

            return "Inter Medium";
        } else {

            if (this.font.is("SF Pro")) {

                return "SF Pro Regular";
            } else {

                if (this.font.is("Tahoma")) {

                    return "Tahoma";
                } else {

                    if (this.font.is("Geist")) {

                        return "Geist";
                    } else {

                        if (this.font.is("Roboto")) {

                            return "Roboto";
                        } else {

                            if (this.font.is("IBM Plex Sans")) {

                                return "IBM Plex Sans";
                            } else {

                                return "Product Sans Regular";
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isRenderBlocked() {
        return mc.gui.screen() != null || mc.player == null || mc.level == null;
    }

    @EventTarget
    public void onVanillaHudRender(VanillaHudRenderEvent event) {

        if (!this.isRenderBlocked()) {
            if (this.renderItem.getValue()) {
                GuiGraphicsExtractor guiGraphicsExtractor = event.getGraphics();

                for (Entity entity2 : this.trackedProjectiles) {
                    ProjectileIndicator indicator = this.computeIndicator(entity2);
                    if (indicator != null) {
                        float iconX;
                        float iconY;
                        float iconRotation;
                        byte itemOffsetX;
                        byte itemOffsetY;
                        if (indicator.getStack().getItem() == Items.ARROW) {
                            iconX =
                                    indicator.getCenterX()
                                            + (indicator.getRadius() - 18.0F)
                                                    * indicator.getDirectionX();
                            iconY =
                                    indicator.getCenterY()
                                            + (indicator.getRadius() - 18.0F)
                                                    * indicator.getDirectionY();

                            iconRotation = indicator.getRotation() - 45.0F;
                            itemOffsetX = -12;

                            itemOffsetY = -4;
                        } else {
                            iconX =
                                    indicator.getCenterX()
                                            + (indicator.getRadius() - 20.0F)
                                                    * indicator.getDirectionX();
                            iconY =
                                    indicator.getCenterY()
                                            + (indicator.getRadius() - 20.0F)
                                                    * indicator.getDirectionY();

                            iconRotation = 0.0F;
                            itemOffsetX = -8;

                            itemOffsetY = -9;
                        }

                        guiGraphicsExtractor.pose().pushMatrix();
                        guiGraphicsExtractor.pose().translate(iconX, iconY);
                        if (iconRotation != 0.0F) {
                            guiGraphicsExtractor
                                    .pose()
                                    .rotate((float) Math.toRadians(iconRotation));
                        }

                        guiGraphicsExtractor.item(indicator.getStack(), itemOffsetX, itemOffsetY);
                        guiGraphicsExtractor.pose().popMatrix();
                    }
                }
            }
        }
    }

    private GpuTexture ensureArrowTexture() {

        if (this.arrowTexture != null) {
            return this.arrowTexture;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (float[] point : arrowOutline) {
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }

        float originX = minX - 2.4F;
        float originY = minY - 2.4F;
        maxX += 2.4F;
        maxY += 2.4F;
        this.arrowBoundsX = originX;
        this.arrowBoundsY = originY;
        this.arrowBoundsWidth = maxX - originX;
        this.arrowBoundsHeight = maxY - originY;

        NativeImage nativeImage = new NativeImage(128, 128, true);

        try {
            int pixelY = 0;

            while (true) {
                if (pixelY >= 128) {
                    mc.getTextureManager()
                            .register(
                                    arrowTextureId,
                                    new DynamicTexture(() -> "indicators_arrow", nativeImage));
                    this.arrowTexture =
                            mc.getTextureManager().getTexture(arrowTextureId).getTexture();
                    return this.arrowTexture;
                }

                int pixelX = 0;

                while (true) {
                    if (pixelX >= 128) {
                        pixelY++;
                        break;
                    }

                    float coverage = 0.0F;
                    float edgeCoverage = 0.0F;

                    for (int subY = 0; subY < 2; subY++) {
                        for (int subX = 0; subX < 2; subX++) {
                            float sampleX =
                                    originX
                                            + (pixelX + (subX + 0.5F) * 0.5F)
                                                    / 128.0F
                                                    * this.arrowBoundsWidth;
                            float sampleY =
                                    originY
                                            + (pixelY + (subY + 0.5F) * 0.5F)
                                                    / 128.0F
                                                    * this.arrowBoundsHeight;
                            if (isPointInPolygon(sampleX, sampleY, arrowOutline)) {
                                coverage++;
                            }

                            if (distanceToPolygonEdge(sampleX, sampleY, arrowOutline) < 0.8F) {
                                edgeCoverage++;
                            }
                        }
                    }

                    coverage *= 0.25F;
                    float edgeFraction = edgeCoverage * 0.25F;
                    float alpha = Math.max(coverage, edgeFraction);
                    if (!(alpha < 0.003921569F)) {
                        float brightness =
                                edgeFraction > 0.0F
                                        ? 1.0F - 0.58000004F * Math.min(1.0F, edgeFraction / alpha)
                                        : 1.0F;
                        int channelValue = Math.round(brightness * 255.0F);
                        int alphaValue = Math.round(alpha * 255.0F);

                        int alphaShifted = alphaValue << 24;

                        int alphaRed = alphaShifted | channelValue << 16;

                        nativeImage.setPixel(
                                pixelX, pixelY, alphaRed | channelValue << 8 | channelValue);
                    }

                    pixelX++;
                }
            }
        } catch (RuntimeException ignored) {
            nativeImage.close();
            return null;
        }
    }

    private double estimateImpactSeconds(LargeFireball largeFireball) {
        Vec3 vector3 = largeFireball.getDeltaMovement();
        if (vector3.lengthSqr() <= 1.0E-7) {
            return -1.0;
        }

        AABB boundingBox =
                mc.player
                        .getBoundingBox()
                        .inflate(
                                largeFireball.getBbWidth() / 2.0 + 0.3,
                                largeFireball.getBbHeight() + 0.3,
                                largeFireball.getBbWidth() / 2.0 + 0.3);
        Vec3 vector32 = largeFireball.position();
        int step = 0;

        while (true) {

            if (step >= 200) {
                return -1.0;
            }

            if (boundingBox.contains(vector32)) {
                return step / 20.0;
            }

            Vec3 vector33 = vector32.add(vector3);
            Vec3 vector34 = boundingBox.clip(vector32, vector33).orElse(null);
            BlockHitResult blockHitResult =
                    RotationUtil.rayTraceBetween(vector32, vector33, false, true);
            Vec3 vector35 =
                    blockHitResult.getType() != Type.MISS ? blockHitResult.getLocation() : null;
            Vec3 vector36 = null;
            if (vector34 != null && vector35 != null) {
                vector36 =
                        vector32.distanceToSqr(vector34) <= vector32.distanceToSqr(vector35)
                                ? vector34
                                : vector35;
            } else if (vector34 != null) {
                vector36 = vector34;
            } else if (vector35 != null) {
                vector36 = vector35;
            }

            if (vector36 != null) {
                double segmentLengthSq = vector32.distanceToSqr(vector33);
                double fraction =
                        segmentLengthSq < 1.0E-12
                                ? 0.0
                                : Math.sqrt(vector32.distanceToSqr(vector36) / segmentLengthSq);
                fraction = Math.clamp(fraction, 0.0, 1.0);
                return (step + fraction) / 20.0;
            }

            vector32 = vector33;
            step++;
        }
    }

    private ItemStack getProjectileIcon(Entity entity2) {
        if (entity2 != null && entity2 != mc.player) {
            if (entity2 instanceof ThrownTrident) {
                return null;
            } else if (entity2 instanceof AbstractArrow abstractArrow) {
                return ((AbstractArrowInvoker) abstractArrow).callIsInGround()
                        ? null
                        : new ItemStack(Items.ARROW);
            } else if (entity2 instanceof Fireball) {
                return new ItemStack(Items.FIRE_CHARGE);
            } else if (entity2 instanceof ThrownEnderpearl) {
                return new ItemStack(Items.ENDER_PEARL);
            } else if (entity2 instanceof ThrownEgg) {
                return new ItemStack(Items.EGG);
            } else {
                return entity2 instanceof Snowball ? new ItemStack(Items.SNOWBALL) : null;
            }
        } else {
            return null;
        }
    }

    private static float[][] buildArrowOutline() {
        double[][] corners =
                new double[][] {{0.0, -7.0200000000000005}, {5.46, 5.07}, {-5.46, 5.07}};
        java.util.ArrayList<float[]> outline = new java.util.ArrayList<>();
        double cutRatio = 0.18;
        byte segmentCount = 5;

        for (int cornerIndex = 0; cornerIndex < corners.length; cornerIndex++) {
            double[] prev = corners[(cornerIndex + corners.length - 1) % corners.length];
            double[] corner = corners[cornerIndex];
            double[] next = corners[(cornerIndex + 1) % corners.length];
            double startX = corner[0] + (prev[0] - corner[0]) * cutRatio;
            double startY = corner[1] + (prev[1] - corner[1]) * cutRatio;
            double endX = corner[0] + (next[0] - corner[0]) * cutRatio;
            double endY = corner[1] + (next[1] - corner[1]) * cutRatio;

            for (int segment = 0; segment <= segmentCount; segment++) {
                double t = segment / 5.0;
                double u = 1.0 - t;
                double pointX = u * u * startX + 2.0 * u * t * corner[0] + t * t * endX;
                double pointY = u * u * startY + 2.0 * u * t * corner[1] + t * t * endY;
                outline.add(new float[] {(float) pointX, (float) pointY});
            }
        }

        return outline.toArray(new float[0][]);
    }

    @Override
    public void onDisable() {
        this.trackedProjectiles.clear();
        this.previousPositions.clear();
        this.tickCounter = 0;
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        if (!this.isRenderBlocked()) {
            Render2DContext context = render2DEvent.getContext();
            GpuTexture gpuTexture = this.ensureArrowTexture();

            for (Entity entity2 : this.trackedProjectiles) {
                ProjectileIndicator indicator = this.computeIndicator(entity2);
                if (indicator != null) {
                    Color color = new Color(indicator.getColor(), true);
                    if (gpuTexture != null) {
                        context.pushPose();
                        context.translate(indicator.getMarkerX(), indicator.getMarkerY());
                        context.rotateZ(indicator.getRotation());
                        context.drawTexture(
                                TexturedRectShape.obtain()
                                        .setBounds(
                                                this.arrowBoundsX,
                                                this.arrowBoundsY,
                                                this.arrowBoundsWidth,
                                                this.arrowBoundsHeight)
                                        .setTexture(gpuTexture)
                                        .setColor(color));
                        context.popPose();
                    }

                    if (this.renderDistance.getValue()) {
                        String distanceText = Math.round(mc.player.distanceTo(entity2)) + "m";
                        float textX =
                                indicator.getCenterX()
                                        + (indicator.getRadius()
                                                        - (this.renderItem.getValue()
                                                                ? 36.0F
                                                                : 13.0F))
                                                * indicator.getDirectionX();
                        float textY =
                                indicator.getCenterY()
                                        + (indicator.getRadius()
                                                        - (this.renderItem.getValue()
                                                                ? 36.0F
                                                                : 13.0F))
                                                * indicator.getDirectionY();
                        SizedFont distanceFont = FontManager.getFont(this.resolveFontName(), 8.0F);
                        int textColor =
                                ColorUtil.withAlpha(Color.WHITE, indicator.getAlpha()).getRGB();
                        context.drawText(
                                TextDrawRequest.obtain()
                                        .setText(distanceText)
                                        .setPosition(
                                                textX
                                                        - distanceFont.getStringWidth(distanceText)
                                                                / 2.0F,
                                                textY - distanceFont.getLineHeight() / 2.0F)
                                        .setFont(distanceFont.getMsdfFont())
                                        .setFontSize(distanceFont.getSize())
                                        .setColor(textColor));
                    }

                    if (entity2 instanceof LargeFireball && indicator.getSecondsToImpact() >= 0.0) {
                        String timeText = this.formatSeconds(indicator.getSecondsToImpact());
                        float timeX =
                                indicator.getCenterX()
                                        + (indicator.getRadius() + 13.0F)
                                                * indicator.getDirectionX();
                        float timeY =
                                indicator.getCenterY()
                                        + (indicator.getRadius() + 13.0F)
                                                * indicator.getDirectionY();
                        SizedFont timeFont = FontManager.getFont(this.resolveFontName(), 7.5F);
                        int timeColor =
                                ColorUtil.withAlpha(Color.WHITE, indicator.getAlpha()).getRGB();
                        context.drawText(
                                TextDrawRequest.obtain()
                                        .setText(timeText)
                                        .setPosition(
                                                timeX - timeFont.getStringWidth(timeText) / 2.0F,
                                                timeY - timeFont.getLineHeight() / 2.0F)
                                        .setFont(timeFont.getMsdfFont())
                                        .setFontSize(timeFont.getSize())
                                        .setColor(timeColor));
                    }
                }
            }
        }
    }

    private boolean isProjectileEnabled(Entity entity2) {
        if (entity2 != null && entity2 != mc.player) {
            if (entity2 instanceof ThrownTrident) {
                return false;
            } else if (entity2 instanceof AbstractArrow) {
                return this.renderArrows.getValue();
            } else if (entity2 instanceof LargeFireball) {
                return this.renderFireballs.getValue();
            } else if (entity2 instanceof ThrownEnderpearl) {
                return this.renderEnderPearls.getValue();
            } else if (entity2 instanceof ThrownEgg) {
                return this.renderEggs.getValue();
            } else {
                return entity2 instanceof Snowball ? this.renderSnowballs.getValue() : false;
            }
        } else {
            return false;
        }
    }

    private boolean isApproaching(Vec3 vector3, Vec3 vector32, float height, Player player2) {
        Vec3 vector33 =
                new Vec3(
                        player2.getX(),
                        player2.getY() + player2.getBbHeight() / 2.0,
                        player2.getZ());
        Vec3 vector34 = new Vec3(vector3.x, vector3.y + height / 2.0, vector3.z);
        Vec3 vector35 = new Vec3(vector32.x, vector32.y + height / 2.0, vector32.z);
        double previousDistance = vector34.distanceTo(vector33);
        double currentDistance = vector35.distanceTo(vector33);
        return previousDistance - currentDistance > 1.0
                || Math.abs(vector34.y - vector33.y) - Math.abs(vector35.y - vector33.y) > 0.25;
    }

    static {
        arrowOutline = buildArrowOutline();
        arrowTextureId = Identifier.fromNamespaceAndPath("astralis", "indicators_arrow");
    }

    private static final String MODULE_ID = "beb721aa792be94b513aa49f36d8c04b";
    private static final String RENDER_ARROWS_SETTING_ID = "62eccacdbde1649572f97e3967b819e0";
    private static final String RENDER_ENDER_PEARLS_SETTING_ID =
            "6fe6487456f34fd40a3dd73b28ede58a1cfdfafd238080cf65f76ba163e6f69b";
    private static final String RENDER_FIREBALLS_SETTING_ID = "f88157d6a3ba323a335810ef0d3e14fb";
    private static final String RENDER_EGGS_SETTING_ID = "49f9b86edfae23c0e3dd5f8a5b4173d7";
    private static final String RENDER_SNOWBALLS_SETTING_ID = "607df34bf55d0a20e3fab1a61eff2e05";
    private static final String ITEMS_GROUP_ID = "85bcb947d24612694894cee81e6b2ae8";
    private static final String RADIUS_SETTING_ID = "00107704ec92e058e1cce9b2a3ab2723";
    private static final String FONT_SETTING_ID = "08cf192fd775b99ce904000e4ee6f5f1";
    private static final String ITEM_COLORS_SETTING_ID = "b43ecbba12d9a41ef9ebcefb632c33aa";
    private static final String RENDER_ITEM_SETTING_ID = "17ff121008c430bfc932656bdfd18c97";
    private static final String RENDER_DISTANCE_SETTING_ID = "fc26e7d332bcfcd801ef2e6ccf9054f9";
    private static final String ONLY_WHEN_APPROACHING_SETTING_ID =
            "aad2717a7f8a29d11f015627b69faccf0ee165c7bed1b1ec83075ad6cffd9118";
    private static final String RENDER_ONLY_OFFSCREEN_SETTING_ID =
            "8891cfd1d7f5b572a6485150fb18e9370063b0d189c52acfbad2a85f0e401f69";
}
