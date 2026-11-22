package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.InventoryUtil;
import astralis.util.RotationUtil;
import astralis.util.render.RenderUtil;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.StreamSupport;

public class ZombiesHelper extends Module {
    private final ModeSetting raycast;
    private final NumberSetting predictionTicks;
    private final BooleanSetting autoSlot;
    private final int[] hotbarSlots;
    private int slotIndex;
    private long lastSlotSwitchTime;
    private Entity target;

    public ZombiesHelper() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting = new ModeSetting(RAYCAST_SETTING_ID);
        String[] modeNames = new String[3];

        modeNames[0] = "Enabled";

        modeNames[1] = "Smart";

        modeNames[2] = "Disabled";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.raycast = modeSetting2.setDefault("Enabled");

        this.predictionTicks =
                new NumberSetting(PREDICTION_TICKS_SETTING_ID, 10.0F, 0.0F, 20.0F, 1.0F);

        this.autoSlot = new BooleanSetting(AUTO_SLOT_SETTING_ID, true);
        this.hotbarSlots = new int[] {1, 2, 3};
        this.slotIndex = 1;
        this.lastSlotSwitchTime = 0L;
        this.target = null;
        this.addSettings(this.raycast, this.predictionTicks, this.autoSlot);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            this.target =
                    StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                            .filter(this::isValidTarget)
                            .filter(this::hasLineOfSight)
                            .min(
                                    Comparator.comparingDouble(
                                            entity2 -> mc.player.distanceToSqr(entity2)))
                            .orElse(null);
            if (this.target != null) {
                this.shootAt(this.target);
            }
        }
    }

    private boolean hasLineOfSight(Entity entity2) {

        return this.raycast.is("Disabled") || this.findVisiblePoint(entity2) != null;
    }

    private Vec3 getPredictedOffset(Entity entity2) {
        int ticksAhead =
                !(entity2 instanceof Silverfish) && !(entity2 instanceof Endermite)
                        ? this.predictionTicks.getValue().intValue()
                        : 0;
        Vec3 vector3 = entity2.getDeltaMovement();
        return new Vec3(
                vector3.x * ticksAhead,
                entity2.onGround() && vector3.y < 0.0 ? 0.0 : vector3.y * ticksAhead,
                vector3.z * ticksAhead);
    }

    private void selectNextSlot() {
        if (System.currentTimeMillis() - this.lastSlotSwitchTime >= 100L) {
            for (int i = 0; i < this.hotbarSlots.length; i++) {
                this.slotIndex = (this.slotIndex + 1) % this.hotbarSlots.length;
                int hotbarSlot = this.hotbarSlots[this.slotIndex];
                if (this.isSlotReady(hotbarSlot)) {
                    InventoryUtil.setSlot(hotbarSlot);
                    this.lastSlotSwitchTime = System.currentTimeMillis();
                    break;
                }
            }
        }
    }

    private boolean isSlotReady(int hotbarSlot) {
        ItemStack itemStack = mc.player.getInventory().getItem(hotbarSlot);
        return !itemStack.isEmpty() && !itemStack.is(Items.DYE.lightGray())
                ? itemStack.getCount() > 1
                        || !itemStack.isDamageableItem()
                        || itemStack.getDamageValue() <= 0
                : false;
    }

    private void shootAt(Entity entity2) {
        Vec3 vector3 = this.findVisiblePoint(entity2);
        if (vector3 != null) {
            float[] rotations = RotationUtil.getRotationsTo(vector3.x, vector3.y, vector3.z);
            RotationManager.getInstance().submit(rotations[0], rotations[1], 360.0F, 360.0F);
            if (this.autoSlot.getValue()) {
                this.selectNextSlot();
            }

            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    private boolean isValidTarget(Entity entity2) {
        return entity2 instanceof Mob
                && !(entity2 instanceof Villager)
                && entity2.isAlive()
                && !entity2.isInvisible();
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        if (this.target != null) {
            Vec3 vector3 = this.getPredictedOffset(this.target);
            AABB boundingBox = this.target.getBoundingBox().move(vector3);
            PoseStack poseStack = render3DEvent.getPoseStack();

            RenderUtil.drawBox(poseStack, boundingBox, new Color(255, 0, 0, 100));
        }
    }

    private Vec3 findVisiblePoint(Entity entity2) {
        Vec3 vector3 = mc.player.getEyePosition(1.0F);
        Vec3 vector32 = entity2.position().add(this.getPredictedOffset(entity2));

        for (Vec3 vector33 :
                Arrays.asList(
                        vector32.add(0.0, entity2.getBbHeight() * 0.85, 0.0),
                        vector32.add(0.0, entity2.getBbHeight() * 0.5, 0.0),
                        vector32)) {
            if (this.hasClearPath(vector3, vector33)) {
                return vector33;
            }
        }

        return null;
    }

    private boolean isShootThroughBlock(BlockPos blockPos) {
        BlockState blockState = mc.level.getBlockState(blockPos);
        return blockState.is(Blocks.IRON_BARS)
                || blockState.is(Blocks.OAK_SLAB)
                || blockState.is(Blocks.DARK_OAK_SLAB)
                || blockState.is(Blocks.SMOOTH_STONE_SLAB)
                || blockState.is(Blocks.SPRUCE_SLAB)
                || blockState.getBlock() instanceof StairBlock;
    }

    private boolean hasClearPath(Vec3 vector3, Vec3 vector32) {

        Block block2 = this.raycast.is("Smart") ? Block.OUTLINE : Block.COLLIDER;
        BlockHitResult blockHitResult =
                mc.level.clip(new ClipContext(vector3, vector32, block2, Fluid.NONE, mc.player));
        if (blockHitResult.getType() == Type.MISS) {
            return true;
        }

        return this.raycast.is("Smart") && this.isShootThroughBlock(blockHitResult.getBlockPos());
    }

    private static final String MODULE_ID = "dc495df7d62753a1b5930c8b534e276b";
    private static final String RAYCAST_SETTING_ID = "55c97e377785b5bb7de87b61a342394a";
    private static final String PREDICTION_TICKS_SETTING_ID = "44d7456bb79ab56f0d18611a1c6e7cf1";
    private static final String AUTO_SLOT_SETTING_ID = "a063cd484321ed8e87176add817a1543";
}
