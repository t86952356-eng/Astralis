package astralis.ui.component;

import astralis.ui.screen.FriendsScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FriendEntry extends SocialComponent {
    private final String username;
    private final Runnable onClick;
    private final List<SocialButton> actionButtons = new ArrayList<>();
    private String statusText;
    private Color statusColor;
    private boolean selected;

    public FriendEntry(
            FriendsScreen screen,
            String username,
            String statusText,
            Color color,
            Runnable onClick) {
        super(screen);
        this.username = username;
        this.statusText = statusText;
        this.statusColor = color;
        this.onClick = onClick;
    }

    public void addActionButton(SocialButton button) {
        this.actionButtons.add(button);
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {

        SizedFont nameFont = FontManager.getFont("Product Sans Bold", 9.0F);

        SizedFont statusFont = FontManager.getFont("Product Sans Regular", 7.0F);
        boolean hovered =
                RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight());
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                        .setRadius(7.0F)
                        .setColor(
                                this.selected
                                        ? FlatUiPaletteColors.CONTROL_ACTIVE
                                        : (hovered
                                                ? FlatUiPaletteColors.CONTROL_HOVER
                                                : FlatUiPaletteColors.CONTROL)));
        float buttonX = this.getX() + this.getWidth() - 7.0F;

        for (int i = this.actionButtons.size() - 1; i >= 0; i--) {
            SocialButton button = this.actionButtons.get(i);
            buttonX -= button.getWidth();
            button.setBounds(
                    buttonX,
                    this.getY() + (this.getHeight() - button.getHeight()) / 2.0F,
                    button.getWidth(),
                    button.getHeight());
            button.render(context, mouseX, mouseY);
            buttonX -= 5.0F;
        }

        float nameMaxWidth = this.getWidth() - 72.0F;
        if (this.actionButtons.size() == 1) {
            nameMaxWidth -= 46.0F;
        } else if (this.actionButtons.size() == 2) {
            nameMaxWidth -= 98.0F;
        }

        this.drawAvatar(
                context,
                this.username,
                this.getX() + 11.0F,
                this.getY() + this.getHeight() / 2.0F - 12.0F,
                24.0F,
                statusFont);
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(
                                nameFont.truncateWithEllipsis(
                                        this.username, Math.max(24.0F, nameMaxWidth)))
                        .setPosition(
                                this.getX() + 43.0F, this.getY() + this.getHeight() / 2.0F - 11.0F)
                        .setFont(nameFont.getMsdfFont())
                        .setFontSize(nameFont.getSize())
                        .enableShadow()
                        .setColor(FlatUiPaletteColors.TEXT.getRGB()));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(this.statusText)
                        .setPosition(
                                this.getX() + 43.0F, this.getY() + this.getHeight() / 2.0F + 1.0F)
                        .setFont(statusFont.getMsdfFont())
                        .setFontSize(statusFont.getSize())
                        .enableShadow()
                        .setColor(this.statusColor.getRGB()));
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        Iterator iterator = this.actionButtons.iterator();

        while (iterator.hasNext()) {
            SocialButton button = (SocialButton) iterator.next();
            if (button.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                button.click(mouseButtonEvent);
                return;
            }
        }

        if (mouseButtonEvent.button() == 0
                && RenderUtil.isInsideRect(
                        mouseButtonEvent.x(),
                        mouseButtonEvent.y(),
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight())) {
            this.onClick.run();
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.actionButtons.forEach(button -> button.release(mouseButtonEvent));
    }

    @Generated
    public String getUsername() {
        return this.username;
    }

    @Generated
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    @Generated
    public void setStatusColor(Color color) {
        this.statusColor = color;
    }

    @Generated
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
