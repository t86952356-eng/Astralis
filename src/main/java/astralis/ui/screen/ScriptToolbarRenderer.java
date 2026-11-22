package astralis.ui.screen;

import astralis.ui.component.FlatUiPalette;
import astralis.ui.component.FlatUiPaletteColors;
import astralis.ui.component.ScriptsToolbarAction;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

public final class ScriptToolbarRenderer implements FlatUiPalette {
    ScriptToolbarRenderer() {}

    void drawToolbar(
            Render2DContext context,
            float x,
            float y,
            float width,
            boolean easyMode,
            double mouseX,
            double mouseY) {
        Iterator buttonIterator = this.createButtons(x, y, width, easyMode).iterator();

        while (buttonIterator.hasNext()) {
            ScriptToolbarButton button = (ScriptToolbarButton) buttonIterator.next();
            drawButton(
                    context,
                    button.getLabel(),
                    button.getX(),
                    button.getY(),
                    button.getWidth(),
                    button.isPrimary(),
                    true,
                    mouseX,
                    mouseY);
        }
    }

    ScriptsToolbarAction getActionAt(
            double mouseX, double mouseY, float x, float y, float width, boolean easyMode) {
        Iterator buttonIterator = this.createButtons(x, y, width, easyMode).iterator();

        while (buttonIterator.hasNext()) {
            ScriptToolbarButton button = (ScriptToolbarButton) buttonIterator.next();
            if (isOverButton(mouseX, mouseY, button.getX(), button.getY(), button.getWidth())) {
                return button.getAction();
            }
        }

        return ScriptsToolbarAction.NONE;
    }

    private List<ScriptToolbarButton> createButtons(
            float x, float y, float width, boolean easyMode) {
        float rightEdge = x + width - 8.0F;
        if (easyMode) {

            ScriptToolbarButton newButton =
                    new ScriptToolbarButton(
                            "New",
                            rightEdge - 142.0F,
                            y + 3.0F,
                            42.0F,
                            ScriptsToolbarAction.NEW_EASY,
                            true);

            ScriptToolbarButton libraryButton =
                    new ScriptToolbarButton(
                            "Library",
                            rightEdge - 95.0F,
                            y + 3.0F,
                            52.0F,
                            ScriptsToolbarAction.LIST,
                            false);

            return List.of(
                    newButton,
                    libraryButton,
                    new ScriptToolbarButton(
                            "Back",
                            rightEdge - 38.0F,
                            y + 3.0F,
                            38.0F,
                            ScriptsToolbarAction.BACK,
                            false));
        } else {

            ScriptToolbarButton newEasyButton =
                    new ScriptToolbarButton(
                            "New Easy",
                            rightEdge - 231.0F,
                            y + 3.0F,
                            60.0F,
                            ScriptsToolbarAction.NEW_EASY,
                            true);

            ScriptToolbarButton folderButton =
                    new ScriptToolbarButton(
                            "Folder",
                            rightEdge - 166.0F,
                            y + 3.0F,
                            54.0F,
                            ScriptsToolbarAction.OPEN_FOLDER,
                            false);

            ScriptToolbarButton reloadButton =
                    new ScriptToolbarButton(
                            "Reload",
                            rightEdge - 107.0F,
                            y + 3.0F,
                            54.0F,
                            ScriptsToolbarAction.RELOAD,
                            false);

            return List.of(
                    newEasyButton,
                    folderButton,
                    reloadButton,
                    new ScriptToolbarButton(
                            "Back",
                            rightEdge - 48.0F,
                            y + 3.0F,
                            48.0F,
                            ScriptsToolbarAction.BACK,
                            false));
        }
    }

    static void drawButton(
            Render2DContext context,
            String label,
            float x,
            float y,
            float width,
            boolean primary,
            boolean enabled,
            double mouseX,
            double mouseY) {

        SizedFont labelFont = FontManager.getFont("Product Sans Regular", 7.0F);
        boolean hovered = enabled && isOverButton(mouseX, mouseY, x, y, width);
        if (primary && enabled) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(x, y, width, 22.0F)
                            .setRadius(5.0F)
                            .setPaint(
                                    ShapePaint.obtain()
                                            .setVerticalGradient(
                                                    ColorUtil.animatedThemeColor(y).getRGB(),
                                                    ColorUtil.animatedThemeColor(y + 22.0F)
                                                            .getRGB())));
        } else {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(x, y, width, 22.0F)
                            .setRadius(5.0F)
                            .setColor(
                                    hovered
                                            ? FlatUiPaletteColors.CONTROL_ACTIVE
                                            : FlatUiPaletteColors.CONTROL));
        }

        if (primary && hovered) {
            RoundedRectShape highlightShape =
                    RoundedRectShape.obtain().setBounds(x, y, width, 22.0F).setRadius(5.0F);

            context.drawRect(highlightShape.setColor(new Color(255, 255, 255, 18)));
        }

        context.drawText(
                TextDrawRequest.obtain()
                        .setText(label)
                        .setPosition(x, y)
                        .centerInBox(width, 22.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .setColor(
                                !enabled
                                        ? FlatUiPaletteColors.TEXT_MUTED.getRGB()
                                        : (!primary && !hovered
                                                ? FlatUiPaletteColors.TEXT_SECONDARY.getRGB()
                                                : FlatUiPaletteColors.TEXT.getRGB())));
    }

    private static boolean isOverButton(
            double mouseX, double mouseY, float x, float y, float width) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 22.0F;
    }
}
