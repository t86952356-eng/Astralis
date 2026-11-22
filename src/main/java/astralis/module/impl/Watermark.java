package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.ui.hud.Draggable;
import astralis.util.MoveUtil;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Supplier;

public class Watermark extends Module {
    private final ModeSetting watermarkMode;
    private final ModeSetting font;
    private final NumberSetting fontSize;
    private final BooleanSetting gradientName;
    private final BooleanSetting hideFps;
    private final BooleanSetting bold;
    private final BooleanSetting showFps;
    private final ColorSetting colors;
    private final Draggable hudElement;

    public Watermark() {

        super(MODULE_ID, "", "", Category.MISC);

        ModeSetting modeSetting = new ModeSetting(WATERMARK_MODE_SETTING_ID);
        String[] modeNames = new String[3];

        modeNames[0] = "Modern";

        modeNames[1] = "Simple";

        modeNames[2] = "Old Style";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.watermarkMode = modeSetting2.setDefault("Modern");

        ModeSetting modeSetting3 = new ModeSetting(FONT_SETTING_ID);
        String[] fontNames = new String[7];

        fontNames[0] = "Product Sans";

        fontNames[1] = "Inter";

        fontNames[2] = "SF Pro";

        fontNames[3] = "Tahoma";

        fontNames[4] = "Geist";

        fontNames[5] = "Roboto";

        fontNames[6] = "IBM Plex Sans";
        ModeSetting modeSetting4 = modeSetting3.addModes(fontNames);
        this.font = modeSetting4.setDefault("Product Sans");

        this.fontSize = new NumberSetting(FONT_SIZE_SETTING_ID, 11.0F, 6.0F, 24.0F, 1.0F);

        this.gradientName = new BooleanSetting(GRADIENT_NAME_SETTING_ID, true);

        this.hideFps = new BooleanSetting(HIDE_FPS_SETTING_ID, true);

        this.bold = new BooleanSetting(BOLD_SETTING_ID, true);

        this.showFps = new BooleanSetting(SHOW_FPS_SETTING_ID, false);

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);

        this.hudElement = new Draggable("Watermark", 100.0F, 22.0F, 5.0F, 5.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return this.isEnabled() && !InstanceAccess.mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        this.gradientName.hideIf(
                new Supplier[] {
                    () -> {
                        if (!this.watermarkMode.is("Simple")) {

                            if (!this.watermarkMode.is("Old Style")) {
                                return false;
                            }
                        }

                        return true;
                    }
                });
        this.bold.hideIf(
                new Supplier[] {
                    () -> {
                        if (!this.watermarkMode.is("Simple")) {

                            if (!this.watermarkMode.is("Old Style")) {
                                return true;
                            }
                        }

                        return this.font.is("Tahoma") ? false : true;
                    }
                });
        this.hideFps.hideIf(
                new Supplier[] {
                    () -> {
                        return !this.watermarkMode.is("Modern");
                    }
                });
        ModeSetting modeSetting5 = this.colors.getColorMode();

        modeSetting5.setDefault("Client");

        Setting[] allSettings =
                new Setting[] {
                    this.watermarkMode,
                    this.font,
                    this.fontSize,
                    this.gradientName,
                    this.hideFps,
                    this.bold,
                    null,
                    null
                };

        allSettings[6] = this.showFps;

        allSettings[7] = this.colors;
        this.addSettings(allSettings);
    }

    private void renderSimpleStyle(Render2DEvent render2DEvent) {

        String clientName = Astralis.displayName;
        if (clientName != null && !clientName.isEmpty()) {
            SizedFont titleFont =
                    FontManager.getFont(this.getFontName(true), (Float) this.fontSize.getValue());
            SizedFont versionFont =
                    FontManager.getFont(
                            this.getFontName(false), (Float) this.fontSize.getValue() - 2.0F);

            Color[] gradientColors = this.colors.getColors();
            int startColor = gradientColors[0].getRGB();
            int endColor = gradientColors[1].getRGB();
            String displayText =
                    Character.toUpperCase(clientName.charAt(0))
                            + clientName.substring(1).toLowerCase();

            float nameWidth = titleFont.getStringWidth(displayText);

            float versionWidth = versionFont.getStringWidth("v2.0");
            float totalWidth = 4.0F + nameWidth + 6.0F + versionWidth + 4.0F;
            this.hudElement.setWidth(totalWidth);
            this.hudElement.setHeight(19.0F);
            float hudX = this.hudElement.getX();
            float hudY = this.hudElement.getY();
            if (Effects.isWatermarkBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangle(hudX, hudY, totalWidth, 19.0F, 3.0F);
            }

            Render2DContext rectContext = render2DEvent.getContext();
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(hudX, hudY, totalWidth, 19.0F)
                            .setRadius(3.0F)
                            .setShadowEnabled(Effects.isWatermarkShadow());

            rectContext.drawRect(
                    backgroundShape.setColor(new Color(12, 12, 12, Theme.getBackgroundAlpha())));
            float textX = hudX + 4.0F;
            int nameStartColor = ColorUtil.waveBlend(textX, startColor, endColor);
            int nameEndColor = ColorUtil.waveBlend(textX + nameWidth, startColor, endColor);
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(displayText)
                                    .setPosition(textX, hudY + 4.0F)
                                    .centerVertically(11.0F)
                                    .setFont(titleFont.getMsdfFont())
                                    .setFontSize(titleFont.getSize())
                                    .enableShadow()
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setLinearGradient(
                                                            nameStartColor,
                                                            nameEndColor,
                                                            textX,
                                                            hudY + 4.0F,
                                                            textX + nameWidth,
                                                            hudY + 4.0F)));
            Render2DContext versionContext = render2DEvent.getContext();
            TextDrawRequest versionBuilder = TextDrawRequest.obtain();

            TextDrawRequest versionRequest =
                    versionBuilder
                            .setText("v2.0")
                            .setPosition(hudX + 4.0F + nameWidth + 6.0F, hudY + 4.0F)
                            .centerVertically(11.0F)
                            .setFont(versionFont.getMsdfFont())
                            .setFontSize(versionFont.getSize())
                            .enableShadow();

            versionContext.drawText(versionRequest.setColor(-1834440793));
        }
    }

    @Override
    public String getSuffix() {
        return ((Mode) this.watermarkMode.getValue()).getName();
    }

    private String getFontName(boolean boldVariant) {

        if (this.font.is("Inter")) {

            return "Inter Medium";
        }

        if (this.font.is("SF Pro")) {

            return "SF Pro Regular";
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

        if (!this.font.is("Tahoma")) {
            String fontName;
            if (boldVariant) {

                fontName = "Product Sans Bold";
            } else {

                fontName = "Product Sans Regular";
            }

            return fontName;
        } else {
            label50:
            if (boldVariant) {

                if (!this.watermarkMode.is("Simple")) {

                    if (!this.watermarkMode.is("Old Style")) {
                        break label50;
                    }
                }

                if ((Boolean) this.bold.getValue()) {

                    return "Tahoma Bold";
                }
            }

            return "Tahoma";
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        switch (((Mode) this.watermarkMode.getValue()).getName()) {
            case "Simple":
                this.renderSimpleStyle(render2DEvent);
                break;
            case "Old Style":
                this.renderOldStyle(render2DEvent);
                break;
            default:
                this.renderModernStyle(render2DEvent);
        }

        this.renderBottomInfo(render2DEvent, this.colors.getColors());
    }

    private void renderBottomInfo(Render2DEvent render2DEvent, Color[] gradientColors) {

        {
            SizedFont mainFont = FontManager.getFont(this.getFontName(false), 8.0F);
            SizedFont valueFont = FontManager.getFont(this.getFontName(false), 7.0F);
            SizedFont dateFont = FontManager.getFont(this.getFontName(false), 6.0F);
            SizedFont unitFont = FontManager.getFont(this.getFontName(false), 5.0F);
            float screenHeight = InstanceAccess.mc.getWindow().getGuiScaledHeight();
            String profileName = astralis.OfflineServices.profileName();
            String monthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.US);
            String dayOrdinal = this.formatOrdinalDay(LocalDate.now().getDayOfMonth());
            String dateText = " " + monthName + " " + dayOrdinal;
            String rawSpeedText;
            if ((Boolean) this.showFps.getValue()) {
                rawSpeedText = String.valueOf(InstanceAccess.mc.getFps());
            } else {

                rawSpeedText = String.format(Locale.US, "%.2f", MoveUtil.getBps());
            }

            String speedText = rawSpeedText;
            String rawUnitText;
            if ((Boolean) this.showFps.getValue()) {

                rawUnitText = "fps";
            } else {

                rawUnitText = "B/s";
            }

            String unitText = rawUnitText;
            float mainLineHeight = mainFont.getLineHeight();
            float valueLineHeight = valueFont.getLineHeight();
            float nameY = screenHeight - mainLineHeight - 1.0F;
            float speedY = screenHeight - mainLineHeight - valueLineHeight - 0.5F;
            float speedWidth = valueFont.getStringWidth(speedText);
            Render2DContext speedContext = render2DEvent.getContext();
            TextDrawRequest speedRequest =
                    TextDrawRequest.obtain()
                            .setText(speedText)
                            .setPosition(3.0F, speedY)
                            .setFont(valueFont.getMsdfFont())
                            .setFontSize(valueFont.getSize())
                            .enableShadow();

            speedContext.drawText(speedRequest.setColor(new Color(230, 230, 230).getRGB()));
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(unitText)
                                    .setPosition(3.0F + speedWidth + 1.5F, speedY + 0.5F)
                                    .setFont(unitFont.getMsdfFont())
                                    .setFontSize(unitFont.getSize())
                                    .enableShadow()
                                    .setColor(Color.LIGHT_GRAY.getRGB()));
            float nameWidth = mainFont.getStringWidth(profileName);
            int nameStartColor =
                    ColorUtil.waveBlend(
                            3.0F, gradientColors[0].getRGB(), gradientColors[1].getRGB());
            int nameEndColor =
                    ColorUtil.waveBlend(
                            3.0F + nameWidth,
                            gradientColors[0].getRGB(),
                            gradientColors[1].getRGB());
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(profileName)
                                    .setPosition(3.0F, nameY)
                                    .setFont(mainFont.getMsdfFont())
                                    .setFontSize(mainFont.getSize())
                                    .enableShadow()
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setLinearGradient(
                                                            nameStartColor,
                                                            nameEndColor,
                                                            3.0F,
                                                            nameY,
                                                            3.0F + nameWidth,
                                                            nameY)));
            Render2DContext dateContext = render2DEvent.getContext();
            TextDrawRequest dateRequest =
                    TextDrawRequest.obtain()
                            .setText(dateText)
                            .setPosition(
                                    3.0F + nameWidth,
                                    nameY + (mainFont.getLineHeight() - dateFont.getLineHeight()))
                            .setFont(dateFont.getMsdfFont())
                            .setFontSize(dateFont.getSize())
                            .enableShadow();

            dateContext.drawText(dateRequest.setColor(new Color(230, 230, 230).getRGB()));
        }
    }

    private void renderModernStyle(Render2DEvent render2DEvent) {
        SizedFont nameFont =
                FontManager.getFont(this.getFontName(true), (Float) this.fontSize.getValue());
        SizedFont infoFont =
                FontManager.getFont(this.getFontName(false), (Float) this.fontSize.getValue());
        String clientName = Astralis.displayName;
        String versionText = String.valueOf(2.0);
        int fps = InstanceAccess.mc.getFps();

        String fpsText = fps + " FPS";

        float nameWidth = nameFont.getStringWidth(clientName);
        float versionWidth = infoFont.getStringWidth(versionText);
        float separatorWidth = infoFont.getStringWidth(" | ");
        float fpsWidth = infoFont.getStringWidth(fpsText);
        float totalWidth =
                nameWidth
                        + separatorWidth
                        + versionWidth
                        + (this.hideFps.getValue() ? separatorWidth + fpsWidth : 0.0F)
                        + 16.0F;
        float totalHeight = Math.max(22.0F, infoFont.getLineHeight() + 10.0F);
        this.hudElement.setWidth(totalWidth);
        this.hudElement.setHeight(totalHeight);
        float hudX = this.hudElement.getX();
        float hudY = this.hudElement.getY();
        float textHeight = Math.max(nameFont.getLineHeight(), infoFont.getLineHeight());
        float textY = hudY + (totalHeight - textHeight) / 2.0F;
        if (Effects.isWatermarkBlur()) {
            render2DEvent
                    .getRenderEffects()
                    .blurRoundedRectangle(hudX, hudY, totalWidth, totalHeight, 6.0F);
        }

        Render2DContext backgroundContext = render2DEvent.getContext();
        RoundedRectShape backgroundShape =
                RoundedRectShape.obtain()
                        .setBounds(hudX, hudY, totalWidth, totalHeight)
                        .setRadius(6.0F)
                        .setShadowEnabled(Effects.isWatermarkShadow());

        backgroundContext.drawRect(
                backgroundShape.setColor(new Color(5, 5, 10, Theme.getBackgroundAlpha())));
        float textX = hudX + 8.0F;
        Color[] gradientColors = this.colors.getColors();
        if ((Boolean) this.gradientName.getValue()) {
            int startColor =
                    ColorUtil.waveBlend(
                            textX, gradientColors[0].getRGB(), gradientColors[1].getRGB());
            int endColor =
                    ColorUtil.waveBlend(
                            textX + nameWidth,
                            gradientColors[0].getRGB(),
                            gradientColors[1].getRGB());
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(clientName)
                                    .setPosition(textX, textY)
                                    .setFont(nameFont.getMsdfFont())
                                    .setFontSize(nameFont.getSize())
                                    .enableShadow()
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setLinearGradient(
                                                            startColor,
                                                            endColor,
                                                            textX,
                                                            textY,
                                                            textX + nameWidth,
                                                            textY)));
        } else {
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(clientName)
                                    .setPosition(textX, textY)
                                    .setFont(nameFont.getMsdfFont())
                                    .setFontSize(nameFont.getSize())
                                    .enableShadow()
                                    .setColor(gradientColors[0].getRGB()));
        }

        textX += nameWidth;
        Render2DContext separatorContext = render2DEvent.getContext();
        TextDrawRequest separatorBuilder = TextDrawRequest.obtain();

        TextDrawRequest separatorRequest =
                separatorBuilder
                        .setText(" | ")
                        .setPosition(textX, textY)
                        .setFont(infoFont.getMsdfFont())
                        .setFontSize(infoFont.getSize())
                        .enableShadow();

        separatorContext.drawText(
                separatorRequest.setColor(new Color(120, 120, 130, 200).getRGB()));
        textX += separatorWidth;
        Render2DContext versionContext = render2DEvent.getContext();
        TextDrawRequest versionRequest =
                TextDrawRequest.obtain()
                        .setText(versionText)
                        .setPosition(textX, textY)
                        .setFont(infoFont.getMsdfFont())
                        .setFontSize(infoFont.getSize())
                        .enableShadow();

        versionContext.drawText(versionRequest.setColor(new Color(220, 220, 230, 255).getRGB()));
        if ((Boolean) this.hideFps.getValue()) {
            textX += versionWidth;
            Render2DContext secondSeparatorContext = render2DEvent.getContext();
            TextDrawRequest secondSeparatorBuilder = TextDrawRequest.obtain();

            TextDrawRequest secondSeparatorRequest =
                    secondSeparatorBuilder
                            .setText(" | ")
                            .setPosition(textX, textY)
                            .setFont(infoFont.getMsdfFont())
                            .setFontSize(infoFont.getSize())
                            .enableShadow();

            secondSeparatorContext.drawText(
                    secondSeparatorRequest.setColor(new Color(120, 120, 130, 200).getRGB()));
            textX += separatorWidth;
            Render2DContext fpsContext = render2DEvent.getContext();
            TextDrawRequest fpsRequest =
                    TextDrawRequest.obtain()
                            .setText(fpsText)
                            .setPosition(textX, textY)
                            .setFont(infoFont.getMsdfFont())
                            .setFontSize(infoFont.getSize())
                            .enableShadow();

            fpsContext.drawText(fpsRequest.setColor(new Color(220, 220, 225).getRGB()));
        }
    }

    private String formatOrdinalDay(int day) {
        if (day >= 11) {
            if (day <= 13) {
                return day + "th";
            }
        }

        String ordinal;
        switch (day % 10) {
            case 1:
                ordinal = day + "st";
                break;
            case 2:
                ordinal = day + "nd";
                break;
            case 3:
                ordinal = day + "rd";
                break;
            default:
                ordinal = day + "th";
        }

        return ordinal;
    }

    private void renderOldStyle(Render2DEvent render2DEvent) {
        String clientName = Astralis.displayName;
        if (clientName != null && !clientName.isEmpty()) {
            SizedFont initialFont =
                    FontManager.getFont(this.getFontName(true), (Float) this.fontSize.getValue());
            SizedFont restFont =
                    FontManager.getFont(this.getFontName(false), (Float) this.fontSize.getValue());
            String formattedName =
                    Character.toUpperCase(clientName.charAt(0))
                            + clientName.substring(1).toLowerCase();
            String firstLetter = formattedName.substring(0, 1);
            String remainingLetters = formattedName.length() > 1 ? formattedName.substring(1) : "";
            Color[] gradientColors = this.colors.getColors();
            float firstLetterWidth = initialFont.getStringWidth(firstLetter);
            float totalWidth =
                    firstLetterWidth
                            + (remainingLetters.isEmpty()
                                    ? 0.0F
                                    : restFont.getStringWidth(remainingLetters));
            this.hudElement.setWidth(totalWidth);
            this.hudElement.setHeight(initialFont.getLineHeight());
            float hudX = this.hudElement.getX();
            float hudY = this.hudElement.getY();
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(firstLetter)
                                    .setPosition(hudX, hudY)
                                    .setFont(initialFont.getMsdfFont())
                                    .setFontSize(initialFont.getSize())
                                    .enableShadow()
                                    .setColor(gradientColors[0].getRGB()));
            if (!remainingLetters.isEmpty()) {
                render2DEvent
                        .getContext()
                        .drawText(
                                TextDrawRequest.obtain()
                                        .setText(remainingLetters)
                                        .setPosition(hudX + firstLetterWidth, hudY)
                                        .setFont(restFont.getMsdfFont())
                                        .setFontSize(restFont.getSize())
                                        .enableShadow()
                                        .setColor(Color.WHITE.getRGB()));
            }
        }
    }

    private static final String MODULE_ID = "fc7e3595d72796cf4fbe90ad2d3dbb23";
    private static final String WATERMARK_MODE_SETTING_ID = "41c7e5f1e545db6ae912f0ffeec3382c";
    private static final String FONT_SETTING_ID = "08cf192fd775b99ce904000e4ee6f5f1";
    private static final String FONT_SIZE_SETTING_ID = "7ca88461b9cc7b8535227380d5d8df5d";
    private static final String GRADIENT_NAME_SETTING_ID = "cc77204cd8898dacda2120446ebf1056";
    private static final String HIDE_FPS_SETTING_ID = "31f320c39a5a8d21740d7ce7684f1c7c";
    private static final String BOLD_SETTING_ID = "25b158ccd771fe767c49dbb49a066de6";
    private static final String SHOW_FPS_SETTING_ID = "0df11ccc39047f920855ab0c0877c757";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
