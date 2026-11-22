package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ChatReceiveEvent;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.ui.animation.Animation;
import astralis.ui.hud.Draggable;
import astralis.util.BreakUtil;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.player.LocalPlayer;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class SessionStats extends Module {
    private final ModeSetting mode;
    private final ColorSetting colors;
    private final Draggable hudElement;
    private long sessionStartMillis;
    private int kills;
    private int deaths;
    private int wins;
    private final Animation timerAnimation;
    private String previousTimerText;
    private String currentTimerText;
    private String lastProfileName;
    private static final float PULSIVE_WIDTH = 170.0F;
    private static final float PULSIVE_HEIGHT = 78.0F;
    private static final float PULSIVE_HEADER_HEIGHT = 18.0F;
    private static final float PULSIVE_TEXT_PADDING = 12.0F;
    private static final float PULSIVE_CORNER_RADIUS = 8.0F;
    private static final float ADJUST_WIDTH = 170.0F;
    private static final float ADJUST_HEIGHT = 56.0F;
    private static final float ADJUST_HEADER_HEIGHT = 14.0F;
    private static final float ADJUST_AVATAR_SIZE = 28.0F;
    private static final float ADJUST_AVATAR_MARGIN = 5.0F;
    private static final float ADJUST_CORNER_RADIUS = 0.0F;
    private static final float ADJUST_AVATAR_RADIUS = 2.0F;
    private static DecimalFormat kdrFormat;
    private float adjustTitleWidth;
    private float pulsiveTitleWidth;
    private double lastKdr;
    private String kdrText;
    private float kdrTextWidth;
    private List<String> previousTimerTokens;
    private List<String> currentTimerTokens;
    private float[] previousTimerOffsets;
    private float[] currentTimerOffsets;
    private String cachedPreviousTimerText;
    private String cachedCurrentTimerText;

    public SessionStats() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modes = new String[2];

        modes[0] = "Pulsive";

        modes[1] = "Adjust";
        ModeSetting modeSetting2 = modeSetting.addModes(modes);
        this.mode = modeSetting2.setDefault("Adjust");

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);
        this.sessionStartMillis = System.currentTimeMillis();
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.timerAnimation = new Animation(Easing.EASE_IN_OUT_QUART, 380L);
        this.previousTimerText = "";
        this.currentTimerText = "";
        this.lastProfileName = null;
        this.adjustTitleWidth = -1.0F;
        this.pulsiveTitleWidth = -1.0F;
        this.lastKdr = Double.NaN;
        this.kdrText = "";
        this.cachedPreviousTimerText = "";
        this.cachedCurrentTimerText = "";

        this.hudElement = new Draggable("Session Stats", 170.0F, 78.0F, 10.0F, 40.0F);
        this.hudElement.setVisibleWhen(
                () -> {
                    return this.isEnabled() && !mc.gui.hud.isHidden();
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudElement);
        ModeSetting modeSetting3 = this.colors.getColorMode();

        modeSetting3.setDefault("Client");
        this.addSettings(this.mode, this.colors);
    }

    public void resetSession() {
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.sessionStartMillis = System.currentTimeMillis();
    }

    private boolean matchesNameOrAlias(
            String text, String playerName, String alias, boolean isKiller) {
        return this.matchesName(text, playerName, isKiller)
                ? true
                : alias != null && this.matchesName(text, alias, isKiller);
    }

    private String stripRankPrefix(String text) {
        int index = 0;
        if (index < text.length()) {
            char charForSpace = text.charAt(index);

            if (charForSpace != ' ') {
                char charForOpen = text.charAt(index);

                if (charForOpen != '[') {
                    char charForClose = text.charAt(index);

                    if (charForClose != ']') {
                        char charForPlus = text.charAt(index);

                        if (charForPlus != '+' && !Character.isLetterOrDigit(text.charAt(index))) {
                            return text;
                        }
                    }
                }
            }

            char prefixChar = text.charAt(index);

            if (prefixChar == '[') {

                int closeIndex = text.indexOf(93, index);
                if (closeIndex >= 0) {
                    for (index = closeIndex + 1; index < text.length(); index++) {
                        char trailingChar = text.charAt(index);

                        if (trailingChar != ' ') {
                            break;
                        }
                    }

                    return text.substring(index);
                }
            }
        }

        return text;
    }

    private boolean matchesWordAt(String text, String word, int start) {
        return text.length() < start + word.length()
                ? false
                : text.regionMatches(start, word, 0, word.length())
                        && (text.length() == start + word.length()
                                || !Character.isLetterOrDigit(text.charAt(start + word.length())));
    }

    private float[] computeTokenOffsets(List<String> tokens, SizedFont font) {
        float[] offsets = new float[tokens.size()];
        float currentOffset = 0.0F;

        for (int i = 0; i < tokens.size(); i++) {
            offsets[i] = currentOffset;
            currentOffset += font.getStringWidth((String) tokens.get(i));
        }

        return offsets;
    }

    private void renderAdjustStyle(Render2DEvent render2DEvent) {

        SizedFont titleFont = FontManager.getThemedFont("Tahoma", 9.0F);

        SizedFont nameFont = FontManager.getThemedFont("Tahoma Bold", 11.0F);

        SizedFont infoFont = FontManager.getThemedFont("Tahoma", 9.0F);
        float hudX = this.hudElement.getX();
        float hudY = this.hudElement.getY();
        float hudScale = this.hudElement.getScale();
        int backgroundAlpha = Theme.getBackgroundAlpha();

        Color color = new Color(15, 15, 18, backgroundAlpha);

        Color color2 = new Color(28, 28, 34, backgroundAlpha);

        Color color3 = new Color(220, 220, 225, 255);

        Color color4 = new Color(210, 210, 218, 255);

        Color color5 = new Color(180, 180, 185, 255);

        Color color6 = new Color(0, 0, 0, Math.min(90, backgroundAlpha));

        Color color7 = new Color(0, 0, 0, 90);
        if (Effects.isSessionStatsBlur()) {
            render2DEvent
                    .getRenderEffects()
                    .blurRoundedRectangle(
                            hudX, hudY, 170.0F * hudScale, 56.0F * hudScale, 0.0F * hudScale);
        }

        render2DEvent.getContext().pushPose();
        render2DEvent.getContext().translate(hudX, hudY);
        render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, 170.0F, 56.0F)
                                .setRadius(0.0F)
                                .setShadowEnabled(Effects.isSessionStatsShadow())
                                .setColor(color));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, 170.0F, 14.0F)
                                .setRadius(0.0F)
                                .setColor(color2));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, 0.0F, 14.0F)
                                .setColor(color2));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(170.0F, 0.0F, 0.0F, 14.0F)
                                .setColor(color2));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 14.0F, 170.0F, 1.0F)
                                .setColor(color6));

        if (this.adjustTitleWidth < 0.0F) {

            this.adjustTitleWidth = titleFont.getStringWidth("Session Information");
        }

        float titleWidth = this.adjustTitleWidth;
        float titleX = (170.0F - titleWidth) / 2.0F;
        float titleY = (14.0F - titleFont.getLineHeight()) / 2.0F + 0.5F;
        Render2DContext titleContext = render2DEvent.getContext();
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        titleContext.drawText(
                titleRequest
                        .setText("Session Information")
                        .setPosition(titleX, titleY)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(color4.getRGB()));
        if (mc.player != null) {
            LocalPlayer localPlayer = mc.player;
            GpuTexture gpuTexture =
                    mc.getTextureManager()
                            .getTexture(localPlayer.getSkin().body().texturePath())
                            .getTexture();
            render2DEvent
                    .getContext()
                    .drawTexture(
                            TexturedRectShape.obtain()
                                    .setBounds(5.0F, 21.0F, 28.0F, 28.0F)
                                    .setTexture(gpuTexture)
                                    .useNearestFilter()
                                    .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                                    .setRadius(0.0F)
                                    .setColor(color3));
            render2DEvent
                    .getContext()
                    .drawTexture(
                            TexturedRectShape.obtain()
                                    .setBounds(5.0F, 21.0F, 28.0F, 28.0F)
                                    .setTexture(gpuTexture)
                                    .useNearestFilter()
                                    .setUv(0.625F, 0.125F, 0.75F, 0.25F)
                                    .setRadius(2.0F)
                                    .setColor(color3));
        } else {
            Render2DContext placeholderContext = render2DEvent.getContext();
            RoundedRectShape placeholderShape =
                    RoundedRectShape.obtain().setBounds(5.0F, 21.0F, 28.0F, 28.0F).setRadius(2.0F);

            placeholderContext.drawRect(placeholderShape.setColor(new Color(60, 60, 70)));
        }

        String playerName = BreakUtil.getLocalPlayerName();
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(playerName)
                                .setPosition(40.5F, 22.5F)
                                .setFont(nameFont.getMsdfFont())
                                .setFontSize(nameFont.getSize())
                                .setColor(color7.getRGB()));
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(playerName)
                                .setPosition(39.0F, 21.0F)
                                .setFont(nameFont.getMsdfFont())
                                .setFontSize(nameFont.getSize())
                                .setColor(color3.getRGB()));
        String elapsedText =
                this.formatElapsedTimeCompact(System.currentTimeMillis() - this.sessionStartMillis);
        double kdr;
        if (this.deaths == 0 && this.kills > 0) {
            kdr = this.kills;
        } else {
            kdr = (double) this.kills / Math.max(1, this.deaths);
        }

        if (kdr != this.lastKdr) {
            String formattedKdr = kdrFormat.format(kdr);

            this.kdrText = formattedKdr + " KDR";
            this.kdrTextWidth = infoFont.getStringWidth(this.kdrText);
            this.lastKdr = kdr;
        }

        float infoY = 49.0F - infoFont.getLineHeight() + 4.0F;
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(elapsedText)
                                .setPosition(39.0F, infoY)
                                .setFont(infoFont.getMsdfFont())
                                .setFontSize(infoFont.getSize())
                                .enableShadow()
                                .setColor(color5.getRGB()));
        float kdrWidth = this.kdrTextWidth;
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(this.kdrText)
                                .setPosition(165.0F - kdrWidth, infoY)
                                .setFont(infoFont.getMsdfFont())
                                .setFontSize(infoFont.getSize())
                                .enableShadow()
                                .setColor(color5.getRGB()));
        float barY = 21.0F + nameFont.getLineHeight() + 3.0F;
        float kdrFraction = (float) Math.max(0.0, Math.min(1.0, kdr / 5.0));
        float barWidth = 126.0F * kdrFraction;
        Render2DContext barContext = render2DEvent.getContext();
        RoundedRectShape barShape = RoundedRectShape.obtain().setBounds(39.0F, barY, 126.0F, 2.0F);

        barContext.drawRect(barShape.setColor(new Color(255, 255, 255, 40)));
        if (barWidth > 0.0F) {
            Color[] themeColors = this.colors.getColors();
            int gradientStart =
                    ColorUtil.waveBlend(
                            hudX + 39.0F, themeColors[0].getRGB(), themeColors[1].getRGB());
            int gradientEnd =
                    ColorUtil.waveBlend(
                            hudX + 39.0F + barWidth,
                            themeColors[0].getRGB(),
                            themeColors[1].getRGB());
            render2DEvent
                    .getContext()
                    .drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(39.0F, barY, barWidth, 2.0F)
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setHorizontalGradient(
                                                            gradientStart, gradientEnd)));
        }

        render2DEvent.getContext().popPose();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (mc.player != null && mc.level != null) {
                String profileName = BreakUtil.getLocalPlayerName();
                if (this.lastProfileName != null && !this.lastProfileName.equals(profileName)) {
                    this.resetSession();
                }

                this.lastProfileName = profileName;
            }
        }
    }

    private List<String> tokenizeTimerText(String text) {
        java.util.ArrayList tokens = new java.util.ArrayList();
        int index = 0;

        while (index < text.length()) {
            char currentChar = text.charAt(index);
            if (!Character.isDigit(currentChar)) {
                tokens.add(String.valueOf(currentChar));
                index++;
            } else {
                int digitEnd = index;

                while (digitEnd < text.length() && Character.isDigit(text.charAt(digitEnd))) {
                    digitEnd++;
                }

                tokens.add(text.substring(index, digitEnd));
                index = digitEnd;
            }
        }

        return tokens;
    }

    private void drawFadedText(
            Render2DEvent render2DEvent,
            String text,
            SizedFont font,
            float x,
            float y,
            float offsetY,
            float alpha,
            int color) {

        int alphaByte = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));

        int rgb = color & 16777215;

        int argb = rgb | alphaByte << 24;
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(text)
                                .setPosition(x, y + offsetY)
                                .setFont(font.getMsdfFont())
                                .setFontSize(font.getSize())
                                .enableShadow()
                                .setColor(argb));
    }

    private String getNameHiderAlias() {
        NameHider nameHider = Astralis.getInstance().getModuleManager().getModule(NameHider.class);
        if (nameHider != null && nameHider.isEnabled()) {
            String replacementName = nameHider.getReplacementName().getValue();
            return replacementName != null && !replacementName.isEmpty() ? replacementName : null;
        } else {
            return null;
        }
    }

    @EventTarget
    public void onChatReceive(ChatReceiveEvent event) {
        if (mc.player != null) {
            String message = event.getMessage().getString();
            String playerName = mc.player.getScoreboardName();
            String alias = this.getNameHiderAlias();

            if (!message.contains("You won!")) {

                if (!message.contains("VICTORY!")) {

                    if (!message.contains("1st place")) {

                        if (!message.contains("WINNER!")) {

                            if (!message.contains("Win")) {

                                if (!message.contains("'s bed was destroyed")) {

                                    if (!message.contains("BED DESTRUCTION")) {

                                        int byIndex = message.indexOf(" by ");
                                        if (byIndex >= 0) {
                                            String killerText = message.substring(byIndex + 4);
                                            String victimText = message.substring(0, byIndex);
                                            boolean killerMatchesYou =
                                                    this.matchesNameOrAlias(
                                                            killerText, playerName, alias, true);
                                            boolean victimMatchesYou =
                                                    this.matchesNameOrAlias(
                                                            victimText, playerName, alias, false);
                                            if (killerMatchesYou && !victimMatchesYou) {
                                                this.kills++;
                                            } else if (victimMatchesYou && !killerMatchesYou) {
                                                this.deaths++;
                                            }

                                            return;
                                        }

                                        String[] killVerbs = new String[3];

                                        killVerbs[0] = " killed ";

                                        killVerbs[1] = " eliminated ";

                                        killVerbs[2] = " slain ";

                                        for (String verb : killVerbs) {
                                            int verbIndex = message.indexOf(verb);
                                            if (verbIndex >= 0) {
                                                String killerPart = message.substring(0, verbIndex);
                                                String victimPart =
                                                        message.substring(
                                                                verbIndex + verb.length());
                                                boolean killerIsYou =
                                                        this.matchesNameOrAlias(
                                                                killerPart,
                                                                playerName,
                                                                alias,
                                                                true);
                                                boolean victimIsYou =
                                                        this.matchesNameOrAlias(
                                                                victimPart,
                                                                playerName,
                                                                alias,
                                                                false);
                                                if (killerIsYou && !victimIsYou) {
                                                    this.kills++;
                                                } else if (victimIsYou && !killerIsYou) {
                                                    this.deaths++;
                                                }

                                                return;
                                            }
                                        }

                                        if (!message.contains("You died")) {

                                            if (!message.contains("You were killed")) {

                                                if (!message.contains("You fell")) {
                                                    return;
                                                }
                                            }
                                        }

                                        this.deaths++;
                                        return;
                                    }
                                }

                                return;
                            }
                        }
                    }
                }
            }

            this.wins++;
        }
    }

    private void drawRollingTimer(Render2DEvent render2DEvent, SizedFont font, float y, int color) {
        float progress = (float) this.timerAnimation.getValue();
        boolean animating =
                !this.timerAnimation.isFinished()
                        && !this.previousTimerText.equals(this.currentTimerText);
        if (!this.previousTimerText.equals(this.cachedPreviousTimerText)
                || !this.currentTimerText.equals(this.cachedCurrentTimerText)) {
            this.previousTimerTokens = this.tokenizeTimerText(this.previousTimerText);
            this.currentTimerTokens = this.tokenizeTimerText(this.currentTimerText);
            this.previousTimerOffsets = this.computeTokenOffsets(this.previousTimerTokens, font);
            this.currentTimerOffsets = this.computeTokenOffsets(this.currentTimerTokens, font);
            this.cachedPreviousTimerText = this.previousTimerText;
            this.cachedCurrentTimerText = this.currentTimerText;
        }

        List previousTokens = this.previousTimerTokens;
        List currentTokens = this.currentTimerTokens;
        float[] previousOffsets = this.previousTimerOffsets;
        float[] currentOffsets = this.currentTimerOffsets;
        int tokenCount = Math.max(previousTokens.size(), currentTokens.size());
        float lineHeight = font.getLineHeight();

        for (int i = 0; i < tokenCount; i++) {
            String previousToken = i < previousTokens.size() ? (String) previousTokens.get(i) : "";
            String currentToken = i < currentTokens.size() ? (String) currentTokens.get(i) : "";
            float previousOffset =
                    i < previousOffsets.length
                            ? previousOffsets[i]
                            : (previousOffsets.length > 0
                                    ? previousOffsets[previousOffsets.length - 1]
                                    : 0.0F);
            float currentOffset =
                    i < currentOffsets.length
                            ? currentOffsets[i]
                            : (currentOffsets.length > 0
                                    ? currentOffsets[currentOffsets.length - 1]
                                    : 0.0F);
            boolean changed = animating && !previousToken.equals(currentToken);
            float tokenX =
                    12.0F
                            + (changed
                                    ? previousOffset + (currentOffset - previousOffset) * progress
                                    : currentOffset);
            if (!changed) {
                if (!currentToken.isEmpty() && !currentToken.isBlank()) {
                    this.drawFadedText(
                            render2DEvent, currentToken, font, tokenX, y, 0.0F, 1.0F, color);
                }
            } else {
                float tokenWidth =
                        Math.max(
                                font.getStringWidth(previousToken),
                                font.getStringWidth(currentToken));
                render2DEvent.getContext().beginScissor(tokenX, y, tokenWidth, lineHeight);
                if (!previousToken.isBlank()) {
                    this.drawFadedText(
                            render2DEvent,
                            previousToken,
                            font,
                            tokenX,
                            y,
                            -lineHeight * progress,
                            1.0F - progress,
                            color);
                }

                if (!currentToken.isBlank()) {
                    this.drawFadedText(
                            render2DEvent,
                            currentToken,
                            font,
                            tokenX,
                            y,
                            lineHeight * (1.0F - progress),
                            progress,
                            color);
                }

                render2DEvent.getContext().endScissor();
            }
        }
    }

    private void renderPulsiveStyle(Render2DEvent render2DEvent) {

        SizedFont titleFont = FontManager.getThemedFont("Product Sans Medium", 8.0F);

        SizedFont timerFont = FontManager.getThemedFont("Product Sans Bold", 18.0F);

        SizedFont infoFont = FontManager.getThemedFont("Product Sans Medium", 9.0F);
        float hudX = this.hudElement.getX();
        float hudY = this.hudElement.getY();
        float hudScale = this.hudElement.getScale();
        int backgroundAlpha = Theme.getBackgroundAlpha();

        Color color = new Color(18, 18, 22, backgroundAlpha);

        Color color2 = new Color(38, 38, 46, backgroundAlpha);

        Color color3 = new Color(255, 255, 255, 255);

        Color color4 = new Color(150, 150, 160, 230);

        Color color5 = new Color(0, 0, 0, Math.min(90, backgroundAlpha));
        if (Effects.isSessionStatsBlur()) {
            render2DEvent
                    .getRenderEffects()
                    .blurRoundedRectangle(
                            hudX, hudY, 170.0F * hudScale, 78.0F * hudScale, 8.0F * hudScale);
        }

        render2DEvent.getContext().pushPose();
        render2DEvent.getContext().translate(hudX, hudY);
        render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, 170.0F, 78.0F)
                                .setRadius(8.0F)
                                .setShadowEnabled(Effects.isSessionStatsShadow())
                                .setColor(color));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 0.0F, 170.0F, 18.0F)
                                .setRadius(8.0F)
                                .setColor(color2));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 8.0F, 8.0F, 10.0F)
                                .setColor(color2));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(162.0F, 8.0F, 8.0F, 10.0F)
                                .setColor(color2));
        render2DEvent
                .getContext()
                .drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, 18.0F, 170.0F, 1.0F)
                                .setColor(color5));

        if (this.pulsiveTitleWidth < 0.0F) {

            this.pulsiveTitleWidth = titleFont.getStringWidth("session information");
        }

        float titleWidth = this.pulsiveTitleWidth;
        float titleX = (170.0F - titleWidth) / 2.0F;
        float titleY = (18.0F - titleFont.getLineHeight()) / 2.0F + 1.0F;

        this.drawGradientText(
                render2DEvent, "session information", titleX, titleY, titleWidth, titleFont);
        String elapsedText =
                this.formatElapsedTime(System.currentTimeMillis() - this.sessionStartMillis);
        if (this.currentTimerText.isEmpty()) {
            this.currentTimerText = elapsedText;
            this.previousTimerText = elapsedText;
        } else if (!elapsedText.equals(this.currentTimerText) && this.timerAnimation.isFinished()) {
            this.previousTimerText = this.currentTimerText;
            this.currentTimerText = elapsedText;
            this.timerAnimation.setValue(0.0);
            this.timerAnimation.setTarget(0.0);
        }

        this.timerAnimation.animateTo(1.0);
        this.drawRollingTimer(render2DEvent, timerFont, 24.0F, color3.getRGB());
        float killsY = 24.0F + timerFont.getLineHeight() + 2.0F;
        String killSuffixValue;
        if (this.kills == 1) {

            killSuffixValue = " kill";
        } else {

            killSuffixValue = " kills";
        }

        String killSuffix = killSuffixValue;
        int killCount = this.kills;

        String killsText = "You have gotten " + killCount + killSuffix;
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(killsText)
                                .setPosition(12.0F, killsY)
                                .setFont(infoFont.getMsdfFont())
                                .setFontSize(infoFont.getSize())
                                .enableShadow()
                                .setColor(color4.getRGB()));
        float winsY = killsY + infoFont.getLineHeight();
        String winSuffixValue;
        if (this.wins == 1) {

            winSuffixValue = " time";
        } else {

            winSuffixValue = " times";
        }

        String winSuffix = winSuffixValue;
        int winCount = this.wins;

        String winsText = "You won " + winCount + winSuffix;
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(winsText)
                                .setPosition(12.0F, winsY)
                                .setFont(infoFont.getMsdfFont())
                                .setFontSize(infoFont.getSize())
                                .enableShadow()
                                .setColor(color4.getRGB()));
        render2DEvent.getContext().popPose();
    }

    private String formatElapsedTime(long millis) {
        long remainingSeconds;

        long totalSeconds = millis / 1000L;

        long hours = totalSeconds / 3600L;

        remainingSeconds = totalSeconds % 3600L;

        long minutes = remainingSeconds / 60L;

        long seconds = totalSeconds % 60L;

        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    private void drawGradientText(
            Render2DEvent render2DEvent,
            String text,
            float x,
            float y,
            float width,
            SizedFont font) {
        Color[] gradientColors = this.colors.getColors();
        Color color = gradientColors[0];
        Color color2 = gradientColors[1];
        int startColor = ColorUtil.waveBlend(x, color.getRGB(), color2.getRGB());
        int endColor = ColorUtil.waveBlend(x + width, color.getRGB(), color2.getRGB());
        render2DEvent
                .getContext()
                .drawText(
                        TextDrawRequest.obtain()
                                .setText(text)
                                .setPosition(x, y)
                                .setFont(font.getMsdfFont())
                                .setFontSize(font.getSize())
                                .enableShadow()
                                .setPaint(
                                        ShapePaint.obtain()
                                                .setLinearGradient(
                                                        startColor, endColor, x, y, x + width, y)));
    }

    private boolean matchesName(String text, String name, boolean isKiller) {
        return isKiller
                ? this.matchesWordAt(text, name, 0) || this.stripRankPrefix(text).startsWith(name)
                : text.startsWith(name)
                        || text.contains(" " + name + " ")
                        || text.contains(" " + name + "'")
                        || this.stripRankPrefix(text).startsWith(name);
    }

    private String formatElapsedTimeCompact(long millis) {
        long remainingSeconds;

        long totalSeconds = millis / 1000L;

        long hours = totalSeconds / 3600L;

        remainingSeconds = totalSeconds % 3600L;

        long minutes = remainingSeconds / 60L;

        long seconds = totalSeconds % 60L;
        if (hours > 0L) {

            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else {

            return String.format("%dm %ds", minutes, seconds);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {

        if (this.mode.is("Adjust")) {
            this.hudElement.setWidth(170.0F);
            this.hudElement.setHeight(56.0F);
            this.renderAdjustStyle(render2DEvent);
        } else {
            this.hudElement.setWidth(170.0F);
            this.hudElement.setHeight(78.0F);
            this.renderPulsiveStyle(render2DEvent);
        }
    }

    static {
        kdrFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    }

    private static final String MODULE_ID = "0aed65edd50c08af00f1f7e05d0507c8";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
