package astralis.ui;

import astralis.Astralis;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.util.FormattedCharSequence;

import java.awt.Color;
import java.util.ArrayList;

public final class TooltipOverlay implements InstanceAccess {
    private static Tooltip pendingTooltip;
    private static int tooltipMouseX;
    private static int tooltipMouseY;

    private TooltipOverlay() {}

    public static void setPendingTooltip(Tooltip tooltip, int mouseX, int mouseY) {
        pendingTooltip = tooltip;
        tooltipMouseX = mouseX;
        tooltipMouseY = mouseY;
    }

    public static void clearPendingTooltip() {
        pendingTooltip = null;
    }

    public static boolean hasPendingTooltip() {
        return pendingTooltip != null;
    }

    public static void renderRegistered(Render2DContext context) {
        if (pendingTooltip != null) {
            SizedFont font = FontManager.getFont("Product Sans Regular", 7.5F);
            ArrayList<String> lines = new ArrayList<>();

            for (FormattedCharSequence formattedCharSequence : pendingTooltip.toCharSequence(mc)) {
                StringBuilder lineBuilder = new StringBuilder();
                formattedCharSequence.accept(
                        (index, style, codePoint) -> {
                            lineBuilder.appendCodePoint(codePoint);
                            return true;
                        });
                lines.add(lineBuilder.toString());
            }

            if (lines.isEmpty()) {
                clearPendingTooltip();
            } else {
                float maxLineWidth = 0.0F;

                for (String measuredLine : lines) {
                    maxLineWidth = Math.max(maxLineWidth, font.getStringWidth(measuredLine));
                }

                float boxWidth = maxLineWidth + 12.0F;
                float boxHeight = lines.size() * 10.0F + 8.0F;
                float screenWidth = mc.getWindow().getGuiScaledWidth();
                float screenHeight = mc.getWindow().getGuiScaledHeight();
                float rawX = tooltipMouseX + 12.0F;
                float boxY = tooltipMouseY + 12.0F;
                if (rawX + boxWidth > screenWidth - 4.0F) {
                    rawX = tooltipMouseX - boxWidth - 12.0F;
                }

                if (boxY + boxHeight > screenHeight - 4.0F) {
                    boxY = tooltipMouseY - boxHeight - 12.0F;
                }

                float boxX = Math.clamp(rawX, 4.0F, Math.max(4.0F, screenWidth - boxWidth - 4.0F));
                boxY = Math.clamp(boxY, 4.0F, Math.max(4.0F, screenHeight - boxHeight - 4.0F));
                Color color = Astralis.getInstance().getPrimaryColor();

                Color color2 = new Color(14, 16, 21, 238);

                Color color3 = new Color(235, 237, 242);
                Astralis.getInstance()
                        .getRenderEffects()
                        .drawBlurredRoundedRectangle(boxX, boxY, boxWidth, boxHeight, 5.0F, 1.0F);
                RoundedRectShape borderShape =
                        RoundedRectShape.obtain()
                                .setBounds(boxX, boxY, boxWidth, boxHeight)
                                .setRadius(5.0F);
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                context.drawRect(borderShape.setColor(new Color(red, green, blue, 185)));
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        boxX + 1.0F, boxY + 1.0F, boxWidth - 2.0F, boxHeight - 2.0F)
                                .setRadius(4.0F)
                                .setColor(color2));
                float textY = boxY + 5.0F;

                for (String line : lines) {
                    context.drawText(
                            TextDrawRequest.obtain()
                                    .setText(line)
                                    .setPosition(boxX + 6.0F, textY)
                                    .setFont(font.getMsdfFont())
                                    .setFontSize(font.getSize())
                                    .enableShadow()
                                    .setColor(color3.getRGB()));
                    textY += 10.0F;
                }

                clearPendingTooltip();
            }
        }
    }
}
