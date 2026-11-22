package astralis.manager;

import astralis.util.Listener;

/** Registry of shared listener managers initialized during client startup. */
public class ManagerRegistry extends ItemRegistry<Listener> {
    private boolean initialized;

    /** Looks up the registered manager assignable to the requested type. */
    public <T extends Listener> T getManager(Class<T> type) {
        return getByClass(type);
    }

    /** Registers each built-in listener manager once, preserving startup order. */
    public void registerBuiltInManagers() {
        if (initialized) return;
        initialized = true;
        register(new astralis.util.Blink());
        register(new astralis.util.PacketDelay());
        register(new astralis.util.PlayerListManager());
        register(new astralis.ui.hud.PartyHud());
        register(new astralis.rotation.RotationManager());
        register(new astralis.util.OwnBedTracker());
        register(new astralis.manager.AttackManager());
        register(new astralis.util.inventory.DelayedSlotRestorer());
        register(new astralis.network.proxy.MiniBloxMoveSync());
        register(new astralis.ui.inventory.SlotHighlightManager());
        register(new astralis.ui.chat.ChatTabManager());
        register(new astralis.ui.notification.NotificationManager());
    }
}
