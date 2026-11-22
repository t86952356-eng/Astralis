package astralis.ui.screen;

import astralis.Astralis;
import astralis.ui.animation.Animation;
import astralis.ui.component.AddFriendTab;
import astralis.ui.component.FlatUiPalette;
import astralis.ui.component.FlatUiPaletteColors;
import astralis.ui.component.FriendChatPanel;
import astralis.ui.component.FriendRequestsTab;
import astralis.ui.component.FriendsTab;
import astralis.ui.component.OnlineFriendsTab;
import astralis.ui.component.SapphireScreen;
import astralis.ui.component.SocialTab;
import astralis.ui.component.SocialTextField;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.NonNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class FriendsScreen extends Screen implements InstanceAccess, SapphireScreen, FlatUiPalette {
    public static final float WINDOW_WIDTH = 650.0F;
    public static final float WINDOW_HEIGHT = 368.0F;
    public static final float HEADER_HEIGHT = 28.0F;
    public static final float CONTENT_VIEW_HEIGHT = 332.0F;
    public static final float CORNER_RADIUS = 8.0F;
    public static final float ENTRY_HEIGHT = 42.0F;
    private static float savedOffsetX;
    private static float savedOffsetY;
    private final Screen parentScreen;
    private final List<SocialTab> tabs;
    private final Animation openAnimation;
    private String statusMessage;
    private float offsetX;
    private float offsetY;
    private float windowX;
    private float windowY;
    private float dragStartMouseX;
    private float dragStartMouseY;
    private float dragStartOffsetX;
    private float dragStartOffsetY;
    private int lastMouseX;
    private int lastMouseY;
    private boolean draggingWindow;

    public FriendsScreen(Screen screen) {
        super(Component.literal("Friends"));
        this.tabs = new ArrayList<>();
        this.openAnimation = new Animation(Easing.EASE_OUT_BACK, 420L);
        this.statusMessage = "Ready";
        this.parentScreen = screen;
        SocialTextField searchField =
                new SocialTextField(this, "search", 32, "Product Sans Regular", 7.0F);
        FriendChatPanel chatPanel = new FriendChatPanel(this);
        this.tabs.add(new FriendsTab(this, "All", searchField, chatPanel));
        this.tabs.add(new OnlineFriendsTab(this, searchField, chatPanel));
        this.tabs.add(new FriendRequestsTab(this, searchField, chatPanel));
        this.tabs.add(new AddFriendTab(this, searchField, chatPanel));
        this.tabs.getFirst().activate();
    }

    protected void init() {
        super.init();
        this.offsetX = savedOffsetX;
        this.offsetY = savedOffsetY;
        this.openAnimation.setValue(0.0);
        this.openAnimation.restart();
        this.updateLayout();
        Astralis.getInstance().getFriendManager().setNotificationsMuted(true);
        astralis.OfflineServices.unavailable("Astralis social services");
        this.statusMessage = "Refreshing friend data";
    }

    public void removed() {
        Astralis.getInstance().getFriendManager().setNotificationsMuted(false);
    }

    public void onClose() {
        Astralis.getInstance().getFriendManager().setNotificationsMuted(false);
        InstanceAccess.mc.gui.setScreen(this.parentScreen);
    }

    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.draggingWindow) {
            this.offsetX = this.dragStartOffsetX + mouseX - this.dragStartMouseX;
            this.offsetY = this.dragStartOffsetY + mouseY - this.dragStartMouseY;
            savedOffsetX = this.offsetX;
            savedOffsetY = this.offsetY;
        }

        this.openAnimation.animateTo(1.0);
        this.updateLayout();
        this.tabs.forEach(tab -> tab.mouseDragged(mouseX, mouseY));
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    public void extractBackground(
            GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        if (this.parentScreen instanceof PauseScreen) {
            return;
        }

        this.extractBackground(guiGraphicsExtractor, mouseX, mouseY, partialTick);
    }

    public boolean isPauseScreen() {
        return this.parentScreen instanceof PauseScreen;
    }

    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        this.tabs.forEach(content -> content.click(event));
        for (SocialTab content : this.tabs)
            if (content.isMouseOverTab(event.x(), event.y())) return true;
        if (RenderUtil.isInsideRect(
                event.x(), event.y(), this.windowX, this.windowY, 650.0F, 28.0F)) {
            this.draggingWindow = true;
            this.dragStartMouseX = (float) event.x();
            this.dragStartMouseY = (float) event.y();
            this.dragStartOffsetX = this.offsetX;
            this.dragStartOffsetY = this.offsetY;
            return true;
        }
        return RenderUtil.isInsideRect(
                event.x(), event.y(), this.windowX, this.windowY, 650.0F, 368.0F);
    }

    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        this.draggingWindow = false;
        this.tabs.forEach(tab -> tab.release(event));
        return super.mouseReleased(event);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (SocialTab content : this.tabs)
            if (content.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void renderSapphire(Render2DContext context) {
        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 12.0F);
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        float scale = (float) this.openAnimation.getValue();
        float pivotX = this.windowX + 325.0F;
        float pivotY = this.windowY + 184.0F;
        context.pushPose();
        context.translate(pivotX, pivotY);
        context.scale(scale, scale, 1.0F);
        context.translate(-pivotX, -pivotY);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY, 650.0F, 368.0F)
                        .setRadius(8.0F)
                        .setColor(FlatUiPaletteColors.BACKGROUND));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY, 650.0F, 28.0F)
                        .setCornerRadii(8.0F, 8.0F, 0.0F, 0.0F)
                        .setColor(FlatUiPaletteColors.BACKGROUND_DEEP));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY + 28.0F - 1.0F, 650.0F, 1.0F)
                        .setColor(FlatUiPaletteColors.DIVIDER));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText("Friends")
                        .setPosition(this.windowX + 11.0F, this.windowY)
                        .centerVertically(28.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                ColorUtil.waveBlend(
                                                        this.windowX + 11.0F,
                                                        color.getRGB(),
                                                        color2.getRGB()),
                                                ColorUtil.waveBlend(
                                                        this.windowX
                                                                + 11.0F
                                                                + titleFont.getStringWidth(
                                                                        "Friends"),
                                                        color.getRGB(),
                                                        color2.getRGB()),
                                                this.windowX + 11.0F,
                                                this.windowY,
                                                this.windowX
                                                        + 11.0F
                                                        + titleFont.getStringWidth("Friends"),
                                                this.windowY)));
        this.tabs.forEach(tab -> tab.render(context, this.lastMouseX, this.lastMouseY));
        context.popPose();
        Astralis.getInstance()
                .getRenderEffects()
                .renderShadowShapes(
                        shadowContext -> {
                            shadowContext.pushPose();
                            shadowContext.translate(pivotX, pivotY);
                            shadowContext.scale(scale, scale, 1.0F);
                            shadowContext.translate(-pivotX, -pivotY);
                            shadowContext.drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(this.windowX, this.windowY, 650.0F, 368.0F)
                                            .setRadius(8.0F)
                                            .setColor(Color.BLACK));
                            shadowContext.popPose();
                        });
    }

    public boolean keyPressed(@NonNull KeyEvent event) {
        for (SocialTab content : this.tabs) if (content.keyPressed(event)) return true;
        if (event.key() == 82) {
            astralis.OfflineServices.unavailable("Astralis social services");
            this.statusMessage = "Refreshing friend data";
            return true;
        }
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean charTyped(@NonNull CharacterEvent event) {
        for (SocialTab content : this.tabs) if (content.charTyped(event)) return true;
        return super.charTyped(event);
    }

    private void updateLayout() {
        this.windowX = (this.width - 650.0F) / 2.0F + this.offsetX;
        this.windowY = (this.height - 368.0F) / 2.0F + this.offsetY;
        SizedFont tabFont = FontManager.getFont("Product Sans Medium", 7.0F);
        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 12.0F);
        float tabX = this.windowX + 11.0F + titleFont.getStringWidth("Friends") + 12.0F;

        for (SocialTab tab : this.tabs) {
            float tabWidth = tab.getTabWidth(tabFont);
            tab.setBounds(tabX, this.windowY + 6.0F, tabWidth, 16.0F);
            tab.setPaneBounds(this.windowX, this.windowY + 28.0F, 650.0F, 340.0F);
            tabX += tabWidth + 6.0F;
        }
    }

    public void selectTab(Class<? extends SocialTab> tabClass) {
        this.tabs.forEach(SocialTab::deactivate);
        for (SocialTab content : this.tabs)
            if (content.getClass() == tabClass) {
                content.activate();
                return;
            }
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }
}
