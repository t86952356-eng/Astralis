package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.Module;
import astralis.util.ChatUtil;
import astralis.util.PlayerListManager;

import net.minecraft.world.entity.player.Player;

import pub.astralis.client.module.core.Category;

public class MiddleClick extends Module {
    private boolean wasPressed;

    public MiddleClick() {

        super(MODULE_ID, "", "", Category.Misc);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        boolean pressed = mc.options.keyPickItem.isDown();
        if (pressed && !this.wasPressed) {
            this.toggleFriend();
        }

        this.wasPressed = pressed;
    }

    private void toggleFriend() {
        if (mc.crosshairPickEntity instanceof Player player2) {
            PlayerListManager playerList =
                    Astralis.getInstance().getManagerRegistry().getManager(PlayerListManager.class);
            if (playerList != null) {
                if (playerList.isFriend(player2)) {
                    playerList.removeFriend(player2);
                    String playerId = String.valueOf(player2.getGameProfile().id());

                    ChatUtil.print("Unfriended: " + playerId);
                } else {
                    playerList.addFriend(player2);
                    String playerId = String.valueOf(player2.getGameProfile().id());

                    ChatUtil.print("Friended: " + playerId);
                }
            }
        }
    }

    private static final String MODULE_ID = "78ec186fd3025c24ce0237cd4295c98b";
}
