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
import java.text.DecimalFormat;
import java.util.List;

public class TargetHudAstralis extends TargetHudMode {
    private static final float WIDTH = 140.0F;
    private static final float HEIGHT = 40.0F;
    private static DecimalFormat healthFormat;
    private final LivingEntity[] targets;
    private final boolean[] slotActive;
    private final boolean[] damageTrailActive;
    private final double[] trackedHealths;
    private final float[] previousHealths;
    private final float[] healthFractions;
    private final float[] delayedHealthFractions;
    private final float[] absorptionFractions;
    private final Animation[] appearAnimations;
    private final Animation[] healthRatioAnimations;
    private final Animation[] hurtAnimations;
    private final Animation[] healthTrailAnimations;
    private final Timer[] damageTimers;
    private final Draggable hudElement;

    public TargetHudAstralis(TargetHUD targetHUD) {

        super("Astralis", targetHUD);
        this.targets = new LivingEntity[4];
        this.slotActive = new boolean[4];
        this.damageTrailActive = new boolean[4];
        this.trackedHealths = new double[4];
        this.previousHealths = new float[4];
        this.healthFractions = new float[4];
        this.delayedHealthFractions = new float[4];
        this.absorptionFractions = new float[4];
        this.appearAnimations = new Animation[4];
        this.healthRatioAnimations = new Animation[4];
        this.hurtAnimations = new Animation[4];
        this.healthTrailAnimations = new Animation[4];
        this.damageTimers = new Timer[4];

        for (int slot = 0; slot < 4; slot++) {
            this.previousHealths[slot] = -1.0F;

            this.appearAnimations[slot] = new Animation(Easing.EASE_OUT_BACK, 350L);

            this.healthRatioAnimations[slot] = new Animation(Easing.EASE_OUT_QUINT, 250L);

            this.hurtAnimations[slot] = new Animation(Easing.EASE_OUT_BACK, 220L);

            this.healthTrailAnimations[slot] = new Animation(Easing.EASE_OUT_CUBIC, 200L);
            this.damageTimers[slot] = new Timer();
        }

        this.hudElement = new Draggable("Target HUD", 140.0F, 40.0F, 300.0F, 300.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return targetHUD.isEnabled()
                            && this.targets[0] != null
                            && this.appearAnimations[0].getValue() > 0.05
                            && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
    }

    @Override
    public void renderSlot(Render2DEvent render2DEvent, int slot) {

        float appearProgress = (float) this.appearAnimations[slot].getValue();
        if (!(appearProgress <= 0.05F) || this.slotActive[slot]) {
            if (this.targets[slot] != null) {
                float[] slotPosition = this.getSlotPosition(this.hudElement, slot, 140.0F, 40.0F);
                float cardX = slotPosition[0];

                float cardY = slotPosition[1];
                float hudScale = this.hudElement.getScale();
                SizedFont nameFont = FontManager.getThemedFont("Product Sans Bold", 12.0F);

                SizedFont healthFont = FontManager.getThemedFont("Product Sans Regular", 8.0F);
                Color[] themeColors = this.getThemeColors();
                Color color = themeColors[0];
                Color color2 = themeColors[1];
                float scaledWidth = 140.0F * appearProgress;
                float scaledHeight = 40.0F * appearProgress;
                float offsetX = (140.0F - scaledWidth) / 2.0F;
                float offsetY = (40.0F - scaledHeight) / 2.0F;
                if (Effects.isTargetHudBlur()) {
                    render2DEvent
                            .getRenderEffects()
                            .blurRoundedRectangle(
                                    cardX + offsetX * hudScale,
                                    cardY + offsetY * hudScale,
                                    scaledWidth * hudScale,
                                    scaledHeight * hudScale,
                                    8.0F * hudScale);
                }

                render2DEvent.getContext().pushPose();
                render2DEvent.getContext().translate(cardX, cardY);
                render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
                render2DEvent.getContext().translate(70.0F, 20.0F);
                render2DEvent.getContext().scale(appearProgress, appearProgress, 1.0F);
                render2DEvent.getContext().translate(-70.0F, -20.0F);
                Render2DContext backgroundContext = render2DEvent.getContext();
                RoundedRectShape backgroundShape =
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, 140.0F, 40.0F)
                                .setRadius(8.0F)
                                .setShadowEnabled(Effects.isTargetHudShadow());

                long alphaValue;
                backgroundContext.drawRect(
                        backgroundShape.setColor(
                                new Color(
                                        5,
                                        5,
                                        10,
                                        Math.round(Theme.getBackgroundAlpha() * appearProgress))));
                float hurtProgress = (float) this.hurtAnimations[slot].getValue();
                float avatarSize = 28.0F * (1.0F - 0.08F * hurtProgress);
                float avatarInset = (28.0F - avatarSize) / 2.0F;
                float avatarX = 6.0F + avatarInset;
                float avatarY = 6.0F + avatarInset;
                Color color3 =
                        this.getHurtTint(this.targets[slot], (int) (appearProgress * 255.0F));
                if (this.targets[slot] instanceof Player) {
                    AbstractClientPlayer abstractClientPlayer =
                            (AbstractClientPlayer) this.targets[slot];
                    GpuTexture gpuTexture =
                            mc.getTextureManager()
                                    .getTexture(abstractClientPlayer.getSkin().body().texturePath())
                                    .getTexture();
                    float skinScale = avatarSize / 8.0F;
                    float skinTextureSize = 64.0F * skinScale;
                    float skinOriginX = avatarX - 8.0F * skinScale;
                    float skinOriginY = avatarY - 8.0F * skinScale;
                    render2DEvent
                            .getContext()
                            .drawTexture(
                                    TexturedRectShape.obtain()
                                            .setBounds(avatarX, avatarY, avatarSize, avatarSize)
                                            .setTexture(gpuTexture)
                                            .setUv(
                                                    (avatarX - skinOriginX) / skinTextureSize,
                                                    (avatarY - skinOriginY) / skinTextureSize,
                                                    (avatarX + avatarSize - skinOriginX)
                                                            / skinTextureSize,
                                                    (avatarY + avatarSize - skinOriginY)
                                                            / skinTextureSize)
                                            .setRadius(4.0F)
                                            .useNearestFilter()
                                            .setColor(color3));
                }

                String targetName = this.targets[slot].getName().getString();
                if (nameFont.getStringWidth(targetName) > 94.0F) {
                    int nameLength = targetName.length();

                    String truncatedName = targetName.substring(0, Math.min(nameLength, 12));
                    targetName = truncatedName + "...";
                }

                float nameWidth = nameFont.getStringWidth(targetName);
                render2DEvent
                        .getContext()
                        .drawText(
                                TextDrawRequest.obtain()
                                        .setText(targetName)
                                        .setPosition(40.0F, 5.0F)
                                        .setFont(nameFont.getMsdfFont())
                                        .setFontSize(nameFont.getSize())
                                        .enableShadow()
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setLinearGradient(
                                                                ColorUtil.waveBlend(
                                                                        cardX + 40.0F,
                                                                        color.getRGB(),
                                                                        color2.getRGB()),
                                                                ColorUtil.waveBlend(
                                                                        cardX + 40.0F + nameWidth,
                                                                        color.getRGB(),
                                                                        color2.getRGB()),
                                                                40.0F,
                                                                6.0F,
                                                                40.0F + nameWidth,
                                                                6.0F)));
                Render2DContext healthContext = render2DEvent.getContext();
                TextDrawRequest healthRequest =
                        TextDrawRequest.obtain()
                                .setText(healthFormat.format(this.targets[slot].getHealth()))
                                .setPosition(40.0F, 18.0F)
                                .setFont(healthFont.getMsdfFont())
                                .setFontSize(healthFont.getSize())
                                .enableShadow();

                healthContext.drawText(healthRequest.setColor(new Color(220, 220, 230).getRGB()));
                float delayedBarWidth = 94.0F * this.delayedHealthFractions[slot];
                float healthBarWidth = 94.0F * this.healthFractions[slot];
                Render2DContext barContext = render2DEvent.getContext();
                RoundedRectShape barShape =
                        RoundedRectShape.obtain()
                                .setBounds(40.0F, 28.0F, 94.0F, 6.0F)
                                .setRadius(3.0F);

                barContext.drawRect(barShape.setColor(new Color(30, 30, 35, 150)));
                if (delayedBarWidth > 0.001F) {
                    Render2DContext delayedBarContext = render2DEvent.getContext();
                    RoundedRectShape delayedBarShape =
                            RoundedRectShape.obtain()
                                    .setBounds(40.0F, 28.0F, delayedBarWidth, 6.0F)
                                    .setRadius(3.0F);
                    Color color4 = ColorUtil.lerpColor(color, Color.BLACK, 0.2F);
                    delayedBarContext.drawRect(
                            delayedBarShape.setColor(ColorUtil.withAlpha(color4, 255)));
                }

                if (healthBarWidth > 0.0F) {
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(40.0F, 28.0F, healthBarWidth, 6.0F)
                                            .setRadius(3.0F)
                                            .setColor(color));
                }

                float absorptionBarWidth = 94.0F * this.absorptionFractions[slot];
                if (absorptionBarWidth > 0.001F) {
                    Render2DContext absorptionContext = render2DEvent.getContext();
                    RoundedRectShape absorptionShape =
                            RoundedRectShape.obtain()
                                    .setBounds(40.0F, 28.0F, absorptionBarWidth, 6.0F)
                                    .setRadius(3.0F);

                    alphaValue = (int) (appearProgress * 255.0F);

                    absorptionContext.drawRect(
                            absorptionShape.setColor(
                                    new Color(255, 255, 50, Math.clamp(alphaValue, 0, 255))));
                }

                render2DEvent.getContext().popPose();
            }
        }
    }

    @Override
    public void onDisable() {

        for (int slot = 0; slot < 4; slot++) {
            this.targets[slot] = null;
            this.slotActive[slot] = false;
            this.trackedHealths[slot] = 0.0;
            this.previousHealths[slot] = -1.0F;
            this.damageTrailActive[slot] = false;
            this.healthFractions[slot] = 0.0F;
            this.delayedHealthFractions[slot] = 0.0F;
            this.absorptionFractions[slot] = 0.0F;
        }
    }

    @Override
    public void updateTargets() {
        long duration;
        List displayTargets = this.parent.getDisplayTargets(this.targets[0]);

        for (int slot = 0; slot < 4; slot++) {
            LivingEntity livingEntity =
                    slot < displayTargets.size() ? (LivingEntity) displayTargets.get(slot) : null;
            if (livingEntity != null && livingEntity != this.targets[slot]) {
                double initialHealthRatio =
                        livingEntity.getMaxHealth() > 0.0F
                                ? livingEntity.getHealth() / livingEntity.getMaxHealth()
                                : 0.0;
                this.healthRatioAnimations[slot].setStartValue(initialHealthRatio);
                this.healthRatioAnimations[slot].setTarget(initialHealthRatio);
                this.healthTrailAnimations[slot].setValue(livingEntity.getHealth());
                this.healthTrailAnimations[slot].setTarget(livingEntity.getHealth());
                this.trackedHealths[slot] = livingEntity.getHealth();
                this.previousHealths[slot] = livingEntity.getHealth();
                this.damageTrailActive[slot] = false;
                float initialMaxHealth = Math.max(livingEntity.getMaxHealth(), 1.0F);
                float initialFraction =
                        this.clampToUnit(
                                (livingEntity.getHealth() + livingEntity.getAbsorptionAmount())
                                        / initialMaxHealth);
                this.healthFractions[slot] = initialFraction;
                this.delayedHealthFractions[slot] = initialFraction;
                this.absorptionFractions[slot] =
                        this.clampToUnit(livingEntity.getAbsorptionAmount() / initialMaxHealth);
            }

            if (livingEntity != null) {
                this.targets[slot] = livingEntity;
            }

            this.slotActive[slot] = livingEntity != null;
            Animation appearAnimation = this.appearAnimations[slot];
            if (this.slotActive[slot]) {

                duration = 350L;
            } else {

                duration = 250L;
            }

            appearAnimation.setDuration(duration);
            this.appearAnimations[slot].setEasing(
                    this.slotActive[slot] ? Easing.EASE_OUT_BACK : Easing.EASE_IN_QUAD);
            this.appearAnimations[slot].animateTo(this.slotActive[slot] ? 1.0 : 0.0);
            if (this.targets[slot] != null) {
                float health = this.targets[slot].getHealth();
                float maxHealth = this.targets[slot].getMaxHealth();
                double healthRatio = maxHealth > 0.0F ? health / maxHealth : 0.0;
                float targetFraction =
                        this.clampToUnit(
                                (health + this.targets[slot].getAbsorptionAmount())
                                        / Math.max(maxHealth, 1.0F));
                float absorptionFraction =
                        this.clampToUnit(
                                this.targets[slot].getAbsorptionAmount()
                                        / Math.max(maxHealth, 1.0F));
                this.healthFractions[slot] =
                        this.healthFractions[slot]
                                + (targetFraction - this.healthFractions[slot]) * 0.2F;
                this.delayedHealthFractions[slot] =
                        this.delayedHealthFractions[slot]
                                + (targetFraction - this.delayedHealthFractions[slot]) * 0.05F;
                this.absorptionFractions[slot] =
                        this.absorptionFractions[slot]
                                + (absorptionFraction - this.absorptionFractions[slot]) * 0.2F;
                if (Math.abs(this.healthFractions[slot] - targetFraction) < 0.001F) {
                    this.healthFractions[slot] = targetFraction;
                }

                if (Math.abs(this.delayedHealthFractions[slot] - targetFraction) < 0.001F) {
                    this.delayedHealthFractions[slot] = targetFraction;
                }

                if (Math.abs(this.absorptionFractions[slot] - absorptionFraction) < 0.001F) {
                    this.absorptionFractions[slot] = absorptionFraction;
                }

                if (Math.abs(health - this.trackedHealths[slot]) > 0.1) {
                    this.healthRatioAnimations[slot].setStartValue(
                            this.healthRatioAnimations[slot].getValue());
                    this.healthRatioAnimations[slot].setTarget(healthRatio);
                    this.healthRatioAnimations[slot].restart();
                    if (this.previousHealths[slot] >= 0.0F && health < this.previousHealths[slot]) {
                        this.healthTrailAnimations[slot].setValue(this.previousHealths[slot]);
                        this.healthTrailAnimations[slot].setTarget(this.previousHealths[slot]);
                        this.damageTimers[slot].reset();
                        this.damageTrailActive[slot] = true;
                        this.hurtAnimations[slot].setValue(1.0);
                        this.hurtAnimations[slot].setStartValue(1.0);
                        this.hurtAnimations[slot].setTarget(0.0);
                        this.hurtAnimations[slot].restart();
                    } else {
                        this.healthTrailAnimations[slot].setValue(health);
                        this.healthTrailAnimations[slot].setTarget(health);
                        this.damageTrailActive[slot] = false;
                    }

                    this.trackedHealths[slot] = health;
                    this.previousHealths[slot] = health;
                }

                label111:
                {
                    this.healthRatioAnimations[slot].animateTo(healthRatio);
                    if (this.damageTrailActive[slot]) {
                        Timer damageTimer = this.damageTimers[slot];

                        if (damageTimer.hasPassed(300L)) {
                            this.healthTrailAnimations[slot].animateTo(health);
                            this.damageTrailActive[slot] = false;
                            break label111;
                        }
                    }

                    this.healthTrailAnimations[slot].animateTo(
                            this.healthTrailAnimations[slot].getTarget());
                }

                this.hurtAnimations[slot].animateTo(0.0);
            }

            if (livingEntity == null && this.appearAnimations[slot].getValue() <= 0.05F) {
                this.targets[slot] = null;
            }
        }
    }

    static {
        healthFormat = new DecimalFormat("0.0");
    }
}
