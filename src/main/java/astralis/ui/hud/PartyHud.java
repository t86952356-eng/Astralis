package astralis.ui.hud;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.InGameKeyPressEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.Render3DEvent;
import astralis.module.impl.Effects;
import astralis.module.impl.PartySettings;
import astralis.module.impl.Theme;
import astralis.social.party.PartyInvite;
import astralis.social.party.PartyManager;
import astralis.social.party.PartyMember;
import astralis.social.party.PartyMessage;
import astralis.social.party.PartyWaypoint;
import astralis.social.party.WaypointFadeState;
import astralis.social.profile.PresenceManager;
import astralis.ui.screen.PartyChatScreen;
import astralis.util.Listener;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyHud extends Listener {
    private final Draggable draggable;
    private final List<PartyHeadRegion> headRegions;
    private final List<PartyActionRegion> actionRegions;
    private final Map<String, WaypointFadeState> waypointBeams;
    private final Map<UUID, Identifier> skinTextureCache;
    private UUID selectedPlayerId;
    private int messageScrollOffset;
    private boolean collapsed;
    private boolean pickKeyHeld;
    private boolean dismissed;

    public PartyHud() {

        this.headRegions = new ArrayList<>();
        this.actionRegions = new ArrayList<>();
        this.waypointBeams = new LinkedHashMap<>();
        this.skinTextureCache = new LinkedHashMap<>();

        this.draggable = new Draggable("Party HUD", 252.0F, 108.0F, 10.0F, 125.0F);
        this.draggable.setVisibleWhen(
                () ->
                        this.isHudVisible()
                                && (!Astralis.getInstance().getPartyManager().getInvites().isEmpty()
                                        || Astralis.getInstance().getPartyManager().isInParty()));
        Astralis.getInstance().getHudManager().addDraggable(this.draggable);
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        long scrollValue;
        if (this.isHudVisible()) {
            PartyManager partyManager = Astralis.getInstance().getPartyManager();
            if (partyManager.isInParty() || !partyManager.getInvites().isEmpty()) {
                PartyChatScreen chatScreen =
                        mc.gui.screen() instanceof PartyChatScreen currentScreen
                                ? currentScreen
                                : null;
                SizedFont titleFont = FontManager.getThemedFont("Product Sans Bold", 9.0F);
                SizedFont labelFont = FontManager.getThemedFont("Product Sans Medium", 7.0F);
                SizedFont textFont = FontManager.getThemedFont("Product Sans Regular", 7.0F);
                SizedFont smallFont = FontManager.getThemedFont("Product Sans Regular", 6.0F);
                int memberCount;
                long blinkPhase;

                Color color = new Color(238, 239, 242);

                Color color2 = new Color(145, 147, 154);

                Color color3 = new Color(78, 196, 116);

                Color color4 = new Color(112, 114, 120);

                Color color5 = new Color(225, 177, 58);
                int backgroundAlpha = Theme.getBackgroundAlpha();
                float hudX = this.draggable.getX();
                float hudY = this.draggable.getY();
                float hudScale = this.draggable.getScale();
                this.headRegions.clear();
                this.actionRegions.clear();
                if (this.collapsed) {
                    this.draggable.setHeight(18.0F);
                    if (Effects.isPartyHudBlur()) {
                        render2DEvent
                                .getRenderEffects()
                                .blurRoundedRectangle(
                                        hudX,
                                        hudY,
                                        252.0F * hudScale,
                                        18.0F * hudScale,
                                        6.0F * hudScale);
                    }

                    Render2DContext backgroundContext = render2DEvent.getContext();
                    RoundedRectShape backgroundShape =
                            RoundedRectShape.obtain()
                                    .setBounds(hudX, hudY, 252.0F * hudScale, 18.0F * hudScale)
                                    .setRadius(6.0F * hudScale)
                                    .setShadowEnabled(Effects.isPartyHudShadow());

                    backgroundContext.drawRect(
                            backgroundShape.setColor(new Color(18, 18, 22, backgroundAlpha)));
                    Render2DContext titleContext = render2DEvent.getContext();
                    TextDrawRequest titleRequest = TextDrawRequest.obtain();
                    titleContext.drawText(
                            titleRequest
                                    .setText("Party")
                                    .setPosition(hudX + 8.0F * hudScale, hudY + 4.0F * hudScale)
                                    .setFont(titleFont.getMsdfFont())
                                    .setFontSize(titleFont.getSize() * hudScale)
                                    .enableShadow()
                                    .setColor(color.getRGB()));
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setText("+")
                                            .setPosition(hudX + 220.0F * hudScale, hudY)
                                            .centerInBox(16.0F * hudScale, 18.0F * hudScale)
                                            .setFont(titleFont.getMsdfFont())
                                            .setFontSize(titleFont.getSize() * hudScale)
                                            .enableShadow()
                                            .setColor(color2.getRGB()));
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setText("×")
                                            .setPosition(hudX + 236.0F * hudScale, hudY)
                                            .centerInBox(16.0F * hudScale, 18.0F * hudScale)
                                            .setFont(titleFont.getMsdfFont())
                                            .setFontSize(titleFont.getSize() * hudScale)
                                            .enableShadow()
                                            .setColor(color2.getRGB()));
                } else {
                    if (chatScreen == null) {
                        this.messageScrollOffset = 0;
                    }

                    scrollValue = this.messageScrollOffset;
                    int messageCount = partyManager.getMessages().size();

                    this.messageScrollOffset =
                            Math.clamp(scrollValue, 0, Math.max(0, messageCount - 7));
                    int visibleCount = Math.min(7, partyManager.getMessages().size());
                    int endIndex = partyManager.getMessages().size() - this.messageScrollOffset;
                    int startIndex = endIndex - visibleCount;
                    float messagesHeight =
                            (visibleCount == 0 ? 14.0F : visibleCount * 14.0F)
                                    + (chatScreen == null ? 0.0F : 19.0F)
                                    + (!partyManager.getInvites().isEmpty() ? 14.0F : 0.0F)
                                    + (!partyManager.getJoinRequests().isEmpty() ? 14.0F : 0.0F);
                    float membersHeight =
                            13.0F
                                    + Math.max(1, partyManager.getMembers().size()) * 19.0F
                                    + (this.selectedPlayerId == null ? 0.0F : 33.0F);
                    float hudHeight = 24.0F + Math.max(messagesHeight, membersHeight) + 6.0F;
                    this.draggable.setHeight(hudHeight);
                    if (Effects.isPartyHudBlur()) {
                        render2DEvent
                                .getRenderEffects()
                                .blurRoundedRectangle(
                                        hudX,
                                        hudY,
                                        252.0F * hudScale,
                                        hudHeight * hudScale,
                                        6.0F * hudScale);
                    }

                    render2DEvent.getContext().pushPose();
                    render2DEvent.getContext().translate(hudX, hudY);
                    render2DEvent.getContext().scale(hudScale, hudScale, 1.0F);
                    Render2DContext backgroundContext = render2DEvent.getContext();
                    RoundedRectShape backgroundShape =
                            RoundedRectShape.obtain()
                                    .setBounds(0.0F, 0.0F, 252.0F, hudHeight)
                                    .setRadius(6.0F)
                                    .setShadowEnabled(Effects.isPartyHudShadow());

                    backgroundContext.drawRect(
                            backgroundShape.setColor(new Color(18, 18, 22, backgroundAlpha)));
                    Render2DContext headerContext = render2DEvent.getContext();
                    RoundedRectShape headerShape =
                            RoundedRectShape.obtain()
                                    .setBounds(0.0F, 0.0F, 252.0F, 18.0F)
                                    .setCornerRadii(6.0F, 6.0F, 0.0F, 0.0F);

                    headerContext.drawRect(
                            headerShape.setColor(new Color(28, 28, 34, backgroundAlpha)));
                    Render2DContext titleContext = render2DEvent.getContext();
                    TextDrawRequest titleRequest = TextDrawRequest.obtain();
                    titleContext.drawText(
                            titleRequest
                                    .setText("Party")
                                    .setPosition(8.0F, 4.0F)
                                    .setFont(titleFont.getMsdfFont())
                                    .setFontSize(titleFont.getSize())
                                    .enableShadow()
                                    .setColor(color.getRGB()));
                    Render2DContext hintContext = render2DEvent.getContext();
                    TextDrawRequest hintRequest = TextDrawRequest.obtain();
                    TextDrawRequest hintRequestWithText = hintRequest.setText("P to chat");

                    hintContext.drawText(
                            hintRequestWithText
                                    .setPosition(
                                            244.0F - smallFont.getStringWidth("P to chat"), 0.0F)
                                    .centerVertically(18.0F)
                                    .setFont(smallFont.getMsdfFont())
                                    .setFontSize(smallFont.getSize())
                                    .enableShadow()
                                    .setColor(color2.getRGB()));
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setText("−")
                                            .setPosition(188.0F, 0.0F)
                                            .centerInBox(16.0F, 18.0F)
                                            .setFont(titleFont.getMsdfFont())
                                            .setFontSize(titleFont.getSize())
                                            .enableShadow()
                                            .setColor(color2.getRGB()));
                    render2DEvent
                            .getContext()
                            .drawText(
                                    TextDrawRequest.obtain()
                                            .setText("×")
                                            .setPosition(204.0F, 0.0F)
                                            .centerInBox(16.0F, 18.0F)
                                            .setFont(titleFont.getMsdfFont())
                                            .setFontSize(titleFont.getSize())
                                            .enableShadow()
                                            .setColor(color2.getRGB()));
                    Render2DContext dividerContext = render2DEvent.getContext();
                    RoundedRectShape dividerShape =
                            RoundedRectShape.obtain()
                                    .setBounds(173.0F, 24.0F, 1.0F, hudHeight - 30.0F);

                    dividerContext.drawRect(dividerShape.setColor(new Color(255, 255, 255, 18)));
                    float currentY = 24.0F;
                    if (visibleCount == 0) {
                        Render2DContext emptyContext = render2DEvent.getContext();
                        TextDrawRequest emptyRequest = TextDrawRequest.obtain();
                        String emptyMessage;
                        if (partyManager.isInParty()) {
                            emptyMessage = "No party messages yet";
                        } else {
                            emptyMessage = "Accept an invitation to join";
                        }

                        emptyContext.drawText(
                                emptyRequest
                                        .setText(emptyMessage)
                                        .setPosition(8.0F, currentY)
                                        .centerVertically(14.0F)
                                        .setFont(textFont.getMsdfFont())
                                        .setFontSize(textFont.getSize())
                                        .enableShadow()
                                        .setColor(color2.getRGB()));
                        currentY += 14.0F;
                    } else {
                        for (int messageIndex = startIndex;
                                messageIndex < endIndex;
                                messageIndex++) {
                            PartyMessage message = partyManager.getMessages().get(messageIndex);
                            Render2DContext rowContext = render2DEvent.getContext();
                            RoundedRectShape rowShape =
                                    RoundedRectShape.obtain()
                                            .setBounds(6.0F, currentY, 163.0F, 12.0F)
                                            .setRadius(3.0F);

                            rowContext.drawRect(rowShape.setColor(new Color(255, 255, 255, 7)));
                            this.drawAvatar(
                                    render2DEvent.getContext(),
                                    message.getMinecraftUuid(),
                                    8.0F,
                                    currentY + 1.0F,
                                    10.0F,
                                    smallFont,
                                    message.getSenderName());
                            this.headRegions.add(
                                    new PartyHeadRegion(
                                            message.getMinecraftUuid(),
                                            hudX + 8.0F * hudScale,
                                            hudY + (currentY + 1.0F) * hudScale,
                                            10.0F * hudScale));
                            String senderName = message.getSenderName();
                            String senderLabel = senderName + ": ";
                            render2DEvent
                                    .getContext()
                                    .drawText(
                                            TextDrawRequest.obtain()
                                                    .setText(senderLabel)
                                                    .setPosition(21.0F, currentY + 2.0F)
                                                    .setFont(labelFont.getMsdfFont())
                                                    .setFontSize(labelFont.getSize())
                                                    .enableShadow()
                                                    .setColor(
                                                            ColorUtil.animatedThemeColor(
                                                                            hudY + currentY)
                                                                    .getRGB()));
                            render2DEvent
                                    .getContext()
                                    .drawText(
                                            TextDrawRequest.obtain()
                                                    .setText(
                                                            textFont.truncateWithEllipsis(
                                                                    message.getMessage(),
                                                                    143.0F
                                                                            - labelFont
                                                                                    .getStringWidth(
                                                                                            senderLabel)))
                                                    .setPosition(
                                                            21.0F
                                                                    + labelFont.getStringWidth(
                                                                            senderLabel),
                                                            currentY + 2.0F)
                                                    .setFont(textFont.getMsdfFont())
                                                    .setFontSize(textFont.getSize())
                                                    .enableShadow()
                                                    .setColor(color2.getRGB()));
                            currentY += 14.0F;
                        }
                    }

                    if (!partyManager.getInvites().isEmpty()) {
                        PartyInvite invite = partyManager.getInvites().getFirst();
                        Render2DContext inviteContext = render2DEvent.getContext();
                        TextDrawRequest inviteRequest = TextDrawRequest.obtain();
                        String leaderName = invite.getLeaderName();

                        inviteContext.drawText(
                                inviteRequest
                                        .setText(
                                                textFont.truncateWithEllipsis(
                                                        "Invite from " + leaderName, 119.0F))
                                        .setPosition(8.0F, currentY)
                                        .centerVertically(14.0F)
                                        .setFont(textFont.getMsdfFont())
                                        .setFontSize(textFont.getSize())
                                        .enableShadow()
                                        .setColor(color3.getRGB()));
                        Render2DContext acceptBackgroundContext = render2DEvent.getContext();
                        RoundedRectShape acceptShape =
                                RoundedRectShape.obtain()
                                        .setBounds(132.0F, currentY + 2.0F, 35.0F, 10.0F)
                                        .setRadius(3.0F);
                        int red = color3.getRed();
                        int green = color3.getGreen();
                        int blue = color3.getBlue();

                        acceptBackgroundContext.drawRect(
                                acceptShape.setColor(new Color(red, green, blue, 45)));
                        Render2DContext acceptTextContext = render2DEvent.getContext();
                        TextDrawRequest acceptTextRequest = TextDrawRequest.obtain();
                        acceptTextContext.drawText(
                                acceptTextRequest
                                        .setText("Accept")
                                        .setPosition(132.0F, currentY + 2.0F)
                                        .centerInBox(35.0F, 10.0F)
                                        .setFont(smallFont.getMsdfFont())
                                        .setFontSize(smallFont.getSize())
                                        .enableShadow()
                                        .setColor(color3.getRGB()));

                        this.actionRegions.add(
                                new PartyActionRegion(
                                        "ACCEPT_INVITE",
                                        invite.getPartyId(),
                                        hudX + 132.0F * hudScale,
                                        hudY + (currentY + 2.0F) * hudScale,
                                        35.0F * hudScale,
                                        10.0F * hudScale));
                        currentY += 14.0F;
                    }

                    if (!partyManager.getJoinRequests().isEmpty()) {
                        String requesterName = partyManager.getJoinRequests().getFirst();
                        Render2DContext requestContext = render2DEvent.getContext();
                        TextDrawRequest requestRequest = TextDrawRequest.obtain();

                        requestContext.drawText(
                                requestRequest
                                        .setText(
                                                textFont.truncateWithEllipsis(
                                                        requesterName + " wants to join", 119.0F))
                                        .setPosition(8.0F, currentY)
                                        .centerVertically(14.0F)
                                        .setFont(textFont.getMsdfFont())
                                        .setFontSize(textFont.getSize())
                                        .enableShadow()
                                        .setColor(color3.getRGB()));
                        Render2DContext acceptBackgroundContext = render2DEvent.getContext();
                        RoundedRectShape acceptShape =
                                RoundedRectShape.obtain()
                                        .setBounds(132.0F, currentY + 2.0F, 35.0F, 10.0F)
                                        .setRadius(3.0F);
                        int red = color3.getRed();
                        int green = color3.getGreen();
                        int blue = color3.getBlue();

                        acceptBackgroundContext.drawRect(
                                acceptShape.setColor(new Color(red, green, blue, 45)));
                        Render2DContext acceptTextContext = render2DEvent.getContext();
                        TextDrawRequest acceptTextRequest = TextDrawRequest.obtain();
                        acceptTextContext.drawText(
                                acceptTextRequest
                                        .setText("Accept")
                                        .setPosition(132.0F, currentY + 2.0F)
                                        .centerInBox(35.0F, 10.0F)
                                        .setFont(smallFont.getMsdfFont())
                                        .setFontSize(smallFont.getSize())
                                        .enableShadow()
                                        .setColor(color3.getRGB()));

                        this.actionRegions.add(
                                new PartyActionRegion(
                                        "ACCEPT_REQUEST",
                                        requesterName,
                                        hudX + 132.0F * hudScale,
                                        hudY + (currentY + 2.0F) * hudScale,
                                        35.0F * hudScale,
                                        10.0F * hudScale));
                        currentY += 14.0F;
                    }

                    if (chatScreen != null) {
                        Render2DContext inputContext = render2DEvent.getContext();
                        RoundedRectShape inputShape =
                                RoundedRectShape.obtain()
                                        .setBounds(6.0F, currentY + 1.0F, 163.0F, 15.0F)
                                        .setRadius(4.0F);

                        inputContext.drawRect(inputShape.setColor(new Color(255, 255, 255, 14)));
                        Render2DContext inputTextContext = render2DEvent.getContext();
                        TextDrawRequest inputTextRequest = TextDrawRequest.obtain();
                        String inputText =
                                textFont.truncateWithEllipsis(chatScreen.getInputText(), 148.0F);
                        blinkPhase = System.currentTimeMillis();

                        blinkPhase /= 500L;

                        inputTextContext.drawText(
                                inputTextRequest
                                        .setText(inputText + (blinkPhase % 2L == 0L ? "|" : ""))
                                        .setPosition(11.0F, currentY + 5.0F)
                                        .setFont(textFont.getMsdfFont())
                                        .setFontSize(textFont.getSize())
                                        .enableShadow()
                                        .setColor(color.getRGB()));
                    }

                    Render2DContext membersContext = render2DEvent.getContext();
                    TextDrawRequest membersRequest = TextDrawRequest.obtain();
                    memberCount = partyManager.getMembers().size();

                    membersContext.drawText(
                            membersRequest
                                    .setText("Members (" + memberCount + ")")
                                    .setPosition(178.0F, 23.0F)
                                    .setFont(labelFont.getMsdfFont())
                                    .setFontSize(labelFont.getSize())
                                    .enableShadow()
                                    .setColor(color.getRGB()));
                    float memberY = 35.0F;

                    for (PartyMember member : partyManager.getMembers()) {
                        Color color6 = new Color(member.getColor(), true);
                        this.drawAvatar(
                                render2DEvent.getContext(),
                                member.getMinecraftUuid(),
                                178.0F,
                                memberY,
                                14.0F,
                                smallFont,
                                member.getBackendName());
                        this.headRegions.add(
                                new PartyHeadRegion(
                                        member.getMinecraftUuid(),
                                        hudX + 178.0F * hudScale,
                                        hudY + memberY * hudScale,
                                        14.0F * hudScale));
                        render2DEvent
                                .getContext()
                                .drawText(
                                        TextDrawRequest.obtain()
                                                .setText(
                                                        labelFont.truncateWithEllipsis(
                                                                member.getBackendName(),
                                                                member.isLeader() ? 38.0F : 49.0F))
                                                .setPosition(196.0F, memberY)
                                                .setFont(labelFont.getMsdfFont())
                                                .setFontSize(labelFont.getSize())
                                                .enableShadow()
                                                .setColor(color6.getRGB()));
                        Render2DContext statusContext = render2DEvent.getContext();
                        TextDrawRequest statusRequest = TextDrawRequest.obtain();
                        String statusText;
                        if (member.getMinecraftName().isBlank()) {

                            statusText = "Not in world";
                        } else {
                            statusText = member.getMinecraftName();
                        }

                        statusContext.drawText(
                                statusRequest
                                        .setText(smallFont.truncateWithEllipsis(statusText, 49.0F))
                                        .setPosition(196.0F, memberY + 8.0F)
                                        .setFont(smallFont.getMsdfFont())
                                        .setFontSize(smallFont.getSize())
                                        .enableShadow()
                                        .setColor((member.isOnline() ? color3 : color4).getRGB()));
                        if (member.isLeader()) {
                            Render2DContext badgeContext = render2DEvent.getContext();
                            RoundedRectShape badgeShape =
                                    RoundedRectShape.obtain()
                                            .setBounds(238.0F, memberY - 1.0F, 10.0F, 9.0F)
                                            .setRadius(3.0F);
                            int red = color5.getRed();
                            int green = color5.getGreen();
                            int blue = color5.getBlue();

                            badgeContext.drawRect(
                                    badgeShape.setColor(new Color(red, green, blue, 45)));
                            render2DEvent
                                    .getContext()
                                    .drawText(
                                            TextDrawRequest.obtain()
                                                    .setText("L")
                                                    .setPosition(238.0F, memberY - 1.0F)
                                                    .centerInBox(10.0F, 9.0F)
                                                    .setFont(smallFont.getMsdfFont())
                                                    .setFontSize(smallFont.getSize())
                                                    .enableShadow()
                                                    .setColor(color5.getRGB()));
                        }

                        memberY += 19.0F;
                    }

                    if (this.selectedPlayerId != null) {
                        this.drawAvatar(
                                render2DEvent.getContext(),
                                this.selectedPlayerId.toString(),
                                178.0F,
                                memberY,
                                28.0F,
                                titleFont,
                                "?");
                        Render2DContext playerHeadContext = render2DEvent.getContext();
                        TextDrawRequest playerHeadRequest = TextDrawRequest.obtain();
                        playerHeadContext.drawText(
                                playerHeadRequest
                                        .setText("Player head")
                                        .setPosition(210.0F, memberY + 9.0F)
                                        .setFont(smallFont.getMsdfFont())
                                        .setFontSize(smallFont.getSize())
                                        .enableShadow()
                                        .setColor(color2.getRGB()));
                    }

                    render2DEvent.getContext().popPose();
                }
            }
        }
    }

    public void handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float hudScale = this.draggable.getScale();
            if (RenderUtil.isInsideRect(
                    mouseX,
                    mouseY,
                    this.draggable.getX() + (this.collapsed ? 236.0F : 204.0F) * hudScale,
                    this.draggable.getY(),
                    16.0F * hudScale,
                    18.0F * hudScale)) {
                this.dismissed = true;
                if (mc.gui.screen() instanceof PartyChatScreen) {
                    mc.gui.setScreen(null);
                }
            } else if (RenderUtil.isInsideRect(
                    mouseX,
                    mouseY,
                    this.draggable.getX() + (this.collapsed ? 220.0F : 188.0F) * hudScale,
                    this.draggable.getY(),
                    16.0F * hudScale,
                    18.0F * hudScale)) {
                this.collapsed = !this.collapsed;
            } else {
                for (PartyActionRegion actionRegion : this.actionRegions) {
                    if (RenderUtil.isInsideRect(
                            mouseX,
                            mouseY,
                            actionRegion.x(),
                            actionRegion.y(),
                            actionRegion.width(),
                            actionRegion.height())) {
                        Astralis.getInstance()
                                .getPartyManager()
                                .sendPartyAction(actionRegion.action(), actionRegion.target(), "");
                        return;
                    }
                }

                for (PartyHeadRegion headRegion : this.headRegions) {
                    if (RenderUtil.isInsideRect(
                            mouseX,
                            mouseY,
                            headRegion.x(),
                            headRegion.y(),
                            headRegion.size(),
                            headRegion.size())) {
                        try {
                            this.selectedPlayerId =
                                    headRegion.playerId().isBlank()
                                            ? null
                                            : UUID.fromString(headRegion.playerId());
                        } catch (IllegalArgumentException ignored) {
                            this.selectedPlayerId = null;
                        }

                        return;
                    }
                }
            }
        }
    }

    private void drawAvatar(
            Render2DContext context,
            String playerId,
            float x,
            float y,
            float size,
            SizedFont font,
            String displayName) {
        Identifier identifier = null;

        try {
            if (!playerId.isBlank()) {
                UUID uuid = UUID.fromString(playerId);
                AbstractClientPlayer abstractClientPlayer =
                        (AbstractClientPlayer) mc.level.getPlayerByUUID(uuid);
                if (abstractClientPlayer != null) {
                    identifier = abstractClientPlayer.getSkin().body().texturePath();
                } else {
                    PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(uuid);
                    if (playerInfo != null) {
                        identifier = playerInfo.getSkin().body().texturePath();
                    }
                }

                if (identifier == null) {
                    identifier = this.skinTextureCache.get(uuid);
                } else {
                    this.skinTextureCache.put(uuid, identifier);
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        if (identifier == null) {
            RoundedRectShape placeholderShape =
                    RoundedRectShape.obtain().setBounds(x, y, size, size).setRadius(2.0F);

            context.drawRect(placeholderShape.setColor(new Color(67, 57, 91)));
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(displayName.isBlank() ? "?" : displayName.substring(0, 1))
                            .setPosition(x, y)
                            .centerInBox(size, size)
                            .setFont(font.getMsdfFont())
                            .setFontSize(font.getSize())
                            .enableShadow()
                            .setColor(Color.WHITE.getRGB()));
        } else {
            GpuTexture gpuTexture = mc.getTextureManager().getTexture(identifier).getTexture();
            float pixelScale = size / 8.0F;
            float skinSize = 64.0F * pixelScale;
            float skinOriginX = x - 8.0F * pixelScale;
            float skinOriginY = y - 8.0F * pixelScale;
            context.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds(x, y, size, size)
                            .setTexture(gpuTexture)
                            .useNearestFilter()
                            .setUv(
                                    (x - skinOriginX) / skinSize,
                                    (y - skinOriginY) / skinSize,
                                    (x + size - skinOriginX) / skinSize,
                                    (y + size - skinOriginY) / skinSize)
                            .setRadius(2.0F)
                            .setColor(Color.WHITE));
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        long waypointAge;
        PartySettings partySettings =
                Astralis.getInstance().getModuleManager().getModule(PartySettings.class);
        if (partySettings.getSharedWaypoints().getValue()) {
            PartyManager partyManager = Astralis.getInstance().getPartyManager();
            PresenceManager presenceManager = Astralis.getInstance().getProfileManager();
            long nowMs = System.currentTimeMillis();
            this.waypointBeams.values().forEach(beam -> beam.active = false);

            for (PartyWaypoint waypoint : partyManager.getWaypoints()) {
                waypointAge = nowMs - waypoint.getTimestamp();

                if (waypointAge < 5000L) {
                    this.waypointBeams.computeIfAbsent(
                                            waypoint.getId(),
                                            key -> new WaypointFadeState(waypoint))
                                    .active =
                            true;
                }
            }

            Iterator iterator = this.waypointBeams.values().iterator();

            while (iterator.hasNext()) {
                WaypointFadeState fadeState = (WaypointFadeState) iterator.next();
                float fadeDelta =
                        (float) (nowMs - fadeState.lastUpdateTime)
                                / (fadeState.active ? 725.351F : 824.807F);
                fadeState.fade =
                        Math.clamp(
                                fadeState.fade + (fadeState.active ? fadeDelta : -fadeDelta),
                                0.0F,
                                1.0F);
                fadeState.lastUpdateTime = nowMs;
                if (!fadeState.active && fadeState.fade == 0.0F) {
                    iterator.remove();
                } else {
                    PartyWaypoint beamWaypoint = fadeState.waypoint;
                    if ((beamWaypoint
                                            .getServerAddress()
                                            .equalsIgnoreCase(presenceManager.getServerAddress())
                                    || !beamWaypoint.getServerFingerprint().isBlank()
                                            && beamWaypoint
                                                    .getServerFingerprint()
                                                    .equals(presenceManager.getServerFingerprint()))
                            && beamWaypoint.getDimension().equals(presenceManager.getDimension())) {
                        Color color = new Color(beamWaypoint.getColor(), true);
                        RenderUtil.drawBox(
                                render3DEvent.getPoseStack(),
                                new AABB(
                                        beamWaypoint.getX() - 0.28,
                                        beamWaypoint.getY(),
                                        beamWaypoint.getZ() - 0.28,
                                        beamWaypoint.getX() + 0.28,
                                        beamWaypoint.getY() + 256.0,
                                        beamWaypoint.getZ() + 0.28),
                                new Color(
                                        color.getRed(),
                                        color.getGreen(),
                                        color.getBlue(),
                                        Math.round(28.0F * fadeState.fade)),
                                partySettings.getWaypointsThroughBlocks().getValue());
                        RenderUtil.drawBox(
                                render3DEvent.getPoseStack(),
                                new AABB(
                                        beamWaypoint.getX() - 0.11,
                                        beamWaypoint.getY(),
                                        beamWaypoint.getZ() - 0.11,
                                        beamWaypoint.getX() + 0.11,
                                        beamWaypoint.getY() + 256.0,
                                        beamWaypoint.getZ() + 0.11),
                                new Color(
                                        color.getRed(),
                                        color.getGreen(),
                                        color.getBlue(),
                                        Math.round(125.0F * fadeState.fade)),
                                partySettings.getWaypointsThroughBlocks().getValue());
                        RenderUtil.drawBox(
                                render3DEvent.getPoseStack(),
                                new AABB(
                                        beamWaypoint.getX() - 0.35,
                                        beamWaypoint.getY() - 0.02,
                                        beamWaypoint.getZ() - 0.35,
                                        beamWaypoint.getX() + 0.35,
                                        beamWaypoint.getY() + 0.12,
                                        beamWaypoint.getZ() + 0.35),
                                new Color(
                                        color.getRed(),
                                        color.getGreen(),
                                        color.getBlue(),
                                        Math.round(170.0F * fadeState.fade)),
                                partySettings.getWaypointsThroughBlocks().getValue());
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent playerUpdateEvent) {
        boolean pickKeyDown = mc.options.keyPickItem.isDown();
        PartySettings partySettings =
                Astralis.getInstance().getModuleManager().getModule(PartySettings.class);
        PartyManager partyManager = Astralis.getInstance().getPartyManager();
        if (pickKeyDown
                && !this.pickKeyHeld
                && partySettings.getSharedWaypoints().getValue()
                && partyManager.isInParty()) {
            this.pickKeyHeld = true;
            HitResult hitResult =
                    mc.player.pick(
                            512.0, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
            if (hitResult.getType() == Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
                Vec3 vector3 =
                        new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                PresenceManager presenceManager = Astralis.getInstance().getProfileManager();
                PartyWaypoint closestWaypoint = null;
                double closestDistanceSq = 4.0;

                for (PartyWaypoint waypoint : partyManager.getWaypoints()) {
                    if ((waypoint.getServerAddress()
                                            .equalsIgnoreCase(presenceManager.getServerAddress())
                                    || !waypoint.getServerFingerprint().isBlank()
                                            && waypoint.getServerFingerprint()
                                                    .equals(presenceManager.getServerFingerprint()))
                            && waypoint.getDimension().equals(presenceManager.getDimension())) {
                        double distanceSq =
                                vector3.distanceToSqr(
                                        waypoint.getX(), waypoint.getY(), waypoint.getZ());
                        if (!(distanceSq > closestDistanceSq)) {
                            closestWaypoint = waypoint;
                            closestDistanceSq = distanceSq;
                        }
                    }
                }

                if (closestWaypoint != null) {

                    partyManager.sendPartyAction("REMOVE_WAYPOINT", closestWaypoint.getId(), "");
                } else {

                    partyManager.addWaypoint("Waypoint", vector3.x, vector3.y, vector3.z);
                }
            }
        } else {
            this.pickKeyHeld = pickKeyDown;
        }
    }

    private boolean isHudVisible() {
        return !this.dismissed
                || mc.gui.screen() instanceof ChatScreen
                || mc.gui.screen() instanceof PartyChatScreen;
    }

    public boolean handleMouseScrolled(double scrollDelta) {
        if (scrollDelta != 0.0 && !this.collapsed) {
            int messageCount = Astralis.getInstance().getPartyManager().getMessages().size();

            int maxScrollOffset = Math.max(0, messageCount - 7);
            if (maxScrollOffset == 0) {
                return false;
            }

            this.messageScrollOffset =
                    Math.clamp(
                            this.messageScrollOffset + (scrollDelta > 0.0 ? 1 : -1),
                            0,
                            maxScrollOffset);
            return true;
        } else {
            return false;
        }
    }

    @EventTarget
    public void onInGameKeyPress(InGameKeyPressEvent inGameKeyPressEvent) {
        PartyManager partyManager = Astralis.getInstance().getPartyManager();
        int keyCode = inGameKeyPressEvent.getKey();

        if (keyCode == 80 && (partyManager.isInParty() || !partyManager.getInvites().isEmpty())) {
            this.collapsed = false;
            mc.gui.setScreen(new PartyChatScreen());
        }
    }
}
