package astralis.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record MiniBloxMovePayload(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float forward,
        float strafe,
        boolean jump,
        boolean sneak,
        boolean onGround,
        boolean sprinting)
        implements CustomPacketPayload {
    public static Type<MiniBloxMovePayload> payloadType =
            new Type<>(Identifier.fromNamespaceAndPath("miniblox", "movepacket"));
    public static StreamCodec<RegistryFriendlyByteBuf, MiniBloxMovePayload> streamCodec =
            CustomPacketPayload.codec(MiniBloxMovePayload::writeToBuffer, MiniBloxMovePayload::new);

    public MiniBloxMovePayload(RegistryFriendlyByteBuf buffer) {
        this(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    public MiniBloxMovePayload(
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            float forward,
            float strafe,
            boolean jump,
            boolean sneak,
            boolean onGround,
            boolean sprinting) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.onGround = onGround;
        this.sprinting = sprinting;
    }

    @Override
    public Type<MiniBloxMovePayload> type() {
        return payloadType;
    }

    private void writeToBuffer(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        registryFriendlyByteBuf.writeDouble(this.x);
        registryFriendlyByteBuf.writeDouble(this.y);
        registryFriendlyByteBuf.writeDouble(this.z);
        registryFriendlyByteBuf.writeFloat(this.yaw);
        registryFriendlyByteBuf.writeFloat(this.pitch);
        registryFriendlyByteBuf.writeFloat(this.forward);
        registryFriendlyByteBuf.writeFloat(this.strafe);
        registryFriendlyByteBuf.writeBoolean(this.jump);
        registryFriendlyByteBuf.writeBoolean(this.sneak);
        registryFriendlyByteBuf.writeBoolean(this.onGround);
        registryFriendlyByteBuf.writeBoolean(this.sprinting);
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public double getY() {
        return this.y;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public float getForward() {
        return this.forward;
    }

    public float getYaw() {
        return this.yaw;
    }

    public boolean isSneaking() {
        return this.sneak;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public double getX() {
        return this.x;
    }

    public boolean isJumping() {
        return this.jump;
    }
}
