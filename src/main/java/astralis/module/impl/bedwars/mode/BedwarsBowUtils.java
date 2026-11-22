package astralis.module.impl.bedwars.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ChatReceiveEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.Bedwars;
import astralis.setting.Mode;
import astralis.util.ChatUtil;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarsBowUtils extends Mode<Bedwars> {
    private static Pattern healthMessagePattern;
    private double shotX;
    private double shotY;
    private double shotZ;
    private long shotTime;
    private boolean wasUsingBow;

    public BedwarsBowUtils(Bedwars bedwars) {

        super("Bow Utils", bedwars);
    }

    private boolean matchesPlayerName(Player player2, String playerName) {
        if (player2.getGameProfile().name().equalsIgnoreCase(playerName)) {
            return true;
        } else {
            String normalizedDisplayName =
                    ChatTextUtil.normalizeText(player2.getDisplayName().getString());
            if (namesMatch(normalizedDisplayName, playerName)) {
                return true;
            } else if (mc.getConnection() == null) {
                return false;
            } else {
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player2.getUUID());
                if (playerInfo == null) {
                    return false;
                } else {
                    return playerInfo.getProfile().name().equalsIgnoreCase(playerName)
                            ? true
                            : playerInfo.getTabListDisplayName() != null
                                    && namesMatch(
                                            ChatTextUtil.normalizeText(
                                                    playerInfo.getTabListDisplayName().getString()),
                                            playerName);
                }
            }
        }
    }

    private static boolean isBow(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getItem() instanceof BowItem;
    }

    @EventTarget
    public void onChatReceive(ChatReceiveEvent event) {
        if (!event.isOverlay() && this.shotTime != 0L) {
            if (System.currentTimeMillis() - this.shotTime > 15000L) {
                this.shotTime = 0L;
            } else {
                Matcher matcher =
                        healthMessagePattern.matcher(
                                ChatTextUtil.normalizeText(event.getMessage().getString()));
                if (matcher.find() && mc.level != null) {
                    String victimName = matcher.group(1);
                    Player player2 = null;

                    for (Player player3 : mc.level.players()) {
                        if (this.matchesPlayerName(player3, victimName)) {
                            player2 = player3;
                            break;
                        }
                    }

                    if (player2 != null) {
                        double deltaX = this.shotX - player2.getX();
                        double deltaY = this.shotY - player2.getY();
                        double deltaZ = this.shotZ - player2.getZ();

                        ChatUtil.print(
                                String.format(
                                        Locale.ROOT,
                                        " (%.1f blocks)",
                                        Math.sqrt(
                                                deltaX * deltaX
                                                        + deltaY * deltaY
                                                        + deltaZ * deltaZ)));
                        this.shotTime = 0L;
                    }
                }
            }
        }
    }

    private void recordShot() {
        this.shotX = mc.player.getX();
        this.shotY = mc.player.getY();
        this.shotZ = mc.player.getZ();
        this.shotTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.resetShot();
    }

    private static boolean namesMatch(String fullName, String name) {

        if (fullName.equalsIgnoreCase(name)) {
            return true;
        }

        int lastSpaceIndex = fullName.lastIndexOf(32);
        return lastSpaceIndex >= 0 && fullName.substring(lastSpaceIndex + 1).equalsIgnoreCase(name);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST && mc.player != null) {
            boolean usingBow = isBow(mc.player.getUseItem());
            if (this.wasUsingBow && !usingBow && Bedwars.isInBedwarsGame()) {
                this.recordShot();
            }

            this.wasUsingBow = usingBow;
        }
    }

    private void resetShot() {
        this.shotTime = 0L;
        this.wasUsingBow = false;
    }

    @Override
    public void onDisable() {
        this.resetShot();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.SEND
                && Bedwars.isInBedwarsGame()
                && mc.player != null) {
            if (packetEvent.getPacket()
                            instanceof ServerboundPlayerActionPacket serverboundPlayerActionPacket
                    && serverboundPlayerActionPacket.getAction() == Action.RELEASE_USE_ITEM) {
                if (isBow(mc.player.getUseItem())
                        || isBow(mc.player.getMainHandItem())
                        || isBow(mc.player.getOffhandItem())) {
                    this.recordShot();
                }
            }
        }
    }

    static {
        healthMessagePattern =
                Pattern.compile("([A-Za-z0-9_]{1,16}) is on \\d+(?:\\.\\d+)? HP!", 2);
    }
}
