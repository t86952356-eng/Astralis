package astralis.ui.screen;

import astralis.Astralis;
import astralis.account.Account;
import astralis.account.AccountManager;
import astralis.account.AccountShopTab;
import astralis.account.AccountType;
import astralis.account.DecryptResult;
import astralis.account.StorageCipher;
import astralis.resourcepack.migration.CookieFileImporter;
import astralis.social.model.ParsedTokens;
import astralis.ui.component.SapphireScreen;
import astralis.ui.input.TextInputUtil;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.NonNull;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NiceAltsScreen extends Screen implements InstanceAccess, SapphireScreen {
    private static byte[] apiKeyFileSecret =
            "astralis-nicealts-key-v1".getBytes(StandardCharsets.UTF_8);
    private static final float CORNER_RADIUS = 10.0F;
    private static final float MAX_WINDOW_WIDTH = 760.0F;
    private static final float MAX_WINDOW_HEIGHT = 560.0F;
    private static final float WINDOW_MARGIN = 10.0F;
    private static final float CONTENT_PADDING = 16.0F;
    private static Color glassColor = new Color(18, 20, 25, 105);
    private static Color glassHoverColor = new Color(18, 20, 25, 150);
    private static Color glassBorderColor = new Color(42, 42, 48, 110);
    private static Color dividerColor = new Color(40, 40, 40);
    private static Color mutedTextColor = new Color(110, 110, 110);
    private static Color secondaryTextColor = new Color(160, 160, 160);
    private static Color errorColor = new Color(200, 70, 70);
    private static HttpClient httpClient = HttpClient.newHttpClient();
    private static NiceAltsProduct[] hypixelProducts = {
        new NiceAltsProduct("Unban (1-7)", "Hypixel unbanned accounts (Level 1-7).", 1),
        new NiceAltsProduct("Unban (8+)", "Hypixel unbanned accounts (Level 8+).", 2),
        new NiceAltsProduct("Unban (Ranked)", "Hypixel unbanned accounts with ranks.", 3),
        new NiceAltsProduct("Unbanned Cookies", "Unbanned accounts delivered as cookie files.", 9)
    };
    private static NiceAltsProduct customUnbanProduct =
            new NiceAltsProduct("Unban (Custom)", "Custom server unbanned accounts.", -1, true);
    private static NiceAltsProduct[] otherProducts = {
        new NiceAltsProduct("Donut", "DonutSMP unbanned accounts.", 5),
        new NiceAltsProduct("Ban", "Accounts with valid tokens but banned on major servers.", 6),
        customUnbanProduct
    };
    private static NiceAltsProduct[] generatorProducts = {
        new NiceAltsProduct(
                "Unban (1-7)", "Hypixel unbanned accounts (Level 1-7).", 1, false, "Unbanned"),
        new NiceAltsProduct(
                "Banned",
                "Accounts with valid tokens but banned on major servers.",
                6,
                false,
                "Banned"),
        new NiceAltsProduct("Donut", "DonutSMP unbanned accounts.", 5, false, "DonutSMP")
    };
    private final Screen parentScreen;
    private final Object apiKeyInputKey;
    private final Object serverInputKey;
    private File apiKeyFile;
    private String apiKey;
    private boolean showingApiKeyPrompt;
    private String apiKeyInput;
    private String statusMessage;
    private AccountShopTab currentTab;
    private boolean customPurchaseOpen;
    private String customServer;
    private int customQuantity;
    private String customProtocol;
    private String customBanMessageMode;
    private String customSkinMode;
    private boolean customServerFocused;
    private static List<String> trendingSkinIds = new ArrayList<>();
    private static Map<String, Identifier> skinTextures = new ConcurrentHashMap<>();
    private static Set<String> loadingSkinIds = ConcurrentHashMap.newKeySet();
    private static Map<Integer, String> productSkinIds = new ConcurrentHashMap<>();
    private static Pattern skinIdPattern =
            Pattern.compile("3d/skin/body\\.png\\?id=([a-f0-9]{16})");
    private static Pattern tokenAccountNamePattern = Pattern.compile("^\\[[^]]*]\\[([^]]+)]");
    private static volatile boolean skinListRequested = false;
    private final Map<Integer, Integer> stockByProductId;
    private final Map<String, Integer> priceByProductId;
    private long lastStockRefreshMillis;
    private volatile int creditBalance;
    private volatile boolean historyLoaded;
    private final List<NiceAltsHistoryEntry> purchaseHistory;
    private final List<NiceAltsHistoryEntry> generatorHistory;
    private volatile int lastMouseX;
    private volatile int lastMouseY;

    private static URI createApiUri(String path) {

        String separator = path.contains("?") ? "&" : "?";
        String requestPath = path;

        return URI.create("https://app.nicealts.com" + requestPath + separator + "ref=astralis");
    }

    public NiceAltsScreen(Screen screen) {
        super(Component.empty());
        this.apiKeyInputKey = new Object();
        this.serverInputKey = new Object();
        this.apiKeyInput = "";

        this.statusMessage = "Idle";
        this.currentTab = AccountShopTab.SHOP;
        this.customServer = "";
        this.customQuantity = 1;

        this.customProtocol = "1.21";

        this.customBanMessageMode = "normal";

        this.customSkinMode = "default";
        this.stockByProductId = new ConcurrentHashMap<>();
        this.priceByProductId = new ConcurrentHashMap<>();
        this.creditBalance = -1;
        this.purchaseHistory = new CopyOnWriteArrayList<>();
        this.generatorHistory = new CopyOnWriteArrayList<>();
        this.parentScreen = screen;
    }

    @Override
    protected void init() {
        super.init();

        this.apiKeyFile = new File(mc.gameDirectory, "astralis/nicealts.key");
        this.apiKey = this.loadStoredApiKey();
        this.showingApiKeyPrompt = this.apiKey == null || this.apiKey.isEmpty();
        if (!this.showingApiKeyPrompt) {
            this.refreshStock();
            this.refreshBalance();
            this.refreshHistory();
        }

        ensureNameMcSkinsLoaded();
    }

    @Override
    public void onClose() {
        mc.gui.setScreen(this.parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderSapphire(Render2DContext renderer) {
        if (this.showingApiKeyPrompt) {
            this.renderApiKeyPrompt(renderer);
        } else {
            switch (this.currentTab) {
                case SHOP:
                    this.renderShop(renderer);
                    break;
                case GENERATOR:
                    this.renderGenerator(renderer);
                    break;
                case PURCHASE_HISTORY:
                    this.renderHistory(renderer, false);
                    break;
                case GENERATOR_HISTORY:
                    this.renderHistory(renderer, true);
            }

            if (this.customPurchaseOpen) {
                this.renderCustomPurchaseDialog(renderer);
            }
        }
    }

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    private void renderApiKeyPrompt(Render2DContext renderer) {

        int mouseX = this.lastMouseX;
        int mouseY = this.lastMouseY;
        float panelX = (this.width - 320.0F) / 2.0F;
        float panelY = (this.height - 150.0F) / 2.0F;
        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();
        this.drawGlass(renderer, panelX, panelY, 320.0F, 150.0F, 10.0F, false);

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 14.0F);

        SizedFont hintFont = FontManager.getFont("Product Sans Regular", 8.0F);

        SizedFont inputFont = FontManager.getFont("Product Sans Regular", 9.0F);

        SizedFont buttonFont = FontManager.getFont("Product Sans Medium", 9.0F);

        float titleX =
                panelX + (320.0F - titleFont.getStringWidth("Enter NiceAlts API Key")) / 2.0F;
        float titleY = panelY + 16.0F;

        float titleWidth = titleFont.getStringWidth("Enter NiceAlts API Key");
        int gradientStart = ColorUtil.waveBlend(titleX, color.getRGB(), color2.getRGB());
        int gradientEnd = ColorUtil.waveBlend(titleX + titleWidth, color.getRGB(), color2.getRGB());
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        renderer.drawText(
                titleRequest
                        .setText("Enter NiceAlts API Key")
                        .setPosition(titleX, titleY)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
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

        TextDrawRequest hintRequest = TextDrawRequest.obtain();

        TextDrawRequest hintTextRequest =
                hintRequest.setText(
                        "Find your API key at https://nicealts.com/?ref=astralis under your account.");

        renderer.drawText(
                hintTextRequest
                        .setPosition(
                                panelX
                                        + (320.0F
                                                        - hintFont.getStringWidth(
                                                                "Find your API key at https://nicealts.com/?ref=astralis under your account."))
                                                / 2.0F,
                                titleY + titleFont.getLineHeight() + 6.0F)
                        .setFont(hintFont.getMsdfFont())
                        .setFontSize(hintFont.getSize())
                        .enableShadow()
                        .setColor(mutedTextColor.getRGB()));
        float inputX = panelX + 20.0F;
        float inputY = panelY + 60.0F;
        this.drawGlass(renderer, inputX, inputY, 280.0F, 26.0F, 6.0F, false);
        String shownText;
        if (this.apiKeyInput.isEmpty()) {

            shownText = "Paste API key (Ctrl+V)...";
        } else {
            shownText = this.maskApiKey(this.apiKeyInput);
        }

        String displayText = shownText;
        int inputTextColor =
                this.apiKeyInput.isEmpty() ? mutedTextColor.getRGB() : Color.WHITE.getRGB();
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(displayText)
                        .setPosition(inputX + 10.0F, inputY)
                        .centerVertically(26.0F)
                        .setFont(inputFont.getMsdfFont())
                        .setFontSize(inputFont.getSize())
                        .enableShadow()
                        .setColor(inputTextColor));
        float saveButtonX = inputX;
        float cancelButtonX = inputX + 135.0F + 10.0F;
        float buttonY = inputY + 26.0F + 12.0F;
        boolean saveHovered = this.isInside(mouseX, mouseY, saveButtonX, buttonY, 135.0F, 26.0F);
        boolean cancelHovered =
                this.isInside(mouseX, mouseY, cancelButtonX, buttonY, 135.0F, 26.0F);
        this.drawGlass(renderer, saveButtonX, buttonY, 135.0F, 26.0F, 6.0F, saveHovered);
        TextDrawRequest saveRequest = TextDrawRequest.obtain();

        renderer.drawText(
                saveRequest
                        .setText("Save")
                        .setPosition(saveButtonX, buttonY)
                        .centerInBox(135.0F, 26.0F)
                        .setFont(buttonFont.getMsdfFont())
                        .setFontSize(buttonFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        this.drawGlass(renderer, cancelButtonX, buttonY, 135.0F, 26.0F, 6.0F, cancelHovered);
        TextDrawRequest cancelRequest = TextDrawRequest.obtain();

        renderer.drawText(
                cancelRequest
                        .setText("Cancel")
                        .setPosition(cancelButtonX, buttonY)
                        .centerInBox(135.0F, 26.0F)
                        .setFont(buttonFont.getMsdfFont())
                        .setFontSize(buttonFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
    }

    private void renderShop(Render2DContext renderer) {
        int mouseX = this.lastMouseX;
        int mouseY = this.lastMouseY;
        float windowX = this.getWindowX();
        float windowY = this.getWindowY();
        float windowWidth = this.getWindowWidth();
        float windowHeight = this.getWindowHeight();
        this.drawWindowBackground(renderer, windowX, windowY, windowWidth, windowHeight);

        this.drawHeader(renderer, "NiceAlts", windowX, windowY, windowWidth, mouseX, mouseY);
        this.renderTabButtons(
                renderer, windowX, windowY, windowWidth, mouseX, mouseY, AccountShopTab.SHOP);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(windowX + 16.0F, windowY + 40.0F, windowWidth - 32.0F, 1.0F)
                        .setColor(dividerColor));
        float sectionY = windowY + 70.0F;
        float cardHeight = this.getProductCardHeight(windowHeight);

        this.renderProductSection(
                renderer,
                "Hypixel",
                hypixelProducts,
                windowX,
                sectionY,
                windowWidth,
                cardHeight,
                mouseX,
                mouseY);

        this.renderProductSection(
                renderer,
                "Other",
                otherProducts,
                windowX,
                sectionY + cardHeight + 18.0F + 22.0F,
                windowWidth,
                cardHeight,
                mouseX,
                mouseY);
        this.drawStatusBadge(renderer, windowX, windowY, windowWidth, windowHeight);
    }

    private void renderProductSection(
            Render2DContext renderer,
            String sectionTitle,
            NiceAltsProduct[] products,
            float windowX,
            float cardY,
            float windowWidth,
            float cardHeight,
            int mouseX,
            int mouseY) {

        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 11.0F);
        float titleX = windowX + 16.0F;
        float titleY = cardY - 18.0F;
        float titleWidth = titleFont.getStringWidth(sectionTitle);
        int gradientStart = ColorUtil.waveBlend(titleX, color.getRGB(), color2.getRGB());
        int gradientEnd = ColorUtil.waveBlend(titleX + titleWidth, color.getRGB(), color2.getRGB());
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(sectionTitle)
                        .setPosition(titleX, titleY)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
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
        int productCount = products.length;
        float cardsX = windowX + 16.0F;
        float cardsWidth = windowWidth - 32.0F;
        float cardWidth = (cardsWidth - 8.0F * (productCount - 1)) / productCount;

        for (int i = 0; i < productCount; i++) {
            NiceAltsProduct product = products[i];
            float cardX = cardsX + i * (cardWidth + 8.0F);

            this.renderProductCard(
                    renderer,
                    product,
                    cardX,
                    cardY,
                    cardWidth,
                    cardHeight,
                    mouseX,
                    mouseY,
                    "Purchase");
        }
    }

    private void renderGenerator(Render2DContext renderer) {

        int mouseX = this.lastMouseX;
        int mouseY = this.lastMouseY;
        float windowX = this.getWindowX();
        float windowY = this.getWindowY();
        float windowWidth = this.getWindowWidth();
        float windowHeight = this.getWindowHeight();
        this.drawWindowBackground(renderer, windowX, windowY, windowWidth, windowHeight);

        this.drawHeader(renderer, "Generator", windowX, windowY, windowWidth, mouseX, mouseY);
        this.renderTabButtons(
                renderer, windowX, windowY, windowWidth, mouseX, mouseY, AccountShopTab.GENERATOR);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(windowX + 16.0F, windowY + 40.0F, windowWidth - 32.0F, 1.0F)
                        .setColor(dividerColor));

        SizedFont stockFont = FontManager.getFont("Product Sans Bold", 11.0F);
        float stockX = windowX + 16.0F;
        float stockY = windowY + 50.0F;

        for (NiceAltsProduct product : generatorProducts) {
            Integer stock = this.stockByProductId.get(product.productId);
            String stockText;
            int stockColor;
            if (stock == null) {
                stockText = product.name + ": ...";
                stockColor = mutedTextColor.getRGB();
            } else if (stock == 0) {
                stockText = product.name + ": 0";
                stockColor = errorColor.getRGB();
            } else {

                stockText = product.name + ": " + stock;

                stockColor = new Color(75, 195, 100).getRGB();
            }

            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(stockText)
                            .setPosition(stockX, stockY)
                            .setFont(stockFont.getMsdfFont())
                            .setFontSize(stockFont.getSize())
                            .enableShadow()
                            .setColor(stockColor));
            stockX += stockFont.getStringWidth(stockText) + 18.0F;
        }

        int productCount = generatorProducts.length;
        float cardHeight = Math.min(175.0F, Math.max(120.0F, windowHeight - 130.0F));
        float cardY = windowY + 90.0F;
        float contentWidth = windowWidth - 32.0F;
        float totalCardsWidth =
                Math.min(contentWidth, 175.0F * productCount + 12.0F * (productCount - 1));
        float cardWidth = (totalCardsWidth - 12.0F * (productCount - 1)) / productCount;
        float cardsX = windowX + (windowWidth - totalCardsWidth) / 2.0F;

        for (int i = 0; i < productCount; i++) {
            float cardX = cardsX + i * (cardWidth + 12.0F);
            NiceAltsProduct cardProduct = generatorProducts[i];

            this.renderProductCard(
                    renderer,
                    cardProduct,
                    cardX,
                    cardY,
                    cardWidth,
                    cardHeight,
                    mouseX,
                    mouseY,
                    "Generate");
        }

        this.drawStatusBadge(renderer, windowX, windowY, windowWidth, windowHeight);
    }

    private void drawWindowBackground(
            Render2DContext renderer, float x, float y, float width, float height) {
        this.drawGlass(renderer, x, y, width, height, 10.0F, false);
    }

    private void drawGlass(
            Render2DContext renderer,
            float x,
            float y,
            float width,
            float height,
            float radius,
            boolean hovered) {
        ShadowRenderer shadowRenderer = renderer.getShadowRenderer();
        Consumer<Render2DContext> shadowShapeDrawer =
                shadowContext ->
                        shadowContext.drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(x, y, width, height)
                                        .setRadius(radius)
                                        .setColor(Color.WHITE));

        Color color = new Color(0, 0, 0, 95);

        shadowRenderer.renderShapes(shadowShapeDrawer, color, 8, 0.55F);
        Astralis.getInstance()
                .getRenderEffects()
                .drawBlurredRoundedRectangle(x, y, width, height, radius, 1.0F);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(x, y, width, height)
                        .setRadius(radius)
                        .setColor(hovered ? glassHoverColor : glassColor)
                        .setOutline(1.0F, glassBorderColor));
    }

    private void renderTabButtons(
            Render2DContext renderer,
            float windowX,
            float windowY,
            float windowWidth,
            int mouseX,
            int mouseY,
            AccountShopTab activeTab) {

        String[] labels;
        if (activeTab == AccountShopTab.GENERATOR) {
            labels = new String[3];

            labels[0] = "Shop";

            labels[1] = "Purchased";

            labels[2] = "Generated";
        } else {
            labels = new String[3];

            labels[0] = "Generator";

            labels[1] = "Purchased";

            labels[2] = "Generated";
        }

        String[] tabLabels = labels;
        AccountShopTab[] tabTargets =
                activeTab == AccountShopTab.GENERATOR
                        ? new AccountShopTab[] {
                            AccountShopTab.SHOP,
                            AccountShopTab.PURCHASE_HISTORY,
                            AccountShopTab.GENERATOR_HISTORY
                        }
                        : new AccountShopTab[] {
                            AccountShopTab.GENERATOR,
                            AccountShopTab.PURCHASE_HISTORY,
                            AccountShopTab.GENERATOR_HISTORY
                        };
        float[] tabWidths = new float[] {72.0F, 70.0F, 70.0F};

        float totalTabsWidth = tabWidths[0] + tabWidths[1] + tabWidths[2] + 10.0F;
        float tabsStartX =
                windowX + windowWidth - this.getAccountInfoWidth() - totalTabsWidth - 32.0F;
        SizedFont tabFont = FontManager.getFont("Product Sans Medium", 8.0F);
        float tabX = tabsStartX;

        for (int i = 0; i < tabLabels.length; i++) {
            boolean hovered =
                    this.isInside(mouseX, mouseY, tabX, windowY + 10.0F, tabWidths[i], 22.0F);
            this.drawGlass(
                    renderer,
                    tabX,
                    windowY + 10.0F,
                    tabWidths[i],
                    22.0F,
                    6.0F,
                    hovered || activeTab == tabTargets[i]);
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(tabLabels[i])
                            .setPosition(tabX, windowY + 10.0F)
                            .centerInBox(tabWidths[i], 22.0F)
                            .setFont(tabFont.getMsdfFont())
                            .setFontSize(tabFont.getSize())
                            .enableShadow()
                            .setColor(Color.WHITE.getRGB()));
            tabX += tabWidths[i] + 5.0F;
        }
    }

    private void renderHistory(Render2DContext renderer, boolean showGeneratorHistory) {

        int mouseX = this.lastMouseX;
        int mouseY = this.lastMouseY;
        float windowX = this.getWindowX();
        float windowY = this.getWindowY();
        float windowWidth = this.getWindowWidth();
        float windowHeight = this.getWindowHeight();
        String titleChoice;
        if (showGeneratorHistory) {

            titleChoice = "Generator History";
        } else {

            titleChoice = "Purchased History";
        }

        String headerTitle = titleChoice;
        List entries = showGeneratorHistory ? this.generatorHistory : this.purchaseHistory;
        this.drawWindowBackground(renderer, windowX, windowY, windowWidth, windowHeight);
        this.drawHeader(renderer, headerTitle, windowX, windowY, windowWidth, mouseX, mouseY);
        this.renderTabButtons(
                renderer,
                windowX,
                windowY,
                windowWidth,
                mouseX,
                mouseY,
                showGeneratorHistory
                        ? AccountShopTab.GENERATOR_HISTORY
                        : AccountShopTab.PURCHASE_HISTORY);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(windowX + 16.0F, windowY + 40.0F, windowWidth - 32.0F, 1.0F)
                        .setColor(dividerColor));

        SizedFont nameFont = FontManager.getFont("Product Sans Bold", 11.0F);

        SizedFont smallFont = FontManager.getFont("Product Sans Regular", 8.0F);
        if (entries.isEmpty()) {
            String emptyMessageChoice;
            if (showGeneratorHistory) {

                emptyMessageChoice = "No generated products yet.";
            } else {

                emptyMessageChoice = "No purchased orders yet.";
            }

            String emptyMessage = emptyMessageChoice;
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(emptyMessage)
                            .setPosition(windowX + 16.0F, windowY + 70.0F)
                            .setFont(smallFont.getMsdfFont())
                            .setFontSize(smallFont.getSize())
                            .enableShadow()
                            .setColor(mutedTextColor.getRGB()));
        } else {
            List sortedEntries = this.sortedHistory(entries);
            float rowY = windowY + 58.0F;

            int rowCount = Math.min(8, sortedEntries.size());

            for (int i = 0; i < rowCount; i++) {
                NiceAltsHistoryEntry entry = (NiceAltsHistoryEntry) sortedEntries.get(i);
                this.drawGlass(
                        renderer, windowX + 16.0F, rowY, windowWidth - 32.0F, 44.0F, 6.0F, false);
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(this.getHistoryAccountName(entry))
                                .setPosition(windowX + 16.0F + 10.0F, rowY + 7.0F)
                                .setFont(nameFont.getMsdfFont())
                                .setFontSize(nameFont.getSize())
                                .enableShadow()
                                .setColor(Color.WHITE.getRGB()));
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(entry.timestamp)
                                .setPosition(windowX + 16.0F + 10.0F, rowY + 25.0F)
                                .setFont(smallFont.getMsdfFont())
                                .setFontSize(smallFont.getSize())
                                .enableShadow()
                                .setColor(secondaryTextColor.getRGB()));
                rowY += 50.0F;
            }
        }

        this.drawStatusBadge(renderer, windowX, windowY, windowWidth, windowHeight);
    }

    private void renderCustomPurchaseDialog(Render2DContext renderer) {

        float dialogX = (this.width - 520.0F) / 2.0F;
        float dialogY = (this.height - 440.0F) / 2.0F;
        RoundedRectShape overlayShape =
                RoundedRectShape.obtain().setBounds(0.0F, 0.0F, this.width, this.height);

        renderer.drawRect(overlayShape.setColor(new Color(0, 0, 0, 145)));
        this.drawGlass(renderer, dialogX, dialogY, 520.0F, 440.0F, 10.0F, false);

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 14.0F);

        SizedFont labelFont = FontManager.getFont("Product Sans Regular", 8.0F);

        SizedFont valueFont = FontManager.getFont("Product Sans Medium", 9.0F);
        TextDrawRequest titleRequest = TextDrawRequest.obtain();

        renderer.drawText(
                titleRequest
                        .setText("Purchase")
                        .setPosition(dialogX + 24.0F, dialogY + 18.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        TextDrawRequest subtitleRequest = TextDrawRequest.obtain();

        renderer.drawText(
                subtitleRequest
                        .setText("Review your custom unban order.")
                        .setPosition(dialogX + 24.0F, dialogY + 39.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(secondaryTextColor.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText("×")
                        .setPosition(dialogX + 520.0F - 30.0F, dialogY + 16.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setColor(secondaryTextColor.getRGB()));
        TextDrawRequest productRequest = TextDrawRequest.obtain();

        renderer.drawText(
                productRequest
                        .setText("Unban (Custom)")
                        .setPosition(dialogX + 24.0F, dialogY + 76.0F)
                        .setFont(valueFont.getMsdfFont())
                        .setFontSize(valueFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        this.drawButton(
                renderer,
                dialogX + 24.0F,
                dialogY + 96.0F,
                32.0F,
                26.0F,
                "−",
                this.lastMouseX,
                this.lastMouseY);
        this.drawButton(
                renderer,
                dialogX + 64.0F,
                dialogY + 96.0F,
                32.0F,
                26.0F,
                String.valueOf(this.customQuantity),
                this.lastMouseX,
                this.lastMouseY);
        this.drawButton(
                renderer,
                dialogX + 104.0F,
                dialogY + 96.0F,
                32.0F,
                26.0F,
                "+",
                this.lastMouseX,
                this.lastMouseY);

        this.drawFieldLabel(renderer, "Server", dialogX + 24.0F, dialogY + 137.0F, labelFont);
        float serverFieldX = dialogX + 24.0F;
        float serverFieldY = dialogY + 148.0F;
        String serverText;
        if (this.customServer.isEmpty()) {
            serverText = "play.example.com";
        } else {
            serverText = this.customServer;
        }

        this.drawSelectableButton(
                renderer,
                serverFieldX,
                serverFieldY,
                472.0F,
                30.0F,
                serverText,
                this.lastMouseX,
                this.lastMouseY,
                this.customServerFocused);

        this.drawFieldLabel(renderer, "Protocol", dialogX + 24.0F, dialogY + 194.0F, labelFont);
        float protocolX = dialogX + 24.0F;
        float protocolY = dialogY + 205.0F;
        String[] protocolOptions = new String[3];

        protocolOptions[0] = "1.8";

        protocolOptions[1] = "1.21";

        protocolOptions[2] = "26.1";
        this.drawOptionButtons(
                renderer,
                protocolX,
                protocolY,
                472.0F,
                30.0F,
                protocolOptions,
                this.customProtocol);

        this.drawFieldLabel(
                renderer, "Server ban system", dialogX + 24.0F, dialogY + 257.0F, labelFont);
        float banSystemX = dialogX + 24.0F;
        float banSystemY = dialogY + 268.0F;
        String[] banSystemOptions = new String[2];

        banSystemOptions[0] = "Default";

        banSystemOptions[1] = "Chat (+1 credit)";
        String selectedBanSystem;
        if (this.customBanMessageMode.equals("chat")) {

            selectedBanSystem = "Chat (+1 credit)";
        } else {

            selectedBanSystem = "Default";
        }

        this.drawOptionButtons(
                renderer,
                banSystemX,
                banSystemY,
                472.0F,
                30.0F,
                banSystemOptions,
                selectedBanSystem);

        this.drawFieldLabel(renderer, "Skin", dialogX + 24.0F, dialogY + 320.0F, labelFont);
        float skinX = dialogX + 24.0F;
        float skinY = dialogY + 331.0F;
        String[] skinOptions = new String[2];

        skinOptions[0] = "Default";

        skinOptions[1] = "Legit";
        String selectedSkin;
        if (this.customSkinMode.equals("legit")) {

            selectedSkin = "Legit";
        } else {

            selectedSkin = "Default";
        }

        this.drawOptionButtons(renderer, skinX, skinY, 472.0F, 30.0F, skinOptions, selectedSkin);
        Integer unitPrice = this.priceByProductId.get("C");
        String totalTextChoice;
        if (unitPrice == null) {

            totalTextChoice = "Price unavailable";
        } else {
            int quantity = this.customQuantity;
            int unitPriceValue = unitPrice;

            int totalCredits =
                    quantity
                            * (unitPriceValue + (this.customBanMessageMode.equals("chat") ? 1 : 0));

            totalTextChoice = totalCredits + " credits total";
        }

        String totalText = totalTextChoice;
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(totalText)
                        .setPosition(dialogX + 520.0F - 145.0F, dialogY + 374.0F)
                        .setFont(valueFont.getMsdfFont())
                        .setFontSize(valueFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        float buyButtonX = dialogX + 24.0F;
        float buyButtonY = dialogY + 394.0F;
        this.drawButton(
                renderer,
                buyButtonX,
                buyButtonY,
                472.0F,
                30.0F,
                "Purchase",
                this.lastMouseX,
                this.lastMouseY);
    }

    private void drawFieldLabel(
            Render2DContext renderer, String text, float x, float y, SizedFont font) {
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(text)
                        .setPosition(x, y)
                        .setFont(font.getMsdfFont())
                        .setFontSize(font.getSize())
                        .enableShadow()
                        .setColor(secondaryTextColor.getRGB()));
    }

    private void drawOptionButtons(
            Render2DContext renderer,
            float x,
            float y,
            float width,
            float height,
            String[] options,
            String selectedOption) {
        float buttonWidth = (width - 5.0F * (options.length - 1)) / options.length;

        for (int i = 0; i < options.length; i++) {
            this.drawSelectableButton(
                    renderer,
                    x + i * (buttonWidth + 5.0F),
                    y,
                    buttonWidth,
                    height,
                    options[i],
                    this.lastMouseX,
                    this.lastMouseY,
                    options[i].equals(selectedOption));
        }
    }

    private void drawButton(
            Render2DContext renderer,
            float x,
            float y,
            float width,
            float height,
            String label,
            int mouseX,
            int mouseY) {
        this.drawSelectableButton(renderer, x, y, width, height, label, mouseX, mouseY, false);
    }

    private void drawSelectableButton(
            Render2DContext renderer,
            float x,
            float y,
            float width,
            float height,
            String label,
            int mouseX,
            int mouseY,
            boolean selected) {

        this.drawGlass(
                renderer,
                x,
                y,
                width,
                height,
                6.0F,
                selected || this.isInside(mouseX, mouseY, x, y, width, height));

        SizedFont labelFont = FontManager.getFont("Product Sans Medium", 8.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(label)
                        .setPosition(x, y)
                        .centerInBox(width, height)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
    }

    private boolean handleDialogClick(double mouseX, double mouseY) {
        float dialogX = (this.width - 520.0F) / 2.0F;
        float dialogY = (this.height - 440.0F) / 2.0F;
        if (!this.isInside(mouseX, mouseY, dialogX, dialogY, 520.0F, 440.0F)) {
            this.customPurchaseOpen = false;
            this.customServerFocused = false;
            return true;
        }

        if (this.isInside(
                mouseX, mouseY, dialogX + 520.0F - 42.0F, dialogY + 10.0F, 32.0F, 32.0F)) {
            this.customPurchaseOpen = false;
            this.customServerFocused = false;
        } else if (this.isInside(mouseX, mouseY, dialogX + 24.0F, dialogY + 96.0F, 32.0F, 26.0F)) {
            this.customQuantity = Math.max(1, this.customQuantity - 1);
        } else if (this.isInside(mouseX, mouseY, dialogX + 104.0F, dialogY + 96.0F, 32.0F, 26.0F)) {

            this.customQuantity = Math.min(10, this.customQuantity + 1);
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F, dialogY + 148.0F, 472.0F, 30.0F)) {
            this.customServerFocused = true;
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F, dialogY + 205.0F, 154.0F, 30.0F)) {

            this.customProtocol = "1.8";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F + 159.0F, dialogY + 205.0F, 154.0F, 30.0F)) {

            this.customProtocol = "1.21";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F + 318.0F, dialogY + 205.0F, 154.0F, 30.0F)) {

            this.customProtocol = "26.1";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F, dialogY + 268.0F, 233.5F, 30.0F)) {

            this.customBanMessageMode = "normal";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F + 238.5F, dialogY + 268.0F, 233.5F, 30.0F)) {

            this.customBanMessageMode = "chat";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F, dialogY + 331.0F, 233.5F, 30.0F)) {

            this.customSkinMode = "default";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F + 238.5F, dialogY + 331.0F, 233.5F, 30.0F)) {

            this.customSkinMode = "legit";
        } else if (this.isInside(
                mouseX, mouseY, dialogX + 24.0F, dialogY + 394.0F, 472.0F, 30.0F)) {
            if (this.customServer.isBlank()) {

                this.statusMessage = "Enter a server address";
            } else if (!this.priceByProductId.containsKey("C")) {

                this.statusMessage = "Custom price is unavailable";
            } else {
                this.customPurchaseOpen = false;
                this.customServerFocused = false;
                this.customPurchase();
            }
        }

        return true;
    }

    private boolean handleTabClick(
            double mouseX, double mouseY, float windowX, float windowY, float windowWidth) {
        boolean onGeneratorTab = this.currentTab == AccountShopTab.GENERATOR;
        float[] tabWidths = new float[] {72.0F, 70.0F, 70.0F};
        float totalTabsWidth = tabWidths[0] + tabWidths[1] + tabWidths[2] + 10.0F;
        float tabX = windowX + windowWidth - this.getAccountInfoWidth() - totalTabsWidth - 32.0F;

        for (int i = 0; i < tabWidths.length; i++) {
            if (this.isInside(mouseX, mouseY, tabX, windowY + 10.0F, tabWidths[i], 22.0F)) {
                this.currentTab =
                        i == 0
                                ? (onGeneratorTab ? AccountShopTab.SHOP : AccountShopTab.GENERATOR)
                                : (i == 1
                                        ? AccountShopTab.PURCHASE_HISTORY
                                        : AccountShopTab.GENERATOR_HISTORY);
                if (this.currentTab == AccountShopTab.PURCHASE_HISTORY) {
                    this.refreshHistory();
                }

                return true;
            }

            tabX += tabWidths[i] + 5.0F;
        }

        return false;
    }

    private float getWindowWidth() {
        return Math.min(760.0F, this.width - 20.0F);
    }

    private float getWindowHeight() {
        return Math.min(560.0F, this.height - 20.0F);
    }

    private float getWindowX() {
        return (this.width - this.getWindowWidth()) / 2.0F;
    }

    private float getWindowY() {
        return (this.height - this.getWindowHeight()) / 2.0F;
    }

    private float getProductCardHeight(float windowHeight) {
        return Math.min(175.0F, Math.max(120.0F, (windowHeight - 140.0F) / 2.0F));
    }

    private void drawHeader(
            Render2DContext renderer,
            String titleText,
            float windowX,
            float windowY,
            float windowWidth,
            int mouseX,
            int mouseY) {

        Color color = Astralis.getInstance().getPrimaryColor();
        Color color2 = Astralis.getInstance().getSecondaryColor();

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 14.0F);
        float titleX = windowX + 16.0F;
        float titleY = windowY + 12.0F;

        if ("NiceAlts".equals(titleText)) {
            AltsLogoRenderer.drawLogo(
                    renderer,
                    titleX,
                    titleY - 3.0F,
                    windowWidth - 32.0F,
                    titleFont.getLineHeight() + 8.0F,
                    false);
        } else {
            float titleWidth = titleFont.getStringWidth(titleText);
            int gradientStart = ColorUtil.waveBlend(titleX, color.getRGB(), color2.getRGB());
            int gradientEnd =
                    ColorUtil.waveBlend(titleX + titleWidth, color.getRGB(), color2.getRGB());
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(titleText)
                            .setPosition(titleX, titleY)
                            .setFont(titleFont.getMsdfFont())
                            .setFontSize(titleFont.getSize())
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
        }

        SizedFont infoFont = FontManager.getFont("Product Sans Regular", 7.0F);
        String infoText = this.getAccountInfoText();

        float infoTextWidth = infoFont.getStringWidth(infoText);
        SizedFont buttonFont = FontManager.getFont("Product Sans Medium", 8.0F);
        float infoTotalWidth = infoTextWidth + 8.0F + 62.0F;
        float infoBlockX = windowX + windowWidth - infoTotalWidth - 16.0F;
        float infoX = infoBlockX;
        float infoY = titleY + 2.0F;
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(infoText)
                        .setPosition(infoX, infoY)
                        .setFont(infoFont.getMsdfFont())
                        .setFontSize(infoFont.getSize())
                        .enableShadow()
                        .setColor(mutedTextColor.getRGB()));
        float logoutX = infoX + infoTextWidth + 8.0F;
        float logoutY = titleY + (titleFont.getLineHeight() - 20.0F) / 2.0F;
        boolean logoutHovered = this.isInside(mouseX, mouseY, logoutX, logoutY, 62.0F, 20.0F);
        this.drawGlass(renderer, logoutX, logoutY, 62.0F, 20.0F, 6.0F, logoutHovered);
        RoundedRectShape logoutShape =
                RoundedRectShape.obtain().setBounds(logoutX, logoutY, 62.0F, 20.0F).setRadius(6.0F);
        Color color3;

        byte logoutAlpha;
        if (logoutHovered) {

            logoutAlpha = 85;
        } else {

            logoutAlpha = 55;
        }

        color3 = new Color(190, 60, 60, logoutAlpha);
        renderer.drawRect(logoutShape.setColor(color3));
        TextDrawRequest logoutRequest = TextDrawRequest.obtain();

        renderer.drawText(
                logoutRequest
                        .setText("Logout")
                        .setPosition(logoutX, logoutY)
                        .centerInBox(62.0F, 20.0F)
                        .setFont(buttonFont.getMsdfFont())
                        .setFontSize(buttonFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
    }

    private String getAccountInfoText() {
        String maskedKey = this.maskApiKey(this.apiKey);
        Serializable creditsDisplay;
        if (this.creditBalance < 0) {

            creditsDisplay = "...";
        } else {
            creditsDisplay = this.creditBalance;
        }

        String creditsText = String.valueOf(creditsDisplay);

        return "Key: " + maskedKey + " | Credits: " + creditsText;
    }

    private float getAccountInfoWidth() {

        SizedFont infoFont = FontManager.getFont("Product Sans Regular", 7.0F);
        float textWidth = infoFont.getStringWidth(this.getAccountInfoText());
        return textWidth + 8.0F + 62.0F;
    }

    private void drawStatusBadge(
            Render2DContext renderer,
            float windowX,
            float windowY,
            float windowWidth,
            float windowHeight) {

        SizedFont statusFont = FontManager.getFont("Product Sans Regular", 8.0F);
        Color color = this.getStatusColor();
        float badgeWidth = statusFont.getStringWidth(this.statusMessage) + 16.0F;
        float badgeX = windowX + (windowWidth - badgeWidth) / 2.0F;
        float badgeY = windowY + windowHeight - 28.0F;

        Color color2;
        if (this.statusMessage.equals("Idle")) {

            color2 = new Color(100, 100, 110, 45);
        } else {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            color2 = new Color(red, green, blue, 30);
        }

        Color color3 = color2;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(badgeX, badgeY, badgeWidth, 15.0F)
                        .setRadius(7.5F)
                        .setColor(color3));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.statusMessage)
                        .setPosition(
                                badgeX
                                        + (badgeWidth
                                                        - statusFont.getStringWidth(
                                                                this.statusMessage))
                                                / 2.0F,
                                badgeY)
                        .setFont(statusFont.getMsdfFont())
                        .setFontSize(statusFont.getSize())
                        .centerVertically(15.0F)
                        .enableShadow()
                        .setColor(color.getRGB()));
    }

    private void renderProductCard(
            Render2DContext renderer,
            NiceAltsProduct product,
            float x,
            float y,
            float width,
            float height,
            int mouseX,
            int mouseY,
            String actionLabel) {
        Integer stock;
        boolean available;
        SizedFont labelFont;
        SizedFont valueFont;
        SizedFont buttonFont;
        float textX;
        Integer price;
        boolean hasPrice;
        float textY;
        int discountedPrice;
        label98:
        {
            label97:
            {
                boolean cardHovered = this.isInside(mouseX, mouseY, x, y, width, height);
                stock = product.custom ? 1 : this.stockByProductId.get(product.productId);
                available = product.custom || stock != null && stock > 0;
                this.drawGlass(renderer, x, y, width, height, 8.0F, cardHovered);

                float imageAreaHeight = height * 0.42F;
                float skinSize = Math.min(imageAreaHeight - 10.0F, width - 24.0F);
                float skinX = x + (width - skinSize) / 2.0F;
                float skinY = y + 8.0F;
                this.drawProductSkin(renderer, product, skinX, skinY, skinSize);
                SizedFont nameFont = FontManager.getFont("Product Sans Bold", 10.0F);

                SizedFont descriptionFont = FontManager.getFont("Product Sans Regular", 7.0F);

                labelFont = FontManager.getFont("Product Sans Regular", 7.0F);

                valueFont = FontManager.getFont("Product Sans Regular", 8.0F);

                buttonFont = FontManager.getFont("Product Sans Medium", 9.0F);
                textX = x + 10.0F;
                textY = y + imageAreaHeight + 4.0F;
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(product.name)
                                .setPosition(textX, textY)
                                .setFont(nameFont.getMsdfFont())
                                .setFontSize(nameFont.getSize())
                                .enableShadow()
                                .setColor(Color.WHITE.getRGB()));
                textY += nameFont.getLineHeight() + 3.0F;
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(product.description)
                                .setPosition(textX, textY)
                                .setFont(descriptionFont.getMsdfFont())
                                .setFontSize(descriptionFont.getSize())
                                .enableShadow()
                                .setColor(secondaryTextColor.getRGB()));
                textY += descriptionFont.getLineHeight() + 6.0F;
                price =
                        this.priceByProductId.get(
                                product.custom ? "C" : String.valueOf(product.productId));
                hasPrice = price != null;

                if ("Purchase".equals(actionLabel)) {
                    if (product.productId == 1) {
                        break label97;
                    }

                    if (product.productId == 9) {
                        break label97;
                    }
                }

                discountedPrice = 0;
                break label98;
            }

            discountedPrice = 2;
        }

        byte apiDiscount = (byte) discountedPrice;
        TextDrawRequest labelRequest = TextDrawRequest.obtain();

        renderer.drawText(
                labelRequest
                        .setText("Stock")
                        .setPosition(textX, textY)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(mutedTextColor.getRGB()));
        if (hasPrice) {
            labelRequest = TextDrawRequest.obtain();

            renderer.drawText(
                    labelRequest
                            .setText("Price")
                            .setPosition(textX + width / 2.0F - 12.0F, textY)
                            .setFont(labelFont.getMsdfFont())
                            .setFontSize(labelFont.getSize())
                            .enableShadow()
                            .setColor(mutedTextColor.getRGB()));
        }

        float valueY = textY + (labelFont.getLineHeight() + 1.0F);
        String stockText;
        int stockColor;
        if (product.custom) {
            stockText = "Available";
            stockColor = Color.WHITE.getRGB();
        } else if (stock == null) {
            stockText = "Loading...";
            stockColor = mutedTextColor.getRGB();
        } else if (stock == 0) {
            stockText = "Out of stock";
            stockColor = errorColor.getRGB();
        } else {
            stockText = stock + " in stock";
            stockColor = Color.WHITE.getRGB();
        }

        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(stockText)
                        .setPosition(textX, valueY)
                        .setFont(valueFont.getMsdfFont())
                        .setFontSize(valueFont.getSize())
                        .enableShadow()
                        .setColor(stockColor));
        if (hasPrice) {
            String priceTextChoice;
            if (apiDiscount > 0) {
                discountedPrice = Math.max(0, price - apiDiscount);

                priceTextChoice = discountedPrice + " credits (" + apiDiscount + " off via API)";
            } else {
                priceTextChoice = price + " credits";
            }

            String priceText = priceTextChoice;
            renderer.drawText(
                    TextDrawRequest.obtain()
                            .setText(priceText)
                            .setPosition(
                                    textX
                                            + width / 2.0F
                                            - valueFont.getStringWidth(priceText) / 2.0F,
                                    valueY)
                            .setFont(valueFont.getMsdfFont())
                            .setFontSize(valueFont.getSize())
                            .enableShadow()
                            .setColor(Color.WHITE.getRGB()));
        }

        float buttonX = x + 8.0F;
        float buttonY = y + height - 20.0F - 8.0F;
        float buttonWidth = width - 16.0F;
        boolean buttonHovered = this.isInside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 20.0F);
        this.drawGlass(
                renderer, buttonX, buttonY, buttonWidth, 20.0F, 6.0F, buttonHovered && available);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(actionLabel)
                        .setPosition(buttonX, buttonY)
                        .centerInBox(buttonWidth, 20.0F)
                        .setFont(buttonFont.getMsdfFont())
                        .setFontSize(buttonFont.getSize())
                        .enableShadow()
                        .setColor(available ? Color.WHITE.getRGB() : mutedTextColor.getRGB()));
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.button() != 0) {
            return false;
        }

        double mouseX = event.x();
        double mouseY = event.y();
        if (this.showingApiKeyPrompt) {
            float dialogX = (this.width - 320.0F) / 2.0F;
            float dialogY = (this.height - 150.0F) / 2.0F;
            float inputX = dialogX + 20.0F;
            float inputY = dialogY + 60.0F;
            float saveButtonX = inputX;
            float cancelButtonX = inputX + 135.0F + 10.0F;
            float buttonsY = inputY + 26.0F + 12.0F;
            if (this.isInside(mouseX, mouseY, saveButtonX, buttonsY, 135.0F, 26.0F)) {
                if (!this.apiKeyInput.isEmpty()) {
                    this.apiKey = this.apiKeyInput;
                    this.saveApiKey(this.apiKey);
                    this.showingApiKeyPrompt = false;
                    this.refreshStock();
                    this.refreshBalance();
                    this.refreshHistory();
                }

                return true;
            } else {
                if (!this.isInside(mouseX, mouseY, cancelButtonX, buttonsY, 135.0F, 26.0F)) {
                    return true;
                }

                if (this.apiKey != null && !this.apiKey.isEmpty()) {
                    this.showingApiKeyPrompt = false;
                } else {
                    this.onClose();
                }

                return true;
            }
        } else {
            if (this.customPurchaseOpen) {
                return this.handleDialogClick(mouseX, mouseY);
            }

            float windowX = this.getWindowX();
            float windowY = this.getWindowY();

            float windowWidth = this.getWindowWidth();
            float windowHeight = this.getWindowHeight();
            SizedFont infoFont = FontManager.getFont("Product Sans Regular", 7.0F);
            float infoTextWidth = infoFont.getStringWidth(this.getAccountInfoText());

            float infoTotalWidth = infoTextWidth + 8.0F + 62.0F;
            float infoStartX = windowX + windowWidth - infoTotalWidth - 16.0F;
            SizedFont titleFont = FontManager.getFont("Product Sans Bold", 14.0F);
            float logoutX = infoStartX + infoTextWidth + 8.0F;
            float logoutY = windowY + 12.0F + (titleFont.getLineHeight() - 20.0F) / 2.0F;
            if (this.isInside(mouseX, mouseY, logoutX, logoutY, 62.0F, 20.0F)) {
                this.logout();
                return true;
            }

            if (this.handleTabClick(mouseX, mouseY, windowX, windowY, windowWidth)) {
                return true;
            }

            if (this.currentTab == AccountShopTab.SHOP) {
                float cardHeight = this.getProductCardHeight(windowHeight);
                float hypixelRowY = windowY + 70.0F;
                float otherRowY = hypixelRowY + cardHeight + 18.0F + 22.0F;
                if (this.handleProductRowClick(
                        mouseX,
                        mouseY,
                        hypixelProducts,
                        windowX,
                        hypixelRowY,
                        windowWidth,
                        cardHeight)) {
                    return true;
                }

                if (this.handleProductRowClick(
                        mouseX,
                        mouseY,
                        otherProducts,
                        windowX,
                        otherRowY,
                        windowWidth,
                        cardHeight)) {
                    return true;
                }
            } else if (this.currentTab == AccountShopTab.GENERATOR) {
                int productCount = generatorProducts.length;
                float generatorCardHeight =
                        Math.min(175.0F, Math.max(120.0F, windowHeight - 130.0F));
                float cardY = windowY + 90.0F;
                float contentWidth = windowWidth - 32.0F;

                float totalCardsWidth =
                        Math.min(contentWidth, 175 * productCount + 12.0F * (productCount - 1));
                float cardWidth = (totalCardsWidth - 12.0F * (productCount - 1)) / productCount;
                float cardsX = windowX + (windowWidth - totalCardsWidth) / 2.0F;

                for (int i = 0; i < productCount; i++) {
                    float cardX = cardsX + i * (cardWidth + 12.0F);
                    float buttonX = cardX + 8.0F;
                    float buttonY = cardY + generatorCardHeight - 28.0F;
                    float buttonWidth = cardWidth - 16.0F;
                    if (this.isInside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 20.0F)) {
                        NiceAltsProduct product = generatorProducts[i];
                        Integer stock = this.stockByProductId.get(product.productId);
                        if (stock != null && stock > 0) {
                            this.generate(product);
                            return true;
                        }

                        this.statusMessage = product.name + " is out of stock";
                        return true;
                    }
                }
            } else if (this.currentTab == AccountShopTab.PURCHASE_HISTORY
                    || this.currentTab == AccountShopTab.GENERATOR_HISTORY) {
                List entries =
                        this.sortedHistory(
                                this.currentTab == AccountShopTab.GENERATOR_HISTORY
                                        ? this.generatorHistory
                                        : this.purchaseHistory);
                float rowY = windowY + 58.0F;

                int rowCount = Math.min(8, entries.size());

                for (int i = 0; i < rowCount; i++) {
                    if (this.isInside(
                            mouseX, mouseY, windowX + 16.0F, rowY, windowWidth - 32.0F, 44.0F)) {
                        this.loginHistoryAccount((NiceAltsHistoryEntry) entries.get(i));
                        return true;
                    }

                    rowY += 50.0F;
                }
            }

            return false;
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!this.showingApiKeyPrompt) {
            if (this.customPurchaseOpen) {
                if (event.isEscape()) {
                    this.customPurchaseOpen = false;
                    this.customServerFocused = false;
                    TextInputUtil.clearSelectAll(this.serverInputKey);
                    return true;
                }

                if (this.customServerFocused) {
                    Object inputKey = this.serverInputKey;
                    String currentText = this.customServer;
                    Consumer<String> textSetter = newText -> this.customServer = newText;

                    if (TextInputUtil.handleControlShortcut(
                            inputKey,
                            event,
                            currentText,
                            textSetter,
                            128,
                            TextInputUtil::isPrintableCodePoint)) {
                        return true;
                    }
                }

                if (this.customServerFocused) {
                    int keyCode = event.key();

                    if (keyCode == 259) {
                        TextInputUtil.handleBackspace(
                                this.serverInputKey,
                                this.customServer,
                                newText -> this.customServer = newText);
                        return true;
                    }
                }

                return true;
            } else if (event.isEscape()) {
                if (this.currentTab != AccountShopTab.SHOP) {
                    this.currentTab = AccountShopTab.SHOP;
                    return true;
                } else {
                    this.onClose();
                    return true;
                }
            } else {
                return super.keyPressed(event);
            }
        } else {
            int keyCode = (int) this.apiKeyInputKey;
            String currentText = this.apiKeyInput;
            Consumer<String> textSetter =
                    newText -> this.apiKeyInput = AccountManager.extractToken(newText);

            if (TextInputUtil.handleControlShortcut(
                    keyCode,
                    event,
                    currentText,
                    textSetter,
                    4096,
                    TextInputUtil::isPrintableCodePoint)) {
                return true;
            }

            keyCode = event.key();
            if (keyCode == 259) {
                TextInputUtil.handleBackspace(
                        this.apiKeyInputKey,
                        this.apiKeyInput,
                        newText -> this.apiKeyInput = newText);
                return true;
            }

            keyCode = event.key();
            if (keyCode == 257 && !this.apiKeyInput.isEmpty()) {
                this.apiKey = this.apiKeyInput;
                this.saveApiKey(this.apiKey);
                this.showingApiKeyPrompt = false;
                this.refreshStock();
                this.refreshBalance();
                this.refreshHistory();
                return true;
            }

            if (!event.isEscape()) {
                return true;
            }

            if (this.apiKey != null && !this.apiKey.isEmpty()) {
                this.showingApiKeyPrompt = false;
            } else {
                this.onClose();
            }

            TextInputUtil.clearSelectAll(this.apiKeyInputKey);
            return true;
        }
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (this.showingApiKeyPrompt) {
            Object inputKey = this.apiKeyInputKey;
            String currentText = this.apiKeyInput;
            String typedText = event.codepointAsString();
            Consumer<String> textSetter = newText -> this.apiKeyInput = newText;

            TextInputUtil.insertText(
                    inputKey,
                    currentText,
                    typedText,
                    textSetter,
                    4096,
                    TextInputUtil::isPrintableCodePoint);
            return true;
        } else if (this.customPurchaseOpen && this.customServerFocused) {
            Object inputKey = this.serverInputKey;
            String currentText = this.customServer;
            String typedText = event.codepointAsString();
            Consumer<String> textSetter = newText -> this.customServer = newText;

            TextInputUtil.insertText(
                    inputKey,
                    currentText,
                    typedText,
                    textSetter,
                    128,
                    TextInputUtil::isPrintableCodePoint);
            return true;
        } else {
            return super.charTyped(event);
        }
    }

    private boolean handleProductRowClick(
            double mouseX,
            double mouseY,
            NiceAltsProduct[] products,
            float windowX,
            float rowY,
            float windowWidth,
            float cardHeight) {
        int productCount = products.length;
        float cardsX = windowX + 16.0F;
        float cardsWidth = windowWidth - 32.0F;
        float cardWidth = (cardsWidth - 8.0F * (productCount - 1)) / productCount;

        for (int i = 0; i < productCount; i++) {
            float cardX = cardsX + i * (cardWidth + 8.0F);
            float buttonX = cardX + 8.0F;
            float buttonY = rowY + cardHeight - 20.0F - 8.0F;
            float buttonWidth = cardWidth - 16.0F;
            if (this.isInside(mouseX, mouseY, buttonX, buttonY, buttonWidth, 20.0F)) {
                NiceAltsProduct product = products[i];
                if (product.custom) {
                    this.customPurchaseOpen = true;
                    this.customServerFocused = true;
                    return true;
                }

                Integer stock = this.stockByProductId.get(product.productId);
                if (stock != null && stock > 0) {
                    this.purchase(product);
                    return true;
                }

                this.statusMessage = product.name + " is out of stock";
                return true;
            }
        }

        return false;
    }

    private void purchase(NiceAltsProduct product) {
        if (this.apiKey != null && !this.apiKey.isEmpty()) {

            this.statusMessage = "Purchasing " + product.name + "...";
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            JsonObject jsonObject = new JsonObject();

                            jsonObject.addProperty("api_key", this.apiKey);

                            jsonObject.addProperty("product_id", String.valueOf(product.productId));
                            Builder requestBuilder = HttpRequest.newBuilder();

                            Builder uriBuilder = requestBuilder.uri(createApiUri("/api/purchase"));

                            HttpRequest request =
                                    uriBuilder
                                            .header("Content-Type", "application/json")
                                            .POST(BodyPublishers.ofString(jsonObject.toString()))
                                            .build();
                            HttpResponse response =
                                    httpClient.send(request, BodyHandlers.ofString());
                            JsonObject jsonObject2 =
                                    JsonParser.parseString((String) response.body())
                                            .getAsJsonObject();
                            int statusCode = response.statusCode();

                            if (statusCode != 200) {
                                this.statusMessage =
                                        this.getPurchaseErrorMessage(
                                                response.statusCode(), jsonObject2);
                                return;
                            }

                            String orderId = extractOrderId(jsonObject2);

                            List itemsChoice;
                            if (jsonObject2.has("items")) {

                                itemsChoice = toStringList(jsonObject2.getAsJsonArray("items"));
                            } else {
                                itemsChoice = List.of();
                            }

                            List items = itemsChoice;

                            if (product.productId == 9) {
                                this.downloadCookieOrder(orderId, items);
                                return;
                            }

                            if (items.isEmpty()) {

                                this.statusMessage = "Error: No items returned";
                                return;
                            }

                            Account account =
                                    this.createAccountFromToken(
                                            (String) items.getFirst(),
                                            this.isRefreshTokenProduct(product));
                            this.purchaseHistory.add(
                                    new NiceAltsHistoryEntry(
                                            orderId,
                                            String.valueOf(product.productId),
                                            currentTimestamp(),
                                            items,
                                            account.getUsername(),
                                            ""));
                            String username = account.getUsername();

                            this.statusMessage = "Successfully logged in as " + username;
                            this.refreshStock();
                            this.refreshBalance();
                            this.refreshHistory();
                        } catch (Exception e) {
                            String errorMessage = e.getMessage();

                            this.statusMessage = "Error: " + errorMessage;
                        }
                    });
        } else {
            this.showingApiKeyPrompt = true;
        }
    }

    private void customPurchase() {
        if (this.apiKey != null && !this.apiKey.isEmpty()) {

            this.statusMessage = "Purchasing custom unban...";
            String server = this.customServer.trim();
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            JsonObject jsonObject = new JsonObject();

                            jsonObject.addProperty("api_key", this.apiKey);

                            jsonObject.addProperty("server", server);

                            jsonObject.addProperty("protocol", this.customProtocol);

                            jsonObject.addProperty("ban_message", this.customBanMessageMode);

                            jsonObject.addProperty("skin", this.customSkinMode);

                            jsonObject.addProperty("quantity", this.customQuantity);
                            Builder requestBuilder = HttpRequest.newBuilder();

                            Builder uriBuilder =
                                    requestBuilder.uri(createApiUri("/api/custompurchase"));

                            HttpRequest request =
                                    uriBuilder
                                            .header("Content-Type", "application/json")
                                            .POST(BodyPublishers.ofString(jsonObject.toString()))
                                            .build();
                            HttpResponse response =
                                    httpClient.send(request, BodyHandlers.ofString());
                            JsonObject jsonObject2 =
                                    JsonParser.parseString((String) response.body())
                                            .getAsJsonObject();
                            int statusCode = response.statusCode();

                            if (statusCode != 200) {
                                this.statusMessage =
                                        this.getPurchaseErrorMessage(
                                                response.statusCode(), jsonObject2);
                                return;
                            }

                            List<String> tokensChoice;
                            if (jsonObject2.has("tokens")) {

                                tokensChoice = toStringList(jsonObject2.getAsJsonArray("tokens"));
                            } else {

                                if (jsonObject2.has("items")) {

                                    tokensChoice =
                                            toStringList(jsonObject2.getAsJsonArray("items"));
                                } else {
                                    tokensChoice = List.of();
                                }
                            }

                            List<String> tokens = tokensChoice;
                            if (tokens.isEmpty()) {

                                this.statusMessage = "Error: No tokens returned";
                                return;
                            }

                            ArrayList usernames = new ArrayList();

                            for (String token : tokens) {
                                usernames.add(
                                        this.createAccountFromToken(token, false).getUsername());
                            }

                            List history = this.purchaseHistory;
                            String orderId = extractOrderId(jsonObject2);

                            String timestamp = currentTimestamp();

                            history.add(
                                    new NiceAltsHistoryEntry(
                                            orderId,
                                            "custom",
                                            timestamp,
                                            tokens,
                                            String.join(", ", usernames),
                                            ""));
                            int count = tokens.size();
                            String plural = tokens.size() == 1 ? "" : "s";

                            this.statusMessage =
                                    "Successfully purchased " + count + " custom alt" + plural;
                            this.refreshHistory();
                        } catch (Exception e) {
                            String errorMessage = e.getMessage();

                            this.statusMessage = "Error: " + errorMessage;
                        }
                    });
        } else {
            this.showingApiKeyPrompt = true;
        }
    }

    private void downloadCookieOrder(String orderId, List<String> items)
            throws java.io.IOException, InterruptedException {
        String resolvedOrderId = orderId;
        if (resolvedOrderId == null || resolvedOrderId.isBlank()) {
            this.fetchHistory();
            NiceAltsHistoryEntry latestCookieEntry =
                    this.purchaseHistory.stream()
                            .filter(entry -> "9".equals(entry.productId))
                            .reduce((earlier, later) -> later)
                            .orElse(null);
            resolvedOrderId = latestCookieEntry == null ? null : latestCookieEntry.orderId;
        }

        if (resolvedOrderId != null && !resolvedOrderId.isBlank()) {
            String encodedKey = URLEncoder.encode(this.apiKey, StandardCharsets.UTF_8);
            String encodedOrderId = URLEncoder.encode(resolvedOrderId, StandardCharsets.UTF_8);

            String query = "?api_key=" + encodedKey + "&order_id=" + encodedOrderId;
            Builder requestBuilder = HttpRequest.newBuilder();

            HttpRequest request =
                    requestBuilder.uri(createApiUri("/api/download-file" + query)).GET().build();
            HttpResponse response = httpClient.send(request, BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                int failedStatusCode = response.statusCode();

                this.statusMessage =
                        "Error: Cookie download failed (HTTP " + failedStatusCode + ")";
            } else {

                File file2 =
                        new File(
                                mc.gameDirectory,
                                "astralis/nicealts/cookies-" + resolvedOrderId + ".zip");
                Files.createDirectories(file2.toPath().getParent());
                Files.write(file2.toPath(), (byte[]) response.body());
                List<File> cookieFiles =
                        CookieFileImporter.importCookieFiles(List.of(file2), mc.gameDirectory);
                int importedCount = 0;
                ArrayList usernames = new ArrayList();

                for (File file3 : cookieFiles) {
                    try {
                        Account account = this.importAccountFile(file3);
                        Astralis.getInstance()
                                .getAccountManager()
                                .setLastAccountId(account.getId());
                        importedCount++;
                        usernames.add(account.getUsername());
                    } catch (Exception ignored) {
                    }
                }

                Astralis.getInstance().getAccountManager().saveAccounts();
                List history = this.purchaseHistory;
                String timestamp = currentTimestamp();

                history.add(
                        new NiceAltsHistoryEntry(
                                resolvedOrderId,
                                "9",
                                timestamp,
                                items,
                                String.join(", ", usernames),
                                ""));
                String resultMessage;
                if (importedCount == 0) {
                    resultMessage = "Cookie download contained no valid accounts";
                } else {
                    String plural = importedCount == 1 ? "" : "s";

                    resultMessage = "Imported " + importedCount + " cookie account" + plural;
                }

                this.statusMessage = resultMessage;
                this.refreshHistory();
            }
        } else {

            this.statusMessage = "Cookie order created, but its order ID was not returned";
            this.refreshHistory();
        }
    }

    private Account importAccountFile(File file2) throws java.io.IOException {

        try {
            String content = Files.readString(file2.toPath());
            if (!AccountManager.parseTokens(content).isEmpty()) {
                return this.createAccountFromToken(content, false);
            }
        } catch (MalformedInputException ignored) {
        }

        Account account = new Account(file2.getAbsolutePath(), file2.getName());
        Astralis.getInstance().getAccountManager().addAccount(account);

        try {
            Astralis.getInstance().getAccountManager().login(account);
            Astralis.getInstance().getAccountManager().saveAccounts();
            return account;
        } catch (Exception e) {
            Astralis.getInstance().getAccountManager().removeAccount(account);
            throw e;
        }
    }

    private String getPurchaseErrorMessage(int statusCode, JsonObject jsonObject) {
        String errorTextChoice;
        if (jsonObject.has("error")) {
            errorTextChoice = jsonObject.get("error").getAsString();
        } else {
            errorTextChoice = "HTTP " + statusCode;
        }

        String errorText = errorTextChoice;
        String message;
        switch (statusCode) {
            case 400:
                message = "Error: Invalid purchase parameters";
                break;
            case 401:
                message = "Error: Invalid API key";
                break;
            case 402:
                message = "Error: Insufficient balance";
                break;
            case 403:
                message = "Error: Product unavailable or account restricted";
                break;
            case 404:
                message = "Error: Out of stock";
                break;
            case 429:
                message = "Error: Rate limited or purchase already in progress";
                break;
            default:
                message = "Error: " + errorText;
        }

        return message;
    }

    private void generate(NiceAltsProduct product) {
        if (this.apiKey != null && !this.apiKey.isEmpty()) {

            this.statusMessage = "Generating " + product.name + "...";
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            JsonObject jsonObject = new JsonObject();

                            jsonObject.addProperty("api_key", this.apiKey);

                            jsonObject.addProperty("category", product.generatorCategory);
                            Builder requestBuilder = HttpRequest.newBuilder();

                            Builder uriBuilder = requestBuilder.uri(createApiUri("/api/generate"));

                            HttpRequest request =
                                    uriBuilder
                                            .header("Content-Type", "application/json")
                                            .POST(BodyPublishers.ofString(jsonObject.toString()))
                                            .build();
                            HttpResponse response =
                                    httpClient.send(request, BodyHandlers.ofString());
                            JsonObject jsonObject2 =
                                    JsonParser.parseString((String) response.body())
                                            .getAsJsonObject();
                            int statusCode = response.statusCode();

                            if (statusCode != 200) {

                                String errorText;
                                if (jsonObject2.has("error")) {

                                    errorText = jsonObject2.get("error").getAsString();
                                } else {
                                    int errorStatus = response.statusCode();

                                    errorText = "HTTP " + errorStatus;
                                }

                                this.statusMessage = "Error: " + errorText;
                                return;
                            }

                            String token = jsonObject2.get("token").getAsString();
                            Account account = this.createAccountFromToken(token, false);
                            this.generatorHistory.add(
                                    new NiceAltsHistoryEntry(
                                            null,
                                            null,
                                            currentTimestamp(),
                                            List.of(token),
                                            account.getUsername(),
                                            ""));
                            String username = account.getUsername();

                            this.statusMessage = "Successfully logged in as " + username;
                            this.refreshStock();
                            this.refreshBalance();
                        } catch (Exception e) {
                            String errorMessage = e.getMessage();

                            this.statusMessage = "Error: " + errorMessage;
                        }
                    });
        } else {
            this.showingApiKeyPrompt = true;
        }
    }

    private boolean isRefreshTokenProduct(NiceAltsProduct product) {

        return Integer.valueOf(12)
                .equals(this.priceByProductId.get(String.valueOf(product.productId)));
    }

    private Account createAccountFromToken(String token, boolean refreshOnly)
            throws java.io.IOException {
        ParsedTokens parsedTokens = AccountManager.parseTokens(token);
        if (parsedTokens.isEmpty()) {

            throw new IllegalArgumentException("No token returned");
        }

        Account account;
        if (refreshOnly) {
            if (parsedTokens.getRefreshToken() == null
                    || parsedTokens.getRefreshToken().isBlank()) {

                throw new IllegalArgumentException("NiceAlts did not return a refresh token");
            }

            String refreshToken = parsedTokens.getRefreshToken();

            account = new Account(null, refreshToken, "Refresh Alt", "Refresh Alt");
            account.setType(AccountType.REFRESH);
        } else {
            String accessToken = parsedTokens.getAccessToken();
            String refreshToken = parsedTokens.getRefreshToken();

            account = new Account(accessToken, refreshToken, "Token Alt", "Token Alt");
        }

        Astralis.getInstance().getAccountManager().addAccount(account);

        try {
            Astralis.getInstance().getAccountManager().login(account);
        } catch (Exception e) {
            if (Astralis.getInstance().getAccountManager().getAccounts().contains(account)) {
                Astralis.getInstance().getAccountManager().removeAccount(account);
            }

            throw e;
        }

        Astralis.getInstance().getAccountManager().setLastAccountId(account.getId());
        Astralis.getInstance().getAccountManager().saveAccounts();
        return account;
    }

    private void drawProductSkin(
            Render2DContext renderer, NiceAltsProduct product, float x, float y, float size) {
        String skinId =
                productSkinIds.computeIfAbsent(product.productId, NiceAltsScreen::pickRandomSkinId);
        Identifier identifier = skinId == null ? null : skinTextures.get(skinId);

        try {
            if (identifier != null) {
                AbstractTexture abstractTexture = mc.getTextureManager().getTexture(identifier);
                if (abstractTexture != null) {
                    GpuTexture gpuTexture = abstractTexture.getTexture();
                    renderer.drawTexture(
                            TexturedRectShape.obtain()
                                    .setBounds(x, y, size, size)
                                    .setTexture(gpuTexture)
                                    .setUv(0.0F, 0.0F, 1.0F, 1.0F)
                                    .setRadius(5.0F)
                                    .setColor(Color.WHITE));
                    return;
                }
            }

            GpuTexture gpuTexture2 =
                    mc.getTextureManager()
                            .getTexture(DefaultPlayerSkin.getDefaultTexture())
                            .getTexture();
            renderer.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds(x, y, size, size)
                            .setTexture(gpuTexture2)
                            .setUv(0.125F, 0.125F, 0.25F, 0.25F)
                            .setRadius(5.0F)
                            .useNearestFilter()
                            .setColor(Color.WHITE));
        } catch (Exception ignored) {
        }
    }

    private static String pickRandomSkinId(int productId) {
        synchronized (trendingSkinIds) {
            if (trendingSkinIds.isEmpty()) {
                return null;
            }

            String skinId =
                    trendingSkinIds.get(
                            ThreadLocalRandom.current().nextInt(trendingSkinIds.size()));
            loadNameMcTexture(skinId);
            return skinId;
        }
    }

    private static void ensureNameMcSkinsLoaded() {
        if (!skinListRequested) {
            synchronized (trendingSkinIds) {
                if (skinListRequested) {
                    return;
                }

                skinListRequested = true;
            }

            CompletableFuture.runAsync(
                    () -> {
                        try {
                            HttpClient client = HttpClient.newHttpClient();
                            Builder requestBuilder = HttpRequest.newBuilder();

                            Builder uriBuilder =
                                    requestBuilder.uri(
                                            new URI("https://namemc.com/minecraft-skins/trending"));

                            HttpRequest request =
                                    uriBuilder
                                            .header(
                                                    "User-Agent",
                                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                            .GET()
                                            .build();
                            HttpResponse response = client.send(request, BodyHandlers.ofString());
                            int statusCode = response.statusCode();

                            if (statusCode != 200) {
                                return;
                            }

                            Matcher matcher = skinIdPattern.matcher((CharSequence) response.body());
                            ArrayList skinIds = new ArrayList();

                            while (matcher.find()) {
                                String skinId = matcher.group(1);
                                if (!skinIds.contains(skinId)) {
                                    skinIds.add(skinId);
                                }
                            }

                            synchronized (trendingSkinIds) {
                                trendingSkinIds.clear();
                                trendingSkinIds.addAll(skinIds);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private static void loadNameMcTexture(String skinId) {
        if (!skinTextures.containsKey(skinId)) {
            if (loadingSkinIds.add(skinId)) {
                CompletableFuture.runAsync(
                        () -> {
                            try {
                                HttpClient client = HttpClient.newHttpClient();
                                Builder requestBuilder = HttpRequest.newBuilder();

                                Builder uriBuilder =
                                        requestBuilder.uri(
                                                new URI(
                                                        "https://s.namemc.com/3d/skin/body.png?id="
                                                                + skinId
                                                                + "&width=160&height=160"));

                                HttpRequest request =
                                        uriBuilder
                                                .header(
                                                        "User-Agent",
                                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                                .GET()
                                                .build();
                                HttpResponse response =
                                        client.send(request, BodyHandlers.ofByteArray());
                                int statusCode = response.statusCode();

                                if (statusCode != 200) {
                                    loadingSkinIds.remove(skinId);
                                    return;
                                }

                                NativeImage nativeImage =
                                        NativeImage.read(
                                                new ByteArrayInputStream((byte[]) response.body()));
                                Minecraft.getInstance()
                                        .execute(
                                                () -> {
                                                    try {
                                                        DynamicTexture dynamicTexture =
                                                                new DynamicTexture(
                                                                        () -> "namemc-" + skinId,
                                                                        nativeImage);

                                                        Identifier identifier =
                                                                Identifier.fromNamespaceAndPath(
                                                                        "astralis",
                                                                        "namemc/" + skinId);
                                                        Minecraft.getInstance()
                                                                .getTextureManager()
                                                                .register(
                                                                        identifier, dynamicTexture);
                                                        skinTextures.put(skinId, identifier);
                                                    } catch (Exception ignored) {
                                                    } finally {
                                                        loadingSkinIds.remove(skinId);
                                                    }
                                                });
                            } catch (Exception ignored) {
                                loadingSkinIds.remove(skinId);
                            }
                        });
            }
        }
    }

    private void refreshStock() {
        long currentMillis = System.currentTimeMillis();
        if (currentMillis - this.lastStockRefreshMillis >= 1000L) {
            this.lastStockRefreshMillis = currentMillis;
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            Builder requestBuilder = HttpRequest.newBuilder();

                            HttpRequest request =
                                    requestBuilder.uri(createApiUri("/public/stock")).GET().build();
                            HttpResponse response =
                                    httpClient.send(request, BodyHandlers.ofString());
                            int statusCode = response.statusCode();

                            if (statusCode != 200) {
                                return;
                            }

                            JsonObject jsonObject =
                                    JsonParser.parseString((String) response.body())
                                            .getAsJsonObject();

                            if (jsonObject.has("stock")) {
                                HashMap parsedStock = new HashMap();

                                for (String productIdText :
                                        jsonObject.getAsJsonObject("stock").keySet()) {
                                    try {
                                        Integer productId = Integer.parseInt(productIdText);

                                        parsedStock.put(
                                                productId,
                                                jsonObject
                                                        .getAsJsonObject("stock")
                                                        .get(productIdText)
                                                        .getAsInt());
                                    } catch (NumberFormatException ignored) {
                                    }
                                }

                                this.stockByProductId.clear();
                                this.stockByProductId.putAll(parsedStock);
                            }

                            if (jsonObject.has("prices")) {
                                HashMap parsedPrices = new HashMap();

                                for (String priceKey :
                                        jsonObject.getAsJsonObject("prices").keySet()) {
                                    try {

                                        parsedPrices.put(
                                                priceKey,
                                                jsonObject
                                                        .getAsJsonObject("prices")
                                                        .get(priceKey)
                                                        .getAsInt());
                                    } catch (NumberFormatException ignored) {
                                    }
                                }

                                this.priceByProductId.clear();
                                this.priceByProductId.putAll(parsedPrices);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private void refreshBalance() {
        if (this.apiKey != null && !this.apiKey.isBlank()) {
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            JsonObject jsonObject = new JsonObject();

                            jsonObject.addProperty("api_key", this.apiKey);
                            Builder requestBuilder = HttpRequest.newBuilder();

                            Builder uriBuilder = requestBuilder.uri(createApiUri("/api/balance"));

                            HttpRequest request =
                                    uriBuilder
                                            .header("Content-Type", "application/json")
                                            .POST(BodyPublishers.ofString(jsonObject.toString()))
                                            .build();
                            HttpResponse response =
                                    httpClient.send(request, BodyHandlers.ofString());
                            int statusCode = response.statusCode();

                            if (statusCode != 200) {
                                return;
                            }

                            JsonObject jsonObject2 =
                                    JsonParser.parseString((String) response.body())
                                            .getAsJsonObject();

                            if (jsonObject2.has("balance")) {

                                this.creditBalance = jsonObject2.get("balance").getAsInt();
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private void refreshHistory() {
        if (this.apiKey != null && !this.apiKey.isBlank()) {
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            this.fetchHistory();
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    private void fetchHistory() throws java.io.IOException, InterruptedException {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("api_key", this.apiKey);
        Builder requestBuilder = HttpRequest.newBuilder();

        Builder uriBuilder = requestBuilder.uri(createApiUri("/api/history"));

        HttpRequest request =
                uriBuilder
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(jsonObject.toString()))
                        .build();
        HttpResponse response = httpClient.send(request, BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode == 200) {
            JsonObject jsonObject2 =
                    JsonParser.parseString((String) response.body()).getAsJsonObject();

            if (jsonObject2.has("history")) {
                ArrayList entries = new ArrayList();

                for (JsonElement jsonElement : jsonObject2.getAsJsonArray("history")) {
                    JsonObject jsonObject3 = jsonElement.getAsJsonObject();

                    String purchaseId = getJsonString(jsonObject3, "purchase_id");

                    String productId = getJsonString(jsonObject3, "product_id");

                    String timestamp = getJsonString(jsonObject3, "timestamp");

                    List items = getStringList(jsonObject3, "items");

                    String accountNames =
                            String.join(", ", getStringList(jsonObject3, "mc_usernames"));

                    entries.add(
                            new NiceAltsHistoryEntry(
                                    purchaseId,
                                    productId,
                                    timestamp,
                                    items,
                                    accountNames,
                                    getJsonString(jsonObject3, "refund_status")));
                }

                this.purchaseHistory.clear();
                this.purchaseHistory.addAll(entries);
                this.historyLoaded = true;
            }
        }
    }

    private static List<String> toStringList(JsonArray jsonArray) {
        ArrayList strings = new ArrayList();

        for (JsonElement jsonElement : jsonArray) {
            strings.add(jsonElement.getAsString());
        }

        return strings;
    }

    private static List<String> getStringList(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && jsonObject.get(key).isJsonArray()
                ? toStringList(jsonObject.getAsJsonArray(key))
                : List.of();
    }

    private static String getJsonString(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull()
                ? jsonObject.get(key).getAsString()
                : "";
    }

    private static String extractOrderId(JsonObject jsonObject) {

        for (String key : List.of("order_id", "purchase_id", "id")) {
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                return jsonObject.get(key).getAsString();
            }
        }

        return "";
    }

    public static String currentTimestamp() {
        return Instant.now().toString();
    }

    private List<NiceAltsHistoryEntry> sortedHistory(List<NiceAltsHistoryEntry> history) {
        ArrayList sorted = new ArrayList(history);
        sorted.sort(
                Comparator.<NiceAltsHistoryEntry, String>comparing(entry -> entry.timestamp)
                        .reversed());
        return sorted;
    }

    private String getHistoryAccountName(NiceAltsHistoryEntry entry) {

        if (entry.accountNames != null
                && !entry.accountNames.isBlank()
                && !entry.accountNames.startsWith("[")) {

            if (!entry.accountNames.contains("mctoken")) {
                return entry.accountNames;
            }
        }

        for (String item : entry.items) {
            Matcher matcher = tokenAccountNamePattern.matcher(item);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return "Unknown account";
    }

    private void loginHistoryAccount(NiceAltsHistoryEntry entry) {
        String token =
                entry.items.stream()
                        .filter(
                                item -> {
                                    return !AccountManager.parseTokens(item).isEmpty();
                                })
                        .findFirst()
                        .orElse(null);
        if (token == null) {

            this.statusMessage = "This history entry has no reusable token";
        } else {
            String accountName = this.getHistoryAccountName(entry);

            this.statusMessage = "Logging in as " + accountName + "...";
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            Account account = this.createAccountFromToken(token, false);
                            String username = account.getUsername();

                            this.statusMessage = "Successfully logged in as " + username;
                        } catch (Exception e) {
                            String errorMessage = e.getMessage();

                            this.statusMessage = "Login failed: " + errorMessage;
                        }
                    });
        }
    }

    private static String getProductName(String productId) {
        String key = productId;
        byte matchIndex = -1;
        switch (key.hashCode()) {
            case -1349088399:
                if (key.equals("custom")) {
                    matchIndex = 6;
                }
                break;
            case 49:
                if (key.equals("1")) {
                    matchIndex = 0;
                }
                break;
            case 50:
                if (key.equals("2")) {
                    matchIndex = 1;
                }
                break;
            case 51:
                if (key.equals("3")) {
                    matchIndex = 2;
                }
                break;
            case 53:
                if (key.equals("5")) {
                    matchIndex = 3;
                }
                break;
            case 54:
                if (key.equals("6")) {
                    matchIndex = 4;
                }
                break;
            case 57:
                if (key.equals("9")) {
                    matchIndex = 5;
                }
        }

        String name;
        switch (matchIndex) {
            case 0:
                name = "Unban (1-7)";
                break;
            case 1:
                name = "Unban (8+)";
                break;
            case 2:
                name = "Unban (Ranked)";
                break;
            case 3:
                name = "Donut";
                break;
            case 4:
                name = "Ban";
                break;
            case 5:
                name = "Unbanned Cookies";
                break;
            case 6:
                name = "Unban (Custom)";
                break;
            default:
                name = "Product " + productId;
        }

        return name;
    }

    private String loadStoredApiKey() {
        if (!this.apiKeyFile.exists()) {
            return null;
        }

        try {
            byte[] encrypted = Files.readAllBytes(this.apiKeyFile.toPath());
            DecryptResult decrypted = StorageCipher.decrypt(encrypted, apiKeyFileSecret);
            String storedKey = new String(decrypted.getData(), StandardCharsets.UTF_8);
            if (decrypted.isResaveRequired()) {
                this.saveApiKey(storedKey);
            }

            return storedKey;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void saveApiKey(String key) {
        try {
            byte[] encrypted =
                    StorageCipher.encrypt(key.getBytes(StandardCharsets.UTF_8), apiKeyFileSecret);
            this.apiKeyFile.getParentFile().mkdirs();
            Files.write(this.apiKeyFile.toPath(), encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        this.apiKey = null;
        this.apiKeyInput = "";
        this.stockByProductId.clear();
        this.priceByProductId.clear();
        this.purchaseHistory.clear();
        this.creditBalance = -1;

        this.statusMessage = "Logged out";

        try {
            if (this.apiKeyFile != null && this.apiKeyFile.exists()) {
                Files.delete(this.apiKeyFile.toPath());
            }
        } catch (Exception ignored) {
        }

        this.showingApiKeyPrompt = true;
    }

    private String maskApiKey(String key) {
        if (key != null && !key.isEmpty()) {
            int length = key.length();

            if (length <= 8) {
                return "•".repeat(key.length());
            }

            String prefix = key.substring(0, 4);

            int keyLength = key.length();

            return prefix
                    + "•".repeat(Math.min(8, keyLength - 8))
                    + key.substring(key.length() - 4);
        } else {
            return "";
        }
    }

    private Color getStatusColor() {

        if (!this.statusMessage.startsWith("Successfully")) {

            if (!this.statusMessage.startsWith("Purchased")) {
                String lowerMessage = this.statusMessage.toLowerCase();

                if (!lowerMessage.contains("error")) {
                    String lowerStatus = this.statusMessage.toLowerCase();

                    if (!lowerStatus.contains("failed")) {

                        if (this.statusMessage.contains("...")) {

                            return new Color(215, 175, 55);
                        }

                        return secondaryTextColor;
                    }
                }

                return errorColor;
            }
        }

        return new Color(75, 195, 100);
    }

    private boolean isInside(
            double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
