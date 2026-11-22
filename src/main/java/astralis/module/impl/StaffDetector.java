package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.metadata.MetadataState;
import astralis.mixin.ClientboundMoveEntityPacketAccessor;
import astralis.module.Module;
import astralis.network.hypixel.HypixelLocation;
import astralis.util.ChatUtil;

import net.hypixel.data.type.GameType;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket.Action;

import org.apache.commons.lang3.StringUtils;

import pub.astralis.client.module.core.Category;

public class StaffDetector extends Module {
    public StaffDetector() {

        super(MODULE_ID, "", "", Category.Misc);
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.RECEIVE) {
            if (packetEvent.getPacket()
                            instanceof ClientboundSetPlayerTeamPacket clientboundSetPlayerTeamPacket
                    && this.isStaffTeamName(clientboundSetPlayerTeamPacket.getName())
                    && clientboundSetPlayerTeamPacket.getTeamAction() == Action.ADD) {
                for (int i = 0; i < 5; i++) {
                    String teamName = clientboundSetPlayerTeamPacket.getName();

                    ChatUtil.print("A staff member has joined your game. " + teamName);
                }
            }

            if (packetEvent.getPacket()
                    instanceof ClientboundMoveEntityPacket clientboundMoveEntityPacket) {
                int entityId =
                        ((ClientboundMoveEntityPacketAccessor) clientboundMoveEntityPacket)
                                .getEntityId();
                if (mc.level != null && mc.level.getEntity(entityId) == null) {
                    double deltaX = clientboundMoveEntityPacket.getXa() / 4096.0;
                    double deltaY = clientboundMoveEntityPacket.getYa() / 4096.0;
                    double deltaZ = clientboundMoveEntityPacket.getZa() / 4096.0;

                    System.out.printf(
                            "§c[Staff Detect] §fGhost Entity (ID: %d) moved: [%.2f, %.2f, %.2f]%n",
                            entityId, deltaX, deltaY, deltaZ);
                }
            }
        }
    }

    public boolean isStaffTeamName(String teamName) {
        MetadataState metadata = MetadataState.getInstance();

        String staffRegex = metadata.getParameter("staff_team_regex");
        if (staffRegex == null) {
            return false;
        }

        if (!teamName.matches(staffRegex)) {

            if (!teamName.equals(metadata.getParameter("staff_team_exact"))) {

                String staffPrefixes = metadata.getParameter("staff_team_prefix");
                if (staffPrefixes != null) {
                    for (String prefix : staffPrefixes.split(",")) {
                        if (!prefix.isEmpty() && teamName.startsWith(prefix)) {
                            return true;
                        }
                    }
                }

                String numericSuffix = metadata.getParameter("staff_team_numeric_suffix");
                if (numericSuffix == null) {
                    return false;
                }

                HypixelLocation location = Astralis.getInstance().getHypixelApi().getLocation();
                if (teamName.contains("§")) {
                    String plainName = teamName.split("§")[0];

                    if (plainName.endsWith("00")) {
                        return true;
                    }
                }

                if (!StringUtils.isNumeric(teamName)
                        || !teamName.endsWith(numericSuffix)
                        || location == null
                        || location.getServerType() == GameType.PIT) {
                    return false;
                }

                return true;
            }
        }

        return true;
    }

    private static final String MODULE_ID = "32522bf27fc184b85bf580ceb0cb7b5c";
}
