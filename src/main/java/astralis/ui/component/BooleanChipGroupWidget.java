package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.BooleanSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.ui.animation.Animation;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Collapsible grid of toggle chips for a setting group marked collapsed; starts collapsed and
 * shows only the group's boolean settings.
 */
public class BooleanChipGroupWidget extends SettingWidget {
    private final SettingGroup settingGroup;
    private final Animation expandAnimation;
    private final HashMap<BooleanSetting, Animation> chipAnimations;
    private boolean collapsed;

    public BooleanChipGroupWidget(SettingGroup settingGroup) {
        super();
        this.expandAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.chipAnimations = new HashMap<>();
        this.collapsed = true;
        this.settingGroup = settingGroup;
        this.setSetting(settingGroup);
        this.setHeight(15.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        SizedFont mediumFont = FontManager.getFont(mediumFontName(), 7.0F);
        SizedFont regularFont = FontManager.getFont(regularFontName(), 8.0F);
        ArrayList<BooleanSetting> booleanSettings = new ArrayList<>();

        for (Setting setting2 : this.settingGroup.getChildren()) {
            if (!setting2.isHidden() && setting2 instanceof BooleanSetting booleanSetting) {
                booleanSettings.add(booleanSetting);
            }
        }

        float availableWidth = this.getWidth() - 10.0F;
        float rowWidth = 0.0F;
        int rowCount = 1;

        for (BooleanSetting booleanSetting2 : booleanSettings) {
            float measuredWidth = mediumFont.getStringWidth(booleanSetting2.getName()) + 14.0F;
            if (rowWidth > 0.0F && rowWidth + 4.0F + measuredWidth > availableWidth) {
                rowCount++;
                rowWidth = measuredWidth;
            } else {
                rowWidth = rowWidth == 0.0F ? measuredWidth : rowWidth + 4.0F + measuredWidth;
            }
        }

        float expandedHeight = rowCount * 14.0F + (rowCount - 1) * 4.0F + 2.0F;
        this.expandAnimation.animateTo(this.collapsed ? 0.0 : 1.0);
        float animatedHeight = (float) (this.expandAnimation.getValue() * expandedHeight);
        this.setHeight(15.0F + animatedHeight);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.settingGroup.getName())
                        .setPosition(this.getX() + 5.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(regularFont.getMsdfFont())
                        .setFontSize(regularFont.getSize())
                        .enableShadow()
                        .setColor(Color.white.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.collapsed ? "+" : "-")
                        .setPosition(this.getX() + this.getWidth() - 10.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(regularFont.getMsdfFont())
                        .setFontSize(regularFont.getSize())
                        .enableShadow()
                        .setColor(Color.gray.getRGB()));
        if (animatedHeight > 0.1F) {
            renderer.beginScissor(
                    this.getX(), this.getY() + 14.0F, this.getWidth(), animatedHeight + 1.0F);
            float chipX = this.getX() + 5.0F;
            float chipY = this.getY() + 15.0F;

            for (BooleanSetting booleanSetting3 : booleanSettings) {
                float chipWidth = mediumFont.getStringWidth(booleanSetting3.getName()) + 14.0F;
                if (chipX > this.getX() + 5.0F
                        && chipX + chipWidth > this.getX() + 5.0F + availableWidth) {
                    chipX = this.getX() + 5.0F;
                    chipY += 18.0F;
                }

                Animation chipAnimation =
                        this.chipAnimations.computeIfAbsent(
                                booleanSetting3,
                                booleanSetting4 -> {
                                    Animation initialAnimation =
                                            new Animation(Easing.EASE_OUT_QUINT, 300L);
                                    initialAnimation.setValue(
                                            booleanSetting3.getValue() ? 1.0 : 0.0);
                                    return initialAnimation;
                                });
                chipAnimation.animateTo(booleanSetting3.getValue() ? 1.0 : 0.0);
                float chipProgress = (float) chipAnimation.getValue();
                renderer.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(chipX, chipY, chipWidth, 14.0F)
                                .setRadius(3.0F)
                                .setColor(
                                        RenderUtil.isInsideRect(
                                                                mouseX, mouseY, chipX, chipY,
                                                                chipWidth, 14.0F)
                                                        && !booleanSetting3.getValue()
                                                ? new Color(50, 50, 55)
                                                : new Color(38, 38, 42)));
                if (chipProgress > 0.0F) {
                    Color color =
                            ColorUtil.animatedBlend(
                                    chipY,
                                    Astralis.getInstance().getPrimaryColor(),
                                    Astralis.getInstance().getSecondaryColor());
                    float fillWidth = chipWidth * chipProgress;
                    float fillHeight = 14.0F * chipProgress;
                    renderer.drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(
                                            chipX + (chipWidth - fillWidth) / 2.0F,
                                            chipY + (14.0F - fillHeight) / 2.0F,
                                            fillWidth,
                                            fillHeight)
                                    .setRadius(3.0F)
                                    .setColor(color));
                }

                chipX += chipWidth + 4.0F;
            }

            float labelX = this.getX() + 5.0F;
            float labelY = this.getY() + 15.0F;

            for (BooleanSetting booleanSetting5 : booleanSettings) {
                float slotWidth = mediumFont.getStringWidth(booleanSetting5.getName()) + 14.0F;
                if (labelX > this.getX() + 5.0F
                        && labelX + slotWidth > this.getX() + 5.0F + availableWidth) {
                    labelX = this.getX() + 5.0F;
                    labelY += 18.0F;
                }

                Animation storedAnimation = this.chipAnimations.get(booleanSetting5);
                float labelProgress =
                        storedAnimation != null
                                ? (float) storedAnimation.getValue()
                                : (booleanSetting5.getValue() ? 1.0F : 0.0F);
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(booleanSetting5.getName())
                                .setPosition(labelX + 7.0F, labelY)
                                .centerVertically(14.0F)
                                .setFont(mediumFont.getMsdfFont())
                                .setFontSize(mediumFont.getSize())
                                .enableShadow()
                                .setColor(
                                        labelProgress > 0.5F
                                                ? Color.white.getRGB()
                                                : new Color(160, 160, 165).getRGB()));
                labelX += slotWidth + 4.0F;
            }

            renderer.endScissor();
        }
    }

    @Override
    public boolean isSettled() {
        if (this.collapsed && this.expandAnimation.isFinished()) {
            for (Animation chipAnimation : this.chipAnimations.values()) {
                if (!chipAnimation.isFinished()) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (!RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                15.0F)) {
            if (!this.collapsed && mouseButtonEvent.button() == 0) {
                SizedFont mediumFont = FontManager.getFont(mediumFontName(), 7.0F);
                ArrayList<BooleanSetting> booleanSettings = new ArrayList<>();

                for (Setting setting2 : this.settingGroup.getChildren()) {
                    if (!setting2.isHidden() && setting2 instanceof BooleanSetting booleanSetting) {
                        booleanSettings.add(booleanSetting);
                    }
                }

                float availableWidth = this.getWidth() - 10.0F;
                float chipX = this.getX() + 5.0F;
                float chipY = this.getY() + 15.0F;

                for (BooleanSetting booleanSetting2 : booleanSettings) {
                    float chipWidth = mediumFont.getStringWidth(booleanSetting2.getName()) + 14.0F;
                    if (chipX > this.getX() + 5.0F
                            && chipX + chipWidth > this.getX() + 5.0F + availableWidth) {
                        chipX = this.getX() + 5.0F;
                        chipY += 18.0F;
                    }

                    if (RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            chipX,
                            chipY,
                            chipWidth,
                            14.0F)) {
                        booleanSetting2.setValueRaw(!booleanSetting2.getValue());
                        return;
                    }

                    chipX += chipWidth + 4.0F;
                }
            }
        } else {
            if (mouseButtonEvent.button() == 0 || mouseButtonEvent.button() == 1) {
                this.collapsed = !this.collapsed;
            }
        }
    }
}
