package astralis.ui.component;

import astralis.Astralis;
import astralis.setting.BlockListSetting;
import astralis.ui.animation.Animation;
import astralis.ui.input.TextInputUtil;
import astralis.util.Easing;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Searchable, scrollable block selection list for a block-list setting. */
public final class BlockListSettingWidget extends SettingWidget {
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float SEARCH_BAR_HEIGHT = 17.0F;
    private static final float ROW_HEIGHT = 14.0F;
    private static final float LIST_HEIGHT = 145.0F;
    private static final float LIST_PADDING = 4.0F;
    private final BlockListSetting blockListSetting;
    private final ArrayList<SettingWidget> siblingWidgets;
    private final Animation expandAnimation;
    private final Animation toggleFlashAnimation;
    private final Animation searchFocusAnimation;
    private boolean expanded;
    private boolean searchFocused;
    private boolean draggingScrollbar;
    private Block lastToggledBlock;
    private long toggleFlashEndTime;
    private String searchQuery;
    private int scrollOffset;

    public BlockListSettingWidget(
            BlockListSetting blockListSetting, ArrayList<SettingWidget> siblingWidgets) {

        this.expandAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 240L);

        this.toggleFlashAnimation = new Animation(Easing.EASE_OUT_QUINT, 180L);

        this.searchFocusAnimation = new Animation(Easing.EASE_OUT_QUINT, 160L);
        this.searchQuery = "";
        this.blockListSetting = blockListSetting;
        this.siblingWidgets = siblingWidgets;
        this.setSetting(blockListSetting);
        this.setHeight(15.0F);
    }

    @Override
    public void render(Render2DContext renderer, float mouseX, float mouseY) {
        long blinkTick;
        List filteredBlocks = this.blockListSetting.filtered(this.searchQuery);
        float expandedHeight = this.updateExpandedHeight();
        this.toggleFlashAnimation.animateTo(
                System.currentTimeMillis() < this.toggleFlashEndTime ? 1.0 : 0.0);
        this.searchFocusAnimation.animateTo(this.searchFocused ? 1.0 : 0.0);
        float toggleFlashProgress = (float) this.toggleFlashAnimation.getValue();
        float searchFocusProgress = (float) this.searchFocusAnimation.getValue();
        SizedFont labelFont = FontManager.getFont(regularFontName(), 8.0F);
        String countText = Integer.toString(this.blockListSetting.getValue().size());
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(this.blockListSetting.getName())
                        .setPosition(this.getX() + 5.0F, this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.white.getRGB()));
        renderer.drawText(
                TextDrawRequest.obtain()
                        .setText(countText)
                        .setPosition(
                                this.getX()
                                        + this.getWidth()
                                        - labelFont.getStringWidth(countText)
                                        - 6.0F,
                                this.getY())
                        .centerVertically(15.0F)
                        .setFont(labelFont.getMsdfFont())
                        .setFontSize(labelFont.getSize())
                        .enableShadow()
                        .setColor(Color.gray.getRGB()));
        if (!(expandedHeight < 0.1F)) {
            float bodyY = this.getY() + 15.0F;
            renderer.beginScissor(this.getX(), bodyY, this.getWidth(), expandedHeight);
            RoundedRectShape searchBarRect =
                    RoundedRectShape.obtain()
                            .setBounds(
                                    this.getX() + 4.0F, bodyY + 1.0F, this.getWidth() - 8.0F, 14.0F)
                            .setRadius(3.0F);
            int red;

            renderer.drawRect(
                    searchBarRect.setColor(
                            new Color(
                                    255,
                                    255,
                                    255,
                                    Math.round(20.0F + searchFocusProgress * 25.0F))));
            String searchLabel = this.searchQuery.isEmpty() ? "Search blocks..." : this.searchQuery;
            TextDrawRequest searchText =
                    TextDrawRequest.obtain()
                            .setText(searchLabel)
                            .setPosition(this.getX() + 8.0F, bodyY + 1.0F)
                            .centerVertically(14.0F)
                            .setFont(labelFont.getMsdfFont())
                            .setFontSize(labelFont.getSize())
                            .enableShadow();
            int searchTextColor;
            if (this.searchQuery.isEmpty()) {

                searchTextColor = -7829368;
            } else {
                searchTextColor = Color.white.getRGB();
            }

            renderer.drawText(searchText.setColor(searchTextColor));
            if (this.searchFocused) {
                blinkTick = System.currentTimeMillis();
                blinkTick /= 450L;
                if (blinkTick % 2L == 0L) {
                    String queryBeforeCaret = this.searchQuery.isEmpty() ? "" : this.searchQuery;
                    float caretX =
                            this.getX() + 8.0F + labelFont.getStringWidth(queryBeforeCaret) + 1.0F;
                    RoundedRectShape caretRect =
                            RoundedRectShape.obtain()
                                    .setBounds(caretX, bodyY + 4.0F, 1.0F, 8.0F)
                                    .setRadius(0.5F);

                    renderer.drawRect(
                            caretRect.setColor(
                                    new Color(
                                            255,
                                            255,
                                            255,
                                            Math.round(150.0F + searchFocusProgress * 105.0F))));
                }
            }

            float listY = bodyY + 17.0F;
            float listHeight = Math.clamp(expandedHeight - 17.0F, 1.0F, 145.0F);
            renderer.beginScissor(this.getX() + 3.0F, listY, this.getWidth() - 6.0F, listHeight);
            RoundedRectShape listRect =
                    RoundedRectShape.obtain()
                            .setBounds(this.getX() + 4.0F, listY, this.getWidth() - 8.0F, 145.0F)
                            .setRadius(3.0F);

            renderer.drawRect(listRect.setColor(new Color(30, 30, 30)));
            int blockCount = filteredBlocks.size();
            int nameColor;

            int maxScroll = Math.max(0, blockCount - 10);
            this.scrollOffset = Math.clamp(this.scrollOffset, 0, maxScroll);
            if (this.draggingScrollbar && maxScroll > 0) {
                this.setScrollFromScrollbar(mouseY, listY, filteredBlocks.size(), maxScroll);
            }

            for (int rowIndex = this.scrollOffset; rowIndex < filteredBlocks.size(); rowIndex++) {
                float rowY = listY + (rowIndex - this.scrollOffset) * 14.0F;
                if (rowY > listY + 145.0F) {
                    break;
                }

                Block block2 = (Block) filteredBlocks.get(rowIndex);
                boolean selected = this.blockListSetting.isSelected(block2);
                boolean rowHovered =
                        RenderUtil.isInsideRect(
                                mouseX,
                                mouseY,
                                this.getX() + 4.0F,
                                rowY,
                                this.getWidth() - 8.0F,
                                14.0F);
                if (selected) {
                    Color color =
                            ColorUtil.animatedBlend(
                                    rowY,
                                    Astralis.getInstance().getPrimaryColor(),
                                    Astralis.getInstance().getSecondaryColor());
                    Color color2 =
                            ColorUtil.animatedBlend(
                                    rowY + 14.0F,
                                    Astralis.getInstance().getPrimaryColor(),
                                    Astralis.getInstance().getSecondaryColor());
                    RoundedRectShape selectedRowRect =
                            RoundedRectShape.obtain()
                                    .setBounds(
                                            this.getX() + 4.0F,
                                            rowY,
                                            this.getWidth() - 8.0F,
                                            14.0F);
                    red = color.getRed();
                    int green = color.getGreen();
                    int blue = color.getBlue();

                    renderer.drawRect(selectedRowRect.setColor(new Color(red, green, blue, 28)));
                    renderer.drawRect(
                            RoundedRectShape.obtain()
                                    .setBounds(this.getX() + 4.0F, rowY, 2.0F, 14.0F)
                                    .setPaint(
                                            ShapePaint.obtain()
                                                    .setVerticalGradient(
                                                            color.getRGB(), color2.getRGB())));
                }

                if (rowHovered) {
                    RoundedRectShape hoverRect =
                            RoundedRectShape.obtain()
                                    .setBounds(
                                            this.getX() + 4.0F,
                                            rowY,
                                            this.getWidth() - 8.0F,
                                            14.0F);

                    renderer.drawRect(hoverRect.setColor(new Color(255, 255, 255, 15)));
                }

                if (block2 == this.lastToggledBlock && toggleFlashProgress > 0.01F) {
                    RoundedRectShape flashRect =
                            RoundedRectShape.obtain()
                                    .setBounds(
                                            this.getX() + 4.0F,
                                            rowY,
                                            this.getWidth() - 8.0F,
                                            14.0F);

                    renderer.drawRect(
                            flashRect.setColor(
                                    new Color(
                                            255,
                                            255,
                                            255,
                                            Math.round(55.0F * toggleFlashProgress))));
                }

                if (selected) {
                    renderer.drawText(
                            TextDrawRequest.obtain()
                                    .setText("✓")
                                    .setPosition(this.getX() + this.getWidth() - 33.0F, rowY)
                                    .centerVertically(14.0F)
                                    .setFont(labelFont.getMsdfFont())
                                    .setFontSize(labelFont.getSize())
                                    .enableShadow()
                                    .setColor(Color.white.getRGB()));
                }

                String blockName = this.blockListSetting.getDisplayName(block2);
                float nameX = this.getX() + 10.0F;
                float nameAreaWidth = Math.max(10.0F, this.getWidth() - 45.0F);
                float nameTextWidth = labelFont.getStringWidth(blockName);
                float marqueeOffset =
                        nameTextWidth > nameAreaWidth
                                ? this.getMarqueeOffset(
                                        nameTextWidth - nameAreaWidth,
                                        this.blockListSetting.getBlockId(block2))
                                : 0.0F;
                renderer.beginScissor(nameX, rowY, nameAreaWidth, 14.0F);
                TextDrawRequest nameText =
                        TextDrawRequest.obtain()
                                .setText(blockName)
                                .setPosition(nameX - marqueeOffset, rowY)
                                .centerVertically(14.0F)
                                .setFont(labelFont.getMsdfFont())
                                .setFontSize(labelFont.getSize())
                                .enableShadow();
                if (selected) {
                    nameColor = Color.white.getRGB();
                } else {

                    nameColor = new Color(180, 180, 180).getRGB();
                }

                renderer.drawText(nameText.setColor(nameColor));
                renderer.endScissor();
            }

            if (maxScroll > 0) {
                float scrollbarX = this.getX() + this.getWidth() - 8.0F;
                float thumbHeight = Math.max(18.0F, 19321.0F / (filteredBlocks.size() * 14.0F));
                float thumbY =
                        listY + 3.0F + (139.0F - thumbHeight) * this.scrollOffset / maxScroll;
                RoundedRectShape scrollTrackRect =
                        RoundedRectShape.obtain()
                                .setBounds(scrollbarX, listY + 3.0F, 4.0F, 139.0F)
                                .setRadius(2.0F);
                red = Astralis.getInstance().getPrimaryColor().getRed();
                int primaryGreen = Astralis.getInstance().getPrimaryColor().getGreen();
                int primaryBlue = Astralis.getInstance().getPrimaryColor().getBlue();

                renderer.drawRect(
                        scrollTrackRect.setColor(new Color(red, primaryGreen, primaryBlue, 35)));
                Color color3 =
                        ColorUtil.animatedBlend(
                                thumbY,
                                Astralis.getInstance().getPrimaryColor(),
                                Astralis.getInstance().getSecondaryColor());
                Color color4 =
                        ColorUtil.animatedBlend(
                                thumbY + thumbHeight,
                                Astralis.getInstance().getPrimaryColor(),
                                Astralis.getInstance().getSecondaryColor());
                RoundedRectShape thumbRect =
                        RoundedRectShape.obtain()
                                .setBounds(scrollbarX, thumbY, 4.0F, thumbHeight)
                                .setRadius(2.0F);
                ShapePaint thumbPaint = ShapePaint.obtain();
                int startRed = color3.getRed();
                int startGreen = color3.getGreen();
                int startBlue = color3.getBlue();

                int startRgb = new Color(startRed, startGreen, startBlue, 220).getRGB();
                int endRed = color4.getRed();
                int endGreen = color4.getGreen();
                int endBlue = color4.getBlue();

                renderer.drawRect(
                        thumbRect.setPaint(
                                thumbPaint.setVerticalGradient(
                                        startRgb,
                                        new Color(endRed, endGreen, endBlue, 220).getRGB())));
            }

            renderer.endScissor();
            renderer.endScissor();
            if (this.draggingScrollbar
                    && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) != 1) {
                this.draggingScrollbar = false;
            }
        }
    }

    private float updateExpandedHeight() {
        this.expandAnimation.animateTo(this.expanded ? 1.0 : 0.0);
        float expandedHeight = (float) (this.expandAnimation.getValue() * 162.0);
        this.setHeight(15.0F + expandedHeight);
        return expandedHeight;
    }

    public void renderBlockIcons(GuiGraphicsExtractor guiGraphicsExtractor) {
        List filteredBlocks = this.blockListSetting.filtered(this.searchQuery);
        float expandedHeight = this.updateExpandedHeight();
        if (!(expandedHeight < 0.1F)) {
            float bodyY = this.getY() + 15.0F;
            float listY = bodyY + 17.0F;
            float listHeight = Math.clamp(expandedHeight - 17.0F, 1.0F, 145.0F);
            int blockCount = filteredBlocks.size();

            int maxScroll = Math.max(0, blockCount - 10);
            this.scrollOffset = Math.clamp(this.scrollOffset, 0, maxScroll);
            int scissorLeft = Math.round(this.getX() + 3.0F);
            int scissorTop = Math.round(listY);
            int scissorRight = Math.round(this.getX() + this.getWidth() - 3.0F);
            int scissorBottom = Math.round(listY + listHeight);
            guiGraphicsExtractor.enableScissor(
                    scissorLeft, scissorTop, scissorRight, scissorBottom);

            for (int rowIndex = this.scrollOffset; rowIndex < filteredBlocks.size(); rowIndex++) {
                float rowY = listY + (rowIndex - this.scrollOffset) * 14.0F;
                if (rowY >= listY + listHeight) {
                    break;
                }

                Item item2 = ((Block) filteredBlocks.get(rowIndex)).asItem();
                if (item2 != Items.AIR) {
                    float iconX = this.getX() + this.getWidth() - 21.0F;
                    guiGraphicsExtractor.pose().pushMatrix();
                    guiGraphicsExtractor.pose().translate(iconX, rowY + 1.0F);
                    guiGraphicsExtractor.pose().scale(0.625F, 0.625F);
                    guiGraphicsExtractor.fakeItem(
                            new ItemStack(item2),
                            0,
                            0,
                            ((Block) filteredBlocks.get(rowIndex)).hashCode());
                    guiGraphicsExtractor.pose().popMatrix();
                }
            }

            guiGraphicsExtractor.disableScissor();
        }
    }

    private float getMarqueeOffset(float overflowWidth, String blockId) {
        long marqueeTime;
        long blockIdHash;

        marqueeTime = System.currentTimeMillis();
        blockIdHash = blockId.hashCode();

        marqueeTime += blockIdHash * 17L;

        long cycleMillis = Math.floorMod(marqueeTime, 4000L);

        if (cycleMillis < 650L) {
            return 0.0F;
        }

        if (cycleMillis < 2350L) {

            return overflowWidth * (float) (cycleMillis - 650L) / 1700.0F;
        }

        if (cycleMillis < 3000L) {
            return overflowWidth;
        }

        return overflowWidth * (1.0F - (float) (cycleMillis - 3000L) / 1000.0F);
    }

    @Override
    public boolean isSettled() {
        return !this.expanded
                && !this.searchFocused
                && !this.draggingScrollbar
                && this.expandAnimation.isFinished()
                && this.toggleFlashAnimation.isFinished()
                && this.searchFocusAnimation.isFinished();
    }

    @Override
    public void click(MouseButtonEvent mouseButtonEvent) {
        long nowMillis;
        TextInputUtil.clearSelectAll(this);
        if (RenderUtil.isInsideRect(
                mouseButtonEvent.x(),
                mouseButtonEvent.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                15.0F)) {
            if (mouseButtonEvent.button() == 1) {
                this.expanded = !this.expanded;
            }

            this.searchFocused = false;
        } else if (this.expanded) {
            float bodyY = this.getY() + 15.0F;
            if (RenderUtil.isInsideRect(
                    mouseButtonEvent.x(),
                    mouseButtonEvent.y(),
                    this.getX() + 4.0F,
                    bodyY,
                    this.getWidth() - 8.0F,
                    17.0F)) {
                this.searchFocused = mouseButtonEvent.button() == 0;
            } else if (mouseButtonEvent.button() == 0) {
                List filteredBlocks = this.blockListSetting.filtered(this.searchQuery);
                float listY = bodyY + 17.0F;
                int blockCount = filteredBlocks.size();

                int maxScroll = Math.max(0, blockCount - 10);
                float scrollbarX = this.getX() + this.getWidth() - 10.0F;
                if (maxScroll > 0
                        && RenderUtil.isInsideRect(
                                mouseButtonEvent.x(),
                                mouseButtonEvent.y(),
                                scrollbarX,
                                listY,
                                8.0F,
                                145.0F)) {
                    this.draggingScrollbar = true;
                    this.setScrollFromScrollbar(
                            mouseButtonEvent.y(), listY, filteredBlocks.size(), maxScroll);
                } else {
                    if (RenderUtil.isInsideRect(
                            mouseButtonEvent.x(),
                            mouseButtonEvent.y(),
                            this.getX() + 3.0F,
                            listY,
                            this.getWidth() - 6.0F,
                            145.0F)) {
                        int clickedIndex =
                                this.scrollOffset + (int) ((mouseButtonEvent.y() - listY) / 14.0);
                        if (clickedIndex >= 0 && clickedIndex < filteredBlocks.size()) {
                            this.blockListSetting.toggleBlock(
                                    (Block) filteredBlocks.get(clickedIndex));
                        }

                        if (clickedIndex >= 0 && clickedIndex < filteredBlocks.size()) {
                            this.lastToggledBlock = (Block) filteredBlocks.get(clickedIndex);
                            nowMillis = System.currentTimeMillis();

                            this.toggleFlashEndTime = nowMillis + 180L;
                        }
                    }
                }
            }
        }
    }

    private void setScrollFromScrollbar(double mouseY, float listY, int itemCount, int maxScroll) {
        float thumbHeight = Math.max(18.0F, 19321.0F / (itemCount * 14.0F));
        float thumbOffset =
                Math.clamp(
                        (float) mouseY - listY - 3.0F - thumbHeight / 2.0F,
                        0.0F,
                        139.0F - thumbHeight);
        this.scrollOffset =
                Math.clamp(
                        Math.round(thumbOffset / (139.0F - thumbHeight) * maxScroll), 0, maxScroll);
    }

    @Override
    public void mouseDragged(float mouseX, float mouseY) {
        if (this.draggingScrollbar) {
            List filteredBlocks = this.blockListSetting.filtered(this.searchQuery);
            int blockCount = filteredBlocks.size();

            int maxScroll = Math.max(0, blockCount - 10);
            if (maxScroll > 0) {
                this.setScrollFromScrollbar(
                        mouseY, this.getY() + 15.0F + 17.0F, filteredBlocks.size(), maxScroll);
            }
        }
    }

    @Override
    public void release(MouseButtonEvent mouseButtonEvent) {
        this.draggingScrollbar = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        long currentScroll;
        if (this.expanded
                && RenderUtil.isInsideRect(
                        (float) mouseX,
                        (float) mouseY,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight())) {
            int blockCount = this.blockListSetting.filtered(this.searchQuery).size();

            int maxScroll = Math.max(0, blockCount - 10);
            currentScroll = this.scrollOffset;
            long scrollDirection = (int) Math.signum(scrollY);

            this.scrollOffset = Math.clamp(currentScroll - scrollDirection * 3L, 0, maxScroll);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.searchFocused) {
            return false;
        }

        String currentQuery = this.searchQuery;
        Consumer<String> querySetter =
                newQuery -> {
                    this.searchQuery = newQuery;
                    this.scrollOffset = 0;
                };

        if (TextInputUtil.handleControlShortcut(
                this, event, currentQuery, querySetter, 64, TextInputUtil::isPrintableCodePoint)) {
            return true;
        }

        if (!event.isEscape()) {
            int keyCode = event.key();
            if (keyCode != 257) {
                int pressedKeyCode = event.key();
                if (pressedKeyCode == 259) {
                    TextInputUtil.handleBackspace(
                            this,
                            this.searchQuery,
                            updatedQuery -> {
                                this.searchQuery = updatedQuery;
                                this.scrollOffset = 0;
                            });
                    return true;
                }

                return true;
            }
        }

        this.searchFocused = false;
        TextInputUtil.clearSelectAll(this);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.searchFocused) {
            return false;
        }

        String currentQuery = this.searchQuery;
        String typedText = characterEvent.codepointAsString();
        Consumer<String> querySetter =
                newQuery -> {
                    this.searchQuery = newQuery;
                    this.scrollOffset = 0;
                };

        TextInputUtil.insertText(
                this,
                currentQuery,
                typedText,
                querySetter,
                64,
                TextInputUtil::isPrintableCodePoint);
        return true;
    }
}
