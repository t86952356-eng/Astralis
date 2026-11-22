package astralis.ui.screen;

import astralis.scripting.ScriptSource;
import astralis.ui.component.FlatUiPalette;
import astralis.ui.component.FlatUiPaletteColors;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import java.util.List;

public final class ScriptDetailPanel implements FlatUiPalette {
    ScriptDetailPanel() {}

    void drawDetails(
            Render2DContext context,
            float x,
            float y,
            float panelWidth,
            float panelHeight,
            ScriptSource script,
            List<String> sourceLines,
            boolean easyMode,
            double mouseX,
            double mouseY) {

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 11.0F);

        SizedFont smallFont = FontManager.getFont("Product Sans Regular", 7.0F);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x, y, panelWidth, panelHeight)
                        .setColor(FlatUiPaletteColors.BACKGROUND_DIM));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x, y, 1.0F, panelHeight)
                        .setColor(FlatUiPaletteColors.DIVIDER));
        context.beginScissor(x + 12.0F, y, panelWidth - 24.0F, panelHeight);
        TextDrawRequest titleRequest = TextDrawRequest.obtain();
        String displayText;
        if (script == null) {

            displayText = "Your script library";
        } else {
            displayText = script.name();
        }

        context.drawText(
                titleRequest
                        .setText(titleFont.truncateWithEllipsis(displayText, panelWidth - 52.0F))
                        .setPosition(x + 12.0F, y + 14.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .setColor(FlatUiPaletteColors.TEXT.getRGB()));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText("x")
                        .setPosition(x + panelWidth - 28.0F, y + 8.0F)
                        .centerInBox(20.0F, 20.0F)
                        .setFont(smallFont.getMsdfFont())
                        .setFontSize(smallFont.getSize())
                        .setColor(FlatUiPaletteColors.TEXT_SECONDARY.getRGB()));
        TextDrawRequest subtitleRequest = TextDrawRequest.obtain();
        String subtitle;
        if (script == null) {
            subtitle = "Select a script to inspect its source.";
        } else {
            String typeLabel;
            if (easyMode) {
                typeLabel = "Easy Mode project";
            } else {
                typeLabel = "Java source";
            }

            if (script.enabled()) {

                displayText = "Enabled";
            } else if (script.loaded()) {

                displayText = "Loaded";
            } else {

                displayText = "Not loaded";
            }

            subtitle = typeLabel + "  /  " + displayText;
        }

        context.drawText(
                subtitleRequest
                        .setText(subtitle)
                        .setPosition(x + 12.0F, y + 34.0F)
                        .setFont(smallFont.getMsdfFont())
                        .setFontSize(smallFont.getSize())
                        .setColor(FlatUiPaletteColors.TEXT_SECONDARY.getRGB()));
        float previewHeight = Math.max(0.0F, panelHeight - 116.0F);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x + 12.0F, y + 57.0F, panelWidth - 24.0F, previewHeight)
                        .setRadius(6.0F)
                        .setColor(FlatUiPaletteColors.OVERLAY));
        context.beginScissor(
                x + 20.0F, y + 65.0F, panelWidth - 40.0F, Math.max(0.0F, previewHeight - 16.0F));

        for (int lineIndex = 0;
                lineIndex < sourceLines.size() && !(lineIndex * 12.0F >= previewHeight - 16.0F);
                lineIndex++) {
            TextDrawRequest lineRequest = TextDrawRequest.obtain();
            displayText = (String) sourceLines.get(lineIndex);

            context.drawText(
                    lineRequest
                            .setText(
                                    smallFont.truncateWithEllipsis(
                                            displayText.replace("\t", "    "), panelWidth - 40.0F))
                            .setPosition(x + 20.0F, y + 65.0F + lineIndex * 12.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .setColor(FlatUiPaletteColors.TEXT_SECONDARY.getRGB()));
        }

        context.endScissor();
        TextDrawRequest hintRequest = TextDrawRequest.obtain();
        if (easyMode) {

            displayText = "Continue editing your visual project.";
        } else {

            displayText = "Source preview is read-only. Edit Java from the folder.";
        }

        context.drawText(
                hintRequest
                        .setText(smallFont.truncateWithEllipsis(displayText, panelWidth - 24.0F))
                        .setPosition(x + 12.0F, y + panelHeight - 48.0F)
                        .setFont(smallFont.getMsdfFont())
                        .setFontSize(smallFont.getSize())
                        .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
        String buttonLabel;
        if (easyMode) {

            buttonLabel = "Open Easy Mode";
        } else {

            buttonLabel = "Open scripts folder";
        }

        ScriptToolbarRenderer.drawButton(
                context,
                buttonLabel,
                x + 12.0F,
                y + panelHeight - 32.0F,
                panelWidth - 24.0F,
                true,
                true,
                mouseX,
                mouseY);
        context.endScissor();
    }
}
