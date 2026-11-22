package astralis.ui;

import astralis.Astralis;
import astralis.module.ModuleManager;
import astralis.module.impl.Theme;
import astralis.util.render.BlurShadowManager;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShadowRenderer;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.gui.components.AbstractSliderButton;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class CustomSliderRenderer {
    private static List<CustomSliderEntry> sliders;

    private CustomSliderRenderer() {}

    public static boolean isCustomSlidersEnabled() {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        return moduleManager != null
                && !moduleManager.isEmpty()
                && moduleManager.getModule(Theme.class).getCustomSliders().getValue();
    }

    public static void register(AbstractSliderButton abstractSliderButton, double value) {
        if (isCustomSlidersEnabled()
                && !sliders.stream().anyMatch(entry -> entry.getSlider() == abstractSliderButton)) {
            sliders.add(new CustomSliderEntry(abstractSliderButton, value));
        }
    }

    public static void renderRegistered(Render2DContext context) {
        if (!isCustomSlidersEnabled()) {
            clearRegistered();
        } else if (!sliders.isEmpty()) {
            ShadowRenderer shadowRenderer = context.getShadowRenderer();
            Consumer<Render2DContext> maskDrawer =
                    maskContext -> {
                        Iterator iterator = sliders.iterator();

                        while (iterator.hasNext()) {
                            CustomSliderEntry maskEntry = (CustomSliderEntry) iterator.next();
                            AbstractSliderButton abstractSliderButton = maskEntry.getSlider();
                            RoundedRectShape shape =
                                    RoundedRectShape.obtain()
                                            .setBounds(
                                                    abstractSliderButton.getX(),
                                                    abstractSliderButton.getY(),
                                                    abstractSliderButton.getWidth(),
                                                    abstractSliderButton.getHeight())
                                            .setRadius(6.0F);

                            long maskAlpha;
                            maskAlpha = Math.round(255.0F * abstractSliderButton.getAlpha());

                            maskContext.drawRect(
                                    shape.setColor(
                                            new Color(
                                                    255, 255, 255, Math.clamp(maskAlpha, 0, 255))));
                        }
                    };

            Color color = new Color(0, 0, 0, 95);

            shadowRenderer.renderShapes(maskDrawer, color, 8, 0.55F);
            BlurShadowManager blurShadowManager = Astralis.getInstance().getRenderEffects();
            SizedFont font = FontManager.getFont("Product Sans Medium", 8.5F);
            Color color2 = Astralis.getInstance().getPrimaryColor();
            Color color3 = Astralis.getInstance().getSecondaryColor();

            for (CustomSliderEntry sliderEntry : sliders) {
                AbstractSliderButton abstractSliderButton2 = sliderEntry.getSlider();
                float x = abstractSliderButton2.getX();
                float y = abstractSliderButton2.getY();
                float width = abstractSliderButton2.getWidth();
                float height = abstractSliderButton2.getHeight();
                float alpha = abstractSliderButton2.getAlpha();
                float trackX = x + 7.0F;
                float trackY = y + height - 6.0F;
                float trackWidth = width - 14.0F;
                float knobX = trackX + (float) sliderEntry.getValue() * trackWidth;
                blurShadowManager.drawBlurredRoundedRectangle(x, y, width, height, 6.0F, alpha);
                RoundedRectShape backgroundShape =
                        RoundedRectShape.obtain().setBounds(x, y, width, height).setRadius(6.0F);

                int baseAlpha;
                if (abstractSliderButton2.active) {
                    if (abstractSliderButton2.isHoveredOrFocused()) {

                        baseAlpha = 118;
                    } else {

                        baseAlpha = 82;
                    }
                } else {

                    baseAlpha = 44;
                }

                long backgroundAlpha = Math.round(baseAlpha * alpha);

                color = new Color(18, 20, 25, Math.clamp(backgroundAlpha, 0, 255));
                context.drawRect(backgroundShape.setColor(color));
                RoundedRectShape trackShape =
                        RoundedRectShape.obtain()
                                .setBounds(trackX, trackY, trackWidth, 2.0F)
                                .setRadius(1.0F);

                long trackAlpha = Math.round(38.0F * alpha);

                context.drawRect(
                        trackShape.setColor(
                                new Color(255, 255, 255, Math.clamp(trackAlpha, 0, 255))));
                if (knobX > trackX) {
                    context.drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(trackX, trackY, knobX - trackX, 2.0F)
                                    .setRadius(1.0F)
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setHorizontalGradient(
                                                            color2.getRGB(), color3.getRGB())));
                }

                float knobRadius = abstractSliderButton2.isHoveredOrFocused() ? 4.0F : 3.5F;
                RoundedRectShape knobShape =
                        RoundedRectShape.obtain()
                                .setBounds(
                                        knobX - knobRadius,
                                        trackY + 1.0F - knobRadius,
                                        knobRadius * 2.0F,
                                        knobRadius * 2.0F)
                                .setRadius(knobRadius);

                long knobAlpha = Math.round(255.0F * alpha);

                context.drawRect(
                        knobShape.setColor(
                                new Color(245, 246, 250, Math.clamp(knobAlpha, 0, 255))));
                TextDrawRequest textRequest =
                        TextDrawRequest.obtain()
                                .setText(abstractSliderButton2.getMessage().getString())
                                .setPosition(x, y - 2.0F)
                                .centerInBox(width, height - 3.0F)
                                .setFont(font.getMsdfFont())
                                .setFontSize(font.getSize());

                if (abstractSliderButton2.active) {

                    baseAlpha = 230;
                } else {

                    baseAlpha = 105;
                }

                long textAlpha = Math.round(baseAlpha * alpha);

                color = new Color(245, 246, 250, Math.clamp(textAlpha, 0, 255));
                context.drawText(textRequest.setColor(color.getRGB()));
            }

            clearRegistered();
        }
    }

    public static boolean hasRegisteredSliders() {
        return !sliders.isEmpty();
    }

    public static void clearRegistered() {
        sliders.clear();
    }

    static {
        sliders = new ArrayList<>();
    }
}
