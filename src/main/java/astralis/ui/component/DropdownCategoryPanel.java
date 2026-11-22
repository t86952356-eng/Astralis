package astralis.ui.component;

import astralis.Astralis;
import astralis.module.Module;
import astralis.ui.animation.Animation;
import astralis.ui.screen.DropdownClickGuiScreen;
import astralis.ui.screen.DropdownRenderLayer;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import lombok.Generated;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Dropdown ClickGUI column listing one category's modules. */
public class DropdownCategoryPanel extends SettingWidget {
    private final List<DropdownModuleEntry> moduleEntries;
    private final Category category;
    private final int columnIndex;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean manuallyPositioned;
    private float lastMouseX;
    private float lastMouseY;
    private float tiltPivotX;
    private float tiltPivotY;
    private float tiltAngle;
    private float previousTiltAngle;
    private float panelScale;
    private float totalHeight;
    private int headerTextColor;
    private final Animation openAnimation;
    private final Animation closeAnimation;
    private boolean closing;
    private final DropdownClickGuiScreen screen;

    public DropdownCategoryPanel(
            DropdownClickGuiScreen screen,
            Category category,
            List<Module> modules,
            float panelX,
            float panelY,
            float panelWidth,
            float panelHeight,
            int columnIndex) {
        super(panelX, panelY, panelWidth, panelHeight);
        this.moduleEntries = new ArrayList<>();
        this.dragging = false;
        this.tiltAngle = 0.0F;
        this.previousTiltAngle = 0.0F;
        this.panelScale = 1.0F;

        this.openAnimation = new Animation(Easing.EASE_OUT_BACK, 500L);

        this.closeAnimation = new Animation(Easing.EASE_IN_BACK, 380L);
        this.closing = false;
        this.category = category;
        this.columnIndex = columnIndex;
        this.startOpenAnimation();
        long columnStagger;
        columnStagger = columnIndex;
        this.openAnimation.setDuration(400L + columnStagger * 150L);
        float entryY = this.getY() + this.getHeight();
        this.screen = screen;

        for (Module module2 : modules) {
            this.moduleEntries.add(
                    new DropdownModuleEntry(
                            this.screen,
                            module2,
                            this.moduleEntries,
                            panelX,
                            entryY,
                            this.getWidth(),
                            19.0F));
            entryY += 19.0F;
        }
    }

    public void startOpenAnimation() {
        this.closing = false;
        this.openAnimation.setValue(0.0);
        this.openAnimation.restart();
    }

    public void startCloseAnimation() {
        this.closing = true;
        this.closeAnimation.setValue(1.0);
        this.closeAnimation.restart();
        long columnStagger;
        columnStagger = this.columnIndex;

        this.closeAnimation.setDuration(280L + columnStagger * 60L);
    }

    public boolean isCloseAnimationFinished() {
        return this.closing && this.closeAnimation.getValue() <= 0.01;
    }

    public void refresh(List<Module> modules) {
        this.moduleEntries.removeIf(entry -> !modules.contains(entry.getModule()));
        Iterator iterator = modules.iterator();

        while (iterator.hasNext()) {
            Module module2 = (Module) iterator.next();
            boolean hasEntry =
                    this.moduleEntries.stream()
                            .anyMatch(existingEntry -> existingEntry.getModule() == module2);
            if (!hasEntry) {
                this.moduleEntries.add(
                        new DropdownModuleEntry(
                                this.screen,
                                module2,
                                this.moduleEntries,
                                this.getX(),
                                0.0F,
                                this.getWidth(),
                                19.0F));
            }
        }

        this.moduleEntries.sort(
                (leftEntry, rightEntry) -> {
                    int leftIndex = modules.indexOf(leftEntry.getModule());
                    int rightIndex = modules.indexOf(rightEntry.getModule());
                    return Integer.compare(leftIndex, rightIndex);
                });
        float entryY = this.getY() + this.getHeight();

        for (DropdownModuleEntry layoutEntry : this.moduleEntries) {
            layoutEntry.setX(this.getX());
            layoutEntry.setY(entryY);
            entryY += 19.0F;
        }
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        int primaryRgb = Astralis.getInstance().getPrimaryColor().getRGB();
        int secondaryRgb = Astralis.getInstance().getSecondaryColor().getRGB();
        this.update(
                mouseX,
                mouseY,
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                primaryRgb,
                secondaryRgb,
                System.currentTimeMillis() / 1000.0);
        this.renderLayer(renderer, DropdownRenderLayer.HEADER_BACKGROUND, mouseX, mouseY);
        this.renderLayer(renderer, DropdownRenderLayer.MODULE_BACKGROUNDS, mouseX, mouseY);
        this.renderLayer(renderer, DropdownRenderLayer.TOGGLE_OVERLAYS, mouseX, mouseY);
        this.renderLayer(renderer, DropdownRenderLayer.SETTINGS, mouseX, mouseY);
        this.renderLayer(renderer, DropdownRenderLayer.HEADER_LABEL, mouseX, mouseY);
        this.renderLayer(renderer, DropdownRenderLayer.MODULE_LABELS, mouseX, mouseY);
    }

    public void update(
            float mouseX,
            float mouseY,
            float viewportTop,
            float viewportBottom,
            int primaryRgb,
            int secondaryRgb,
            double timeSeconds) {
        float mouseDeltaX = mouseX - this.lastMouseX;
        if (this.dragging) {
            this.setX(mouseX - this.dragOffsetX);
            this.setY(mouseY - this.dragOffsetY);
            this.tiltPivotX = mouseX;
            this.tiltPivotY = mouseY;
        }

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        float targetTiltAngle =
                this.dragging ? Math.clamp(mouseDeltaX * 8.0F, -35.0F, 35.0F) : 0.0F;
        this.tiltAngle =
                this.previousTiltAngle + (targetTiltAngle - this.previousTiltAngle) / 35.0F;
        this.previousTiltAngle = this.tiltAngle;
        this.headerTextColor =
                ColorUtil.animatedBlendArgb(this.getY(), primaryRgb, secondaryRgb, timeSeconds);
        if (this.closing) {
            this.closeAnimation.animateTo(0.0);
            this.panelScale = (float) this.closeAnimation.getValue();
        } else {
            this.openAnimation.animateTo(1.0);
            this.panelScale = (float) this.openAnimation.getValue();
        }

        float entryY = this.getY() + this.getHeight() - 1.0F;

        for (DropdownModuleEntry entry : this.moduleEntries) {
            entry.setX(this.getX());
            entry.setY(entryY);
            entry.update(
                    mouseX,
                    mouseY,
                    viewportTop,
                    viewportBottom,
                    !this.dragging && Math.abs(this.tiltAngle) < 0.01F && this.panelScale >= 0.99F,
                    primaryRgb,
                    secondaryRgb,
                    timeSeconds);
            entryY += entry.getHeight() + entry.getSettingsHeight();
        }

        this.totalHeight = entryY - this.getY() + 1.0F;
    }

    public void renderLayer(
            Render2DContext renderer, DropdownRenderLayer layer, float mouseX, float mouseY) {
        this.pushPanelTransform(renderer);
        switch (layer) {
            case HEADER_BACKGROUND:
                renderer.drawRect(
                        RoundedRectShape.obtain()
                                .setBounds(
                                        this.getX(), this.getY(), this.getWidth(), this.getHeight())
                                .setCornerRadii(5.0F, 5.0F, 0.0F, 0.0F)
                                .setColor(ClickGuiPaletteColors.CATEGORY_HEADER_BACKGROUND));
                break;
            case MODULE_BACKGROUNDS:
                for (DropdownModuleEntry backgroundEntry : this.moduleEntries) {
                    if (backgroundEntry.isInViewport()) {
                        backgroundEntry.renderBackground(renderer);
                    }
                }
                break;
            case STATIC_MODULE_BACKGROUNDS:
                for (DropdownModuleEntry staticBackgroundEntry : this.moduleEntries) {
                    if (staticBackgroundEntry.isInViewport()
                            && staticBackgroundEntry.isBackgroundStatic()) {
                        staticBackgroundEntry.renderBackground(renderer);
                    }
                }
                break;
            case DYNAMIC_MODULE_BACKGROUNDS:
                for (DropdownModuleEntry dynamicBackgroundEntry : this.moduleEntries) {
                    if (dynamicBackgroundEntry.isInViewport()
                            && !dynamicBackgroundEntry.isBackgroundStatic()) {
                        dynamicBackgroundEntry.renderBackground(renderer);
                    }
                }
                break;
            case TOGGLE_OVERLAYS:
                for (DropdownModuleEntry toggleEntry : this.moduleEntries) {
                    if (toggleEntry.isInViewport()) {
                        toggleEntry.renderToggleOverlay(renderer);
                    }
                }
                break;
            case SETTINGS:
                for (DropdownModuleEntry settingsEntry : this.moduleEntries) {
                    if (settingsEntry.isInViewport()) {
                        settingsEntry.renderSettings(renderer, mouseX, mouseY);
                    }
                }
                break;
            case HEADER_LABEL:
                SizedFont headerFont = FontManager.getFont("Product Sans Bold", 10.0F);
                renderer.drawText(
                        TextDrawRequest.obtain()
                                .setText(this.category.getName())
                                .setPosition(this.getX(), this.getY())
                                .setFont(headerFont.getMsdfFont())
                                .setFontSize(headerFont.getSize())
                                .centerInBox(this.getWidth(), this.getHeight())
                                .enableShadow()
                                .setColor(this.headerTextColor));
                break;
            case MODULE_LABELS:
                SizedFont moduleLabelFont = FontManager.getFont("Product Sans Medium", 8.0F);

                for (DropdownModuleEntry labelEntry : this.moduleEntries) {
                    if (labelEntry.isInViewport()) {
                        labelEntry.renderLabel(renderer, moduleLabelFont);
                    }
                }
        }

        renderer.popPose();
    }

    public boolean isAtRest() {
        return !this.dragging
                && !this.closing
                && this.openAnimation.isFinished()
                && Math.abs(this.tiltAngle) < 0.01F;
    }

    public boolean hasExpandAnimatingEntry() {
        Iterator iterator = this.moduleEntries.iterator();

        while (iterator.hasNext()) {
            DropdownModuleEntry entry = (DropdownModuleEntry) iterator.next();
            if (entry.isExpandAnimating()) {
                return true;
            }
        }

        return false;
    }

    public boolean consumeStaticBackgroundDirty() {
        boolean anyDirty = false;

        for (DropdownModuleEntry entry : this.moduleEntries) {
            if (entry.consumeStaticBackgroundDirty()) {
                anyDirty = true;
            }
        }

        return anyDirty;
    }

    public void invalidateRetainedText() {
        Iterator iterator = this.moduleEntries.iterator();

        while (iterator.hasNext()) {
            DropdownModuleEntry entry = (DropdownModuleEntry) iterator.next();
            entry.invalidateRetainedText();
        }
    }

    public void extractSettingItems(GuiGraphicsExtractor guiGraphicsExtractor) {
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor
                .pose()
                .translate(
                        this.getX() + this.getWidth() / 2.0F,
                        this.getY() + this.totalHeight / 2.0F);
        guiGraphicsExtractor.pose().scale(this.panelScale, this.panelScale);
        guiGraphicsExtractor
                .pose()
                .translate(
                        -(this.getX() + this.getWidth() / 2.0F),
                        -(this.getY() + this.totalHeight / 2.0F));
        guiGraphicsExtractor.pose().translate(this.tiltPivotX, this.tiltPivotY);
        guiGraphicsExtractor.pose().rotate((float) Math.toRadians(this.tiltAngle));
        guiGraphicsExtractor.pose().translate(-this.tiltPivotX, -this.tiltPivotY);

        for (DropdownModuleEntry entry : this.moduleEntries) {
            if (entry.isInViewport()) {
                entry.extractSettingItems(guiGraphicsExtractor);
            }
        }

        guiGraphicsExtractor.pose().popMatrix();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return RenderUtil.isInsideRect(
                mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.totalHeight);
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight())) {
            this.dragging = true;
            this.manuallyPositioned = true;
            this.screen.bringToFront(this);
            this.dragOffsetX = (float) mouseButtonEvent.x() - this.getX();
            this.dragOffsetY = (float) mouseButtonEvent.y() - this.getY();
            this.tiltPivotX = (float) mouseButtonEvent.x();
            this.tiltPivotY = (float) mouseButtonEvent.y();
        }

        for (DropdownModuleEntry entry : this.moduleEntries) {
            entry.click(mouseButtonEvent);
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.dragging = false;
        float entryY = this.getY() + this.getHeight();

        for (DropdownModuleEntry entry : this.moduleEntries) {
            float entryHeight = entry.getHeight();
            if (entry.isExpanded() || entry.isExpandAnimating()) {
                entryHeight += (float) entry.getExpandAnimation().getValue();
            }

            if (RenderUtil.isInsideRect(
                    mouseButtonEvent.x(),
                    mouseButtonEvent.y(),
                    this.getX(),
                    entryY,
                    this.getWidth(),
                    entryHeight)) {
                entry.release(mouseButtonEvent);
                break;
            }

            entryY += entryHeight;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = false;

        for (DropdownModuleEntry entry : this.moduleEntries) {
            if (entry.keyPressed(event)) {
                handled = true;
            }
        }

        return handled;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean handled = false;
        Iterator iterator = this.moduleEntries.iterator();

        while (iterator.hasNext()) {
            DropdownModuleEntry entry = (DropdownModuleEntry) iterator.next();
            if (entry.charTyped(characterEvent)) {
                handled = true;
            }
        }

        return handled;
    }

    @Override
    public void renderMask(Render2DContext renderer) {
        this.pushPanelTransform(renderer);
        renderer.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                        .setCornerRadii(5.0F, 5.0F, 0.0F, 0.0F)
                        .setColor(Color.black));

        for (DropdownModuleEntry entry : this.moduleEntries) {
            if (entry.isInViewport()) {
                entry.renderMask(renderer);
            }
        }

        renderer.popPose();
    }

    private void pushPanelTransform(Render2DContext renderer) {
        renderer.pushPose();
        renderer.translate(
                this.getX() + this.getWidth() / 2.0F, this.getY() + this.totalHeight / 2.0F);
        renderer.scale(this.panelScale, this.panelScale, 1.0F);
        renderer.translate(
                -(this.getX() + this.getWidth() / 2.0F), -(this.getY() + this.totalHeight / 2.0F));
        renderer.translate(this.tiltPivotX, this.tiltPivotY);
        renderer.rotateZ(this.tiltAngle);
        renderer.translate(-this.tiltPivotX, -this.tiltPivotY);
    }

    @Generated
    public List<DropdownModuleEntry> getModuleEntries() {
        return this.moduleEntries;
    }

    @Generated
    public Category getCategory() {
        return this.category;
    }

    @Generated
    public int getColumnIndex() {
        return this.columnIndex;
    }

    @Generated
    public boolean isDragging() {
        return this.dragging;
    }

    @Generated
    public float getDragOffsetX() {
        return this.dragOffsetX;
    }

    @Generated
    public float getDragOffsetY() {
        return this.dragOffsetY;
    }

    @Generated
    public boolean isManuallyPositioned() {
        return this.manuallyPositioned;
    }

    @Generated
    public float getLastMouseX() {
        return this.lastMouseX;
    }

    @Generated
    public float getLastMouseY() {
        return this.lastMouseY;
    }

    @Generated
    public float getTiltPivotX() {
        return this.tiltPivotX;
    }

    @Generated
    public float getTiltPivotY() {
        return this.tiltPivotY;
    }

    @Generated
    public float getTiltAngle() {
        return this.tiltAngle;
    }

    @Generated
    public float getPreviousTiltAngle() {
        return this.previousTiltAngle;
    }

    @Generated
    public float getPanelScale() {
        return this.panelScale;
    }

    @Generated
    public float getTotalHeight() {
        return this.totalHeight;
    }

    @Generated
    public int getHeaderTextColor() {
        return this.headerTextColor;
    }

    @Generated
    public Animation getOpenAnimation() {
        return this.openAnimation;
    }

    @Generated
    public Animation getCloseAnimation() {
        return this.closeAnimation;
    }

    @Generated
    public boolean isClosing() {
        return this.closing;
    }

    @Generated
    public DropdownClickGuiScreen getScreen() {
        return this.screen;
    }
}
