package astralis.module.impl.targethud.mode;

import astralis.Astralis;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.TargetHUD;
import astralis.module.impl.Theme;
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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TargetHudAdjust extends TargetHudMode {
    private static DecimalFormat healthDiffFormat;
    private static final float PADDING = 3.0F;
    private static final float AVATAR_SIZE = 25.0F;
    private static final float AVATAR_GAP = 1.0F;
    private static final float HEIGHT = 35.0F;
    private static final float BAR_HEIGHT = 3.0F;
    private static final float ITEM_SIZE = 14.0F;
    private static final float ITEM_GAP = 1.0F;
    private static final float MIN_WIDTH = 130.0F;
    private final LivingEntity[] targets;
    private final AdjustCardInfo[] cardInfos;
    private final float[] healthFractions;
    private final float[] delayedHealthFractions;
    private final float[] absorptionFractions;
    private final Animation[] appearAnimations;
    private final List<ItemStack>[] equipmentItems;
    private final Draggable hudElement;

    public TargetHudAdjust(TargetHUD targetHUD) {

        super("Adjust", targetHUD);
        this.targets = new LivingEntity[4];
        this.cardInfos = new AdjustCardInfo[4];
        this.healthFractions = new float[4];
        this.delayedHealthFractions = new float[4];
        this.absorptionFractions = new float[4];
        this.appearAnimations = new Animation[4];
        this.equipmentItems = new ArrayList[4];

        for (int slot = 0; slot < 4; slot++) {
            this.equipmentItems[slot] = new ArrayList<>();
            this.appearAnimations[slot] = new Animation(Easing.EASE_OUT_QUINT, 250L);
        }

        this.hudElement = new Draggable("Target HUD", 130.0F, 35.0F, 300.0F, 300.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return targetHUD.isEnabled()
                            && this.targets[0] != null
                            && this.appearAnimations[0].getValue() > 0.0
                            && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
    }

    private void collectEquipment(Player player2, List<ItemStack> items) {
        ItemStack itemStack = player2.getMainHandItem();
        if (itemStack != null && !itemStack.isEmpty()) {
            items.add(itemStack);
        }

        this.addIfNotEmpty(items, player2.getItemBySlot(EquipmentSlot.HEAD));
        this.addIfNotEmpty(items, player2.getItemBySlot(EquipmentSlot.CHEST));
        this.addIfNotEmpty(items, player2.getItemBySlot(EquipmentSlot.LEGS));
        this.addIfNotEmpty(items, player2.getItemBySlot(EquipmentSlot.FEET));
    }

    @Override
    public void renderSlot(Render2DEvent render2DEvent, int slot) {

        float appearProgress = (float) this.appearAnimations[slot].getValue();
        if (this.targets[slot] != null && !(appearProgress <= 0.0F)) {

            SizedFont nameFont = FontManager.getThemedFont("Tahoma", 9.0F);

            SizedFont diffFont = FontManager.getThemedFont("Tahoma", 8.0F);
            AdjustCardInfo cardInfo = this.cardInfos[slot];
            int alpha = (int) (appearProgress * 255.0F);
            float cardWidth = cardInfo.getWidth();
            float[] slotPosition = this.getSlotPosition(this.hudElement, slot, cardWidth, 35.0F);
            float cardX = slotPosition[0];
            float cardY = slotPosition[1];
            float hudScale = this.hudElement.getScale();
            if (Effects.isTargetHudBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangleWithAlpha(
                                cardX,
                                cardY,
                                cardWidth * hudScale,
                                35.0F * hudScale,
                                0.0F,
                                appearProgress);
            }

            render2DEvent.getContext().pushPose();
            render2DEvent.getContext().translate(cardX, cardY);
            render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
            render2DEvent
                    .getContext()
                    .drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(0.0F, 0.0F, cardWidth, 35.0F)
                                    .setShadow(Effects.isTargetHudShadow(), alpha)
                                    .setColor(
                                            ColorUtil.withAlpha(
                                                    Color.BLACK,
                                                    Math.min(alpha, Theme.getBackgroundAlpha()))));
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
                                        .setBounds(3.0F, 3.0F, 25.0F, 25.0F)
                                        .setTexture(gpuTexture)
                                        .useNearestFilter()
                                        .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                                        .setRadius(0.0F)
                                        .setColor(this.getHurtTint(this.targets[slot], alpha)));
                render2DEvent
                        .getContext()
                        .drawTexture(
                                TexturedRectShape.obtain()
                                        .setBounds(3.0F, 3.0F, 25.0F, 25.0F)
                                        .setTexture(gpuTexture)
                                        .useNearestFilter()
                                        .setUv(0.625F, 0.125F, 0.75F, 0.25F)
                                        .setRadius(0.0F)
                                        .setColor(this.getHurtTint(this.targets[slot], alpha)));
            } else {
                Render2DContext placeholderContext = render2DEvent.getContext();
                RoundedRectShape placeholderShape =
                        RoundedRectShape.obtain().setBounds(3.0F, 3.0F, 25.0F, 25.0F);

                placeholderContext.drawRect(
                        placeholderShape.setColor(
                                ColorUtil.withAlpha(new Color(60, 60, 70), alpha)));
            }

            Render2DContext nameContext = render2DEvent.getContext();
            TextDrawRequest nameRequest =
                    TextDrawRequest.obtain()
                            .setText(cardInfo.getName())
                            .setPosition(29.0F, 3.0F)
                            .setFont(nameFont.getMsdfFont())
                            .setFontSize(nameFont.getSize())
                            .enableShadow();

            nameContext.drawText(
                    nameRequest.setColor(
                            ColorUtil.withAlpha(new Color(220, 220, 225), alpha).getRGB()));
            if (!cardInfo.getHealthDiffText().isEmpty()) {
                Render2DContext diffContext = render2DEvent.getContext();
                TextDrawRequest diffRequest =
                        TextDrawRequest.obtain()
                                .setText(cardInfo.getHealthDiffText())
                                .setPosition(
                                        cardWidth - 3.0F - cardInfo.getHealthDiffWidth(),
                                        29.0F - diffFont.getLineHeight())
                                .setFont(diffFont.getMsdfFont())
                                .setFontSize(diffFont.getSize())
                                .enableShadow();

                diffContext.drawText(
                        diffRequest.setColor(
                                ColorUtil.withAlpha(new Color(220, 220, 225), alpha).getRGB()));
            }

            float barWidth = cardWidth - 6.0F;
            Color[] themeColors = this.getThemeColors();
            Color color =
                    ColorUtil.withAlpha(
                            ColorUtil.lerpColor(themeColors[0], Color.BLACK, 0.7F), alpha);
            render2DEvent
                    .getContext()
                    .drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(3.0F, 30.0F, barWidth, 3.0F)
                                    .setColor(color));
            float healthWidth = barWidth * this.healthFractions[slot];
            float delayedHealthWidth = barWidth * this.delayedHealthFractions[slot];
            if (delayedHealthWidth > 0.001F) {
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(3.0F, 30.0F, delayedHealthWidth, 3.0F)
                                        .setColor(
                                                ColorUtil.withAlpha(
                                                        ColorUtil.lerpColor(
                                                                themeColors[0], Color.BLACK, 0.2F),
                                                        alpha)));
            }

            if (healthWidth > 0.0F) {
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(3.0F, 30.0F, healthWidth, 3.0F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setHorizontalGradient(
                                                                ColorUtil.withAlpha(
                                                                                new Color(
                                                                                        ColorUtil
                                                                                                .waveBlend(
                                                                                                        cardX
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
                                                                                                        cardX
                                                                                                                + 3.0F
                                                                                                                + barWidth,
                                                                                                        themeColors[
                                                                                                                0]
                                                                                                                .getRGB(),
                                                                                                        themeColors[
                                                                                                                1]
                                                                                                                .getRGB())),
                                                                                alpha)
                                                                        .getRGB())));
            }

            float absorptionWidth = barWidth * this.absorptionFractions[slot];
            if (absorptionWidth > 0.001F) {
                Render2DContext absorptionContext = render2DEvent.getContext();
                RoundedRectShape absorptionShape =
                        RoundedRectShape.obtain().setBounds(3.0F, 30.0F, absorptionWidth, 3.0F);

                absorptionContext.drawRect(
                        absorptionShape.setColor(
                                ColorUtil.withAlpha(new Color(255, 255, 50), alpha)));
            }

            render2DEvent.getContext().popPose();
        }
    }

    private void renderEquipmentItems(VanillaHudRenderEvent event, int slot) {

        float appearProgress = (float) this.appearAnimations[slot].getValue();
        if (this.targets[slot] != null
                && !this.equipmentItems[slot].isEmpty()
                && !(appearProgress < 0.6F)) {
            GuiGraphicsExtractor guiGraphicsExtractor = event.getGraphics();
            float hudScale = this.hudElement.getScale();

            float nameLineHeight = FontManager.getThemedFont("Tahoma", 9.0F).getLineHeight();
            float[] slotPosition =
                    this.getSlotPosition(
                            this.hudElement, slot, this.cardInfos[slot].getWidth(), 35.0F);
            float itemsX = slotPosition[0] + 29.0F * hudScale;
            float itemsY = slotPosition[1] + (3.0F + nameLineHeight) * hudScale;
            float itemScale = 0.875F * hudScale;
            float itemSpacing = 15.0F * hudScale;
            guiGraphicsExtractor.pose().pushMatrix();
            guiGraphicsExtractor.pose().translate(itemsX, itemsY);
            guiGraphicsExtractor.pose().scale(itemScale, itemScale);

            for (int itemIndex = 0; itemIndex < this.equipmentItems[slot].size(); itemIndex++) {
                ItemStack itemStack = this.equipmentItems[slot].get(itemIndex);
                if (itemStack != null && !itemStack.isEmpty()) {
                    guiGraphicsExtractor.item(
                            itemStack, (int) (itemIndex * itemSpacing / itemScale), 0);
                }
            }

            guiGraphicsExtractor.pose().popMatrix();
        }
    }

    @Override
    public void renderVanillaHud(VanillaHudRenderEvent event) {

        for (int slot = 0; slot < 4; slot++) {
            this.renderEquipmentItems(event, slot);
        }
    }

    private String formatHealthDifference(LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null) {
            float healthDifference = mc.player.getHealth() - livingEntity.getHealth();
            String formattedDifference = healthDiffFormat.format(healthDifference);
            return healthDifference > 0.0F ? "+" + formattedDifference : formattedDifference;
        } else {
            return "";
        }
    }

    @Override
    public void onDisable() {

        for (int slot = 0; slot < 4; slot++) {
            this.targets[slot] = null;
            this.equipmentItems[slot].clear();
            this.healthFractions[slot] = 0.0F;
            this.delayedHealthFractions[slot] = 0.0F;
            this.absorptionFractions[slot] = 0.0F;
        }
    }

    private float clampFraction(float fraction) {
        return Math.max(0.0F, Math.min(1.0F, fraction));
    }

    private void addIfNotEmpty(List<ItemStack> items, ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            items.add(itemStack);
        }
    }

    @Override
    public void updateTargets() {

        List displayTargets = this.parent.getDisplayTargets(this.targets[0]);

        SizedFont nameFont = FontManager.getThemedFont("Tahoma", 9.0F);

        SizedFont diffFont = FontManager.getThemedFont("Tahoma", 8.0F);

        for (int slot = 0; slot < 4; slot++) {
            LivingEntity livingEntity =
                    slot < displayTargets.size() ? (LivingEntity) displayTargets.get(slot) : null;
            if (livingEntity != null && livingEntity != this.targets[slot]) {
                float maxHealth = Math.max(livingEntity.getMaxHealth(), 1.0F);
                float healthFraction =
                        this.clampFraction(
                                (livingEntity.getHealth() + livingEntity.getAbsorptionAmount())
                                        / maxHealth);
                this.healthFractions[slot] = healthFraction;
                this.delayedHealthFractions[slot] = healthFraction;
                this.absorptionFractions[slot] =
                        this.clampFraction(livingEntity.getAbsorptionAmount() / maxHealth);
            }

            if (livingEntity != null) {
                this.targets[slot] = livingEntity;
            }

            this.appearAnimations[slot].animateTo(livingEntity != null ? 1.0 : 0.0);
            if (this.targets[slot] != null) {
                float targetMaxHealth = Math.max(this.targets[slot].getMaxHealth(), 1.0F);
                float targetHealthFraction =
                        this.clampFraction(
                                (this.targets[slot].getHealth()
                                                + this.targets[slot].getAbsorptionAmount())
                                        / targetMaxHealth);
                float targetAbsorptionFraction =
                        this.clampFraction(
                                this.targets[slot].getAbsorptionAmount() / targetMaxHealth);
                this.healthFractions[slot] =
                        this.healthFractions[slot]
                                + (targetHealthFraction - this.healthFractions[slot]) * 0.2F;
                this.delayedHealthFractions[slot] =
                        this.delayedHealthFractions[slot]
                                + (targetHealthFraction - this.delayedHealthFractions[slot])
                                        * 0.05F;
                this.absorptionFractions[slot] =
                        this.absorptionFractions[slot]
                                + (targetAbsorptionFraction - this.absorptionFractions[slot])
                                        * 0.2F;
                if (Math.abs(this.healthFractions[slot] - targetHealthFraction) < 0.001F) {
                    this.healthFractions[slot] = targetHealthFraction;
                }

                if (Math.abs(this.delayedHealthFractions[slot] - targetHealthFraction) < 0.001F) {
                    this.delayedHealthFractions[slot] = targetHealthFraction;
                }

                if (Math.abs(this.absorptionFractions[slot] - targetAbsorptionFraction) < 0.001F) {
                    this.absorptionFractions[slot] = targetAbsorptionFraction;
                }
            }

            if (livingEntity == null && this.appearAnimations[slot].getValue() <= 0.0) {
                this.targets[slot] = null;
            }

            this.equipmentItems[slot].clear();
            if (this.targets[slot] instanceof Player player2) {
                this.collectEquipment(player2, this.equipmentItems[slot]);
            }

            if (this.targets[slot] != null) {
                String rawName = this.targets[slot].getName().getString();

                String plainName = rawName.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                String diffText = this.formatHealthDifference(this.targets[slot]);
                float diffWidth = diffText.isEmpty() ? 0.0F : diffFont.getStringWidth(diffText);
                float itemsWidth =
                        this.equipmentItems[slot].isEmpty()
                                ? 0.0F
                                : this.equipmentItems[slot].size() * 14.0F
                                        + (this.equipmentItems[slot].size() - 1) * 1.0F;
                float textWidth =
                        nameFont.getStringWidth(plainName)
                                + (diffWidth > 0.0F ? 6.0F + diffWidth : 0.0F);
                this.cardInfos[slot] =
                        new AdjustCardInfo(
                                plainName,
                                diffText,
                                Math.max(130.0F, 29.0F + Math.max(textWidth, itemsWidth) + 3.0F),
                                diffWidth);
                if (slot == 0) {
                    this.hudElement.setWidth(this.cardInfos[slot].getWidth());
                    this.hudElement.setHeight(35.0F);
                }
            }
        }
    }

    static {
        healthDiffFormat = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
    }
}
