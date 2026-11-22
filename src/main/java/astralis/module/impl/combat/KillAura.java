package astralis.module.impl.combat;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.HandleKeybindsEvent;
import astralis.event.impl.ItemUseAnimationEvent;
import astralis.event.impl.KeyPressCheckEvent;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WebSlowdownEvent;
import astralis.manager.AttackManager;
import astralis.module.Module;
import astralis.module.impl.Criticals;
import astralis.module.impl.Flight;
import astralis.module.impl.world.Scaffold;
import astralis.rotation.Rotation;
import astralis.rotation.RotationManager;
import astralis.rotation.RotationPriority;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.RangeSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.Blink;
import astralis.util.BreakUtil;
import astralis.util.InventoryUtil;
import astralis.util.MathUtil;
import astralis.util.PacketDelay;
import astralis.util.PacketUtil;
import astralis.util.PlayerListManager;
import astralis.util.RotationUtil;
import astralis.util.SwingType;
import astralis.util.target.TargetSortOrder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;

import pub.astralis.client.module.core.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects nearby targets, submits combat rotations and coordinates attacks with automatic blocking.
 * Setting IDs are kept because saved configurations and module metadata use them as keys.
 */
public class KillAura extends Module {
    private final ModeSetting clickMode;
    private final ModeSetting attackMode;
    private final ModeSetting targetPriority;
    private final NumberSetting targetSwitchDelayMillis;
    private final NumberSetting scanRange;
    private final NumberSetting attackRange;
    private final NumberSetting autoBlockRange;
    private final BooleanSetting requireWeapon;
    private final BooleanSetting pauseWhileUsingItem;
    private final BooleanSetting pauseInScreens;
    private final BooleanSetting pauseWhileManualBlocking;
    private final NumberSetting attackCooldownThreshold;
    private final BooleanSetting waitForCrits;
    private final RangeSetting attacksPerSecond;
    private final BooleanSetting legacyAttackOrder;
    private final BooleanSetting rayCast;
    private final ModeSetting rotationMode;
    private final RangeSetting yawSpeed;
    private final RangeSetting pitchSpeed;
    private final BooleanSetting lockViewYaw;
    private final BooleanSetting lockViewPitch;
    private final BooleanSetting autoBlock;
    private final BooleanSetting onlyOnRightClick;
    private final BooleanSetting forceBlockAnimation;
    private final BooleanSetting forceSwingAnimation;
    private final ModeSetting autoBlockMode;
    private final NumberSetting lagBlockDurationTicks;
    private final NumberSetting lagPacketDelayMillis;
    private final NumberSetting targetFovDegrees;
    private final BooleanSetting includeMobs;
    private final BooleanSetting includeAnimalsAndVillagers;
    private final BooleanSetting includeInvisiblePlayers;
    private final BooleanSetting includeTeammates;
    private final ModeSetting throughWallsMode;
    public LivingEntity target;
    public List<LivingEntity> targets;
    private boolean autoBlocking;
    private boolean blinking;
    private boolean packetDelayActive;
    private int blockTickCounter;
    private long blockStartTimeMillis;
    private float[] targetRotation;
    private boolean suppressForwardAndSprint;
    private long lastTargetSwitchTimeMillis;
    boolean inWeb;

    public KillAura() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting clickModeSetting = new ModeSetting(CLICK_MODE_SETTING_ID);
        String[] clickModes = new String[2];

        clickModes[0] = "Modern";

        clickModes[1] = "Legacy";
        ModeSetting clickModeWithOptions = clickModeSetting.addModes(clickModes);
        this.clickMode = clickModeWithOptions.setDefault("Modern");

        ModeSetting attackModeSetting = new ModeSetting(ATTACK_MODE_SETTING_ID);
        String[] attackModes = new String[2];

        attackModes[0] = "Packet";

        attackModes[1] = "Click";
        ModeSetting attackModeWithOptions = attackModeSetting.addModes(attackModes);
        this.attackMode = attackModeWithOptions.setDefault("Packet");

        ModeSetting targetPrioritySetting = new ModeSetting(PRIORITY_SETTING_ID);
        String[] targetPriorities = new String[5];

        targetPriorities[0] = "Health";

        targetPriorities[1] = "Distance";

        targetPriorities[2] = "Hurt Time";

        targetPriorities[3] = "Fov";

        targetPriorities[4] = "Switch";
        ModeSetting targetPriorityWithOptions = targetPrioritySetting.addModes(targetPriorities);
        this.targetPriority = targetPriorityWithOptions.setDefault("Hurt Time");

        this.targetSwitchDelayMillis =
                new NumberSetting(
                        TARGET_SWITCH_DELAY_MILLIS_SETTING_ID, 150.0F, 0.0F, 2000.0F, 10.0F);

        this.scanRange = new NumberSetting(SCAN_RANGE_SETTING_ID, 4.5F, 3.0F, 10.0F, 0.1F);

        this.attackRange = new NumberSetting(ATTACK_RANGE_SETTING_ID, 3.0F, 3.0F, 6.0F, 0.1F);

        this.autoBlockRange =
                new NumberSetting(AUTO_BLOCK_RANGE_SETTING_ID, 4.5F, 3.0F, 10.0F, 0.1F);

        this.requireWeapon = new BooleanSetting(REQUIRE_WEAPON_SETTING_ID, true);

        this.pauseWhileUsingItem = new BooleanSetting(PAUSE_WHILE_USING_ITEM_SETTING_ID, true);

        this.pauseInScreens = new BooleanSetting(PAUSE_IN_SCREENS_SETTING_ID, true);

        this.pauseWhileManualBlocking =
                new BooleanSetting(PAUSE_WHILE_MANUAL_BLOCKING_SETTING_ID, true);

        this.attackCooldownThreshold =
                new NumberSetting(ATTACK_COOLDOWN_THRESHOLD_SETTING_ID, 1.0F, 0.1F, 1.0F, 0.01F);

        this.waitForCrits = new BooleanSetting(WAIT_FOR_CRITS_SETTING_ID, false);

        this.attacksPerSecond =
                new RangeSetting(ATTACKS_PER_SECOND_SETTING_ID, 10.0F, 10.0F, 0.0F, 20.0F, 1.0F);

        this.legacyAttackOrder = new BooleanSetting(LEGACY_ATTACK_ORDER_SETTING_ID, false);

        this.rayCast = new BooleanSetting(RAY_CAST_SETTING_ID, false);

        ModeSetting rotationModeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] rotationModes = new String[3];

        rotationModes[0] = "Normal";

        rotationModes[1] = "Snap";

        rotationModes[2] = "None";
        ModeSetting rotationModeWithOptions = rotationModeSetting.addModes(rotationModes);
        this.rotationMode = rotationModeWithOptions.setDefault("Normal");

        this.yawSpeed = new RangeSetting(YAW_SPEED_SETTING_ID, 90.0F, 90.0F, 5.0F, 360.0F, 5.0F);

        this.pitchSpeed =
                new RangeSetting(PITCH_SPEED_SETTING_ID, 90.0F, 90.0F, 5.0F, 360.0F, 5.0F);

        this.lockViewYaw = new BooleanSetting(LOCK_VIEW_YAW_SETTING_ID, false);

        this.lockViewPitch = new BooleanSetting(LOCK_VIEW_PITCH_SETTING_ID, false);

        this.autoBlock = new BooleanSetting(AUTO_BLOCK_SETTING_ID, false);

        this.onlyOnRightClick = new BooleanSetting(ONLY_ON_RIGHT_CLICK_SETTING_ID, false);

        this.forceBlockAnimation = new BooleanSetting(FORCE_BLOCK_ANIMATION_SETTING_ID, false);

        this.forceSwingAnimation = new BooleanSetting(FORCE_SWING_ANIMATION_SETTING_ID, false);

        ModeSetting autoBlockModeSetting = new ModeSetting(AUTO_BLOCK_MODE_SETTING_ID);
        String[] autoBlockModes = new String[5];

        autoBlockModes[0] = "Vanilla";

        autoBlockModes[1] = "Watchdog A";

        autoBlockModes[2] = "Watchdog B";

        autoBlockModes[3] = "Watchdog C";

        autoBlockModes[4] = "Lag";
        ModeSetting autoBlockModeWithOptions = autoBlockModeSetting.addModes(autoBlockModes);
        this.autoBlockMode = autoBlockModeWithOptions.setDefault("Vanilla");

        this.lagBlockDurationTicks =
                new NumberSetting(LAG_BLOCK_DURATION_TICKS_SETTING_ID, 1.0F, 1.0F, 20.0F, 1.0F);

        this.lagPacketDelayMillis =
                new NumberSetting(
                        LAG_PACKET_DELAY_MILLIS_SETTING_ID, 150.0F, 50.0F, 1000.0F, 50.0F);

        this.targetFovDegrees =
                new NumberSetting(TARGET_FOV_DEGREES_SETTING_ID, 360.0F, 1.0F, 360.0F, 1.0F);

        this.includeMobs = new BooleanSetting(INCLUDE_MOBS_SETTING_ID, false);

        this.includeAnimalsAndVillagers =
                new BooleanSetting(INCLUDE_ANIMALS_AND_VILLAGERS_SETTING_ID, false);

        this.includeInvisiblePlayers =
                new BooleanSetting(INCLUDE_INVISIBLE_PLAYERS_SETTING_ID, false);

        this.includeTeammates = new BooleanSetting(INCLUDE_TEAMMATES_SETTING_ID, false);

        ModeSetting throughWallsSetting = new ModeSetting(THROUGH_WALLS_SETTING_ID);
        String[] throughWallsModes = new String[3];

        throughWallsModes[0] = "Disabled";

        throughWallsModes[1] = "Aim";

        throughWallsModes[2] = "Aim and Attack";
        ModeSetting throughWallsWithOptions = throughWallsSetting.addModes(throughWallsModes);
        this.throughWallsMode = throughWallsWithOptions.setDefault("Disabled");
        this.targets = new ArrayList<>();
        this.targetSwitchDelayMillis.hideIf(() -> !this.targetPriority.is("Switch"));
        this.attacksPerSecond.hideIf(
                () -> {
                    return this.clickMode.is("Modern");
                });
        this.attackCooldownThreshold.hideIf(() -> !this.clickMode.is("Modern"));
        this.waitForCrits.hideIf(() -> !this.clickMode.is("Modern"));
        this.legacyAttackOrder.hideIf(
                () -> {
                    if (!this.clickMode.is("Modern")) {

                        if (!this.attackMode.is("Click")) {
                            return false;
                        }
                    }

                    return true;
                });
        this.throughWallsMode.hideIf(() -> !this.attackMode.is("Packet"));
        this.yawSpeed.hideIf(
                () -> {
                    return this.rotationMode.is("None");
                });
        this.pitchSpeed.hideIf(
                () -> {
                    return this.rotationMode.is("None");
                });
        this.lockViewYaw.hideIf(
                () -> {
                    return this.rotationMode.is("None");
                });
        this.lockViewPitch.hideIf(
                () -> {
                    return this.rotationMode.is("None");
                });
        this.autoBlockMode.hideIf(() -> !this.autoBlock.getValue());
        this.onlyOnRightClick.hideIf(() -> !this.autoBlock.getValue());
        this.autoBlockRange.hideIf(() -> !this.autoBlock.getValue());
        this.lagBlockDurationTicks.hideIf(
                () -> !this.autoBlockMode.is("Lag") || !this.autoBlock.getValue());
        this.lagPacketDelayMillis.hideIf(
                () -> !this.autoBlockMode.is("Lag") || !this.autoBlock.getValue());
        SettingGroup viewLockGroup =
                new SettingGroup(LOCK_VIEW_GROUP_ID)
                        .addValues(this.lockViewYaw, this.lockViewPitch)
                        .collapsed();
        SettingGroup rotationGroup =
                new SettingGroup(ROTATIONS_GROUP_ID)
                        .addValues(
                                this.rotationMode, this.yawSpeed, this.pitchSpeed, viewLockGroup);
        SettingGroup checksGroup =
                new SettingGroup(CHECKS_GROUP_ID)
                        .addValues(
                                this.requireWeapon,
                                this.pauseInScreens,
                                this.pauseWhileUsingItem,
                                this.pauseWhileManualBlocking)
                        .collapsed();
        SettingGroup targetFiltersGroup =
                new SettingGroup(TARGETS_GROUP_ID)
                        .addValues(
                                this.includeMobs,
                                this.includeAnimalsAndVillagers,
                                this.includeInvisiblePlayers,
                                this.includeTeammates)
                        .collapsed();

        Setting[] settings =
                new Setting[] {
                    this.targetPriority,
                    this.targetSwitchDelayMillis,
                    this.clickMode,
                    this.attackMode,
                    this.attackCooldownThreshold,
                    this.scanRange,
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
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        settings[6] = this.attackRange;

        settings[7] = this.autoBlockRange;

        settings[8] = this.attacksPerSecond;

        settings[9] = checksGroup;

        settings[10] = this.waitForCrits;

        settings[11] = this.legacyAttackOrder;

        settings[12] = this.rayCast;

        settings[13] = rotationGroup;

        settings[14] = this.autoBlock;

        settings[15] = this.onlyOnRightClick;

        settings[16] = this.autoBlockMode;

        settings[17] = this.lagBlockDurationTicks;

        settings[18] = this.lagPacketDelayMillis;

        settings[19] = this.forceBlockAnimation;

        settings[20] = this.forceSwingAnimation;

        settings[21] = this.throughWallsMode;

        settings[22] = targetFiltersGroup;

        settings[23] = this.targetFovDegrees;
        this.addSettings(settings);
    }

    /** Refreshes the target list and prepares rotation and blocking before keybind attacks. */
    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getStage() != EventStage.POST) {
            this.inWeb = false;
            if (this.pauseInScreens.getValue() && mc.gui.screen() != null) {
                this.clearTargetAndResetClicks();
            } else {
                Scaffold scaffold =
                        Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
                if (scaffold.isEnabled()) {
                    this.clearTargetAndResetClicks();
                } else {
                    boolean includeAnimalsAndVillagers;
                    boolean includeMobs;
                    boolean includeTeammates;
                    boolean includeInvisiblePlayers;
                    boolean allowThroughWalls;
                    throughWallsCheck:
                    {
                        includeAnimalsAndVillagers = this.includeAnimalsAndVillagers.getValue();
                        includeMobs = this.includeMobs.getValue();
                        includeTeammates = this.includeTeammates.getValue();
                        includeInvisiblePlayers = this.includeInvisiblePlayers.getValue();

                        if (this.attackMode.is("Packet")) {

                            if (!this.throughWallsMode.is("Disabled")) {
                                allowThroughWalls = true;
                                break throughWallsCheck;
                            }
                        }

                        allowThroughWalls = false;
                    }

                    float searchRadius = this.scanRange.getValue();

                    TargetSortOrder sortOrder;
                    if (this.targetPriority.is("Health")) {
                        sortOrder = TargetSortOrder.HEALTH;
                    } else {

                        if (this.targetPriority.is("Distance")) {
                            sortOrder = TargetSortOrder.DISTANCE;
                        } else {

                            sortOrder =
                                    this.targetPriority.is("Fov")
                                            ? TargetSortOrder.ANGLE
                                            : TargetSortOrder.HURT_TIME;
                        }
                    }

                    this.targets =
                            BreakUtil.getTargets(
                                    includeAnimalsAndVillagers,
                                    includeMobs,
                                    includeTeammates,
                                    includeInvisiblePlayers,
                                    allowThroughWalls,
                                    searchRadius,
                                    sortOrder,
                                    true);
                    this.targets =
                            this.targets.stream()
                                    .filter(
                                            candidate ->
                                                    RotationUtil.isInFov(
                                                            candidate,
                                                            this.targetFovDegrees.getValue()))
                                    .toList();
                    if (this.targets.isEmpty()) {
                        this.handleNoTarget();
                    } else if (this.requireWeapon.getValue() && !InventoryUtil.isHoldingWeapon()) {
                        AttackManager.resetClickTimer();
                    } else if (this.pauseWhileUsingItem.getValue() && BreakUtil.isUsingItem()) {
                        AttackManager.resetClickTimer();
                    } else if (!this.pauseWhileManualBlocking.getValue()
                            || !InventoryUtil.isHoldingSword()
                            || !mc.options.keyUse.isDown()
                            || this.autoBlocking
                            || this.autoBlock.getValue() && this.onlyOnRightClick.getValue()) {
                        if (this.targetPriority.is("Switch")) {
                            long nowMillis = System.currentTimeMillis();
                            int currentTargetIndex = this.targets.indexOf(this.target);
                            if (currentTargetIndex < 0) {
                                this.target = this.targets.getFirst();
                                this.lastTargetSwitchTimeMillis = nowMillis;
                            } else if (nowMillis - this.lastTargetSwitchTimeMillis
                                    >= this.targetSwitchDelayMillis.getValue().longValue()) {
                                for (int targetOffset = 1;
                                        targetOffset < this.targets.size();
                                        targetOffset++) {
                                    LivingEntity candidateTarget =
                                            this.targets.get(
                                                    (currentTargetIndex + targetOffset)
                                                            % this.targets.size());
                                    if (!(RotationUtil.distanceToBox(candidateTarget)
                                            > this.attackRange.getValue().floatValue())) {
                                        this.target = candidateTarget;
                                        this.lastTargetSwitchTimeMillis = nowMillis;
                                        break;
                                    }
                                }
                            }
                        } else {
                            this.target = this.targets.getFirst();
                        }

                        if (this.target instanceof Player
                                && Astralis.getInstance()
                                        .getManagerRegistry()
                                        .getManager(PlayerListManager.class)
                                        .isFriend((Player) this.target)) {
                            this.handleNoTarget();
                        } else {
                            this.targetRotation = RotationUtil.getRotationsToEntity(this.target);
                            if (this.rotationMode.is("Normal")) {
                                this.submitRotation(this.targetRotation[0], this.targetRotation[1]);
                            }

                            double targetDistance = RotationUtil.distanceToBox(this.target);
                            if (!this.autoBlockMode.is("Watchdog B")) {
                                if (!this.autoBlockMode.is("Watchdog C")) {
                                    if (this.shouldAutoBlock()) {
                                        this.doAutoBlock();
                                    } else {
                                        this.unblock();
                                    }
                                }
                            }

                            if ((this.forceSwingAnimation.getValue()
                                            || this.shouldWatchdogASwing(targetDistance))
                                    && this.target != null
                                    && targetDistance <= this.attackRange.getValue().floatValue()) {
                                mc.player.swing(InteractionHand.MAIN_HAND, false);
                            }
                        }
                    } else {
                        AttackManager.resetClickTimer();
                    }
                }
            }
        }
    }

    public boolean isBlinking() {
        return this.blinking;
    }

    public boolean isAutoBlocking() {
        return this.autoBlocking;
    }

    public boolean isThroughWallsActive() {
        if (this.isEnabled()) {

            if (!this.throughWallsMode.is("Disabled") && this.target != null) {
                return true;
            }
        }

        return false;
    }

    public boolean isPacketDelayActive() {
        return this.packetDelayActive;
    }

    @Override
    public String getSuffix() {
        return this.clickMode.getValue().getName();
    }

    public boolean shouldForceBlockAnimation() {
        return this.isEnabled()
                && this.target != null
                && RotationUtil.distanceToBox(this.target)
                        <= this.autoBlockRange.getValue().floatValue()
                && this.forceBlockAnimation.getValue()
                && (!this.autoBlock.getValue() || this.isRightClickConditionMet());
    }

    public ModeSetting getAutoBlockMode() {
        return this.autoBlockMode;
    }

    private boolean shouldWatchdogASwing(double targetDistance) {
        if (this.autoBlock.getValue() && this.isRightClickConditionMet()) {

            if (this.autoBlockMode.is("Watchdog A")
                    && InventoryUtil.isHoldingSword()
                    && targetDistance <= this.attackRange.getValue().floatValue()) {
                return true;
            }
        }

        return false;
    }

    private void attackTarget(LivingEntity candidateTarget) {
        if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR && candidateTarget != null) {
            SwingType swingType = SwingType.PACKET;

            if (this.attackMode.is("Click")) {
                swingType = SwingType.CLICK;
            } else if (this.legacyAttackOrder.getValue()) {
                swingType = SwingType.ONEDOTEIGHT;
            }

            if (this.clickMode.is("Modern")) {
                AttackManager.attackIfCooldownReady(
                        swingType,
                        candidateTarget,
                        this.attackCooldownThreshold.getValue().doubleValue());
            } else {

                if (this.autoBlockMode.is("Watchdog A")) {
                    AttackManager.attackAtFixedCps(swingType, candidateTarget, 10.0);
                } else {
                    AttackManager.attackAtRandomCps(
                            swingType,
                            candidateTarget,
                            this.attacksPerSecond.getLow(),
                            this.attacksPerSecond.getHigh());
                }
            }
        }
    }

    @EventTarget
    public void onWebSlowdown(WebSlowdownEvent webSlowdownEvent) {
        this.inWeb = true;
    }

    private void handleNoTarget() {
        this.clearTarget();
    }

    @EventTarget
    public void onItemUseAnimation(ItemUseAnimationEvent event) {
        event.setUsing(
                this.target != null
                        && RotationUtil.distanceToBox(this.target)
                                <= this.autoBlockRange.getValue().floatValue()
                        && this.forceBlockAnimation.getValue()
                        && (!this.autoBlock.getValue() || this.isRightClickConditionMet()));
    }

    public NumberSetting getScanRange() {
        return this.scanRange;
    }

    public void setWatchdogCBlocking(boolean blocking) {

        if (this.autoBlockMode.is("Watchdog C")) {
            this.autoBlocking = blocking;
        }
    }

    @EventTarget
    public void onKeyPressCheck(KeyPressCheckEvent event) {
        boolean pressUseKey;
        useKeyCheck:
        {
            if (this.shouldAutoBlock()) {

                if (!this.autoBlockMode.is("Lag") || this.blockTickCounter != 1) {
                    pressUseKey = true;
                    break useKeyCheck;
                }
            }

            pressUseKey = false;
        }

        event.setPressed(pressUseKey);
    }

    private void clearTargetAndResetClicks() {
        this.clearTarget();
        AttackManager.resetClickTimer();
    }

    @EventTarget
    public void onHandleKeybinds(HandleKeybindsEvent handleKeybindsEvent) {
        if (this.target != null && mc.gui.screen() == null) {
            double targetDistance = RotationUtil.distanceToBox(this.target);
            if (!(targetDistance > this.attackRange.getValue().floatValue())
                    && this.critWaitAllowsAttack()) {

                if (this.rotationMode.is("Snap")) {
                    this.submitRotation(this.targetRotation[0], this.targetRotation[1]);
                }

                if (this.canHitTarget(this.target)) {

                    if (!this.attackMode.is("Click")
                            || mc.hitResult instanceof EntityHitResult entityHit
                                    && entityHit.getEntity() == this.target) {
                        this.attackTarget(this.target);
                    }
                }
            }
        }
    }

    public void setPacketDelayActive(boolean active) {
        this.packetDelayActive = active;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getStage() == EventStage.SEND && this.isWatchdogCActive()) {
            if (event.getPacket() instanceof ServerboundInteractPacket) {
                if (this.packetDelayActive) {
                    PacketDelay.stopAndFlush();
                    this.packetDelayActive = false;
                }
            }
        }
    }

    private void slotSwitchUnblock(boolean blinkAfterSlotSwitch) {
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        int slotOffset = MathUtil.randomInt(1, 5);
        int unwrappedTemporarySlot = selectedSlot + slotOffset;

        int temporarySlot = unwrappedTemporarySlot % 9;
        PacketUtil.send(new ServerboundSetCarriedItemPacket(temporarySlot));
        this.blinking = blinkAfterSlotSwitch;
        PacketUtil.send(new ServerboundSetCarriedItemPacket(selectedSlot));
        PacketUtil.send(
                new ServerboundPlayerActionPacket(
                        Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
    }

    private boolean shouldAutoBlock() {
        if (!this.isEnabled() || !this.autoBlock.getValue() || !InventoryUtil.isHoldingSword()) {
            return false;
        } else {
            return this.onlyOnRightClick.getValue()
                    ? mc.options.keyUse.isDown()
                    : this.target != null
                            && RotationUtil.distanceToBox(this.target)
                                    <= this.autoBlockRange.getValue().floatValue();
        }
    }

    /** Advances the selected blocking mode, including its blink or packet-delay stage. */
    private void doAutoBlock() {
        String autoBlockModeName =
                ((astralis.setting.Mode) this.autoBlockMode.getValue()).getName();
        int autoBlockModeIndex = -1;
        switch (autoBlockModeName.hashCode()) {
            case 1897755483:
                {
                    if (!autoBlockModeName.equals("Vanilla")) break;
                    autoBlockModeIndex = 0;
                    break;
                }
            case 1898048270:
                {
                    if (!autoBlockModeName.equals("Watchdog A")) break;
                    autoBlockModeIndex = 1;
                    break;
                }
            case 1898048271:
                {
                    if (!autoBlockModeName.equals("Watchdog B")) break;
                    autoBlockModeIndex = 2;
                    break;
                }
            case 1898048272:
                {
                    if (!autoBlockModeName.equals("Watchdog C")) break;
                    autoBlockModeIndex = 3;
                    break;
                }
            case 76146:
                {
                    if (!autoBlockModeName.equals("Lag")) break;
                    autoBlockModeIndex = 4;
                }
        }
        switch (autoBlockModeIndex) {
            case 0:
                {
                    ((astralis.mixin.MinecraftAccessor) mc).callStartUseItem();
                    this.autoBlocking = true;
                    break;
                }
            case 1:
                {
                    if (this.shouldAutoBlock()) {
                        if (this.autoBlocking) break;
                        KillAura.mc.options.keyUse.setDown(true);
                        this.autoBlocking = true;
                        break;
                    }
                    this.unblock();
                    break;
                }
            case 2:
                {
                    if (this.blockTickCounter == 1) {
                        this.blockTickCounter = 0;
                        return;
                    }
                    if (!KillAura.mc.player.isInLiquid()) {
                        ((astralis.mixin.MinecraftAccessor) mc).callStartUseItem();
                        Blink.stop();
                        this.blinking = false;
                        Blink.startOutgoing();
                        this.blinking = true;
                        this.autoBlocking = true;
                        ++this.blockTickCounter;
                        break;
                    }
                    this.unblock();
                    break;
                }
            case 3:
                {
                    if (this.packetDelayActive) {
                        if (PacketDelay.isDelaying()) {
                            return;
                        }
                        this.packetDelayActive = false;
                        this.autoBlocking = false;
                    }
                    if (!this.autoBlocking) {
                        ((astralis.mixin.MinecraftAccessor) mc).callStartUseItem();
                        this.autoBlocking = true;
                        this.blockStartTimeMillis = System.currentTimeMillis();
                        return;
                    }
                    if (System.currentTimeMillis() - this.blockStartTimeMillis < 150L) {
                        return;
                    }
                    KillAura.mc.gameMode.releaseUsingItem((Player) KillAura.mc.player);
                    this.autoBlocking = false;
                    PacketDelay.startDelay((long) 200L);
                    this.packetDelayActive = true;
                    break;
                }
            case 4:
                {
                    if (this.packetDelayActive) {
                        this.unblock();
                        if ((double) this.blockTickCounter
                                >= ((Float) this.lagPacketDelayMillis.getValue()).doubleValue()
                                        / 50.0) {
                            if (this.shouldAutoBlock()) {
                                KillAura.mc.options.keyUse.setDown(true);
                                this.autoBlocking = true;
                            }
                            PacketDelay.stopAndFlush();
                            this.packetDelayActive = false;
                            this.blockTickCounter = 0;
                            break;
                        }
                        ++this.blockTickCounter;
                        break;
                    }
                    if (this.blockTickCounter
                            < ((Float) this.lagBlockDurationTicks.getValue()).intValue()) {
                        if (this.shouldAutoBlock()) {
                            if (!this.autoBlocking) {
                                KillAura.mc.options.keyUse.setDown(true);
                                this.autoBlocking = true;
                            }
                        }
                        ++this.blockTickCounter;
                        break;
                    }
                    this.unblock();
                    PacketDelay.startDelay(
                            (long) ((Float) this.lagPacketDelayMillis.getValue()).longValue());
                    this.packetDelayActive = true;
                    this.blockTickCounter = 0;
                }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {

        if (!this.autoBlockMode.is("Watchdog B")) {

            if (!this.autoBlockMode.is("Watchdog C")) {
                return;
            }
        }

        if (this.shouldAutoBlock()) {
            this.doAutoBlock();
        } else {
            this.unblock();
        }
    }

    /** Applies attack visibility and ray-cast rules to a candidate already selected by the scan. */
    public boolean canHitTarget(LivingEntity candidateTarget) {
        if (candidateTarget == null) {
            return false;
        }

        if (!mc.player.hasLineOfSight(candidateTarget)) {

            if (this.attackMode.is("Packet")) {

                if (this.throughWallsMode.is("Aim")) {
                    return false;
                }
            }
        }

        if (this.attackMode.is("Packet")) {

            if (this.throughWallsMode.is("Aim and Attack")) {
                return this.passesRayCast(candidateTarget, true);
            }
        }

        return this.passesRayCast(candidateTarget, false);
    }

    public NumberSetting getAutoBlockRange() {
        return this.autoBlockRange;
    }

    @Override
    public void onDisable() {
        this.suppressForwardAndSprint = false;
        this.clearTargetAndResetClicks();
    }

    @EventTarget(4)
    public void onMoveInput(MoveInputEvent event) {
        if (this.suppressForwardAndSprint) {
            event.setForward(false);
            event.setSprint(false);
            this.suppressForwardAndSprint = false;
        }
    }

    /** Waits for the airborne critical window and adjusts the following movement input. */
    private boolean critWaitAllowsAttack() {

        if (this.clickMode.is("Modern") && this.waitForCrits.getValue()) {
            Criticals criticals =
                    Astralis.getInstance().getModuleManager().getModule(Criticals.class);
            if (!mc.player.onGround() && criticals.canCrit(false) && !this.inWeb) {
                boolean shouldWaitForCritical = criticals.shouldWaitForCrit(this.target);
                if (!shouldWaitForCritical && !criticals.isFallingCritReady(true)) {
                    this.suppressForwardAndSprint = false;
                    return true;
                } else {
                    this.suppressForwardAndSprint = true;
                    return !shouldWaitForCritical && criticals.isFallingCritReady(false)
                            || Astralis.getInstance()
                                    .getModuleManager()
                                    .getModule(Flight.class)
                                    .isEnabled();
                }
            } else {
                this.suppressForwardAndSprint = false;
                return true;
            }
        } else {
            return true;
        }
    }

    private boolean isRightClickConditionMet() {
        return !this.onlyOnRightClick.getValue() || mc.options.keyUse.isDown();
    }

    private void submitRotation(float desiredYaw, float desiredPitch) {
        Rotation rotationRequest =
                new Rotation(
                        desiredYaw,
                        desiredPitch,
                        MathUtil.randomFloat(this.yawSpeed.getLow(), this.yawSpeed.getHigh()),
                        MathUtil.randomFloat(this.pitchSpeed.getLow(), this.pitchSpeed.getHigh()),
                        RotationPriority.MEDIUM);

        rotationRequest.setTag("Kill Aura");
        rotationRequest.setTargetEntity(this.target);
        rotationRequest.setApplyYawToPlayerView(this.lockViewYaw.getValue());
        rotationRequest.setApplyPitchToPlayerView(this.lockViewPitch.getValue());
        RotationManager.getInstance().submit(rotationRequest);
    }

    private void clearTarget() {
        this.target = null;
        if (this.packetDelayActive) {
            PacketDelay.stopAndFlush();
            this.packetDelayActive = false;
        }

        if (this.shouldAutoBlock()) {
            if (!this.autoBlocking) {
                this.doAutoBlock();
            }
        } else {
            this.unblock();
        }
    }

    @Override
    public void onEnable() {
        this.autoBlocking = false;
        this.blinking = false;
        this.packetDelayActive = false;
        this.suppressForwardAndSprint = false;
        this.lastTargetSwitchTimeMillis = 0L;
        AttackManager.resetClickTimer();
    }

    public NumberSetting getAttackRange() {
        return this.attackRange;
    }

    private boolean passesRayCast(LivingEntity candidateTarget, boolean allowThroughWalls) {
        if (!this.rayCast.getValue()) {
            return true;
        } else {

            if (!this.rotationMode.is("Snap")) {

                return this.rotationMode.is("None")
                        ? RotationUtil.rayTraceHitsEntity(
                                candidateTarget,
                                this.attackRange.getValue(),
                                allowThroughWalls,
                                mc.player.getYRot(),
                                mc.player.getXRot())
                        : RotationUtil.rayTraceHitsEntity(
                                candidateTarget,
                                this.attackRange.getValue(),
                                allowThroughWalls,
                                RotationManager.getInstance().getYaw(),
                                RotationManager.getInstance().getPitch());
            } else {
                return this.targetRotation != null
                        && RotationUtil.rayTraceHitsEntity(
                                candidateTarget,
                                this.attackRange.getValue(),
                                allowThroughWalls,
                                this.targetRotation[0],
                                this.targetRotation[1]);
            }
        }
    }

    /** Releases the active blocking method and flushes an outstanding outgoing blink. */
    private void unblock() {
        releaseBlocking:
        if (this.autoBlocking && InventoryUtil.isHoldingSword()) {

            if (!this.autoBlockMode.is("Watchdog A")) {

                if (!this.autoBlockMode.is("Watchdog B")) {

                    if (!this.autoBlockMode.is("Lag")) {
                        mc.gameMode.releaseUsingItem(mc.player);
                        break releaseBlocking;
                    }
                }
            }

            mc.options.keyUse.setDown(false);
        }

        this.blockTickCounter = 0;
        this.autoBlocking = false;
        if (this.blinking) {
            Blink.stop();
            this.blinking = false;
        }
    }

    public boolean isWatchdogCActive() {
        if (this.isEnabled()) {

            if (this.autoBlockMode.is("Watchdog C")
                    && (this.autoBlocking || this.packetDelayActive)) {
                return true;
            }
        }

        return false;
    }

    private static final String MODULE_ID = "c1ad727d6401d858d0a5fdde4574b1d0";
    private static final String CLICK_MODE_SETTING_ID = "f80930a2e1f6500489b68d6af4496ec2";
    private static final String ATTACK_MODE_SETTING_ID = "993e77f79127192d0ff21751e0912d9c";
    private static final String PRIORITY_SETTING_ID = "08090004ab596f97273168c5eb2f974d";
    private static final String TARGET_SWITCH_DELAY_MILLIS_SETTING_ID =
            "ac578093825e7db51956cb948cfe8272";
    private static final String SCAN_RANGE_SETTING_ID = "28ee0a3da47d621fba0c06a6630727e4";
    private static final String ATTACK_RANGE_SETTING_ID = "bf8ba8b033c6e98dfb6315c8b79564ae";
    private static final String AUTO_BLOCK_RANGE_SETTING_ID = "94f573da53ca5c601e469ad3df823c49";
    private static final String REQUIRE_WEAPON_SETTING_ID = "475c4989d42bb1722ad7dd2d74209595";
    private static final String PAUSE_WHILE_USING_ITEM_SETTING_ID =
            "72b9079ecaa2ba4987d85e8c7fce01b3";
    private static final String PAUSE_IN_SCREENS_SETTING_ID = "9b30df64aa789abb3c192dde144beb96";
    private static final String PAUSE_WHILE_MANUAL_BLOCKING_SETTING_ID =
            "9914ea821218572e4d5784cde8c942f7";
    private static final String ATTACK_COOLDOWN_THRESHOLD_SETTING_ID =
            "a78ec8fb2f89fd8fea33d2adc091900930adfaee2a993919e993dbb4caef6f57";
    private static final String WAIT_FOR_CRITS_SETTING_ID = "3aac224dadded091fcac0d512193d45c";
    private static final String ATTACKS_PER_SECOND_SETTING_ID = "d5f6cf1ccb6966151a0d975ed13c109f";
    private static final String LEGACY_ATTACK_ORDER_SETTING_ID =
            "7d3f217f4eb14e7a215c1ae07a908219713619fe42c7e2195b6191f38ec8aa19";
    private static final String RAY_CAST_SETTING_ID = "7fbd0e29f43ba141554533640e751c94";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String YAW_SPEED_SETTING_ID = "7dafa637ea670fc815c9274ad2e35e7f";
    private static final String PITCH_SPEED_SETTING_ID = "7178b99f9c8ed1c6338486b30f7e0f8b";
    private static final String LOCK_VIEW_YAW_SETTING_ID = "18ef4a13984667e2b3485a2732c93ee9";
    private static final String LOCK_VIEW_PITCH_SETTING_ID = "12bee2530125516d21571a80e596c719";
    private static final String AUTO_BLOCK_SETTING_ID = "334c31eeaa7c1fcf7c4a9a6f0ea4f971";
    private static final String ONLY_ON_RIGHT_CLICK_SETTING_ID =
            "c4e8ea2e800ed02c66b56229b91504fbd6e107aeebfd16f5ff018bc64a04d385";
    private static final String FORCE_BLOCK_ANIMATION_SETTING_ID =
            "d38948b64e1b5e9d07963c28d7a423e623102ce9c83469176d5298c986231b4a";
    private static final String FORCE_SWING_ANIMATION_SETTING_ID =
            "c7df91eda8c87a998bc7d2fbabffa47b23102ce9c83469176d5298c986231b4a";
    private static final String AUTO_BLOCK_MODE_SETTING_ID = "b0f0fc672c23bd71e5c8fa1833315a0a";
    private static final String LAG_BLOCK_DURATION_TICKS_SETTING_ID =
            "b84dbea3b2ddcb1e95044088b3ceaffe";
    private static final String LAG_PACKET_DELAY_MILLIS_SETTING_ID =
            "f481484d72cd3cdb312b9498d0e99a39";
    private static final String TARGET_FOV_DEGREES_SETTING_ID = "7aee8b188353d76022ea4d69fd720955";
    private static final String INCLUDE_MOBS_SETTING_ID = "b2e39a7090da277f100b28beac526e55";
    private static final String INCLUDE_ANIMALS_AND_VILLAGERS_SETTING_ID =
            "3591a85a0cad9ff80c1df14006606ca8";
    private static final String INCLUDE_INVISIBLE_PLAYERS_SETTING_ID =
            "a63ea6c796b6ef2ae55074f6ee3726db";
    private static final String INCLUDE_TEAMMATES_SETTING_ID = "46334632fc765d791744b4e27384ec4d";
    private static final String THROUGH_WALLS_SETTING_ID = "4fb927fdbee561036f15bca7ddf5b4c4";
    private static final String LOCK_VIEW_GROUP_ID = "d0516d2479ba2e348be4f374e7788fb3";
    private static final String ROTATIONS_GROUP_ID = "b64d2022fbf8a68f2bcf0b4bf8376a1f";
    private static final String CHECKS_GROUP_ID = "6b0341c82f929a90d2899b657641d3e1";
    private static final String TARGETS_GROUP_ID = "6ef0e612f8bb85c79eef6a9949fb6be4";
}
