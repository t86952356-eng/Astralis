package astralis.util.render.font;

import com.astralis.scripting.InstanceAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SizedFont implements InstanceAccess {
    private final String name;
    private final float size;
    private final MsdfFont msdfFont;
    private final HashMap<String, Float> widthCache;

    public SizedFont(String name, float size) {

        this.widthCache = new HashMap<>();
        this.name = name;
        this.size = size;
        String resourceName = name.toLowerCase().replace(" ", "_");
        this.msdfFont = MsdfFont.load(mc.getResourceManager(), name, resourceName, resourceName);
    }

    public HashMap<String, Float> getWidthCache() {
        return this.widthCache;
    }

    public float getLineHeight() {
        return this.msdfFont.getScaledLineHeight(this.size);
    }

    public float getStringWidth(String text) {
        if (text == null) {
            return 0.0F;
        }

        Float cachedWidth = this.widthCache.get(text);
        if (cachedWidth != null) {
            return cachedWidth;
        }

        float width = this.msdfFont.getStringWidth(text, this.size);
        this.widthCache.put(text, width);
        return width;
    }

    public String truncateWithEllipsis(String text, float maxWidth) {
        if (this.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String truncated;
        for (truncated = text;
                !truncated.isEmpty();
                truncated = truncated.substring(0, truncated.length() - 1)) {

            if (!(this.getStringWidth(truncated + "...") > maxWidth)) {
                break;
            }
        }

        return truncated + "...";
    }

    public String getName() {
        return this.name;
    }

    public float getSize() {
        return this.size;
    }

    public MsdfFont getMsdfFont() {
        return this.msdfFont;
    }

    public List<String> wrapText(String text, float maxWidth) {
        ArrayList lines = new ArrayList();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (this.getStringWidth(candidate) <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }

                if (this.getStringWidth(word) <= maxWidth) {
                    currentLine.append(word);
                } else {
                    String remaining = word;

                    while (!remaining.isEmpty()) {
                        int chunkLength = remaining.length();

                        while (chunkLength > 1
                                && this.getStringWidth(remaining.substring(0, chunkLength))
                                        > maxWidth) {
                            chunkLength--;
                        }

                        lines.add(remaining.substring(0, chunkLength));
                        remaining = remaining.substring(chunkLength);
                    }
                }
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines.isEmpty() ? List.of(text) : lines;
    }
}
