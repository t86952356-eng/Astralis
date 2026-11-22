package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.ActionSetting;
import astralis.setting.BlockListSetting;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.TextSetting;
import astralis.ui.animation.Animation;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Objects;

/** Expandable group of nested setting widgets. */
public class SettingGroupWidget extends SettingWidget {
    private final ArrayList<SettingWidget> settingWidgets;
    private final SettingGroup settingGroup;
    private final Animation expandAnimation;
    private boolean expanded;

    public SettingGroupWidget(SettingGroup settingGroup) {

        this.settingWidgets = new ArrayList<>();
        this.expandAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.settingGroup = settingGroup;
        this.setSetting(settingGroup);
        this.setHeight(15.0F);

        for (Setting setting2 : settingGroup.getChildren()) {
            this.addValue(setting2);
        }
    }

    private void addValue(Setting<?> settingToAdd) {
        Setting setting2 = settingToAdd;
        byte restartIndex = 0;

        SettingWidget widget =
                switch (typeSwitch0(setting2, restartIndex)) {
                    case 0 -> {
                        ActionSetting actionSetting = (ActionSetting) setting2;
                        yield new ActionSettingWidget(actionSetting);
                    }
                    case 1 -> {
                        BooleanSetting booleanSetting = (BooleanSetting) setting2;
                        yield new BooleanSettingWidget(booleanSetting, this.settingWidgets);
                    }
                    case 2 -> {
                        BlockListSetting blockListSetting = (BlockListSetting) setting2;
                        yield new BlockListSettingWidget(blockListSetting, this.settingWidgets);
                    }
                    case 3 -> {
                        NumberSetting numberSetting = (NumberSetting) setting2;
                        yield new NumberSettingWidget(numberSetting, this.settingWidgets);
                    }
                    case 4 -> {
                        RangeSetting rangeSetting = (RangeSetting) setting2;
                        yield new RangeSettingWidget(rangeSetting, this.settingWidgets);
                    }
                    case 5 -> {
                        ModeSetting modeSetting = (ModeSetting) setting2;
                        yield new ModeSettingWidget(modeSetting, this.settingWidgets);
                    }
                    case 6 -> {
                        ColorSetting colorSetting = (ColorSetting) setting2;
                        yield new ColorSettingWidget(colorSetting, this.settingWidgets);
                    }
                    case 7 -> {
                        TextSetting textSetting = (TextSetting) setting2;
                        yield new TextSettingWidget(textSetting, this.settingWidgets);
                    }
                    case 8 -> {
                        SettingGroup settingGroup = (SettingGroup) setting2;
                        yield settingGroup.isCollapsed()
                                ? new BooleanChipGroupWidget(settingGroup)
                                : new SettingGroupWidget(settingGroup);
                    }
                    default -> null;
                };
        if (widget != null) {
            this.settingWidgets.add(widget);
        } else {
            System.out.println("Unexpected value inside Tree: " + settingToAdd);
        }

        if (!(settingToAdd instanceof SettingGroup)
                && !(settingToAdd instanceof ColorSetting)
                && settingToAdd.getChildren() != null) {
            for (Setting setting3 : settingToAdd.getChildren()) {
                this.addValue(setting3);
            }
        }
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        float contentHeight = 0.0F;

        for (SettingWidget childWidget : this.settingWidgets) {
            if (!childWidget.getSetting().isHidden()) {
                contentHeight += childWidget.getHeight();
            }
        }

        this.expandAnimation.animateTo(this.expanded ? 1.0 : 0.0);
        float animatedHeight = (float) (this.expandAnimation.getValue() * contentHeight);
        this.setHeight(15.0F + animatedHeight);
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.settingGroup.getName())
                        .setPosition(this.getX() + 5.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.white.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.expanded ? "-" : "+")
                        .setPosition(this.getX() + this.getWidth() - 10.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.gray.getRGB()));
        if (animatedHeight > 0.1F) {
            float contentY = this.getY() + 15.0F;
            Color color =
                    ColorUtil.animatedBlend(
                            contentY,
                            Astralis.getInstance().getPrimaryColor(),
                            Astralis.getInstance().getSecondaryColor());
            Color color2 =
                    ColorUtil.animatedBlend(
                            contentY + animatedHeight,
                            Astralis.getInstance().getPrimaryColor(),
                            Astralis.getInstance().getSecondaryColor());
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX() + 5.0F, contentY, 2.0F, animatedHeight)
                            .setRadius(Math.min(1.0F, animatedHeight * 0.5F))
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(color.getRGB(), color2.getRGB())));
            renderer.beginScissor(this.getX(), contentY, this.getWidth(), animatedHeight);
            float childY = contentY;

            for (SettingWidget child : this.settingWidgets) {
                if (!child.getSetting().isHidden()) {
                    child.setX(this.getX() + 5.0F);
                    child.setY(childY);
                    child.setWidth(this.getWidth() - 5.0F);
                    child.render(renderer, mouseX, mouseY);
                    childY += child.getHeight();
                }
            }

            renderer.endScissor();
        }
    }

    @Override
    public boolean isSettled() {
        return !this.expanded && this.expandAnimation.isFinished();
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
            if (mouseButtonEvent.button() == 0 || mouseButtonEvent.button() == 1) {
                this.expanded = !this.expanded;
            }
        } else {
            if (this.expanded || this.expandAnimation.getValue() > 0.0) {
                float childY = this.getY() + 15.0F;

                for (SettingWidget child : this.settingWidgets) {
                    if (!child.getSetting().isHidden()) {
                        if (RenderUtil.isInsideRect(
                                mouseButtonEvent.x(),
                                mouseButtonEvent.y(),
                                this.getX(),
                                childY,
                                this.getWidth(),
                                child.getHeight())) {
                            child.click(mouseButtonEvent);
                        }

                        childY += child.getHeight();
                    }
                }
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        if (this.expanded || this.expandAnimation.getValue() > 0.0) {
            this.settingWidgets.forEach(widget -> widget.release(mouseButtonEvent));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.expanded) {
            return false;
        }

        for (SettingWidget child : this.settingWidgets) {
            if (!child.getSetting().isHidden() && child.keyPressed(event)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.expanded) {
            return false;
        }

        for (SettingWidget child : this.settingWidgets) {
            if (!child.getSetting().isHidden() && child.charTyped(characterEvent)) {
                return true;
            }
        }

        return false;
    }

    @Generated
    public ArrayList<SettingWidget> getSettingWidgets() {
        return this.settingWidgets;
    }

    @Generated
    public SettingGroup getSettingGroup() {
        return this.settingGroup;
    }

    @Generated
    public Animation getExpandAnimation() {
        return this.expandAnimation;
    }

    @Generated
    public boolean isExpanded() {
        return this.expanded;
    }

    private static int typeSwitch0(Setting setting2, int restartIndex) {
        Objects.checkIndex(restartIndex, 10);
        if (setting2 == null) {
            return -1;
        } else if (restartIndex <= 0 && setting2 instanceof ActionSetting) {
            return 0;
        } else if (restartIndex <= 1 && setting2 instanceof BooleanSetting) {
            return 1;
        } else if (restartIndex <= 2 && setting2 instanceof BlockListSetting) {
            return 2;
        } else if (restartIndex <= 3 && setting2 instanceof NumberSetting) {
            return 3;
        } else if (restartIndex <= 4 && setting2 instanceof RangeSetting) {
            return 4;
        } else if (restartIndex <= 5 && setting2 instanceof ModeSetting) {
            return 5;
        } else if (restartIndex <= 6 && setting2 instanceof ColorSetting) {
            return 6;
        } else if (restartIndex <= 7 && setting2 instanceof TextSetting) {
            return 7;
        } else {
            return restartIndex <= 8 && setting2 instanceof SettingGroup ? 8 : 9;
        }
    }
}
