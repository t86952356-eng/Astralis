package astralis.ui.screen;

import astralis.Astralis;
import astralis.ui.animation.Animation;
import astralis.ui.component.DropdownCategoryPanel;
import astralis.ui.component.SapphireScreen;
import astralis.util.Easing;
import astralis.util.render.FontManager;
import astralis.util.render.FullscreenPassRenderer;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.TexturedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;

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
import java.util.Iterator;
import java.util.Optional;

/** ClickGUI screen in "Dropdown" style: draggable category columns. */
public class DropdownClickGuiScreen extends Screen implements SapphireScreen {
    private final ArrayList<DropdownCategoryPanel> categoryPanels;
    private String hoveredDescription;
    private float targetScrollOffset;
    private final Animation scrollAnimation;
    private volatile int lastMouseX;
    private volatile int lastMouseY;
    private volatile float scrollOffset;
    private int lastLayoutWidth;
    private int lastLayoutHeight;
    private boolean closing;
    private TextureTarget moduleBackgroundCache;
    private boolean backgroundCacheValid;
    private float backgroundCacheScrollOffset;
    private float backgroundCacheScale;
    private float retainedTextScrollOffset;
    private float retainedTextScale;
    private boolean retainedTextStateValid;

    public DropdownClickGuiScreen() {
        super(Component.empty());
        this.categoryPanels = new ArrayList<>();
        this.hoveredDescription = "";
        this.targetScrollOffset = 0.0F;
        this.scrollAnimation = new Animation(Easing.EASE_OUT_QUINT, 600L);
        this.lastLayoutWidth = -1;
        this.lastLayoutHeight = -1;
        this.closing = false;
    }

    @Override
    protected void init() {
        super.init();
        this.backgroundCacheValid = false;
        this.refreshModules();
        this.categoryPanels.forEach(DropdownCategoryPanel::startOpenAnimation);
        int categoryCount = Category.values().length;
        float columnsWidth = categoryCount * 115.0F + (categoryCount - 1) * 10.0F;
        float columnsStartX = this.width / 2.0F - columnsWidth / 2.0F;
        int columnIndex = 0;
        if (this.categoryPanels.isEmpty()) {
            for (Category category : Category.values()) {
                float panelX = columnsStartX + columnIndex * 125.0F;
                this.categoryPanels.add(
                        new DropdownCategoryPanel(
                                this,
                                category,
                                Astralis.getInstance()
                                        .getModuleManager()
                                        .getModulesFromCategory(category),
                                panelX,
                                50.0F,
                                115.0F,
                                20.0F,
                                columnIndex));
                columnIndex++;
            }
        } else {
            for (DropdownCategoryPanel panel : this.categoryPanels) {
                if (!panel.isManuallyPositioned()) {
                    panel.setX(columnsStartX + panel.getColumnIndex() * 125.0F);
                }
            }
        }
    }

    public void setHoveredDescription(String hoveredDescription) {
        this.hoveredDescription = hoveredDescription;
    }

    @Override
    public void onClose() {
        if (!this.closing) {
            this.closing = true;
            this.categoryPanels.forEach(DropdownCategoryPanel::startCloseAnimation);
        } else {
            super.onClose();
        }
    }

    private void updateLayoutIfResized() {
        if (this.lastLayoutWidth != this.width || this.lastLayoutHeight != this.height) {
            this.lastLayoutWidth = this.width;
            this.lastLayoutHeight = this.height;
            this.backgroundCacheValid = false;
            float columnsWidth =
                    Category.values().length * 115.0F + (Category.values().length - 1) * 10.0F;
            float columnsStartX = this.width / 2.0F - columnsWidth / 2.0F;

            for (DropdownCategoryPanel panel : this.categoryPanels) {
                if (!panel.isManuallyPositioned()) {
                    panel.setX(columnsStartX + panel.getColumnIndex() * 125.0F);
                }

                panel.setY(50.0F);
            }
        }
    }

    @Override
    public void extractRenderState(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredDescription = "";
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.updateLayoutIfResized();
        this.scrollAnimation.animateTo(this.targetScrollOffset);
        this.scrollOffset = (float) this.scrollAnimation.getValue();
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (this.closing
                && this.categoryPanels.stream()
                        .allMatch(DropdownCategoryPanel::isCloseAnimationFinished)) {
            this.closing = false;
            super.onClose();
        }
    }

    @Override
    public void extractBackground(
            @NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        this.backgroundCacheValid = false;
        if (event.button() == 0
                && event.x() >= 12.0
                && event.x() < 82.0
                && event.y() >= this.height - 32.0
                && event.y() < this.height - 10.0) {
            Minecraft.getInstance().gui.setScreen(new ScriptEditorScreen(this));
            return true;
        }

        float fitScale = this.computeFitScale();
        double contentMouseX = (event.x() - this.width / 2.0F) / fitScale + this.width / 2.0F;
        double contentMouseY = (event.y() - this.height / 2.0F) / fitScale + this.height / 2.0F;
        float animatedScrollOffset = (float) this.scrollAnimation.getValue();
        MouseButtonEvent mouseButtonEvent =
                new MouseButtonEvent(
                        contentMouseX, contentMouseY - animatedScrollOffset, event.buttonInfo());

        for (int panelIndex = this.categoryPanels.size() - 1; panelIndex >= 0; panelIndex--) {
            DropdownCategoryPanel panel = this.categoryPanels.get(panelIndex);
            if (panel.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                panel.click(mouseButtonEvent);
                break;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        this.backgroundCacheValid = false;
        float fitScale = this.computeFitScale();
        double contentMouseX = (event.x() - this.width / 2.0F) / fitScale + this.width / 2.0F;
        double contentMouseY = (event.y() - this.height / 2.0F) / fitScale + this.height / 2.0F;
        float animatedScrollOffset = (float) this.scrollAnimation.getValue();
        MouseButtonEvent mouseButtonEvent =
                new MouseButtonEvent(
                        contentMouseX, contentMouseY - animatedScrollOffset, event.buttonInfo());
        this.categoryPanels.forEach(panel -> panel.release(mouseButtonEvent));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.backgroundCacheValid = false;
        this.targetScrollOffset += (float) (scrollY * 30.0);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderSapphire(Render2DContext renderer) {

        float currentScrollOffset = this.scrollOffset;
        int mouseX = this.lastMouseX;
        int mouseY = this.lastMouseY;
        float fitScale = this.computeFitScale();
        float contentMouseX = (mouseX - this.width / 2.0F) / fitScale + this.width / 2.0F;
        float contentMouseY = (mouseY - this.height / 2.0F) / fitScale + this.height / 2.0F;
        renderer.pushPose();
        renderer.translate(this.width / 2.0F, this.height / 2.0F);
        renderer.scale(fitScale, fitScale, 1.0F);
        renderer.translate(-this.width / 2.0F, -this.height / 2.0F);
        renderer.translate(0.0F, currentScrollOffset);
        float visibleTop =
                this.height / 2.0F - this.height / (2.0F * fitScale) - currentScrollOffset;
        float visibleBottom =
                this.height / 2.0F + this.height / (2.0F * fitScale) - currentScrollOffset;
        int primaryRgb = Astralis.getInstance().getPrimaryColor().getRGB();
        int secondaryRgb = Astralis.getInstance().getSecondaryColor().getRGB();
        double timeSeconds = System.currentTimeMillis() / 1000.0;

        for (DropdownCategoryPanel panel : this.categoryPanels) {
            panel.update(
                    contentMouseX,
                    contentMouseY - currentScrollOffset,
                    visibleTop,
                    visibleBottom,
                    primaryRgb,
                    secondaryRgb,
                    timeSeconds);
        }

        boolean allPanelsAtRest = true;

        for (DropdownCategoryPanel restCheckPanel : this.categoryPanels) {
            if (!restCheckPanel.isAtRest()) {
                allPanelsAtRest = false;
                break;
            }
        }

        if (!allPanelsAtRest
                || !this.retainedTextStateValid
                || Math.abs(this.retainedTextScrollOffset - currentScrollOffset) > 0.01F
                || Math.abs(this.retainedTextScale - fitScale) > 1.0E-4F) {
            for (DropdownCategoryPanel retainedTextPanel : this.categoryPanels) {
                retainedTextPanel.invalidateRetainedText();
            }

            this.retainedTextScrollOffset = currentScrollOffset;
            this.retainedTextScale = fitScale;
            this.retainedTextStateValid = true;
        }

        if (allPanelsAtRest && !this.hasOverlappingPanels()) {
            this.updateModuleBackgroundCache(renderer, currentScrollOffset, fitScale);
            renderer.popPose();
            renderer.drawTexture(
                    TexturedRectShape.obtain()
                            .setBounds(0.0F, 0.0F, this.width, this.height)
                            .setTexture(this.moduleBackgroundCache.getColorTexture())
                            .setUv(0.0F, 1.0F, 1.0F, 0.0F)
                            .setColor(Color.WHITE));
            renderer.pushPose();
            renderer.translate(this.width / 2.0F, this.height / 2.0F);
            renderer.scale(fitScale, fitScale, 1.0F);
            renderer.translate(-this.width / 2.0F, -this.height / 2.0F);
            renderer.translate(0.0F, currentScrollOffset);

            for (DropdownCategoryPanel dynamicBackgroundPanel : this.categoryPanels) {
                dynamicBackgroundPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.DYNAMIC_MODULE_BACKGROUNDS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }

            for (DropdownCategoryPanel toggleOverlayPanel : this.categoryPanels) {
                toggleOverlayPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.TOGGLE_OVERLAYS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }

            for (DropdownCategoryPanel settingsPanel : this.categoryPanels) {
                settingsPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.SETTINGS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }

            for (DropdownCategoryPanel moduleLabelPanel : this.categoryPanels) {
                moduleLabelPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.MODULE_LABELS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }

            for (DropdownCategoryPanel headerBackgroundPanel : this.categoryPanels) {
                headerBackgroundPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.HEADER_BACKGROUND,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }

            for (DropdownCategoryPanel headerLabelPanel : this.categoryPanels) {
                headerLabelPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.HEADER_LABEL,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }
        } else {
            this.backgroundCacheValid = false;

            for (DropdownCategoryPanel uncachedPanel : this.categoryPanels) {
                uncachedPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.HEADER_BACKGROUND,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
                uncachedPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.MODULE_BACKGROUNDS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
                uncachedPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.TOGGLE_OVERLAYS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
                uncachedPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.SETTINGS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
                uncachedPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.HEADER_LABEL,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
                uncachedPanel.renderLayer(
                        renderer,
                        DropdownRenderLayer.MODULE_LABELS,
                        contentMouseX,
                        contentMouseY - currentScrollOffset);
            }
        }

        boolean isOverScripts;
        label146:
        {
            renderer.popPose();
            Astralis.getInstance()
                    .getRenderEffects()
                    .renderShadowShapes(
                            maskRenderer -> {
                                maskRenderer.pushPose();
                                maskRenderer.translate(this.width / 2.0F, this.height / 2.0F);
                                maskRenderer.scale(fitScale, fitScale, 1.0F);
                                maskRenderer.translate(-this.width / 2.0F, -this.height / 2.0F);
                                maskRenderer.translate(0.0F, currentScrollOffset);
                                this.categoryPanels.forEach(
                                        maskPanel -> maskPanel.renderMask(maskRenderer));
                                maskRenderer.popPose();
                            });

            if (mouseX >= 12) {

                if (mouseX < 82) {

                    if (mouseY >= this.height - 32) {

                        if (mouseY < this.height - 10) {
                            isOverScripts = true;
                            break label146;
                        }
                    }
                }
            }

            isOverScripts = false;
        }

        boolean scriptsHovered = isOverScripts;
        RoundedRectShape scriptsRect =
                RoundedRectShape.obtain()
                        .setBounds(12.0F, this.height - 32.0F, 70.0F, 22.0F)
                        .setRadius(5.0F);
        Color color;
        if (scriptsHovered) {

            color = new Color(48, 55, 70, 238);
        } else {

            color = new Color(22, 25, 32, 232);
        }

        renderer.drawRect(scriptsRect.setColor(color));

        SizedFont scriptsFont = FontManager.getFont("Product Sans Bold", 8.0F);
        TextDrawRequest scriptsTextBuilder = TextDrawRequest.obtain();

        TextDrawRequest scriptsText =
                scriptsTextBuilder
                        .setText("Scripts")
                        .setPosition(12.0F, this.height - 32.0F)
                        .centerInBox(70.0F, 22.0F)
                        .setFont(scriptsFont.getMsdfFont())
                        .setFontSize(scriptsFont.getSize())
                        .enableShadow();
        int scriptsTextRgb;
        if (scriptsHovered) {
            scriptsTextRgb = Color.WHITE.getRGB();
        } else {

            scriptsTextRgb = new Color(205, 210, 220).getRGB();
        }

        renderer.drawText(scriptsText.setColor(scriptsTextRgb));
        if (!this.hoveredDescription.isEmpty()) {

            SizedFont descriptionFont = FontManager.getFont("Product Sans Regular", 8.0F);
            ArrayList<String> descriptionLines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();

            for (String word : this.hoveredDescription.split(" ")) {
                String candidateLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (!currentLine.isEmpty()
                        && descriptionFont.getStringWidth(candidateLine) > 220.0F) {
                    descriptionLines.add(currentLine.toString());
                    currentLine.setLength(0);
                }

                if (!currentLine.isEmpty()) {
                    currentLine.append(' ');
                }

                currentLine.append(word);
            }

            if (!currentLine.isEmpty()) {
                descriptionLines.add(currentLine.toString());
            }

            float maxLineWidth = 0.0F;

            for (String wrappedLine : descriptionLines) {
                maxLineWidth = Math.max(maxLineWidth, descriptionFont.getStringWidth(wrappedLine));
            }

            float descriptionBoxWidth = maxLineWidth + 18.0F;
            float descriptionBoxHeight = descriptionLines.size() * 10.0F + 10.0F;
            float descriptionBoxX = this.width - descriptionBoxWidth - 12.0F;
            float descriptionBoxY = this.height - descriptionBoxHeight - 12.0F;

            Color color2 = new Color(14, 16, 21, 232);

            Color color3 = new Color(235, 237, 242);
            Astralis.getInstance()
                    .getRenderEffects()
                    .drawBlurredRoundedRectangle(
                            descriptionBoxX,
                            descriptionBoxY,
                            descriptionBoxWidth,
                            descriptionBoxHeight,
                            5.0F,
                            1.0F);
            renderer.drawRect(
                    RoundedRectShape.obtain()
                            .setBounds(
                                    descriptionBoxX,
                                    descriptionBoxY,
                                    descriptionBoxWidth,
                                    descriptionBoxHeight)
                            .setRadius(5.0F)
                            .setColor(color2));
            float descriptionLineY = descriptionBoxY + 5.0F;

            for (String lineToDraw : descriptionLines) {
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(lineToDraw)
                                .setPosition(descriptionBoxX, descriptionLineY)
                                .centerInBox(descriptionBoxWidth, 10.0F)
                                .setFont(descriptionFont.getMsdfFont())
                                .setFontSize(descriptionFont.getSize())
                                .enableShadow()
                                .setColor(color3.getRGB()));
                descriptionLineY += 10.0F;
            }
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        this.backgroundCacheValid = false;
        boolean handled = false;

        for (DropdownCategoryPanel panel : this.categoryPanels) {
            if (panel.keyPressed(event)) {
                handled = true;
            }
        }

        return handled ? true : super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        this.backgroundCacheValid = false;
        boolean handled = false;

        for (DropdownCategoryPanel panel : this.categoryPanels) {
            if (panel.charTyped(event)) {
                handled = true;
            }
        }

        return handled ? true : super.charTyped(event);
    }

    @Override
    public void extractSapphireItems(GuiGraphicsExtractor guiGraphicsExtractor) {
        float fitScale = this.computeFitScale();
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor.pose().translate(this.width / 2.0F, this.height / 2.0F);
        guiGraphicsExtractor.pose().scale(fitScale, fitScale);
        guiGraphicsExtractor.pose().translate(-this.width / 2.0F, -this.height / 2.0F);
        guiGraphicsExtractor.pose().translate(0.0F, this.scrollOffset);
        this.categoryPanels.forEach(panel -> panel.extractSettingItems(guiGraphicsExtractor));
        guiGraphicsExtractor.pose().popMatrix();
    }

    private float computeFitScale() {
        float columnsWidth =
                Category.values().length * 115.0F + (Category.values().length - 1) * 10.0F;
        return Math.clamp((this.width - 24.0F) / columnsWidth, 0.5F, 1.0F);
    }

    public void bringToFront(DropdownCategoryPanel panel) {
        if (this.categoryPanels.remove(panel)) {
            this.categoryPanels.add(panel);
            this.backgroundCacheValid = false;
        }
    }

    private boolean hasOverlappingPanels() {
        int i = 0;

        while (i < this.categoryPanels.size()) {
            DropdownCategoryPanel panel = this.categoryPanels.get(i);
            float panelRight = panel.getX() + panel.getWidth();
            float panelBottom = panel.getY() + panel.getTotalHeight();

            for (int j = i + 1; j < this.categoryPanels.size(); j++) {
                DropdownCategoryPanel otherPanel = this.categoryPanels.get(j);
                if (panel.getX() < otherPanel.getX() + otherPanel.getWidth()
                        && panelRight > otherPanel.getX()
                        && panel.getY() < otherPanel.getY() + otherPanel.getTotalHeight()
                        && panelBottom > otherPanel.getY()) {
                    return true;
                }
            }

            i++;
        }

        return false;
    }

    private void clearCacheTarget(RenderTarget renderTarget) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderPass renderPass =
                commandEncoder.createRenderPass(
                        () -> "Dropdown Module Cache Clear",
                        renderTarget.getColorTextureView(),
                        Optional.of(FullscreenPassRenderer.clearColor));
        if (renderPass != null) {
            renderPass.close();
        }
    }

    private void updateModuleBackgroundCache(
            Render2DContext renderer, float currentScrollOffset, float fitScale) {
        if (this.moduleBackgroundCache == null
                || this.moduleBackgroundCache.width != renderer.getTargetWidth()
                || this.moduleBackgroundCache.height != renderer.getTargetHeight()) {
            if (this.moduleBackgroundCache != null) {
                this.moduleBackgroundCache.destroyBuffers();
            }

            this.moduleBackgroundCache =
                    new TextureTarget(
                            "Dropdown Module Background Cache",
                            renderer.getTargetWidth(),
                            renderer.getTargetHeight(),
                            false,
                            GpuFormat.RGBA8_UNORM);
            this.backgroundCacheValid = false;
        }

        if (this.backgroundCacheValid
                && (Math.abs(this.backgroundCacheScrollOffset - currentScrollOffset) > 0.01F
                        || Math.abs(this.backgroundCacheScale - fitScale) > 1.0E-4F)) {
            this.backgroundCacheValid = false;
        }

        for (DropdownCategoryPanel panel : this.categoryPanels) {
            if (panel.hasExpandAnimatingEntry() || panel.consumeStaticBackgroundDirty()) {
                this.backgroundCacheValid = false;
            }
        }

        if (!this.backgroundCacheValid) {
            RenderTarget renderTarget = renderer.getRenderTarget();
            this.clearCacheTarget(this.moduleBackgroundCache);
            renderer.setRenderTarget(this.moduleBackgroundCache);

            for (DropdownCategoryPanel staticBackgroundPanel : this.categoryPanels) {
                staticBackgroundPanel.renderLayer(
                        renderer, DropdownRenderLayer.STATIC_MODULE_BACKGROUNDS, 0.0F, 0.0F);
            }

            renderer.flush();
            renderer.setRenderTarget(renderTarget);
            this.backgroundCacheScrollOffset = currentScrollOffset;
            this.backgroundCacheScale = fitScale;
            this.backgroundCacheValid = true;
        }
    }

    public void refreshModules() {
        this.backgroundCacheValid = false;
        Iterator iterator = this.categoryPanels.iterator();

        while (iterator.hasNext()) {
            DropdownCategoryPanel panel = (DropdownCategoryPanel) iterator.next();
            panel.refresh(
                    Astralis.getInstance()
                            .getModuleManager()
                            .getModulesFromCategory(panel.getCategory()));
        }
    }
}
