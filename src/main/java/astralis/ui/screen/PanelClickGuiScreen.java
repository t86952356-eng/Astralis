package astralis.ui.screen;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.impl.ClickGui;
import astralis.ui.animation.Animation;
import astralis.ui.component.BlockListSettingWidget;
import astralis.ui.component.ClickGuiPalette;
import astralis.ui.component.PanelCategoryTab;
import astralis.ui.component.PanelModuleCard;
import astralis.ui.component.SapphireScreen;
import astralis.ui.component.SettingWidget;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.NonNull;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.ArrayList;

/** ClickGUI screen in "Panel" style: a single window with category tabs and module cards. */
public class PanelClickGuiScreen extends Screen implements SapphireScreen, ClickGuiPalette {
    private static final float PANEL_WIDTH = 440.0F;
    private static final float PANEL_HEIGHT = 250.0F;
    private static final float SIDEBAR_WIDTH = 90.0F;
    private static final float HEADER_HEIGHT = 20.0F;
    private static final float MODULE_CARD_HEIGHT = 20.0F;
    private static final float SIDEBAR_DIVIDER_WIDTH = 1.0F;
    private static final float SETTINGS_PANEL_WIDTH = 200.0F;
    private static final float PROFILE_AREA_HEIGHT = 62.0F;
    private final ArrayList<PanelCategoryTab> categoryTabs;
    private final ArrayList<PanelModuleCard> moduleCards;
    private Category selectedCategory;
    private PanelModuleCard selectedModuleCard;
    private final Animation openAnimation;
    private final Animation closeAnimation;
    private final Animation settingsPanelAnimation;
    private float moduleScrollOffset;
    private float targetModuleScrollOffset;
    private final Animation moduleScrollAnimation;
    private float settingsScrollOffset;
    private float targetSettingsScrollOffset;
    private final Animation settingsScrollAnimation;
    private String hoveredDescription;
    private volatile int panelMouseX;
    private volatile int panelMouseY;
    private volatile int screenMouseX;
    private volatile int screenMouseY;
    private volatile float currentScale;
    private volatile boolean settingsMouseDown;
    private boolean closing;
    private float panelOffsetX;
    private float panelOffsetY;
    private boolean draggingPanel;
    private float dragStartMouseX;
    private float dragStartMouseY;
    private float dragStartOffsetX;
    private float dragStartOffsetY;
    private GpuTexture pfpTexture;
    private byte[] pfpImageBytes;

    public PanelClickGuiScreen() {
        super(Component.empty());
        this.categoryTabs = new ArrayList<>();
        this.moduleCards = new ArrayList<>();
        this.selectedModuleCard = null;
        this.openAnimation = new Animation(Easing.EASE_OUT_BACK, 400L);
        this.closeAnimation = new Animation(Easing.EASE_IN_BACK, 280L);
        this.settingsPanelAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250L);
        this.moduleScrollOffset = 0.0F;
        this.targetModuleScrollOffset = 0.0F;
        this.moduleScrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 300L);
        this.settingsScrollOffset = 0.0F;
        this.targetSettingsScrollOffset = 0.0F;
        this.settingsScrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 300L);
        this.hoveredDescription = "";
        this.currentScale = 1.0F;
        this.settingsMouseDown = false;
        this.closing = false;
        this.panelOffsetX = 0.0F;
        this.panelOffsetY = 0.0F;
        this.draggingPanel = false;
    }

    protected void init() {
        super.init();
        this.openAnimation.setValue(0.0);
        this.openAnimation.restart();
        this.closeAnimation.setValue(1.0);
        this.closing = false;
        if (this.categoryTabs.isEmpty()) {
            Category[] categories = Category.values();

            for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
                this.categoryTabs.add(
                        new PanelCategoryTab(
                                this,
                                categories[categoryIndex],
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                categoryIndex == 0));
            }

            this.selectedCategory = categories[0];
        }

        this.rebuildModuleCards();
    }

    public void onClose() {
        if (!this.closing) {
            this.closing = true;
            this.closeAnimation.setValue(1.0);
            this.closeAnimation.restart();
            this.openAnimation.setValue(1.0);
        }
    }

    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.screenMouseX = mouseX;
        this.screenMouseY = mouseY;
        if (this.closing) {
            this.closeAnimation.animateTo(0.0);
            if (this.closeAnimation.getValue() <= 0.01) {
                this.closing = false;
                super.onClose();
            }
        } else {
            this.openAnimation.animateTo(1.0);
        }

        float visibilityProgress =
                this.closing
                        ? (float) this.closeAnimation.getValue()
                        : (float) this.openAnimation.getValue();
        if (!this.settingsMouseDown) {
            this.currentScale = visibilityProgress * this.getConfiguredScale();
        }

        float[] panelPoint = this.toPanelCoordinates(mouseX, mouseY);
        this.panelMouseX = (int) panelPoint[0];
        this.panelMouseY = (int) panelPoint[1];
        this.moduleScrollAnimation.animateTo(this.targetModuleScrollOffset);
        this.moduleScrollOffset = (float) this.moduleScrollAnimation.getValue();
        this.settingsScrollAnimation.animateTo(this.targetSettingsScrollOffset);
        this.settingsScrollOffset = (float) this.settingsScrollAnimation.getValue();
        this.settingsPanelAnimation.animateTo(this.selectedModuleCard != null ? 1.0 : 0.0);
        this.tryLoadPfp();
    }

    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    public boolean isAllowedInPortal() {
        return true;
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && event.x() >= 12.0
                && event.x() < 82.0
                && event.y() >= this.height - 32.0
                && event.y() < this.height - 10.0) {
            Minecraft.getInstance().gui.setScreen(new ScriptEditorScreen(this));
            return true;
        }

        float[] panelPoint = this.toPanelCoordinates(event.x(), event.y());
        float localMouseX = panelPoint[0];
        float localMouseY = panelPoint[1];
        float panelX = this.getPanelX();
        float panelY = this.getPanelY();
        if (event.button() == 0) {
            float closeX = panelX + 440.0F - 18.0F;
            float closeY = panelY + 2.0F;
            if (RenderUtil.isInsideRect(localMouseX, localMouseY, closeX, closeY, 16.0F, 16.0F)) {
                this.onClose();
                return true;
            }
        }

        if (event.button() == 0
                && RenderUtil.isInsideRect(
                        localMouseX, localMouseY, panelX, panelY, 440.0F, 20.0F)) {
            this.draggingPanel = true;
            this.dragStartMouseX = (float) event.x();
            this.dragStartMouseY = (float) event.y();
            this.dragStartOffsetX = this.panelOffsetX;
            this.dragStartOffsetY = this.panelOffsetY;
        }

        float settingsPanelProgress = (float) this.settingsPanelAnimation.getValue();
        if (settingsPanelProgress > 0.5F && this.selectedModuleCard != null) {
            float settingsBaseX = panelX + 90.0F + 1.0F;
            float settingsX = settingsBaseX + 349.0F - 200.0F * settingsPanelProgress;
            float settingsY = panelY + 20.0F;
            if (RenderUtil.isInsideRect(
                    localMouseX,
                    localMouseY,
                    settingsX,
                    settingsY,
                    200.0F * settingsPanelProgress,
                    230.0F)) {
                if (RenderUtil.isInsideRect(
                        localMouseX,
                        localMouseY,
                        settingsX + 6.0F,
                        settingsY + 2.0F,
                        16.0F,
                        16.0F)) {
                    this.selectedModuleCard = null;
                    return true;
                }

                if (event.button() == 0) {
                    this.settingsMouseDown = true;
                }

                MouseButtonEvent mouseButtonEvent =
                        new MouseButtonEvent(localMouseX, localMouseY, event.buttonInfo());

                for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                    if (settingWidget.getSetting() == null
                            || !settingWidget.getSetting().isHidden()) {
                        settingWidget.click(mouseButtonEvent);
                    }
                }

                return true;
            }
        }

        MouseButtonEvent mouseButtonEvent2 =
                new MouseButtonEvent(localMouseX, localMouseY, event.buttonInfo());

        for (PanelCategoryTab categoryTab : this.categoryTabs) {
            categoryTab.click(mouseButtonEvent2);
        }

        float contentX = panelX + 90.0F + 1.0F;
        float contentY = panelY + 20.0F;

        for (PanelModuleCard moduleCard : this.moduleCards) {
            if (!RenderUtil.isInsideRect(
                    localMouseX, localMouseY, contentX, contentY, 349.0F, 230.0F)) {
                break;
            }

            if (RenderUtil.isInsideRect(
                    localMouseX,
                    localMouseY,
                    moduleCard.getX(),
                    moduleCard.getY(),
                    moduleCard.getWidth(),
                    moduleCard.getHeight())) {
                if (event.button() == 1 && !moduleCard.getSettingWidgets().isEmpty()) {
                    this.selectedModuleCard =
                            this.selectedModuleCard == moduleCard ? null : moduleCard;
                    this.settingsPanelAnimation.restart();
                    this.targetSettingsScrollOffset = 0.0F;
                    this.settingsScrollAnimation.setValue(0.0);
                    return true;
                }

                moduleCard.click(mouseButtonEvent2);
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        this.draggingPanel = false;
        this.settingsMouseDown = false;
        if (this.selectedModuleCard != null) {
            float[] panelPoint = this.toPanelCoordinates(event.x(), event.y());
            MouseButtonEvent mouseButtonEvent =
                    new MouseButtonEvent(panelPoint[0], panelPoint[1], event.buttonInfo());

            for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                if (settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden()) {
                    settingWidget.release(mouseButtonEvent);
                }
            }
        }

        return super.mouseReleased(event);
    }

    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingPanel) {
            this.panelOffsetX = this.dragStartOffsetX + ((float) event.x() - this.dragStartMouseX);
            this.panelOffsetY = this.dragStartOffsetY + ((float) event.y() - this.dragStartMouseY);
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float[] panelPoint = this.toPanelCoordinates(mouseX, mouseY);
        float localMouseX = panelPoint[0];
        float localMouseY = panelPoint[1];
        float panelX = this.getPanelX();
        float panelY = this.getPanelY();
        float contentX = panelX + 90.0F + 1.0F;
        float contentY = panelY + 20.0F;
        if (this.selectedModuleCard != null) {
            float settingsPanelProgress = (float) this.settingsPanelAnimation.getValue();
            float settingsX = contentX + 349.0F - 200.0F * settingsPanelProgress;
            float settingsY = panelY + 20.0F + 20.0F;
            if (RenderUtil.isInsideRect(
                    localMouseX,
                    localMouseY,
                    settingsX,
                    settingsY,
                    200.0F * settingsPanelProgress,
                    210.0F)) {
                for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                    if ((settingWidget.getSetting() == null
                                    || !settingWidget.getSetting().isHidden())
                            && settingWidget.mouseScrolled(
                                    localMouseX, localMouseY, scrollX, scrollY)) {
                        return true;
                    }
                }

                float settingsContentHeight = 4.0F;

                for (SettingWidget measuredWidget : this.selectedModuleCard.getSettingWidgets()) {
                    if (measuredWidget.getSetting() == null
                            || !measuredWidget.getSetting().isHidden()) {
                        settingsContentHeight += measuredWidget.getHeight();
                    }
                }

                float minSettingsScrollOffset = Math.min(0.0F, 210.0F - settingsContentHeight);
                this.targetSettingsScrollOffset =
                        Math.clamp(
                                this.targetSettingsScrollOffset + (float) (scrollY * 20.0),
                                minSettingsScrollOffset,
                                0.0F);
                return true;
            }
        }

        float settingsSlideProgress = (float) this.settingsPanelAnimation.getValue();
        float moduleAreaWidth = 349.0F - 200.0F * settingsSlideProgress;
        if (RenderUtil.isInsideRect(
                localMouseX, localMouseY, contentX, contentY, moduleAreaWidth, 230.0F)) {
            float moduleContentHeight = this.moduleCards.size() * 21.0F + 8.0F;
            float minModuleScrollOffset = Math.min(0.0F, 230.0F - moduleContentHeight);
            this.targetModuleScrollOffset =
                    Math.clamp(
                            this.targetModuleScrollOffset + (float) (scrollY * 20.0),
                            minModuleScrollOffset,
                            0.0F);
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderSapphire(Render2DContext renderer) {
        int mouseX = this.panelMouseX;
        int mouseY = this.panelMouseY;
        float panelX = this.getPanelX();
        float panelY = this.getPanelY();
        float panelCenterX = panelX + 220.0F;
        float panelCenterY = panelY + 125.0F;
        float scale = this.currentScale;
        renderer.pushPose();
        renderer.translate(panelCenterX, panelCenterY);
        renderer.scale(scale, scale, 1.0F);
        renderer.translate(-panelCenterX, -panelCenterY);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(panelX, panelY, 440.0F, 250.0F)
                        .setRadius(8.0F)
                        .setColor(new Color(22, 22, 22)));
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(panelX + 90.0F, panelY, 1.0F, 250.0F)
                        .setColor(new Color(30, 30, 30)));
        this.drawTitleBar(renderer, panelX, panelY, mouseX, mouseY);
        this.drawCategoryTabs(renderer, panelX, panelY, mouseX, mouseY);
        this.drawProfileArea(renderer, panelX, panelY);
        this.drawModuleCards(renderer, panelX, panelY, mouseX, mouseY);
        this.drawSettingsPanel(renderer, panelX, panelY, mouseX, mouseY);
        renderer.popPose();
        Astralis.getInstance()
                .getRenderEffects()
                .renderShadowShapes(
                        shadowRenderer -> {
                            shadowRenderer.pushPose();
                            shadowRenderer.translate(panelCenterX, panelCenterY);
                            shadowRenderer.scale(scale, scale, 1.0F);
                            shadowRenderer.translate(-panelCenterX, -panelCenterY);
                            shadowRenderer.drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(panelX, panelY, 440.0F, 250.0F)
                                            .setRadius(8.0F)
                                            .setColor(Color.BLACK));
                            shadowRenderer.popPose();
                        });
        boolean scriptsHovered =
                this.screenMouseX >= 12
                        && this.screenMouseX < 82
                        && this.screenMouseY >= this.height - 32
                        && this.screenMouseY < this.height - 10;
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(12.0F, this.height - 32.0F, 70.0F, 22.0F)
                        .setRadius(5.0F)
                        .setColor(
                                scriptsHovered
                                        ? new Color(48, 55, 70, 238)
                                        : new Color(22, 25, 32, 232)));
        SizedFont scriptsFont = FontManager.getFont("Product Sans Bold", 8.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText("Scripts")
                        .setPosition(12.0F, this.height - 32.0F)
                        .centerInBox(70.0F, 22.0F)
                        .setFont(scriptsFont.getMsdfFont())
                        .setFontSize(scriptsFont.getSize())
                        .enableShadow()
                        .setColor(
                                scriptsHovered
                                        ? Color.WHITE.getRGB()
                                        : new Color(205, 210, 220).getRGB()));
    }

    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.selectedModuleCard != null) {
            for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                if ((settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden())
                        && settingWidget.keyPressed(event)) {
                    return true;
                }
            }
        }

        for (PanelModuleCard moduleCard : this.moduleCards) {
            if (moduleCard.keyPressed(event)) {
                return true;
            }
        }

        return super.keyPressed(event);
    }

    public boolean charTyped(@NonNull CharacterEvent event) {
        if (this.selectedModuleCard != null) {
            for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                if ((settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden())
                        && settingWidget.charTyped(event)) {
                    return true;
                }
            }
        }

        return super.charTyped(event);
    }

    @Override
    public void extractSapphireItems(GuiGraphicsExtractor guiGraphicsExtractor) {
        float settingsPanelProgress = (float) this.settingsPanelAnimation.getValue();
        if (!(settingsPanelProgress <= 0.01F) && this.selectedModuleCard != null) {
            float panelX = this.getPanelX();
            float panelY = this.getPanelY();
            float panelCenterX = panelX + 220.0F;
            float panelCenterY = panelY + 125.0F;
            float contentX = panelX + 90.0F + 1.0F;
            float settingsX = contentX + 349.0F - 200.0F * settingsPanelProgress;
            float settingsY = panelY + 20.0F;
            float settingsWidth = 200.0F * settingsPanelProgress;
            float settingsContentY = settingsY + 20.0F;
            guiGraphicsExtractor.pose().pushMatrix();
            guiGraphicsExtractor.pose().translate(panelCenterX, panelCenterY);
            guiGraphicsExtractor.pose().scale(this.currentScale, this.currentScale);
            guiGraphicsExtractor.pose().translate(-panelCenterX, -panelCenterY);
            guiGraphicsExtractor.enableScissor(
                    Math.round(settingsX + 1.0F),
                    Math.round(settingsContentY),
                    Math.round(settingsX + settingsWidth),
                    Math.round(settingsY + 230.0F));

            for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                if ((settingWidget.getSetting() == null || !settingWidget.getSetting().isHidden())
                        && settingWidget instanceof BlockListSettingWidget blockListWidget) {
                    blockListWidget.renderBlockIcons(guiGraphicsExtractor);
                }
            }

            guiGraphicsExtractor.disableScissor();
            guiGraphicsExtractor.pose().popMatrix();
        }
    }

    private void drawSettingsPanel(
            Render2DContext renderer, float panelX, float panelY, float mouseX, float mouseY) {
        float settingsPanelProgress = (float) this.settingsPanelAnimation.getValue();
        if (!(settingsPanelProgress <= 0.01F)) {
            float contentX = panelX + 90.0F + 1.0F;
            float settingsX = contentX + 349.0F - 200.0F * settingsPanelProgress;
            float settingsY = panelY + 20.0F;
            float settingsWidth = 200.0F * settingsPanelProgress;
            renderer.beginScissor(panelX, panelY, 440.0F, 250.0F);
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(settingsX, settingsY, 1.0F, 230.0F)
                            .setColor(new Color(35, 35, 35)));
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(settingsX + 1.0F, settingsY, settingsWidth - 1.0F, 230.0F)
                            .setCornerRadii(0.0F, 0.0F, 8.0F, 0.0F)
                            .setColor(new Color(18, 18, 18)));
            if (this.selectedModuleCard == null) {
                renderer.endScissor();
            } else {
                SizedFont settingsFont = FontManager.getFont(boldOrMediumFontName(false), 7.0F);
                String settingsTitle = this.selectedModuleCard.getModule().getName() + " Settings";
                renderer.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        settingsX + 1.0F,
                                        settingsY + 20.0F - 1.0F,
                                        settingsWidth - 1.0F,
                                        1.0F)
                                .setColor(new Color(30, 30, 30)));
                boolean backHovered =
                        RenderUtil.isInsideRect(
                                mouseX, mouseY, settingsX + 6.0F, settingsY + 2.0F, 16.0F, 16.0F);
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText("<")
                                .setPosition(settingsX + 8.0F, settingsY)
                                .centerVertically(20.0F)
                                .setFont(settingsFont.getMsdfFont())
                                .setFontSize(settingsFont.getSize())
                                .enableShadow()
                                .setColor(
                                        backHovered
                                                ? Color.WHITE.getRGB()
                                                : new Color(120, 120, 120).getRGB()));
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(settingsTitle)
                                .setPosition(settingsX + 20.0F, settingsY)
                                .centerVertically(20.0F)
                                .setFont(settingsFont.getMsdfFont())
                                .setFontSize(settingsFont.getSize())
                                .enableShadow()
                                .setColor(Color.WHITE.getRGB()));
                float listTopY = settingsY + 20.0F;
                renderer.beginScissor(settingsX + 1.0F, listTopY, settingsWidth - 1.0F, 210.0F);
                float widgetY = listTopY + this.settingsScrollOffset + 2.0F;

                for (SettingWidget settingWidget : this.selectedModuleCard.getSettingWidgets()) {
                    if (settingWidget.getSetting() == null
                            || !settingWidget.getSetting().isHidden()) {
                        settingWidget.setX(settingsX + 5.0F);
                        settingWidget.setY(widgetY);
                        settingWidget.setWidth(settingsWidth - 10.0F);
                        settingWidget.render(renderer, mouseX, mouseY);
                        widgetY += settingWidget.getHeight();
                    }
                }

                renderer.endScissor();
                renderer.endScissor();
            }
        }
    }

    private void drawModuleCards(
            Render2DContext renderer, float panelX, float panelY, float mouseX, float mouseY) {
        float contentX = panelX + 90.0F + 1.0F;
        float contentY = panelY + 20.0F;
        float settingsPanelProgress = (float) this.settingsPanelAnimation.getValue();
        float moduleAreaWidth = 349.0F - 200.0F * settingsPanelProgress;
        renderer.beginScissor(contentX, contentY, moduleAreaWidth, 230.0F);
        float cardY = contentY + this.moduleScrollOffset;

        for (PanelModuleCard moduleCard : this.moduleCards) {
            moduleCard.setX(contentX);
            moduleCard.setY(cardY);
            moduleCard.setWidth(moduleAreaWidth);
            moduleCard.setHeight(20.0F);
            moduleCard.render(renderer, mouseX, mouseY);
            cardY += 21.0F;
        }

        renderer.endScissor();
    }

    private float getPanelX() {
        return (this.width - 440.0F) / 2.0F + this.panelOffsetX;
    }

    private float[] toPanelCoordinates(double screenX, double screenY) {
        float scale = this.currentScale > 0.0F ? this.currentScale : 1.0F;
        float panelX = this.getPanelX();
        float panelY = this.getPanelY();
        float panelCenterX = panelX + 220.0F;
        float panelCenterY = panelY + 125.0F;
        return new float[] {
            (float) ((screenX - panelCenterX) / scale + panelCenterX),
            (float) ((screenY - panelCenterY) / scale + panelCenterY)
        };
    }

    private void rebuildModuleCards() {
        this.moduleCards.clear();
        if (this.selectedCategory != null) {
            for (Module module2 :
                    Astralis.getInstance()
                            .getModuleManager()
                            .getModulesFromCategory(this.selectedCategory)) {
                this.moduleCards.add(new PanelModuleCard(module2, 0.0F, 0.0F, 0.0F, 20.0F));
            }
        }
    }

    private float getPanelY() {
        return (this.height - 250.0F) / 2.0F + this.panelOffsetY;
    }

    private void tryLoadPfp() {
        // Client-backend avatars were removed along with client authentication.
    }

    public void refreshModuleCards() {
        this.rebuildModuleCards();
    }

    public void selectCategory(Category category) {
        if (this.selectedCategory != category) {
            this.selectedCategory = category;
            this.targetModuleScrollOffset = 0.0F;
            this.moduleScrollAnimation.setValue(0.0);
            this.selectedModuleCard = null;
            this.settingsPanelAnimation.setValue(0.0);
            this.rebuildModuleCards();

            for (PanelCategoryTab categoryTab : this.categoryTabs) {
                categoryTab.setSelected(categoryTab.getCategory() == category);
            }
        }
    }

    private void drawProfileArea(Render2DContext renderer, float panelX, float panelY) {
        float profileAreaY = panelY + 250.0F - 62.0F;
        float profileCenterX = panelX + 45.0F;
        float avatarX = profileCenterX - 13.0F;
        float avatarY = profileAreaY + 8.0F;
        if (this.pfpTexture != null) {
            renderer.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds(avatarX + 1.5F, avatarY + 1.5F, 23.0F, 23.0F)
                            .setTexture(this.pfpTexture)
                            .setRadius(11.5F)
                            .setColor(Color.WHITE));
        }

        SizedFont nameFont = FontManager.getFont(boldOrMediumFontName(false), 7.0F);
        SizedFont subtitleFont = FontManager.getFont(regularFontName(), 6.0F);
        float nameWidth = nameFont.getStringWidth(astralis.OfflineServices.profileName());
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(astralis.OfflineServices.profileName())
                        .setPosition(profileCenterX - nameWidth / 2.0F, avatarY + 26.0F + 4.0F)
                        .setFont(nameFont.getMsdfFont())
                        .setFontSize(nameFont.getSize())
                        .enableShadow()
                        .setColor(Color.WHITE.getRGB()));
        String subtitle = "Local profile";
        float subtitleWidth = subtitleFont.getStringWidth(subtitle);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(subtitle)
                        .setPosition(profileCenterX - subtitleWidth / 2.0F, avatarY + 26.0F + 13.0F)
                        .setFont(subtitleFont.getMsdfFont())
                        .setFontSize(subtitleFont.getSize())
                        .enableShadow()
                        .setColor(new Color(120, 120, 125).getRGB()));
    }

    private void drawCategoryTabs(
            Render2DContext renderer, float panelX, float panelY, float mouseX, float mouseY) {
        int tabCount = this.categoryTabs.size();
        float tabsY = panelY + 20.0F;
        float tabHeight = 168.0F / tabCount;

        for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
            PanelCategoryTab categoryTab = this.categoryTabs.get(tabIndex);
            categoryTab.setX(panelX);
            categoryTab.setY(tabsY + tabIndex * tabHeight);
            categoryTab.setWidth(90.0F);
            categoryTab.setHeight(tabHeight);
            categoryTab.render(renderer, mouseX, mouseY);
        }
    }

    public void setHoveredDescription(String description) {
        this.hoveredDescription = description;
    }

    public static String regularFontName() {
        ClickGui clickGui = Astralis.getInstance().getModuleManager().getModule(ClickGui.class);
        return clickGui != null && clickGui.getPanelFontSetting().is("Tahoma")
                ? "Tahoma"
                : "Product Sans Regular";
    }

    private float getConfiguredScale() {
        ClickGui clickGui = Astralis.getInstance().getModuleManager().getModule(ClickGui.class);
        return clickGui != null ? clickGui.getPanelScaleSetting().getValue() : 1.0F;
    }

    private void drawTitleBar(
            Render2DContext renderer, float panelX, float panelY, float mouseX, float mouseY) {
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(panelX, panelY, 440.0F, 20.0F)
                        .setCornerRadii(8.0F, 8.0F, 0.0F, 0.0F)
                        .setColor(new Color(18, 18, 18)));
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(panelX, panelY + 20.0F - 1.0F, 440.0F, 1.0F)
                        .setColor(new Color(30, 30, 30)));
        SizedFont titleFont = FontManager.getFont(boldOrMediumFontName(true), 8.0F);
        Color color =
                ColorUtil.animatedBlend(
                        panelY + 10.0F,
                        Astralis.getInstance().getPrimaryColor(),
                        Astralis.getInstance().getSecondaryColor());
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(Astralis.displayName)
                        .setPosition(panelX + 8.0F, panelY)
                        .centerVertically(20.0F)
                        .setFont(titleFont.getMsdfFont())
                        .setFontSize(titleFont.getSize())
                        .enableShadow()
                        .setHorizontalGradient(color.getRGB(), color.getRGB()));
        SizedFont versionFont = FontManager.getFont(regularFontName(), 6.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText("v2.0")
                        .setPosition(
                                panelX
                                        + 8.0F
                                        + titleFont.getStringWidth(Astralis.displayName)
                                        + 5.0F,
                                panelY)
                        .centerVertically(20.0F)
                        .setFont(versionFont.getMsdfFont())
                        .setFontSize(versionFont.getSize())
                        .enableShadow()
                        .setColor(new Color(120, 120, 125).getRGB()));
        SizedFont closeFont = FontManager.getFont(boldOrMediumFontName(false), 8.0F);
        float closeX = panelX + 440.0F - 18.0F;
        float closeY = panelY + 2.0F;
        boolean closeHovered =
                RenderUtil.isInsideRect(mouseX, mouseY, closeX, closeY, 16.0F, 16.0F);
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText("x")
                        .setPosition(closeX, panelY)
                        .centerInBox(16.0F, 20.0F)
                        .setFont(closeFont.getMsdfFont())
                        .setFontSize(closeFont.getSize())
                        .enableShadow()
                        .setColor(
                                closeHovered
                                        ? Color.WHITE.getRGB()
                                        : new Color(120, 120, 120).getRGB()));
    }

    public static String boldOrMediumFontName(boolean bold) {
        ClickGui clickGui = Astralis.getInstance().getModuleManager().getModule(ClickGui.class);
        if (clickGui != null && clickGui.getPanelFontSetting().is("Tahoma")) {
            return bold ? "Tahoma Bold" : "Tahoma";
        } else {
            return bold ? "Product Sans Bold" : "Product Sans Medium";
        }
    }
}
