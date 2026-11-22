package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.ui.animation.Animation;
import astralis.ui.theme.ThemePreset;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Dropdown selector for a mode setting. */
public class ModeSettingWidget extends SettingWidget {
    private final ArrayList<SettingWidget> siblingWidgets;
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float OPTION_HEIGHT = 13.0F;
    private final ModeSetting modeSetting;
    private final boolean animateExpansion;
    private final Animation expandAnimation;
    private boolean expanded;
    private boolean showThemePreviews;

    public ModeSettingWidget(ModeSetting modeSetting, ArrayList<SettingWidget> siblingWidgets) {
        this(modeSetting, siblingWidgets, true);
    }

    public ModeSettingWidget(
            ModeSetting modeSetting,
            ArrayList<SettingWidget> siblingWidgets,
            boolean animateExpansion) {

        this.expandAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.modeSetting = modeSetting;
        this.siblingWidgets = siblingWidgets;
        this.animateExpansion = animateExpansion;
        this.setSetting(modeSetting);
        this.setHeight(15.0F);
        this.showThemePreviews = modeSetting.getName().equalsIgnoreCase("Themes");
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        List<Mode<?>> modes = this.modeSetting.getValues();
        float fullOptionsHeight = modes.size() * 13.0F;
        if (this.animateExpansion) {
            this.expandAnimation.animateTo(this.expanded ? 1.0 : 0.0);
        } else {
            this.expandAnimation.setValue(this.expanded ? 1.0 : 0.0);
        }

        float optionsHeight = (float) (this.expandAnimation.getValue() * fullOptionsHeight);
        this.setHeight(15.0F + optionsHeight);
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        String currentModeName = this.modeSetting.getValue().getName();
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.modeSetting.getName())
                        .setPosition(this.getX() + 5.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.white.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(currentModeName)
                        .setPosition(
                                this.getX()
                                        + this.getWidth()
                                        - labelFont.getStringWidth(currentModeName)
                                        - 6.0F,
                                this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.gray.getRGB()));
        if (optionsHeight > 0.1F) {
            float optionsY = this.getY() + 15.0F;
            renderer.beginScissor(this.getX(), optionsY, this.getWidth(), optionsHeight);
            RoundedRectShape backgroundRect =
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX() + 4.0F,
                                    optionsY,
                                    this.getWidth() - 8.0F,
                                    optionsHeight)
                            .setRadius(3.0F);

            renderer.drawRect(backgroundRect.setColor(new Color(30, 30, 30)));
            float rowY = optionsY;

            for (Mode mode : modes) {
                boolean isSelected = mode == this.modeSetting.getValue();
                boolean rowHovered =
                        RenderUtil.isInsideRect(
                                mouseX,
                                mouseY,
                                this.getX() + 4.0F,
                                rowY,
                                this.getWidth() - 8.0F,
                                13.0F);
                if (isSelected) {
                    Color color =
                            ColorUtil.animatedBlend(
                                    optionsY,
                                    Astralis.getInstance().getPrimaryColor(),
                                    Astralis.getInstance().getSecondaryColor());
                    Color color2 =
                            ColorUtil.animatedBlend(
                                    optionsY + optionsHeight,
                                    Astralis.getInstance().getPrimaryColor(),
                                    Astralis.getInstance().getSecondaryColor());
                    renderer.drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(this.getX() + 4.0F, optionsY, 2.0F, optionsHeight)
                                    .setRadius(1.0F)
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setVerticalGradient(
                                                            color.getRGB(), color2.getRGB())));
                }

                if (this.showThemePreviews) {
                    ThemePreset theme = ThemePreset.fromName(mode.getName());
                    if (theme != null) {
                        renderer.drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(
                                                this.getX() + this.getWidth() - 15.0F,
                                                rowY + 1.0F,
                                                10.0F,
                                                10.0F)
                                        .setRadius(1.0F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setQuadGradient(
                                                                theme.getPrimaryColor().getRGB(),
                                                                theme.getSecondaryColor().getRGB(),
                                                                theme.getPrimaryColor().getRGB(),
                                                                theme.getSecondaryColor()
                                                                        .getRGB())));
                    }
                }

                if (rowHovered) {
                    RoundedRectShape hoverRect =
                            RoundedRectShape.obtain()
                                    .setBounds(
                                            this.getX() + 4.0F, rowY, this.getWidth() - 8.0F, 13.0F)
                                    .setRadius(0.0F);

                    renderer.drawRect(hoverRect.setColor(new Color(255, 255, 255, 15)));
                }

                rowY += 13.0F;
            }

            float labelY = optionsY;

            for (Mode mode2 : modes) {
                boolean isCurrent = mode2 == this.modeSetting.getValue();
                String optionName = mode2.getName();
                float labelX = this.getX() + 10.0F;
                float labelWidth = labelFont.getStringWidth(optionName);
                if (isCurrent) {
                    int selectedRgb =
                            ColorUtil.animatedBlend(
                                            labelY,
                                            Astralis.getInstance().getPrimaryColor(),
                                            Astralis.getInstance().getSecondaryColor())
                                    .getRGB();
                    renderer.drawText(
                            TextDrawRequest.obtain()
                                    .setText(optionName)
                                    .setPosition(labelX, labelY)
                                    .centerVertically(13.0F)
                                    .setFont(labelFont.getMsdfFont())
                                    .setFontSize(labelFont.getSize())
                                    .enableShadow()
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setLinearGradient(
                                                            selectedRgb,
                                                            selectedRgb,
                                                            labelX,
                                                            labelY,
                                                            labelX + labelWidth,
                                                            labelY)));
                } else {
                    TextDrawRequest unselectedText =
                            TextDrawRequest.obtain()
                                    .setText(optionName)
                                    .setPosition(labelX, labelY)
                                    .centerVertically(13.0F)
                                    .setFont(labelFont.getMsdfFont())
                                    .setFontSize(labelFont.getSize())
                                    .enableShadow();

                    renderer.drawText(unselectedText.setColor(new Color(180, 180, 180).getRGB()));
                }

                labelY += 13.0F;
            }

            renderer.endScissor();
        }
    }

    public float getTargetHeight() {
        return 15.0F + (this.expanded ? this.modeSetting.getValues().size() * 13.0F : 0.0F);
    }

    @Override
    public boolean isSettled() {
        return !this.expanded && (!this.animateExpansion || this.expandAnimation.isFinished());
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                15.0F)) {
            if (mouseButtonEvent.button() == 1) {
                this.expanded = !this.expanded;
            }
        } else {
            if (this.expanded || this.expandAnimation.getValue() > 0.0) {
                List<Mode<?>> modes = this.modeSetting.getValues();
                float optionY = this.getY() + 15.0F;

                for (Mode mode : modes) {
                    if (RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            this.getX(),
                            optionY,
                            this.getWidth(),
                            13.0F)) {
                        if (mouseButtonEvent.button() == 0) {
                            this.modeSetting.switchMode(mode);
                            this.expanded = false;
                        }

                        return;
                    }

                    optionY += 13.0F;
                }
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {}
}
