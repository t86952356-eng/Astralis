package astralis.mixin;

import astralis.event.EventStage;
import astralis.integration.mixin.ConnectionExtension;
import astralis.network.PacketEventDispatcher;

import com.astralis.scripting.InstanceAccess;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.RunningOnDifferentThreadException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.RejectedExecutionException;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements ConnectionExtension, InstanceAccess {
    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("MixinConnection");
    @Shadow private Channel channel;
    @Shadow private PacketListener packetListener;

    @Shadow
    public abstract void send(Packet packet, @Nullable ChannelFutureListener listener);

    @Shadow
    private static void genericsFtw(Packet packet, PacketListener packetListener2) {}

    @Shadow
    public abstract void disconnect(Component component);

    @Overwrite
    public void send(Packet packet) {
        if (this.packetListener instanceof ClientGamePacketListener) {
            packet = PacketEventDispatcher.filterPacket(packet, EventStage.SEND);
        }

        if (packet != null) {
            this.send(packet, null);
        }
    }

    @Override
    public void astralis$sendSilent(Packet packet) {
        this.send(packet, null);
    }

    @Override
    public void astralis$sendWithEvent(Packet packet) {
        this.send(packet);
    }

    @Override
    public void astralis$receiveNoEvent(Packet packet) {
        if (this.channel.isOpen()) {
            try {
                genericsFtw(packet, this.packetListener);
            } catch (RunningOnDifferentThreadException runningOnDifferentThreadException) {
            } catch (RejectedExecutionException ignored) {
                this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
            } catch (ClassCastException e) {
                LOGGER.error("Received {} that couldn't be processed", packet.getClass(), e);
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
            }
        }
    }

    @Overwrite
    public void channelRead0(ChannelHandlerContext context, Packet packet) {
        if (this.packetListener instanceof ClientGamePacketListener && !mc.isSameThread()) {
            mc.execute(() -> this.processReceivedPacket(packet));
        } else {
            this.processReceivedPacket(packet);
        }
    }

    @Unique
    private void processReceivedPacket(Packet packet) {
        if (this.channel.isOpen()) {
            try {
                if (this.packetListener instanceof ClientGamePacketListener) {
                    packet = PacketEventDispatcher.filterPacket(packet, EventStage.RECEIVE);
                }

                if (packet == null) {
                    return;
                }

                genericsFtw(packet, this.packetListener);
            } catch (RunningOnDifferentThreadException runningOnDifferentThreadException) {
            } catch (RejectedExecutionException ignored) {
                this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
            } catch (ClassCastException e) {
                LOGGER.error("Received {} that couldn't be processed", packet.getClass(), e);
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
            }
        }
    }
}
