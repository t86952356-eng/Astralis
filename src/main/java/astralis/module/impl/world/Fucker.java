package astralis.module.impl.world;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.ContinueAttackEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.RenderHeldItemEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.event.impl.TickEvent;
import astralis.mixin.MultiPlayerGameModeAccessor;
import astralis.module.Module;
import astralis.module.impl.Effects;
import astralis.module.impl.FastMine;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.world.fucker.BedTarget;
import astralis.rotation.Rotation;
import astralis.rotation.RotationManager;
import astralis.rotation.RotationPriority;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.ui.animation.Animation;
import astralis.util.BreakUtil;
import astralis.util.Easing;
import astralis.util.HypixelModApi;
import astralis.util.InventoryUtil;
import astralis.util.OwnBedTracker;
import astralis.util.PacketUtil;
import astralis.util.RotationUtil;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Selects nearby beds, removes their cover, and drives normal or packet-based breaking. */
public class Fucker extends Module {
    private static final int BREAK_CONFIRMATION_TICKS = 4;
    private static final int STUCK_TIMEOUT_TICKS = 10;
    private static final int GHOST_AIR_TICKS = 3;
    private static final int GROUNDED_AIR_TICKS = 2;
    private static final int FADE_OUT_DURATION_MS = 400;
    private final ModeSetting swingMode;
    private final ModeSetting rotationMode;
    private final ModeSetting switchMode;
    private final NumberSetting breakRadius;
    private final BooleanSetting renderTarget;
    private final BooleanSetting ignoreOwnBed;
    private final BooleanSetting showProgressHud;
    private final ColorSetting targetColor;
    private final BooleanSetting pauseForKillAura;
    private final BooleanSetting onlyWithTool;
    private final BooleanSetting watchdogInstant;
    private final BooleanSetting ghostBlockDetection;
    private final Animation boxAnimation;
    private final Animation hudFadeAnimation;
    private final Animation hudProgressAnimation;
    // An instant-mode target may break cover first, then overlap breaking the bed beneath it.
    private BlockPos followUpPos;
    private BlockPos startedBreakPos; // The START packet still awaiting a STOP or ABORT.
    private BlockPos bedPos;
    private BlockPos coverPos;
    private BlockPos fallbackBedPos;
    private AABB renderBox;
    private AABB fadingBox;
    private BlockPos renderPos;
    private long fadeStartTime;
    private long fadeDuration;
    private float fadeStartAlpha;
    private float simulatedProgress;
    private float followUpProgress;
    private int breakTicks;
    private int followUpBreakTicks;
    private int coverAirTicks;
    private int coverStuckTicks;
    private int unconfirmedTicks;
    private int confirmedAirTicks;
    private boolean followUpStarted;
    private boolean stopSent; // Final STOP sent; wait for the block to become and remain air.
    private boolean instantMode;
    private boolean otherActionThisTick;
    private boolean pausedForKillAura;
    private boolean airborneDuringBreak;
    private boolean ghostFallback;
    private boolean lastGhostDetection;
    public BlockPos currentTarget;
    private int originalSlot;

    public Fucker() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting swingSettingBuilder = new ModeSetting(SWING_MODE_SETTING_ID);
        String[] swingModes = new String[2];

        swingModes[0] = "Normal";

        swingModes[1] = "Silent";
        ModeSetting configuredSwingModes = swingSettingBuilder.addModes(swingModes);
        this.swingMode = configuredSwingModes.setDefault("Normal");
        ModeSetting rotationSettingBuilder = new ModeSetting(ROTATION_MODE_SETTING_ID);
        String[] rotationModes = new String[2];
        rotationModes[0] = "Normal";
        rotationModes[1] = "Snap";
        ModeSetting configuredRotationModes = rotationSettingBuilder.addModes(rotationModes);

        this.rotationMode = configuredRotationModes.setDefault("Snap");
        ModeSetting switchSettingBuilder = new ModeSetting(SWITCH_MODE_SETTING_ID);
        String[] switchModes = new String[2];
        switchModes[0] = "Normal";
        switchModes[1] = "Silent";
        ModeSetting configuredSwitchModes = switchSettingBuilder.addModes(switchModes);

        this.switchMode = configuredSwitchModes.setDefault("Normal");
        this.breakRadius = new NumberSetting(BREAK_RADIUS_SETTING_ID, 4.5F, 1.0F, 5.0F, 0.5F);
        this.renderTarget = new BooleanSetting(RENDER_TARGET_SETTING_ID, true);
        this.ignoreOwnBed = new BooleanSetting(IGNORE_OWN_BED_SETTING_ID, true);
        this.showProgressHud = new BooleanSetting(HUD_SETTING_ID, true);

        this.targetColor = new ColorSetting(TARGET_COLOR_SETTING_ID, new Color(255, 80, 80, 180));
        this.pauseForKillAura = new BooleanSetting(PAUSE_FOR_KILL_AURA_SETTING_ID, true);
        this.onlyWithTool = new BooleanSetting(ONLY_WITH_TOOL_SETTING_ID, false);
        this.watchdogInstant = new BooleanSetting(WATCHDOG_INSTANT_SETTING_ID, false);
        this.ghostBlockDetection = new BooleanSetting(GHOST_BLOCK_DETECTION_SETTING_ID, true);

        this.boxAnimation = new Animation(Easing.EASE_OUT_CUBIC, (long) FADE_OUT_DURATION_MS);

        this.hudFadeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);

        this.hudProgressAnimation = new Animation(Easing.EASE_OUT_QUINT, 210L);
        this.currentTarget = null;
        this.originalSlot = -1;
        this.targetColor.hideIf(
                () -> {
                    return !this.renderTarget.getValue();
                });

        SettingGroup checksGroup =
                new SettingGroup(CHECKS_GROUP_ID)
                        .addValues(
                                this.pauseForKillAura, this.onlyWithTool, this.ghostBlockDetection);
        Setting[] settings =
                new Setting[] {
                    this.swingMode,
                    this.rotationMode,
                    this.switchMode,
                    this.breakRadius,
                    this.ignoreOwnBed,
                    this.renderTarget,
                    null,
                    null,
                    null,
                    null
                };
        settings[6] = this.targetColor;
        settings[7] = this.watchdogInstant;
        settings[8] = this.showProgressHud;
        settings[9] = checksGroup;
        this.addSettings(settings);
    }

    // Pick each bed's fastest reachable cover, then choose the nearest of those covers.
    private BedTarget findNearestCoverTarget(List<BlockPos> bedsInRange) {
        double nearestDistance = Double.MAX_VALUE;
        BedTarget nearestTarget = null;

        for (BlockPos bedCandidate : bedsInRange) {
            BlockPos[] bedParts = this.getBedParts(bedCandidate);
            BlockPos coverCandidate = this.findCoverBlock(bedParts[0], bedParts[1], null);
            if (coverCandidate != null) {
                double coverDistance =
                        mc.player.position().distanceTo(Vec3.atCenterOf(coverCandidate));
                if (coverDistance < nearestDistance) {
                    nearestDistance = coverDistance;
                    nearestTarget =
                            new BedTarget(
                                    coverCandidate,
                                    this.isWatchdogInstantActive() ? bedCandidate : null,
                                    bedCandidate);
                }
            }
        }

        return nearestTarget;
    }

    /** Returns the KillAura pause toggle. */
    public BooleanSetting getPauseForKillAura() {
        return this.pauseForKillAura;
    }

    private void sendBreakPacket(Action action, BlockPos pos, Direction face) {
        if (action == Action.STOP_DESTROY_BLOCK) {
            this.playBreakSound(pos);
        }

        PacketUtil.sendPrediction(
                predictionSequence ->
                        new ServerboundPlayerActionPacket(action, pos, face, predictionSequence));
    }

    private float getBreakProgress() {
        if (this.instantMode) {
            return this.simulatedProgress;
        } else {
            return mc.gameMode instanceof MultiPlayerGameModeAccessor breakingProgressAccessor
                    ? breakingProgressAccessor.getCurrentBreakingProgress()
                    : 0.0F;
        }
    }

    private void tickBreaking(boolean resumedFromKillAura) {
        if (this.followUpPos == null || !mc.level.getBlockState(this.currentTarget).isAir()) {
            InventoryUtil.switchToBestTool(this.currentTarget);
        }

        // Normal rotations track the target each tick; a KillAura resume also reacquires it.
        applyRotation:
        {
            if (!resumedFromKillAura) {
                String rotationName = this.rotationMode.getValue().getName();

                if (!rotationName.equals("Normal")) {
                    break applyRotation;
                }
            }

            if (!this.instantMode || !this.stopSent) {
                this.rotateTo(
                        this.instantMode && this.followUpPos != null && this.followUpStarted
                                ? this.followUpPos
                                : this.currentTarget);
            }
        }

        if (this.instantMode) {
            this.tickInstantBreak();
        } else {
            mc.gameMode.continueDestroyBlock(
                    this.currentTarget, this.getBreakFace(this.currentTarget));
            this.swingHand();
            FastMine fastMine = Astralis.getInstance().getModuleManager().getModule(FastMine.class);
            if (fastMine.isEnabled()
                    && mc.gameMode instanceof MultiPlayerGameModeAccessor breakingProgressAccessor
                    && breakingProgressAccessor.getCurrentBreakingProgress()
                            >= 1.0F / fastMine.getSpeedSetting().getValue()) {
                breakingProgressAccessor.setCurrentBreakingProgress(1.0F);
            }
        }
    }

    @EventTarget
    public void onRenderHeldItem(RenderHeldItemEvent event) {

        if (this.switchMode.is("Silent")
                && event.getHand() == InteractionHand.MAIN_HAND
                && this.originalSlot != -1
                && this.currentTarget != null) {
            event.setStack(mc.player.getInventory().getItem(this.originalSlot));
        }
    }

    private void releaseRotation() {

        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager != null) {
            Rotation currentRotation = rotationManager.getCurrent();
            if (currentRotation != null && "Fucker".equals(currentRotation.getTag())) {
                rotationManager.resetImmediately();
            }
        }
    }

    @Override
    public String getSuffix() {
        return this.rotationMode.getValue().getName();
    }

    private void stopBreaking() {
        if (this.instantMode) {
            if (this.startedBreakPos != null && mc.player != null && mc.level != null) {
                this.sendBreakPacket(
                        Action.ABORT_DESTROY_BLOCK, this.startedBreakPos, Direction.DOWN);
            }
        } else if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }

        this.releaseRotation();
        this.resetBreakState();
        this.instantMode = false;
        this.currentTarget = null;
        this.ghostFallback = false;
        this.fallbackBedPos = null;
        if (this.originalSlot != -1) {
            InventoryUtil.setSlot(this.originalSlot);
            this.originalSlot = -1;
        }
    }

    // Either bed half is exposed when any face other than its underside borders air.
    private boolean isBedExposed(BlockPos bedCandidate) {

        for (BlockPos bedPartPos : this.getBedParts(bedCandidate)) {
            for (Direction exposedFace : Direction.values()) {
                if (exposedFace != Direction.DOWN
                        && mc.level.getBlockState(bedPartPos.relative(exposedFace)).isAir()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void drawTargetBox(
            Render3DEvent event, AABB targetBox, float visibility, float breakProgress) {
        if (!(visibility <= 0.01F)) {
            float shrinkFraction = Math.min(1.0F, (1.0F - visibility) / 0.7F);
            float alphaFraction = visibility < 0.3F ? visibility / 0.3F : 1.0F;
            double minimumSize =
                    Math.min(
                            0.18,
                            Math.min(
                                    targetBox.getXsize(),
                                    Math.min(targetBox.getYsize(), targetBox.getZsize())));
            AABB animatedBox =
                    AABB.ofSize(
                            targetBox.getCenter(),
                            minimumSize
                                    + (targetBox.getXsize() - minimumSize)
                                            * (1.0F - shrinkFraction),
                            minimumSize
                                    + (targetBox.getYsize() - minimumSize)
                                            * (1.0F - shrinkFraction),
                            minimumSize
                                    + (targetBox.getZsize() - minimumSize)
                                            * (1.0F - shrinkFraction));
            float pulseAlpha =
                    0.8F
                            + 0.2F
                                    * (float)
                                            (Math.sin(System.currentTimeMillis() / 240.0) * 0.5
                                                    + 0.5);
            RenderUtil.drawBox(
                    event.getPoseStack(),
                    animatedBox,
                    ColorUtil.withAlphaFactor(
                            this.targetColor.getValue(),
                            alphaFraction * (0.55F + 0.45F * breakProgress) * pulseAlpha));
        }
    }

    @EventTarget
    public void onContinueAttack(ContinueAttackEvent event) {
        if (this.instantMode) {
            this.otherActionThisTick = true;
        }
    }

    private boolean isValidBed(BlockPos bedCandidate) {
        if (bedCandidate != null
                && mc.level.getBlockState(bedCandidate).getBlock() instanceof BedBlock) {
            BlockPos[] bedParts = this.getBedParts(bedCandidate);
            BlockState headState = mc.level.getBlockState(bedParts[0]);
            BlockState footState = mc.level.getBlockState(bedParts[1]);
            return headState.getBlock() instanceof BedBlock
                    && footState.getBlock() instanceof BedBlock
                    && headState.getValue(BedBlock.PART) == BedPart.HEAD
                    && footState.getValue(BedBlock.PART) == BedPart.FOOT
                    && headState.getValue(BedBlock.FACING) == footState.getValue(BedBlock.FACING);
        } else {
            return false;
        }
    }

    /** Returns the head and foot in that order, or repeats the position for a non-bed block. */
    private BlockPos[] getBedParts(BlockPos bedCandidate) {
        BlockState bedState = mc.level.getBlockState(bedCandidate);
        if (!(bedState.getBlock() instanceof BedBlock)) {
            return new BlockPos[] {bedCandidate, bedCandidate};
        }

        BedPart bedPart = bedState.getValue(BedBlock.PART);
        Direction bedFacing = bedState.getValue(BedBlock.FACING);
        BlockPos headPos =
                bedPart == BedPart.HEAD ? bedCandidate : bedCandidate.relative(bedFacing);
        BlockPos footPos =
                bedPart == BedPart.FOOT
                        ? bedCandidate
                        : bedCandidate.relative(bedFacing.getOpposite());
        return new BlockPos[] {headPos, footPos};
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.instantMode && event.getStage() == EventStage.SEND) {
            Packet outgoingPacket = event.getPacket();
            if (outgoingPacket instanceof ServerboundAttackPacket
                    || outgoingPacket instanceof ServerboundInteractPacket
                    || outgoingPacket instanceof ServerboundUseItemOnPacket
                    || outgoingPacket instanceof ServerboundUseItemPacket
                    || outgoingPacket instanceof ServerboundSwingPacket) {
                this.otherActionThisTick = true;
            } else if (outgoingPacket instanceof ServerboundPlayerActionPacket blockActionPacket) {
                Action blockAction = blockActionPacket.getAction();
                if ((blockAction == Action.START_DESTROY_BLOCK
                                || blockAction == Action.ABORT_DESTROY_BLOCK
                                || blockAction == Action.STOP_DESTROY_BLOCK)
                        && !blockActionPacket.getPos().equals(this.currentTarget)
                        && !blockActionPacket.getPos().equals(this.followUpPos)) {
                    this.otherActionThisTick = true;
                }
            }
        }
    }

    // Cancel the disputed packet break and retry the bed or another cover through normal breaking.
    private void handleGhostBlock(BlockPos bedCandidate, BlockPos suspectBlock) {
        if (bedCandidate != null
                && suspectBlock != null
                && !OwnBedTracker.isOwnBed(bedCandidate)
                && this.isValidBed(bedCandidate)) {
            if (this.startedBreakPos != null) {
                this.sendBreakPacket(
                        Action.ABORT_DESTROY_BLOCK, this.startedBreakPos, Direction.DOWN);
            }

            if (!suspectBlock.equals(this.startedBreakPos)) {
                this.sendBreakPacket(Action.ABORT_DESTROY_BLOCK, suspectBlock, Direction.DOWN);
            }

            BlockPos[] bedParts = this.getBedParts(bedCandidate);
            boolean exposed = this.isBedExposed(bedCandidate);
            BlockPos coverCandidate =
                    exposed ? null : this.findCoverBlock(bedParts[0], bedParts[1], suspectBlock);
            this.releaseRotation();
            this.ghostFallback = true;
            this.fallbackBedPos = bedCandidate;
            this.startTarget(
                    new BedTarget(
                            coverCandidate == null ? bedCandidate : coverCandidate,
                            null,
                            bedCandidate));
        } else {
            this.stopBreaking();
        }
    }

    private BlockPos findCoverBlock(BlockPos headPos, BlockPos footPos, BlockPos excludedCoverPos) {

        BlockPos[] candidates =
                new BlockPos[] {
                    headPos.above(),
                    headPos.north(),
                    headPos.south(),
                    headPos.east(),
                    headPos.west(),
                    footPos.above(),
                    null,
                    null,
                    null,
                    null
                };

        candidates[6] = footPos.north();

        candidates[7] = footPos.south();

        candidates[8] = footPos.east();

        candidates[9] = footPos.west();
        BlockPos[] coverCandidates = candidates;
        float fastestBreakTicks = Float.MAX_VALUE;
        ArrayList<BlockPos> fastestCovers = new ArrayList<>();

        // Keep covers with the best break-time score; distance resolves the remaining candidates.
        for (BlockPos coverCandidate : coverCandidates) {
            BlockState coverState = mc.level.getBlockState(coverCandidate);
            if (!coverCandidate.equals(excludedCoverPos)
                    && !coverState.isAir()
                    && !(coverState.getBlock() instanceof BedBlock)
                    && this.isInRange(coverCandidate)) {
                float candidateBreakTicks =
                        BreakUtil.getBreakTicks(coverCandidate, mc.player.getMainHandItem());
                if (candidateBreakTicks < fastestBreakTicks) {
                    fastestBreakTicks = candidateBreakTicks;
                    fastestCovers.clear();
                    fastestCovers.add(coverCandidate);
                } else if (Math.abs(candidateBreakTicks - fastestBreakTicks) < 0.01F) {
                    fastestCovers.add(coverCandidate);
                }
            }
        }

        BlockPos nearestCover = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos tiedCover : fastestCovers) {
            double coverDistance = mc.player.position().distanceTo(Vec3.atCenterOf(tiedCover));
            if (coverDistance < nearestDistance) {
                nearestDistance = coverDistance;
                nearestCover = tiedCover;
            }
        }

        return nearestCover;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {

        float breakProgress =
                this.currentTarget == null ? 0.0F : Math.clamp(this.getBreakProgress(), 0.0F, 1.0F);
        boolean showHud =
                this.showProgressHud.getValue()
                        && this.currentTarget != null
                        && breakProgress > 0.0F;
        this.hudFadeAnimation.animateTo(showHud ? 1.0 : 0.0);
        this.hudProgressAnimation.animateTo(this.currentTarget == null ? 0.0 : breakProgress);
        float hudAlpha = Math.clamp((float) this.hudFadeAnimation.getValue(), 0.0F, 1.0F);
        if (!(hudAlpha <= 0.01F)) {
            float displayedProgress =
                    Math.clamp((float) this.hudProgressAnimation.getValue(), 0.0F, 1.0F);

            SizedFont font = FontManager.getThemedFont("Product Sans Medium", 7.0F);
            float barX = (mc.getWindow().getGuiScaledWidth() - 116.0F) * 0.5F;
            float barY =
                    mc.getWindow().getGuiScaledHeight() * 0.5F + 15.0F + (1.0F - hudAlpha) * 3.0F;
            float fillX = barX;
            float fillY = barY;
            float fillWidth = 116.0F * displayedProgress;
            Color primaryColor = Astralis.getInstance().getPrimaryColor();
            Color secondaryColor = Astralis.getInstance().getSecondaryColor();
            double elapsedSeconds = System.nanoTime() / 1.0E9;
            float gradientPhase = (float) (elapsedSeconds * 8.0);
            String progressText = Math.round(displayedProgress * 100.0F) + "%";
            if (Effects.isBedNukerBlur()) {
                event.getRenderEffects()
                        .blurRoundedRectangleWithAlpha(barX, barY, 116.0F, 12.0F, 6.0F, hudAlpha);
            }

            Render2DContext renderContext = event.getContext();
            RoundedRectShape backgroundShape =
                    RoundedRectShape.obtain()
                            .setBounds(barX, barY, 116.0F, 12.0F)
                            .setRadius(6.0F)
                            .setShadow(
                                    Effects.isBedNukerShadowActive(),
                                    Math.round(hudAlpha * 255.0F));

            renderContext.drawRect(
                    backgroundShape.setColor(new Color(20, 23, 30, Math.round(84.0F * hudAlpha))));
            if (fillWidth > 0.5F) {
                event.getContext()
                        .drawRect(
                                RoundedRectShape.obtain()
                                        .setBounds(fillX, fillY, fillWidth, 12.0F)
                                        .setRadius(Math.min(12.0F, fillWidth) * 0.5F)
                                        .setPaint(
                                                ShapePaint.obtain()
                                                        .setHorizontalGradient(
                                                                ColorUtil.withAlphaFactor(
                                                                                new Color(
                                                                                        ColorUtil
                                                                                                .waveBlend(
                                                                                                        fillX
                                                                                                                + gradientPhase,
                                                                                                        primaryColor
                                                                                                                .getRGB(),
                                                                                                        secondaryColor
                                                                                                                .getRGB()),
                                                                                        true),
                                                                                hudAlpha)
                                                                        .getRGB(),
                                                                ColorUtil.withAlphaFactor(
                                                                                new Color(
                                                                                        ColorUtil
                                                                                                .waveBlend(
                                                                                                        fillX
                                                                                                                + fillWidth
                                                                                                                + gradientPhase,
                                                                                                        primaryColor
                                                                                                                .getRGB(),
                                                                                                        secondaryColor
                                                                                                                .getRGB()),
                                                                                        true),
                                                                                hudAlpha)
                                                                        .getRGB())));
            }

            event.getContext()
                    .drawText(
                            TextDrawRequest.obtain()
                                    .setFont(font.getMsdfFont())
                                    .setText(progressText)
                                    .setPosition(
                                            barX
                                                    + (116.0F - font.getStringWidth(progressText))
                                                            * 0.5F,
                                            barY + (12.0F - font.getLineHeight()) * 0.5F)
                                    .setFontSize(font.getSize())
                                    .enableShadow()
                                    .setColor(
                                            ColorUtil.withAlphaFactor(Color.WHITE, hudAlpha)
                                                    .getRGB()));
        }
    }

    private void playHitSound(BlockPos pos) {
        SoundType soundType = mc.level.getBlockState(pos).getSoundType();
        mc.getSoundManager()
                .play(
                        new SimpleSoundInstance(
                                soundType.getHitSound(),
                                SoundSource.BLOCKS,
                                (soundType.getVolume() + 1.0F) / 8.0F,
                                soundType.getPitch() * 0.5F,
                                SoundInstance.createUnseededRandom(),
                                pos));
    }

    // Instant mode prefers a block directly above a bed, scoring break time before distance.
    private BedTarget findTopCoverTarget(List<BlockPos> bedsInRange) {
        float fastestBreakTicks = Float.MAX_VALUE;
        double nearestDistance = Double.MAX_VALUE;
        BedTarget bestTarget = null;

        for (BlockPos bedCandidate : bedsInRange) {
            BlockPos coverCandidate = bedCandidate.above();
            BlockState coverState = mc.level.getBlockState(coverCandidate);
            if (!coverState.isAir()
                    && !(coverState.getBlock() instanceof BedBlock)
                    && this.isInRange(coverCandidate)) {
                float candidateBreakTicks =
                        BreakUtil.getBreakTicks(coverCandidate, mc.player.getMainHandItem());
                double coverDistance =
                        mc.player.position().distanceTo(Vec3.atCenterOf(coverCandidate));
                if (candidateBreakTicks < fastestBreakTicks
                        || Math.abs(candidateBreakTicks - fastestBreakTicks) < 0.01F
                                && coverDistance < nearestDistance) {
                    fastestBreakTicks = candidateBreakTicks;
                    nearestDistance = coverDistance;
                    bestTarget = new BedTarget(coverCandidate, bedCandidate, bedCandidate);
                }
            }
        }

        return bestTarget;
    }

    public BooleanSetting getIgnoreOwnBed() {
        return this.ignoreOwnBed;
    }

    private void resetBreakState() {
        this.followUpPos = null;
        this.startedBreakPos = null;
        this.bedPos = null;
        this.coverPos = null;
        this.simulatedProgress = 0.0F;
        this.followUpProgress = 0.0F;
        this.breakTicks = 0;
        this.followUpBreakTicks = 0;
        this.coverAirTicks = 0;
        this.coverStuckTicks = 0;
        this.unconfirmedTicks = 0;
        this.confirmedAirTicks = 0;
        this.followUpStarted = false;
        this.stopSent = false;
        this.pausedForKillAura = false;
        this.airborneDuringBreak = false;
    }

    private void swingHand() {
        String selectedSwingMode = this.swingMode.getValue().getName();
        switch (selectedSwingMode) {
            case "Normal":
                mc.player.swing(InteractionHand.MAIN_HAND);
                break;
            case "Silent":
                PacketUtil.sendSilent(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }

    private void startTarget(BedTarget target) {
        BlockPos breakPos = target.breakPos();
        if (!OwnBedTracker.isOwnBed(breakPos)) {
            if (mc.gameMode.isDestroying()) {
                mc.gameMode.stopDestroyBlock();
            }

            this.resetBreakState();
            this.currentTarget = breakPos;
            this.instantMode = this.isWatchdogInstantActive() && !this.ghostFallback;
            this.followUpPos = this.instantMode ? target.followUpPos() : null;
            this.bedPos = target.bedPos();
            this.coverPos = target.followUpPos() == null ? null : target.breakPos();
            InventoryUtil.switchToBestTool(this.currentTarget);
            this.rotateTo(this.currentTarget);
            if (this.instantMode) {
                Direction breakFace = this.getBreakFace(this.currentTarget);
                this.sendBreakPacket(Action.START_DESTROY_BLOCK, this.currentTarget, breakFace);
                this.startedBreakPos = this.currentTarget;
                if (this.followUpPos != null) {
                    // Cover is stopped immediately; subsequent ticks coordinate the bed break.
                    this.sendBreakPacket(Action.STOP_DESTROY_BLOCK, this.currentTarget, breakFace);
                    this.startedBreakPos = null;
                }
            } else {
                mc.gameMode.startDestroyBlock(
                        this.currentTarget, this.getBreakFace(this.currentTarget));
            }

            if (!this.instantMode) {
                this.swingHand();
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.instantMode && event.getStage() == EventStage.POST) {
            this.otherActionThisTick = true;
        }
    }

    private BedTarget findTarget() {
        List bedsInRange = this.findBedsInRange(mc.player.blockPosition());
        if (bedsInRange.isEmpty()) {
            return null;
        }

        // Search priority: exposed bed, instant-mode top cover, then surrounding cover.
        BlockPos exposedBed = this.findNearestExposedBed(bedsInRange);
        if (exposedBed != null) {
            return new BedTarget(exposedBed, null, exposedBed);
        }

        if (this.isWatchdogInstantActive()) {
            BedTarget topCoverTarget = this.findTopCoverTarget(bedsInRange);
            if (topCoverTarget != null) {
                return topCoverTarget;
            }
        }

        return this.findNearestCoverTarget(bedsInRange);
    }

    private void tickInstantBreak() {

        if (!this.stopSent) {
            BlockState coverState = mc.level.getBlockState(this.currentTarget);
            this.breakTicks++;
            if (!coverState.isAir() && this.breakTicks % 4 == 0) {
                this.playHitSound(this.currentTarget);
            }

            // Simulate the current cover/bed progress without changing the client block state.
            float coverProgressPerTick =
                    coverState.isAir()
                            ? 0.0F
                            : coverState.getDestroyProgress(
                                    mc.player, mc.level, this.currentTarget);
            float coverProgress =
                    coverState.isAir()
                            ? 1.0F
                            : Math.min(1.0F, coverProgressPerTick * (this.breakTicks + 1));
            this.simulatedProgress = coverProgress;
            BlockPos followUpTarget = this.followUpPos;
            if (followUpTarget != null && coverState.isAir()) {
                this.coverAirTicks++;
                this.coverStuckTicks = 0;
            } else if (followUpTarget != null) {
                // A cover that reappears after enough air observations is treated as a ghost block.
                if (this.ghostBlockDetection.getValue() && this.coverAirTicks >= GHOST_AIR_TICKS) {
                    this.handleGhostBlock(this.bedPos, this.currentTarget);
                    return;
                }

                this.coverAirTicks = 0;
                if (this.ghostBlockDetection.getValue() && coverProgress >= 1.0F) {
                    this.coverStuckTicks++;
                } else {
                    this.coverStuckTicks = 0;
                }

                if (this.ghostBlockDetection.getValue()
                        && this.coverStuckTicks >= STUCK_TIMEOUT_TICKS) {
                    this.handleGhostBlock(this.bedPos, this.currentTarget);
                    return;
                }
            }

            if (followUpTarget == null) {
                // A single-block target enters confirmation as soon as simulated progress finishes.
                if (!(coverProgress < 1.0F)) {
                    this.sendBreakPacket(
                            Action.STOP_DESTROY_BLOCK,
                            this.currentTarget,
                            this.getBreakFace(this.currentTarget));
                    this.startedBreakPos = null;
                    this.stopSent = true;
                    this.unconfirmedTicks = 0;
                    this.confirmedAirTicks = 0;
                    this.releaseRotation();
                }
            } else {
                BlockState followUpState = mc.level.getBlockState(followUpTarget);
                if (!followUpState.isAir()) {
                    float followUpProgressPerTick =
                            followUpState.getDestroyProgress(mc.player, mc.level, followUpTarget);
                    if (!this.followUpStarted) {
                        // Start the bed when the remaining cover time fits inside its break time.
                        int computedFollowUpTicks;
                        if (followUpProgressPerTick <= 0.0F) {

                            computedFollowUpTicks = Integer.MAX_VALUE;
                        } else {
                            computedFollowUpTicks =
                                    Math.max(
                                            1, (int) Math.ceil(1.0F / followUpProgressPerTick) - 1);
                        }

                        int followUpTicksRequired = computedFollowUpTicks;
                        int computedRemainingCoverTicks;
                        if (coverProgress >= 1.0F) {
                            computedRemainingCoverTicks = 0;
                        } else if (coverProgressPerTick <= 0.0F) {

                            computedRemainingCoverTicks = Integer.MAX_VALUE;
                        } else {
                            computedRemainingCoverTicks =
                                    Math.max(
                                            0,
                                            (int)
                                                    Math.ceil(
                                                            (1.0F - coverProgress)
                                                                    / coverProgressPerTick));
                        }

                        int remainingCoverTicks = computedRemainingCoverTicks;
                        if (remainingCoverTicks <= followUpTicksRequired) {
                            this.followUpStarted = true;
                            this.followUpBreakTicks = 0;
                            this.rotateTo(followUpTarget);
                            this.sendBreakPacket(
                                    Action.START_DESTROY_BLOCK,
                                    followUpTarget,
                                    this.getBreakFace(followUpTarget));
                            this.startedBreakPos = followUpTarget;
                        }
                    } else {
                        this.followUpBreakTicks++;
                        this.followUpProgress =
                                Math.min(
                                        1.0F,
                                        followUpProgressPerTick * (this.followUpBreakTicks + 1));
                        if (this.followUpBreakTicks % 4 == 0) {
                            this.playHitSound(followUpTarget);
                        }

                        // The bed STOP also requires air at the cover: longer after an airborne
                        // tick.
                        if (!(this.followUpProgress < 1.0F)
                                && this.coverAirTicks
                                        >= (this.airborneDuringBreak
                                                ? GHOST_AIR_TICKS
                                                : GROUNDED_AIR_TICKS)) {
                            this.sendBreakPacket(
                                    Action.STOP_DESTROY_BLOCK,
                                    followUpTarget,
                                    this.getBreakFace(followUpTarget));
                            this.startedBreakPos = null;
                            this.currentTarget = followUpTarget;
                            this.followUpPos = null;
                            this.stopSent = true;
                            this.unconfirmedTicks = 0;
                            this.confirmedAirTicks = 0;
                            this.releaseRotation();
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        BlockPos visibleTarget = null;
        if (this.renderTarget.getValue() && this.currentTarget != null) {
            visibleTarget =
                    !this.instantMode
                                    || this.followUpPos == null
                                    || !this.followUpStarted
                                            && !mc.level.getBlockState(this.currentTarget).isAir()
                            ? this.currentTarget
                            : this.followUpPos;
        }

        boolean hasVisibleTarget =
                visibleTarget != null && !mc.level.getBlockState(visibleTarget).isAir();
        long nowMs = System.currentTimeMillis();
        if (hasVisibleTarget) {
            if (!visibleTarget.equals(this.renderPos)) {
                if (this.fadingBox == null
                        && this.renderPos != null
                        && this.renderBox != null
                        && mc.level.getBlockState(this.renderPos).isAir()) {
                    this.fadeStartAlpha = (float) this.boxAnimation.getValue();
                    if (this.fadeStartAlpha > 0.01F) {
                        this.fadingBox = this.renderBox;
                        this.fadeStartTime = nowMs;
                        this.fadeDuration =
                                Math.max(
                                        1L,
                                        (long)
                                                ((float) FADE_OUT_DURATION_MS
                                                        * this.fadeStartAlpha));
                    }
                }

                this.renderPos = visibleTarget;
                this.renderBox =
                        mc.level
                                .getBlockState(visibleTarget)
                                .getShape(mc.level, visibleTarget)
                                .bounds()
                                .move(visibleTarget)
                                .inflate(0.002);
                this.boxAnimation.setValue(1.0);
                this.boxAnimation.setTarget(1.0);
            } else {
                this.renderBox =
                        mc.level
                                .getBlockState(visibleTarget)
                                .getShape(mc.level, visibleTarget)
                                .bounds()
                                .move(visibleTarget)
                                .inflate(0.002);
            }
        }

        this.boxAnimation.animateTo(hasVisibleTarget ? 1.0 : 0.0);
        float boxAlpha = (float) this.boxAnimation.getValue();
        if (this.renderBox != null) {
            if (!hasVisibleTarget && boxAlpha <= 0.01F) {
                this.renderBox = null;
                this.renderPos = null;
            } else {
                float breakProgress =
                        Math.min(
                                1.0F,
                                this.instantMode && this.followUpStarted
                                        ? this.followUpProgress
                                        : this.getBreakProgress());
                this.drawTargetBox(event, this.renderBox, boxAlpha, breakProgress);
            }
        }

        // A completed target keeps its own cubic fade while the next target is being rendered.
        if (this.fadingBox != null) {
            double fadeFraction =
                    Math.min(1.0, (double) (nowMs - this.fadeStartTime) / this.fadeDuration);
            if (fadeFraction >= 1.0) {
                this.fadingBox = null;
            } else {
                double remainingFade = 1.0 - fadeFraction;
                this.drawTargetBox(
                        event,
                        this.fadingBox,
                        this.fadeStartAlpha
                                * (float) (remainingFade * remainingFade * remainingFade),
                        1.0F);
            }
        }
    }

    private boolean isWatchdogInstantActive() {
        HypixelModApi hypixelApi = Astralis.getInstance().getHypixelApi();
        return this.watchdogInstant.getValue() && hypixelApi != null && hypixelApi.isConnected();
    }

    private BlockPos findNearestExposedBed(List<BlockPos> bedsInRange) {
        double nearestDistance = Double.MAX_VALUE;
        BlockPos nearestBed = null;

        for (BlockPos bedCandidate : bedsInRange) {
            if (this.isBedExposed(bedCandidate)) {
                double bedDistance = mc.player.position().distanceTo(Vec3.atCenterOf(bedCandidate));
                if (bedDistance < nearestDistance) {
                    nearestDistance = bedDistance;
                    nearestBed = bedCandidate;
                }
            }
        }

        return nearestBed;
    }

    @Override
    public void onDisable() {
        if (this.instantMode
                && this.startedBreakPos != null
                && mc.player != null
                && mc.level != null) {
            this.sendBreakPacket(Action.ABORT_DESTROY_BLOCK, this.startedBreakPos, Direction.DOWN);
        }

        if (this.ghostFallback && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }

        if (this.originalSlot != -1) {
            InventoryUtil.setSlot(this.originalSlot);
        }

        this.resetBreakState();
        this.releaseRotation();
        this.otherActionThisTick = false;
        this.instantMode = false;
        this.currentTarget = null;
        this.ghostFallback = false;
        this.fallbackBedPos = null;
        this.renderPos = null;
        this.renderBox = null;
        this.fadingBox = null;
        this.originalSlot = -1;
    }

    private List<BlockPos> findBedsInRange(BlockPos searchCenter) {
        ArrayList bedsInRange = new ArrayList();
        int radius = this.breakRadius.getValue().intValue();

        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetY = -radius; offsetY <= radius; offsetY++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    BlockPos bedCandidate = searchCenter.offset(offsetX, offsetY, offsetZ);
                    if (mc.level.getBlockState(bedCandidate).getBlock() instanceof BedBlock
                            && this.isInRange(bedCandidate)
                            && !OwnBedTracker.isOwnBed(bedCandidate)
                            && this.isValidBed(bedCandidate)) {
                        bedsInRange.add(bedCandidate);
                    }
                }
            }
        }

        return bedsInRange;
    }

    private void rotateTo(BlockPos targetPos) {
        float[] angles =
                RotationUtil.getRotationsTo(
                        targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        Rotation rotation =
                new Rotation(angles[0], angles[1], 360.0F, 360.0F, RotationPriority.HIGH);

        rotation.setTag("Fucker");
        RotationManager.getInstance().submit(rotation);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getStage() == EventStage.POST) {
            // Delay instant-mode swings until other actions and manual input have been observed.
            if (this.instantMode
                    && this.currentTarget != null
                    && !this.stopSent
                    && !this.pausedForKillAura
                    && !this.otherActionThisTick
                    && !mc.player.swinging
                    && !mc.options.keyAttack.isDown()
                    && !mc.options.keyUse.isDown()) {
                this.swingHand();
            }

            this.otherActionThisTick = false;
        } else if (mc.gameMode != null) {
            if (this.lastGhostDetection != this.ghostBlockDetection.getValue()) {
                // Changing ghost detection invalidates the observations collected under its old
                // mode.
                this.lastGhostDetection = this.ghostBlockDetection.getValue();
                this.coverAirTicks = 0;
                this.coverStuckTicks = 0;
                this.unconfirmedTicks = 0;
                this.confirmedAirTicks = 0;
            }

            if (this.instantMode && this.followUpPos != null && !mc.player.onGround()) {
                this.airborneDuringBreak = true;
            }

            if (this.currentTarget != null
                    && this.instantMode
                            != (this.isWatchdogInstantActive() && !this.ghostFallback)) {
                this.stopBreaking();
            } else {
                if (this.pauseForKillAura.getValue()) {
                    // Pause without discarding the target; restore the slot and release our
                    // rotation.
                    KillAura killAura =
                            Astralis.getInstance().getModuleManager().getModule(KillAura.class);
                    if (killAura.isEnabled() && killAura.target != null) {
                        if (!this.pausedForKillAura) {
                            this.releaseRotation();
                            if (this.currentTarget != null && this.originalSlot != -1) {
                                InventoryUtil.setSlot(this.originalSlot);
                            }
                        }

                        this.pausedForKillAura = true;
                        return;
                    }
                }

                boolean resumedFromKillAura = this.pausedForKillAura;
                this.pausedForKillAura = false;
                if (resumedFromKillAura && this.currentTarget != null) {
                    InventoryUtil.switchToBestTool(this.currentTarget);
                }

                if (this.onlyWithTool.getValue() && !InventoryUtil.isHoldingTool()) {
                    this.stopBreaking();
                } else if (this.currentTarget != null) {
                    if (this.bedPos != null && !this.isValidBed(this.bedPos)) {
                        this.stopBreaking();
                    } else {
                        if (this.followUpPos != null) {
                            if (OwnBedTracker.isOwnBed(this.followUpPos)
                                    || mc.level.getBlockState(this.followUpPos).isAir()
                                    || !this.isInRange(this.followUpPos)) {
                                this.stopBreaking();
                                return;
                            }
                        } else if (OwnBedTracker.isOwnBed(this.currentTarget)) {
                            this.stopBreaking();
                            return;
                        }

                        if (!this.isInRange(this.currentTarget)) {
                            this.stopBreaking();
                        } else if (this.ghostFallback
                                && this.fallbackBedPos != null
                                && !this.currentTarget.equals(this.fallbackBedPos)
                                && mc.level.getBlockState(this.currentTarget).isAir()) {
                            // A normal fallback has removed the cover; continue with the remembered
                            // bed.
                            BlockPos fallbackBed = this.fallbackBedPos;
                            if (!mc.level.getBlockState(fallbackBed).isAir()
                                    && !OwnBedTracker.isOwnBed(fallbackBed)) {
                                this.startTarget(new BedTarget(fallbackBed, null, fallbackBed));
                            } else {
                                this.stopBreaking();
                            }
                        } else if (this.instantMode && this.stopSent) {
                            this.tickBreakConfirmation();
                        } else if ((!this.instantMode || this.followUpPos == null)
                                && mc.level.getBlockState(this.currentTarget).isAir()) {
                            this.stopBreaking();
                        } else {
                            this.tickBreaking(resumedFromKillAura);
                        }
                    }
                } else {
                    BedTarget nextTarget = this.findTarget();
                    if (nextTarget != null) {
                        if (this.originalSlot == -1) {
                            this.originalSlot = mc.player.getInventory().getSelectedSlot();
                        }

                        this.startTarget(nextTarget);
                    }
                }
            }
        }
    }

    private void playBreakSound(BlockPos pos) {
        SoundType soundType = mc.level.getBlockState(pos).getSoundType();
        mc.getSoundManager()
                .play(
                        new SimpleSoundInstance(
                                soundType.getBreakSound(),
                                SoundSource.BLOCKS,
                                soundType.getVolume(),
                                soundType.getPitch(),
                                SoundInstance.createUnseededRandom(),
                                pos));
    }

    @Override
    public void onEnable() {
        this.resetBreakState();
        this.otherActionThisTick = false;
        this.instantMode = false;
        this.ghostFallback = false;
        this.lastGhostDetection = this.ghostBlockDetection.getValue();
        this.fallbackBedPos = null;
        this.currentTarget = null;
        this.originalSlot = -1;
        this.renderPos = null;
        this.renderBox = null;
        this.fadingBox = null;
    }

    // Respect both the configured center-distance radius and Minecraft's interaction reach.
    private boolean isInRange(BlockPos pos) {
        return pos == null
                ? false
                : mc.player.position().distanceTo(Vec3.atCenterOf(pos))
                                <= this.breakRadius.getValue().floatValue()
                        && mc.player.isWithinBlockInteractionRange(
                                pos,
                                Math.min(
                                        1.0,
                                        this.breakRadius.getValue().floatValue()
                                                - mc.player.blockInteractionRange()));
    }

    @EventTarget
    public void onStartUseItem(StartUseItemEvent event) {
        if (this.instantMode) {
            this.otherActionThisTick = true;
        }
    }

    // Choose the face whose normal points most closely toward the player's eye position.
    private Direction getBreakFace(BlockPos targetPos) {
        Direction bestFace = Direction.UP;
        double bestAlignment = -Double.MAX_VALUE;

        for (Direction candidateFace : Direction.values()) {
            Vec3 faceNormal =
                    new Vec3(
                            candidateFace.getStepX(),
                            candidateFace.getStepY(),
                            candidateFace.getStepZ());
            double alignment =
                    faceNormal.dot(
                            mc.player
                                    .getEyePosition(1.0F)
                                    .subtract(Vec3.atCenterOf(targetPos))
                                    .normalize());
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                bestFace = candidateFace;
            }
        }

        return bestFace;
    }

    // Confirm the final STOP with four air ticks, or recover from reappearance/stalled
    // confirmation.
    private void tickBreakConfirmation() {
        if (mc.level.getBlockState(this.currentTarget).isAir()) {
            this.confirmedAirTicks++;
            this.unconfirmedTicks = 0;
            if (this.confirmedAirTicks >= BREAK_CONFIRMATION_TICKS) {
                this.stopBreaking();
            }
        } else if (!this.ghostBlockDetection.getValue()) {
            this.confirmedAirTicks = 0;
            this.unconfirmedTicks = 0;
        } else if (this.coverPos != null
                && this.coverAirTicks >= GHOST_AIR_TICKS
                && !mc.level.getBlockState(this.coverPos).isAir()) {
            this.handleGhostBlock(this.bedPos, this.coverPos);
        } else if (this.confirmedAirTicks >= GHOST_AIR_TICKS) {
            this.handleGhostBlock(this.bedPos, this.currentTarget);
        } else {
            this.confirmedAirTicks = 0;
            int waitingTicks = ++this.unconfirmedTicks;

            if (waitingTicks >= STUCK_TIMEOUT_TICKS) {
                this.handleGhostBlock(this.bedPos, this.currentTarget);
            }
        }
    }

    private static final String MODULE_ID = "3db04cc72535846fc0bfac11a16c2eae";
    private static final String SWING_MODE_SETTING_ID = "98ea31083df0c406e4d7c0c13ebe064d";
    private static final String ROTATION_MODE_SETTING_ID = "0cb3564adcc63f5c01856a71185da240";
    private static final String SWITCH_MODE_SETTING_ID = "0058e7b926474c34fe94f7f287d7ac0f";
    private static final String BREAK_RADIUS_SETTING_ID = "f3dd625af9796d19b3340f4304d3f0ae";
    private static final String RENDER_TARGET_SETTING_ID = "75c5a64d48729255dd5f54ac71f5f336";
    private static final String IGNORE_OWN_BED_SETTING_ID = "59c64615c732aa6636544f22fe300596";
    private static final String HUD_SETTING_ID = "7f966037ac422960332f29d28c47d204";
    private static final String TARGET_COLOR_SETTING_ID = "7c9f9de47f38567dc02d165982d63046";
    private static final String PAUSE_FOR_KILL_AURA_SETTING_ID =
            "6c63cfbf173853ba3d216063a111451dace23faad0992d71b8ab8c9323679b51";
    private static final String ONLY_WITH_TOOL_SETTING_ID = "e7e51538277c7f8365eb16d551ce12a7";
    private static final String WATCHDOG_INSTANT_SETTING_ID = "ecfbc626c5c7da5f6f18f6588b8b5995";
    private static final String GHOST_BLOCK_DETECTION_SETTING_ID =
            "ad320bff1b11778eb93180b1880c491676b002382013c9c27bfab6ce8fe0520e";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
}
