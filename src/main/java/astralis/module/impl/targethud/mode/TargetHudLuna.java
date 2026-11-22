package astralis.module.impl.targethud.mode;

import astralis.Astralis;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.TargetHUD;
import astralis.module.impl.Theme;
import astralis.setting.BooleanSetting;
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

public class TargetHudLuna extends TargetHudMode {
    private static final float WIDTH = 150.0F;
    private static final float HEIGHT = 45.0F;
    private static final float BAR_WIDTH = 100.0F;
    private static DecimalFormat healthFormat;
    private final LivingEntity[] targets;
    private final float[] previousHealths;
    private final float[] previousArmors;
    private final float[] healthFractions;
    private final float[] delayedHealthFractions;
    private final float[] absorptionFractions;
    private final boolean[] damageTrailActive;
    private final Animation[] appearAnimations;
    private final Animation[] healthRatioAnimations;
    private final Animation[] armorAnimations;
    private final Animation[] hurtAnimations;
    private final Animation[] healthTrailAnimations;
    private final Timer[] damageTimers;
    private final BooleanSetting damageAnimation;
    private final BooleanSetting healthPercentage;
    private final Draggable hudElement;

    public TargetHudLuna(TargetHUD targetHUD) {

        super("Luna", targetHUD);
        this.targets = new LivingEntity[4];
        this.previousHealths = new float[4];
        this.previousArmors = new float[4];
        this.healthFractions = new float[4];
        this.delayedHealthFractions = new float[4];
        this.absorptionFractions = new float[4];
        this.damageTrailActive = new boolean[4];
        this.appearAnimations = new Animation[4];
        this.healthRatioAnimations = new Animation[4];
        this.armorAnimations = new Animation[4];
        this.hurtAnimations = new Animation[4];
        this.healthTrailAnimations = new Animation[4];
        this.damageTimers = new Timer[4];

        this.damageAnimation = new BooleanSetting("c7a3d81ced8c63e5a40ea5b56fc8bd27", true);

        this.healthPercentage =
                new BooleanSetting(
                        "37eb655f86303df6542483666318a39530adfaee2a993919e993dbb4caef6f57", false);

        for (int i = 0; i < 4; i++) {
            this.previousHealths[i] = -1.0F;

            this.appearAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 250L);

            this.healthRatioAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 350L);

            this.armorAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 350L);

            this.hurtAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 450L);

            this.healthTrailAnimations[i] = new Animation(Easing.EASE_OUT_CUBIC, 200L);
            this.damageTimers[i] = new Timer();
        }

        this.hudElement = new Draggable("Target HUD", 150.0F, 45.0F, 300.0F, 300.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return targetHUD.isEnabled()
                            && this.targets[0] != null
                            && this.appearAnimations[0].getValue() > 0.0
                            && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        this.addSettings(this.damageAnimation, this.healthPercentage);
    }

    @Override
    public void renderSlot(Render2DEvent render2DEvent, int slot) {

        float appearProgress = (float) this.appearAnimations[slot].getValue();
        if (this.targets[slot] != null && !(appearProgress <= 0.0F)) {
            float[] slotPosition = this.getSlotPosition(this.hudElement, slot, 150.0F, 45.0F);
            float x = slotPosition[0];

            float y = slotPosition[1];
            float scale = this.hudElement.getScale();
            SizedFont nameFont = FontManager.getThemedFont("Product Sans Medium", 11.0F);

            SizedFont healthFont = FontManager.getThemedFont("Product Sans Regular", 9.0F);
            Color[] themeColors = this.getThemeColors();
            Color color = themeColors[0];
            Color color2 = themeColors[1];
            float health = this.targets[slot].getHealth();
            float maxHealth = this.targets[slot].getMaxHealth();
            float healthRatio = maxHealth > 0.0F ? health / maxHealth : 0.0F;
            float healthBarWidth = 100.0F * this.healthFractions[slot];
            float armorBarWidth =
                    100.0F * (float) Math.clamp(this.armorAnimations[slot].getValue(), 0.0, 1.0);
            float hurtProgress =
                    this.damageAnimation.getValue()
                            ? (float) Math.max(this.hurtAnimations[slot].getValue(), 0.0)
                            : 0.0F;
            float delayedHealthBarWidth = 100.0F * this.delayedHealthFractions[slot];
            int alpha = (int) (appearProgress * 255.0F);
            int gradientStart =
                    ColorUtil.withAlpha(
                                    new Color(
                                            ColorUtil.waveBlend(
                                                    x + 45.0F * scale,
                                                    color.getRGB(),
                                                    color2.getRGB()),
                                            true),
                                    alpha)
                            .getRGB();
            int gradientEnd =
                    ColorUtil.withAlpha(
                                    new Color(
                                            ColorUtil.waveBlend(
                                                    x + 145.0F * scale,
                                                    color.getRGB(),
                                                    color2.getRGB()),
                                            true),
                                    alpha)
                            .getRGB();
            if (Effects.isTargetHudBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangleWithAlpha(
                                x, y, 150.0F * scale, 45.0F * scale, 8.0F * scale, appearProgress);
            }

            render2DEvent.getContext().pushPose();
            render2DEvent.getContext().translate(x, y);
            render2DEvent.getContext().scale(scale, scale, 1.0F);
            Render2DContext backgroundContext = render2DEvent.getContext();
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(0.0F, 0.0F, 150.0F, 45.0F)
                            .setRadius(8.0F)
                            .setShadow(Effects.isTargetHudShadow(), alpha);

            backgroundContext.drawRect(
                    backgroundShape.setColor(
                            new Color(
                                    25,
                                    25,
                                    25,
                                    Math.round(Theme.getBackgroundAlpha() * appearProgress))));
            Render2DContext healthTrackContext = render2DEvent.getContext();
            RoundedRectShape healthTrackShape =
                    RoundedRectShape.obtain().setBounds(45.0F, 27.5F, 100.0F, 5.0F).setRadius(2.0F);

            healthTrackContext.drawRect(healthTrackShape.setColor(new Color(38, 38, 38, alpha)));
            if (delayedHealthBarWidth > 0.001F) {
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(45.0F, 27.5F, delayedHealthBarWidth, 5.0F)
                                        .setRadius(2.0F)
                                        .setColor(
                                                ColorUtil.withAlpha(
                                                        ColorUtil.lerpColor(
                                                                color, Color.BLACK, 0.2F),
                                                        alpha)));
            }

            if (healthBarWidth > 0.0F) {
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(45.0F, 27.5F, healthBarWidth, 5.0F)
                                        .setRadius(2.0F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setHorizontalGradient(
                                                                gradientStart, gradientEnd)));
            }

            float absorptionBarWidth = 100.0F * this.absorptionFractions[slot];
            if (absorptionBarWidth > 0.001F) {
                Render2DContext absorptionContext = render2DEvent.getContext();
                RoundedRectShape absorptionShape =
                        RoundedRectShape.obtain()
                                .setBounds(45.0F, 27.5F, absorptionBarWidth, 5.0F)
                                .setRadius(2.0F);

                absorptionContext.drawRect(
                        absorptionShape.setColor(
                                ColorUtil.withAlpha(new Color(255, 255, 50), alpha)));
            }

            Render2DContext armorTrackContext = render2DEvent.getContext();
            RoundedRectShape armorTrackShape =
                    RoundedRectShape.obtain().setBounds(45.0F, 35.0F, 100.0F, 5.0F).setRadius(2.0F);

            armorTrackContext.drawRect(armorTrackShape.setColor(new Color(38, 38, 38, alpha)));
            if (armorBarWidth > 0.0F) {
                Render2DContext armorContext = render2DEvent.getContext();
                RoundedRectShape armorShape =
                        RoundedRectShape.obtain()
                                .setBounds(45.0F, 35.0F, armorBarWidth, 5.0F)
                                .setRadius(2.0F);
                ShapePaint armorPaint = ShapePaint.obtain();

                int armorGradientStart = new Color(25, 229, 255, alpha).getRGB();

                armorContext.drawRect(
                        armorShape.setPaint(
                                armorPaint.setHorizontalGradient(
                                        armorGradientStart,
                                        new Color(127, 229, 255, alpha).getRGB())));
            }

            Render2DContext nameContext = render2DEvent.getContext();
            TextDrawRequest nameRequest = TextDrawRequest.obtain();
            String targetName = this.targets[slot].getName().getString();
            nameContext.drawText(
                    nameRequest
                            .setText(targetName.replaceAll("§[0-9a-fk-orA-FK-OR]", ""))
                            .setPosition(45.0F, 3.0F)
                            .setFont(nameFont.getMsdfFont())
                            .setFontSize(nameFont.getSize())
                            .enableShadow()
                            .setColor(ColorUtil.withAlpha(Color.WHITE, alpha).getRGB()));
            String formattedHealth;
            if (this.healthPercentage.getValue()) {

                formattedHealth = String.format("%d%%", (int) (healthRatio * 100.0F));
            } else {
                formattedHealth = healthFormat.format(health);
            }

            String healthText = formattedHealth;
            float healthTextWidth = healthFont.getStringWidth(healthText);
            float healthTextX =
                    Math.max(
                            45.0F,
                            Math.min(
                                    145.0F - healthTextWidth,
                                    45.0F + healthBarWidth - healthTextWidth / 2.0F));
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(healthText)
                                    .setPosition(healthTextX, 16.0F)
                                    .setFont(healthFont.getMsdfFont())
                                    .setFontSize(healthFont.getSize())
                                    .setColor(ColorUtil.withAlpha(Color.WHITE, alpha).getRGB()));
            if (this.targets[slot] instanceof Player) {
                AbstractClientPlayer abstractClientPlayer =
                        (AbstractClientPlayer) this.targets[slot];
                GpuTexture gpuTexture =
                        mc.getTextureManager()
                                .getTexture(abstractClientPlayer.getSkin().body().texturePath())
                                .getTexture();
                float headSize = 37.5F + hurtProgress * 2.0F;
                float headOffset = 4.0F - hurtProgress;
                render2DEvent
                        .getContext()
                        .drawTexture(
                                TexturedRectShape.obtain()
                                        .setBounds(headOffset, headOffset, headSize, headSize)
                                        .setTexture(gpuTexture)
                                        .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                                        .setRadius(4.0F)
                                        .useNearestFilter()
                                        .setColor(this.getHurtTint(this.targets[slot], alpha)));
            }

            render2DEvent.getContext().popPose();
        }
    }

    @Override
    public void onDisable() {

        for (int i = 0; i < 4; i++) {
            this.targets[i] = null;
            this.previousHealths[i] = -1.0F;
            this.previousArmors[i] = 0.0F;
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
                this.healthRatioAnimations[i].setValue(
                        livingEntity.getHealth() / livingEntity.getMaxHealth());
                this.healthRatioAnimations[i].setTarget(
                        livingEntity.getHealth() / livingEntity.getMaxHealth());
                this.healthTrailAnimations[i].setValue(livingEntity.getHealth());
                this.healthTrailAnimations[i].setTarget(livingEntity.getHealth());
                this.previousHealths[i] = livingEntity.getHealth();
                this.previousArmors[i] = livingEntity.getArmorValue();
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
                float maxHealth = this.targets[i].getMaxHealth();
                float healthRatio = maxHealth > 0.0F ? health / maxHealth : 0.0F;
                float healthFraction =
                        this.clampToUnit(
                                (health + this.targets[i].getAbsorptionAmount())
                                        / Math.max(maxHealth, 1.0F));
                float absorptionFraction =
                        this.clampToUnit(
                                this.targets[i].getAbsorptionAmount() / Math.max(maxHealth, 1.0F));
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

                if (this.previousHealths[i] >= 0.0F
                        && Math.abs(health - this.previousHealths[i]) > 0.01F) {
                    this.healthRatioAnimations[i].setTarget(healthRatio);
                    this.healthRatioAnimations[i].restart();
                    if (this.damageAnimation.getValue() && health < this.previousHealths[i]) {
                        this.hurtAnimations[i].setValue(1.0);
                        this.hurtAnimations[i].setTarget(0.0);
                        this.hurtAnimations[i].restart();
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

                label97:
                {
                    this.healthRatioAnimations[i].animateTo(healthRatio);
                    this.hurtAnimations[i].animateTo(0.0);
                    if (this.damageTrailActive[i]) {
                        Timer damageTimer = this.damageTimers[i];

                        if (damageTimer.hasPassed(300L)) {
                            this.healthTrailAnimations[i].animateTo(health);
                            this.damageTrailActive[i] = false;
                            break label97;
                        }
                    }

                    this.healthTrailAnimations[i].animateTo(
                            this.healthTrailAnimations[i].getTarget());
                }

                float armor = this.targets[i].getArmorValue();
                if (Math.abs(armor - this.previousArmors[i]) > 0.01F) {
                    this.armorAnimations[i].setTarget(Math.min(armor, 20.0F) / 20.0F);
                    this.armorAnimations[i].restart();
                    this.previousArmors[i] = armor;
                }

                this.armorAnimations[i].animateTo(Math.min(armor, 20.0F) / 20.0F);
            }

            if (livingEntity == null && this.appearAnimations[i].getValue() <= 0.0) {
                this.targets[i] = null;
            }
        }
    }

    static {
        healthFormat = new DecimalFormat("0.0");
    }
}
