package astralis.command.impl;

public record PlayerPose(double x, double y, double z, float yaw) {
    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public double getY() {
        return this.y;
    }

    public double getX() {
        return this.x;
    }
}
