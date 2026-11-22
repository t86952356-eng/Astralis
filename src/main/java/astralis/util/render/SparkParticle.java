package astralis.util.render;

public final class SparkParticle {
    public double x;
    public double y;
    public double z;
    public double velocityX;
    public double velocityY;
    public double velocityZ;
    public float remainingLife;

    public SparkParticle(
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            float life) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.remainingLife = life;
    }
}
