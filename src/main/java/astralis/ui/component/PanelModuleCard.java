package astralis.ui.component;

import astralis.Astralis;
import astralis.module.Module;
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
import astralis.ui.screen.PanelClickGuiScreen;
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
import java.util.HashMap;
import java.util.Objects;

/** Panel ClickGUI module card with its setting widgets. */
public class PanelModuleCard extends SettingWidget {
    private final Module module;
    private final ArrayList<SettingWidget> settingWidgets;
    private final HashMap<Class<?>, ArrayList<SettingWidget>> settingWidgetsByType;
    private final Animation toggleAnimation;
    private final Animation hoverAnimation;
    private boolean binding;

    public PanelModuleCard(
            Module module2, float cardX, float cardY, float cardWidth, float cardHeight) {
        super(cardX, cardY, cardWidth, cardHeight);
        this.settingWidgets = new ArrayList<>();
        this.settingWidgetsByType = new HashMap<>();

        this.hoverAnimation = new Animation(Easing.EASE_OUT_QUINT, 180L);
        this.module = module2;

        this.toggleAnimation = new Animation(Easing.EASE_OUT_QUINT, 300L);
        this.toggleAnimation.setValue(module2.isEnabled() ? 1.0 : 0.0);

        for (Setting setting2 : module2.getSettings()) {
            this.addValue(setting2);
        }
    }

    private void addValue(Setting<?> settingToAdd) {
        ArrayList sameTypeWidgets =
                this.settingWidgetsByType.computeIfAbsent(
                        settingToAdd.getClass(), settingClass -> new ArrayList<>());

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
                        yield new BooleanSettingWidget(booleanSetting, sameTypeWidgets);
                    }
                    case 2 -> {
                        BlockListSetting blockListSetting = (BlockListSetting) setting2;
                        yield new BlockListSettingWidget(blockListSetting, sameTypeWidgets);
                    }
                    case 3 -> {
                        NumberSetting numberSetting = (NumberSetting) setting2;
                        yield new NumberSettingWidget(numberSetting, sameTypeWidgets);
                    }
                    case 4 -> {
                        RangeSetting rangeSetting = (RangeSetting) setting2;
                        yield new RangeSettingWidget(rangeSetting, sameTypeWidgets);
                    }
                    case 5 -> {
                        ModeSetting modeSetting = (ModeSetting) setting2;
                        yield new ModeSettingWidget(modeSetting, sameTypeWidgets);
                    }
                    case 6 -> {
                        ColorSetting colorSetting = (ColorSetting) setting2;
                        yield new ColorSettingWidget(colorSetting, sameTypeWidgets);
                    }
                    case 7 -> {
                        SettingGroup settingGroup = (SettingGroup) setting2;
                        yield settingGroup.isCollapsed()
                                ? new BooleanChipGroupWidget(settingGroup)
                                : new SettingGroupWidget(settingGroup);
                    }
                    case 8 -> {
                        TextSetting textSetting = (TextSetting) setting2;
                        yield new TextSettingWidget(textSetting, sameTypeWidgets);
                    }
                    default -> null;
                };
        if (widget != null) {
            this.settingWidgets.add(widget);
            sameTypeWidgets.add(widget);
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
        boolean cardHovered =
                RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight());

        this.hoverAnimation.animateTo(cardHovered ? 1.0 : 0.0);
        this.toggleAnimation.animateTo(this.module.isEnabled() ? 1.0 : 0.0);
        float hoverProgress = (float) this.hoverAnimation.getValue();
        float toggleProgress = (float) this.toggleAnimation.getValue();
        if (hoverProgress > 0.0F) {
            RoundedRectShape hoverRect =
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX(),
                                    this.getY() - 1.0F,
                                    this.getWidth(),
                                    this.getHeight() + 1.0F);

            renderer.drawRect(
                    hoverRect.setColor(new Color(255, 255, 255, (int) (hoverProgress * 8.0F))));
        }

        if (toggleProgress > 0.0F) {
            Color color =
                    ColorUtil.animatedBlend(
                            this.getY() + this.getHeight() / 2.0F,
                            Astralis.getInstance().getPrimaryColor(),
                            Astralis.getInstance().getSecondaryColor());
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX(),
                                    this.getY() - 1.0F,
                                    this.getWidth(),
                                    this.getHeight() + 1.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setHorizontalGradient(
                                                    ColorUtil.withAlpha(
                                                                    color,
                                                                    (int) (toggleProgress * 35.0F))
                                                            .getRGB(),
                                                    ColorUtil.withAlpha(color, 0).getRGB())));
        }

        RoundedRectShape dividerRect =
                RoundedRectShape.obtain()
                        .setBounds(
                                this.getX(),
                                this.getY() + this.getHeight() - 0.5F,
                                this.getWidth(),
                                0.5F);

        renderer.drawRect(dividerRect.setColor(new Color(40, 40, 42)));
        SizedFont nameFont =
                FontManager.getFont(PanelClickGuiScreen.boldOrMediumFontName(false), 7.0F);
        SizedFont suffixFont = FontManager.getFont(PanelClickGuiScreen.regularFontName(), 6.0F);
        String displayName = this.binding ? "..." : this.module.getName();
        float nameWidth = nameFont.getStringWidth(displayName);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(displayName)
                        .setPosition(this.getX() + 8.0F, this.getY())
                        .centerVertically(this.getHeight())
                        .setFont(nameFont.getMsdfFont())
                        .setFontSize(nameFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        String suffix = this.module.getSuffix();
        if (suffix != null && !suffix.isEmpty()) {
            TextDrawRequest suffixText =
                    TextDrawRequest.obtain()
                            .setText(suffix)
                            .setPosition(this.getX() + 8.0F + nameWidth + 4.0F, this.getY())
                            .centerVertically(this.getHeight())
                            .setFont(suffixFont.getMsdfFont())
                            .setFontSize(suffixFont.getSize())
                            .enableShadow();

            renderer.drawText(suffixText.setColor(new Color(90, 90, 95).getRGB()));
        }

        this.drawToggleSwitch(renderer);
    }

    private void drawToggleSwitch(Render2DContext renderer) {

        if (this.module.canToggle()) {
            float switchX = this.getX() + this.getWidth() - 16.0F - 6.0F;
            float switchY = this.getY() + (this.getHeight() - 9.0F) / 2.0F;
            float toggleProgress = (float) this.toggleAnimation.getValue();
            RoundedRectShape trackRect =
                    RoundedRectShape.obtain()
                            .setBounds(switchX, switchY, 16.0F, 9.0F)
                            .setRadius(4.5F);

            Color color = new Color(45, 45, 48);

            renderer.drawRect(
                    trackRect.setColor(
                            ColorUtil.lerpColor(color, new Color(25, 25, 28), toggleProgress)));
            if (toggleProgress > 0.0F) {
                Color color2 =
                        ColorUtil.animatedBlend(
                                switchY,
                                Astralis.getInstance().getPrimaryColor(),
                                Astralis.getInstance().getSecondaryColor());
                Color color3 =
                        ColorUtil.animatedBlend(
                                switchY + 9.0F,
                                Astralis.getInstance().getPrimaryColor(),
                                Astralis.getInstance().getSecondaryColor());
                RoundedRectShape fillRect =
                        RoundedRectShape.obtain()
                                .setBounds(switchX, switchY, 16.0F, 9.0F)
                                .setRadius(4.5F);
                ShapePaint fillPaint = ShapePaint.obtain();

                int fillStartRgb =
                        ColorUtil.lerpColor(new Color(45, 45, 48), color2, toggleProgress).getRGB();

                renderer.drawRect(
                        fillRect.setPaint(
                                fillPaint.setVerticalGradient(
                                        fillStartRgb,
                                        ColorUtil.lerpColor(
                                                        new Color(45, 45, 48),
                                                        color3,
                                                        toggleProgress)
                                                .getRGB())));
            }

            float knobOffX = switchX + 3.0F + 2.0F;
            float knobOnX = switchX + 16.0F - 3.0F - 2.0F;
            float knobX = knobOffX + (knobOnX - knobOffX) * toggleProgress;
            float knobY = switchY + 4.5F;
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F)
                            .setRadius(3.0F)
                            .setColor(Color.WHITE));
        }
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {

        if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight())) {
            if (mouseButtonEvent.button() == 0 && this.module.canToggle()) {
                this.module.toggle();
            } else if (mouseButtonEvent.button() == 2) {
                this.binding = !this.binding;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if (this.binding) {
            if (event.isEscape()) {
                this.module.setKey(0);
            } else {
                this.module.setKey(event.key());
            }

            Astralis.getInstance().getConfigManager().syncKeybindsToServer();
            this.binding = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        return false;
    }

    @Generated
    public Module getModule() {
        return this.module;
    }

    @Generated
    public ArrayList<SettingWidget> getSettingWidgets() {
        return this.settingWidgets;
    }

    @Generated
    public HashMap<Class<?>, ArrayList<SettingWidget>> getSettingWidgetsByType() {
        return this.settingWidgetsByType;
    }

    @Generated
    public Animation getToggleAnimation() {
        return this.toggleAnimation;
    }

    @Generated
    public Animation getHoverAnimation() {
        return this.hoverAnimation;
    }

    @Generated
    public boolean isBinding() {
        return this.binding;
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
        } else if (restartIndex <= 7 && setting2 instanceof SettingGroup) {
            return 7;
        } else {
            return restartIndex <= 8 && setting2 instanceof TextSetting ? 8 : 9;
        }
    }
}
