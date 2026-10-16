package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics.CopyReason;
import uwu.lopyluna.create_bs.mixins.InventorySummaryAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A private grouped snapshot, never an independently mutable public summary. */
public final class VaultSummarySnapshot {
    final InventorySummary summary;
    final VaultDiagnostics.Origin origin;
    final boolean inputSlots;
    InventorySummary independent;
    private boolean copying;

    VaultSummarySnapshot(InventorySummary summary, VaultDiagnostics.Origin origin, boolean inputSlots) {
        this.summary = summary;
        this.origin = origin;
        this.inputSlots = inputSlots;
    }

    InventorySummary copy(CopyReason reason, Consumer<VaultDiagnostics.CopyEvent> events) {
        if (copying) throw new IllegalStateException("Reentrant deferred vault summary copy");
        copying = true;
        VaultDiagnostics.CopyEvent event = VaultDiagnostics.copyStarted(origin, inputSlots, reason);
        boolean completed = false;
        int groups = 0;
        try {
            InventorySummary result = new InventorySummary();
            for (var bucket : summary.getItemMap().entrySet()) {
                List<BigItemStack> entries = bucket.getValue();
                List<BigItemStack> copies = new ArrayList<>(entries.size());
                for (BigItemStack entry : entries)
                    copies.add(new BigItemStack(SummaryNbtCopy.copyStack(entry.stack), entry.count));
                result.getItemMap().put(bucket.getKey(), copies);
                groups += entries.size();
            }
            ((InventorySummaryAccessor) result).create_bs$setTotalCount(summary.getTotalCount());
            result.contributingLinks = summary.contributingLinks;
            completed = true;
            return result;
        } finally {
            copying = false;
            if (event != null) {
                event.finished(completed, groups);
                if (completed && events != null) events.accept(event);
            }
        }
    }

    /** Only invalidation before any observable arrival mutation may retry. */
    public static final class Changed extends RuntimeException {
        public Changed() { super(null, null, false, false); }
    }
}
