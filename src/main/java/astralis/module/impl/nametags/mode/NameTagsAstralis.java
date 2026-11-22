package astralis.module.impl.nametags.mode;

import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.MurderMystery;
import astralis.module.impl.NameTags;
import astralis.setting.Mode;
import astralis.util.BreakUtil;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ScreenProjector;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;
import astralis.util.target.TargetSortOrder;

import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;

public class NameTagsAstralis extends Mode<NameTags> {
    private static DecimalFormat integerFormat;

    public NameTagsAstralis(NameTags nameTags) {

        super("Astralis", nameTags);
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {

        List<LivingEntity> targets =
                BreakUtil.getTargets(
                        this.parent.animals.getValue(),
                        this.parent.monsters.getValue(),
                        this.parent.teammates.getValue(),
                        this.parent.invisible.getValue(),
                        this.parent.throughWalls.getValue(),
                        256.0F,
                        TargetSortOrder.DISTANCE,
                        false);
        if (targets != null) {
            Render2DContext context = render2DEvent.getContext();

            SizedFont font = FontManager.getFont("Product Sans Medium", 10.0F);

            for (LivingEntity livingEntity : targets) {
                if (livingEntity != mc.player || !mc.options.getCameraType().isFirstPerson()) {
                    double interpolatedX =
                            livingEntity.xo
                                    + (livingEntity.getX() - livingEntity.xo)
                                            * mc.getDeltaTracker()
                                                    .getGameTimeDeltaPartialTick(false);
                    double interpolatedY =
                            livingEntity.yo
                                    + (livingEntity.getY() - livingEntity.yo)
                                            * mc.getDeltaTracker()
                                                    .getGameTimeDeltaPartialTick(false);
                    double interpolatedZ =
                            livingEntity.zo
                                    + (livingEntity.getZ() - livingEntity.zo)
                                            * mc.getDeltaTracker()
                                                    .getGameTimeDeltaPartialTick(false);
                    Vec3 vector3 =
                            ScreenProjector.worldToScreen(
                                    new Vec3(
                                            interpolatedX,
                                            interpolatedY + livingEntity.getBbHeight() + 0.2,
                                            interpolatedZ));
                    if (!(vector3.z < 0.0) && !(vector3.z > 1.0)) {
                        float distance = mc.player.distanceTo(livingEntity);
                        float tagScale =
                                this.parent.baseScale.getValue() * (1.0F + distance / 10000.0F);
                        String murdererPrefix;
                        if (livingEntity instanceof Player player2
                                && MurderMystery.isMurderer(player2)) {

                            murdererPrefix = "§c⚔ §r";
                        } else {
                            murdererPrefix = "";
                        }

                        String displayName = livingEntity.getDisplayName().getString();
                        String healthText =
                                this.parent.showHealth.getValue()
                                        ? " "
                                                + ColorUtil.healthColorCode(livingEntity)
                                                + integerFormat.format(livingEntity.getHealth())
                                        : "";
                        String distanceText;
                        if (this.parent.showDistance.getValue()) {
                            distanceText = integerFormat.format(distance);

                            distanceText = " §7" + distanceText + "m";
                        } else {
                            distanceText = "";
                        }

                        String tagText = murdererPrefix + displayName + healthText + distanceText;
                        float textWidth = font.getStringWidth(tagText);
                        float fontSize = font.getSize();
                        boolean drawHead =
                                this.parent.showHead.getValue() && livingEntity instanceof Player;
                        float headSize = drawHead ? fontSize + 2.0F : 0.0F;
                        float headGap = drawHead ? 3.0F : 0.0F;
                        float tagWidth = textWidth + 10.0F + headSize + headGap;
                        float tagHeight = fontSize + 7.0F;
                        float screenX = (float) vector3.x;
                        float screenY = (float) vector3.y;
                        if (Effects.isNameTagsBlur()) {
                            render2DEvent
                                    .getRenderEffects()
                                    .blurRoundedRectangle(
                                            screenX - tagWidth * tagScale / 2.0F,
                                            screenY - tagHeight * tagScale,
                                            tagWidth * tagScale,
                                            tagHeight * tagScale,
                                            4.0F * tagScale);
                        }

                        context.pushPose();
                        context.translate(screenX, screenY);
                        context.scale(tagScale, tagScale, 1.0F);
                        context.translate(-tagWidth / 2.0F, -tagHeight);
                        RoundedRectShape backgroundShape =
                                RoundedRectShape.obtain()
                                        .setBounds(0.0F, 0.0F, tagWidth, tagHeight)
                                        .setRadius(4.0F)
                                        .setShadowEnabled(Effects.isNameTagsShadow());

                        context.drawRect(backgroundShape.setColor(new Color(5, 5, 10, 160)));
                        if (drawHead) {
                            AbstractClientPlayer abstractClientPlayer =
                                    (AbstractClientPlayer) livingEntity;
                            GpuTexture gpuTexture =
                                    mc.getTextureManager()
                                            .getTexture(
                                                    abstractClientPlayer
                                                            .getSkin()
                                                            .body()
                                                            .texturePath())
                                            .getTexture();
                            float skinScale = headSize / 8.0F;
                            float skinTextureSize = 64.0F * skinScale;
                            float skinOriginX = 5.0F - 8.0F * skinScale;
                            float skinOriginY = 2.5F - 8.0F * skinScale;
                            context.drawTexture(
                                    TexturedRectShape.obtain()
                                            .setBounds(5.0F, 2.5F, headSize, headSize)
                                            .setTexture(gpuTexture)
                                            .useNearestFilter()
                                            .setUv(
                                                    (5.0F - skinOriginX) / skinTextureSize,
                                                    (2.5F - skinOriginY) / skinTextureSize,
                                                    (5.0F + headSize - skinOriginX)
                                                            / skinTextureSize,
                                                    (2.5F + headSize - skinOriginY)
                                                            / skinTextureSize)
                                            .setRadius(2.0F)
                                            .setColor(Color.WHITE));
                        }

                        context.drawText(
                                TextDrawRequest.obtain()
                                        .setFont(font.getMsdfFont())
                                        .setText(tagText)
                                        .setPosition(5.0F + headSize + headGap, 2.0F)
                                        .setFontSize(fontSize)
                                        .enableShadow()
                                        .setColor(Color.WHITE));
                        context.popPose();
                    }
                }
            }
        }
    }

    static {
        integerFormat = new DecimalFormat("0");
    }
}
