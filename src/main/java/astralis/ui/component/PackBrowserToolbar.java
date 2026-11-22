package astralis.ui.component;

import astralis.ui.screen.PackBrowserScreen;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.font.SizedFont;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;

public class PackBrowserToolbar extends PackBrowserComponent {
    private final PackBrowserTextField searchField;
    private final PackBrowserButton sortButton;
    private final PackBrowserButton sizeFilterButton;
    private final PackBrowserButton versionFilterButton;
    private final PackBrowserButton searchButton;

    public PackBrowserToolbar(PackBrowserScreen screen) {
        super(screen);

        this.searchField =
                new PackBrowserTextField(
                        screen, "Search packs...", 64, "Product Sans Regular", 7.0F);
        this.searchField.setOnSubmit(screen::searchPacks);
        this.sortButton =
                new PackBrowserButton(
                        screen,
                        () -> screen.getSortOrder().getDisplayName(),
                        98.0F,
                        16.0F,
                        6.5F,
                        PackBrowserButtonStyle.ICON,
                        PackBrowserPaletteColors.TEXT_SECONDARY,
                        () -> true,
                        screen::cycleSortOrder);
        this.sizeFilterButton =
                new PackBrowserButton(
                        screen,
                        () -> {
                            String label;
                            if (screen.getResolutionFilter().isEmpty()) {

                                label = "All sizes";
                            } else {
                                label = screen.getResolutionFilter() + "x";
                            }

                            return label;
                        },
                        62.0F,
                        16.0F,
                        6.5F,
                        PackBrowserButtonStyle.ICON,
                        PackBrowserPaletteColors.TEXT_SECONDARY,
                        () -> true,
                        screen::cycleResolutionFilter);
        this.versionFilterButton =
                new PackBrowserButton(
                        screen,
                        () -> {
                            String label;
                            if (screen.getVersionFilter().isEmpty()) {

                                label = "All versions";
                            } else {
                                label = screen.getVersionFilter();
                            }

                            return label;
                        },
                        70.0F,
                        16.0F,
                        6.5F,
                        PackBrowserButtonStyle.ICON,
                        PackBrowserPaletteColors.TEXT_SECONDARY,
                        () -> true,
                        screen::cycleVersionFilter);
        this.searchButton =
                new PackBrowserButton(
                        screen,
                        () -> {
                            String label;
                            if (screen.isLoading()) {

                                label = "...";
                            } else {

                                label = "Go";
                            }

                            return label;
                        },
                        24.0F,
                        20.0F,
                        6.5F,
                        PackBrowserButtonStyle.PRIMARY,
                        Color.WHITE,
                        () -> true,
                        screen::searchPacks);
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {

        super.setBounds(x, y, width, height);

        SizedFont titleFont = FontManager.getFont("Product Sans Bold", 12.0F);
        float titleX = x + 11.0F;

        float buttonX = titleX + titleFont.getStringWidth("Pack Browser") + 12.0F;
        this.sortButton.setBounds(buttonX, y + 6.0F, 98.0F, 16.0F);
        buttonX += 104.0F;
        this.sizeFilterButton.setBounds(buttonX, y + 6.0F, 62.0F, 16.0F);
        buttonX += 68.0F;
        this.versionFilterButton.setBounds(buttonX, y + 6.0F, 70.0F, 16.0F);
        this.searchField.setBounds(x + width - 167.0F, y + 4.0F, 132.0F, 20.0F);
        this.searchButton.setBounds(x + width - 30.0F, y + 4.0F, 24.0F, 20.0F);
    }

    @Override
    public void render(Render2DContext context, float mouseX, float mouseY) {
        this.sortButton.render(context, mouseX, mouseY);
        this.sizeFilterButton.render(context, mouseX, mouseY);
        this.versionFilterButton.render(context, mouseX, mouseY);
        this.searchField.render(context, mouseX, mouseY);
        this.searchButton.render(context, mouseX, mouseY);
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        this.searchField.click(mouseButtonEvent);
        this.sortButton.click(mouseButtonEvent);
        this.sizeFilterButton.click(mouseButtonEvent);
        this.versionFilterButton.click(mouseButtonEvent);
        this.searchButton.click(mouseButtonEvent);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return this.searchField.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        return this.searchField.charTyped(characterEvent);
    }

    public boolean isMouseOverControls(double mouseX, double mouseY) {
        return RenderUtil.isInsideRect(
                        mouseX,
                        mouseY,
                        this.searchField.getX(),
                        this.searchField.getY(),
                        this.searchField.getWidth(),
                        this.searchField.getHeight())
                || this.sortButton.isMouseOver(mouseX, mouseY)
                || this.sizeFilterButton.isMouseOver(mouseX, mouseY)
                || this.versionFilterButton.isMouseOver(mouseX, mouseY)
                || this.searchButton.isMouseOver(mouseX, mouseY);
    }

    public void focusSearchField() {
        this.searchField.setFocused(true);
    }

    public String getSearchQuery() {
        return this.searchField.getText().trim();
    }
}
