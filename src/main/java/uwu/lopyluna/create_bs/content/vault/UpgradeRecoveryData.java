package uwu.lopyluna.create_bs.content.vault;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Retains inventory snapshots only if an exceptional upgrade rollback cannot complete. */
public class UpgradeRecoveryData extends SavedData {
    private ListTag entries = new ListTag();

    public static UpgradeRecoveryData load(CompoundTag tag) {
        UpgradeRecoveryData data = new UpgradeRecoveryData();
        data.entries = tag.getList("Entries", Tag.TAG_COMPOUND).copy();
        return data;
    }

    public static void preserve(ServerLevel level, CompoundTag snapshot) {
        UpgradeRecoveryData data = level.getDataStorage().computeIfAbsent(
            UpgradeRecoveryData::load, UpgradeRecoveryData::new, "create_bs_upgrade_recovery");
        data.entries.add(snapshot.copy());
        data.setDirty();
        level.getDataStorage().save();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("Entries", entries);
        return tag;
    }
}
