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
import astralis.ui.screen.DropdownClickGuiScreen;
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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/** Dropdown ClickGUI module row with its expandable setting widgets. */
public class DropdownModuleEntry extends SettingWidget {
    private final ArrayList<SettingWidget> settingWidgets;
    private final HashMap<Class<?>, ArrayList<SettingWidget>> settingWidgetsByType;
    private final List<DropdownModuleEntry> panelEntries;
    private final Module module;
    private boolean expanded;
    private final Animation expandAnimation;
    private final Animation toggleAnimation;
    private boolean expanding;
    private boolean expandAnimating;
    private boolean listeningForKeybind;
    private float settingsHeight;
    private float toggleProgress;
    private int gradientTopColor;
    private int gradientBottomColor;
    private boolean inViewport;
    private boolean lastBackgroundStatic;
    private boolean staticBackgroundDirty;
    private float staticBackgroundX;
    private float staticBackgroundY;
    private float retainedTextX;
    private float retainedTextY;
    private float retainedTextWidth;
    private boolean staticBackgroundPositionCached;
    private boolean retainedTextBoundsCached;
    private boolean retainedTextDirty;
    private final DropdownClickGuiScreen screen;

    public DropdownModuleEntry(
            DropdownClickGuiScreen screen,
            Module module2,
            List<DropdownModuleEntry> panelEntries,
            float entryX,
            float entryY,
            float entryWidth,
            float entryHeight) {
        super(entryX, entryY, entryWidth, entryHeight);
        this.settingWidgets = new ArrayList<>();
        this.settingWidgetsByType = new HashMap<>();

        this.expandAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.expanding = true;
        this.expandAnimating = false;
        this.inViewport = true;
        this.module = module2;
        this.panelEntries = panelEntries;

        this.toggleAnimation = new Animation(Easing.EASE_OUT_QUINT, 300L);
        this.toggleAnimation.setValue(module2.isEnabled() ? 1.0 : 0.0);

        for (Setting setting2 : module2.getSettings()) {
            this.addValue(setting2);
        }

        this.screen = screen;
    }

    private void addValue(Setting<?> addedSetting) {
        ArrayList typeWidgets =
                this.settingWidgetsByType.computeIfAbsent(
                        addedSetting.getClass(), settingClass -> new ArrayList<>());
        Setting setting2 = addedSetting;
        byte restartIndex = 0;

        SettingWidget settingWidget =
                switch (typeSwitch0(setting2, restartIndex)) {
                    case 0 -> {
                        ActionSetting actionSetting = (ActionSetting) setting2;
                        yield new ActionSettingWidget(actionSetting);
                    }
                    case 1 -> {
                        BooleanSetting booleanSetting = (BooleanSetting) setting2;
                        yield new BooleanSettingWidget(booleanSetting, typeWidgets);
                    }
                    case 2 -> {
                        BlockListSetting blockListSetting = (BlockListSetting) setting2;
                        yield new BlockListSettingWidget(blockListSetting, typeWidgets);
                    }
                    case 3 -> {
                        NumberSetting numberSetting = (NumberSetting) setting2;
                        yield new NumberSettingWidget(numberSetting, typeWidgets);
                    }
                    case 4 -> {
                        RangeSetting rangeSetting = (RangeSetting) setting2;
                        yield new RangeSettingWidget(rangeSetting, typeWidgets);
                    }
                    case 5 -> {
                        ModeSetting modeSetting = (ModeSetting) setting2;
                        yield new ModeSettingWidget(modeSetting, typeWidgets);
                    }
                    case 6 -> {
                        ColorSetting colorSetting = (ColorSetting) setting2;
                        yield new ColorSettingWidget(colorSetting, typeWidgets);
                    }
                    case 7 -> {
                        SettingGroup settingGroup = (SettingGroup) setting2;
                        yield settingGroup.isCollapsed()
                                ? new BooleanChipGroupWidget(settingGroup)
                                : new SettingGroupWidget(settingGroup);
                    }
                    case 8 -> {
                        TextSetting textSetting = (TextSetting) setting2;
                        yield new TextSettingWidget(textSetting, typeWidgets);
                    }
                    default -> null;
                };
        if (settingWidget != null) {
            this.settingWidgets.add(settingWidget);
            typeWidgets.add(settingWidget);
        } else {
            PrintStream stdout = System.out;
            String settingText = String.valueOf(addedSetting);

            stdout.println("Unexpected value: " + settingText);
        }

        if (!(addedSetting instanceof SettingGroup)
                && !(addedSetting instanceof ColorSetting)
                && addedSetting.getChildren() != null) {
            for (Setting setting3 : addedSetting.getChildren()) {
                this.addValue(setting3);
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.invalidateRetainedText();
        if (this.expanded) {
            this.settingWidgets.forEach(
                    settingWidget -> {
                        if (settingWidget.getSetting() == null
                                || !settingWidget.getSetting().isHidden()) {
                            settingWidget.release(mouseButtonEvent);
                        }
                    });
        }

        super.release(mouseButtonEvent);
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        this.invalidateRetainedText();
        if (this.expanded) {
            this.settingWidgets.forEach(
                    settingWidget -> {
                        if (settingWidget.getSetting() == null
                                || !settingWidget.getSetting().isHidden()) {
                            settingWidget.click(mouseButtonEvent);
                        }
                    });
        }

        if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight())) {
            if (mouseButtonEvent.button() == 0 && this.module.canToggle()) {
                this.module.toggle();
            } else if (mouseButtonEvent.button() == 1 && !this.settingWidgets.isEmpty()) {
                this.expanded = !this.expanded;
                this.expanding = this.expanded;
                this.expandAnimating = true;
                this.expandAnimation.restart();
            } else if (mouseButtonEvent.button() == 2) {
                this.listeningForKeybind = !this.listeningForKeybind;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        this.invalidateRetainedText();
        if (this.listeningForKeybind) {
            if (event.isEscape()) {
                this.module.setKey(0);
            } else {
                this.module.setKey(event.key());
            }

            Astralis.getInstance().getConfigManager().syncKeybindsToServer();
            this.listeningForKeybind = false;
            return true;
        } else {
            if (this.expanded) {
                for (SettingWidget settingWidget : this.settingWidgets) {
                    if ((settingWidget.getSetting() == null
                                    || !settingWidget.getSetting().isHidden())
                            && settingWidget.keyPressed(event)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        this.invalidateRetainedText();
        if (this.expanded) {
            for (SettingWidget settingWidget : this.settingWidgets) {
                if ((settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden())
                        && settingWidget.charTyped(characterEvent)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        int primaryArgb = Astralis.getInstance().getPrimaryColor().getRGB();
        int secondaryArgb = Astralis.getInstance().getSecondaryColor().getRGB();
        this.update(
                mouseX,
                mouseY,
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                false,
                primaryArgb,
                secondaryArgb,
                System.currentTimeMillis() / 1000.0);
        this.renderBackground(renderer);
        this.renderSettings(renderer, mouseX, mouseY);

        this.renderLabel(renderer, FontManager.getFont("Product Sans Medium", 8.0F));
    }

    public void update(
            float mouseX,
            float mouseY,
            float viewportTop,
            float viewportBottom,
            boolean cullOffscreen,
            int primaryArgb,
            int secondaryArgb,
            double timeSeconds) {
        float targetSettingsHeight = 0.0F;
        if (this.expanded || this.expandAnimating) {
            for (SettingWidget settingWidget : this.settingWidgets) {
                if (settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden()) {
                    targetSettingsHeight += settingWidget.getHeight();
                }
            }
        }

        if (this.expandAnimating) {
            if (this.expanding) {
                this.expandAnimation.animateTo(targetSettingsHeight);
                this.settingsHeight = (float) this.expandAnimation.getValue();
                if (this.settingsHeight >= targetSettingsHeight - 0.1F) {
                    this.expandAnimating = false;
                    this.settingsHeight = targetSettingsHeight;
                }
            } else {
                this.expandAnimation.animateTo(0.0);
                this.settingsHeight = (float) this.expandAnimation.getValue();
                if (this.settingsHeight <= 0.1F) {
                    this.expandAnimating = false;
                    this.settingsHeight = 0.0F;
                }
            }
        } else if (this.expanded) {
            this.settingsHeight = targetSettingsHeight;
            this.expandAnimation.setValue(targetSettingsHeight);
        }

        this.toggleAnimation.animateTo(this.module.isEnabled() ? 1.0 : 0.0);
        this.toggleProgress = (float) this.toggleAnimation.getValue();
        this.gradientTopColor =
                ColorUtil.animatedBlendArgb(this.getY(), primaryArgb, secondaryArgb, timeSeconds);
        this.gradientBottomColor =
                ColorUtil.animatedBlendArgb(
                        this.getY() + this.getHeight(), primaryArgb, secondaryArgb, timeSeconds);
        this.inViewport =
                !cullOffscreen
                        || this.getY() + this.getHeight() + this.settingsHeight
                                        >= viewportTop - 12.0F
                                && this.getY() <= viewportBottom + 12.0F;
        if (this.inViewport
                && RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX() + 2.0F,
                        this.getY() + 2.0F,
                        this.getWidth() - 4.0F,
                        this.getHeight() - 2.0F)) {
            this.screen.setHoveredDescription(this.module.getDescription());
        }

        boolean backgroundStatic = this.isBackgroundStatic();
        if (this.lastBackgroundStatic != backgroundStatic) {
            this.lastBackgroundStatic = backgroundStatic;
            this.staticBackgroundDirty = true;
        }

        if (backgroundStatic
                && (!this.staticBackgroundPositionCached
                        || Math.abs(this.staticBackgroundX - this.getX()) > 0.01F
                        || Math.abs(this.staticBackgroundY - this.getY()) > 0.01F)) {
            this.staticBackgroundX = this.getX();
            this.staticBackgroundY = this.getY();
            this.staticBackgroundPositionCached = true;
            this.staticBackgroundDirty = true;
        }

        if (this.expanded
                && (!this.retainedTextBoundsCached
                        || Math.abs(this.retainedTextX - this.getX()) > 0.01F
                        || Math.abs(this.retainedTextY - this.getY()) > 0.01F
                        || Math.abs(this.retainedTextWidth - this.getWidth()) > 0.01F)) {
            this.retainedTextX = this.getX();
            this.retainedTextY = this.getY();
            this.retainedTextWidth = this.getWidth();
            this.retainedTextBoundsCached = true;
            this.invalidateRetainedText();
        }
    }

    public void renderBackground(Render2DContext renderer) {

        if (this.settingsHeight > 0.0F) {
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX() + 2.0F,
                                    this.getY() + this.getHeight() - 10.0F,
                                    this.getWidth() - 4.0F,
                                    this.settingsHeight + 10.0F)
                            .setCornerRadii(0.0F, 0.0F, 6.0F, 6.0F)
                            .setColor(ClickGuiPaletteColors.MODULE_SETTINGS_BACKGROUND));
        }

        byte lastEntryRadius;
        if (this.panelEntries.getLast() == this) {

            lastEntryRadius = 6;
        } else {
            lastEntryRadius = 0;
        }

        byte cornerRadius = lastEntryRadius;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(
                                this.getX(),
                                this.getY(),
                                this.getWidth(),
                                this.getHeight()
                                        + this.settingsHeight
                                        + 1.0F
                                        + (this.panelEntries.getLast() == this ? 3 : 0))
                        .setCornerRadii(0.0F, 0.0F, cornerRadius, cornerRadius)
                        .setColor(ClickGuiPaletteColors.MODULE_ENTRY_BACKGROUND));
        float buttonX = this.getX() + 2.0F;
        float buttonY = this.getY() + 2.0F;
        float buttonWidth = this.getWidth() - 4.0F;
        float buttonHeight = this.getHeight() - 2.0F;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(buttonX, buttonY, buttonWidth, buttonHeight)
                        .setRadius(3.0F)
                        .setColor(ClickGuiPaletteColors.MODULE_BUTTON_BACKGROUND));
    }

    public void renderToggleOverlay(Render2DContext renderer) {
        if (this.toggleProgress > 0.0F) {
            float buttonX = this.getX() + 2.0F;
            float buttonY = this.getY() + 2.0F;
            float buttonWidth = this.getWidth() - 4.0F;
            float buttonHeight = this.getHeight() - 2.0F;
            float overlayWidth = buttonWidth * this.toggleProgress;
            float overlayHeight = buttonHeight * this.toggleProgress;
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    buttonX + buttonWidth / 2.0F - overlayWidth / 2.0F,
                                    buttonY + buttonHeight / 2.0F - overlayHeight / 2.0F,
                                    overlayWidth,
                                    overlayHeight)
                            .setRadius(3.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(
                                                    this.gradientTopColor,
                                                    this.gradientBottomColor)));
        }
    }

    public boolean isBackgroundStatic() {
        return !this.expanded && !this.expandAnimating && !this.listeningForKeybind;
    }

    public boolean consumeStaticBackgroundDirty() {
        if (!this.staticBackgroundDirty) {
            return false;
        }

        this.staticBackgroundDirty = false;
        return true;
    }

    public void renderSettings(Render2DContext renderer, float mouseX, float mouseY) {
        if (this.settingsHeight > 0.0F) {
            float settingsY = this.getY() + this.getHeight();
            renderer.beginScissor(this.getX(), settingsY, this.getWidth(), this.settingsHeight);
            renderer.beginDeferred();
            boolean canRetainText = this.canRetainSettingsText();
            if (canRetainText) {
                renderer.beginRetainedText(this, this.retainedTextDirty);
                this.retainedTextDirty = false;
            } else {
                this.invalidateRetainedText();
            }

            float widgetY = settingsY;

            for (SettingWidget settingWidget : this.settingWidgets) {
                if (settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden()) {
                    settingWidget.setX(this.getX());
                    settingWidget.setY(widgetY);
                    settingWidget.setWidth(this.getWidth());
                    settingWidget.render(renderer, mouseX, mouseY);
                    widgetY += settingWidget.getHeight();
                }
            }

            renderer.endDeferred();
            if (canRetainText) {
                renderer.endRetainedText(this);
            }

            renderer.endScissor();
        }
    }

    private boolean canRetainSettingsText() {
        if (this.expandAnimating) {
            return false;
        }

        for (SettingWidget settingWidget : this.settingWidgets) {
            if ((settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden())
                    && !settingWidget.isSettled()) {
                return false;
            }
        }

        return true;
    }

    public void invalidateRetainedText() {
        this.retainedTextDirty = true;
    }

    public void renderLabel(Render2DContext renderer, SizedFont labelFont) {
        float buttonX = this.getX() + 2.0F;
        float buttonY = this.getY() + 2.0F;
        float buttonWidth = this.getWidth() - 4.0F;
        float buttonHeight = this.getHeight() - 2.0F;
        TextDrawRequest labelText = TextDrawRequest.obtain();
        String labelString;
        if (this.listeningForKeybind) {

            labelString = "...";
        } else {
            labelString = this.module.getName();
        }

        renderer.drawText(
                labelText
                        .setText(labelString)
                        .setPosition(buttonX, buttonY)
                        .centerInBox(buttonWidth, buttonHeight)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.white.getRGB()));
    }

    public boolean isInViewport() {
        return this.inViewport;
    }

    public void extractSettingItems(GuiGraphicsExtractor guiGraphicsExtractor) {
        if (!(this.settingsHeight <= 0.01F)) {
            float settingsY = this.getY() + this.getHeight();
            guiGraphicsExtractor.enableScissor(
                    Math.round(this.getX()),
                    Math.round(settingsY),
                    Math.round(this.getX() + this.getWidth()),
                    Math.round(settingsY + this.settingsHeight));
            float widgetY = settingsY;

            for (SettingWidget settingWidget : this.settingWidgets) {
                if (settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden()) {
                    settingWidget.setX(this.getX());
                    settingWidget.setY(widgetY);
                    settingWidget.setWidth(this.getWidth());
                    if (settingWidget instanceof BlockListSettingWidget blockListWidget) {
                        blockListWidget.renderBlockIcons(guiGraphicsExtractor);
                    }

                    widgetY += settingWidget.getHeight();
                }
            }

            guiGraphicsExtractor.disableScissor();
        }
    }

    @Override
    public void renderMask(Render2DContext renderer) {

        float animatedSettingsHeight = 0.0F;
        float targetSettingsHeight = 0.0F;
        if (this.expanded || this.expandAnimating) {
            for (SettingWidget settingWidget : this.settingWidgets) {
                if (settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden()) {
                    targetSettingsHeight += settingWidget.getHeight();
                }
            }
        }

        if (this.expandAnimating) {
            animatedSettingsHeight = (float) this.expandAnimation.getValue();
        } else if (this.expanded) {
            animatedSettingsHeight = targetSettingsHeight;
        }

        byte lastEntryRadius;
        if (this.panelEntries.getLast() == this) {

            lastEntryRadius = 6;
        } else {
            lastEntryRadius = 0;
        }

        byte cornerRadius = lastEntryRadius;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(
                                this.getX(),
                                this.getY(),
                                this.getWidth(),
                                this.getHeight()
                                        + animatedSettingsHeight
                                        + (this.panelEntries.getLast() == this ? 3 : 0))
                        .setCornerRadii(0.0F, 0.0F, cornerRadius, cornerRadius)
                        .setColor(Color.black));
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
    public List<DropdownModuleEntry> getPanelEntries() {
        return this.panelEntries;
    }

    @Generated
    public Module getModule() {
        return this.module;
    }

    @Generated
    public boolean isExpanded() {
        return this.expanded;
    }

    @Generated
    public Animation getExpandAnimation() {
        return this.expandAnimation;
    }

    @Generated
    public Animation getToggleAnimation() {
        return this.toggleAnimation;
    }

    @Generated
    public boolean isExpanding() {
        return this.expanding;
    }

    @Generated
    public boolean isExpandAnimating() {
        return this.expandAnimating;
    }

    @Generated
    public boolean isListeningForKeybind() {
        return this.listeningForKeybind;
    }

    @Generated
    public float getToggleProgress() {
        return this.toggleProgress;
    }

    @Generated
    public int getGradientTopColor() {
        return this.gradientTopColor;
    }

    @Generated
    public int getGradientBottomColor() {
        return this.gradientBottomColor;
    }

    @Generated
    public boolean isLastBackgroundStatic() {
        return this.lastBackgroundStatic;
    }

    @Generated
    public boolean isStaticBackgroundDirty() {
        return this.staticBackgroundDirty;
    }

    @Generated
    public float getStaticBackgroundX() {
        return this.staticBackgroundX;
    }

    @Generated
    public float getStaticBackgroundY() {
        return this.staticBackgroundY;
    }

    @Generated
    public float getRetainedTextX() {
        return this.retainedTextX;
    }

    @Generated
    public float getRetainedTextY() {
        return this.retainedTextY;
    }

    @Generated
    public float getRetainedTextWidth() {
        return this.retainedTextWidth;
    }

    @Generated
    public boolean isStaticBackgroundPositionCached() {
        return this.staticBackgroundPositionCached;
    }

    @Generated
    public boolean isRetainedTextBoundsCached() {
        return this.retainedTextBoundsCached;
    }

    @Generated
    public boolean isRetainedTextDirty() {
        return this.retainedTextDirty;
    }

    @Generated
    public DropdownClickGuiScreen getScreen() {
        return this.screen;
    }

    @Generated
    public float getSettingsHeight() {
        return this.settingsHeight;
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
