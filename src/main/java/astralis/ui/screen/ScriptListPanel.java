package astralis.ui.screen;

import astralis.Astralis;
import astralis.scripting.ScriptSource;
import astralis.ui.component.FlatUiPalette;
import astralis.ui.component.FlatUiPaletteColors;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import java.util.List;

public final class ScriptListPanel implements FlatUiPalette {
    private ScriptListLayout layout = new ScriptListLayout(0.0F, 0.0F, 0.0F, 0.0F, false);
    private float scrollOffset;
    private float thumbGrabOffset;

    ScriptListPanel() {}

    void setLayout(float x, float y, float width, float height, boolean compact) {
        this.layout =
                new ScriptListLayout(x, y, Math.max(0.0F, width), Math.max(0.0F, height), compact);
    }

    void drawList(
            Render2DContext context,
            List<ScriptSource> scripts,
            String selectedScriptName,
            boolean hasSearchQuery,
            double mouseX,
            double mouseY) {

        if (!(this.layout.getWidth() <= 0.0F) && !(this.layout.getHeight() <= 0.0F)) {

            SizedFont nameFont = FontManager.getFont("Product Sans Bold", 9.0F);

            SizedFont detailFont = FontManager.getFont("Product Sans Regular", 7.0F);
            this.clampScroll(scripts.size());
            context.beginScissor(
                    this.layout.x(),
                    this.layout.getY(),
                    this.layout.getWidth(),
                    this.layout.getHeight());
            if (scripts.isEmpty()) {
                TextDrawRequest emptyTitleRequest = TextDrawRequest.obtain();
                String emptyText;
                if (hasSearchQuery) {

                    emptyText = "No matching scripts";
                } else {

                    emptyText = "No scripts installed";
                }

                context.drawText(
                        emptyTitleRequest
                                .setText(
                                        nameFont.truncateWithEllipsis(
                                                emptyText, this.layout.getWidth() - 16.0F))
                                .setPosition(this.layout.x() + 8.0F, this.layout.getY() + 14.0F)
                                .setFont(nameFont.getMsdfFont())
                                .setFontSize(nameFont.getSize())
                                .setColor(FlatUiPaletteColors.TEXT.getRGB()));
                TextDrawRequest emptyHintRequest = TextDrawRequest.obtain();
                if (hasSearchQuery) {

                    emptyText = "Try another name or clear the search.";
                } else {

                    emptyText = "Create one with New Easy, or add a source in the scripts folder.";
                }

                context.drawText(
                        emptyHintRequest
                                .setText(
                                        detailFont.truncateWithEllipsis(
                                                emptyText, this.layout.getWidth() - 16.0F))
                                .setPosition(this.layout.x() + 8.0F, this.layout.getY() + 31.0F)
                                .setFont(detailFont.getMsdfFont())
                                .setFontSize(detailFont.getSize())
                                .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
            }

            int hoveredIndex =
                    this.layout.getRowIndexAt(mouseX, mouseY, this.scrollOffset, scripts.size());

            for (int index = Math.max(0, (int) (this.scrollOffset / this.layout.getRowStride()));
                    index < scripts.size();
                    index++) {
                float rowY = this.layout.getRowY(index, this.scrollOffset);
                if (rowY >= this.layout.getY() + this.layout.getHeight()) {
                    break;
                }

                ScriptSource script = (ScriptSource) scripts.get(index);
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.layout.x(),
                                        rowY,
                                        this.layout.getRowWidth(),
                                        this.layout.getRowHeight())
                                .setRadius(7.0F)
                                .setColor(
                                        script.name().equals(selectedScriptName)
                                                ? FlatUiPaletteColors.CONTROL_ACTIVE
                                                : (index == hoveredIndex
                                                        ? FlatUiPaletteColors.CONTROL_HOVER
                                                        : FlatUiPaletteColors.CONTROL)));
                float nameMaxWidth =
                        this.layout.getRowWidth() - (this.layout.isCompact() ? 20.0F : 78.0F);
                context.drawText(
                        TextDrawRequest.obtain()
                                .setText(nameFont.truncateWithEllipsis(script.name(), nameMaxWidth))
                                .setPosition(
                                        this.layout.x() + 10.0F,
                                        rowY + (this.layout.isCompact() ? 4.0F : 8.0F))
                                .setFont(nameFont.getMsdfFont())
                                .setFontSize(nameFont.getSize())
                                .setColor(FlatUiPaletteColors.TEXT.getRGB()));
                TextDrawRequest statusRequest = TextDrawRequest.obtain();
                String statusText;
                if (script.enabled()) {

                    statusText = "Enabled";
                } else if (script.loaded()) {

                    statusText = "Loaded";
                } else {

                    statusText = "Not loaded";
                }

                context.drawText(
                        statusRequest
                                .setText(statusText)
                                .setPosition(
                                        this.layout.x() + 10.0F,
                                        rowY + (this.layout.isCompact() ? 18.0F : 24.0F))
                                .setFont(detailFont.getMsdfFont())
                                .setFontSize(detailFont.getSize())
                                .setColor(
                                        script.enabled()
                                                ? FlatUiPaletteColors.SUCCESS.getRGB()
                                                : FlatUiPaletteColors.TEXT_MUTED.getRGB()));
                if (!this.layout.isCompact()) {
                    ScriptToolbarRenderer.drawButton(
                            context,
                            "View",
                            this.layout.x() + this.layout.getRowWidth() - 52.0F,
                            rowY + 10.0F,
                            42.0F,
                            false,
                            true,
                            mouseX,
                            mouseY);
                }
            }

            context.endScissor();
            if (!(this.layout.getMaxScroll(scripts.size()) <= 0.0F)) {
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.layout.x() + this.layout.getWidth() - 3.0F,
                                        this.layout.getThumbY(scripts.size(), this.scrollOffset),
                                        2.0F,
                                        this.layout.getThumbHeight(scripts.size()))
                                .setRadius(1.0F)
                                .setColor(Astralis.getInstance().getPrimaryColor()));
            }
        }
    }

    String getScriptNameAt(double mouseX, double mouseY, List<ScriptSource> scripts) {
        int rowIndex = this.layout.getRowIndexAt(mouseX, mouseY, this.scrollOffset, scripts.size());
        return rowIndex < 0 ? null : ((ScriptSource) scripts.get(rowIndex)).name();
    }

    boolean beginScrollbarDrag(double mouseX, double mouseY, int rowCount) {
        if (!(this.layout.getMaxScroll(rowCount) <= 0.0F)
                && this.layout.containsPoint(mouseX, mouseY)
                && !(mouseX < this.layout.x() + this.layout.getWidth() - 8.0F)) {
            float thumbY = this.layout.getThumbY(rowCount, this.scrollOffset);
            float thumbHeight = this.layout.getThumbHeight(rowCount);
            this.thumbGrabOffset =
                    mouseY >= thumbY && mouseY < thumbY + thumbHeight
                            ? (float) mouseY - thumbY
                            : thumbHeight / 2.0F;
            this.dragScrollbar(mouseY, rowCount);
            return true;
        } else {
            return false;
        }
    }

    void dragScrollbar(double mouseY, int rowCount) {
        this.scrollOffset =
                this.layout.getScrollForThumbDrag(mouseY, this.thumbGrabOffset, rowCount);
    }

    boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta, int rowCount) {
        if (!this.layout.containsPoint(mouseX, mouseY)) {
            return false;
        }

        this.scrollOffset =
                Math.clamp(
                        this.scrollOffset - (float) scrollDelta * this.layout.getRowStride(),
                        0.0F,
                        this.layout.getMaxScroll(rowCount));
        return true;
    }

    void clampScroll(int rowCount) {
        this.scrollOffset = Math.clamp(this.scrollOffset, 0.0F, this.layout.getMaxScroll(rowCount));
    }
}
