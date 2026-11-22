package astralis.ui.notification;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.Theme;
import astralis.util.Listener;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import java.awt.Color;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NotificationManager extends Listener {
    private final CopyOnWriteArrayList<NotificationEntry> entries = new CopyOnWriteArrayList<>();
    private long lastFrameNanos;

    public void addNotification(Notification notification) {
        this.entries.add(new NotificationEntry(notification));
    }

    @EventTarget
    public void onSapphireRender2D(Render2DEvent render2DEvent) {

        long nowNanos = System.nanoTime();
        float deltaSeconds =
                this.lastFrameNanos == 0L
                        ? 0.0F
                        : Math.clamp(
                                (float) (nowNanos - this.lastFrameNanos) / 1.0E9F, 0.0F, 0.05F);
        this.lastFrameNanos = nowNanos;
        this.entries.removeIf(
                queued ->
                        nowNanos - queued.notification.getCreatedAtNanos()
                                >= queued.notification.getDurationNanos());
        if (!this.entries.isEmpty()) {

            SizedFont titleFont = FontManager.getThemedFont("Product Sans Bold", 8.5F);

            SizedFont messageFont = FontManager.getThemedFont("Product Sans Regular", 7.5F);

            SizedFont iconFont = FontManager.getFont("Notification Icons", 20.0F);

            Color color = new Color(12, 12, 12, Theme.getBackgroundAlpha());

            Color color2 = new Color(190, 190, 195);
            Color color3 = Astralis.getInstance().getThemeManager().getPrimaryColor();
            Color color4 = Astralis.getInstance().getThemeManager().getSecondaryColor();
            float screenWidth = mc.getWindow().getGuiScaledWidth();
            float screenHeight = mc.getWindow().getGuiScaledHeight();
            float maxBoxWidth = Math.max(60.0F, Math.min(280.0F, screenWidth - 16.0F));
            float targetOffset = 8.0F;

            for (NotificationEntry entry : this.entries) {
                Notification notification = entry.notification;
                if (Float.isNaN(entry.animatedOffset)) {
                    entry.animatedOffset = targetOffset;
                } else {
                    entry.animatedOffset =
                            entry.animatedOffset
                                    + (targetOffset - entry.animatedOffset)
                                            * (1.0F - (float) Math.exp(-14.0F * deltaSeconds));
                }

                long elapsedNanos = Math.max(0L, nowNanos - notification.getCreatedAtNanos());
                long remainingNanos = notification.getDurationNanos() - elapsedNanos;
                float fadeNanos = Math.min(2.2E8F, (float) notification.getDurationNanos() / 2.0F);
                float fadeInProgress = Math.clamp((float) elapsedNanos / fadeNanos, 0.0F, 1.0F);
                float fadeOutProgress = Math.clamp((float) remainingNanos / fadeNanos, 0.0F, 1.0F);
                float slideProgress =
                        (1.0F - (float) Math.pow(1.0F - fadeInProgress, 3.0))
                                * fadeOutProgress
                                * fadeOutProgress
                                * (3.0F - 2.0F * fadeOutProgress);
                float remainingFraction =
                        Math.clamp(
                                (float) remainingNanos / (float) notification.getDurationNanos(),
                                0.0F,
                                1.0F);
                String truncatedTitle =
                        titleFont.truncateWithEllipsis(
                                notification.getTitle(), maxBoxWidth - 44.0F);
                String truncatedMessage =
                        messageFont.truncateWithEllipsis(
                                notification.getMessage(), maxBoxWidth - 44.0F);
                float boxWidth =
                        Math.clamp(
                                Math.max(
                                                titleFont.getStringWidth(truncatedTitle),
                                                messageFont.getStringWidth(truncatedMessage))
                                        + 44.0F,
                                Math.min(140.0F, maxBoxWidth),
                                maxBoxWidth);
                float boxX =
                        screenWidth - boxWidth - 8.0F + (1.0F - slideProgress) * (boxWidth + 20.0F);
                float boxY = screenHeight - entry.animatedOffset - 32.0F;
                Color color5 =
                        notification.getType() == NotificationType.INFO
                                ? color3
                                : notification.getType().getAccentColor();
                Color color6 = notification.getType() == NotificationType.INFO ? color4 : color5;
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangle(boxX, boxY, boxWidth, 32.0F, 3.0F);
                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(boxX, boxY, boxWidth, 32.0F)
                                        .setRadius(3.0F)
                                        .enableShadow()
                                        .setColor(color));

                for (int i = 0; i < 3; i++) {
                    float glowRadius = 10.0F - i;
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(
                                                    boxX + 16.0F - glowRadius,
                                                    boxY + 16.0F - glowRadius,
                                                    glowRadius * 2.0F,
                                                    glowRadius * 2.0F)
                                            .setRadius(glowRadius)
                                            .setColor(
                                                    new Color(
                                                            color5.getRed(),
                                                            color5.getGreen(),
                                                            color5.getBlue(),
                                                            2 + i)));
                }

                render2DEvent
                        .getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(boxX + 4.0F, boxY + 4.0F, 24.0F, 24.0F)
                                        .setRadius(12.0F)
                                        .setColor(color5.darker().darker()));
                render2DEvent
                        .getContext()
                        .drawText(
                                TextDrawRequest.obtain()
                                        .setText(String.valueOf(notification.getType().getIcon()))
                                        .setPosition(boxX + 4.0F, boxY + 4.5F)
                                        .centerInBox(24.0F, 24.0F)
                                        .setFont(iconFont.getMsdfFont())
                                        .setFontSize(iconFont.getSize())
                                        .setColor(color5));
                render2DEvent
                        .getContext()
                        .drawText(
                                TextDrawRequest.obtain()
                                        .setText(truncatedTitle)
                                        .setPosition(boxX + 32.0F, boxY + 4.0F)
                                        .setFont(titleFont.getMsdfFont())
                                        .setFontSize(titleFont.getSize())
                                        .enableShadow()
                                        .setColor(Color.WHITE));
                render2DEvent
                        .getContext()
                        .drawText(
                                TextDrawRequest.obtain()
                                        .setText(truncatedMessage)
                                        .setPosition(boxX + 32.0F, boxY + 16.0F)
                                        .setFont(messageFont.getMsdfFont())
                                        .setFontSize(messageFont.getSize())
                                        .enableShadow()
                                        .setColor(color2));
                Render2DContext context = render2DEvent.getContext();
                RoundedRectShape trackShape =
                        RoundedRectShape.obtain()
                                .setBounds(boxX + 32.0F, boxY + 29.0F, boxWidth - 42.0F, 1.0F)
                                .setRadius(0.5F);

                context.drawRect(trackShape.setColor(new Color(255, 255, 255, 18)));
                if (remainingFraction > 0.0F) {
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(
                                                    boxX + 32.0F,
                                                    boxY + 29.0F,
                                                    (boxWidth - 42.0F) * remainingFraction,
                                                    1.0F)
                                            .setRadius(0.5F)
                                            .setPaint(
                                                    ShapePaint.obtain()
                                                            .setHorizontalGradient(
                                                                    color5.getRGB(),
                                                                    color6.getRGB())));
                }

                targetOffset += 38.0F;
            }
        }
    }
}
