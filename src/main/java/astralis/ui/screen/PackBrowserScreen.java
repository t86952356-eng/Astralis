package astralis.ui.screen;

import astralis.Astralis;
import astralis.resourcepack.PackThumbnailCache;
import astralis.resourcepack.PvprpClient;
import astralis.resourcepack.PvprpPack;
import astralis.ui.animation.Animation;
import astralis.ui.component.PackBrowserPalette;
import astralis.ui.component.PackBrowserPaletteColors;
import astralis.ui.component.PackBrowserToolbar;
import astralis.ui.component.PackDetailsPanel;
import astralis.ui.component.PackGridPanel;
import astralis.ui.component.PackSortOrder;
import astralis.ui.component.SapphireScreen;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public class PackBrowserScreen extends Screen
        implements InstanceAccess, SapphireScreen, PackBrowserPalette {
    public static final float WINDOW_WIDTH = 700.0F;
    public static final float WINDOW_HEIGHT = 408.0F;
    public static final float HEADER_HEIGHT = 28.0F;
    public static final float DETAIL_PANEL_WIDTH = 250.0F;
    public static final float CORNER_RADIUS = 8.0F;
    private static float savedOffsetX;
    private static float savedOffsetY;
    private final Screen parentScreen;
    private final PvprpClient packApi;
    private final PackThumbnailCache thumbnailCache;
    private final PackBrowserToolbar headerBar;
    private final PackDetailsPanel detailPanel;
    private final PackGridPanel packGrid;
    private final Animation openAnimation;
    private final Set<String> downloadingPackIds;
    private final Set<String> downloadedPackIds;
    private final Map<String, Float> downloadProgressByPackId;
    private volatile List<PvprpPack> packs;
    private volatile String statusMessage;
    private PackSortOrder sortOrder;
    private String resolutionFilter;
    private String versionFilter;
    private int currentPage;
    private volatile boolean loading;
    private volatile boolean morePagesAvailable;
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
    private int requestCounter;
    private boolean draggingWindow;
    private boolean screenClosed;

    public PackBrowserScreen(Screen screen) {

        super(Component.literal("Pack Browser"));
        this.packApi = new PvprpClient();
        this.thumbnailCache = new PackThumbnailCache();
        this.headerBar = new PackBrowserToolbar(this);
        this.detailPanel = new PackDetailsPanel(this, this.thumbnailCache);
        this.packGrid = new PackGridPanel(this, this.thumbnailCache, this.detailPanel);
        this.openAnimation = new Animation(Easing.EASE_OUT_BACK, 420L);
        this.downloadingPackIds = ConcurrentHashMap.newKeySet();
        this.downloadedPackIds = ConcurrentHashMap.newKeySet();
        this.downloadProgressByPackId = new ConcurrentHashMap<>();
        this.packs = List.of();

        this.statusMessage = "Ready";
        this.sortOrder = PackSortOrder.MOST_DOWNLOADED;
        this.resolutionFilter = "";
        this.versionFilter = "";
        this.currentPage = 1;
        this.parentScreen = screen;
    }

    public String getVersionFilter() {
        return this.versionFilter;
    }

    @Override
    protected void init() {
        super.init();
        this.offsetX = savedOffsetX;
        this.offsetY = savedOffsetY;
        this.openAnimation.setValue(0.0);
        this.openAnimation.restart();
        this.layoutComponents();
        if (this.packs.isEmpty() && !this.loading) {
            this.reloadPacks();
        }
    }

    @Override
    public void removed() {
        this.screenClosed = true;
        this.thumbnailCache.close();
    }

    @Override
    public void onClose() {
        mc.gui.setScreen(this.parentScreen);
    }

    @Override
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
        this.layoutComponents();
        this.detailPanel.mouseDragged(mouseX, mouseY);
        this.packGrid.mouseDragged(mouseX, mouseY);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        if (!(this.parentScreen instanceof PauseScreen)) {
            super.extractBackground(guiGraphicsExtractor, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return this.parentScreen instanceof PauseScreen;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        } else if (event.button() != 0) {
            return false;
        } else {
            this.headerBar.click(event);
            if (this.headerBar.isMouseOverControls(event.x(), event.y())) {
                return true;
            } else {
                this.detailPanel.click(event);
                if (this.detailPanel.isMouseOverPanel(event.x(), event.y())) {
                    return true;
                } else {
                    this.packGrid.click(event);
                    if (this.packGrid.isMouseOverList(event.x(), event.y())) {
                        return true;
                    } else if (RenderUtil.isInsideRect(
                            event.x(), event.y(), this.windowX, this.windowY, 700.0F, 28.0F)) {
                        this.draggingWindow = true;
                        this.dragStartMouseX = (float) event.x();
                        this.dragStartMouseY = (float) event.y();
                        this.dragStartOffsetX = this.offsetX;
                        this.dragStartOffsetY = this.offsetY;
                        return true;
                    } else {
                        return RenderUtil.isInsideRect(
                                event.x(), event.y(), this.windowX, this.windowY, 700.0F, 408.0F);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        this.draggingWindow = false;
        this.packGrid.release(event);
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.packGrid.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                ? true
                : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderSapphire(Render2DContext context) {

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 12.0F);
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        float openScale = (float) this.openAnimation.getValue();
        float centerX = this.windowX + 350.0F;
        float centerY = this.windowY + 204.0F;
        context.pushPose();
        context.translate(centerX, centerY);
        context.scale(openScale, openScale, 1.0F);
        context.translate(-centerX, -centerY);
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY, 700.0F, 408.0F)
                        .setRadius(8.0F)
                        .setColor(PackBrowserPaletteColors.BACKGROUND));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY, 700.0F, 28.0F)
                        .setCornerRadii(8.0F, 8.0F, 0.0F, 0.0F)
                        .setColor(PackBrowserPaletteColors.BACKGROUND_DEEP));
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.windowX, this.windowY + 28.0F - 1.0F, 700.0F, 1.0F)
                        .setColor(PackBrowserPaletteColors.DIVIDER));
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        TextDrawRequest styledTitleRequest =
                titleRequest
                        .setText("Pack Browser")
                        .setPosition(this.windowX + 11.0F, this.windowY)
                        .centerVertically(28.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow();
        ShapePaint titlePaint = ShapePaint.obtain();
        int gradientStartColor =
                ColorUtil.waveBlend(this.windowX + 11.0F, color.getRGB(), color2.getRGB());
        float titleStartX = this.windowX + 11.0F;

        int gradientEndColor =
                ColorUtil.waveBlend(
                        titleStartX + titleFont.getStringWidth("Pack Browser"),
                        color.getRGB(),
                        color2.getRGB());
        float gradientStartX = this.windowX + 11.0F;
        float gradientEndBaseX = this.windowX + 11.0F;

        context.drawText(
                styledTitleRequest.setPaint(
                        titlePaint.setLinearGradient(
                                gradientStartColor,
                                gradientEndColor,
                                gradientStartX,
                                this.windowY,
                                gradientEndBaseX + titleFont.getStringWidth("Pack Browser"),
                                this.windowY)));
        this.headerBar.render(context, this.lastMouseX, this.lastMouseY);
        this.packGrid.render(context, this.lastMouseX, this.lastMouseY);
        this.detailPanel.render(context, this.lastMouseX, this.lastMouseY);
        context.popPose();
        Astralis.getInstance()
                .getRenderEffects()
                .renderShadowShapes(
                        shadowContext -> {
                            shadowContext.pushPose();
                            shadowContext.translate(centerX, centerY);
                            shadowContext.scale(openScale, openScale, 1.0F);
                            shadowContext.translate(-centerX, -centerY);
                            shadowContext.drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(this.windowX, this.windowY, 700.0F, 408.0F)
                                            .setRadius(8.0F)
                                            .setColor(Color.BLACK));
                            shadowContext.popPose();
                        });
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.headerBar.keyPressed(event)) {
            return true;
        }

        if (event.hasControlDown()) {
            int ctrlKeyCode = event.key();

            if (ctrlKeyCode == 70) {
                this.headerBar.focusSearchField();
                return true;
            }
        }

        int keyCode = event.key();

        if (keyCode == 82) {
            this.reloadPacks();
            return true;
        } else if (event.isEscape() && this.detailPanel.isOpen()) {
            this.detailPanel.closePanel();
            return true;
        } else if (event.isEscape()) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return this.headerBar.charTyped(event) ? true : super.charTyped(event);
    }

    public void cycleVersionFilter() {

        this.versionFilter =
                switch (this.versionFilter) {
                    case "" -> {
                        yield "1.8";
                    }
                    case "1.8" -> {
                        yield "1.12";
                    }
                    case "1.12" -> {
                        yield "1.16";
                    }
                    case "1.16" -> {
                        yield "1.20";
                    }
                    case "1.20" -> {
                        yield "1.21";
                    }
                    case "1.21" -> {
                        yield "26.1";
                    }
                    default -> "";
                };
        this.currentPage = 1;
        this.reloadPacks();
    }

    private void layoutComponents() {
        this.windowX = (this.width - 700.0F) / 2.0F + this.offsetX;
        this.windowY = (this.height - 408.0F) / 2.0F + this.offsetY;
        this.headerBar.setBounds(this.windowX, this.windowY, 700.0F, 28.0F);
        this.packGrid.setBounds(this.windowX, this.windowY + 28.0F, 700.0F, 380.0F);
        this.detailPanel.setBounds(this.windowX, this.windowY + 28.0F, 700.0F, 380.0F);
    }

    public boolean isDownloading(PvprpPack pack) {
        return this.downloadingPackIds.contains(pack.getPageUrl());
    }

    private void reloadPacks() {
        this.requestPacks(false);
    }

    public void cycleSortOrder() {
        PackSortOrder[] sortOrders = PackSortOrder.values();
        this.sortOrder = sortOrders[(this.sortOrder.ordinal() + 1) % sortOrders.length];
        this.currentPage = 1;
        this.reloadPacks();
    }

    public String getDownloadButtonLabel(PvprpPack pack) {

        if (this.downloadedPackIds.contains(pack.getPageUrl())) {

            return "Downloaded";
        }

        if (!this.downloadingPackIds.contains(pack.getPageUrl())) {

            return "Download";
        }

        float progress = this.getDownloadProgress(pack);
        String label;
        if (progress <= 0.0F) {

            label = "Downloading...";
        } else {
            int percent = Math.round(progress * 100.0F);

            label = "Downloading " + percent + "%";
        }

        return label;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public PackSortOrder getSortOrder() {
        return this.sortOrder;
    }

    public String getResolutionFilter() {
        return this.resolutionFilter;
    }

    private String getErrorMessage(Throwable throwable) {
        Throwable cause = throwable;

        while ((cause instanceof CompletionException || cause.getClass() == RuntimeException.class)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() != null && !cause.getMessage().isBlank()
                ? cause.getMessage()
                : cause.getClass().getSimpleName();
    }

    private void requestPacks(boolean loadMore) {
        int requestId = ++this.requestCounter;
        int requestPage = this.currentPage;
        this.loading = true;
        this.morePagesAvailable = false;
        if (!loadMore) {
            this.packs = List.of();
        }

        String loadingStatus;
        if (loadMore) {

            loadingStatus = "Loading more packs from PVPRP";
        } else {

            loadingStatus = "Loading packs from PVPRP";
        }

        this.statusMessage = loadingStatus;
        this.packApi
                .fetchPacks(
                        this.headerBar.getSearchQuery(),
                        this.currentPage,
                        this.resolutionFilter,
                        this.versionFilter,
                        this.sortOrder)
                .whenComplete(
                        (page, throwable) ->
                                mc.execute(
                                        () -> {
                                            if (!this.screenClosed
                                                    && requestId == this.requestCounter) {
                                                this.loading = false;
                                                if (throwable != null) {
                                                    if (loadMore
                                                            && this.currentPage == requestPage) {
                                                        this.currentPage--;
                                                    }

                                                    String errorMessage =
                                                            this.getErrorMessage(throwable);

                                                    this.statusMessage =
                                                            "Couldn't load packs: " + errorMessage;
                                                    Astralis.LOGGER.warn(
                                                            "Failed to load PVPRP packs",
                                                            throwable);
                                                } else {
                                                    if (loadMore) {
                                                        ArrayList<PvprpPack> mergedPacks =
                                                                new ArrayList<>(this.packs);
                                                        HashSet seenPageUrls = new HashSet();
                                                        mergedPacks.forEach(
                                                                existingPack ->
                                                                        seenPageUrls.add(
                                                                                existingPack
                                                                                        .getPageUrl()));

                                                        for (PvprpPack fetchedPack :
                                                                page.getPacks()) {
                                                            if (seenPageUrls.add(
                                                                    fetchedPack.getPageUrl())) {
                                                                mergedPacks.add(fetchedPack);
                                                            }
                                                        }

                                                        this.packs = List.copyOf(mergedPacks);
                                                    } else {
                                                        this.packs = page.getPacks();
                                                    }

                                                    this.morePagesAvailable = page.hasNextPage();
                                                    String resultStatus;
                                                    if (this.packs.isEmpty()) {

                                                        resultStatus =
                                                                "No packs matched these filters";
                                                    } else {
                                                        int packCount = this.packs.size();

                                                        resultStatus =
                                                                "Loaded " + packCount + " packs";
                                                    }

                                                    this.statusMessage = resultStatus;
                                                }
                                            }
                                        }));
    }

    public void downloadPack(PvprpPack pack) {
        if (this.downloadingPackIds.add(pack.getPageUrl())
                && !this.downloadedPackIds.contains(pack.getPageUrl())) {
            this.downloadProgressByPackId.put(pack.getPageUrl(), 0.0F);
            String packName = pack.getName();

            this.statusMessage = "Preparing " + packName;
            this.packApi
                    .downloadPack(
                            pack,
                            mc.getResourcePackDirectory(),
                            (downloadedBytes, totalBytes) -> {
                                if (totalBytes > 0L) {
                                    this.downloadProgressByPackId.put(
                                            pack.getPageUrl(),
                                            Math.clamp(
                                                    (float) downloadedBytes / (float) totalBytes,
                                                    0.0F,
                                                    1.0F));
                                }
                            })
                    .whenComplete(
                            (path, throwable) ->
                                    mc.execute(
                                            () -> {
                                                this.downloadingPackIds.remove(pack.getPageUrl());

                                                if (throwable != null) {
                                                    this.downloadProgressByPackId.remove(
                                                            pack.getPageUrl());
                                                    String errorMessage =
                                                            this.getErrorMessage(throwable);

                                                    this.statusMessage =
                                                            "Failed to download: " + errorMessage;
                                                    Astralis.LOGGER.warn(
                                                            "Failed to download PVPRP pack {}",
                                                            pack.getName(),
                                                            throwable);
                                                } else {
                                                    this.downloadProgressByPackId.put(
                                                            pack.getPageUrl(), 1.0F);
                                                    this.downloadedPackIds.add(pack.getPageUrl());
                                                    String fileName =
                                                            String.valueOf(path.getFileName());

                                                    this.statusMessage =
                                                            "Downloaded "
                                                                    + fileName
                                                                    + " · enable it in Resource Packs";
                                                }
                                            }));
        }
    }

    public List<PvprpPack> getPacks() {
        return this.packs;
    }

    public boolean hasMorePages() {
        return this.morePagesAvailable;
    }

    public boolean isLoading() {
        return this.loading;
    }

    public void cycleResolutionFilter() {
        String resolution = this.resolutionFilter;
        byte resolutionIndex = -1;
        switch (resolution.hashCode()) {
            case 0:
                if (resolution.equals("")) {
                    resolutionIndex = 0;
                }
                break;
            case 56:
                if (resolution.equals("8")) {
                    resolutionIndex = 1;
                }
                break;
            case 1573:
                if (resolution.equals("16")) {
                    resolutionIndex = 2;
                }
                break;
            case 1631:
                if (resolution.equals("32")) {
                    resolutionIndex = 3;
                }
                break;
            case 1726:
                if (resolution.equals("64")) {
                    resolutionIndex = 4;
                }
                break;
            case 48695:
                if (resolution.equals("128")) {
                    resolutionIndex = 5;
                }
                break;
            case 49747:
                if (resolution.equals("256")) {
                    resolutionIndex = 6;
                }
        }
        this.resolutionFilter =
                switch (resolutionIndex) {
                    case 0 -> "8";
                    case 1 -> {
                        yield "16";
                    }
                    case 2 -> {
                        yield "32";
                    }
                    case 3 -> {
                        yield "64";
                    }
                    case 4 -> {
                        yield "128";
                    }
                    case 5 -> {
                        yield "256";
                    }
                    case 6 -> {
                        yield "512";
                    }
                    default -> "";
                };
        this.currentPage = 1;
        this.reloadPacks();
    }

    public float getDownloadProgress(PvprpPack pack) {
        return this.downloadProgressByPackId.getOrDefault(pack.getPageUrl(), 0.0F);
    }

    public boolean canDownload(PvprpPack pack) {
        return !this.downloadingPackIds.contains(pack.getPageUrl())
                && !this.downloadedPackIds.contains(pack.getPageUrl());
    }

    public void loadNextPage() {
        if (this.morePagesAvailable && !this.loading) {
            this.currentPage++;
            this.requestPacks(true);
        }
    }

    public void searchPacks() {
        this.currentPage = 1;
        this.reloadPacks();
    }
}
