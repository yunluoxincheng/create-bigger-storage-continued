package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;
import uwu.lopyluna.create_bs.mixins.InventorySummaryAccessor;

import java.util.ArrayList;
import java.util.List;

public final class VaultInventoryWrapper extends VersionedInventoryWrapper {

    private final TieredVaultBlockEntity owner;
    private InventorySummary extractableSummary;
    private InventorySummary inputSummary;
    private long summaryRevision;

    public VaultInventoryWrapper(IItemHandlerModifiable inventory, TieredVaultBlockEntity owner) {
        super(inventory);
        this.owner = owner;
    }

    public TieredVaultBlockEntity getOwner() { return owner; }

    @Override
    public void incrementVersion() {
        super.incrementVersion();
        VaultDiagnostics.versionIncrement(this);
        clearPackagerSummaries();
    }

    public void invalidatePackagerSummaries() {
        VaultDiagnostics.memberClearSignal(this);
        clearPackagerSummaries();
    }

    private void clearPackagerSummaries() {
        extractableSummary = null;
        inputSummary = null;
        summaryRevision++;
    }

    public InventorySummary getPackagerSummary(boolean scanInputSlots) {
        InventorySummary snapshot = scanInputSlots ? inputSummary : extractableSummary;
        VaultDiagnostics.ModeStats metrics = VaultDiagnostics.sharedRequest(this, scanInputSlots, snapshot != null);
        if (snapshot == null) {
            int version = getVersion();
            long revision = summaryRevision;
            int slots = getSlots();
            long started = metrics == null ? 0 : System.nanoTime();
            boolean completed = false;
            boolean retained = false;
            try {
                snapshot = new InventorySummary();
                for (int slot = 0; slot < slots; slot++) {
                    int limit = getSlotLimit(slot);
                    // Simulated extraction already returns an independent stack for
                    // our ItemStackHandler members; direct reads need a copy too.
                    ItemStack stack = scanInputSlots ? getStackInSlot(slot).copy()
                        : extractItem(slot, limit, true);
                    snapshot.add(stack);
                }
                completed = true;

                // Reentrant item/capability callbacks must not publish an outdated scan.
                if (version == getVersion() && revision == summaryRevision) {
                    if (scanInputSlots)
                        inputSummary = snapshot;
                    else
                        extractableSummary = snapshot;
                    retained = true;
                }
            } finally {
                if (metrics != null)
                    metrics.rebuilt(System.nanoTime() - started, completed ? slots : 0, completed, retained);
            }
        }

        long started = metrics == null ? 0 : System.nanoTime();
        boolean completed = false;
        try {
            InventorySummary result = copySummary(snapshot);
            completed = true;
            return result;
        } finally {
            if (metrics != null) metrics.copied(System.nanoTime() - started, completed);
        }
    }

    private static InventorySummary copySummary(InventorySummary snapshot) {
        InventorySummary copy = new InventorySummary();
        snapshot.getItemMap().forEach((item, entries) -> {
            List<BigItemStack> copiedEntries = new ArrayList<>(entries.size());
            for (BigItemStack entry : entries)
                copiedEntries.add(new BigItemStack(entry.stack.copy(), entry.count));
            copy.getItemMap().put(item, copiedEntries);
        });
        // Groups were already built with Create's NBT/capability-sensitive add().
        // Copy their exact totals without merging/comparing the same entries again.
        ((InventorySummaryAccessor) copy).create_bs$setTotalCount(snapshot.getTotalCount());
        copy.contributingLinks = snapshot.contributingLinks;
        return copy;
    }
}
