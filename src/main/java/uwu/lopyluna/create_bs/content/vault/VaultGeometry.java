package uwu.lopyluna.create_bs.content.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Shared coordinate projection for horizontal and vertical vault structures. */
public final class VaultGeometry {
    private VaultGeometry() {}

    public static BlockPos position(BlockPos origin, Direction.Axis axis, int length, int first, int second) {
        return switch (axis) {
            case X -> origin.offset(length, first, second);
            case Y -> origin.offset(first, length, second);
            case Z -> origin.offset(first, second, length);
        };
    }
}
