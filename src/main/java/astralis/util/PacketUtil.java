package astralis.util;

import astralis.integration.mixin.ConnectionExtension;
import astralis.mixin.MultiPlayerGameModeAccessor;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;

public class PacketUtil implements InstanceAccess {

    public static void receiveNoEvent(Packet<?> packet) {
        ConnectionExtension connection = (ConnectionExtension) mc.player.connection.getConnection();
        connection.astralis$receiveNoEvent(packet);
    }

    public static void sendSilent(Packet<?> packet) {
        ConnectionExtension connection = (ConnectionExtension) mc.player.connection.getConnection();
        connection.astralis$sendSilent(packet);
    }

    public static void send(Packet<?> packet) {
        ConnectionExtension connection = (ConnectionExtension) mc.player.connection.getConnection();
        connection.astralis$sendWithEvent(packet);
    }

    public static void sendPrediction(PredictiveAction action) {
        ((MultiPlayerGameModeAccessor) mc.gameMode).callStartPrediction(mc.level, action);
    }

    public static void handle(Packet<?> packet) {
        try {
            ((Packet) packet).handle(mc.getConnection());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
