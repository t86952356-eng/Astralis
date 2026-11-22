package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.media.music.LetterAnimation;
import astralis.media.music.LyricLine;
import astralis.media.music.LyricLineSplit;
import astralis.media.music.LyricSegment;
import astralis.media.music.LyricWord;
import astralis.media.music.LyricsState;
import astralis.media.music.MusicTracker;
import astralis.media.music.SpringValue;
import astralis.media.music.TrackInfo;
import astralis.media.music.WordAnimation;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.ui.animation.Animation;
import astralis.ui.hud.Draggable;
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

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;

public class MusicViewer extends Module {
    private final BooleanSetting lyrics;
    private final NumberSetting lyricsOffset;
    private final NumberSetting wordLift;
    private final NumberSetting letterLift;
    private final ColorSetting colors;
    private final MusicTracker mediaSession;
    private final Animation scrollAnimation;
    private final Animation breakGapAnimation;
    private final List<WordAnimation> wordAnimations;
    private final Draggable hudWidget;
    private LyricLine animatedLine;
    private long lastFrameNanos;
    private int breakLineIndex;

    public MusicViewer() {

        super(MODULE_ID, "", "", Category.Misc);

        this.lyrics = new BooleanSetting(LYRICS_SETTING_ID, true);

        this.lyricsOffset =
                new NumberSetting(LYRICS_OFFSET_SETTING_ID, 250.0F, -1000.0F, 1000.0F, 25.0F);

        this.wordLift = new NumberSetting(WORD_LIFT_SETTING_ID, 6.0F, 0.0F, 20.0F, 0.5F);

        this.letterLift = new NumberSetting(LETTER_LIFT_SETTING_ID, 12.0F, 0.0F, 40.0F, 0.5F);

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);
        this.mediaSession = new MusicTracker();

        this.scrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 450L);

        this.breakGapAnimation = new Animation(Easing.EASE_OUT_QUINT, 450L);
        this.wordAnimations = new java.util.ArrayList<>();
        this.breakLineIndex = -1;

        this.hudWidget = new Draggable("Music Viewer", 190.0F, 101.0F, 10.0F, 200.0F);
        this.hudWidget.setVisibleWhen(
                () -> {
                    return this.isEnabled()
                            && !mc.gui.hud.isHidden()
                            && this.mediaSession.getCurrentTrack() != null;
                });
        Astralis.getInstance().getHudManager().addDraggable(this.hudWidget);
        this.lyricsOffset.hideIf(
                () -> {
                    return !this.lyrics.getValue();
                });
        this.wordLift.hideIf(
                () -> {
                    return !this.lyrics.getValue();
                });
        this.letterLift.hideIf(
                () -> {
                    return !this.lyrics.getValue();
                });
        ModeSetting modeSetting = this.colors.getColorMode();

        modeSetting.setDefault("Client");
        this.addSettings(
                this.lyrics, this.lyricsOffset, this.wordLift, this.letterLift, this.colors);
    }

    private LyricLineSplit splitBracketedText(LyricLine lyricLine) {
        StringBuilder mainBuilder = new StringBuilder();
        StringBuilder backgroundBuilder = new StringBuilder();
        boolean[] bracketedMask = new boolean[lyricLine.getText().length()];
        int copiedUpTo = 0;
        int openIndex = -1;

        for (int i = 0; i < lyricLine.getText().length(); i++) {
            label121:
            {
                char currentChar = lyricLine.getText().charAt(i);
                if (openIndex < 0) {

                    if (currentChar == '(') {
                        break label121;
                    }

                    if (currentChar == '（') {
                        break label121;
                    }
                }

                if (openIndex < 0) {
                    continue;
                }

                if (currentChar != ')') {

                    if (currentChar != '）') {
                        continue;
                    }
                }

                mainBuilder.append(lyricLine.getText(), copiedUpTo, openIndex);
                if (!backgroundBuilder.isEmpty()
                        && !Character.isWhitespace(
                                backgroundBuilder.charAt(backgroundBuilder.length() - 1))) {

                    backgroundBuilder.append(' ');
                }

                backgroundBuilder.append(lyricLine.getText(), openIndex + 1, i);

                for (int j = openIndex; j <= i; j++) {
                    bracketedMask[j] = true;
                }

                copiedUpTo = i + 1;
                openIndex = -1;
                continue;
            }

            openIndex = i;
        }

        if (backgroundBuilder.isEmpty()) {
            return new LyricLineSplit(lyricLine.getText(), "", lyricLine.getWords());
        }

        mainBuilder.append(lyricLine.getText(), copiedUpTo, lyricLine.getText().length());
        String strippedMain = mainBuilder.toString().strip();

        String mainText = strippedMain.replaceAll("\\s{2,}", " ");
        String strippedBackground = backgroundBuilder.toString().strip();

        String backgroundText = strippedBackground.replaceAll("\\s{2,}", " ");
        if (!lyricLine.hasWords()) {
            return new LyricLineSplit(mainText, backgroundText, null);
        }

        java.util.ArrayList keptWords = new java.util.ArrayList();
        int searchFrom = 0;

        for (LyricWord word : lyricLine.getWords()) {
            String wordText = word.getText().strip();
            if (!wordText.isEmpty()) {
                int wordStart = lyricLine.getText().indexOf(wordText, searchFrom);
                if (wordStart >= 0) {
                    searchFrom = wordStart + wordText.length();
                    boolean bracketed = false;

                    for (int charIndex = wordStart; charIndex < searchFrom; charIndex++) {
                        if (bracketedMask[charIndex]) {
                            bracketed = true;
                            break;
                        }
                    }

                    if (!bracketed) {
                        keptWords.add(word);
                    }
                }
            }
        }

        return new LyricLineSplit(mainText, backgroundText, List.copyOf(keptWords));
    }

    private String formatPlaybackTime(long positionMs) {
        long minutes;

        long totalSeconds = positionMs / 1000L;

        minutes = totalSeconds / 60L;

        Object[] args = new Object[1];

        args[0] = totalSeconds % 60L;
        return minutes + ":" + String.format("%02d", args);
    }

    private SizedFont selectFont(String fontName, float size, String text) {

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch > '~') {

                String cjkFontName;
                if (fontName.contains("Bold")) {

                    cjkFontName = "Noto Sans JP Bold";
                } else {

                    cjkFontName = "Noto Sans JP";
                }

                return FontManager.getFont(cjkFontName, size);
            }
        }

        return FontManager.getThemedFont(fontName, size);
    }

    private boolean endsWithWhitespace(String text) {
        return !text.isEmpty() && Character.isWhitespace(text.charAt(text.length() - 1));
    }

    private boolean containsRtlText(String text) {

        for (int i = 0; i < text.length(); i++) {
            byte directionality = Character.getDirectionality(text.charAt(i));
            if (directionality == 1 || directionality == 2) {
                return true;
            }
        }

        return false;
    }

    private List<LyricSegment> layoutWords(
            LyricLine lyricLine, SizedFont font, float maxWidth, long lineEndMs) {
        java.util.ArrayList wordTexts = new java.util.ArrayList();
        java.util.ArrayList startTimes = new java.util.ArrayList();
        java.util.ArrayList endTimes = new java.util.ArrayList();
        java.util.ArrayList<Boolean> spaceAfter = new java.util.ArrayList<>();
        boolean hasWordTimings = lyricLine.hasWords();
        if (hasWordTimings) {
            List lyricWords = lyricLine.getWords();
            java.util.ArrayList rawTexts = new java.util.ArrayList(lyricWords.size());

            for (int i = 0; i < lyricWords.size(); i++) {
                LyricWord lyricWord = (LyricWord) lyricWords.get(i);
                String rawText = lyricWord.getText();
                String strippedText = rawText.strip();
                if (!strippedText.isEmpty()) {
                    long wordEndMs =
                            lyricWord.getEndMs() > lyricWord.getStartMs()
                                    ? lyricWord.getEndMs()
                                    : (i + 1 < lyricWords.size()
                                            ? ((LyricWord) lyricWords.get(i + 1)).getStartMs()
                                            : lineEndMs);
                    wordTexts.add(strippedText);
                    rawTexts.add(rawText);
                    startTimes.add(lyricWord.getStartMs());
                    endTimes.add(Math.max(lyricWord.getStartMs() + 1L, wordEndMs));
                }
            }

            this.computeTrailingSpaces(lyricLine.getText(), wordTexts, rawTexts, spaceAfter);
        } else {
            long lineStartMs = lyricLine.getStartMs();
            long lineDurationMs = Math.max(1L, lineEndMs - lineStartMs);
            String strippedLine = lyricLine.getText().strip();

            String[] tokens = strippedLine.split("\\s+");
            float totalWidth = 0.0F;

            for (String token : tokens) {
                totalWidth += font.getStringWidth(token);
            }

            float consumedWidth = 0.0F;

            for (String currentToken : tokens) {
                if (!currentToken.isEmpty()) {
                    wordTexts.add(currentToken);
                    startTimes.add(
                            lineStartMs
                                    + Math.round(
                                            (float) lineDurationMs
                                                    * consumedWidth
                                                    / Math.max(0.001F, totalWidth)));
                    consumedWidth += font.getStringWidth(currentToken);
                    endTimes.add(
                            lineStartMs
                                    + Math.round(
                                            (float) lineDurationMs
                                                    * consumedWidth
                                                    / Math.max(0.001F, totalWidth)));
                    spaceAfter.add(true);
                }
            }
        }

        java.util.ArrayList segments = new java.util.ArrayList(wordTexts.size());
        float spaceWidth = font.getStringWidth(" ");
        float cursorX = 0.0F;
        int rowIndex = 0;

        for (int wordIndex = 0; wordIndex < wordTexts.size(); wordIndex++) {
            String wordText = (String) wordTexts.get(wordIndex);
            float wordWidth = font.getStringWidth(wordText);
            if (cursorX > 0.0F && (wordIndex == 0 || (Boolean) spaceAfter.get(wordIndex - 1))) {
                float groupWidth = wordWidth;

                for (int j = wordIndex; !spaceAfter.get(j) && j + 1 < wordTexts.size(); j++) {
                    groupWidth += font.getStringWidth((String) wordTexts.get(j + 1));
                }

                if (cursorX + groupWidth > maxWidth) {
                    rowIndex++;
                    cursorX = 0.0F;
                }
            }

            boolean letterAnimated;
            long segmentStartMs;
            long segmentEndMs;
            label92:
            {
                segmentStartMs = (Long) startTimes.get(wordIndex);
                segmentEndMs = (Long) endTimes.get(wordIndex);
                if (hasWordTimings && wordText.length() > 1) {
                    long segmentDurationMs =
                            (Long) endTimes.get(wordIndex) - (Long) startTimes.get(wordIndex);

                    if (segmentDurationMs >= 1000L && !this.containsRtlText(wordText)) {
                        letterAnimated = true;
                        break label92;
                    }
                }

                letterAnimated = false;
            }

            LyricSegment segment =
                    new LyricSegment(
                            wordText,
                            cursorX,
                            rowIndex,
                            segmentStartMs,
                            segmentEndMs,
                            letterAnimated,
                            false);
            segments.add(segment);
            cursorX += wordWidth + (spaceAfter.get(wordIndex) ? spaceWidth : 0.0F);
        }

        return segments;
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        long breakDurationMs;

        this.mediaSession.poll();
        TrackInfo track = this.mediaSession.getCurrentTrack();
        if (track != null) {
            if (this.lyrics.getValue()) {
                LyricsState.requestLyrics(track);
            }

            SizedFont titleFont = this.selectFont("Product Sans Bold", 9.0F, track.getTitle());

            SizedFont artistFont = this.selectFont("Product Sans Regular", 7.0F, track.getArtist());

            SizedFont pausedFont = FontManager.getThemedFont("Product Sans Regular", 7.0F);

            SizedFont timeFont = FontManager.getThemedFont("Product Sans Regular", 6.0F);

            Color color = new Color(238, 239, 242);

            Color color2 = new Color(145, 147, 154);
            Color[] themeColors = this.colors.getColors();
            int backgroundAlpha = Theme.getBackgroundAlpha();
            float hudX = this.hudWidget.getX();
            float hudY = this.hudWidget.getY();
            float hudScale = this.hudWidget.getScale();
            List lyricLines = this.lyrics.getValue() ? LyricsState.getLyrics() : null;
            float hudHeight = lyricLines == null ? 58.0F : 101.0F;
            this.hudWidget.setHeight(hudHeight);
            if (Effects.isMusicViewerBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangle(
                                hudX,
                                hudY,
                                190.0F * hudScale,
                                hudHeight * hudScale,
                                8.0F * hudScale);
            }

            render2DEvent.getContext().pushPose();
            render2DEvent.getContext().translate(hudX, hudY);
            render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
            Render2DContext backgroundContext = render2DEvent.getContext();
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(0.0F, 0.0F, 190.0F, hudHeight)
                            .setRadius(8.0F)
                            .setShadowEnabled(Effects.isMusicViewerShadow());

            long segmentStartMs;
            long segmentEndMs;
            backgroundContext.drawRect(
                    backgroundShape.setColor(new Color(18, 18, 22, backgroundAlpha)));
            GpuTexture gpuTexture = this.mediaSession.getCoverTexture();
            if (gpuTexture == null) {
                Render2DContext placeholderContext = render2DEvent.getContext();
                RoundedRectShape placeholderShape =
                        RoundedRectShape.obtain()
                                .setBounds(8.0F, 8.0F, 42.0F, 42.0F)
                                .setRadius(4.0F);

                placeholderContext.drawRect(
                        placeholderShape.setColor(new Color(28, 28, 34, backgroundAlpha)));
            } else {
                Render2DContext coverContext = render2DEvent.getContext();
                TexturedRectShape coverShape =
                        TexturedRectShape.obtain()
                                .setBounds(8.0F, 8.0F, 42.0F, 42.0F)
                                .setTexture(gpuTexture)
                                .setUv(0.0F, 0.0F, 1.0F, 1.0F)
                                .setRadius(4.0F);
                Color color3;

                short coverAlpha;
                if (track.isPlaying()) {

                    coverAlpha = 255;
                } else {

                    coverAlpha = 150;
                }

                color3 = new Color(255, 255, 255, coverAlpha);
                coverContext.drawTexture(coverShape.setColor(color3));
            }

            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(
                                            titleFont.truncateWithEllipsis(
                                                    track.getTitle(), 124.0F))
                                    .setPosition(58.0F, 9.0F)
                                    .setFont(titleFont.getMsdfFont())
                                    .setFontSize(titleFont.getSize())
                                    .enableShadow()
                                    .setColor(color.getRGB()));

            String artistName = track.getArtist();
            float artistMaxWidth;
            if (track.isPlaying()) {
                artistMaxWidth = 124.0F;
            } else {

                artistMaxWidth = 120.0F - pausedFont.getStringWidth("• Paused");
            }

            String artistText = artistFont.truncateWithEllipsis(artistName, artistMaxWidth);
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(artistText)
                                    .setPosition(58.0F, 21.0F)
                                    .setFont(artistFont.getMsdfFont())
                                    .setFontSize(artistFont.getSize())
                                    .enableShadow()
                                    .setColor(color2.getRGB()));
            if (!track.isPlaying()) {
                Render2DContext pausedContext = render2DEvent.getContext();
                TextDrawRequest pausedRequest = TextDrawRequest.obtain();

                pausedContext.drawText(
                        pausedRequest
                                .setText("• Paused")
                                .setPosition(62.0F + artistFont.getStringWidth(artistText), 21.0F)
                                .setFont(pausedFont.getMsdfFont())
                                .setFontSize(pausedFont.getSize())
                                .enableShadow()
                                .setColor(color2.getRGB()));
            }

            long positionMs = this.mediaSession.getTrackPosition(track);
            long durationMs = Math.max(track.getDurationMs(), 1L);
            String durationText = this.formatPlaybackTime(track.getDurationMs());
            Render2DContext trackContext = render2DEvent.getContext();
            RoundedRectShape trackShape =
                    RoundedRectShape.obtain().setBounds(58.0F, 37.0F, 124.0F, 3.0F).setRadius(1.5F);

            long segmentDurationMs;
            trackContext.drawRect(trackShape.setColor(new Color(255, 255, 255, 40)));
            float progressWidth =
                    124.0F * Math.clamp((float) positionMs / (float) durationMs, 0.0F, 1.0F);
            if (progressWidth > 0.5F) {
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(58.0F, 37.0F, progressWidth, 3.0F)
                                        .setRadius(1.5F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setHorizontalGradient(
                                                                ColorUtil.waveBlend(
                                                                        hudX + 58.0F * hudScale,
                                                                        themeColors[0].getRGB(),
                                                                        themeColors[1].getRGB()),
                                                                ColorUtil.waveBlend(
                                                                        hudX
                                                                                + (58.0F
                                                                                                + progressWidth)
                                                                                        * hudScale,
                                                                        themeColors[0].getRGB(),
                                                                        themeColors[1].getRGB()))));
            }

            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(this.formatPlaybackTime(positionMs))
                                    .setPosition(58.0F, 43.0F)
                                    .setFont(timeFont.getMsdfFont())
                                    .setFontSize(timeFont.getSize())
                                    .enableShadow()
                                    .setColor(color2.getRGB()));
            render2DEvent
                    .getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setText(durationText)
                                    .setPosition(
                                            182.0F - timeFont.getStringWidth(durationText), 43.0F)
                                    .setFont(timeFont.getMsdfFont())
                                    .setFontSize(timeFont.getSize())
                                    .enableShadow()
                                    .setColor(color2.getRGB()));
            if (lyricLines != null) {
                long lyricTimeMs =
                        Math.clamp(
                                positionMs - Math.round(this.lyricsOffset.getValue()),
                                0L,
                                durationMs);
                int currentLineIndex = -1;

                while (currentLineIndex + 1 < lyricLines.size()
                        && ((LyricLine) lyricLines.get(currentLineIndex + 1)).getStartMs()
                                <= lyricTimeMs) {
                    currentLineIndex++;
                }

                int[] lineRowStarts = new int[lyricLines.size()];
                int[] lineRowCounts = new int[lyricLines.size()];
                java.util.ArrayList lineSegments = new java.util.ArrayList(lyricLines.size());
                java.util.ArrayList lineFonts = new java.util.ArrayList(lyricLines.size());
                java.util.ArrayList backgroundFonts = new java.util.ArrayList(lyricLines.size());
                int totalRows = 0;

                for (int lineIndex = 0; lineIndex < lyricLines.size(); lineIndex++) {
                    LyricLine lyricLine = (LyricLine) lyricLines.get(lineIndex);
                    lineRowStarts[lineIndex] = totalRows;
                    if (lyricLine.getText().isBlank()) {
                        lineSegments.add(null);
                        lineFonts.add(null);
                        backgroundFonts.add(null);
                    } else {
                        long nextLineStartMs =
                                lineIndex + 1 < lyricLines.size()
                                        ? ((LyricLine) lyricLines.get(lineIndex + 1)).getStartMs()
                                        : durationMs;
                        long lineEndMs =
                                lyricLine.getEndMs() > 0L ? lyricLine.getEndMs() : nextLineStartMs;
                        LyricLineSplit lineSplit = this.splitBracketedText(lyricLine);

                        SizedFont lineFont =
                                this.selectFont(
                                        "Product Sans Bold",
                                        8.0F,
                                        lineSplit.getMainText().isBlank()
                                                ? lyricLine.getText()
                                                : lineSplit.getMainText());
                        List<LyricSegment> segments;
                        SizedFont backgroundFont;
                        if (lineSplit.getBackgroundText().isBlank()) {
                            segments = this.layoutWords(lyricLine, lineFont, 174.0F, lineEndMs);
                            backgroundFont = null;
                        } else {
                            segments =
                                    this.layoutWords(
                                            new LyricLine(
                                                    lyricLine.getStartMs(),
                                                    lyricLine.getEndMs(),
                                                    lineSplit.getMainText(),
                                                    lineSplit.getWords()),
                                            lineFont,
                                            174.0F,
                                            lineEndMs);

                            backgroundFont =
                                    this.selectFont(
                                            "Product Sans Bold",
                                            6.0F,
                                            lineSplit.getBackgroundText());
                            int mainRows = 0;

                            for (LyricSegment mainSegment : segments) {
                                mainRows = Math.max(mainRows, mainSegment.getRow() + 1);
                            }

                            long backgroundStartMs =
                                    lineSplit.getWords() != null && !lineSplit.getWords().isEmpty()
                                            ? lineSplit.getWords().getFirst().getStartMs()
                                            : lyricLine.getStartMs();
                            long backgroundEndMs =
                                    lineSplit.getWords() != null && !lineSplit.getWords().isEmpty()
                                            ? (lineSplit.getWords().getLast().getEndMs()
                                                            > backgroundStartMs
                                                    ? lineSplit.getWords().getLast().getEndMs()
                                                    : lineEndMs)
                                            : lineEndMs;

                            for (LyricSegment backgroundSegment :
                                    this.layoutWords(
                                            new LyricLine(
                                                    backgroundStartMs,
                                                    backgroundEndMs,
                                                    lineSplit.getBackgroundText(),
                                                    null),
                                            backgroundFont,
                                            174.0F,
                                            backgroundEndMs)) {
                                String segmentText;
                                float segmentOffsetX;
                                int segmentRow;
                                boolean segmentLetterAnimated;
                                label523:
                                {
                                    segmentText = backgroundSegment.getText();
                                    segmentOffsetX = backgroundSegment.getOffsetX();
                                    segmentRow = backgroundSegment.getRow() + mainRows;
                                    segmentStartMs = backgroundSegment.getStartMs();
                                    segmentEndMs = backgroundSegment.getEndMs();
                                    if (backgroundSegment.getText().length() > 1) {
                                        segmentDurationMs =
                                                backgroundSegment.getEndMs()
                                                        - backgroundSegment.getStartMs();

                                        if (segmentDurationMs >= 1000L
                                                && !this.containsRtlText(
                                                        backgroundSegment.getText())) {
                                            segmentLetterAnimated = true;
                                            break label523;
                                        }
                                    }

                                    segmentLetterAnimated = false;
                                }

                                LyricSegment positionedSegment =
                                        new LyricSegment(
                                                segmentText,
                                                segmentOffsetX,
                                                segmentRow,
                                                segmentStartMs,
                                                segmentEndMs,
                                                segmentLetterAnimated,
                                                true);
                                segments.add(positionedSegment);
                            }
                        }

                        for (LyricSegment lineSegment : segments) {
                            lineRowCounts[lineIndex] =
                                    Math.max(lineRowCounts[lineIndex], lineSegment.getRow() + 1);
                        }

                        lineSegments.add(segments);
                        lineFonts.add(lineFont);
                        backgroundFonts.add(backgroundFont);
                        totalRows += lineRowCounts[lineIndex];
                    }
                }

                long breakStartMs = -1L;

                long breakEndMs = -1L;
                if (currentLineIndex < 0) {
                    breakStartMs = 0L;
                    breakEndMs = ((LyricLine) lyricLines.getFirst()).getStartMs();
                } else {
                    LyricLine currentLine = (LyricLine) lyricLines.get(currentLineIndex);
                    long currentNextStartMs =
                            currentLineIndex + 1 < lyricLines.size()
                                    ? ((LyricLine) lyricLines.get(currentLineIndex + 1))
                                            .getStartMs()
                                    : durationMs;
                    if (currentLine.getText().isBlank()) {
                        breakStartMs = currentLine.getStartMs();
                        breakEndMs = currentNextStartMs;
                    } else if (currentLine.getEndMs() > 0L
                            && lyricTimeMs >= currentLine.getEndMs()) {
                        breakStartMs = currentLine.getEndMs();
                        breakEndMs = currentNextStartMs;
                    }
                }

                boolean longBreak;
                label506:
                {
                    if (breakStartMs >= 0L) {
                        breakDurationMs = breakEndMs - breakStartMs;

                        if (breakDurationMs >= 2000L) {
                            longBreak = true;
                            break label506;
                        }
                    }

                    longBreak = false;
                }

                boolean inBreak = longBreak;
                if (inBreak) {
                    this.breakLineIndex = currentLineIndex;
                } else if (this.breakLineIndex >= lyricLines.size()) {
                    this.breakLineIndex = -1;
                }

                boolean wordsSettled = true;
                if (currentLineIndex >= 0
                        && this.animatedLine == lyricLines.get(currentLineIndex)) {
                    for (WordAnimation wordAnimation : this.wordAnimations) {
                        if (!wordAnimation.isSettled()) {
                            wordsSettled = false;
                            break;
                        }
                    }
                }

                boolean advanceToNext =
                        !inBreak
                                && currentLineIndex >= 0
                                && currentLineIndex + 1 < lyricLines.size()
                                && ((LyricLine) lyricLines.get(currentLineIndex)).getEndMs() > 0L
                                && lyricTimeMs
                                        >= ((LyricLine) lyricLines.get(currentLineIndex)).getEndMs()
                                && wordsSettled;
                this.breakGapAnimation.animateTo(inBreak ? 13.0 : 0.0);
                this.scrollAnimation.animateTo(
                        (currentLineIndex < 0
                                        ? 0
                                        : lineRowStarts[
                                                advanceToNext
                                                        ? currentLineIndex + 1
                                                        : currentLineIndex])
                                * 13.0);
                float breakGap = (float) this.breakGapAnimation.getValue();
                render2DEvent.getContext().beginScissor(8.0F, 55.0F, 174.0F, 42.0F);
                if (breakGap > 0.5F) {
                    int breakLineRows =
                            this.breakLineIndex >= 0 ? lineRowCounts[this.breakLineIndex] : 0;
                    float dotsY =
                            58.0F
                                    + (this.breakLineIndex < 0
                                            ? 0.0F
                                            : (lineRowStarts[this.breakLineIndex] + breakLineRows)
                                                    * 13.0F)
                                    + breakGap
                                    - 13.0F
                                    - (float) this.scrollAnimation.getValue();
                    float breakProgress =
                            inBreak
                                    ? Math.clamp(
                                            (float) (lyricTimeMs - breakStartMs)
                                                    / (float) (breakEndMs - breakStartMs),
                                            0.0F,
                                            1.0F)
                                    : 1.0F;

                    for (int dotIndex = 0; dotIndex < 3; dotIndex++) {
                        float dotProgress = Math.clamp(breakProgress * 3.0F - dotIndex, 0.0F, 1.0F);
                        float dotOffsetY = (float) Math.sin(dotProgress * Math.PI) * -2.5F;
                        render2DEvent
                                .getContext()
                                .drawRect(
                                        RoundedRectShape.obtain()
                                                .setBounds(
                                                        9.0F + dotIndex * 8.0F,
                                                        dotsY + 3.0F + dotOffsetY,
                                                        5.0F,
                                                        5.0F)
                                                .setRadius(2.5F)
                                                .setColor(
                                                        ColorUtil.withAlpha(
                                                                ColorUtil.lerpColor(
                                                                        color2,
                                                                        themeColors[0],
                                                                        dotProgress),
                                                                Math.round(
                                                                        (90.0F
                                                                                        + 165.0F
                                                                                                * dotProgress)
                                                                                * breakGap
                                                                                / 13.0F))));
                    }
                }

                for (int drawLineIndex = 0; drawLineIndex < lyricLines.size(); drawLineIndex++) {
                    LyricLine drawLine = (LyricLine) lyricLines.get(drawLineIndex);
                    List<LyricSegment> drawSegments =
                            (List<LyricSegment>) lineSegments.get(drawLineIndex);
                    if (drawSegments != null) {
                        SizedFont drawFont = (SizedFont) lineFonts.get(drawLineIndex);
                        float lineY =
                                58.0F
                                        + lineRowStarts[drawLineIndex] * 13.0F
                                        + (drawLineIndex > this.breakLineIndex ? breakGap : 0.0F)
                                        - (float) this.scrollAnimation.getValue();
                        float focus =
                                Math.clamp(
                                        1.0F
                                                - Math.abs(
                                                                lineRowStarts[drawLineIndex] * 13.0F
                                                                        - (float)
                                                                                this.scrollAnimation
                                                                                        .getValue())
                                                        / 26.0F,
                                        0.0F,
                                        1.0F);
                        Color color4 = ColorUtil.lerpColor(color2, color, focus);
                        int lineColor =
                                new Color(
                                                color4.getRed(),
                                                color4.getGreen(),
                                                color4.getBlue(),
                                                Math.round(90.0F + 165.0F * focus))
                                        .getRGB();
                        if (drawLineIndex != currentLineIndex) {
                            boolean upcoming =
                                    drawLineIndex > currentLineIndex
                                            && (!advanceToNext
                                                    || drawLineIndex != currentLineIndex + 1);
                            float lineScale = upcoming ? 0.95F : 0.9F + 0.1F * focus;
                            int segmentColor;
                            if (upcoming) {
                                int red = color4.getRed();
                                int green = color4.getGreen();
                                int blue = color4.getBlue();
                                segmentColor = new Color(red, green, blue, 89).getRGB();
                            } else {
                                segmentColor = lineColor;
                            }

                            int baseColor = segmentColor;

                            for (LyricSegment drawSegment : drawSegments) {
                                SizedFont segmentFont =
                                        drawSegment.isBackground()
                                                ? (SizedFont) backgroundFonts.get(drawLineIndex)
                                                : drawFont;
                                float segmentY = lineY + drawSegment.getRow() * 13.0F;
                                if (!(segmentY < 47.0F) && !(segmentY > 97.0F)) {
                                    render2DEvent.getContext().pushPose();
                                    render2DEvent.getContext().translate(8.0F, segmentY + 4.0F);
                                    render2DEvent.getContext().scale(lineScale, lineScale, 1.0F);
                                    render2DEvent.getContext().translate(-8.0F, -(segmentY + 4.0F));
                                    Render2DContext textContext = render2DEvent.getContext();
                                    TextDrawRequest textRequest =
                                            TextDrawRequest.obtain()
                                                    .setText(drawSegment.getText())
                                                    .setPosition(
                                                            8.0F + drawSegment.getOffsetX(),
                                                            segmentY)
                                                    .setFont(segmentFont.getMsdfFont())
                                                    .setFontSize(segmentFont.getSize())
                                                    .enableShadow();
                                    int textColor;
                                    if (drawSegment.isBackground()) {
                                        Color color5 = new Color(baseColor, true);

                                        int alpha = baseColor >>> 24;

                                        textColor =
                                                ColorUtil.withAlpha(
                                                                color5,
                                                                Math.round((alpha & 0xFF) * 0.68F))
                                                        .getRGB();
                                    } else {
                                        textColor = baseColor;
                                    }

                                    textContext.drawText(textRequest.setColor(textColor));
                                    render2DEvent.getContext().popPose();
                                }
                            }
                        } else {
                            Color color6 = new Color(lineColor, true);
                            long nowNanos = System.nanoTime();
                            if (this.animatedLine != drawLine) {
                                this.animatedLine = drawLine;

                                this.lastFrameNanos = nowNanos - 16666667L;
                                this.wordAnimations.clear();

                                for (LyricSegment newSegment : drawSegments) {
                                    java.util.ArrayList letterAnimations =
                                            new java.util.ArrayList();
                                    if (newSegment.isLetterAnimated()) {
                                        for (int charIndex = 0;
                                                charIndex < newSegment.getText().length();
                                                charIndex++) {
                                            letterAnimations.add(
                                                    new LetterAnimation(
                                                            new SpringValue(0.95, 0.88, 0.64),
                                                            new SpringValue(0.01, 1.45, 0.4)));
                                        }
                                    }

                                    this.wordAnimations.add(
                                            new WordAnimation(
                                                    new SpringValue(0.95, 0.88, 0.64),
                                                    new SpringValue(0.01, 1.45, 0.4),
                                                    letterAnimations));
                                }
                            }

                            double deltaSeconds =
                                    Math.max((nowNanos - this.lastFrameNanos) / 1.0E9, 0.001);
                            this.lastFrameNanos = nowNanos;

                            for (int segmentIndex = 0;
                                    segmentIndex < drawSegments.size();
                                    segmentIndex++) {
                                LyricSegment activeSegment =
                                        (LyricSegment) drawSegments.get(segmentIndex);
                                SizedFont activeFont =
                                        activeSegment.isBackground()
                                                ? (SizedFont) backgroundFonts.get(drawLineIndex)
                                                : drawFont;
                                String activeText = activeSegment.getText();
                                WordAnimation segmentAnimation =
                                        this.wordAnimations.get(segmentIndex);
                                boolean hasLetters = !segmentAnimation.getLetters().isEmpty();
                                long wordStartMs = activeSegment.getStartMs();
                                long wordEndMs = activeSegment.getEndMs();
                                long wordDurationMs = Math.max(1L, wordEndMs - wordStartMs);
                                float wordProgress =
                                        Math.clamp(
                                                (float) (lyricTimeMs - wordStartMs)
                                                        / (float) wordDurationMs,
                                                0.0F,
                                                1.0F);
                                float wordY = lineY + activeSegment.getRow() * 13.0F;
                                float wordX = 8.0F + activeSegment.getOffsetX();
                                float wordWidth = activeFont.getStringWidth(activeText);
                                boolean visible = wordY >= 47.0F && wordY <= 97.0F;
                                segmentAnimation
                                        .getScale()
                                        .setTarget(
                                                this.evaluateEaseCurve(
                                                        wordProgress, 0.7, 0.95, 1.0505, 1.0));
                                segmentAnimation
                                        .getLift()
                                        .setTarget(
                                                this.evaluateEaseCurve(
                                                        wordProgress,
                                                        0.9,
                                                        0.01,
                                                        -0.016666666666666666,
                                                        0.0));
                                float wordScale =
                                        (float) segmentAnimation.getScale().advance(deltaSeconds);
                                float liftOffset =
                                        (float) segmentAnimation.getLift().advance(deltaSeconds)
                                                * this.wordLift.getValue();
                                float centerX = wordX + wordWidth / 2.0F;
                                float centerY = wordY + activeFont.getSize() / 2.0F;
                                Color color7 =
                                        activeSegment.isBackground()
                                                ? ColorUtil.withAlpha(
                                                        color6,
                                                        Math.round(color6.getAlpha() * 0.68F))
                                                : color6;
                                if (!hasLetters) {
                                    if (visible) {
                                        float highlightStartX =
                                                wordX + wordWidth * (-0.2F + 1.2F * wordProgress);
                                        float highlightEndX = highlightStartX + wordWidth * 0.2F;
                                        float gradientStartX =
                                                hudX
                                                        + hudScale
                                                                * (centerX
                                                                        + wordScale
                                                                                * (highlightStartX
                                                                                        - centerX));
                                        float gradientEndX =
                                                hudX
                                                        + hudScale
                                                                * (centerX
                                                                        + wordScale
                                                                                * (highlightEndX
                                                                                        - centerX));
                                        int waveColor =
                                                ColorUtil.waveBlend(
                                                        hudX
                                                                + (wordX + wordWidth / 2.0F)
                                                                        * hudScale,
                                                        themeColors[0].getRGB(),
                                                        themeColors[1].getRGB());
                                        render2DEvent.getContext().pushPose();
                                        render2DEvent
                                                .getContext()
                                                .translate(0.0F, activeFont.getSize() * liftOffset);
                                        render2DEvent.getContext().translate(centerX, centerY);
                                        render2DEvent
                                                .getContext()
                                                .scale(wordScale, wordScale, 1.0F);
                                        render2DEvent.getContext().translate(-centerX, -centerY);
                                        Render2DContext wordContext = render2DEvent.getContext();
                                        TextDrawRequest wordRequest =
                                                TextDrawRequest.obtain()
                                                        .setText(activeText)
                                                        .setPosition(wordX, wordY)
                                                        .setFont(activeFont.getMsdfFont())
                                                        .setFontSize(activeFont.getSize())
                                                        .enableShadow();
                                        ShapePaint wordPaint = ShapePaint.obtain();
                                        int wordColor;
                                        if (activeSegment.isBackground()) {
                                            Color color8 = new Color(waveColor, true);

                                            wordColor = ColorUtil.withAlpha(color8, 174).getRGB();
                                        } else {
                                            wordColor = waveColor;
                                        }

                                        wordContext.drawText(
                                                wordRequest.setPaint(
                                                        wordPaint.setLinearGradient(
                                                                wordColor,
                                                                color7.getRGB(),
                                                                gradientStartX,
                                                                0.0F,
                                                                gradientEndX,
                                                                0.0F)));
                                        render2DEvent.getContext().popPose();
                                    }
                                } else {
                                    int activeLetterIndex = -1;
                                    float letterProgress = 0.0F;

                                    for (int searchIndex = 0;
                                            searchIndex < activeText.length();
                                            searchIndex++) {
                                        long rangeStartMs =
                                                wordStartMs
                                                        + Math.round(
                                                                (float) wordDurationMs
                                                                        * ((float) searchIndex
                                                                                / activeText
                                                                                        .length()));
                                        long rangeEndMs =
                                                wordStartMs
                                                        + Math.round(
                                                                (float) wordDurationMs
                                                                        * ((float) (searchIndex + 1)
                                                                                / activeText
                                                                                        .length()));
                                        if (lyricTimeMs >= rangeStartMs
                                                && lyricTimeMs < rangeEndMs) {
                                            activeLetterIndex = searchIndex;
                                            letterProgress =
                                                    Math.clamp(
                                                            (float) (lyricTimeMs - rangeStartMs)
                                                                    / (float)
                                                                            Math.max(
                                                                                    1L,
                                                                                    rangeEndMs
                                                                                            - rangeStartMs),
                                                            0.0F,
                                                            1.0F);
                                            break;
                                        }
                                    }

                                    float letterX = wordX;

                                    for (int letterIndex = 0;
                                            letterIndex < activeText.length();
                                            letterIndex++) {
                                        char letter = activeText.charAt(letterIndex);
                                        float letterWidth =
                                                activeFont.getStringWidth(String.valueOf(letter));
                                        long letterStartMs =
                                                wordStartMs
                                                        + Math.round(
                                                                (float) wordDurationMs
                                                                        * ((float) letterIndex
                                                                                / activeText
                                                                                        .length()));
                                        LetterAnimation letterAnimation =
                                                segmentAnimation.getLetters().get(letterIndex);
                                        double scaleTarget = 0.95;
                                        double liftTarget = 0.01;
                                        if (activeLetterIndex >= 0) {
                                            double distance =
                                                    Math.abs(letterIndex - activeLetterIndex);
                                            double falloff = 1.0 / (1.0 + Math.pow(distance, 2.8));
                                            scaleTarget +=
                                                    (this.evaluateEaseCurve(
                                                                            letterProgress,
                                                                            0.7,
                                                                            0.95,
                                                                            1.175,
                                                                            1.0)
                                                                    - 0.95)
                                                            * falloff;
                                            liftTarget +=
                                                    (this.evaluateEaseCurve(
                                                                            letterProgress,
                                                                            0.9,
                                                                            0.01,
                                                                            -0.017857142857142856,
                                                                            0.0)
                                                                    - 0.01)
                                                            * falloff;
                                        }

                                        if (lyricTimeMs < letterStartMs) {
                                            scaleTarget = 0.95;
                                            liftTarget = 0.01;
                                        } else if (lyricTimeMs >= wordEndMs) {
                                            scaleTarget = 1.0;
                                            liftTarget = 0.0;
                                        }

                                        letterAnimation.getScale().setTarget(scaleTarget);
                                        letterAnimation.getLift().setTarget(liftTarget);
                                        float letterScale =
                                                (float)
                                                        letterAnimation
                                                                .getScale()
                                                                .advance(deltaSeconds);
                                        float letterLiftOffset =
                                                (float)
                                                                letterAnimation
                                                                        .getLift()
                                                                        .advance(deltaSeconds)
                                                        * this.letterLift.getValue();
                                        float letterCenterX = letterX + letterWidth / 2.0F;
                                        if (visible && !Character.isWhitespace(letter)) {
                                            float fillProgress =
                                                    wordProgress >= 1.0F
                                                                    || letterIndex
                                                                            < activeLetterIndex
                                                            ? 1.0F
                                                            : (letterIndex == activeLetterIndex
                                                                    ? (float)
                                                                            Math.sin(
                                                                                    letterProgress
                                                                                            * Math
                                                                                                    .PI
                                                                                            / 2.0)
                                                                    : 0.0F);
                                            Color color9 =
                                                    new Color(
                                                            ColorUtil.waveBlend(
                                                                    hudX
                                                                            + (letterX
                                                                                            + letterWidth
                                                                                                    / 2.0F)
                                                                                    * hudScale,
                                                                    themeColors[0].getRGB(),
                                                                    themeColors[1].getRGB()),
                                                            true);
                                            short letterAlpha;
                                            if (activeSegment.isBackground()) {

                                                letterAlpha = 174;
                                            } else {

                                                letterAlpha = 255;
                                            }

                                            Color color10 =
                                                    ColorUtil.lerpColor(
                                                            color7,
                                                            ColorUtil.withAlpha(
                                                                    color9, letterAlpha),
                                                            fillProgress);
                                            render2DEvent.getContext().pushPose();
                                            render2DEvent
                                                    .getContext()
                                                    .translate(
                                                            0.0F,
                                                            activeFont.getSize() * liftOffset);
                                            render2DEvent.getContext().translate(centerX, centerY);
                                            render2DEvent
                                                    .getContext()
                                                    .scale(wordScale, wordScale, 1.0F);
                                            render2DEvent
                                                    .getContext()
                                                    .translate(-centerX, -centerY);
                                            render2DEvent
                                                    .getContext()
                                                    .translate(
                                                            0.0F,
                                                            activeFont.getSize()
                                                                    * letterLiftOffset);
                                            render2DEvent
                                                    .getContext()
                                                    .translate(letterCenterX, centerY);
                                            render2DEvent
                                                    .getContext()
                                                    .scale(letterScale, letterScale, 1.0F);
                                            render2DEvent
                                                    .getContext()
                                                    .translate(-letterCenterX, -centerY);
                                            render2DEvent
                                                    .getContext()
                                                    .drawText(
                                                            TextDrawRequest.obtain()
                                                                    .setText(String.valueOf(letter))
                                                                    .setPosition(letterX, wordY)
                                                                    .setFont(
                                                                            activeFont
                                                                                    .getMsdfFont())
                                                                    .setFontSize(
                                                                            activeFont.getSize())
                                                                    .enableShadow()
                                                                    .setColor(color10.getRGB()));
                                            render2DEvent.getContext().popPose();
                                        }

                                        letterX += letterWidth;
                                    }
                                }
                            }
                        }
                    }
                }

                render2DEvent.getContext().endScissor();
            }

            render2DEvent.getContext().popPose();
        }
    }

    private double evaluateEaseCurve(
            double t, double splitPoint, double startValue, double splitValue, double endValue) {
        double firstSpan = splitPoint;
        double secondSpan = 1.0 - splitPoint;
        double curvature =
                (3.0 / secondSpan * (endValue - splitValue)
                                - 3.0 / firstSpan * (splitValue - startValue))
                        / 2.0;
        if (t <= splitPoint) {
            double localT = t;
            double linearTerm = (splitValue - startValue) / firstSpan - firstSpan * curvature / 3.0;
            return startValue
                    + linearTerm * localT
                    + curvature / (3.0 * firstSpan) * localT * localT * localT;
        } else {
            double tAfterSplit = t - splitPoint;
            double linearTermAfter =
                    (endValue - splitValue) / secondSpan - secondSpan * curvature * 2.0 / 3.0;
            return splitValue
                    + linearTermAfter * tAfterSplit
                    + curvature * tAfterSplit * tAfterSplit
                    - curvature / (3.0 * secondSpan) * tAfterSplit * tAfterSplit * tAfterSplit;
        }
    }

    private void computeTrailingSpaces(
            String text, List<String> words, List<String> rawWords, List<Boolean> spaceAfter) {
        spaceAfter.clear();
        boolean anyRawEndsWithSpace = false;

        for (String rawWord : rawWords) {
            if (this.endsWithWhitespace(rawWord)) {
                anyRawEndsWithSpace = true;
                break;
            }
        }

        int searchFrom = 0;

        for (int i = 0; i < words.size(); i++) {
            String word = (String) words.get(i);
            int foundAt = text.indexOf(word, searchFrom);
            if (foundAt < 0) {
                spaceAfter.add(
                        !anyRawEndsWithSpace || this.endsWithWhitespace((String) rawWords.get(i)));
            } else {
                searchFrom = foundAt + word.length();
                spaceAfter.add(
                        searchFrom < text.length()
                                && Character.isWhitespace(text.charAt(searchFrom)));
            }
        }
    }

    private static final String MODULE_ID = "9079288b470e2344edd08677232cf7ad";
    private static final String LYRICS_SETTING_ID = "f10fad59900197dbb4948db0a7dd5d3c";
    private static final String LYRICS_OFFSET_SETTING_ID = "4027c342c5af66e384e37308704d4c43";
    private static final String WORD_LIFT_SETTING_ID = "609e4b5618f5672cbd5e72096fd4c065";
    private static final String LETTER_LIFT_SETTING_ID = "955b853e5eff3eb7fa936ce8f45c106c";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
