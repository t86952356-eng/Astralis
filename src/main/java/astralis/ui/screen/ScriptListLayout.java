package astralis.ui.screen;

public record ScriptListLayout(float x, float y, float width, float height, boolean compact) {

    float getRowHeight() {
        return this.compact ? 32.0F : 42.0F;
    }

    float getRowStride() {
        return this.getRowHeight() + 5.0F;
    }

    float getRowWidth() {
        return Math.max(0.0F, this.width - 8.0F);
    }

    float getRowY(int rowIndex, float scroll) {
        return this.y + rowIndex * this.getRowStride() - scroll;
    }

    float getMaxScroll(int rowCount) {
        return Math.max(0.0F, rowCount * this.getRowStride() - 5.0F - this.height);
    }

    boolean containsPoint(double mouseX, double mouseY) {
        return mouseX >= this.x
                && mouseX < this.x + this.width
                && mouseY >= this.y
                && mouseY < this.y + this.height;
    }

    int getRowIndexAt(double mouseX, double mouseY, float scroll, int rowCount) {
        if (this.containsPoint(mouseX, mouseY) && !(mouseX >= this.x + this.getRowWidth())) {
            double contentY = mouseY - this.y + scroll;
            int rowIndex = (int) Math.floor(contentY / this.getRowStride());
            return rowIndex >= 0
                            && rowIndex < rowCount
                            && contentY - rowIndex * this.getRowStride() < this.getRowHeight()
                    ? rowIndex
                    : -1;
        } else {
            return -1;
        }
    }

    float getThumbHeight(int rowCount) {
        return this.height <= 0.0F
                ? 0.0F
                : Math.min(
                        this.height,
                        Math.max(
                                20.0F,
                                this.height
                                        * this.height
                                        / Math.max(
                                                this.height,
                                                rowCount * this.getRowStride() - 5.0F)));
    }

    float getThumbY(int rowCount, float scroll) {
        float maxScroll = this.getMaxScroll(rowCount);
        return this.y
                + (maxScroll == 0.0F
                        ? 0.0F
                        : scroll / maxScroll * (this.height - this.getThumbHeight(rowCount)));
    }

    float getScrollForThumbDrag(double mouseY, float grabOffset, int rowCount) {
        float trackLength = this.height - this.getThumbHeight(rowCount);
        return trackLength <= 0.0F
                ? 0.0F
                : Math.clamp(((float) mouseY - this.y - grabOffset) / trackLength, 0.0F, 1.0F)
                        * this.getMaxScroll(rowCount);
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public boolean isCompact() {
        return this.compact;
    }
}
