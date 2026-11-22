package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.module.Module;
import astralis.module.impl.arraylist.ArrayListEntry;
import astralis.module.impl.arraylist.CategorySwitchMap;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.ui.animation.Animation;
import astralis.ui.hud.ArrayListRow;
import astralis.ui.hud.Draggable;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.ChatFormatting;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ArrayList extends Module {
    private final ModeSetting displayMode;
    private final BooleanSetting lowerCase;
    private final BooleanSetting noSpaces;
    private final BooleanSetting background;
    private final NumberSetting radius;
    private final BooleanSetting dynamicRadius;
    private final NumberSetting spacing;
    private final BooleanSetting sideLine;
    private final ModeSetting sideLineMode;
    private final BooleanSetting semiBold;
    private final ModeSetting font;
    private final NumberSetting fontSize;
    private final BooleanSetting visualsCategory;
    private final BooleanSetting combatCategory;
    private final BooleanSetting playerCategory;
    private final BooleanSetting miscCategory;
    private final BooleanSetting worldCategory;
    private final BooleanSetting ghostCategory;
    private final BooleanSetting movementCategory;
    private final SettingGroup categoriesGroup;
    private final ColorSetting colors;
    private final Draggable hudElement;
    private final List<ArrayListRow> renderEntries;
    private final float[] cornerRadii;
    private final List<ArrayListEntry> sortedModuleLabels;
    private final Map<Module, ArrayListEntry> moduleLabelCache;
    private final Map<Module, String> cachedSuffixes;
    private String fontCacheKey;

    public ArrayList() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] displayModes = new String[2];

        displayModes[0] = "Default";

        displayModes[1] = "Simple";
        ModeSetting modeSetting2 = modeSetting.addModes(displayModes);
        this.displayMode = modeSetting2.setDefault("Default");

        this.lowerCase = new BooleanSetting(LOWER_CASE_SETTING_ID, false);

        this.noSpaces = new BooleanSetting(NO_SPACES_SETTING_ID, false);

        this.background = new BooleanSetting(BACK_GROUND_SETTING_ID, false);

        this.radius = new NumberSetting(RADIUS_SETTING_ID, 3.0F, 0.0F, 8.0F, 0.5F);

        this.dynamicRadius = new BooleanSetting(DYNAMIC_RADIUS_SETTING_ID, true);

        this.spacing = new NumberSetting(SPACING_SETTING_ID, 0.0F, 0.0F, 10.0F, 0.5F);

        this.sideLine = new BooleanSetting(SIDE_LINE_SETTING_ID, false);

        ModeSetting modeSetting3 = new ModeSetting(SIDE_LINE_MODE_SETTING_ID);
        String[] sideLineModes = new String[2];

        sideLineModes[0] = "Default";

        sideLineModes[1] = "Full";
        ModeSetting modeSetting4 = modeSetting3.addModes(sideLineModes);
        this.sideLineMode = modeSetting4.setDefault("Default");

        this.semiBold = new BooleanSetting(SEMI_BOLD_SETTING_ID, true);

        ModeSetting modeSetting5 = new ModeSetting(FONT_SETTING_ID);
        String[] fonts = new String[6];

        fonts[0] = "Product Sans";

        fonts[1] = "Inter";

        fonts[2] = "SF Pro";

        fonts[3] = "Tahoma";

        fonts[4] = "Geist";

        fonts[5] = "IBM Plex Sans";
        ModeSetting modeSetting6 = modeSetting5.addModes(fonts);
        this.font = modeSetting6.setDefault("Product Sans");

        this.fontSize = new NumberSetting(FONT_SIZE_SETTING_ID, 10.0F, 6.0F, 18.0F, 1.0F);

        this.visualsCategory = new BooleanSetting(VISUALS_SETTING_ID, false);

        this.combatCategory = new BooleanSetting(COMBAT_SETTING_ID, false);

        this.playerCategory = new BooleanSetting(PLAYER_SETTING_ID, false);

        this.miscCategory = new BooleanSetting(MISC_SETTING_ID, false);

        this.worldCategory = new BooleanSetting(WORLD_SETTING_ID, false);

        this.ghostCategory = new BooleanSetting(GHOST_SETTING_ID, false);

        this.movementCategory = new BooleanSetting(MOVEMENT_SETTING_ID, false);

        SettingGroup settingGroup = new SettingGroup(CATEGORIES_GROUP_ID);
        Setting[] categorySettings =
                new Setting[] {
                    this.visualsCategory,
                    this.combatCategory,
                    this.playerCategory,
                    this.miscCategory,
                    this.movementCategory,
                    this.worldCategory,
                    null
                };
        categorySettings[6] = this.ghostCategory;
        this.categoriesGroup = settingGroup.addValues(categorySettings).collapsed();

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);
        this.renderEntries = new java.util.ArrayList<>();
        this.cornerRadii = new float[4];
        this.sortedModuleLabels = new java.util.ArrayList<>();
        this.moduleLabelCache = new HashMap<>();
        this.cachedSuffixes = new HashMap<>();
        this.fontCacheKey = "";

        this.hudElement =
                new Draggable("Array List", 0.0F, 0.0F, mc.getWindow().getGuiScaledWidth(), 4.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    if (this.isEnabled()) {

                        if (!this.displayMode.is("Simple") && !mc.gui.hud.isHidden()) {
                            return true;
                        }
                    }

                    return false;
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        this.background.hideIf(
                () -> {
                    return this.displayMode.is("Simple");
                });
        this.radius.hideIf(
                () -> {
                    return this.displayMode.is("Simple") || !this.background.getValue();
                });
        this.dynamicRadius.hideIf(
                () -> {
                    return this.displayMode.is("Simple") || !this.background.getValue();
                });
        this.spacing.hideIf(
                () -> {
                    return this.displayMode.is("Simple");
                });
        this.sideLine.hideIf(
                () -> {
                    return this.displayMode.is("Simple");
                });
        this.sideLineMode.hideIf(
                () -> {
                    return this.displayMode.is("Simple") || !this.sideLine.getValue();
                });
        this.semiBold.hideIf(
                () -> {
                    if (!this.displayMode.is("Simple")) {

                        if (this.font.is("Product Sans")) {
                            return false;
                        }
                    }

                    return true;
                });
        this.font.hideIf(
                () -> {
                    return this.displayMode.is("Simple");
                });
        ModeSetting modeSetting7 = this.colors.getColorMode();

        modeSetting7.setDefault("Client");

        Setting[] settings =
                new Setting[] {
                    this.displayMode,
                    this.lowerCase,
                    this.noSpaces,
                    this.background,
                    this.radius,
                    this.dynamicRadius,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        settings[6] = this.spacing;

        settings[7] = this.sideLine;

        settings[8] = this.sideLineMode;

        settings[9] = this.font;

        settings[10] = this.fontSize;

        settings[11] = this.semiBold;

        settings[12] = this.categoriesGroup;

        settings[13] = this.colors;
        this.addSettings(settings);
    }

    private void updateCornerRadii(int index, float minProgress) {
        ArrayListRow row = this.renderEntries.get(index);
        float maxRadius = Math.min(this.radius.getValue(), row.getWidth() * 0.5F);
        float previousX = Float.NaN;
        float nextX = Float.NaN;

        for (int i = index - 1; i >= 0; i--) {
            if (!(this.renderEntries.get(i).getAnimationProgress() < minProgress)) {
                previousX = this.renderEntries.get(i).getTargetX();
                break;
            }
        }

        for (int j = index + 1; j < this.renderEntries.size(); j++) {
            if (!(this.renderEntries.get(j).getAnimationProgress() < minProgress)) {
                nextX = this.renderEntries.get(j).getTargetX();
                break;
            }
        }

        boolean hasPrevious = !Float.isNaN(previousX);
        boolean hasNext = !Float.isNaN(nextX);
        boolean useDynamicRadius = this.dynamicRadius.getValue();
        this.cornerRadii[0] =
                hasPrevious
                        ? (useDynamicRadius
                                ? Math.clamp(previousX - row.getTargetX(), 0.0F, maxRadius)
                                : 0.0F)
                        : maxRadius;
        this.cornerRadii[1] = hasPrevious ? 0.0F : maxRadius;
        this.cornerRadii[2] = hasNext ? 0.0F : maxRadius;
        this.cornerRadii[3] =
                hasNext
                        ? (useDynamicRadius
                                ? Math.clamp(nextX - row.getTargetX(), 0.0F, maxRadius)
                                : 0.0F)
                        : maxRadius;
        float maxCornerSum =
                Math.max(
                        this.cornerRadii[0] + this.cornerRadii[3],
                        this.cornerRadii[1] + this.cornerRadii[2]);
        if (maxCornerSum > row.getHeight() && maxCornerSum > 0.0F) {
            float scale = row.getHeight() / maxCornerSum;
            this.cornerRadii[0] = this.cornerRadii[0] * scale;
            this.cornerRadii[1] = this.cornerRadii[1] * scale;
            this.cornerRadii[2] = this.cornerRadii[2] * scale;
            this.cornerRadii[3] = this.cornerRadii[3] * scale;
        }

        if (this.sideLine.getValue()) {
            this.cornerRadii[1] = 0.0F;
            this.cornerRadii[2] = 0.0F;
        }
    }

    private String getFontName() {

        if (this.font.is("Inter")) {

            return "Inter Medium";
        }

        if (this.font.is("SF Pro")) {

            return "SF Pro Regular";
        }

        if (this.font.is("Tahoma")) {

            return "Tahoma";
        }

        if (this.font.is("Geist")) {

            return "Geist";
        }

        if (this.font.is("Roboto")) {

            return "Roboto";
        }

        if (this.font.is("IBM Plex Sans")) {

            return "IBM Plex Sans";
        }

        String fontName;
        if (this.semiBold.getValue()) {

            fontName = "Product Sans Medium";
        } else {

            fontName = "Product Sans Regular";
        }

        return fontName;
    }

    private boolean isCategoryHidden(Category category) {

        return switch (CategorySwitchMap.categoryOrdinalMap[category.ordinal()]) {
            case 1 -> true;
            case 2 -> this.combatCategory.getValue();
            case 3 -> this.ghostCategory.getValue();
            case 4 -> this.movementCategory.getValue();
            case 5 -> this.playerCategory.getValue();
            case 6 -> this.worldCategory.getValue();
            case 7 -> this.visualsCategory.getValue();
            case 8 -> this.miscCategory.getValue();
            default -> throw new MatchException(null, null);
        };
    }

    private String getDisplayText(Module module2) {
        String name = module2.getName();
        String suffix = module2.getSuffix();
        if (this.noSpaces.getValue()) {
            name = name.replace(" ", "");
        }

        if (this.lowerCase.getValue()) {
            name = name.toLowerCase();
            if (suffix != null) {
                suffix = suffix.toLowerCase();
            }
        }

        return suffix != null && !suffix.isEmpty()
                ? name + " " + ChatFormatting.GRAY + suffix
                : name;
    }

    @EventTarget
    public void onSapphireRenderEvent(Render2DEvent render2DEvent) {

        this.renderEntries.clear();

        boolean simpleMode = this.displayMode.is("Simple");
        String selectedFontName;
        if (simpleMode) {

            selectedFontName = "Tahoma";
        } else {
            selectedFontName = this.getFontName();
        }

        String fontName = selectedFontName;
        String cacheKey =
                fontName
                        + this.fontSize.getValue()
                        + this.noSpaces.getValue()
                        + this.lowerCase.getValue();
        if (!cacheKey.equals(this.fontCacheKey)) {
            this.fontCacheKey = cacheKey;
            this.moduleLabelCache.clear();
        }

        SizedFont sizedFont = FontManager.getFont((String) fontName, this.fontSize.getValue());
        this.sortedModuleLabels.clear();

        for (Module module2 : Astralis.getInstance().getModuleManager().getVisibleModules()) {
            if (!module2.isHidden() && !this.isCategoryHidden(module2.getCategory())) {
                String suffix = module2.getSuffix();
                ArrayListEntry entry = this.moduleLabelCache.get(module2);
                if (entry == null || !Objects.equals(suffix, this.cachedSuffixes.get(module2))) {
                    String displayText = this.getDisplayText(module2);
                    entry =
                            new ArrayListEntry(
                                    module2,
                                    displayText,
                                    "",
                                    sizedFont.getStringWidth(displayText));
                    this.moduleLabelCache.put(module2, entry);
                    this.cachedSuffixes.put(module2, suffix);
                }

                this.sortedModuleLabels.add(entry);
            }
        }

        this.sortedModuleLabels.sort(Comparator.comparingDouble(labelEntry -> -labelEntry.width()));
        float rowHeight = sizedFont.getSize() + 4.0F;
        float insetX = simpleMode ? 0.0F : 1.0F;
        float listWidth =
                this.sortedModuleLabels.isEmpty()
                        ? this.hudElement.getWidth() + (simpleMode ? 0 : 4)
                        : (simpleMode
                                ? this.sortedModuleLabels.getFirst().getWidth() + 6.0F
                                : 5.0F + this.sortedModuleLabels.getFirst().getWidth());
        float hudWidth = listWidth - (simpleMode ? 0 : 4);
        float totalHeight = 0.0F;
        int rowIndex = 0;
        float previousWidth = 0.0F;

        for (ArrayListEntry entry : this.sortedModuleLabels) {
            Module module3 = entry.getModule();
            Animation animation =
                    Astralis.getInstance().getModuleAnimations().getAnimation(module3);
            boolean moduleEnabled = module3.isEnabled();
            animation.animateTo(moduleEnabled ? 1.0 : 0.0);
            animation.setDuration(500L);
            float animationProgress = (float) animation.getValue();
            if (!(animationProgress <= 0.0F)) {
                Color[] gradientColors = this.colors.getColors();

                Color color =
                        ColorUtil.pingPongBlend(
                                10, 3 + rowIndex * 20, gradientColors[0], gradientColors[1], false);
                float rowWidth = simpleMode ? entry.getWidth() + 6.0F : 5.0F + entry.getWidth();
                float animatedHeight =
                        (rowHeight - 1.0F + (simpleMode ? 0.0F : this.spacing.getValue()))
                                * animationProgress;
                float rowX;
                float rowDrawWidth;
                if (simpleMode) {
                    rowX = listWidth - rowWidth - rowWidth * (animationProgress - 1.0F);
                    rowDrawWidth = rowWidth;
                } else {
                    rowX = listWidth - rowWidth * animationProgress - insetX;
                    rowDrawWidth = hudWidth - rowX + 1.0F;
                }

                this.renderEntries.add(
                        new ArrayListRow(
                                module3,
                                entry.displayText(),
                                rowX,
                                totalHeight,
                                rowDrawWidth,
                                animatedHeight,
                                listWidth - rowWidth - insetX,
                                rowWidth,
                                animationProgress,
                                color.getRGB(),
                                rowIndex,
                                previousWidth));
                rowIndex++;
                totalHeight += animatedHeight;
                previousWidth = rowWidth;
            }
        }

        this.hudElement.setWidth(listWidth - (simpleMode ? 0 : 4));
        this.hudElement.setHeight(totalHeight);
        if (!this.renderEntries.isEmpty()) {
            float originX;
            float originY;
            float hudScale;
            boolean fullLine;
            label215:
            {
                originX =
                        simpleMode
                                ? mc.getWindow().getGuiScaledWidth() - listWidth
                                : this.hudElement.getX();
                originY = simpleMode ? 0.0F : this.hudElement.getY();
                hudScale = simpleMode ? 1.0F : this.hudElement.getScale();
                if (this.sideLine.getValue()) {

                    if (this.sideLineMode.is("Full")) {
                        fullLine = true;
                        break label215;
                    }
                }

                fullLine = false;
            }

            boolean fullSideLine = fullLine;
            if (!simpleMode && this.background.getValue() && Effects.isArrayListBlur()) {
                for (int i = 0; i < this.renderEntries.size(); i++) {
                    ArrayListRow row = this.renderEntries.get(i);
                    if (!(row.getAnimationProgress() < 0.1F)) {
                        this.updateCornerRadii(i, 0.1F);
                        render2DEvent
                                .getRenderEffects()
                                .blurRectangleWithRadii(
                                        originX + row.getX() * hudScale,
                                        originY + row.getY() * hudScale,
                                        row.getWidth() * hudScale,
                                        row.getHeight() * hudScale,
                                        this.cornerRadii[0] * hudScale,
                                        this.cornerRadii[1] * hudScale,
                                        this.cornerRadii[2] * hudScale,
                                        this.cornerRadii[3] * hudScale,
                                        true);
                    }
                }
            }

            render2DEvent.getContext().pushPose();
            render2DEvent.getContext().translate(originX, originY);
            render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);

            for (int i = 0; i < this.renderEntries.size(); i++) {
                ArrayListRow row = this.renderEntries.get(i);
                float rowProgress = row.getAnimationProgress();
                if (simpleMode) {
                    if (rowProgress >= 0.1F) {
                        Render2DContext context = render2DEvent.getContext();
                        RoundedRectShape shape =
                                RoundedRectShape.obtain()
                                        .setBounds(
                                                row.getX(),
                                                row.getY(),
                                                row.getWidth(),
                                                row.getHeight());

                        context.drawRect(
                                shape.setColor(new Color(25, 25, 25, Theme.getBackgroundAlpha())));
                        int rowColor = row.color();
                        render2DEvent
                                .getContext()
                                .drawRect(
                                        RoundedRectShape.obtain()
                                                .setBounds(
                                                        row.getX() - 0.5F,
                                                        row.getY(),
                                                        0.5F,
                                                        row.getHeight() + 0.5F)
                                                .setColor(new Color(rowColor, true)));
                        if (row.getPreviousWidth() != 0.0F) {
                            float trailWidth =
                                    row.getPreviousWidth() - row.getFullWidth() * rowProgress;
                            if (trailWidth > 0.0F) {
                                render2DEvent
                                        .getContext()
                                        .drawRect(
                                                RoundedRectShape.obtain()
                                                        .setBounds(
                                                                row.getX()
                                                                        - (row.getPreviousWidth()
                                                                                - row
                                                                                        .getFullWidth()),
                                                                row.getY(),
                                                                trailWidth,
                                                                0.5F)
                                                        .setColor(new Color(rowColor, true)));
                            }
                        }

                        if (i == this.renderEntries.size() - 1) {
                            render2DEvent
                                    .getContext()
                                    .drawRect(
                                            RoundedRectShape.obtain()
                                                    .setBounds(
                                                            row.getX(),
                                                            row.getY() + row.getHeight(),
                                                            row.getWidth(),
                                                            0.5F)
                                                    .setColor(new Color(rowColor, true)));
                        }
                    }
                } else {
                    if (this.background.getValue() && rowProgress >= 0.1F) {
                        this.updateCornerRadii(i, 0.1F);
                        Render2DContext context = render2DEvent.getContext();
                        RoundedRectShape shape =
                                RoundedRectShape.obtain()
                                        .setBounds(
                                                row.getX(),
                                                row.getY(),
                                                row.getWidth(),
                                                row.getHeight())
                                        .setCornerRadii(
                                                this.cornerRadii[0],
                                                this.cornerRadii[1],
                                                this.cornerRadii[2],
                                                this.cornerRadii[3])
                                        .setOuterAntialiasOnly(true)
                                        .setShadowEnabled(Effects.isArrayListShadow());

                        context.drawRect(
                                shape.setColor(new Color(5, 5, 10, Theme.getBackgroundAlpha())));
                    }

                    if (this.sideLine.getValue() && rowProgress >= 0.1F) {
                        render2DEvent
                                .getContext()
                                .drawRect(
                                        RoundedRectShape.obtain()
                                                .setBounds(
                                                        hudWidth + 1.0F,
                                                        row.getY()
                                                                + (fullSideLine
                                                                        ? 0.0F
                                                                        : 1.0F
                                                                                + this.spacing
                                                                                                .getValue()
                                                                                        * rowProgress
                                                                                        * 0.5F),
                                                        1.0F,
                                                        row.getHeight()
                                                                - (fullSideLine
                                                                        ? 0.0F
                                                                        : 3.0F
                                                                                + this.spacing
                                                                                                .getValue()
                                                                                        * rowProgress))
                                                .setColor(new Color(row.color(), true))
                                                .setRadius(fullSideLine ? 0.0F : 2.0F));
                    }
                }

                if (simpleMode) {
                    float textX = row.getX() + 3.0F;
                    float textY =
                            row.getY() + (row.getHeight() - sizedFont.getSize()) / 2.0F - 1.5F;
                    Render2DContext context = render2DEvent.getContext();
                    TextDrawRequest fontRequest =
                            TextDrawRequest.obtain().setFont(sizedFont.getMsdfFont());
                    String labelText = row.getLabel();

                    TextDrawRequest shadowRequest =
                            fontRequest
                                    .setText(labelText.replaceAll("§.", ""))
                                    .setPosition(textX + 0.5F, textY + 0.5F)
                                    .setFontSize(sizedFont.getSize());

                    context.drawText(shadowRequest.setColor(new Color(17, 17, 17, 200).getRGB()));
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setFont(sizedFont.getMsdfFont())
                                            .setText(row.getLabel())
                                            .setPosition(textX, textY)
                                            .setFontSize(sizedFont.getSize())
                                            .setColor(new Color(row.color(), true)));
                } else {
                    float textX = listWidth - (insetX + (row.getFullWidth() * rowProgress - 1.5F));
                    render2DEvent
                            .getContext()
                            .beginScissor(row.getX(), row.getY(), row.getWidth(), row.getHeight());
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setFont(sizedFont.getMsdfFont())
                                            .setText(row.getLabel())
                                            .setPosition(
                                                    textX,
                                                    row.getY()
                                                            + this.spacing.getValue()
                                                                    * rowProgress
                                                                    * 0.5F)
                                            .setFontSize(sizedFont.getSize())
                                            .enableShadow()
                                            .setColor(new Color(row.color(), true)));
                    render2DEvent.getContext().endScissor();
                }
            }

            render2DEvent.getContext().popPose();
        }
    }

    private static final String MODULE_ID = "cf5871b95dba8b4018414780dc9675aa";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String LOWER_CASE_SETTING_ID = "71bbaccd3fbe3bc5c72dcfa09119986b";
    private static final String NO_SPACES_SETTING_ID = "137f2e2f226533977e839e975d0ddbf3";
    private static final String BACK_GROUND_SETTING_ID = "075eded90238175e8be79c1032e3c979";
    private static final String RADIUS_SETTING_ID = "00107704ec92e058e1cce9b2a3ab2723";
    private static final String DYNAMIC_RADIUS_SETTING_ID = "dbf5d2910d826fea2b523f7ac8e89b70";
    private static final String SPACING_SETTING_ID = "c9aee03f224403badc7b4d0871e3a38b";
    private static final String SIDE_LINE_SETTING_ID = "122d9f08326a858444503a7959917335";
    private static final String SIDE_LINE_MODE_SETTING_ID = "99c563d0758511fd8b323af7fa46383e";
    private static final String SEMI_BOLD_SETTING_ID = "b83ba025cd9f358b9c1e1a2ab7602b45";
    private static final String FONT_SETTING_ID = "08cf192fd775b99ce904000e4ee6f5f1";
    private static final String FONT_SIZE_SETTING_ID = "7ca88461b9cc7b8535227380d5d8df5d";
    private static final String VISUALS_SETTING_ID = "087bb197a9ad627bd347a839d7f02549";
    private static final String COMBAT_SETTING_ID = "4896b42617c0ea98b7c290d4137b229e";
    private static final String PLAYER_SETTING_ID = "b38b0ed17ee84290ffacd1ce5b97b037";
    private static final String MISC_SETTING_ID = "a3b22bfc01af0faa7ec7da742132b467";
    private static final String WORLD_SETTING_ID = "ef8677762d553ff5b177204d810d8e6b";
    private static final String GHOST_SETTING_ID = "5aaff00d6f20e78676e59dce6f760334";
    private static final String MOVEMENT_SETTING_ID = "078a6b9d4debc827e38ed6decb26f0d3";
    private static final String CATEGORIES_GROUP_ID = "65e1d9ebb15b187ea3423dd216086fb4";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
