package astralis.util.world;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WorldUtil implements InstanceAccess {

    public static boolean isAirBlock(BlockPos blockPos) {
        return mc.level.getBlockState(blockPos).getBlock() instanceof AirBlock;
    }

    public static BlockState getBlockStateAtOffset(double offsetX, double offsetY, double offsetZ) {
        BlockPos blockPos = mc.player.blockPosition();
        return mc.level.getBlockState(blockPos.offset((int) offsetX, (int) offsetY, (int) offsetZ));
    }

    public static boolean hasCollisionAt(double x, double y, double z) {
        BlockPos blockPos = BlockPos.containing(x, y, z);
        BlockState blockState = mc.level.getBlockState(blockPos);
        boolean hasCollision = !blockState.getCollisionShape(mc.level, blockPos).isEmpty();
        return hasCollision;
    }

    public static boolean hasNearbySupport(Player player2) {
        Vec3 vector3 = player2.getViewVector(1.0F);
        double forwardOffsetX = vector3.x * 0.45;
        double forwardOffsetZ = vector3.z * 0.45;
        return hasCollisionAt(player2.getX(), player2.getY() - 0.6, player2.getZ())
                || hasCollisionAt(
                        player2.getX() + forwardOffsetX,
                        player2.getY() - 0.6,
                        player2.getZ() + forwardOffsetZ)
                || hasCollisionAt(player2.getX() + 0.3, player2.getY() - 0.6, player2.getZ())
                || hasCollisionAt(player2.getX() - 0.3, player2.getY() - 0.6, player2.getZ())
                || hasCollisionAt(player2.getX(), player2.getY() - 0.6, player2.getZ() + 0.3)
                || hasCollisionAt(player2.getX(), player2.getY() - 0.6, player2.getZ() - 0.3);
    }
}
