package astralis.module.impl.bedwars.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MouseScrollEvent;
import astralis.event.impl.TickEvent;
import astralis.module.impl.Bedwars;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.setting.SettingGroup;
import astralis.util.InventoryUtil;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BedwarsAutoChest extends Mode<Bedwars> {
    private final BooleanSetting autoClose;
    private final BooleanSetting easyDeposit;
    private final BooleanSetting iron;
    private final BooleanSetting gold;
    private final BooleanSetting diamond;
    private final BooleanSetting emerald;
    private final SettingGroup settingsGroup;
    private final SettingGroup valuablesGroup;
    private int previousSlot;

    public BedwarsAutoChest(Bedwars bedwars) {

        super("Auto Chest", bedwars);

        this.autoClose = new BooleanSetting("d924b3f03b52ba1765fa1aa722e0340e", true);

        this.easyDeposit = new BooleanSetting("9fdecb4ace6fcb90d25227a042664677", true);

        this.iron = new BooleanSetting("035a4400124659067dc4456ec078be99", true);

        this.gold = new BooleanSetting("622027d1825716b3a4a5ee053b59b0c1", true);

        this.diamond = new BooleanSetting("d83ab89f68128949d5445af1b4ecf462", true);

        this.emerald = new BooleanSetting("0ee71ba56f4128a2ba0e87993a486476", true);

        this.settingsGroup =
                new SettingGroup("e1b6ffd7ce011c799b9f90fcf88693e3")
                        .addValues(this.autoClose, this.easyDeposit);

        this.valuablesGroup =
                new SettingGroup("c83dad577799055bad1fbad03a2ec13a")
                        .addValues(this.iron, this.gold, this.diamond, this.emerald);
        this.previousSlot = -1;
    }

    private boolean isValuable(ItemStack stack) {
        return InventoryUtil.isWantedResource(
                stack,
                this.iron.getValue(),
                this.gold.getValue(),
                this.diamond.getValue(),
                this.emerald.getValue());
    }

    private void quickMoveRange(
            ChestMenu chestMenu, int startSlot, int endSlot, boolean onlyFirst) {
        int slotIndex = startSlot;

        while (slotIndex < endSlot) {
            if (this.isValuable(chestMenu.slots.get(slotIndex).getItem())) {
                mc.gameMode.handleContainerInput(
                        chestMenu.containerId, slotIndex, 0, ContainerInput.QUICK_MOVE, mc.player);
                if (onlyFirst) {
                    return;
                }
            }

            slotIndex++;
        }
    }

    @EventTarget
    public void onMouseScroll(MouseScrollEvent mouseScrollEvent) {
        AbstractContainerMenu abstractContainerMenu = mc.player.containerMenu;
        if (abstractContainerMenu instanceof ChestMenu chestMenu && Bedwars.isInBedwarsGame()) {
            double scrollAmount = mouseScrollEvent.getVertical();
            if (scrollAmount != 0.0) {
                int slotCount = chestMenu.slots.size();

                int inventoryStart = slotCount - 36;
                if (scrollAmount > 0.0) {
                    this.quickMoveRange(chestMenu, inventoryStart, chestMenu.slots.size(), false);
                } else {
                    this.quickMoveRange(chestMenu, 0, inventoryStart, false);
                }

                if (this.autoClose.getValue()) {
                    mc.player.closeContainer();
                }
            }
        }
    }

    public SettingGroup getSettingsGroup() {
        return this.settingsGroup;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (!this.easyDeposit.getValue() || !Bedwars.isInBedwarsGame()) {
            this.restorePreviousSlot();
        } else if (!mc.gameMode.getPlayerMode().isCreative()) {
            if (this.previousSlot != -1
                    && mc.player.getInventory().getSelectedSlot() == this.previousSlot) {
                this.previousSlot = -1;
            }

            if (!mc.player.isUsingItem()
                    || mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
                if (mc.options.keyAttack.isDown()
                        && mc.hitResult instanceof BlockHitResult blockHitResult) {
                    BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());
                    if (!blockState.is(Blocks.ENDER_CHEST) && !blockState.is(Blocks.CHEST)) {
                        this.restorePreviousSlot();
                    } else {
                        int selectedSlot = mc.player.getInventory().getSelectedSlot();
                        int valuableSlot = InventoryUtil.findHotbarSlot(this::isValuable);
                        if (valuableSlot != -1 && valuableSlot != selectedSlot) {
                            if (this.previousSlot == -1) {
                                this.previousSlot = selectedSlot;
                            }

                            InventoryUtil.setSlot(valuableSlot);
                        }

                        if (tickEvent.getStage() == EventStage.PRE && valuableSlot != -1) {
                            if (mc.player.containerMenu instanceof ChestMenu chestMenu) {
                                int slotCount = chestMenu.slots.size();

                                this.quickMoveRange(
                                        chestMenu, slotCount - 36, chestMenu.slots.size(), true);
                            } else {
                                mc.gameMode.startDestroyBlock(
                                        blockHitResult.getBlockPos(),
                                        blockHitResult.getDirection());
                                mc.player.swing(InteractionHand.MAIN_HAND);
                            }
                        }
                    }
                } else {
                    this.restorePreviousSlot();
                }
            }
        }
    }

    public SettingGroup getValuablesGroup() {
        return this.valuablesGroup;
    }

    private void restorePreviousSlot() {
        if (this.previousSlot != -1 && mc.gameMode != null) {
            InventoryUtil.setSlot(this.previousSlot);
            this.previousSlot = -1;
        } else {
            this.previousSlot = -1;
        }
    }

    @Override
    public void onDisable() {
        this.restorePreviousSlot();
    }
}
