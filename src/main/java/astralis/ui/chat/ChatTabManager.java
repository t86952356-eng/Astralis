package astralis.ui.chat;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.ChatInputEvent;
import astralis.event.impl.Render2DEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.Theme;
import astralis.social.chat.IrcChatManager;
import astralis.ui.ChatScrollAccessor;
import astralis.util.ChatUtil;
import astralis.util.HypixelModApi;
import astralis.util.Listener;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class ChatTabManager extends Listener {
    private float chatTabX;
    private float partyTabX;
    private float ircTabX;
    private float tabY;
    private boolean draggingScrollbar;
    private float dragGrabOffset;
    private static ChatTab selectedTab;

    private boolean isPartyMessage(Component component) {
        String plainText = ChatFormatting.stripFormatting(component.getString());
        if (plainText != null) {

            if (plainText.contains("Party >")) {
                return true;
            }

            if (plainText.contains("P >")) {
                return true;
            }
        }

        return false;
    }

    private ChatScrollbarMetrics computeScrollbarMetrics() {
        if (selectedTab != ChatTab.IRC) {
            return null;
        }

        ChatComponent chatComponent = mc.gui.hud.getChat();
        ChatScrollAccessor scrollAccessor = (ChatScrollAccessor) chatComponent;
        int maxScroll = scrollAccessor.astralis$getMaximumScroll();
        if (maxScroll <= 0) {
            return null;
        }

        float chatScale = mc.options.chatScale().get().floatValue();
        int linesPerPage = chatComponent.getLinesPerPage();
        int totalLines = maxScroll + linesPerPage;
        int lineHeight = (int) (9.0 * (mc.options.chatLineSpacing().get() + 1.0));
        int visibleHeight = linesPerPage * lineHeight;
        int totalHeight = totalLines * lineHeight;
        int thumbHeight = visibleHeight * visibleHeight / totalHeight;
        int guiHeight = mc.getWindow().getGuiScaledHeight();

        int chatBottomY = (int) Math.floor((guiHeight - 40) / chatScale);
        int thumbOffset = scrollAccessor.astralis$getScrollPosition() * visibleHeight / totalLines;
        int chatWidth =
                (int) Math.ceil(ChatComponent.getWidth(mc.options.chatWidth().get()) / chatScale);
        float trackX = (chatWidth + 8.0F) * chatScale;
        float trackY = (chatBottomY - visibleHeight) * chatScale;
        float trackHeight = visibleHeight * chatScale;
        float trackWidth = 3.0F * chatScale;
        float thumbY = (chatBottomY - thumbOffset - thumbHeight) * chatScale;
        return new ChatScrollbarMetrics(
                trackX,
                trackY,
                trackWidth,
                trackHeight,
                thumbY,
                thumbHeight * chatScale,
                maxScroll);
    }

    private boolean isInHypixelParty() {
        HypixelModApi hypixelApi = Astralis.getInstance().getHypixelApi();
        return hypixelApi != null && hypixelApi.isConnected() && hypixelApi.isInParty();
    }

    public void dragScrollbar(double mouseY) {
        if (this.draggingScrollbar) {
            ChatScrollbarMetrics metrics = this.computeScrollbarMetrics();
            if (metrics != null) {
                float trackTravel = metrics.getTrackHeight() - metrics.getThumbHeight();
                float thumbY =
                        Math.clamp(
                                (float) mouseY - this.dragGrabOffset,
                                metrics.getTrackY(),
                                metrics.getTrackY() + trackTravel);
                float scrollFraction = (thumbY - metrics.getTrackY()) / Math.max(1.0F, trackTravel);
                ChatScrollAccessor scrollAccessor = (ChatScrollAccessor) mc.gui.hud.getChat();
                scrollAccessor.astralis$setScrollPosition(
                        Math.round((1.0F - scrollFraction) * metrics.getMaxScroll()));
            }
        }
    }

    public boolean shouldShowMessage(GuiMessage guiMessage) {
        if (IrcChatManager.isLiveMessage(guiMessage)) {
            return selectedTab != ChatTab.PARTY;
        } else {
            return IrcChatManager.isIrcMessage(guiMessage)
                    ? selectedTab == ChatTab.IRC
                    : selectedTab != ChatTab.IRC
                            && (selectedTab != ChatTab.PARTY
                                    || this.isPartyMessage(guiMessage.content()));
        }
    }

    public boolean handleScrollbarClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        } else {
            ChatScrollbarMetrics metrics = this.computeScrollbarMetrics();
            if (metrics != null
                    && !(mouseX < metrics.getTrackX() - 5.0F)
                    && !(mouseX > metrics.getTrackX() + metrics.getTrackWidth() + 5.0F)
                    && !(mouseY < metrics.getTrackY())
                    && !(mouseY > metrics.getTrackY() + metrics.getTrackHeight())) {
                this.draggingScrollbar = true;
                this.dragGrabOffset =
                        mouseY >= metrics.getThumbY()
                                        && mouseY <= metrics.getThumbY() + metrics.getThumbHeight()
                                ? (float) mouseY - metrics.getThumbY()
                                : metrics.getThumbHeight() / 2.0F;
                this.dragScrollbar(mouseY);
                return true;
            } else {
                return false;
            }
        }
    }

    public void onChatScreenInit() {
        this.draggingScrollbar = false;
        IrcChatManager ircChatManager = Astralis.getInstance().getIrcChatManager();
        if (selectedTab == ChatTab.IRC) {
            ircChatManager.queueRecentHistory();
        }

        ircChatManager.requestRemoteHistory();
        Astralis.getInstance().getHypixelApi().requestPartyInfo();
    }

    public void handleTabClick(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (RenderUtil.isInsideRect(mouseX, mouseY, this.chatTabX, this.tabY, 42.0F, 13.0F)) {
                this.selectTab(ChatTab.CHAT);
            } else if (this.isInHypixelParty()
                    && RenderUtil.isInsideRect(
                            mouseX, mouseY, this.partyTabX, this.tabY, 42.0F, 13.0F)) {
                this.selectTab(ChatTab.PARTY);
            } else {
                if (RenderUtil.isInsideRect(
                        mouseX, mouseY, this.ircTabX, this.tabY, 42.0F, 13.0F)) {
                    this.selectTab(ChatTab.IRC);
                }
            }
        }
    }

    public void releaseScrollbar() {
        this.draggingScrollbar = false;
    }

    private void selectTab(ChatTab tab) {
        if (selectedTab != tab) {
            selectedTab = tab;
            this.draggingScrollbar = false;
            if (tab == ChatTab.IRC) {
                IrcChatManager ircChatManager = Astralis.getInstance().getIrcChatManager();
                ircChatManager.queueRecentHistory();
                ircChatManager.requestRemoteHistory();
            }

            mc.gui.hud.getChat().rescaleChat();
        }
    }

    public boolean isIrcTabSelected() {
        return selectedTab == ChatTab.IRC;
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {

        if (mc.gui.screen() instanceof ChatScreen) {
            boolean inHypixelParty = this.isInHypixelParty();
            if (!inHypixelParty && selectedTab == ChatTab.PARTY) {
                this.selectTab(ChatTab.CHAT);
            }

            SizedFont font = FontManager.getThemedFont("Product Sans Medium", 7.0F);

            Color color = new Color(238, 239, 242);

            Color color2 = new Color(145, 147, 154);
            Color color3 = Astralis.getInstance().getPrimaryColor();
            int backgroundAlpha = Theme.getBackgroundAlpha();
            this.chatTabX = 2.0F;
            this.partyTabX = this.chatTabX + 44.0F;
            this.ircTabX = this.partyTabX + (inHypixelParty ? 44.0F : 0.0F);
            this.tabY = mc.getWindow().getGuiScaledHeight() - 30.0F;
            if (Effects.isChatTabsBlur()) {
                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangle(this.chatTabX, this.tabY, 42.0F, 13.0F, 3.0F);
                if (inHypixelParty) {
                    render2DEvent
                            .getRenderEffects()
                            .blurRoundedRectangle(this.partyTabX, this.tabY, 42.0F, 13.0F, 3.0F);
                }

                render2DEvent
                        .getRenderEffects()
                        .blurRoundedRectangle(this.ircTabX, this.tabY, 42.0F, 13.0F, 3.0F);
            }

            Render2DContext chatContext = render2DEvent.getContext();

            this.renderTabButton(
                    chatContext,
                    this.chatTabX,
                    "Chat",
                    selectedTab == ChatTab.CHAT,
                    font,
                    color3,
                    color,
                    color2,
                    backgroundAlpha);
            if (inHypixelParty) {
                Render2DContext partyContext = render2DEvent.getContext();

                this.renderTabButton(
                        partyContext,
                        this.partyTabX,
                        "Party",
                        selectedTab == ChatTab.PARTY,
                        font,
                        color3,
                        color,
                        color2,
                        backgroundAlpha);
            }

            Render2DContext ircContext = render2DEvent.getContext();

            this.renderTabButton(
                    ircContext,
                    this.ircTabX,
                    "IRC",
                    selectedTab == ChatTab.IRC,
                    font,
                    color3,
                    color,
                    color2,
                    backgroundAlpha);
        }
    }

    @EventTarget
    public void onChatInput(ChatInputEvent event) {
        if (!event.getMessage().isBlank()
                && !event.getMessage().startsWith("/")
                && !event.getMessage().startsWith("#")
                && !event.getMessage()
                        .startsWith(Astralis.getInstance().getCommandManager().getPrefix())) {
            if (selectedTab == ChatTab.IRC) {
                event.setCancelled(true);
                Astralis.getInstance().getIrcChatManager().sendMessage(event.getMessage().trim());
            } else if (selectedTab == ChatTab.PARTY) {
                String message = event.getMessage();

                if (!message.startsWith("/pc ")) {
                    event.setCancelled(true);
                    String command = event.getMessage().trim();

                    ChatUtil.sendCommand("pc " + command);
                }
            }
        }
    }

    private void renderTabButton(
            Render2DContext context,
            float tabX,
            String label,
            boolean selected,
            SizedFont font,
            Color color,
            Color color2,
            Color color3,
            int backgroundAlpha) {
        RoundedRectShape shape =
                RoundedRectShape.obtain()
                        .setBounds(tabX, this.tabY, 42.0F, 13.0F)
                        .setRadius(3.0F)
                        .setShadowEnabled(Effects.isChatTabsShadow());
        Color color4;
        if (selected) {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            color4 = new Color(red, green, blue, 60);
        } else {

            color4 = new Color(18, 18, 22, backgroundAlpha);
        }

        context.drawRect(shape.setColor(color4));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(label)
                        .setPosition(tabX, this.tabY)
                        .centerInBox(42.0F, 13.0F)
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setColor((selected ? color2 : color3).getRGB()));
    }

    static {
        selectedTab = ChatTab.CHAT;
    }
}
