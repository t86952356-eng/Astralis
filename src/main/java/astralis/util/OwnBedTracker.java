package astralis.util;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.world.Fucker;
import astralis.util.player.TeamUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OwnBedTracker extends Listener {
    private static final Set<BlockPos> trackedPositions = ConcurrentHashMap.newKeySet();

    public static void add(BlockPos position) {
        trackedPositions.add(position);
    }

    public static Set<BlockPos> getPositions() {
        return trackedPositions;
    }

    /** Own-bed protection uses the block's color; recorded positions are tracked separately. */
    public static boolean isOwnBed(BlockPos position) {
        if (position == null) {
            return false;
        }

        if (!Astralis.getInstance()
                .getModuleManager()
                .getModule(Fucker.class)
                .getIgnoreOwnBed()
                .getValue()) {
            return false;
        }

        BlockState blockState = mc.level.getBlockState(position);
        return blockState.getBlock() instanceof BedBlock bed
                ? TeamUtil.isOwnTeamColor(bed.getColor())
                : false;
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent event) {
        trackedPositions.clear();
    }
}
