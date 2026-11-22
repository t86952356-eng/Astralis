package astralis.util;

import astralis.network.hypixel.HypixelLocation;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.fabric.FabricModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

public class HypixelModApi {
    private HypixelLocation location;
    private HypixelLocation previousLocation;
    private boolean initialized;
    private volatile boolean connected;
    private volatile boolean inParty;

    public HypixelModApi() {

        try {
            if (!FabricLoader.getInstance().isModLoaded("hypixel-mod-api")) {
                new FabricModAPI().onInitializeClient();
            }

            HypixelModAPI api = HypixelModAPI.getInstance();
            api.subscribeToEventPacket(ClientboundLocationPacket.class);
            api.createHandler(ClientboundLocationPacket.class, this::onLocationPacket);
            api.createHandler(
                    ClientboundHelloPacket.class,
                    packet -> {
                        this.connected = true;
                        this.requestPartyInfo();
                    });
            api.createHandler(
                            ClientboundPartyInfoPacket.class,
                            packet -> this.inParty = packet.isInParty())
                    .onError(reason -> this.inParty = false);
            ClientPlayConnectionEvents.DISCONNECT.register(
                    (clientPacketListener, minecraft2) -> {
                        this.connected = false;
                        this.inParty = false;
                        this.location = null;
                    });
            this.initialized = true;
        } catch (Throwable ignored) {
            this.initialized = false;
        }
    }

    public void requestPartyInfo() {

        try {
            this.connected =
                    HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
            if (!this.connected) {
                this.inParty = false;
            }
        } catch (Throwable ignored) {
            this.connected = false;
            this.inParty = false;
        }
    }

    public HypixelLocation getLocation() {
        return this.location;
    }

    public HypixelLocation getPreviousLocation() {
        return this.previousLocation;
    }

    public boolean isInParty() {
        return this.inParty;
    }

    public void setLocation(HypixelLocation location) {
        this.location = location;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isConnected() {
        return this.connected;
    }

    private void onLocationPacket(ClientboundLocationPacket packet) {
        this.previousLocation = this.location;
        this.location =
                new HypixelLocation(
                        packet.getServerName(),
                        packet.getServerType().orElse(null),
                        packet.getLobbyName().orElse(null),
                        packet.getMode().orElse(null),
                        packet.getMap().orElse(null));
    }
}
