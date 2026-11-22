package astralis.ui.screen;

import astralis.Astralis;
import astralis.account.Account;
import astralis.account.AccountManager;
import astralis.account.AccountType;
import astralis.account.MicrosoftAuth;
import astralis.account.MicrosoftClientsInitialization;
import astralis.account.SessionLoginUtil;
import astralis.resourcepack.migration.CookieFileImporter;
import astralis.social.model.MicrosoftAuthResult;
import astralis.social.model.ParsedTokens;
import astralis.ui.animation.Animation;
import astralis.ui.component.SapphireScreen;
import astralis.ui.input.TextInputUtil;
import astralis.util.Easing;
import astralis.util.render.AltsLogoRenderer;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShadowRenderer;
import astralis.util.render.ShapePaint;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AltManagerScreen extends Screen implements InstanceAccess, SapphireScreen {
    private static final float PANEL_MARGIN = 10.0F;
    private static final float MAIN_PANEL_WIDTH_RATIO = 0.78F;
    private static final float ROW_HEIGHT = 34.0F;
    private static final float ROW_GAP = 5.0F;
    private static final float MAX_CONTENT_WIDTH = 420.0F;
    private static final float LIST_GAP = 18.0F;
    private static final float HEADER_HEIGHT = 38.0F;
    private static final float FOOTER_HEIGHT = 22.0F;
    private static final float CONTROL_HEIGHT = 20.0F;
    private static final float CONTROL_GAP = 4.0F;
    private static final float ROW_PADDING = 4.0F;
    private static final float CONTROL_RADIUS = 5.0F;
    private static Color glassColor;
    private static Color glassHoverColor;
    private static Color rowColor;
    private static Color rowHoverColor;
    private static Color rowSelectedColor;
    private final Color mutedTextColor;
    private final Color secondaryTextColor;
    private static Map<UUID, PlayerSkin> skinCache;
    private final Screen parentScreen;
    private final Animation scrollAnimation;
    private SizedFont rowNameFont;
    private SizedFont rowHintFont;
    private SizedFont badgeFont;
    private SizedFont emptyStateFont;
    private SizedFont hintFont;
    private SizedFont titleFont;
    private SizedFont currentUserFont;
    private SizedFont statusFont;
    private SizedFont buttonFont;
    private int lastAccountCount;
    private String accountCountText;
    private volatile String statusMessage;
    private Account selectedAccount;
    private AccountType addAccountType;
    private boolean addDialogOpen;
    private String crackedUsername;
    private boolean usernameFieldFocused;
    private final Object textInputOwner;
    private float scrollOffset;
    private float scrollbarGrabOffset;
    private boolean draggingScrollbar;
    private final Button[] actionButtons;
    private GLFWDropCallback dropCallback;
    private volatile int lastMouseX;
    private volatile int lastMouseY;
    private float mainPanelX;
    private float mainPanelY;
    private float mainPanelWidth;
    private float mainPanelHeight;
    private float sidePanelX;
    private float sidePanelY;
    private float sidePanelWidth;
    private float sidePanelHeight;
    private float listTop;
    private float listHeight;

    public AltManagerScreen(Screen screen) {
        super(Component.empty());

        this.mutedTextColor = new Color(110, 110, 110);

        this.secondaryTextColor = new Color(160, 160, 160);

        this.scrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 500L);
        this.lastAccountCount = -1;
        this.accountCountText = "";

        this.statusMessage = "Idle";
        this.addAccountType = AccountType.COOKIE;
        this.crackedUsername = "";
        this.textInputOwner = new Object();
        this.actionButtons = new Button[4];
        this.parentScreen = screen;
    }

    private float getContentCenterX() {
        return this.getContentX() + this.getContentWidth() / 2.0F;
    }

    private void drawDialogButton(
            Render2DContext context,
            float x,
            float y,
            float width,
            float height,
            String label,
            int mouseX,
            int mouseY) {
        this.drawGlass(
                context,
                x,
                y,
                width,
                height,
                5.0F,
                this.isPointInRect(mouseX, mouseY, x, y, width, height));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(label)
                        .setPosition(x, y)
                        .centerInBox(width, height)
                        .setFont(this.buttonFont.getMsdfFont())
                        .setFontSize(this.buttonFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
    }

    private void drawUsernameInput(
            Render2DContext context,
            float x,
            float y,
            float width,
            float height,
            int mouseX,
            int mouseY) {
        long blinkTick;
        this.drawGlass(
                context,
                x,
                y,
                width,
                height,
                5.0F,
                this.isPointInRect(mouseX, mouseY, x, y, width, height));
        boolean usernameEmpty = this.crackedUsername.isEmpty();
        TextDrawRequest textRequest = TextDrawRequest.obtain();
        String displayText;
        if (usernameEmpty) {

            displayText = "Username";
        } else {
            displayText = this.crackedUsername;
        }

        context.drawText(
                textRequest
                        .setText(displayText)
                        .setPosition(x + 6.0F, y)
                        .centerVertically(height)
                        .setFont(this.buttonFont.getMsdfFont())
                        .setFontSize(this.buttonFont.getSize())
                        .enableShadow()
                        .setColor((usernameEmpty ? this.mutedTextColor : Color.WHITE).getRGB()));
        if (this.usernameFieldFocused) {
            blinkTick = System.currentTimeMillis();
            blinkTick /= 700L;
            if (blinkTick % 2L == 0L) {
                float cursorX =
                        x
                                + 6.0F
                                + (usernameEmpty
                                        ? 0.0F
                                        : this.buttonFont.getStringWidth(this.crackedUsername)
                                                + 1.0F);
                float cursorHeight = Math.min(this.buttonFont.getLineHeight(), height - 8.0F);
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        cursorX,
                                        y + (height - cursorHeight) / 2.0F,
                                        1.0F,
                                        cursorHeight)
                                .setRadius(0.5F)
                                .setColor(Color.WHITE));
                return;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.mainPanelX = 10.0F;
        this.mainPanelY = 10.0F;
        this.mainPanelWidth = this.width * 0.78F;
        this.mainPanelHeight = this.height - 20.0F;
        this.sidePanelX = this.mainPanelX + this.mainPanelWidth + 10.0F;
        this.sidePanelY = 10.0F;
        this.sidePanelWidth = this.width - this.sidePanelX - 10.0F;
        this.sidePanelHeight = this.height - 20.0F;
        this.listTop = this.mainPanelY + 38.0F + 18.0F;
        this.listHeight = this.mainPanelHeight - 38.0F - 22.0F - 18.0F;

        this.rowNameFont = FontManager.getFont("Product Sans Bold", 10.0F);

        this.rowHintFont = FontManager.getFont("Product Sans Regular", 8.0F);

        this.badgeFont = FontManager.getFont("Product Sans Regular", 9.0F);

        this.emptyStateFont = FontManager.getFont("Product Sans Regular", 9.0F);

        this.hintFont = FontManager.getFont("Product Sans Regular", 7.0F);

        this.titleFont = FontManager.getFont("Product Sans Bold", 14.0F);

        this.currentUserFont = FontManager.getFont("Product Sans Regular", 9.0F);

        this.statusFont = FontManager.getFont("Product Sans Regular", 8.0F);

        this.buttonFont = FontManager.getFont("Product Sans Medium", 9.0F);
        String[] labelArray = new String[4];
        int buttonWidth;

        labelArray[0] = "Login";

        labelArray[1] = "Add";

        labelArray[2] = "Remove";

        labelArray[3] = "Remove All";
        String[] labels = labelArray;

        for (int buttonIndex = 0; buttonIndex < labels.length; buttonIndex++) {
            int capturedIndex = buttonIndex;
            Builder buttonBuilder =
                    Button.builder(
                            Component.literal(labels[buttonIndex]),
                            button2 -> this.onActionButton(capturedIndex));
            int buttonX = (int) this.getActionButtonX(buttonIndex);
            int buttonY = (int) this.getActionButtonY(buttonIndex);
            buttonWidth = (int) this.getActionButtonWidth();

            Button button3 = buttonBuilder.bounds(buttonX, buttonY, buttonWidth, 20).build();
            button3.setAlpha(0.0F);
            this.actionButtons[buttonIndex] = button3;
            this.addRenderableWidget(button3);
        }

        if (this.dropCallback != null) {
            GLFW.glfwSetDropCallback(mc.getWindow().handle(), null);
            this.dropCallback.free();
        }

        this.dropCallback =
                GLFWDropCallback.create(
                        (window, count, names) -> {
                            ArrayList filePaths = new ArrayList();

                            for (int i = 0; i < count; i++) {
                                long address =
                                        MemoryUtil.memGetAddress(
                                                names + (long) i * Pointer.POINTER_SIZE);
                                String pathText = MemoryUtil.memUTF8(address);
                                filePaths.addAll(
                                        CookieFileImporter.parseFilePathsFromText(pathText));
                            }

                            if (!filePaths.isEmpty()) {
                                mc.execute(() -> this.importCookieFiles(filePaths));
                            }
                        });
        GLFW.glfwSetDropCallback(mc.getWindow().handle(), this.dropCallback);
    }

    @Override
    public void removed() {
        if (this.dropCallback != null) {
            GLFW.glfwSetDropCallback(mc.getWindow().handle(), null);
            this.dropCallback.free();
            this.dropCallback = null;
        }

        super.removed();
    }

    @Override
    public void onClose() {
        mc.gui.setScreen(this.parentScreen);
    }

    private boolean isPointInRect(
            double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {

        if (event.button() == 0 && this.addDialogOpen) {
            double clickX = event.x();
            double clickY = event.y();
            float dialogX = this.getDialogX();
            float dialogY = this.getDialogY();
            float dialogWidth = this.getDialogWidth();
            if (this.isInsideDialog(clickX, clickY)) {
                if (this.isPointInRect(
                        clickX,
                        clickY,
                        dialogX + 8.0F,
                        dialogY + 24.0F,
                        dialogWidth - 16.0F,
                        20.0F)) {
                    this.cycleAccountType();
                    return true;
                }

                if (this.isPointInRect(
                        clickX,
                        clickY,
                        dialogX + 8.0F,
                        dialogY + 49.0F,
                        dialogWidth - 16.0F,
                        20.0F)) {
                    if (this.addAccountType == AccountType.CRACKED) {
                        this.usernameFieldFocused = true;
                        return true;
                    }

                    this.closeAddDialog();
                    this.addAccountFromClipboard();
                    return true;
                }

                this.blurUsernameInput();
                return true;
            }

            this.closeAddDialog();
        }

        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0
                && this.isPointInRect(
                        mouseX,
                        mouseY,
                        this.getNiceAltsButtonX(),
                        this.getNiceAltsButtonY(),
                        this.getActionButtonWidth(),
                        20.0F)) {
            mc.gui.setScreen(new NiceAltsScreen(this));
            return true;
        }

        if (super.mouseClicked(event, doubleClick)) {
            if (!this.isOverActionControl(event.x(), event.y())) {
                this.selectedAccount = null;
            }

            return true;
        } else {
            if (event.button() != 0) {
                return false;
            }

            if (this.isPointInRect(
                    mouseX,
                    mouseY,
                    this.getContentX(),
                    this.listTop,
                    this.getContentWidth(),
                    this.listHeight)) {
                List accounts = Astralis.getInstance().getAccountManager().getAccounts();
                float maxScroll = this.getMaxScroll(accounts.size());
                float trackY = this.listTop + 4.0F;
                float trackHeight = Math.max(0.0F, this.listHeight - 8.0F);
                if (maxScroll > 0.0F
                        && this.isPointInRect(
                                mouseX,
                                mouseY,
                                this.getContentX() + this.getContentWidth() - 9.0F,
                                trackY,
                                10.0F,
                                trackHeight)) {
                    float thumbHeight =
                            this.getScrollbarThumbHeight(
                                    trackHeight, this.getListContentHeight(accounts.size()));
                    float thumbY =
                            this.getScrollbarThumbY(
                                    trackY,
                                    trackHeight,
                                    maxScroll,
                                    Math.clamp(
                                            (float) this.scrollAnimation.getValue(),
                                            -maxScroll,
                                            0.0F));
                    this.scrollbarGrabOffset =
                            this.isPointInRect(
                                            mouseX,
                                            mouseY,
                                            this.getContentX() + this.getContentWidth() - 9.0F,
                                            thumbY,
                                            10.0F,
                                            thumbHeight)
                                    ? (float) mouseY - thumbY
                                    : thumbHeight / 2.0F;
                    this.draggingScrollbar = true;
                    this.scrollOffset =
                            this.getScrollOffsetForThumbDrag(
                                    (float) mouseY,
                                    this.scrollbarGrabOffset,
                                    trackY,
                                    trackHeight,
                                    thumbHeight,
                                    maxScroll);
                    this.scrollAnimation.setValue(this.scrollOffset);
                    return true;
                }

                float scrollPos =
                        Math.clamp((float) this.scrollAnimation.getValue(), -maxScroll, 0.0F);
                float relativeY = (float) mouseY - (this.listTop + 6.0F + scrollPos);
                int rowIndex = (int) Math.floor(relativeY / 39.0F);
                if (relativeY >= 0.0F
                        && rowIndex < accounts.size()
                        && relativeY - rowIndex * 39.0F <= 34.0F) {
                    Account clickedAccount = (Account) accounts.get(rowIndex);
                    this.selectedAccount = clickedAccount;
                    if (doubleClick) {

                        if (!"Logging in...".equals(this.statusMessage)) {
                            this.login(clickedAccount);
                        }
                    }

                    return true;
                }
            }

            this.selectedAccount = null;
            return false;
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        } else {
            return super.mouseReleased(event);
        }
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar && event.button() == 0) {
            int accountCount = Astralis.getInstance().getAccountManager().getAccounts().size();
            float maxScroll = this.getMaxScroll(accountCount);
            if (maxScroll <= 0.0F) {
                this.scrollOffset = 0.0F;
            } else {
                float trackHeight = Math.max(0.0F, this.listHeight - 8.0F);
                float thumbHeight =
                        this.getScrollbarThumbHeight(
                                trackHeight, this.getListContentHeight(accountCount));
                this.scrollOffset =
                        this.getScrollOffsetForThumbDrag(
                                (float) event.y(),
                                this.scrollbarGrabOffset,
                                this.listTop + 4.0F,
                                trackHeight,
                                thumbHeight,
                                maxScroll);
            }

            this.scrollAnimation.setValue(this.scrollOffset);
            return true;
        } else {
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isPointInRect(
                mouseX,
                mouseY,
                this.getContentX(),
                this.listTop,
                this.getContentWidth(),
                this.listHeight)) {
            int accountCount = Astralis.getInstance().getAccountManager().getAccounts().size();
            this.scrollOffset =
                    Math.clamp(
                            this.scrollOffset + (float) (scrollY * 20.0),
                            -this.getMaxScroll(accountCount),
                            0.0F);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }

    @Override
    public void renderSapphire(Render2DContext context) {

        int mouseX = this.lastMouseX;
        int mouseY = this.lastMouseY;
        int accountCount = Astralis.getInstance().getAccountManager().getAccounts().size();
        if (accountCount != this.lastAccountCount) {
            this.accountCountText = String.valueOf(accountCount);
            this.lastAccountCount = accountCount;
        }

        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();

        String currentUserName = mc.getUser().getName();
        List accounts = Astralis.getInstance().getAccountManager().getAccounts();
        float contentHeight = this.getListContentHeight(accounts.size());
        float maxScroll = Math.max(0.0F, contentHeight - this.listHeight);
        this.scrollOffset = Math.clamp(this.scrollOffset, -maxScroll, 0.0F);
        this.scrollAnimation.animateTo(this.scrollOffset);
        float scrollPos = Math.clamp((float) this.scrollAnimation.getValue(), -maxScroll, 0.0F);
        int firstVisibleIndex =
                Math.max(0, (int) Math.floor(Math.max(0.0F, -scrollPos) / 39.0F) - 1);
        int visibleCount = Math.max(1, (int) Math.ceil(this.listHeight / 39.0F) + 2);
        int endIndex = Math.min(accounts.size(), firstVisibleIndex + visibleCount);
        float contentX = this.getContentX();
        float contentWidth = this.getContentWidth();
        context.beginScissor(contentX, this.listTop, contentWidth, this.listHeight);

        for (int index = firstVisibleIndex; index < endIndex; index++) {
            Account account = (Account) accounts.get(index);
            float rowY = this.listTop + 6.0F + index * 39.0F + scrollPos;
            boolean selected = account == this.selectedAccount;
            float rowWidth = contentWidth;
            float rowX = contentX;
            boolean hovered = this.isPointInRect(mouseX, mouseY, rowX, rowY, rowWidth, 34.0F);
            float avatarX = rowX + 4.0F;
            float avatarY = rowY + 4.0F;
            float textX = avatarX + 26.0F + 8.0F;
            String typeLabel = account.getType().toString();
            float badgeWidth = this.badgeFont.getStringWidth(typeLabel) + 12.0F;
            float badgeX = rowX + rowWidth - badgeWidth - 9.0F;
            float badgeY = rowY + 9.0F;
            float shadowRowY = rowY;
            ShadowRenderer shadowRenderer = context.getShadowRenderer();
            Consumer<Render2DContext> shadowPainter =
                    shadowContext ->
                            shadowContext.drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(rowX, shadowRowY, rowWidth, 34.0F)
                                            .setRadius(8.0F)
                                            .setColor(Color.WHITE));

            Color color3 = new Color(0, 0, 0, 95);
            shadowRenderer.renderShapes(shadowPainter, color3, 8, 0.55F);
            Astralis.getInstance()
                    .getRenderEffects()
                    .drawBlurredRoundedRectangle(rowX, rowY, rowWidth, 34.0F, 8.0F, 1.0F);
            if (selected) {
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(rowX, rowY, rowWidth, 34.0F)
                                .setRadius(8.0F)
                                .setColor(rowSelectedColor));
            } else {
                context.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(rowX, rowY, rowWidth, 34.0F)
                                .setRadius(8.0F)
                                .setColor(hovered ? rowHoverColor : rowColor));
            }

            try {
                GpuTexture gpuTexture;
                try {
                    gpuTexture =
                            mc.getTextureManager()
                                    .getTexture(this.skin(account).body().texturePath())
                                    .getTexture();
                } catch (Exception ignored) {
                    gpuTexture =
                            mc.getTextureManager()
                                    .getTexture(DefaultPlayerSkin.getDefaultTexture())
                                    .getTexture();
                }

                context.drawTexture(
                        TexturedRectShape.obtain()
                                .setBounds(avatarX, avatarY, 26.0F, 26.0F)
                                .setTexture(gpuTexture)
                                .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                                .setRadius(5.0F)
                                .useNearestFilter()
                                .setColor(Color.WHITE));
            } catch (Exception ignored) {
            }

            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(account.getUsername())
                            .setPosition(textX, rowY + 5.0F)
                            .setFont(this.rowNameFont.getMsdfFont())
                            .setFontSize(this.rowNameFont.getSize())
                            .enableShadow()
                            .setColor(Color.WHITE.getRGB()));
            TextDrawRequest hintRequest = TextDrawRequest.obtain();
            context.drawText(
                    hintRequest
                            .setText("Double-click to login")
                            .setPosition(textX, rowY + 18.0F)
                            .setFont(this.rowHintFont.getMsdfFont())
                            .setFontSize(this.rowHintFont.getSize())
                            .enableShadow()
                            .setColor(this.mutedTextColor.getRGB()));
            Color color4;
            Color color5;
            switch (AccountTypeSwitchMap.accountTypeOrdinals[account.getType().ordinal()]) {
                case 1:
                    color4 = new Color(210, 150, 60);
                    color5 = new Color(210, 150, 60, 30);
                    break;
                case 2:
                    color4 = new Color(100, 190, 145);
                    color5 = new Color(100, 190, 145, 30);
                    break;
                case 3:
                    color4 = new Color(80, 160, 220);
                    color5 = new Color(80, 160, 220, 30);
                    break;
                case 4:
                    color4 = new Color(180, 180, 180);
                    color5 = new Color(180, 180, 180, 30);
                    break;
                default:
                    color4 = new Color(150, 120, 255);
                    color5 = new Color(150, 120, 255, 30);
            }

            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(badgeX, badgeY, badgeWidth, 16.0F)
                            .setRadius(4.0F)
                            .setColor(color5));
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(typeLabel)
                            .setPosition(
                                    badgeX
                                            + (badgeWidth
                                                            - this.badgeFont.getStringWidth(
                                                                    typeLabel))
                                                    / 2.0F,
                                    badgeY)
                            .centerVertically(16.0F)
                            .setFont(this.badgeFont.getMsdfFont())
                            .setFontSize(this.badgeFont.getSize())
                            .enableShadow()
                            .setColor(color4.getRGB()));
        }

        if (accounts.isEmpty()) {

            TextDrawRequest emptyRequest = TextDrawRequest.obtain();

            TextDrawRequest emptyTextRequest = emptyRequest.setText("No alts added yet");
            float emptyTextCenterX = this.getContentCenterX();

            context.drawText(
                    emptyTextRequest
                            .setPosition(
                                    emptyTextCenterX
                                            - this.emptyStateFont.getStringWidth(
                                                            "No alts added yet")
                                                    / 2.0F,
                                    this.listTop
                                            + this.listHeight / 2.0F
                                            - this.emptyStateFont.getLineHeight())
                            .setFont(this.emptyStateFont.getMsdfFont())
                            .setFontSize(this.emptyStateFont.getSize())
                            .enableShadow()
                            .setColor(this.mutedTextColor.getRGB()));
        }

        float trackHeight = Math.max(0.0F, this.listHeight - 8.0F);
        if (maxScroll > 0.0F && trackHeight > 0.0F) {
            float trackX = contentX + contentWidth - 5.0F;
            float trackY = this.listTop + 4.0F;
            float thumbHeight = this.getScrollbarThumbHeight(trackHeight, contentHeight);
            float thumbY = this.getScrollbarThumbY(trackY, trackHeight, maxScroll, scrollPos);
            RoundedRectShape trackShape =
                    RoundedRectShape.obtain()
                            .setBounds(trackX, trackY, 2.0F, trackHeight)
                            .setRadius(1.0F);
            int thumbRed;

            context.drawRect(trackShape.setColor(new Color(255, 255, 255, 18)));
            RoundedRectShape thumbShape =
                    RoundedRectShape.obtain()
                            .setBounds(trackX, thumbY, 2.0F, thumbHeight)
                            .setRadius(1.0F);
            thumbRed = color.getRed();
            int thumbGreen = color.getGreen();
            int thumbBlue = color.getBlue();

            context.drawRect(thumbShape.setColor(new Color(thumbRed, thumbGreen, thumbBlue, 190)));
        }

        context.endScissor();
        float centerX = this.getContentCenterX();
        int statusRed;

        float titleX = centerX - this.titleFont.getStringWidth("Alt Manager") / 2.0F;
        float titleY = this.sidePanelY + 2.0F;
        float userTextX =
                centerX
                        - this.currentUserFont.getStringWidth(currentUserName) / 2.0F
                        + (this.currentUserFont.getLineHeight() + 4.0F) / 2.0F;
        float userTextY = titleY + this.titleFont.getLineHeight() + 2.0F;
        float statusPillWidth = this.statusFont.getStringWidth(this.statusMessage) + 16.0F;
        float statusPillX = centerX - statusPillWidth / 2.0F;
        float statusPillY = userTextY + this.currentUserFont.getLineHeight() + 4.0F;
        float titleWidth = this.titleFont.getStringWidth("Alt Manager");
        int gradientStart = ColorUtil.waveBlend(titleX, color.getRGB(), color2.getRGB());
        int gradientEnd = ColorUtil.waveBlend(titleX + titleWidth, color.getRGB(), color2.getRGB());
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        context.drawText(
                titleRequest
                        .setText("Alt Manager")
                        .setPosition(titleX, titleY)
                        .setFont(this.titleFont.getMsdfFont())
                        .setFontSize(this.titleFont.getSize())
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                gradientStart,
                                                gradientEnd,
                                                titleX,
                                                titleY,
                                                titleX + titleWidth,
                                                titleY)));

        try {
            GpuTexture gpuTexture2;
            try {
                gpuTexture2 =
                        mc.getTextureManager()
                                .getTexture(this.skin(null).body().texturePath())
                                .getTexture();
            } catch (Exception ignored) {
                System.out.println("fall back");
                gpuTexture2 =
                        mc.getTextureManager()
                                .getTexture(DefaultPlayerSkin.getDefaultTexture())
                                .getTexture();
            }

            float avatarSize = this.currentUserFont.getLineHeight() + 1.0F;
            context.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds(
                                    userTextX - avatarSize - 4.0F,
                                    userTextY - 1.0F,
                                    avatarSize,
                                    avatarSize)
                            .setTexture(gpuTexture2)
                            .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                            .setRadius(2.0F)
                            .useNearestFilter()
                            .setColor(Color.WHITE));
        } catch (Exception ignored) {
        }

        context.drawText(
                TextDrawRequest.obtain()
                        .setText(currentUserName)
                        .setPosition(userTextX, userTextY)
                        .setFont(this.currentUserFont.getMsdfFont())
                        .setFontSize(this.currentUserFont.getSize())
                        .enableShadow()
                        .setColor(this.secondaryTextColor.getRGB()));
        Color color6;
        if (this.statusMessage.startsWith("Successfully")) {

            color6 = new Color(75, 195, 100);
        } else {
            label117:
            {
                if (!this.statusMessage.contains("failed")) {
                    if (!this.statusMessage.contains("error")) {
                        if (!this.statusMessage.contains("Error")) {
                            if (this.statusMessage.contains("...")) {

                                color6 = new Color(215, 175, 55);
                            } else {

                                color6 = new Color(100, 100, 100);
                            }
                            break label117;
                        }
                    }
                }

                color6 = new Color(200, 70, 70);
            }
        }

        boolean idle = this.statusMessage.equals("Idle");
        Color color7;
        if (idle) {

            color7 = new Color(100, 100, 110, 45);
        } else {
            statusRed = color6.getRed();
            int statusGreen = color6.getGreen();
            int statusBlue = color6.getBlue();

            color7 = new Color(statusRed, statusGreen, statusBlue, 30);
        }

        Color color8 = color7;
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(statusPillX, statusPillY, statusPillWidth, 15.0F)
                        .setRadius(7.5F)
                        .setColor(color8));
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(this.statusMessage)
                        .setPosition(
                                statusPillX
                                        + (statusPillWidth
                                                        - this.statusFont.getStringWidth(
                                                                this.statusMessage))
                                                / 2.0F,
                                statusPillY)
                        .centerVertically(15.0F)
                        .setFont(this.statusFont.getMsdfFont())
                        .setFontSize(this.statusFont.getSize())
                        .enableShadow()
                        .setColor(color6.getRGB()));

        for (Button button2 : this.actionButtons) {
            this.drawActionButton(context, button2, mouseX, mouseY);
        }

        this.drawNiceAltsButton(
                context,
                this.getNiceAltsButtonX(),
                this.getNiceAltsButtonY(),
                this.getActionButtonWidth(),
                20.0F,
                mouseX,
                mouseY);
        TextDrawRequest pasteRequest = TextDrawRequest.obtain();

        TextDrawRequest pasteTextRequest =
                pasteRequest.setText("Paste cookie text or drop files/ZIPs");
        float pasteCenterX = this.getContentCenterX();

        context.drawText(
                pasteTextRequest
                        .setPosition(
                                pasteCenterX
                                        - this.hintFont.getStringWidth(
                                                        "Paste cookie text or drop files/ZIPs")
                                                / 2.0F,
                                this.getNiceAltsButtonY() + 20.0F + 5.0F)
                        .setFont(this.hintFont.getMsdfFont())
                        .setFontSize(this.hintFont.getSize())
                        .enableShadow()
                        .setColor(this.mutedTextColor.getRGB()));
        if (this.addDialogOpen) {
            this.drawAddAccountDialog(context, mouseX, mouseY);
        }
    }

    private void onActionButton(int buttonIndex) {
        switch (buttonIndex) {
            case 0:
                if (this.selectedAccount != null) {
                    this.login(this.selectedAccount);
                }
                break;
            case 1:
                this.addDialogOpen = !this.addDialogOpen;
                this.usernameFieldFocused =
                        this.addDialogOpen && this.addAccountType == AccountType.CRACKED;
                if (!this.usernameFieldFocused) {
                    TextInputUtil.clearSelectAll(this.textInputOwner);
                }
                break;
            case 2:
                if (this.selectedAccount != null) {
                    Astralis.getInstance().getAccountManager().removeAccount(this.selectedAccount);
                }

                this.selectedAccount = null;
                break;
            case 3:
                Astralis.getInstance().getAccountManager().clearAccounts();
                this.selectedAccount = null;
        }
    }

    private void cycleAccountType() {
        AccountType[] accountTypes =
                new AccountType[] {
                    AccountType.COOKIE,
                    AccountType.TOKEN,
                    AccountType.REFRESH,
                    AccountType.MICROSOFT,
                    AccountType.CRACKED
                };
        int currentIndex = Arrays.asList(accountTypes).indexOf(this.addAccountType);
        this.addAccountType = accountTypes[(currentIndex + 1) % accountTypes.length];
        this.usernameFieldFocused = this.addAccountType == AccountType.CRACKED;
        if (!this.usernameFieldFocused) {
            TextInputUtil.clearSelectAll(this.textInputOwner);
        }
    }

    private void startMicrosoftLogin() {
        String clientId = MicrosoftClientsInitialization.iasClient.getClientId();

        String authUrl =
                "https://login.live.com/oauth20_authorize.srf?client_id="
                        + clientId
                        + "&response_type=code&redirect_uri=http://localhost:8247&scope=XboxLive.signin%20offline_access";
        mc.keyboardHandler.setClipboard(authUrl);

        this.statusMessage = "Logging in... (URL copied to clipboard)";
        MicrosoftAuth.startBrowserLogin(
                refreshToken -> {
                    if (refreshToken == null) {

                        this.statusMessage = "Microsoft Login Failed (No Refresh Token)";
                    } else {
                        new Thread(
                                        () -> {
                                            this.statusMessage = "Logging in... (Authenticating)";
                                            MicrosoftAuthResult authResult =
                                                    MicrosoftAuth.authenticate(
                                                            refreshToken, clientId);
                                            if (authResult.isSuccess()) {
                                                try {
                                                    User user =
                                                            new User(
                                                                    authResult.playerName,
                                                                    SessionLoginUtil.parseUuid(
                                                                            authResult.playerUuid),
                                                                    authResult.accessToken,
                                                                    Optional.empty(),
                                                                    Optional.empty());
                                                    SessionLoginUtil.applyUserSession(
                                                            user, authResult.accessToken);

                                                    this.statusMessage =
                                                            "Successfully logged in as "
                                                                    + authResult.playerName;
                                                    String accountRefreshToken =
                                                            authResult.refreshToken != null
                                                                    ? authResult.refreshToken
                                                                    : refreshToken;
                                                    Account account =
                                                            new Account(
                                                                    authResult.accessToken,
                                                                    accountRefreshToken,
                                                                    authResult.playerName,
                                                                    authResult.playerName);
                                                    account.setType(AccountType.MICROSOFT);
                                                    this.loginAndSaveAccount(account);
                                                } catch (Exception exception) {
                                                    String errorMessage = exception.getMessage();

                                                    this.statusMessage =
                                                            "Login Error: " + errorMessage;
                                                }
                                            } else {
                                                this.statusMessage =
                                                        "Microsoft Login Failed (Invalid Login Data)";
                                            }
                                        })
                                .start();
                    }
                },
                clientId);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.addDialogOpen && event.isEscape()) {
            this.closeAddDialog();
            return true;
        }

        if (this.usernameFieldFocused && this.addDialogOpen) {
            int keyCode = (int) this.textInputOwner;
            String currentText = this.crackedUsername;
            Consumer<String> textSetter = newText -> this.crackedUsername = newText;

            if (TextInputUtil.handleControlShortcut(
                    keyCode,
                    event,
                    currentText,
                    textSetter,
                    16,
                    AltManagerScreen::isVisibleAsciiChar)) {
                return true;
            }

            keyCode = event.key();
            if (keyCode == 259) {
                TextInputUtil.handleBackspace(
                        this.textInputOwner,
                        this.crackedUsername,
                        newText -> this.crackedUsername = newText);
            } else {
                keyCode = event.key();
                if (keyCode != 257) {
                    keyCode = event.key();
                    if (keyCode != 335) {
                        return true;
                    }
                }

                this.submitCrackedUsername();
            }

            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (this.usernameFieldFocused && this.addDialogOpen) {
            Object owner = this.textInputOwner;
            String currentText = this.crackedUsername;
            String insertedText = event.codepointAsString();
            Consumer<String> textSetter = newText -> this.crackedUsername = newText;

            TextInputUtil.insertText(
                    owner,
                    currentText,
                    insertedText,
                    textSetter,
                    16,
                    AltManagerScreen::isVisibleAsciiChar);
            return true;
        } else {
            return super.charTyped(event);
        }
    }

    private float getDialogWidth() {
        return Math.clamp(this.sidePanelWidth - 12.0F, 190.0F, 230.0F);
    }

    private float getListContentHeight(int accountCount) {
        return accountCount * 39.0F + 12.0F;
    }

    private float getNiceAltsButtonY() {
        return this.getActionButtonY(0) - 20.0F - 4.0F - this.hintFont.getLineHeight() - 5.0F;
    }

    private float getDialogX() {
        return this.getContentCenterX() - this.getDialogWidth() / 2.0F;
    }

    private void addAccountFromClipboard() {
        if (this.addAccountType == AccountType.MICROSOFT) {
            this.startMicrosoftLogin();
        } else {
            String clipboardText = mc.keyboardHandler.getClipboard();
            if (this.addAccountType == AccountType.COOKIE) {
                List clipboardFiles = CookieFileImporter.collectClipboardFiles(clipboardText);
                if (!clipboardFiles.isEmpty()) {
                    this.importCookieFiles(clipboardFiles);
                } else if (clipboardText.isBlank()) {

                    this.statusMessage = "No cookie text or files in clipboard";
                } else {
                    this.login(Account.createCookieAccount(clipboardText));
                }
            } else {
                ParsedTokens parsedTokens = AccountManager.parseTokens(clipboardText);
                String token =
                        this.addAccountType == AccountType.REFRESH
                                ? parsedTokens.getRefreshToken()
                                : parsedTokens.getAccessToken();
                if (token != null && !token.isBlank()) {
                    String accessToken = this.addAccountType == AccountType.TOKEN ? token : null;
                    String refreshToken =
                            this.addAccountType == AccountType.REFRESH
                                    ? token
                                    : parsedTokens.getRefreshToken();
                    String accountName = this.addAccountType.toString();

                    accountName = accountName + " Alt";

                    String tokenPreview = token.substring(0, Math.min(10, token.length()));
                    Account tokenAccount =
                            new Account(
                                    accessToken, refreshToken, accountName, tokenPreview + "...");
                    tokenAccount.setType(this.addAccountType);
                    this.login(tokenAccount);
                } else {
                    String errorMessage;
                    if (this.addAccountType == AccountType.REFRESH) {

                        errorMessage = "No refresh token in clipboard";
                    } else {

                        errorMessage = "No access token in clipboard";
                    }

                    this.statusMessage = errorMessage;
                }
            }
        }
    }

    private float getScrollbarThumbY(
            float trackY, float trackHeight, float maxScroll, float scrollPos) {
        float thumbHeight = this.getScrollbarThumbHeight(trackHeight, maxScroll + this.listHeight);
        return trackY
                + (trackHeight - thumbHeight) * Math.clamp(-scrollPos / maxScroll, 0.0F, 1.0F);
    }

    private void login(Account account) {

        this.statusMessage = "Logging in...";
        new Thread(
                        () -> {
                            try {
                                User user = this.loginAndSaveAccount(account);
                                String playerName = user.getName();

                                this.statusMessage = "Successfully logged in as " + playerName;
                            } catch (Exception exception) {
                                if (!Astralis.getInstance()
                                        .getAccountManager()
                                        .getAccounts()
                                        .contains(account)) {
                                    this.selectedAccount = null;
                                }

                                String failureMessage;
                                if (account.getType() == AccountType.COOKIE) {
                                    String errorDetail;
                                    if (exception.getMessage() == null) {

                                        errorDetail = "unknown error";
                                    } else {
                                        errorDetail = exception.getMessage();
                                    }

                                    failureMessage = "Cookie login failed: " + errorDetail;
                                } else {

                                    failureMessage = "Login failed";
                                }

                                this.statusMessage = failureMessage;
                            }
                        })
                .start();
    }

    private boolean isOverActionControl(double mouseX, double mouseY) {
        Button[] buttons = this.actionButtons;

        for (Button button2 : buttons) {
            if (button2 != null
                    && this.isPointInRect(
                            mouseX,
                            mouseY,
                            button2.getX(),
                            button2.getY(),
                            button2.getWidth(),
                            button2.getHeight())) {
                return true;
            }
        }

        return this.isPointInRect(
                mouseX,
                mouseY,
                this.getNiceAltsButtonX(),
                this.getNiceAltsButtonY(),
                this.getActionButtonWidth(),
                20.0F);
    }

    private void drawAddAccountDialog(Render2DContext context, int mouseX, int mouseY) {
        float dialogX = this.getDialogX();
        float dialogY = this.getDialogY();
        float dialogWidth = this.getDialogWidth();
        this.drawGlass(context, dialogX, dialogY, dialogWidth, this.getDialogHeight(), 8.0F, false);
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        TextDrawRequest titleTextRequest = titleRequest.setText("Add account");

        context.drawText(
                titleTextRequest
                        .setPosition(
                                dialogX
                                        + (dialogWidth
                                                        - this.buttonFont.getStringWidth(
                                                                "Add account"))
                                                / 2.0F,
                                dialogY + 7.0F)
                        .setFont(this.buttonFont.getMsdfFont())
                        .setFontSize(this.buttonFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        this.drawDialogButton(
                context,
                dialogX + 8.0F,
                dialogY + 24.0F,
                dialogWidth - 16.0F,
                20.0F,
                this.addAccountType.toString(),
                mouseX,
                mouseY);
        if (this.addAccountType == AccountType.CRACKED) {
            this.drawUsernameInput(
                    context,
                    dialogX + 8.0F,
                    dialogY + 49.0F,
                    dialogWidth - 16.0F,
                    20.0F,
                    mouseX,
                    mouseY);
        } else {
            float buttonX = dialogX + 8.0F;
            float buttonY = dialogY + 49.0F;
            float buttonWidth = dialogWidth - 16.0F;
            String buttonLabel;
            if (this.addAccountType == AccountType.MICROSOFT) {

                buttonLabel = "Open browser";
            } else {

                buttonLabel = "Use clipboard";
            }

            this.drawDialogButton(
                    context, buttonX, buttonY, buttonWidth, 20.0F, buttonLabel, mouseX, mouseY);
        }
    }

    private boolean isInsideDialog(double x, double y) {
        return this.isPointInRect(
                x,
                y,
                this.getDialogX(),
                this.getDialogY(),
                this.getDialogWidth(),
                this.getDialogHeight());
    }

    private void closeAddDialog() {
        this.addDialogOpen = false;
        this.blurUsernameInput();
    }

    private float getActionButtonWidth() {
        return (this.getButtonRowWidth() - 12.0F) / 4.0F;
    }

    private void submitCrackedUsername() {

        String username = this.crackedUsername.trim();
        if (!username.isEmpty() && StringUtil.isValidPlayerName(username)) {

            if (!"Logging in...".equals(this.statusMessage)) {
                this.crackedUsername = username;
                this.closeAddDialog();
                Account account = null;

                for (Account existingAccount :
                        Astralis.getInstance().getAccountManager().getAccounts()) {
                    if (existingAccount.getType() == AccountType.CRACKED
                            && username.equals(existingAccount.getUsername())) {
                        account = existingAccount;
                        break;
                    }
                }

                if (account == null) {
                    account = new Account(username);
                }

                this.selectedAccount = account;
                this.login(account);
            }
        } else {

            this.statusMessage = "Invalid username";
        }
    }

    private float getButtonRowWidth() {
        return Math.min(this.getContentWidth(), this.width - 40.0F);
    }

    private float getActionButtonX(int index) {
        float startX = this.getContentCenterX() - this.getButtonRowWidth() / 2.0F;
        return startX + index * (this.getActionButtonWidth() + 4.0F);
    }

    private float getActionButtonY(int index) {
        return this.sidePanelY + this.sidePanelHeight - 20.0F - 4.0F;
    }

    private void importCookieFiles(List<File> files) {

        this.statusMessage = "Importing cookie files...";
        Runnable importTask =
                () -> {
                    int importedCount = 0;

                    try {
                        for (File file2 :
                                CookieFileImporter.importCookieFiles(files, mc.gameDirectory)) {
                            try {
                                this.loginAndSaveAccount(
                                        Account.createCookieAccount(
                                                Files.readString(
                                                        file2.toPath(), StandardCharsets.UTF_8)));
                                importedCount++;
                            } catch (Exception ignored) {
                            } finally {
                                try {
                                    Files.deleteIfExists(file2.toPath());
                                } catch (IOException ignored) {
                                }
                            }
                        }

                        String resultMessage;
                        if (importedCount == 0) {

                            resultMessage = "Cookie login failed";
                        } else {
                            String plural = importedCount == 1 ? "" : "s";

                            resultMessage =
                                    "Imported " + importedCount + " cookie account" + plural;
                        }

                        this.statusMessage = resultMessage;
                    } catch (Exception exception) {
                        String errorMessage = exception.getMessage();

                        this.statusMessage = "Cookie import failed: " + errorMessage;
                    }
                };

        new Thread(importTask, "astralis-cookie-import").start();
    }

    private float getContentX() {
        return (this.width - this.getContentWidth()) / 2.0F;
    }

    private float getNiceAltsButtonX() {
        return this.getContentCenterX() - this.getActionButtonWidth() / 2.0F;
    }

    private float getScrollbarThumbHeight(float trackHeight, float contentHeight) {
        return Math.min(trackHeight, Math.max(18.0F, trackHeight * trackHeight / contentHeight));
    }

    private float getScrollOffsetForThumbDrag(
            float mouseY,
            float grabOffset,
            float trackY,
            float trackHeight,
            float thumbHeight,
            float maxScroll) {
        return -Math.clamp(
                        (mouseY - trackY - grabOffset) / Math.max(1.0F, trackHeight - thumbHeight),
                        0.0F,
                        1.0F)
                * maxScroll;
    }

    private float getMaxScroll(int accountCount) {
        return Math.max(0.0F, this.getListContentHeight(accountCount) - this.listHeight);
    }

    private PlayerSkin skin(Account account) {
        String playerName = account == null ? mc.getUser().getName() : account.getUsername();

        UUID uuid =
                UUID.nameUUIDFromBytes(
                        ("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        PlayerSkin playerSkin = skinCache.get(uuid);
        if (playerSkin != null) {
            return playerSkin;
        }

        playerSkin = DefaultPlayerSkin.get(uuid);
        skinCache.put(uuid, playerSkin);
        CompletableFuture.<GameProfile>supplyAsync(
                        () -> {
                            Optional<GameProfile> resolvedProfile =
                                    mc.services().profileResolver().fetchByName(playerName);
                            return resolvedProfile.orElseGet(
                                    () -> new GameProfile(uuid, playerName));
                        },
                        Util.backgroundExecutor())
                .thenComposeAsync(profile -> mc.getSkinManager().get(profile))
                .thenAcceptAsync(
                        resolvedSkin ->
                                resolvedSkin.ifPresent(
                                        playerSkin2 -> skinCache.put(uuid, playerSkin2)),
                        mc);
        return playerSkin;
    }

    private void drawGlass(
            Render2DContext context,
            float x,
            float y,
            float width,
            float height,
            float radius,
            boolean hovered) {
        ShadowRenderer shadowRenderer = context.getShadowRenderer();
        Consumer<Render2DContext> shadowPainter =
                shadowContext ->
                        shadowContext.drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(x, y, width, height)
                                        .setRadius(radius)
                                        .setColor(Color.WHITE));

        Color color = new Color(0, 0, 0, 95);

        shadowRenderer.renderShapes(shadowPainter, color, 8, 0.55F);
        Astralis.getInstance()
                .getRenderEffects()
                .drawBlurredRoundedRectangle(x, y, width, height, radius, 1.0F);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x, y, width, height)
                        .setRadius(radius)
                        .setColor(hovered ? glassHoverColor : glassColor));
        if (hovered) {
            RoundedRectShape highlightShape =
                    RoundedRectShape.obtain().setBounds(x, y, width, height).setRadius(radius);

            context.drawRect(highlightShape.setColor(new Color(255, 255, 255, 22)));
        }
    }

    private float getDialogY() {
        return this.height / 2.0F - this.getDialogHeight() / 2.0F;
    }

    private void blurUsernameInput() {
        this.usernameFieldFocused = false;
        TextInputUtil.clearSelectAll(this.textInputOwner);
    }

    private User loginAndSaveAccount(Account account) throws java.io.IOException {
        User user = Astralis.getInstance().getAccountManager().login(account);
        if (!Astralis.getInstance().getAccountManager().getAccounts().contains(account)) {
            Astralis.getInstance().getAccountManager().addAccount(account);
        }

        Astralis.getInstance().getAccountManager().setLastAccountId(account.getId());
        Astralis.getInstance().getAccountManager().saveAccounts();
        return user;
    }

    private float getDialogHeight() {
        return 86.0F;
    }

    private void drawNiceAltsButton(
            Render2DContext context,
            float x,
            float y,
            float width,
            float height,
            int mouseX,
            int mouseY) {
        boolean hovered = this.isPointInRect(mouseX, mouseY, x, y, width, height);
        this.drawGlass(context, x, y, width, height, 6.0F, hovered);
        AltsLogoRenderer.drawLogo(context, x, y + 1.0F, width, height - 2.0F, true);
    }

    private void drawActionButton(Render2DContext context, Button button2, int mouseX, int mouseY) {
        float x = button2.getX();
        float y = button2.getY();
        float width = button2.getWidth();
        float height = button2.getHeight();
        boolean hovered = this.isPointInRect(mouseX, mouseY, x, y, width, height);
        this.drawGlass(context, x, y, width, height, 6.0F, hovered);
        String label = button2.getMessage().getString();
        context.drawText(
                TextDrawRequest.obtain()
                        .setText(label)
                        .setPosition(x, y)
                        .centerInBox(width, height)
                        .setFont(this.buttonFont.getMsdfFont())
                        .setFontSize(this.buttonFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
    }

    private float getContentWidth() {
        return Math.min(420.0F, this.width - 20.0F);
    }

    private static boolean isVisibleAsciiChar(int codePoint) {

        if (codePoint > 32) {

            if (codePoint < 127) {
                return true;
            }
        }

        return false;
    }

    static {
        glassColor = new Color(18, 20, 25, 105);
        glassHoverColor = new Color(18, 20, 25, 150);
        rowColor = new Color(18, 20, 25, 105);
        rowHoverColor = new Color(18, 20, 25, 145);
        rowSelectedColor = new Color(42, 42, 42, 180);
        skinCache = new ConcurrentHashMap<>(10);
    }
}
