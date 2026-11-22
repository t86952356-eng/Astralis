package astralis.module.impl.beddefender;

public record DefenseBlock(String blockType, int offsetX, int offsetY, int offsetZ) {

    public DefenseBlock(String blockType, int offsetX, int offsetY, int offsetZ) {
        this.blockType = blockType;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public String getBlockType() {
        return this.blockType;
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public int getOffsetZ() {
        return this.offsetZ;
    }
}
