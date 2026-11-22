package astralis.module.impl.targethud.mode;

import astralis.Astralis;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.TargetHUD;
import astralis.module.impl.Theme;
import astralis.setting.ModeSetting;
import astralis.ui.animation.Animation;
import astralis.ui.hud.Draggable;
import astralis.util.Easing;
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

public class TargetHudLunaV2 extends TargetHudMode {
    private static final float HEIGHT = 35.0F;
    private final LivingEntity[] targets;
    private final float[] widths;
    private final float[] healthFractions;
    private final float[] delayedHealthFractions;
    private final float[] absorptionFractions;
    private final String[] cleanNames;
    private final float[] nameWidths;
    private final Animation[] appearAnimations;
    private final ModeSetting healthMode;
    private final Draggable hudElement;

    public TargetHudLunaV2(TargetHUD targetHUD) {

        super("Luna V2", targetHUD);
        this.targets = new LivingEntity[4];
        this.widths = new float[4];
        this.healthFractions = new float[4];
        this.delayedHealthFractions = new float[4];
        this.absorptionFractions = new float[4];
        this.cleanNames = new String[4];
        this.nameWidths = new float[4];
        this.appearAnimations = new Animation[4];

        ModeSetting modeSetting = new ModeSetting("ff38ea18ec506afeafbf9f72f627edd0");
        String[] modeNames = new String[2];

        modeNames[0] = "Health";

        modeNames[1] = "Percentage";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.healthMode = modeSetting2.setDefault("Health");

        for (int i = 0; i < 4; i++) {
            this.cleanNames[i] = "";
            this.widths[i] = 100.0F;
            this.appearAnimations[i] = new Animation(Easing.EASE_OUT_QUINT, 250L);
        }

        this.hudElement = new Draggable("Target HUD", 100.0F, 35.0F, 300.0F, 300.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return targetHUD.isEnabled()
                            && this.targets[0] != null
                            && this.appearAnimations[0].getValue() > 0.0
                            && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        this.addSettings(this.healthMode);
    }

    @Override
    public void renderSlot(Render2DEvent render2DEvent, int slot) {

        float appearProgress = (float) this.appearAnimations[slot].getValue();
        if (this.targets[slot] != null && !(appearProgress <= 0.0F)) {

            SizedFont nameFont = FontManager.getThemedFont("Product Sans Regular", 11.0F);

            SizedFont healthFont = FontManager.getThemedFont("Product Sans Regular", 10.0F);
            String rawName = this.targets[slot].getName().getString();

            String cleanName = rawName.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
            float hudWidth = this.widths[slot];
            float[] slotPosition = this.getSlotPosition(this.hudElement, slot, hudWidth, 35.0F);
            float x = slotPosition[0];
            float y = slotPosition[1];
            float scale = this.hudElement.getScale();
            boolean isPlayer = this.targets[slot] instanceof Player;
            float contentX = isPlayer ? 35.0F : 2.5F;
            float barWidth = hudWidth - 37.5F;
            float health = this.targets[slot].getHealth();
            float maxHealth = this.targets[slot].getMaxHealth();
            float healthRatio = maxHealth > 0.0F ? health / maxHealth : 0.0F;
            float healthBarWidth = barWidth * this.healthFractions[slot];
            float delayedHealthBarWidth = barWidth * this.delayedHealthFractions[slot];
            float absorptionBarWidth = barWidth * this.absorptionFractions[slot];
            Color[] themeColors = this.getThemeColors();
            Color color = themeColors[0];
            Color color2 = themeColors[1];
            int alpha = (int) (appearProgress * 255.0F);
            int gradientStart =
                    ColorUtil.withAlpha(
                                    new Color(
                                            ColorUtil.waveBlend(
                                                    x, color.getRGB(), color2.getRGB())),
                                    alpha)
                            .getRGB();
            int gradientEnd =
                    ColorUtil.withAlpha(
                                    new Color(
                                            ColorUtil.waveBlend(
                                                    x + hudWidth, color.getRGB(), color2.getRGB())),
                                    alpha)
                            .getRGB();
            render2DEvent.getContext().pushPose();
            render2DEvent.getContext().translate(x, y);
            render2DEvent.getContext().scale(scale, scale, 1.0F);
            Render2DContext backgroundContext = render2DEvent.getContext();
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(0.0F, 0.0F, hudWidth, 35.0F)
                            .setShadow(Effects.isTargetHudShadow(), alpha);

            backgroundContext.drawRect(
                    backgroundShape.setColor(
                            new Color(
                                    29,
                                    29,
                                    29,
                                    Math.round(Theme.getBackgroundAlpha() * appearProgress))));
            Render2DContext trackContext = render2DEvent.getContext();
            RoundedRectShape trackShape =
                    RoundedRectShape.obtain().setBounds(contentX, 25.0F, barWidth, 7.5F);

            trackContext.drawRect(
                    trackShape.setColor(
                            new Color(25, 25, 25, (int) (0.75F * appearProgress * 255.0F))));
            if (delayedHealthBarWidth > 0.001F) {
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(contentX, 25.0F, delayedHealthBarWidth, 7.5F)
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
                                        .setBounds(contentX, 25.0F, healthBarWidth, 7.5F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setHorizontalGradient(
                                                                gradientStart, gradientEnd)));
            }

            if (absorptionBarWidth > 0.001F) {
                Render2DContext absorptionContext = render2DEvent.getContext();
                RoundedRectShape absorptionShape =
                        RoundedRectShape.obtain()
                                .setBounds(contentX, 25.0F, absorptionBarWidth, 7.5F);

                absorptionContext.drawRect(
                        absorptionShape.setColor(
                                ColorUtil.withAlpha(new Color(255, 255, 50), alpha)));
            }

            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(cleanName)
                                    .setPosition(contentX, 2.0F)
                                    .setFont(nameFont.getMsdfFont())
                                    .setFontSize(nameFont.getSize())
                                    .setColor(ColorUtil.withAlpha(Color.WHITE, alpha).getRGB()));
            Render2DContext healthTextContext = render2DEvent.getContext();
            TextDrawRequest healthTextRequest = TextDrawRequest.obtain();
            String healthText;
            if (this.healthMode.is("Percentage")) {
                healthText = String.format("%d%%", (int) (healthRatio * 100.0F));
            } else {
                healthText = String.format("%.1f", health);
            }

            healthTextContext.drawText(
                    healthTextRequest
                            .setText(healthText)
                            .setPosition(contentX, 12.5F)
                            .setFont(healthFont.getMsdfFont())
                            .setFontSize(healthFont.getSize())
                            .setColor(ColorUtil.withAlpha(Color.WHITE, alpha).getRGB()));
            if (isPlayer) {
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
                                        .setBounds(2.5F, 2.5F, 30.0F, 30.0F)
                                        .setTexture(gpuTexture)
                                        .setUv(0.125F, 0.125F, 0.25F, 0.25F)
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
            this.healthFractions[i] = 0.0F;
            this.delayedHealthFractions[i] = 0.0F;
            this.absorptionFractions[i] = 0.0F;
        }
    }

    @Override
    public void updateTargets() {

        List displayTargets = this.parent.getDisplayTargets(this.targets[0]);

        SizedFont nameFont = FontManager.getThemedFont("Product Sans Regular", 11.0F);

        for (int i = 0; i < 4; i++) {
            LivingEntity livingEntity =
                    i < displayTargets.size() ? (LivingEntity) displayTargets.get(i) : null;
            if (livingEntity != null && livingEntity != this.targets[i]) {
                float safeMaxHealth = Math.max(livingEntity.getMaxHealth(), 1.0F);
                float healthFraction =
                        this.clampToUnit(
                                (livingEntity.getHealth() + livingEntity.getAbsorptionAmount())
                                        / safeMaxHealth);
                this.healthFractions[i] = healthFraction;
                this.delayedHealthFractions[i] = healthFraction;
                this.absorptionFractions[i] =
                        this.clampToUnit(livingEntity.getAbsorptionAmount() / safeMaxHealth);
            }

            if (livingEntity != null) {
                this.targets[i] = livingEntity;
            }

            this.appearAnimations[i].animateTo(livingEntity != null ? 1.0 : 0.0);
            if (this.targets[i] != null) {
                float safeMaxHealth = Math.max(this.targets[i].getMaxHealth(), 1.0F);
                float healthFraction =
                        this.clampToUnit(
                                (this.targets[i].getHealth()
                                                + this.targets[i].getAbsorptionAmount())
                                        / safeMaxHealth);
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
            }

            if (livingEntity == null && this.appearAnimations[i].getValue() <= 0.0) {
                this.targets[i] = null;
            }

            if (this.targets[i] != null) {
                String rawName = this.targets[i].getName().getString();

                String cleanName = rawName.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                if (!cleanName.equals(this.cleanNames[i])) {

                    this.nameWidths[i] =
                            nameFont.getStringWidth("Nam") + nameFont.getStringWidth(cleanName);
                    this.cleanNames[i] = cleanName;
                }

                this.widths[i] = this.nameWidths[i] + 40.0F;
                if (i == 0) {
                    this.hudElement.setWidth(this.widths[i]);
                    this.hudElement.setHeight(35.0F);
                }
            }
        }
    }
}
