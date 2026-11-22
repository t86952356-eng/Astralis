package astralis.ui.notification;

import lombok.Generated;

import java.awt.Color;

public enum NotificationType {
    INFO(
            (char) ((int) -2290198096916774813L),
            new Color(
                    (int) 2832844408490033264L,
                    (int) -4203285979206778690L,
                    (int) -1186517700868833025L)),
    WARNING(
            (char) ((int) 4788635917948551268L),
            new Color(
                    (int) 779789271509762303L,
                    (int) -294591450350354248L,
                    (int) 6705102599470710876L));

    private final char icon;
    private final Color accentColor;

    NotificationType(char icon, Color color) {
        this.icon = icon;
        this.accentColor = color;
    }

    @Generated
    public char getIcon() {
        return this.icon;
    }

    @Generated
    public Color getAccentColor() {
        return this.accentColor;
    }
}
