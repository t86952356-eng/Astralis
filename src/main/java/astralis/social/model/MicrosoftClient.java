package astralis.social.model;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public record MicrosoftClient(String clientId, String displayName, ChatFormatting color) {
    private static final List<MicrosoftClient> ALL_CLIENTS = new ArrayList<>();

    public MicrosoftClient(String clientId, String displayName, ChatFormatting color) {
        this.clientId = clientId;
        this.displayName = displayName;
        this.color = color;
        ALL_CLIENTS.add(this);
    }

    public static List<MicrosoftClient> getAllClients() {
        return ALL_CLIENTS;
    }

    public static MicrosoftClient getByName(String name) {
        return ALL_CLIENTS.stream()
                .filter(client -> client.displayName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public ChatFormatting getColor() {
        return this.color;
    }
}
