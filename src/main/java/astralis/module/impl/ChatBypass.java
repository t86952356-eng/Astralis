package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.metadata.MetadataState;
import astralis.module.Module;
import astralis.setting.ModeSetting;

import net.minecraft.network.protocol.game.ServerboundChatPacket;

import pub.astralis.client.module.core.Category;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ChatBypass extends Module {
    private final ModeSetting mode;
    private String lastBypassedMessage;

    public ChatBypass() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modeNames = new String[3];

        modeNames[0] = "Accent";

        modeNames[1] = "Dot";

        modeNames[2] = "Underscore";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.mode = modeSetting2.setDefault("Accent");
        this.lastBypassedMessage = null;
        this.addSetting(this.mode);
    }

    private static String applyAccentReplacements(String message) {
        MetadataState metadata = MetadataState.getInstance();

        String rules = metadata.getParameter("chat_bypass_accent");
        if (rules != null && !rules.isEmpty()) {
            for (String rule : rules.split(";")) {
                int separatorIndex = rule.indexOf(61);
                if (separatorIndex > 0 && separatorIndex != rule.length() - 1) {
                    message =
                            message.replace(
                                    rule.substring(0, separatorIndex),
                                    rule.substring(separatorIndex + 1));
                }
            }

            return message;
        } else {
            return message;
        }
    }

    private String intersperse(String text, char separator) {
        return Arrays.stream(text.split(" ", -1))
                .map(word -> String.join(String.valueOf(separator), word.split("")))
                .collect(Collectors.joining(" "));
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getPacket() instanceof ServerboundChatPacket serverboundChatPacket) {
            if (serverboundChatPacket.message().equals(this.lastBypassedMessage)) {
                this.lastBypassedMessage = null;
            } else {
                packetEvent.setCancelled(true);

                String bypassed =
                        switch (this.mode.getValue().getName()) {
                            case "Dot" -> {
                                String original = serverboundChatPacket.message();

                                yield this.intersperse(original, '.');
                            }
                            case "Underscore" -> {
                                String original = serverboundChatPacket.message();

                                yield this.intersperse(original, '_');
                            }
                            default -> applyAccentReplacements(serverboundChatPacket.message());
                        };
                this.lastBypassedMessage = bypassed;
                mc.player.connection.sendChat(bypassed);
            }
        }
    }

    private static final String MODULE_ID = "9a57e26fa9870828d9410695b4a6438a";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
