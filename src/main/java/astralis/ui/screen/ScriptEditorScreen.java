package astralis.ui.screen;

import astralis.Astralis;
import astralis.scripting.ReloadResult;
import astralis.scripting.ScriptManager;
import astralis.scripting.ScriptSource;
import astralis.ui.component.FlatUiPalette;
import astralis.ui.component.FlatUiPaletteColors;
import astralis.ui.component.SapphireScreen;
import astralis.ui.component.ScriptsToolbarAction;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.astralis.scripting.InstanceAccess;
import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OS;

import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ScriptEditorScreen extends Screen
        implements SapphireScreen, InstanceAccess, FlatUiPalette {
    private static float windowFractionX;
    private static float windowFractionY;
    private final Screen parentScreen;
    private final ScriptListPanel libraryList;
    private final ScriptToolbarRenderer toolbar;
    private final ScriptSearchField searchBox;
    private final ScriptDetailPanel detailsPanel;
    private List<String> sourcePreviewLines;
    private boolean selectedHasEasyProject;
    private List<ScriptSource> visibleSources;
    private List<ScriptSource> allSources;
    private String statusMessage;
    private String selectedScriptName;
    private String editingScriptName;
    private int lastMouseX;
    private int lastMouseY;
    private float windowX;
    private float windowY;
    private float windowWidth;
    private float windowHeight;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean draggingWindow;
    private boolean draggingScrollbar;
    private boolean easyModeOpen;
    private boolean browserMouseDown;
    private boolean projectPushPending;
    public MCEFBrowser browser;
    private CefMessageRouter messageRouter;
    private PendingScriptOverwrite pendingOverwrite;

    public ScriptEditorScreen(Screen screen) {

        super(Component.literal("Scripts"));
        this.libraryList = new ScriptListPanel();
        this.toolbar = new ScriptToolbarRenderer();
        this.searchBox = new ScriptSearchField();
        this.detailsPanel = new ScriptDetailPanel();
        this.sourcePreviewLines = List.of();
        this.visibleSources = List.of();
        this.allSources = List.of();

        this.statusMessage = "Open a script in Easy Mode, or edit its source from the folder.";
        this.parentScreen = screen;
    }

    @Override
    protected void init() {
        super.init();
        this.draggingWindow = false;
        this.draggingScrollbar = false;
        this.searchBox.unfocus();
        this.windowWidth =
                Math.max(
                        1.0F,
                        Math.min(
                                this.easyModeOpen ? this.width - 16.0F : 650.0F,
                                this.width - 16.0F));
        this.windowHeight =
                Math.max(
                        1.0F,
                        Math.min(
                                this.easyModeOpen ? this.height - 16.0F : 368.0F,
                                this.height - 16.0F));
        this.windowX =
                this.width <= this.windowWidth
                        ? 0.0F
                        : windowFractionX * (this.width - this.windowWidth);
        this.windowY =
                this.height <= this.windowHeight
                        ? 0.0F
                        : windowFractionY * (this.height - this.windowHeight);
        this.refreshSources();
        this.updateChildLayout();
        if (this.easyModeOpen) {
            this.ensureBrowserCreated();
            if (this.browser != null) {
                this.resizeBrowserToContent();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.easyModeOpen) {
            this.ensureBrowserCreated();
            if (this.browser != null) {
                if (this.isNotEasyModeUrl(this.browser.getURL()) && !this.browser.isLoading()) {

                    this.browser.loadURL("https://docs.astralisclient.com/easy-mode/");
                    this.projectPushPending = true;
                } else {
                    if (this.projectPushPending && !this.browser.isLoading()) {
                        this.projectPushPending = false;
                        this.sendProjectToBrowser();
                    }

                    if (this.pendingOverwrite != null
                            && Util.getMillis() > this.pendingOverwrite.getExpiresAtMillis()) {
                        this.pendingOverwrite = null;
                    }
                }
            }
        }
    }

    @Override
    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (this.easyModeOpen) {
            int browserX = Math.round(this.getBrowserX());
            int browserY = Math.round(this.getBrowserY());
            int browserWidth = Math.round(this.getBrowserWidth());
            int browserHeight = Math.round(this.getBrowserHeight());
            int browserRight = browserX + browserWidth;
            int browserBottom = browserY + browserHeight;

            graphics.fill(browserX, browserY, browserRight, browserBottom, -15987182);
            if (this.browser != null && this.browser.isTextureReady()) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        this.browser.getTextureIdentifier(),
                        browserX,
                        browserY,
                        0.0F,
                        0.0F,
                        browserWidth,
                        browserHeight,
                        browserWidth,
                        browserHeight);
            } else {
                Font font2 = this.font;
                String loadingText;
                if (MCEF.isInitialized()) {
                    loadingText = "Loading Easy Mode…";
                } else {
                    loadingText = "Chromium failed to initialize";
                }

                graphics.centeredText(
                        font2,
                        loadingText,
                        browserX + browserWidth / 2,
                        browserY + browserHeight / 2 - 4,
                        -1);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderSapphire(Render2DContext context) {

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 12.0F);

        SizedFont sectionFont = FontManager.getFont("Product Sans Bold", 9.0F);

        SizedFont smallFont = FontManager.getFont("Product Sans Regular", 7.0F);
        this.updateChildLayout();
        if (!this.easyModeOpen) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.windowX, this.windowY, this.windowWidth, this.windowHeight)
                            .setRadius(8.0F)
                            .setColor(FlatUiPaletteColors.BACKGROUND));
        } else if (this.getEditorSidebarWidth() > 0.0F) {
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.windowX,
                                    this.windowY,
                                    this.getEditorSidebarWidth(),
                                    this.windowHeight)
                            .setCornerRadii(8.0F, 0.0F, 0.0F, 8.0F)
                            .setColor(FlatUiPaletteColors.BACKGROUND));
        }

        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY, this.windowWidth, 28.0F)
                        .setCornerRadii(8.0F, 8.0F, 0.0F, 0.0F)
                        .setColor(FlatUiPaletteColors.BACKGROUND_DEEP));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY + 27.0F, this.windowWidth, 1.0F)
                        .setColor(FlatUiPaletteColors.DIVIDER));
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        context.drawText(
                titleRequest
                        .setText("Scripts")
                        .setPosition(this.windowX + 10.0F, this.windowY)
                        .centerVertically(28.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setPaint(
                                ShapePaint.obtain()
                                        .setLinearGradient(
                                                ColorUtil.waveBlend(
                                                        this.windowX + 10.0F,
                                                        Astralis.getInstance()
                                                                .getPrimaryColor()
                                                                .getRGB(),
                                                        Astralis.getInstance()
                                                                .getSecondaryColor()
                                                                .getRGB()),
                                                ColorUtil.waveBlend(
                                                        this.windowX + 65.0F,
                                                        Astralis.getInstance()
                                                                .getPrimaryColor()
                                                                .getRGB(),
                                                        Astralis.getInstance()
                                                                .getSecondaryColor()
                                                                .getRGB()),
                                                this.windowX + 10.0F,
                                                this.windowY,
                                                this.windowX + 65.0F,
                                                this.windowY)));
        this.toolbar.drawToolbar(
                context,
                this.windowX,
                this.windowY,
                this.windowWidth,
                this.easyModeOpen,
                this.lastMouseX,
                this.lastMouseY);
        if (!this.easyModeOpen || this.getEditorSidebarWidth() != 0.0F) {
            float sidebarWidth =
                    this.easyModeOpen
                            ? this.getEditorSidebarWidth()
                            : this.windowWidth - this.getDetailsPanelWidth();
            context.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(this.windowX, this.windowY + 28.0F, sidebarWidth, 32.0F)
                            .setColor(FlatUiPaletteColors.PANEL));
            TextDrawRequest libraryRequest = TextDrawRequest.obtain();
            String libraryLabel;
            if (this.easyModeOpen) {

                libraryLabel = "Library";
            } else {
                int visibleCount = this.visibleSources.size();

                libraryLabel = "Library  /  " + visibleCount;
            }

            context.drawText(
                    libraryRequest
                            .setText(libraryLabel)
                            .setPosition(this.windowX + 10.0F, this.windowY + 28.0F)
                            .centerVertically(32.0F)
                            .setFont(sectionFont.getMsdfFont())
                            .setFontSize(sectionFont.getSize())
                            .setColor(Astralis.getInstance().getPrimaryColor()));
            if (!this.easyModeOpen && sidebarWidth > 0.0F) {
                this.searchBox.drawField(context);
            }

            this.libraryList.drawList(
                    context,
                    this.visibleSources,
                    this.selectedScriptName,
                    !this.easyModeOpen && !this.searchBox.getQuery().isBlank(),
                    this.lastMouseX,
                    this.lastMouseY);
            context.beginScissor(
                    this.windowX + 10.0F,
                    this.windowY + this.windowHeight - 22.0F,
                    Math.max(0.0F, sidebarWidth - 20.0F),
                    20.0F);
            context.drawText(
                    TextDrawRequest.obtain()
                            .setText(
                                    smallFont.truncateWithEllipsis(
                                            this.statusMessage, sidebarWidth - 20.0F))
                            .setPosition(
                                    this.windowX + 10.0F, this.windowY + this.windowHeight - 20.0F)
                            .centerVertically(18.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .setColor(FlatUiPaletteColors.TEXT_MUTED.getRGB()));
            context.endScissor();
            if (!this.easyModeOpen && this.getDetailsPanelWidth() > 0.0F) {
                this.detailsPanel.drawDetails(
                        context,
                        this.windowX + this.windowWidth - this.getDetailsPanelWidth(),
                        this.windowY + 28.0F,
                        this.getDetailsPanelWidth(),
                        this.windowHeight - 28.0F,
                        this.allSources.stream()
                                .filter(source -> source.name().equals(this.selectedScriptName))
                                .findFirst()
                                .orElse(null),
                        this.sourcePreviewLines,
                        this.selectedHasEasyProject,
                        this.lastMouseX,
                        this.lastMouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        this.updateChildLayout();
        if (!this.easyModeOpen
                && this.windowWidth > this.getDetailsPanelWidth()
                && this.searchBox.handleMouseClick(event.x(), event.y())) {
            return true;
        }

        ScriptsToolbarAction toolbarAction =
                this.toolbar.getActionAt(
                        event.x(),
                        event.y(),
                        this.windowX,
                        this.windowY,
                        this.windowWidth,
                        this.easyModeOpen);
        switch (ScriptEditorScreenSwitchMap.toolbarActionOrdinals[toolbarAction.ordinal()]) {
            case 1:
                this.openEasyMode(null);
                break;
            case 2:
                this.closeEasyMode();
                break;
            case 3:
                OS platform = Util.getPlatform();
                Path path = mc.gameDirectory.toPath();
                Path path2 = path.resolve("astralis");
                platform.openPath(path2.resolve("scripts"));
                this.statusMessage = "Opened the scripts folder.";
                break;
            case 4:
                this.reloadScripts();
                break;
            case 5:
                this.onClose();
            case 6:
        }

        if (toolbarAction != ScriptsToolbarAction.NONE) {
            return true;
        }

        if (!this.easyModeOpen && this.getDetailsPanelWidth() > 0.0F) {
            float detailsX = this.windowX + this.windowWidth - this.getDetailsPanelWidth();
            if (this.isInside(
                    event.x(),
                    event.y(),
                    detailsX + this.getDetailsPanelWidth() - 28.0F,
                    this.windowY + 36.0F,
                    20.0F,
                    20.0F)) {
                this.selectedScriptName = null;
                this.sourcePreviewLines = List.of();
                this.selectedHasEasyProject = false;
                this.updateChildLayout();
                return true;
            }

            if (this.isInside(
                    event.x(),
                    event.y(),
                    detailsX + 12.0F,
                    this.windowY + this.windowHeight - 32.0F,
                    this.getDetailsPanelWidth() - 24.0F,
                    22.0F)) {
                if (this.selectedHasEasyProject) {
                    this.openEasyMode(this.selectedScriptName);
                } else {
                    OS buttonPlatform = Util.getPlatform();
                    Path path3 = mc.gameDirectory.toPath();

                    Path path4 = path3.resolve("astralis");

                    buttonPlatform.openPath(path4.resolve("scripts"));
                    this.statusMessage = "Opened the scripts folder.";
                }

                return true;
            }
        }

        if (this.libraryList.beginScrollbarDrag(event.x(), event.y(), this.visibleSources.size())) {
            this.draggingScrollbar = true;
            return true;
        }

        String clickedScriptName =
                this.libraryList.getScriptNameAt(event.x(), event.y(), this.visibleSources);
        if (clickedScriptName == null) {
            if (this.easyModeOpen
                    && this.isInside(
                            event.x(),
                            event.y(),
                            this.getBrowserX(),
                            this.getBrowserY(),
                            this.getBrowserWidth(),
                            this.getBrowserHeight())) {
                if (this.browser == null) {
                    return true;
                }

                this.browser.sendMousePress(
                        this.toBrowserPixelX(event.x()),
                        this.toBrowserPixelY(event.y()),
                        event.button());
                this.browser.setFocus(true);
                this.browserMouseDown = true;
                return true;
            } else if (this.isInside(
                    event.x(), event.y(), this.windowX, this.windowY, this.windowWidth, 28.0F)) {
                this.draggingWindow = true;
                this.dragOffsetX = (float) event.x() - this.windowX;
                this.dragOffsetY = (float) event.y() - this.windowY;
                return true;
            } else {
                return super.mouseClicked(event, doubleClick);
            }
        } else {
            this.selectedScriptName = clickedScriptName;
            this.loadSelectedSourcePreview();
            if (!this.easyModeOpen && (!doubleClick || !this.selectedHasEasyProject)) {
                String selectionStatus;
                if (this.selectedHasEasyProject) {

                    selectionStatus = "Easy Mode project selected.";
                } else {

                    selectionStatus = "Java source selected. Preview is read-only.";
                }

                this.statusMessage = selectionStatus;
            } else {
                this.openEasyMode(clickedScriptName);
            }

            return true;
        }
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.browserMouseDown && event.button() == 0 && this.browser != null) {
            this.browser.sendMouseMove(
                    this.toBrowserPixelX(event.x()), this.toBrowserPixelY(event.y()));
            return true;
        } else if (this.draggingScrollbar && event.button() == 0) {
            this.libraryList.dragScrollbar(event.y(), this.visibleSources.size());
            return true;
        } else if (this.draggingWindow && event.button() == 0) {
            this.windowX =
                    Math.clamp(
                            (float) event.x() - this.dragOffsetX,
                            0.0F,
                            Math.max(0.0F, this.width - this.windowWidth));
            this.windowY =
                    Math.clamp(
                            (float) event.y() - this.dragOffsetY,
                            0.0F,
                            Math.max(0.0F, this.height - this.windowHeight));
            windowFractionX =
                    this.width <= this.windowWidth
                            ? 0.5F
                            : this.windowX / (this.width - this.windowWidth);
            windowFractionY =
                    this.height <= this.windowHeight
                            ? 0.5F
                            : this.windowY / (this.height - this.windowHeight);
            this.updateChildLayout();
            return true;
        } else {
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (this.browserMouseDown && this.browser != null) {
            this.browser.sendMouseRelease(
                    this.toBrowserPixelX(event.x()),
                    this.toBrowserPixelY(event.y()),
                    event.button());
            this.browserMouseDown = false;
            return true;
        }

        if (!this.draggingScrollbar && !this.draggingWindow) {
            return super.mouseReleased(event);
        }

        this.draggingScrollbar = false;
        this.draggingWindow = false;
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.easyModeOpen
                && this.browser != null
                && this.isInside(
                        mouseX,
                        mouseY,
                        this.getBrowserX(),
                        this.getBrowserY(),
                        this.getBrowserWidth(),
                        this.getBrowserHeight())) {
            this.browser.sendMouseMove(this.toBrowserPixelX(mouseX), this.toBrowserPixelY(mouseY));
        }

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.updateChildLayout();
        if (this.easyModeOpen
                && this.browser != null
                && this.isInside(
                        mouseX,
                        mouseY,
                        this.getBrowserX(),
                        this.getBrowserY(),
                        this.getBrowserWidth(),
                        this.getBrowserHeight())) {
            this.browser.sendMouseWheel(
                    this.toBrowserPixelX(mouseX), this.toBrowserPixelY(mouseY), scrollY, 0);
            return true;
        } else {
            return this.libraryList.handleMouseScroll(
                            mouseX, mouseY, scrollY, this.visibleSources.size())
                    ? true
                    : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!this.easyModeOpen && this.searchBox.keyPressed(event)) {
            this.filterSources();
            return true;
        }

        if (!this.easyModeOpen && this.selectedScriptName != null && this.selectedHasEasyProject) {
            int keyCode = event.key();

            if (keyCode == 257) {
                this.openEasyMode(this.selectedScriptName);
                return true;
            }
        }

        if (this.easyModeOpen) {
            int currentKeyCode = event.key();

            if (currentKeyCode == 256) {
                this.closeEasyMode();
                return true;
            }
        }

        if (this.easyModeOpen && this.browser != null) {
            this.browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
            this.browser.setFocus(true);
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        if (this.easyModeOpen && this.browser != null) {
            this.browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
            return true;
        } else {
            return super.keyReleased(event);
        }
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (!this.easyModeOpen && this.searchBox.charTyped(event)) {
            this.filterSources();
            return true;
        } else if (this.easyModeOpen && this.browser != null && event.codepoint() != 0) {
            this.browser.sendKeyTyped((char) event.codepoint(), 0);
            return true;
        } else {
            return super.charTyped(event);
        }
    }

    @Override
    public void onClose() {
        mc.gui.setScreen(this.parentScreen);
    }

    @Override
    public void removed() {
        this.disposeBrowser();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return this.parentScreen instanceof PauseScreen;
    }

    private void ensureBrowserCreated() {
        if (this.browser == null && MCEF.isInitialized()) {
            if (this.messageRouter == null) {

                this.messageRouter =
                        CefMessageRouter.create(
                                new CefMessageRouterConfig(
                                        "astralisEasyHostQuery", "astralisEasyHostCancel"),
                                new ScriptEditorQueryHandler(this));
                MCEF.getClient().getHandle().addMessageRouter(this.messageRouter);
            }

            this.browser = MCEF.createBrowser("https://docs.astralisclient.com/easy-mode/", false);
            this.resizeBrowserToContent();
            this.browser.setFocus(true);
            this.projectPushPending = true;
        }
    }

    public void handleBridgeRequest(String request, CefQueryCallback callback) {

        if (request != null) {
            int requestLength = request.length();

            if (requestLength <= 2200000) {
                String scriptName;
                String source;
                String projectJson;
                boolean overwrite;
                try {
                    label115:
                    {
                        JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
                        Set requestKeys = jsonObject.keySet();

                        if (requestKeys.equals(
                                Set.of(
                                        "protocol",
                                        "version",
                                        "type",
                                        "name",
                                        "source",
                                        "project",
                                        "issues",
                                        "selectedName",
                                        "overwrite"))) {

                            if ("astralis.easy-mode"
                                    .equals(jsonObject.get("protocol").getAsString())) {

                                if (jsonObject.get("version").getAsInt() == 1) {

                                    if ("save-script"
                                            .equals(jsonObject.get("type").getAsString())) {

                                        scriptName =
                                                ScriptManager.validateScriptName(
                                                        jsonObject.get("name").getAsString());

                                        source = jsonObject.get("source").getAsString();

                                        JsonObject jsonObject2 =
                                                jsonObject.getAsJsonObject("project");
                                        Set projectKeys = jsonObject2.keySet();

                                        if (projectKeys.equals(
                                                Set.of(
                                                        "version",
                                                        "title",
                                                        "category",
                                                        "variables",
                                                        "stacks"))) {

                                            if (jsonObject2.get("version").getAsInt() == 1) {

                                                if (jsonObject2.get("title").isJsonPrimitive()) {

                                                    if (jsonObject2
                                                            .getAsJsonPrimitive("title")
                                                            .isString()) {

                                                        if (!jsonObject2
                                                                .get("title")
                                                                .getAsString()
                                                                .isBlank()) {

                                                            int titleLength =
                                                                    jsonObject2
                                                                            .get("title")
                                                                            .getAsString()
                                                                            .length();

                                                            if (titleLength <= 64) {

                                                                if (jsonObject2
                                                                        .get("category")
                                                                        .isJsonPrimitive()) {

                                                                    if (jsonObject2
                                                                            .getAsJsonPrimitive(
                                                                                    "category")
                                                                            .isString()) {

                                                                        Set allowedCategories =
                                                                                Set.of(
                                                                                        "MISC",
                                                                                        "COMBAT",
                                                                                        "MOVEMENT",
                                                                                        "PLAYER",
                                                                                        "WORLD",
                                                                                        "VISUAL",
                                                                                        "GHOST");

                                                                        if (allowedCategories
                                                                                .contains(
                                                                                        jsonObject2
                                                                                                .get(
                                                                                                        "category")
                                                                                                .getAsString())) {

                                                                            if (jsonObject2
                                                                                    .get(
                                                                                            "variables")
                                                                                    .isJsonArray()) {

                                                                                int variableCount =
                                                                                        jsonObject2
                                                                                                .getAsJsonArray(
                                                                                                        "variables")
                                                                                                .size();

                                                                                if (variableCount
                                                                                        <= 128) {

                                                                                    if (jsonObject2
                                                                                            .get(
                                                                                                    "stacks")
                                                                                            .isJsonArray()) {

                                                                                        int
                                                                                                stackCount =
                                                                                                        jsonObject2
                                                                                                                .getAsJsonArray(
                                                                                                                        "stacks")
                                                                                                                .size();

                                                                                        if (stackCount
                                                                                                <= 32) {
                                                                                            projectJson =
                                                                                                    jsonObject2
                                                                                                            .toString();

                                                                                            overwrite =
                                                                                                    jsonObject
                                                                                                            .get(
                                                                                                                    "overwrite")
                                                                                                            .getAsBoolean();

                                                                                            if (jsonObject
                                                                                                    .get(
                                                                                                            "issues")
                                                                                                    .getAsString()
                                                                                                    .isBlank()) {
                                                                                                String
                                                                                                        editingName =
                                                                                                                this
                                                                                                                        .editingScriptName;

                                                                                                String
                                                                                                        selectedName;
                                                                                                if (jsonObject
                                                                                                        .get(
                                                                                                                "selectedName")
                                                                                                        .isJsonNull()) {
                                                                                                    selectedName =
                                                                                                            null;
                                                                                                } else {

                                                                                                    selectedName =
                                                                                                            jsonObject
                                                                                                                    .get(
                                                                                                                            "selectedName")
                                                                                                                    .getAsString();
                                                                                                }

                                                                                                if (Objects
                                                                                                        .equals(
                                                                                                                editingName,
                                                                                                                selectedName)) {
                                                                                                    int
                                                                                                            sourceBytes =
                                                                                                                    source
                                                                                                                            .getBytes(
                                                                                                                                    StandardCharsets
                                                                                                                                            .UTF_8)
                                                                                                                            .length;

                                                                                                    if (sourceBytes
                                                                                                            <= 1048576) {
                                                                                                        int
                                                                                                                projectBytes =
                                                                                                                        projectJson
                                                                                                                                .getBytes(
                                                                                                                                        StandardCharsets
                                                                                                                                                .UTF_8)
                                                                                                                                .length;

                                                                                                        if (projectBytes
                                                                                                                <= 1048576) {
                                                                                                            String
                                                                                                                    quotedName =
                                                                                                                            Pattern
                                                                                                                                    .quote(
                                                                                                                                            scriptName);

                                                                                                            if (Pattern
                                                                                                                    .compile(
                                                                                                                            "\\bpublic\\s+class\\s+"
                                                                                                                                    + quotedName
                                                                                                                                    + "\\s+extends\\s+Script\\b")
                                                                                                                    .matcher(
                                                                                                                            source)
                                                                                                                    .find()) {
                                                                                                                break
                                                                                                                        label115;
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }

                                                                                            throw new IllegalArgumentException(
                                                                                                    "Easy Mode preview is invalid or out of date.");
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        throw new IllegalArgumentException(
                                                "Easy Mode project has an unsupported structure.");
                                    }
                                }
                            }
                        }

                        throw new IllegalArgumentException("Unsupported Easy Mode bridge request.");
                    }
                } catch (RuntimeException e) {

                    callback.failure(2, "Easy Mode did not provide a valid project.");
                    return;
                }

                mc.execute(
                        () ->
                                this.saveBridgeProject(
                                        scriptName, source, projectJson, overwrite, callback));
                return;
            }
        }

        callback.failure(1, "Easy Mode request is too large.");
    }

    private void saveBridgeProject(
            String scriptName,
            String source,
            String projectJson,
            boolean overwrite,
            CefQueryCallback callback) {
        ScriptManager scriptManager = Astralis.getInstance().getScriptManager();

        try {
            ScriptSource existingScript =
                    scriptManager.getSources().stream()
                            .filter(script -> script.name().equals(scriptName))
                            .findFirst()
                            .orElse(null);
            if (existingScript != null && !overwrite) {
                String revision = scriptManager.readScriptSource(scriptName).revision();
                long nowMillis = Util.getMillis();

                this.pendingOverwrite =
                        new PendingScriptOverwrite(
                                scriptName, source, projectJson, revision, nowMillis + 30000L);

                callback.success(
                        this.createBridgeResponse(
                                "conflict",
                                scriptName,
                                scriptName
                                        + ".java already exists. Confirm overwrite to replace it."));
                return;
            }

            if (existingScript != null) {
                if (this.pendingOverwrite == null
                        || Util.getMillis() > this.pendingOverwrite.getExpiresAtMillis()
                        || !this.pendingOverwrite.getName().equals(scriptName)
                        || !this.pendingOverwrite.getSource().equals(source)
                        || !this.pendingOverwrite.getProject().equals(projectJson)) {

                    callback.success(
                            this.createBridgeResponse(
                                    "conflict",
                                    scriptName,
                                    "The overwrite confirmation expired. Try saving again."));
                    return;
                }

                scriptManager.overwriteScript(
                        scriptName, source, projectJson, this.pendingOverwrite.getRevision());
            } else {
                scriptManager.createScript(scriptName, source, projectJson);
            }

            this.pendingOverwrite = null;
            this.editingScriptName = scriptName;
            this.selectedScriptName = scriptName;
            ReloadResult reloadResult = scriptManager.reload();
            String statusText;
            switch (ScriptEditorScreenSwitchMap.reloadResultOrdinals[reloadResult.ordinal()]) {
                case 1:
                    statusText = scriptName + " saved and loaded.";
                    break;
                case 2:
                    statusText = scriptName + " saved; reload queued.";
                    break;
                case 3:
                    statusText = scriptName + " saved with load errors.";
                    break;
                case 4:
                    statusText = scriptName + " saved but did not compile.";
                    break;
                case 5:
                    statusText = scriptName + " saved; reload failed.";
                    break;
                default:
                    throw new MatchException(null, null);
            }

            this.statusMessage = statusText;
            this.refreshSources();

            callback.success(this.createBridgeResponse("saved", scriptName, this.statusMessage));
        } catch (IOException | RuntimeException e) {

            Astralis.LOGGER.warn("Could not save Easy Mode script {}", scriptName, e);

            String errorMessage;
            if (e.getMessage() != null) {
                errorMessage = e.getMessage();
            } else {

                errorMessage = "Could not save the Easy Mode script.";
            }

            callback.success(this.createBridgeResponse("error", scriptName, errorMessage));
        }
    }

    private String createBridgeResponse(String status, String scriptName, String message) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("protocol", "astralis.easy-mode");

        jsonObject.addProperty("version", 1);

        jsonObject.addProperty("status", status);

        jsonObject.addProperty("message", message);

        jsonObject.addProperty("name", scriptName);
        return jsonObject.toString();
    }

    private void sendProjectToBrowser() {
        if (this.browser != null && !this.isNotEasyModeUrl(this.browser.getURL())) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("protocol", "astralis.easy-mode");

            jsonObject.addProperty("version", 1);

            jsonObject.addProperty("type", "open-project");
            if (this.editingScriptName == null) {

                jsonObject.add("selectedName", null);

                jsonObject.add("project", null);
            } else {

                jsonObject.addProperty("selectedName", this.editingScriptName);

                try {

                    jsonObject.add(
                            "project",
                            JsonParser.parseString(
                                    Astralis.getInstance()
                                            .getScriptManager()
                                            .readEasyModeProject(this.editingScriptName)
                                            .project()));

                    this.statusMessage = this.editingScriptName + " opened in Easy Mode.";
                } catch (IOException | RuntimeException ignored) {

                    jsonObject.add("project", null);

                    this.statusMessage =
                            "Handwritten Java cannot be converted to blocks. Started a new project.";
                }
            }

            MCEFBrowser activeBrowser = this.browser;
            String projectMessage = String.valueOf(jsonObject);

            String javascript = "window.astralisEasyModeHost?.receive(" + projectMessage + ");";

            activeBrowser.executeJavaScript(
                    javascript, "https://docs.astralisclient.com/easy-mode/", 0);
        }
    }

    private void openEasyMode(String scriptName) {
        this.disposeBrowser();
        this.easyModeOpen = true;
        this.editingScriptName = scriptName;
        this.selectedScriptName = scriptName;
        this.pendingOverwrite = null;
        String message;
        if (scriptName == null) {

            message = "New Easy Mode project.";
        } else {

            message = "Opening " + scriptName + "…";
        }

        this.statusMessage = message;
        this.init();
    }

    private void closeEasyMode() {
        this.disposeBrowser();
        this.easyModeOpen = false;
        this.editingScriptName = null;

        this.statusMessage = "Open a script in Easy Mode, or edit its source from the folder.";
        this.init();
    }

    private void disposeBrowser() {
        this.browserMouseDown = false;
        this.projectPushPending = false;
        this.pendingOverwrite = null;
        if (this.messageRouter != null) {
            if (MCEF.isInitialized()) {
                MCEF.getClient().getHandle().removeMessageRouter(this.messageRouter);
            }

            this.messageRouter.dispose();
            this.messageRouter = null;
        }

        if (this.browser != null) {
            this.browser.close();
            this.browser = null;
        }
    }

    private void resizeBrowserToContent() {
        if (this.browser != null) {
            this.browser.resize(
                    Math.max(1, Math.round(this.getBrowserWidth() * mc.getWindow().getGuiScale())),
                    Math.max(
                            1, Math.round(this.getBrowserHeight() * mc.getWindow().getGuiScale())));
        }
    }

    private void reloadScripts() {
        String statusText;
        switch (ScriptEditorScreenSwitchMap.reloadResultOrdinals[
                Astralis.getInstance().getScriptManager().reload().ordinal()]) {
            case 1:
                statusText = "Reload completed.";
                break;
            case 2:
                statusText = "Reload queued until the current script callback finishes.";
                break;
            case 3:
                statusText = "Reload completed with load errors; check chat.";
                break;
            case 4:
                statusText = "Reload rejected because a source did not compile.";
                break;
            case 5:
                statusText = "Reload failed because the scripts folder could not be scanned.";
                break;
            default:
                throw new MatchException(null, null);
        }

        this.statusMessage = statusText;
        this.refreshSources();
    }

    private void refreshSources() {
        this.allSources = Astralis.getInstance().getScriptManager().getSources();
        if (this.selectedScriptName != null
                && this.allSources.stream()
                        .noneMatch(source -> source.name().equals(this.selectedScriptName))) {
            this.selectedScriptName = null;
        }

        this.filterSources();
        if (!this.easyModeOpen
                && this.selectedScriptName == null
                && this.windowWidth >= 520.0F
                && !this.visibleSources.isEmpty()) {
            this.selectedScriptName = this.visibleSources.getFirst().name();
        }

        this.loadSelectedSourcePreview();
    }

    private void filterSources() {
        String query = this.searchBox.getQuery().strip().toLowerCase(Locale.ROOT);
        this.visibleSources =
                !this.easyModeOpen && !query.isEmpty()
                        ? this.allSources.stream()
                                .filter(
                                        source ->
                                                source.name()
                                                        .toLowerCase(Locale.ROOT)
                                                        .contains(query))
                                .toList()
                        : this.allSources;
        if (this.selectedScriptName != null
                && this.visibleSources.stream()
                        .noneMatch(source -> source.name().equals(this.selectedScriptName))) {
            this.selectedScriptName = null;
            this.sourcePreviewLines = List.of();
            this.selectedHasEasyProject = false;
        }

        this.updateChildLayout();
    }

    private void updateChildLayout() {
        this.libraryList.setLayout(
                this.windowX + 8.0F,
                this.windowY + 64.0F,
                this.easyModeOpen
                        ? Math.max(0.0F, this.getEditorSidebarWidth() - 12.0F)
                        : Math.max(0.0F, this.windowWidth - this.getDetailsPanelWidth() - 12.0F),
                Math.max(0.0F, this.windowHeight - 90.0F),
                this.easyModeOpen);
        this.libraryList.clampScroll(this.visibleSources.size());
        float libraryWidth = this.windowWidth - this.getDetailsPanelWidth();
        float searchWidth = Math.min(140.0F, libraryWidth * 0.45F);
        this.searchBox.setBounds(
                this.windowX + libraryWidth - searchWidth - 10.0F,
                this.windowY + 34.0F,
                searchWidth);
    }

    private float getDetailsPanelWidth() {
        if (this.easyModeOpen || this.selectedScriptName == null) {
            return 0.0F;
        } else {
            return this.windowWidth >= 520.0F ? 282.0F : this.windowWidth;
        }
    }

    private void loadSelectedSourcePreview() {
        this.sourcePreviewLines = List.of();
        this.selectedHasEasyProject = false;
        if (this.selectedScriptName != null) {
            try {
                Stream lines =
                        Astralis.getInstance()
                                .getScriptManager()
                                .readScriptSource(this.selectedScriptName)
                                .source()
                                .lines();

                this.sourcePreviewLines = lines.limit(40L).toList();
            } catch (IOException | RuntimeException ignored) {

                this.sourcePreviewLines = List.of("Source preview unavailable.");
            }

            try {
                Astralis.getInstance()
                        .getScriptManager()
                        .readEasyModeProject(this.selectedScriptName);
                this.selectedHasEasyProject = true;
            } catch (IOException | RuntimeException ignored) {
                this.selectedHasEasyProject = false;
            }
        }
    }

    private float getEditorSidebarWidth() {
        return this.windowWidth < 480.0F ? 0.0F : 184.0F;
    }

    private float getBrowserX() {
        return this.windowX + this.getEditorSidebarWidth();
    }

    private float getBrowserY() {
        return this.windowY + 28.0F;
    }

    private float getBrowserWidth() {
        return Math.max(1.0F, this.windowWidth - this.getEditorSidebarWidth());
    }

    private float getBrowserHeight() {
        return Math.max(1.0F, this.windowHeight - 28.0F);
    }

    private int toBrowserPixelX(double screenX) {
        return (int) ((screenX - this.getBrowserX()) * mc.getWindow().getGuiScale());
    }

    private int toBrowserPixelY(double screenY) {
        return (int) ((screenY - this.getBrowserY()) * mc.getWindow().getGuiScale());
    }

    public boolean isNotEasyModeUrl(String url) {

        if (url == null) {
            return true;
        }

        try {
            URI uri = URI.create(url);

            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                String host = uri.getHost();

                if (host.equalsIgnoreCase("docs.astralisclient.com")) {
                    if (uri.getPort() != -1) {
                        int port = uri.getPort();

                        if (port != 443) {
                            return true;
                        }
                    }

                    if (!"/easy-mode".equals(uri.getPath())) {

                        if (!"/easy-mode/".equals(uri.getPath())) {
                            return true;
                        }
                    }

                    return false;
                }
            }

            return true;
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    private boolean isInside(
            double pointX,
            double pointY,
            float rectX,
            float rectY,
            float rectWidth,
            float rectHeight) {
        return pointX >= rectX
                && pointX <= rectX + rectWidth
                && pointY >= rectY
                && pointY <= rectY + rectHeight;
    }

    static {
        windowFractionX = 0.5F;
        windowFractionY = 0.5F;
    }
}
