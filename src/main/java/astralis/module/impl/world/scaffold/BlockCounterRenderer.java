package astralis.module.impl.world.scaffold;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.world.Scaffold;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.ui.animation.Animation;
import astralis.ui.hud.Draggable;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class BlockCounterRenderer implements InstanceAccess {
    private final Scaffold scaffold;
    private final ModeSetting counterMode;
    private final ColorSetting counterColors;
    private final Draggable hudElement;
    private final Animation fancyScaleAnimation;
    private final Animation simpleFadeAnimation;
    private final Animation countChangeAnimation;
    private int currentCount;
    private int previousCount;
    private boolean firstUpdate;
    private final Map<SizedFont, Float> digitWidthCache;
    private float boxWidth;

    public BlockCounterRenderer(
            Scaffold scaffold, ModeSetting counterMode, ColorSetting counterColors) {

        this.fancyScaleAnimation = new Animation(Easing.EASE_OUT_BACK, 350L);

        this.simpleFadeAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 200L);

        this.countChangeAnimation = new Animation(Easing.EASE_IN_OUT_QUART, 380L);
        this.firstUpdate = true;
        this.digitWidthCache = new HashMap<>();
        this.scaffold = scaffold;
        this.counterMode = counterMode;
        this.counterColors = counterColors;

        this.hudElement = new Draggable("Block Counter", 60.0F, 19.0F, 60.0F, 60.0F);
        this.boxWidth = this.hudElement.getWidth();
        this.hudElement.setVisibleWhen(
                () -> {
                    if (scaffold.isEnabled()) {

                        if (!counterMode.is("Off")
                                && this.fancyScaleAnimation.getValue() > 0.02
                                && !mc.gui.hud.isHidden()) {
                            return true;
                        }
                    }

                    return false;
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        Astralis.getInstance().getEventBus().register(this);
    }

    private int countBlocks() {
        int blockCount = 0;
        int slot = 0;

        while (true) {

            if (slot >= 9) {
                ItemStack offhandStack = mc.player.getOffhandItem();
                if (offhandStack.getItem() instanceof BlockItem) {
                    blockCount += offhandStack.getCount();
                }

                return blockCount;
            }

            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (this.scaffold.isValidBlock(stack)) {
                blockCount += stack.getCount();
            }

            slot++;
        }
    }

    private float computeWidth(SizedFont digitFont, SizedFont labelFont) {
        int digitCount =
                Math.max(
                        String.valueOf(this.previousCount).length(),
                        String.valueOf(this.currentCount).length());
        float countWidth = (this.maxDigitWidth(digitFont) - 1.0F) * digitCount;

        float labelWidth = labelFont.getStringWidth("Blocks");
        return countWidth + 3.0F + labelWidth + 12.0F;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        int visibilityResult;
        visibilityCheck:
        {
            if (this.scaffold.isEnabled()) {

                if (!this.counterMode.is("Off")) {
                    visibilityResult = 1;
                    break visibilityCheck;
                }
            }

            visibilityResult = 0;
        }

        boolean visible = visibilityResult != 0;

        if (this.counterMode.is("Simple")) {
            this.simpleFadeAnimation.animateTo(visible ? 1.0 : 0.0);
        } else {
            long animationDuration;
            if (visible) {

                animationDuration = 350L;
            } else {

                animationDuration = 250L;
            }

            this.fancyScaleAnimation.setDuration(animationDuration);
            this.fancyScaleAnimation.setEasing(
                    visible ? Easing.EASE_OUT_BACK : Easing.EASE_IN_QUAD);
            this.fancyScaleAnimation.animateTo(visible ? 1.0 : 0.0);
        }

        int blockCount = visible ? this.countBlocks() : this.currentCount;
        if (visible) {
            this.updateCount(blockCount);
        }

        this.countChangeAnimation.animateTo(1.0);

        SizedFont selectedDigitFont;
        if (this.counterMode.is("Simple")) {

            selectedDigitFont = FontManager.getThemedFont("Tahoma Bold", 9.0F);
        } else {

            selectedDigitFont = FontManager.getThemedFont("Product Sans Medium", 9.0F);
        }

        SizedFont digitFont = selectedDigitFont;

        SizedFont selectedLabelFont;
        if (this.counterMode.is("Simple")) {

            selectedLabelFont = FontManager.getThemedFont("Tahoma", 9.0F);
        } else {

            selectedLabelFont = FontManager.getThemedFont("Product Sans Regular", 9.0F);
        }

        SizedFont labelFont = selectedLabelFont;
        this.boxWidth = this.computeWidth(digitFont, labelFont);
        this.hudElement.setWidth(this.boxWidth);

        if (this.counterMode.is("Simple")) {
            float fade = (float) this.simpleFadeAnimation.getValue();
            if (!(fade <= 0.02F) || visible) {
                float simpleX = this.hudElement.getX();
                float simpleY = this.hudElement.getY();
                Color[] simpleColors = this.counterColors.getColors();
                Color simpleStartColor = simpleColors[0];
                Color simpleEndColor = simpleColors[1];
                event.getContext().pushPose();
                event.getContext().translate(simpleX, simpleY);
                this.renderContent(
                        event.getContext(),
                        digitFont,
                        labelFont,
                        1.0F,
                        simpleStartColor,
                        simpleEndColor,
                        simpleX,
                        fade);
                event.getContext().popPose();
            }
        } else {
            float animationScale = (float) this.fancyScaleAnimation.getValue();
            if (!(animationScale <= 0.02F) || visible) {
                float hudScale = this.hudElement.getScale();
                float renderScale = animationScale * hudScale;
                float hudX = this.hudElement.getX();
                float hudY = this.hudElement.getY();
                float centerX = hudX + this.boxWidth * hudScale / 2.0F;
                float centerY = hudY + 19.0F * hudScale / 2.0F;
                Color[] colors = this.counterColors.getColors();
                Color startColor = colors[0];
                Color endColor = colors[1];
                event.getRenderEffects()
                        .blurRoundedRectangle(
                                centerX - this.boxWidth * renderScale / 2.0F,
                                centerY - 19.0F * renderScale / 2.0F,
                                this.boxWidth * renderScale,
                                19.0F * renderScale,
                                6.0F);
                event.getContext().pushPose();
                event.getContext().translate(centerX, centerY);
                event.getContext().scale(renderScale, renderScale, 1.0F);
                event.getContext().translate(-this.boxWidth / 2.0F, -9.5F);
                Render2DContext context = event.getContext();
                RoundedRectShape background =
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, this.boxWidth, 19.0F)
                                .setRadius(6.0F)
                                .enableShadow();

                context.drawRect(background.setColor(new Color(5, 5, 10, 140)));
                this.renderContent(
                        event.getContext(),
                        digitFont,
                        labelFont,
                        (float) this.countChangeAnimation.getValue(),
                        startColor,
                        endColor,
                        hudX,
                        1.0F);
                event.getContext().popPose();
            }
        }
    }

    private static char paddedCharAt(String text, int paddedLength, int index) {

        int padding = paddedLength - text.length();
        char character;
        if (index < padding) {

            character = ' ';
        } else {
            character = text.charAt(index - padding);
        }

        return character;
    }

    /** Draws the count with clipped digit transitions and the fixed Blocks label. */
    private void renderContent(
            Render2DContext context,
            SizedFont digitFont,
            SizedFont labelFont,
            float countProgress,
            Color startColor,
            Color endColor,
            float hudX,
            float opacity) {
        long scaledAlpha;
        String previousText = String.valueOf(this.previousCount);
        String currentText = String.valueOf(this.currentCount);
        int digitCount = Math.max(previousText.length(), currentText.length());
        boolean countChanging =
                !this.countChangeAnimation.isFinished() && this.previousCount != this.currentCount;
        float digitWidth = this.maxDigitWidth(digitFont) - 1.0F;
        float countWidth = digitWidth * digitCount;

        float labelWidth = labelFont.getStringWidth("Blocks");
        float contentWidth = countWidth + 3.0F + labelWidth;
        float contentX = (this.boxWidth - contentWidth) / 2.0F;
        float digitY = 2.0F + (13.0F - digitFont.getSize()) / 2.0F;
        float labelY = 2.0F + (13.0F - labelFont.getSize()) / 2.0F;

        for (int digitIndex = 0; digitIndex < digitCount; digitIndex++) {
            char previousDigit = paddedCharAt(previousText, digitCount, digitIndex);
            char currentDigit = paddedCharAt(currentText, digitCount, digitIndex);
            float digitX = contentX + digitIndex * digitWidth;
            boolean digitChanged = previousDigit != currentDigit;
            if (countChanging && digitChanged) {
                context.beginScissor(digitX, 2.0F, digitWidth, 13.0F);

                if (previousDigit != ' ') {
                    this.drawGradientText(
                            context,
                            String.valueOf(previousDigit),
                            digitFont,
                            digitX,
                            digitWidth,
                            digitY,
                            -13.0F * countProgress,
                            opacity * (1.0F - countProgress),
                            startColor,
                            endColor,
                            hudX);
                }

                if (currentDigit != ' ') {
                    this.drawGradientText(
                            context,
                            String.valueOf(currentDigit),
                            digitFont,
                            digitX,
                            digitWidth,
                            digitY,
                            13.0F * (1.0F - countProgress),
                            opacity * countProgress,
                            startColor,
                            endColor,
                            hudX);
                }

                context.endScissor();
            } else {

                if (currentDigit != ' ') {
                    this.drawGradientText(
                            context,
                            String.valueOf(currentDigit),
                            digitFont,
                            digitX,
                            digitWidth,
                            digitY,
                            0.0F,
                            opacity,
                            startColor,
                            endColor,
                            hudX);
                }
            }
        }

        float labelX = contentX + countWidth + 3.0F;
        scaledAlpha = (int) (opacity * 255.0F);

        int labelAlpha = Math.clamp(scaledAlpha, 0, 255);
        TextDrawRequest labelText = TextDrawRequest.obtain();

        context.drawText(
                labelText
                        .setText("Blocks")
                        .setPosition(labelX, labelY)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(ColorUtil.withAlpha(Color.WHITE, labelAlpha).getRGB()));
    }

    private void drawGradientText(
            Render2DContext context,
            String text,
            SizedFont font,
            float cellX,
            float cellWidth,
            float baselineY,
            float slideOffsetY,
            float opacity,
            Color startColor,
            Color endColor,
            float hudX) {
        long scaledAlpha;
        float textWidth = font.getStringWidth(text);
        float textX = cellX + (cellWidth - textWidth) / 2.0F;
        float textY = baselineY + slideOffsetY;
        scaledAlpha = (int) (opacity * 255.0F);

        int alpha = Math.clamp(scaledAlpha, 0, 255);
        int gradientStart =
                ColorUtil.withAlpha(
                                new Color(
                                        ColorUtil.waveBlend(
                                                hudX + textX,
                                                startColor.getRGB(),
                                                endColor.getRGB()),
                                        true),
                                alpha)
                        .getRGB();
        int gradientEnd =
                ColorUtil.withAlpha(
                                new Color(
                                        ColorUtil.waveBlend(
                                                hudX + textX + textWidth,
                                                startColor.getRGB(),
                                                endColor.getRGB()),
                                        true),
                                alpha)
                        .getRGB();
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(text)
                        .setPosition(textX, textY)
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                gradientStart,
                                                gradientEnd,
                                                textX,
                                                textY,
                                                textX + textWidth,
                                                textY)));
    }

    private float maxDigitWidth(SizedFont font) {
        return this.digitWidthCache.computeIfAbsent(
                font,
                cachedFont -> {
                    float maxWidth = 0.0F;
                    int digit = 0;

                    while (true) {

                        if (digit >= 10) {
                            return maxWidth;
                        }

                        maxWidth =
                                Math.max(
                                        maxWidth, cachedFont.getStringWidth(String.valueOf(digit)));
                        digit++;
                    }
                });
    }

    private void updateCount(int blockCount) {
        if (this.firstUpdate) {
            this.currentCount = blockCount;
            this.previousCount = blockCount;
            this.firstUpdate = false;
        } else {

            if (this.counterMode.is("Simple")) {
                this.currentCount = blockCount;
                this.previousCount = blockCount;
            } else {
                if (blockCount != this.currentCount && this.countChangeAnimation.isFinished()) {
                    this.previousCount = this.currentCount;
                    this.currentCount = blockCount;
                    this.countChangeAnimation.setValue(0.0);
                    this.countChangeAnimation.setTarget(0.0);
                }
            }
        }
    }
}
