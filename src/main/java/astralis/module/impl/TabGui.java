package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.InGameKeyPressEvent;
import astralis.event.impl.Render2DEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.ui.hud.Draggable;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

public class TabGui extends Module {
    private static final float ROW_HEIGHT = 11.0F;
    private static final float CATEGORY_COLUMN_WIDTH = 70.0F;
    private static final float MODULE_COLUMN_WIDTH = 110.0F;
    private static final float SETTING_COLUMN_WIDTH = 140.0F;
    private static final float TEXT_PADDING = 4.0F;
    private static final float CORNER_RADIUS = 3.0F;
    private static final float SNAP_THRESHOLD = 0.5F;
    private final int[] selectedIndices;
    private final Category[] categories;
    private final Draggable hudElement;
    private final NumberSetting scale;
    private final ColorSetting colors;
    private int activeColumn;
    private boolean editingNumber;
    private boolean animationInitialized;
    private float categoryHighlightY;
    private float moduleColumnY;
    private float moduleHighlightY;
    private float settingColumnY;
    private float settingHighlightY;
    private float animatedModuleWidth;
    private float animatedSettingWidth;
    private float smoothingFactor;
    private float[] categoryAlphas;
    private float[] moduleAlphas;
    private float[] settingAlphas;
    private long lastFrameNanos;

    public TabGui() {

        super(MODULE_ID, "", "", Category.Misc);
        this.selectedIndices = new int[3];
        this.categories = Category.values();

        this.scale = new NumberSetting(SCALE_SETTING_ID, 1.0F, 0.5F, 2.0F, 0.05F);

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);
        this.categoryAlphas = new float[0];
        this.moduleAlphas = new float[0];
        this.settingAlphas = new float[0];

        this.hudElement =
                new Draggable("Tab GUI", 70.0F, this.categories.length * 11.0F, 4.0F, 20.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return this.isEnabled() && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        ModeSetting modeSetting = this.colors.getColorMode();

        modeSetting.setDefault("Client");
        this.addSettings(this.scale, this.colors);
    }

    private String getSettingLabel(Setting<?> setting) {
        Setting setting2 = setting;
        byte caseStart = 0;

        return switch (typeSwitch0(setting2, caseStart)) {
            case 0 -> {
                BooleanSetting booleanSetting = (BooleanSetting) setting2;
                String booleanName = booleanSetting.getName();
                String stateText;
                if (booleanSetting.getValue()) {

                    stateText = "ON";
                } else {

                    stateText = "OFF";
                }

                yield booleanName + ": " + stateText;
            }
            case 1 -> {
                NumberSetting numberSetting = (NumberSetting) setting2;
                String numberName = numberSetting.getName();

                String valueText = String.format("%.2f", numberSetting.getValue());

                yield numberName + ": " + valueText;
            }
            case 2 -> {
                ModeSetting modeSetting = (ModeSetting) setting2;
                String modeName = modeSetting.getName();
                String modeValueName = modeSetting.getValue().getName();

                yield modeName + ": " + modeValueName;
            }
            default -> setting.getName();
        };
    }

    private void activateSelectedSetting() {
        List categoryModules =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModulesFromCategory(this.categories[this.selectedIndices[0]]);
        if (!categoryModules.isEmpty()) {
            List settings =
                    this.getVisibleSettings((Module) categoryModules.get(this.selectedIndices[1]));
            if (this.selectedIndices[2] < settings.size()) {
                Setting setting2 = (Setting) settings.get(this.selectedIndices[2]);
                Setting setting3 = setting2;
                byte caseStart = 0;
                switch (typeSwitch1(setting3, caseStart)) {
                    case 0:
                        BooleanSetting booleanSetting = (BooleanSetting) setting3;
                        booleanSetting.setValueRaw(!booleanSetting.getValue());
                        break;
                    case 1:
                        this.editingNumber = !this.editingNumber;
                        break;
                    case 2:
                        ModeSetting modeSetting = (ModeSetting) setting3;
                        List modes = modeSetting.getValues();
                        int currentIndex = modes.indexOf(modeSetting.getValue());
                        modeSetting.setDefault(
                                ((Mode) modes.get((currentIndex + 1) % modes.size())).getName());
                }
            }
        }
    }

    private void adjustSelectedNumber(int direction) {
        List categoryModules =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModulesFromCategory(this.categories[this.selectedIndices[0]]);
        if (!categoryModules.isEmpty()) {
            List settings =
                    this.getVisibleSettings((Module) categoryModules.get(this.selectedIndices[1]));
            if (this.selectedIndices[2] < settings.size()) {
                if (settings.get(this.selectedIndices[2]) instanceof NumberSetting numberSetting) {
                    float newValue = numberSetting.getValue() + numberSetting.getStep() * direction;
                    numberSetting.setValueRaw(
                            Math.clamp(newValue, numberSetting.getMin(), numberSetting.getMax()));
                }
            }
        }
    }

    private List<Setting<?>> getVisibleSettings(Module module2) {
        java.util.ArrayList<Setting<?>> allSettings = new java.util.ArrayList<>();
        this.collectSettings(module2.getSettings(), allSettings);
        return allSettings.stream()
                .filter(
                        setting -> {
                            return !setting.isHidden();
                        })
                .toList();
    }

    private float[] resizeAlphas(float[] alphas, int size, float defaultAlpha) {
        if (alphas.length == size) {
            return alphas;
        }

        float[] resized = new float[size];

        for (int i = 0; i < size; i++) {
            resized[i] = i < alphas.length ? alphas[i] : defaultAlpha;
        }

        return resized;
    }

    @EventTarget
    public void onInGameKeyPress(InGameKeyPressEvent event) {
        switch (event.getKey()) {
            case 257:
                if (this.activeColumn == 1) {
                    List toggleModules =
                            Astralis.getInstance()
                                    .getModuleManager()
                                    .getModulesFromCategory(
                                            this.categories[this.selectedIndices[0]]);
                    if (!toggleModules.isEmpty()) {
                        ((Module) toggleModules.get(this.selectedIndices[1])).toggle();
                    }
                } else if (this.activeColumn == 2) {
                    this.activateSelectedSetting();
                }
            case 258:
            case 259:
            case 260:
            case 261:
            default:
                break;
            case 262:
                if (this.activeColumn == 0) {
                    List openModules =
                            Astralis.getInstance()
                                    .getModuleManager()
                                    .getModulesFromCategory(
                                            this.categories[this.selectedIndices[0]]);
                    if (!openModules.isEmpty()) {
                        this.selectedIndices[1] = 0;
                        this.activeColumn = 1;
                    }
                } else if (this.activeColumn == 1) {
                    List selectedModules =
                            Astralis.getInstance()
                                    .getModuleManager()
                                    .getModulesFromCategory(
                                            this.categories[this.selectedIndices[0]]);
                    if (!selectedModules.isEmpty()
                            && this.selectedIndices[1] < selectedModules.size()) {
                        Module module2 = (Module) selectedModules.get(this.selectedIndices[1]);
                        long settingCount = this.getVisibleSettings(module2).size();
                        if (settingCount > 0L) {
                            this.selectedIndices[2] = 0;
                            this.editingNumber = false;

                            this.activeColumn = 2;
                        }
                    }
                }
                break;
            case 263:
                if (this.editingNumber) {
                    this.editingNumber = false;
                } else if (this.activeColumn > 0) {
                    this.activeColumn--;
                }
                break;
            case 264:
                if (this.editingNumber) {
                    this.adjustSelectedNumber(-1);
                } else {
                    this.selectedIndices[this.activeColumn] =
                            (this.selectedIndices[this.activeColumn] + 1)
                                    % this.getColumnEntryCount(this.activeColumn);
                }
                break;
            case 265:
                if (this.editingNumber) {
                    this.adjustSelectedNumber(1);
                } else {
                    int entryCount = this.getColumnEntryCount(this.activeColumn);
                    this.selectedIndices[this.activeColumn] =
                            (this.selectedIndices[this.activeColumn] - 1 + entryCount) % entryCount;
                }
        }
    }

    private float animateToward(float current, float target) {
        return Math.abs(target - current) <= 0.5F
                ? target
                : current + (target - current) * this.smoothingFactor;
    }

    private int getColumnEntryCount(int column) {
        if (column == 0) {
            return this.categories.length;
        } else {
            List modules =
                    Astralis.getInstance()
                            .getModuleManager()
                            .getModulesFromCategory(this.categories[this.selectedIndices[0]]);
            if (column == 1) {
                return Math.max(1, modules.size());
            } else {
                return column == 2 && !modules.isEmpty()
                        ? Math.max(
                                1,
                                this.getVisibleSettings(
                                                (Module) modules.get(this.selectedIndices[1]))
                                        .size())
                        : 1;
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {

        this.hudElement.setScale(this.scale.getValue());

        SizedFont font = FontManager.getThemedFont("Product Sans Regular", 7.0F);
        float hudX = this.hudElement.getX();
        float hudY = this.hudElement.getY();
        float hudScale = this.scale.getValue();
        int textColor;

        Color color = new Color(12, 12, 12, Theme.getBackgroundAlpha());
        Color[] themeColors = this.colors.getColors();
        int gradientStart = themeColors[0].getRGB();
        int gradientEnd = themeColors[1].getRGB();
        long nowNanos = System.nanoTime();
        this.smoothingFactor =
                this.lastFrameNanos == 0L
                        ? 1.0F
                        : 1.0F
                                - (float)
                                        Math.pow(
                                                0.78F,
                                                Math.clamp(
                                                        (float) (nowNanos - this.lastFrameNanos)
                                                                / 1.0E9F
                                                                * 60.0F,
                                                        0.25F,
                                                        3.0F));
        this.lastFrameNanos = nowNanos;
        List modules =
                Astralis.getInstance()
                        .getModuleManager()
                        .getModulesFromCategory(this.categories[this.selectedIndices[0]]);
        if (this.selectedIndices[1] >= modules.size()) {
            this.selectedIndices[1] = 0;
        }

        List settings =
                modules.isEmpty()
                        ? List.of()
                        : this.getVisibleSettings((Module) modules.get(this.selectedIndices[1]));
        if (this.selectedIndices[2] >= settings.size()) {
            this.selectedIndices[2] = 0;
        }

        float targetCategoryY = hudY + this.selectedIndices[0] * 11.0F;
        float targetModuleColumnY = targetCategoryY;
        float targetModuleHighlightY =
                this.activeColumn > 0
                        ? targetModuleColumnY + this.selectedIndices[1] * 11.0F
                        : targetModuleColumnY;
        float targetSettingColumnY =
                this.activeColumn > 0 ? targetModuleHighlightY : targetModuleColumnY;
        float targetSettingHighlightY =
                this.activeColumn > 1
                        ? targetSettingColumnY + this.selectedIndices[2] * 11.0F
                        : targetSettingColumnY;
        if (!this.animationInitialized) {
            this.categoryHighlightY = targetCategoryY;
            this.moduleColumnY = targetModuleColumnY;
            this.moduleHighlightY = targetModuleHighlightY;
            this.settingColumnY = targetSettingColumnY;
            this.settingHighlightY = targetSettingHighlightY;
            this.animationInitialized = true;
        }

        this.categoryHighlightY = this.animateToward(this.categoryHighlightY, targetCategoryY);
        this.moduleColumnY = this.animateToward(this.moduleColumnY, targetModuleColumnY);
        this.moduleHighlightY = this.animateToward(this.moduleHighlightY, targetModuleHighlightY);
        this.settingColumnY = this.animateToward(this.settingColumnY, targetSettingColumnY);
        this.settingHighlightY =
                this.animateToward(this.settingHighlightY, targetSettingHighlightY);
        this.animatedModuleWidth =
                this.animateToward(
                        this.animatedModuleWidth,
                        this.activeColumn > 0 && !modules.isEmpty() ? 110.0F : 0.0F);
        this.animatedSettingWidth =
                this.animateToward(
                        this.animatedSettingWidth,
                        this.activeColumn > 1 && !settings.isEmpty() ? 140.0F : 0.0F);
        render2DEvent.getContext().pushPose();
        render2DEvent.getContext().translate(hudX, hudY);
        render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
        render2DEvent.getContext().translate(-hudX, -hudY);
        float categoryColumnHeight = this.categories.length * 11.0F;
        if (Effects.isTabGuiBlur()) {
            render2DEvent
                    .getRenderEffects()
                    .blurRoundedRectangle(
                            hudX, hudY, 70.0F * hudScale, categoryColumnHeight * hudScale, 3.0F);
        }

        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(hudX, hudY, 70.0F, categoryColumnHeight)
                                .setRadius(3.0F)
                                .setShadowEnabled(Effects.isTabGuiShadow())
                                .setColor(color));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(hudX, this.categoryHighlightY, 70.0F, 11.0F)
                                .setCornerRadii(
                                        this.selectedIndices[0] == 0 ? 3.0F : 0.0F,
                                        this.selectedIndices[0] == 0 ? 3.0F : 0.0F,
                                        this.selectedIndices[0] == this.categories.length - 1
                                                ? 3.0F
                                                : 0.0F,
                                        this.selectedIndices[0] == this.categories.length - 1
                                                ? 3.0F
                                                : 0.0F)
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setHorizontalGradient(
                                                        gradientStart, gradientEnd)));
        this.categoryAlphas =
                this.resizeAlphas(this.categoryAlphas, this.categories.length, 146.0F);

        for (int categoryIndex = 0; categoryIndex < this.categories.length; categoryIndex++) {
            float rowY = hudY + categoryIndex * 11.0F;
            boolean selected = categoryIndex == this.selectedIndices[0];
            this.categoryAlphas[categoryIndex] =
                    this.animateToward(
                            this.categoryAlphas[categoryIndex], selected ? 255.0F : 146.0F);
            Render2DContext textContext = render2DEvent.getContext();
            TextDrawRequest textRequest =
                    TextDrawRequest.obtain()
                            .setText(this.categories[categoryIndex].getName())
                            .setPosition(hudX + 4.0F, rowY)
                            .centerVertically(11.0F)
                            .setFont(font.getMsdfFont())
                            .setFontSize(font.getSize())
                            .enableShadow();
            if (selected) {

                textColor = 14406869;
            } else {

                textColor = 11052967;
            }

            textContext.drawText(
                    textRequest.setColor(
                            this.applyAlpha(textColor, this.categoryAlphas[categoryIndex])));
        }

        if (this.animatedModuleWidth <= 0.5F) {
            render2DEvent.getContext().popPose();
        } else {
            float moduleColumnX = hudX + 70.0F + 3.0F;
            float moduleColumnWidth =
                    Math.abs(this.animatedModuleWidth - 110.0F) <= 0.5F
                            ? 110.0F
                            : this.animatedModuleWidth;
            float moduleColumnHeight = Math.max(11.0F, modules.size() * 11.0F);
            if (Effects.isTabGuiBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangle(
                                hudX + (moduleColumnX - hudX) * hudScale,
                                hudY + (this.moduleColumnY - hudY) * hudScale,
                                moduleColumnWidth * hudScale,
                                moduleColumnHeight * hudScale,
                                3.0F);
            }

            render2DEvent
                    .getContext()
                    .drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(
                                            moduleColumnX,
                                            this.moduleColumnY,
                                            moduleColumnWidth,
                                            moduleColumnHeight)
                                    .setRadius(3.0F)
                                    .setShadowEnabled(Effects.isTabGuiShadow())
                                    .setColor(color));
            render2DEvent
                    .getContext()
                    .beginScissor(
                            moduleColumnX - 0.5F,
                            this.moduleColumnY - 0.5F,
                            moduleColumnWidth + 1.0F,
                            moduleColumnHeight + 1.0F);
            this.moduleAlphas = this.resizeAlphas(this.moduleAlphas, modules.size(), 128.0F);

            for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
                float moduleRowY = this.moduleColumnY + moduleIndex * 11.0F;
                Module module2 = (Module) modules.get(moduleIndex);
                boolean moduleSelected = moduleIndex == this.selectedIndices[1];
                this.moduleAlphas[moduleIndex] =
                        this.animateToward(
                                this.moduleAlphas[moduleIndex], moduleSelected ? 255.0F : 128.0F);
                if (moduleSelected) {
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(
                                                    moduleColumnX,
                                                    this.moduleHighlightY,
                                                    moduleColumnWidth,
                                                    11.0F)
                                            .setCornerRadii(
                                                    moduleIndex == 0 ? 3.0F : 0.0F,
                                                    moduleIndex == 0 ? 3.0F : 0.0F,
                                                    moduleIndex == modules.size() - 1 ? 3.0F : 0.0F,
                                                    moduleIndex == modules.size() - 1 ? 3.0F : 0.0F)
                                            .setPaint(
                                                    ShapePaint.obtain()
                                                            .setHorizontalGradient(
                                                                    gradientStart, gradientEnd)));
                }

                Render2DContext moduleTextContext = render2DEvent.getContext();
                TextDrawRequest moduleTextRequest =
                        TextDrawRequest.obtain()
                                .setText(module2.getName())
                                .setPosition(moduleColumnX + 4.0F, moduleRowY)
                                .centerVertically(11.0F)
                                .setFont(font.getMsdfFont())
                                .setFontSize(font.getSize())
                                .enableShadow();
                if (moduleSelected) {

                    textColor = 14406869;
                } else if (module2.isEnabled()) {

                    textColor = 16777215;
                } else {

                    textColor = 8421504;
                }

                moduleTextContext.drawText(
                        moduleTextRequest.setColor(
                                this.applyAlpha(textColor, this.moduleAlphas[moduleIndex])));
            }

            render2DEvent.getContext().endScissor();
            if (!(this.animatedSettingWidth <= 0.5F) && !modules.isEmpty()) {
                float settingColumnX = moduleColumnX + moduleColumnWidth + 3.0F;
                float settingColumnWidth =
                        Math.abs(this.animatedSettingWidth - 140.0F) <= 0.5F
                                ? 140.0F
                                : this.animatedSettingWidth;
                float settingColumnHeight = Math.max(11.0F, settings.size() * 11.0F);
                if (Effects.isTabGuiBlur()) {
                    render2DEvent
                            .getRenderEffects()
                            .blurRoundedRectangle(
                                    hudX + (settingColumnX - hudX) * hudScale,
                                    hudY + (this.settingColumnY - hudY) * hudScale,
                                    settingColumnWidth * hudScale,
                                    settingColumnHeight * hudScale,
                                    3.0F);
                }

                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(
                                                settingColumnX,
                                                this.settingColumnY,
                                                settingColumnWidth,
                                                settingColumnHeight)
                                        .setRadius(3.0F)
                                        .setShadowEnabled(Effects.isTabGuiShadow())
                                        .setColor(color));
                render2DEvent
                        .getContext()
                        .beginScissor(
                                settingColumnX - 0.5F,
                                this.settingColumnY - 0.5F,
                                settingColumnWidth + 1.0F,
                                settingColumnHeight + 1.0F);
                this.settingAlphas = this.resizeAlphas(this.settingAlphas, settings.size(), 128.0F);

                for (int settingIndex = 0; settingIndex < settings.size(); settingIndex++) {
                    float settingRowY = this.settingColumnY + settingIndex * 11.0F;
                    Setting setting2 = (Setting) settings.get(settingIndex);
                    boolean settingSelected = settingIndex == this.selectedIndices[2];
                    this.settingAlphas[settingIndex] =
                            this.animateToward(
                                    this.settingAlphas[settingIndex],
                                    settingSelected ? 255.0F : 128.0F);
                    if (settingSelected) {
                        render2DEvent
                                .getContext()
                                .drawRect(
                                        RoundedRectShape.obtain()
                                                .setBounds(
                                                        settingColumnX,
                                                        this.settingHighlightY,
                                                        settingColumnWidth,
                                                        11.0F)
                                                .setCornerRadii(
                                                        settingIndex == 0 ? 3.0F : 0.0F,
                                                        settingIndex == 0 ? 3.0F : 0.0F,
                                                        settingIndex == settings.size() - 1
                                                                ? 3.0F
                                                                : 0.0F,
                                                        settingIndex == settings.size() - 1
                                                                ? 3.0F
                                                                : 0.0F)
                                                .setPaint(
                                                        ShapePaint.obtain()
                                                                .setHorizontalGradient(
                                                                        gradientStart,
                                                                        gradientEnd)));
                    }

                    Render2DContext settingTextContext = render2DEvent.getContext();
                    TextDrawRequest settingTextRequest =
                            TextDrawRequest.obtain()
                                    .setText(this.getSettingLabel(setting2))
                                    .setPosition(settingColumnX + 4.0F, settingRowY)
                                    .centerVertically(11.0F)
                                    .setFont(font.getMsdfFont())
                                    .setFontSize(font.getSize())
                                    .enableShadow();
                    if (settingSelected && this.editingNumber) {

                        textColor = 8454016;
                    } else if (settingSelected) {

                        textColor = 14406869;
                    } else {

                        textColor = 11052967;
                    }

                    settingTextContext.drawText(
                            settingTextRequest.setColor(
                                    this.applyAlpha(textColor, this.settingAlphas[settingIndex])));
                }

                render2DEvent.getContext().endScissor();
                render2DEvent.getContext().popPose();
            } else {
                render2DEvent.getContext().popPose();
            }
        }
    }

    private int applyAlpha(int color, float alpha) {
        long roundedAlpha;

        int rgb = color & 16777215;
        roundedAlpha = Math.round(alpha);

        int alphaByte = Math.clamp(roundedAlpha, 0, 255);
        return rgb | alphaByte << 24;
    }

    private void collectSettings(List<Setting<?>> settings, List<Setting<?>> result) {

        for (Setting setting2 : settings) {
            if (setting2 instanceof SettingGroup settingGroup) {
                this.collectSettings(settingGroup.getChildren(), result);
            } else {
                result.add(setting2);
                if (setting2.getChildren() != null) {
                    this.collectSettings(setting2.getChildren(), result);
                }
            }
        }
    }

    private static int typeSwitch0(Setting setting2, int startIndex) {
        Objects.checkIndex(startIndex, 4);
        if (setting2 == null) {
            return -1;
        } else if (startIndex <= 0 && setting2 instanceof BooleanSetting) {
            return 0;
        } else if (startIndex <= 1 && setting2 instanceof NumberSetting) {
            return 1;
        } else {
            return startIndex <= 2 && setting2 instanceof ModeSetting ? 2 : 3;
        }
    }

    private static int typeSwitch1(Setting setting2, int startIndex) {
        Objects.checkIndex(startIndex, 4);
        if (setting2 == null) {
            return -1;
        } else if (startIndex <= 0 && setting2 instanceof BooleanSetting) {
            return 0;
        } else if (startIndex <= 1 && setting2 instanceof NumberSetting) {
            return 1;
        } else {
            return startIndex <= 2 && setting2 instanceof ModeSetting ? 2 : 3;
        }
    }

    private static final String MODULE_ID = "45881065cf060da98404cf1a969ce0be";
    private static final String SCALE_SETTING_ID = "137a749a1d2ba133748fc087474416fc";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
