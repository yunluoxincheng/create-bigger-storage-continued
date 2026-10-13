package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Allows a completed wider end layer to detach from an older narrow vault. */
final class VaultConnectivity {
    private VaultConnectivity() {}

    static boolean tryExpandCrossSection(TieredVaultBlockEntity changed) {
        Level level = changed.getLevel();
        if (level == null || level.isClientSide || VaultUpgradeService.isUpdating()) return false;
        Direction.Axis axis = changed.getMainConnectionAxis();
        int width = changed.getMaxWidth();
        for (int first = 0; first < width; first++) {
            for (int second = 0; second < width; second++) {
                BlockPos origin = VaultGeometry.position(changed.getBlockPos(), axis, 0, -first, -second);
                List<TieredVaultBlockEntity> layer = new ArrayList<>();
                Set<TieredVaultBlockEntity> roots = new LinkedHashSet<>();
                boolean valid = true;
                boolean hasNarrowVault = false;
                for (int a = 0; a < width && valid; a++) {
                    for (int b = 0; b < width; b++) {
                        BlockPos pos = VaultGeometry.position(origin, axis, 0, a, b);
                        if (!level.hasChunkAt(pos)) { valid = false; break; }
                        BlockEntity entity = level.getBlockEntity(pos);
                        if (!(entity instanceof TieredVaultBlockEntity part) || part.isRemoved()
                            || part.getType() != changed.getType() || part.getMainConnectionAxis() != axis
                            || !level.hasChunkAt(part.getController())) { valid = false; break; }
                        TieredVaultBlockEntity root = part.getControllerBE();
                        if (root == null || !root.isController() || root.isRemoved()
                            || root.getMainConnectionAxis() != axis || root.getWidth() >= width
                            || !fitsCrossSection(root, origin, axis, width)) { valid = false; break; }
                        hasNarrowVault |= root.getWidth() > 1;
                        roots.add(root);
                        layer.add(part);
                    }
                }
                if (!valid || !hasNarrowVault) continue;
                // Do not partially detach a structure whose members are not loaded.
                for (TieredVaultBlockEntity root : roots) {
                    if (!completeStructure(root, changed.getType())) { valid = false; break; }
                }
                if (!valid) continue;

                // Inventories are owned per block. Only connectivity and cached views
                // change; remaining old members retain contents and queued regrouping.
                for (TieredVaultBlockEntity root : roots) ConnectivityHandler.splitMulti(root);
                for (TieredVaultBlockEntity part : layer) {
                    part.setController(origin);
                    part.setWidth(width);
                    part.setHeight(1);
                    part.preventConnectivityUpdate();
                }
                for (TieredVaultBlockEntity part : layer) {
                    part.notifyMultiUpdated();
                    part.sendData();
                }
                layer.get(0).updateComparators();
                return true;
            }
        }
        return false;
    }

    private static boolean fitsCrossSection(TieredVaultBlockEntity root, BlockPos origin,
                                           Direction.Axis axis, int width) {
        BlockPos start = root.getBlockPos();
        BlockPos end = VaultGeometry.position(start, axis, 0, root.getWidth() - 1, root.getWidth() - 1);
        for (Direction.Axis perpendicular : Direction.Axis.values()) {
            if (perpendicular == axis) continue;
            if (start.get(perpendicular) < origin.get(perpendicular)
                || end.get(perpendicular) >= origin.get(perpendicular) + width) return false;
        }
        return true;
    }

    private static boolean completeStructure(TieredVaultBlockEntity root, BlockEntityType<?> type) {
        Level level = root.getLevel();
        Direction.Axis axis = root.getMainConnectionAxis();
        int width = root.getWidth();
        int length = root.getHeight();
        if (level == null || width < 1 || length < 1 || length > root.getMaxLength(axis, width)) return false;
        for (int l = 0; l < length; l++) {
            for (int a = 0; a < width; a++) {
                for (int b = 0; b < width; b++) {
                    BlockPos pos = VaultGeometry.position(root.getBlockPos(), axis, l, a, b);
                    if (!level.hasChunkAt(pos)) return false;
                    BlockEntity entity = level.getBlockEntity(pos);
                    if (!(entity instanceof TieredVaultBlockEntity part) || part.isRemoved()
                        || part.getType() != type || part.getMainConnectionAxis() != axis
                        || !part.getController().equals(root.getBlockPos())) return false;
                }
            }
        }
        return true;
    }
}
