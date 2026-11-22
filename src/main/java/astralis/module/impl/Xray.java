package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.LightmapExtractEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BlockListSetting;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;

import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import pub.astralis.client.module.core.Category;

import java.util.Set;

public class Xray extends Module {
    private static volatile Xray instance;
    private volatile boolean translucentActive;
    private float lastOpacity;
    private boolean lastExposedOnly;
    private static Direction[] allDirections;
    private final NumberSetting opacity;
    private final BooleanSetting exposedOnly;
    private final BlockListSetting blocks;

    public Xray() {

        super(MODULE_ID, "", "", Category.Misc);
        this.lastOpacity = Float.NaN;

        this.opacity = new NumberSetting(OPACITY_SETTING_ID, 40.0F, 0.0F, 100.0F, 1.0F);

        this.exposedOnly = new BooleanSetting(EXPOSED_ONLY_SETTING_ID, false);

        this.blocks =
                new BlockListSetting("663a6aac94ac9279dca177ea31468466", createDefaultBlocks());
        instance = this;
        this.addSettings(this.opacity, this.exposedOnly, this.blocks);
        this.opacity.addListeners(
                (previousValue, newValue) -> {
                    this.translucentActive = this.isEnabled() && newValue < 100.0F;
                    this.lastOpacity = newValue;
                    this.reloadChunks();
                });
        this.exposedOnly.addListeners(
                (previousValue, newValue) -> {
                    if (this.isEnabled()) {
                        this.reloadChunks();
                    }
                });
        this.blocks.addListeners(
                (previousValue, newValue) -> {
                    if (this.isEnabled()) {
                        this.reloadChunks();
                    }
                });
    }

    public boolean isSelectedBlock(BlockState blockState) {
        return this.blocks.isSelected(blockState.getBlock());
    }

    public boolean isTranslucentActive() {
        return this.translucentActive;
    }

    public static Xray getInstance() {
        return instance;
    }

    public boolean isTranslucencyInactive() {
        float currentOpacity = this.opacity.getValue();
        return !this.isEnabled() || !(currentOpacity > 0.0F) || !(currentOpacity < 100.0F);
    }

    public BooleanSetting getExposedOnly() {
        return this.exposedOnly;
    }

    private static Set<Block> createDefaultBlocks() {

        Block[] defaultBlocks =
                new Block[] {
                    Blocks.COAL_ORE,
                    Blocks.DEEPSLATE_COAL_ORE,
                    Blocks.IRON_ORE,
                    Blocks.DEEPSLATE_IRON_ORE,
                    Blocks.GOLD_ORE,
                    Blocks.DEEPSLATE_GOLD_ORE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        defaultBlocks[6] = Blocks.LAPIS_ORE;

        defaultBlocks[7] = Blocks.DEEPSLATE_LAPIS_ORE;

        defaultBlocks[8] = Blocks.REDSTONE_ORE;

        defaultBlocks[9] = Blocks.DEEPSLATE_REDSTONE_ORE;

        defaultBlocks[10] = Blocks.DIAMOND_ORE;

        defaultBlocks[11] = Blocks.DEEPSLATE_DIAMOND_ORE;

        defaultBlocks[12] = Blocks.EMERALD_ORE;

        defaultBlocks[13] = Blocks.DEEPSLATE_EMERALD_ORE;

        defaultBlocks[14] = Blocks.COPPER_ORE;

        defaultBlocks[15] = Blocks.DEEPSLATE_COPPER_ORE;

        defaultBlocks[16] = Blocks.NETHER_GOLD_ORE;

        defaultBlocks[17] = Blocks.NETHER_QUARTZ_ORE;

        defaultBlocks[18] = Blocks.ANCIENT_DEBRIS;
        return Set.of(defaultBlocks);
    }

    public boolean shouldHideBlockEntity(BlockEntity blockEntity) {
        if (!this.isEnabled()) {
            return false;
        } else {
            return !this.isSelectedBlock(blockEntity.getBlockState())
                    ? true
                    : this.exposedOnly.getValue() && !this.isExposed(blockEntity.getBlockPos());
        }
    }

    private void reloadChunks() {
        if (mc.level != null) {
            mc.levelExtractor.allChanged();
        }
    }

    public NumberSetting getOpacity() {
        return this.opacity;
    }

    public BlockListSetting getBlocks() {
        return this.blocks;
    }

    public boolean isExposed(BlockPos blockPos) {
        return this.hasAirNeighbor(mc.level, blockPos);
    }

    @Override
    public void onDisable() {
        this.translucentActive = false;
        this.reloadChunks();
    }

    public boolean shouldHideBlock(BlockState blockState, BlockPos blockPos) {
        if (!this.isEnabled()) {
            return false;
        } else {
            return this.opacity.getValue() <= 0.0F && !this.isSelectedBlock(blockState)
                    ? true
                    : this.exposedOnly.getValue()
                            && this.isSelectedBlock(blockState)
                            && !this.isExposed(blockPos);
        }
    }

    public int getOpacityAlpha() {
        long scaledAlpha;
        float opacityValue = this.opacity.getValue();
        scaledAlpha = Math.round(opacityValue * 2.55F);

        return Math.clamp(scaledAlpha, 0, 255);
    }

    @EventTarget
    public void onLightmapExtract(LightmapExtractEvent event) {
        LightmapRenderState lightmapRenderState = event.getRenderState();
        lightmapRenderState.brightness = 10.0F;
        lightmapRenderState.needsUpdate = true;
    }

    @Override
    public void onEnable() {
        this.lastOpacity = this.opacity.getValue();
        this.lastExposedOnly = this.exposedOnly.getValue();
        this.translucentActive = this.opacity.getValue() < 100.0F;
        this.reloadChunks();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST && this.isEnabled()) {
            float currentOpacity = this.opacity.getValue();
            if (Float.compare(currentOpacity, this.lastOpacity) != 0) {
                this.lastOpacity = currentOpacity;
                this.reloadChunks();
            }

            boolean currentExposedOnly = this.exposedOnly.getValue();
            if (currentExposedOnly != this.lastExposedOnly) {
                this.lastExposedOnly = currentExposedOnly;
                this.reloadChunks();
            }
        }
    }

    private boolean hasAirNeighbor(BlockGetter blockGetter, BlockPos blockPos) {
        if (blockGetter == null) {
            return true;
        }

        for (Direction direction : allDirections) {
            if (blockGetter.getBlockState(blockPos.relative(direction)).isAir()) {
                return true;
            }
        }

        return false;
    }

    static {
        allDirections = Direction.values();
    }

    private static final String MODULE_ID = "253cdab6299cc755e1bdacfd105ee8c7";
    private static final String OPACITY_SETTING_ID = "e65efe28bf100fd2b8060e20e39e8e6d";
    private static final String EXPOSED_ONLY_SETTING_ID = "792ae3364921e0347d14398b8c6bab8a";
}
