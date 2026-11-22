package astralis.module.impl.anticheat.check;

import astralis.Astralis;
import astralis.setting.BooleanSetting;
import astralis.ui.notification.Notification;
import astralis.ui.notification.NotificationBuilder;
import astralis.ui.notification.NotificationType;
import astralis.util.ChatUtil;
import astralis.util.PlayerListManager;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Rate-limits flags per player/check, reports them, and applies configured player-list actions. */
public class FlagReporter implements InstanceAccess {
    private static final long FLAG_COOLDOWN_MILLIS = 4000L;
    private final BooleanSetting showToasts;
    private final BooleanSetting autoMarkHacker;
    private final BooleanSetting autoTarget;
    private final Set<UUID> markedPlayerIds = ConcurrentHashMap.newKeySet();

    public FlagReporter(
            BooleanSetting toastSetting,
            BooleanSetting markHackerSetting,
            BooleanSetting targetSetting) {
        this.showToasts = toastSetting;
        this.autoMarkHacker = markHackerSetting;
        this.autoTarget = targetSetting;
    }

    /** Suppresses repeated flags for four seconds before updating counts or performing actions. */
    public void reportFlag(
            Player player, PlayerCheckState state, CheckType checkType, String details) {
        long elapsedSincePreviousFlag;

        long now = System.currentTimeMillis();
        Long previousFlagTime = state.lastFlagTimes.get(checkType);
        if (previousFlagTime != null) {
            elapsedSincePreviousFlag = now - previousFlagTime;

            if (elapsedSincePreviousFlag < 4000L) {
                return;
            }
        }

        state.lastFlagTimes.put(checkType, now);
        int violationLevel = state.flagCounts.merge(checkType, 1, Integer::sum);
        String playerName = player.getName().getString();

        MutableComponent messagePrefix =
                Component.literal("[AntiCheat] ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(playerName).withStyle(ChatFormatting.WHITE));

        MutableComponent flagMessage =
                messagePrefix
                        .append(Component.literal(" flagged ").withStyle(ChatFormatting.GRAY))
                        .append(
                                Component.literal(checkType.getDisplayName())
                                        .withStyle(ChatFormatting.YELLOW));

        ChatUtil.print(
                flagMessage.append(
                        Component.literal(" (" + details + ") vl" + violationLevel)
                                .withStyle(ChatFormatting.DARK_GRAY)));
        if (this.showToasts.getValue()) {
            NotificationBuilder notificationManager = Notification.builder();

            NotificationBuilder notification = notificationManager.withTitle("AntiCheat");
            String checkName = checkType.getDisplayName();

            NotificationBuilder warningNotification =
                    notification
                            .withMessage(playerName + " - " + checkName)
                            .withType(NotificationType.WARNING);

            warningNotification.withDuration(2500L).show();
        }

        if (player != mc.player && (this.autoMarkHacker.getValue() || this.autoTarget.getValue())) {
            PlayerListManager playerLists =
                    Astralis.getInstance().getManagerRegistry().getManager(PlayerListManager.class);
            // Marking also adds the player to the enemy list; keep the separate auto-target action.
            if (this.autoMarkHacker.getValue()) {
                this.markedPlayerIds.add(player.getUUID());
                playerLists.addHacker(player.getUUID());
                playerLists.addEnemy(player);
            }

            if (this.autoTarget.getValue()) {
                playerLists.addEnemy(player);
            }
        }
    }

    /** Returns an immutable snapshot of players marked by this reporter. */
    public Set<UUID> getMarkedPlayerIds() {
        return Set.copyOf(this.markedPlayerIds);
    }
}
