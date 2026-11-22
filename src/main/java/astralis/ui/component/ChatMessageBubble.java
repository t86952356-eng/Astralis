package astralis.ui.component;

import astralis.Astralis;
import astralis.ui.screen.FriendsScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatMessageBubble extends SocialComponent {
    private final String friendName;
    private final String sender;
    private final String messageText;
    private final String timeText;
    private final String dateText;
    private List<String> wrappedLines;
    private float wrapWidth;
    private boolean showDateHeader;

    public ChatMessageBubble(FriendsScreen screen, String friendName, String rawMessage) {

        super(screen);
        this.wrappedLines = List.of("");
        this.wrapWidth = -1.0F;
        this.friendName = friendName;

        int timestampEnd = rawMessage.indexOf("] ");

        int arrowIndex = rawMessage.indexOf(" -> ", timestampEnd + 2);

        int colonIndex = rawMessage.indexOf(": ", arrowIndex + 4);
        if (rawMessage.startsWith("[")
                && timestampEnd != -1
                && arrowIndex != -1
                && colonIndex != -1) {
            this.sender = rawMessage.substring(timestampEnd + 2, arrowIndex);
            this.messageText = rawMessage.substring(colonIndex + 2);
            String timestampText = rawMessage.substring(1, timestampEnd);
            String trimmedTimestamp = timestampText.trim();

            String isoTimestamp = trimmedTimestamp.replace(' ', 'T');

            LocalDateTime dateTime;
            try {
                dateTime = OffsetDateTime.parse(isoTimestamp).toLocalDateTime();
            } catch (DateTimeParseException offsetParseException) {
                try {
                    dateTime = LocalDateTime.parse(isoTimestamp);
                } catch (DateTimeParseException localParseException) {
                    int isoLength = isoTimestamp.length();

                    if (isoLength >= 10) {
                        LocalDateTime startOfDay;
                        try {

                            startOfDay =
                                    LocalDate.parse(isoTimestamp.substring(0, 10)).atStartOfDay();
                        } catch (DateTimeParseException dateOnlyParseException) {
                            startOfDay = null;
                        }

                        dateTime = startOfDay;
                    } else {
                        dateTime = null;
                    }
                }
            }

            if (dateTime != null) {

                this.dateText =
                        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).format(dateTime);

                this.timeText = DateTimeFormatter.ofPattern("HH:mm").format(dateTime);
            } else {
                String fallbackDate = "";
                String fallbackTime = "";
                int timestampLength = timestampText.length();

                if (timestampLength >= 10) {
                    char firstSeparator = timestampText.charAt(4);

                    if (firstSeparator == '-') {

                        char secondSeparator = timestampText.charAt(7);

                        if (secondSeparator == '-') {
                            try {

                                DateTimeFormatter dateFormatter =
                                        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

                                fallbackDate =
                                        dateFormatter.format(
                                                LocalDate.parse(timestampText.substring(0, 10)));
                            } catch (DateTimeParseException dateParseException) {

                                fallbackDate = timestampText.substring(0, 10);
                            }
                        }
                    }
                }

                for (int i = 0; i <= timestampText.length() - 5; i++) {
                    if (Character.isDigit(timestampText.charAt(i))
                            && Character.isDigit(timestampText.charAt(i + 1))) {
                        char colonChar = timestampText.charAt(i + 2);

                        if (colonChar == ':'
                                && Character.isDigit(timestampText.charAt(i + 3))
                                && Character.isDigit(timestampText.charAt(i + 4))) {
                            fallbackTime = timestampText.substring(i, i + 5);
                            break;
                        }
                    }
                }

                this.dateText = fallbackDate;
                this.timeText = fallbackTime;
            }
        } else {

            this.sender = "Message";
            this.messageText = rawMessage;
            this.dateText = "";
            this.timeText = "";
        }
    }

    public float layoutHeight(SizedFont font, float width) {
        float maxLineWidth = width * 0.72F - 16.0F;
        if (Math.abs(this.wrapWidth - maxLineWidth) > 0.01F) {
            ArrayList lines = new ArrayList();
            StringBuilder currentLine = new StringBuilder();

            for (String word : this.messageText.split(" ")) {
                if (!currentLine.isEmpty()
                        && font.getStringWidth(currentLine + " " + word) <= maxLineWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                        currentLine.setLength(0);
                    }

                    for (int i = 0; i < word.length(); i++) {
                        if (font.getStringWidth("" + currentLine + word.charAt(i)) > maxLineWidth
                                && !currentLine.isEmpty()) {
                            lines.add(currentLine.toString());
                            currentLine.setLength(0);
                        }

                        currentLine.append(word.charAt(i));
                    }
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            this.wrappedLines = lines.isEmpty() ? List.of("") : lines;
            this.wrapWidth = maxLineWidth;
        }

        this.setHeight(
                (this.showDateHeader ? 22.0F : 0.0F) + 20.0F + this.wrappedLines.size() * 11.0F);
        return this.getHeight();
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {

        SizedFont bodyFont = FontManager.getFont("Product Sans Regular", 8.0F);

        SizedFont smallFont = FontManager.getFont("Product Sans Regular", 7.0F);
        this.layoutHeight(bodyFont, this.getWidth());
        float y = this.getY();
        if (this.showDateHeader) {
            float dateWidth = smallFont.getStringWidth(this.dateText);
            float lineWidth = Math.max(0.0F, (this.getWidth() - dateWidth) / 2.0F - 10.0F);
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.getX(), y + 10.0F, lineWidth, 1.0F)
                            .setColor(FlatUiPaletteColors.DIVIDER));
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX() + this.getWidth() - lineWidth,
                                    y + 10.0F,
                                    lineWidth,
                                    1.0F)
                            .setColor(FlatUiPaletteColors.DIVIDER));
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(this.dateText)
                            .setPosition(
                                    this.getX() + (this.getWidth() - dateWidth) / 2.0F, y + 5.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .enableShadow()
                            .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
            y += 22.0F;
        }

        boolean ownMessage = !this.sender.equalsIgnoreCase(this.friendName);
        float maxBubbleWidth = this.getWidth() * 0.72F;
        String senderLabel;
        if (this.sender.isEmpty()) {

            senderLabel = "Message";
        } else {
            senderLabel = this.sender;
        }

        float contentWidth = smallFont.getStringWidth(senderLabel) + 20.0F;

        for (String wrappedLine : this.wrappedLines) {
            contentWidth = Math.max(contentWidth, bodyFont.getStringWidth(wrappedLine) + 18.0F);
        }

        float bubbleWidth = Math.clamp(contentWidth, 54.0F, maxBubbleWidth);
        float bubbleHeight = 20.0F + this.wrappedLines.size() * 11.0F;
        float bubbleX = ownMessage ? this.getX() + this.getWidth() - bubbleWidth : this.getX();
        Color color = Astralis.getInstance().getPrimaryColor();
        RoundedRectShape bubbleShape =
                RoundedRectShape.obtain()
                        .setBounds(bubbleX, y, bubbleWidth, bubbleHeight)
                        .setRadius(7.0F);
        Color color2;
        if (ownMessage) {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            color2 = new Color(red, green, blue, 42);
        } else {
            color2 = FlatUiPaletteColors.CONTROL;
        }

        context.drawRect(bubbleShape.setColor(color2));
        TextDrawRequest senderRequest = TextDrawRequest.obtain();
        String senderName;
        if (this.sender.isEmpty()) {

            senderName = "Message";
        } else {
            senderName = this.sender;
        }

        context.drawText(
                senderRequest
                        .setText(smallFont.truncateWithEllipsis(senderName, bubbleWidth - 14.0F))
                        .setPosition(bubbleX + 8.0F, y + 5.0F)
                        .setFont(smallFont.getMsdfFont())
                        .setFontSize(smallFont.getSize())
                        .enableShadow()
                        .setColor(
                                ownMessage
                                        ? FlatUiPaletteColors.TEXT.getRGB()
                                        : FlatUiPaletteColors.TEXT_SECONDARY.getRGB()));
        float lineY = y + 17.0F;

        for (String line : this.wrappedLines) {
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(line)
                            .setPosition(bubbleX + 8.0F, lineY)
                            .setFont(bodyFont.getMsdfFont())
                            .setFontSize(bodyFont.getSize())
                            .enableShadow()
                            .setColor(FlatUiPaletteColors.TEXT.getRGB()));
            lineY += 11.0F;
        }

        if (!this.timeText.isEmpty()) {
            String timeLabel = smallFont.truncateWithEllipsis(this.timeText, 34.0F);
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(timeLabel)
                            .setPosition(
                                    ownMessage
                                            ? bubbleX - smallFont.getStringWidth(timeLabel) - 5.0F
                                            : bubbleX + bubbleWidth + 5.0F,
                                    y + bubbleHeight - 10.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .enableShadow()
                            .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
        }
    }

    @Generated
    public String getDateText() {
        return this.dateText;
    }

    @Generated
    public void setShowDateHeader(boolean showDateHeader) {
        this.showDateHeader = showDateHeader;
    }

    @Generated
    public boolean isShowDateHeader() {
        return this.showDateHeader;
    }
}
