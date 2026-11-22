package astralis.module.impl.targethud.mode;

import astralis.Astralis;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.TargetHUD;
import astralis.module.impl.Theme;
import astralis.ui.animation.Animation;
import astralis.ui.hud.Draggable;
import astralis.util.Easing;
import astralis.util.Timer;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.List;

public class TargetHudPulsive extends TargetHudMode {
    private static final float WIDTH = 130.0F;
    private static final float HEIGHT = 47.0F;
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float CORNER_RADIUS = 6.0F;
    private static final float PADDING = 3.0F;
    private final LivingEntity[] targets;
    private final float[] previousHealths;
    private final float[] healthFractions;
    private final float[] delayedHealthFractions;
    private final float[] absorptionFractions;
    private final boolean[] damageTrailActive;
    private final Animation[] appearAnimations;
    private final Animation[] hurtAnimations;
    private final Animation[] healthAnimations;
    private final Animation[] healthTrailAnimations;
    private final Timer[] damageTimers;
    private final Draggable hudElement;

    public TargetHudPulsive(TargetHUD targetHUD) {

        super("Pulsive", targetHUD);
        this.targets = new LivingEntity[4];
        this.previousHealths = new float[4];
        this.healthFractions = new float[4];
        this.delayedHealthFractions = new float[4];
        this.absorptionFractions = new float[4];
        this.damageTrailActive = new boolean[4];
        this.appearAnimations = new Animation[4];
        this.hurtAnimations = new Animation[4];
        this.healthAnimations = new Animation[4];
        this.healthTrailAnimations = new Animation[4];
        this.damageTimers = new Timer[4];

        for (int i = 0; i < 4; i++) {
            this.previousHealths[i] = -1.0F;

            this.appearAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 250L);

            this.hurtAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 150L);

            this.healthAnimations[i] = new Animation(Easing.EASE_OUT_QUART, 220L);

            this.healthTrailAnimations[i] = new Animation(Easing.EASE_OUT_CUBIC, 200L);
            this.damageTimers[i] = new Timer();
        }

        this.hudElement = new Draggable("Target HUD", 130.0F, 47.0F, 300.0F, 300.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return targetHUD.isEnabled()
                            && this.targets[0] != null
                            && this.appearAnimations[0].getValue() > 0.0
                            && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
    }

    @Override
    public void renderSlot(Render2DEvent render2DEvent, int slot) {

        float appearProgress = (float) this.appearAnimations[slot].getValue();
        if (this.targets[slot] != null && !(appearProgress <= 0.0F)) {

            SizedFont titleFont = FontManager.getThemedFont("Product Sans Medium", 7.0F);

            SizedFont nameFont = FontManager.getThemedFont("Product Sans Bold", 11.0F);

            SizedFont healthFont = FontManager.getThemedFont("Product Sans Medium", 8.0F);
            float[] slotPosition = this.getSlotPosition(this.hudElement, slot, 130.0F, 47.0F);
            float x = slotPosition[0];
            float y = slotPosition[1];
            float scale = this.hudElement.getScale();
            int alpha = (int) (appearProgress * 255.0F);
            int backgroundAlpha = Math.min(alpha, Theme.getBackgroundAlpha());
            if (Effects.isTargetHudBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangleWithAlpha(
                                x, y, 130.0F * scale, 47.0F * scale, 6.0F * scale, appearProgress);
            }

            render2DEvent.getContext().pushPose();
            render2DEvent.getContext().translate(x, y);
            render2DEvent.getContext().scale(scale, scale, 1.0F);
            Render2DContext backgroundContext = render2DEvent.getContext();
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(0.0F, 0.0F, 130.0F, 47.0F)
                            .setRadius(6.0F)
                            .setShadow(Effects.isTargetHudShadow(), alpha);

            backgroundContext.drawRect(
                    backgroundShape.setColor(
                            ColorUtil.withAlpha(new Color(20, 20, 26), backgroundAlpha)));
            Render2DContext headerContext = render2DEvent.getContext();
            RoundedRectShape headerShape =
                    RoundedRectShape.obtain().setBounds(0.0F, 0.0F, 130.0F, 15.0F).setRadius(6.0F);

            headerContext.drawRect(
                    headerShape.setColor(
                            ColorUtil.withAlpha(new Color(38, 38, 46), backgroundAlpha)));
            Render2DContext leftCornerContext = render2DEvent.getContext();
            RoundedRectShape leftCornerShape =
                    RoundedRectShape.obtain().setBounds(0.0F, 6.0F, 6.0F, 9.0F);

            leftCornerContext.drawRect(
                    leftCornerShape.setColor(
                            ColorUtil.withAlpha(new Color(38, 38, 46), backgroundAlpha)));
            Render2DContext rightCornerContext = render2DEvent.getContext();
            RoundedRectShape rightCornerShape =
                    RoundedRectShape.obtain().setBounds(124.0F, 6.0F, 6.0F, 9.0F);

            rightCornerContext.drawRect(
                    rightCornerShape.setColor(
                            ColorUtil.withAlpha(new Color(38, 38, 46), backgroundAlpha)));

            float titleWidth = titleFont.getStringWidth("target hud");
            float titleX = (130.0F - titleWidth) / 2.0F;
            float titleY = (15.0F - titleFont.getLineHeight()) / 2.0F + 0.5F;
            Render2DContext titleContext = render2DEvent.getContext();
            TextDrawRequest titleRequest = TextDrawRequest.obtain();

            titleContext.drawText(
                    titleRequest
                            .setText("target hud")
                            .setPosition(titleX, titleY)
                            .setFont(titleFont.getMsdfFont())
                            .setFontSize(titleFont.getSize())
                            .enableShadow()
                            .setPaint(this.createTitlePaint(titleX, titleY, titleWidth, alpha)));
            float iconSize = nameFont.getLineHeight() + healthFont.getLineHeight() - 1.0F;
            float hurtProgress = (float) this.hurtAnimations[slot].getValue();
            float shrunkIconSize = iconSize * (1.0F - 0.18F * hurtProgress);
            float iconInset = (iconSize - shrunkIconSize) / 2.0F;

            Color color =
                    ColorUtil.withAlpha(
                            new Color(
                                    255,
                                    (int) (255.0F * (1.0F - hurtProgress)),
                                    (int) (255.0F * (1.0F - hurtProgress))),
                            alpha);
            if (this.targets[slot] instanceof Player) {
                AbstractClientPlayer abstractClientPlayer =
                        (AbstractClientPlayer) this.targets[slot];
                GpuTexture gpuTexture =
                        mc.getTextureManager()
                                .getTexture(abstractClientPlayer.getSkin().body().texturePath())
                                .getTexture();
                render2DEvent
                        .getContext()
                        .drawTexture(
                                TexturedRectShape.obtain()
                                        .setBounds(
                                                3.0F + iconInset,
                                                18.0F + iconInset,
                                                shrunkIconSize,
                                                shrunkIconSize)
                                        .setTexture(gpuTexture)
                                        .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                                        .setRadius(3.0F)
                                        .useNearestFilter()
                                        .setColor(this.getHurtTint(this.targets[slot], alpha)));
            } else {
                float hurtFactor =
                        this.targets[slot].hurtTime > 0
                                ? Math.min(1.0F, this.targets[slot].hurtTime / 5.0F)
                                : 0.0F;
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(
                                                3.0F + iconInset,
                                                18.0F + iconInset,
                                                shrunkIconSize,
                                                shrunkIconSize)
                                        .setRadius(3.0F)
                                        .setColor(
                                                ColorUtil.withAlpha(
                                                        new Color(
                                                                (int) (60.0F + 195.0F * hurtFactor),
                                                                (int) (60.0F * (1.0F - hurtFactor)),
                                                                (int)
                                                                        (70.0F
                                                                                * (1.0F
                                                                                        - hurtFactor))),
                                                        alpha)));
            }

            float textX = 3.0F + iconSize + 2.0F;
            String rawName = this.targets[slot].getName().getString();

            String displayName = rawName.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            float maxNameWidth = 130.0F - textX - 3.0F;
            if (nameFont.getStringWidth(displayName) > maxNameWidth) {
                while (displayName.length() > 1) {

                    if (!(nameFont.getStringWidth(displayName + "...") > maxNameWidth)) {
                        break;
                    }

                    displayName = displayName.substring(0, displayName.length() - 1);
                }

                displayName = displayName + "...";
            }

            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(displayName)
                                    .setPosition(textX, 18.0F)
                                    .setFont(nameFont.getMsdfFont())
                                    .setFontSize(nameFont.getSize())
                                    .enableShadow()
                                    .setColor(ColorUtil.withAlpha(Color.WHITE, alpha).getRGB()));
            float animatedHealth = (float) this.healthAnimations[slot].getValue();
            Render2DContext healthContext = render2DEvent.getContext();
            TextDrawRequest healthRequest = TextDrawRequest.obtain();
            int displayedHealth = (int) Math.ceil(animatedHealth);
            TextDrawRequest styledHealthRequest =
                    healthRequest
                            .setText("Health: " + displayedHealth)
                            .setPosition(textX, 18.0F + nameFont.getLineHeight() - 1.0F)
                            .setFont(healthFont.getMsdfFont())
                            .setFontSize(healthFont.getSize())
                            .enableShadow();

            healthContext.drawText(
                    styledHealthRequest.setColor(
                            ColorUtil.withAlpha(new Color(170, 170, 180), alpha).getRGB()));
            Render2DContext trackContext = render2DEvent.getContext();
            RoundedRectShape trackShape =
                    RoundedRectShape.obtain().setBounds(3.0F, 42.0F, 124.0F, 3.0F).setRadius(1.5F);

            trackContext.drawRect(
                    trackShape.setColor(ColorUtil.withAlpha(new Color(35, 35, 42), alpha)));
            float healthBarWidth = 124.0F * this.healthFractions[slot];
            float delayedHealthBarWidth = 124.0F * this.delayedHealthFractions[slot];
            if (delayedHealthBarWidth > 0.001F) {
                Color[] themeColors = this.getThemeColors();
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(3.0F, 42.0F, delayedHealthBarWidth, 3.0F)
                                        .setRadius(1.5F)
                                        .setColor(
                                                ColorUtil.withAlpha(
                                                        ColorUtil.lerpColor(
                                                                themeColors[0], Color.BLACK, 0.2F),
                                                        alpha)));
            }

            if (healthBarWidth > 0.0F) {
                Color[] themeColors = this.getThemeColors();
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(3.0F, 42.0F, healthBarWidth, 3.0F)
                                        .setRadius(1.5F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setHorizontalGradient(
                                                                ColorUtil.withAlpha(
                                                                                new Color(
                                                                                        ColorUtil
                                                                                                .waveBlend(
                                                                                                        x
                                                                                                                + 3.0F,
                                                                                                        themeColors[
                                                                                                                0]
                                                                                                                .getRGB(),
                                                                                                        themeColors[
                                                                                                                1]
                                                                                                                .getRGB())),
                                                                                alpha)
                                                                        .getRGB(),
                                                                ColorUtil.withAlpha(
                                                                                new Color(
                                                                                        ColorUtil
                                                                                                .waveBlend(
                                                                                                        x
                                                                                                                + 3.0F
                                                                                                                + 124.0F,
                                                                                                        themeColors[
                                                                                                                0]
                                                                                                                .getRGB(),
                                                                                                        themeColors[
                                                                                                                1]
                                                                                                                .getRGB())),
                                                                                alpha)
                                                                        .getRGB())));
            }

            float absorptionBarWidth = 124.0F * this.absorptionFractions[slot];
            if (absorptionBarWidth > 0.001F) {
                Render2DContext absorptionContext = render2DEvent.getContext();
                RoundedRectShape absorptionShape =
                        RoundedRectShape.obtain()
                                .setBounds(3.0F, 42.0F, absorptionBarWidth, 3.0F)
                                .setRadius(1.5F);

                absorptionContext.drawRect(
                        absorptionShape.setColor(
                                ColorUtil.withAlpha(new Color(255, 255, 50), alpha)));
            }

            render2DEvent.getContext().popPose();
        }
    }

    private ShapePaint createTitlePaint(float x, float y, float width, int alpha) {
        Color[] themeColors = this.getThemeColors();
        return ShapePaint.obtain()
                .setLinearGradient(
                        ColorUtil.withAlpha(
                                        new Color(
                                                ColorUtil.waveBlend(
                                                        x,
                                                        themeColors[0].getRGB(),
                                                        themeColors[1].getRGB())),
                                        alpha)
                                .getRGB(),
                        ColorUtil.withAlpha(
                                        new Color(
                                                ColorUtil.waveBlend(
                                                        x + width,
                                                        themeColors[0].getRGB(),
                                                        themeColors[1].getRGB())),
                                        alpha)
                                .getRGB(),
                        x,
                        y,
                        x + width,
                        y);
    }

    @Override
    public void onDisable() {

        for (int i = 0; i < 4; i++) {
            this.targets[i] = null;
            this.previousHealths[i] = -1.0F;
            this.damageTrailActive[i] = false;
            this.healthFractions[i] = 0.0F;
            this.delayedHealthFractions[i] = 0.0F;
            this.absorptionFractions[i] = 0.0F;
        }
    }

    @Override
    public void updateTargets() {
        List displayTargets = this.parent.getDisplayTargets(this.targets[0]);

        for (int i = 0; i < 4; i++) {
            LivingEntity livingEntity =
                    i < displayTargets.size() ? (LivingEntity) displayTargets.get(i) : null;
            if (livingEntity != null && livingEntity != this.targets[i]) {
                this.healthAnimations[i].setValue(livingEntity.getHealth());
                this.healthAnimations[i].setTarget(livingEntity.getHealth());
                this.healthTrailAnimations[i].setValue(livingEntity.getHealth());
                this.healthTrailAnimations[i].setTarget(livingEntity.getHealth());
                this.previousHealths[i] = livingEntity.getHealth();
                this.damageTrailActive[i] = false;
                float safeMaxHealth = Math.max(livingEntity.getMaxHealth(), 1.0F);
                float initialFraction =
                        this.clampToUnit(
                                (livingEntity.getHealth() + livingEntity.getAbsorptionAmount())
                                        / safeMaxHealth);
                this.healthFractions[i] = initialFraction;
                this.delayedHealthFractions[i] = initialFraction;
                this.absorptionFractions[i] =
                        this.clampToUnit(livingEntity.getAbsorptionAmount() / safeMaxHealth);
            }

            if (livingEntity != null) {
                this.targets[i] = livingEntity;
            }

            this.appearAnimations[i].animateTo(livingEntity != null ? 1.0 : 0.0);
            if (this.targets[i] != null) {
                float health = this.targets[i].getHealth();
                float safeMaxHealth = Math.max(this.targets[i].getMaxHealth(), 1.0F);
                float healthFraction =
                        this.clampToUnit(
                                (health + this.targets[i].getAbsorptionAmount()) / safeMaxHealth);
                float absorptionFraction =
                        this.clampToUnit(this.targets[i].getAbsorptionAmount() / safeMaxHealth);
                this.healthFractions[i] =
                        this.healthFractions[i] + (healthFraction - this.healthFractions[i]) * 0.2F;
                this.delayedHealthFractions[i] =
                        this.delayedHealthFractions[i]
                                + (healthFraction - this.delayedHealthFractions[i]) * 0.05F;
                this.absorptionFractions[i] =
                        this.absorptionFractions[i]
                                + (absorptionFraction - this.absorptionFractions[i]) * 0.2F;
                if (Math.abs(this.healthFractions[i] - healthFraction) < 0.001F) {
                    this.healthFractions[i] = healthFraction;
                }

                if (Math.abs(this.delayedHealthFractions[i] - healthFraction) < 0.001F) {
                    this.delayedHealthFractions[i] = healthFraction;
                }

                if (Math.abs(this.absorptionFractions[i] - absorptionFraction) < 0.001F) {
                    this.absorptionFractions[i] = absorptionFraction;
                }

                if (health != this.previousHealths[i]) {
                    if (this.previousHealths[i] >= 0.0F && health < this.previousHealths[i]) {
                        this.healthTrailAnimations[i].setValue(this.previousHealths[i]);
                        this.healthTrailAnimations[i].setTarget(this.previousHealths[i]);
                        this.damageTimers[i].reset();
                        this.damageTrailActive[i] = true;
                    } else {
                        this.healthTrailAnimations[i].setValue(health);
                        this.healthTrailAnimations[i].setTarget(health);
                        this.damageTrailActive[i] = false;
                    }

                    this.previousHealths[i] = health;
                }

                label91:
                {
                    this.healthAnimations[i].animateTo(health);
                    if (this.damageTrailActive[i]) {
                        Timer damageTimer = this.damageTimers[i];

                        if (damageTimer.hasPassed(300L)) {
                            this.healthTrailAnimations[i].animateTo(health);
                            this.damageTrailActive[i] = false;
                            break label91;
                        }
                    }

                    this.healthTrailAnimations[i].animateTo(
                            this.healthTrailAnimations[i].getTarget());
                }

                this.hurtAnimations[i].animateTo(
                        livingEntity != null && livingEntity.hurtTime > 0 ? 0.8 : 0.0);
            }

            if (livingEntity == null && this.appearAnimations[i].getValue() <= 0.0) {
                this.targets[i] = null;
            }
        }
    }
}
