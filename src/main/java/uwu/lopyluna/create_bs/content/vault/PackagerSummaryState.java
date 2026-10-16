package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import net.minecraftforge.items.IItemHandler;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics.CopyReason;

import java.util.function.Consumer;

/** Per-machine ownership. Pending snapshots retain no handler, member, world or server. */
public final class PackagerSummaryState {
    private static final ThreadLocal<Permit> PERMIT = new ThreadLocal<>();
    private VaultSummarySnapshot pending;

    public enum Entry { NO_ARGS, BOOLEAN }
    public record Permit(PackagerBlockEntity owner, Entry entry) {}

    public static Permit enter(PackagerBlockEntity owner, Entry entry) {
        return enter(new Permit(owner, entry));
    }

    public static Permit enter(Permit permit) {
        Permit previous = PERMIT.get();
        PERMIT.set(permit);
        return previous;
    }

    public static boolean consume(PackagerBlockEntity owner, Entry entry) {
        Permit permit = PERMIT.get();
        // Consume before any inventory/capability callback can make another query.
        PERMIT.set(null);
        return permit != null && permit.owner == owner && permit.entry == entry;
    }

    public static void restore(Permit previous) {
        // Retain an empty thread entry, not an owner, between calls.
        PERMIT.set(previous);
    }

    public static boolean nativePackager(PackagerBlockEntity owner) {
        return owner.getClass() == PackagerBlockEntity.class || owner.getClass() == RepackagerBlockEntity.class;
    }

    public void prune(InventorySummary current) {
        if (pending != null && pending.summary != current) {
            VaultDiagnostics.deferred(pending.origin, pending.inputSlots, VaultDiagnostics.DeferredEvent.DROP);
            pending = null;
        }
    }

    private void converted(VaultSummarySnapshot snapshot, InventorySummary independent) {
        snapshot.independent = independent;
        if (pending != snapshot) return;
        VaultDiagnostics.deferred(snapshot.origin, snapshot.inputSlots, VaultDiagnostics.DeferredEvent.MATERIALIZE);
        pending = null;
    }

    public InventorySummary forSave(InventorySummary current) {
        prune(current);
        if (pending == null) return current;
        return pending.copy(CopyReason.SAVE, null);
    }

    public boolean borrowed(InventorySummary current) { return pending != null && pending.summary == current; }

    public VaultSummarySnapshot pendingFor(InventorySummary current) {
        prune(current);
        return pending;
    }

    public static boolean borrowed(VaultSummarySnapshot snapshot, InventorySummary current) {
        return snapshot != null && snapshot.summary == current;
    }

    public static boolean sameBaseline(VaultSummarySnapshot snapshot, InventorySummary original,
                                       InventorySummary current) {
        return original == current || (borrowed(snapshot, original) && snapshot.independent != null
            && snapshot.independent == current);
    }

    public void saved(InventorySummary original, InventorySummary independent) {
        if (borrowed(original)) converted(pending, independent);
    }

    public Request request(boolean inputSlots, boolean internal, boolean deferAllowed) {
        return new Request(inputSlots, internal, deferAllowed);
    }

    public static final class Request {
        final boolean inputSlots;
        final boolean internal;
        final boolean deferAllowed;
        private boolean counted;

        private Request(boolean inputSlots, boolean internal, boolean deferAllowed) {
            this.inputSlots = inputSlots;
            this.internal = internal;
            this.deferAllowed = deferAllowed;
        }
    }

    public Query attempt(Request request, VaultSummarySnapshot before) {
        return new Query(request, before);
    }

    public final class Query implements AutoCloseable, Consumer<VaultDiagnostics.CopyEvent> {
        private final Request request;
        private final VaultSummarySnapshot previous;
        private VaultDiagnostics.CopyEvent copies;
        private IItemHandler source;
        private boolean bound;
        private boolean observed;
        private int sourceVersion;
        private VaultInventoryWrapper.SummaryRead read;
        private VaultSummarySnapshot candidate;
        private InventorySummary after;
        private boolean borrowedAfter;
        private boolean effects;
        private boolean convertedBefore;
        private InventorySummary independentBefore;
        private boolean classified;
        private boolean attemptClosed;
        private VaultDiagnostics.ModeStats metrics;

        private Query(Request request, VaultSummarySnapshot previous) {
            this.request = request;
            this.previous = previous;
        }

        public boolean protectedMode(boolean inputSlots) {
            return !effects && request.inputSlots == inputSlots;
        }

        public boolean effectsStarted() { return effects; }
        public boolean controlled() { return source instanceof VaultInventoryWrapper || previous != null; }

        public void observe(IItemHandler inventory, boolean localHit) {
            if (bound && source != inventory && (controlled() || inventory instanceof VaultInventoryWrapper))
                throw new VaultSummarySnapshot.Changed();
            if (!bound) {
                bound = true;
                source = inventory;
                if (inventory instanceof VersionedInventoryWrapper versioned) sourceVersion = versioned.getVersion();
            }
            if (!observed && inventory instanceof VaultInventoryWrapper vault) {
                metrics = VaultDiagnostics.query(vault, request.inputSlots, request.internal, !request.counted, localHit);
                request.counted = true;
                observed = true;
                classified = localHit;
            }
        }

        public InventorySummary prepare(IItemHandler inventory) {
            observe(inventory, false);
            if (!(inventory instanceof VaultInventoryWrapper vault)) return null;
            read = vault.beginSummaryRead(request.inputSlots);
            classified = true;
            candidate = read.snapshot(true);
            borrowedAfter = request.deferAllowed && read.canDefer();
            after = borrowedAfter ? candidate.summary : candidate.copy(
                request.internal ? CopyReason.EAGER_FALLBACK : CopyReason.PUBLIC_FRESH, this);
            read.validate();
            return after;
        }

        public boolean preparedFor(IItemHandler inventory) {
            return candidate != null && source == inventory && after != null;
        }

        @Override
        public void accept(VaultDiagnostics.CopyEvent event) { copies = event.retainAfter(copies); }

        public void validate(InvManipulationBehaviour inventory) {
            if (!controlled()) return;
            IItemHandler current = inventory.getInventory();
            if (bound && source != current) throw new VaultSummarySnapshot.Changed();
            if (source instanceof VersionedInventoryWrapper versioned && versioned.getVersion() != sourceVersion)
                throw new VaultSummarySnapshot.Changed();
            if (read != null) read.validate();
        }

        /** Called after native queue discovery, before even the first before.add. */
        public InventorySummary[] prepareArrivals(InventorySummary before, InventorySummary next,
                                                 InvManipulationBehaviour current) {
            InventorySummary independentBefore = before;
            InventorySummary independentAfter = next;
            if (borrowedAfter && candidate != null && next == candidate.summary)
                independentAfter = candidate.copy(CopyReason.ARRIVAL_AFTER, this);
            // Copy after first: its callbacks may legitimately materialize the old
            // baseline through another mode/save. Preserve that canonical object.
            VaultDiagnostics.CopyEvent beforeCopies = copies;
            if (previous != null && before == previous.summary)
                independentBefore = previous.independent != null ? previous.independent
                    : previous.copy(CopyReason.ARRIVAL_BEFORE, this);
            validate(current);
            if (previous != null && before == previous.summary)
                independentBefore = canonical(previous, independentBefore, beforeCopies);
            convertedBefore = independentBefore != before;
            this.independentBefore = independentBefore;
            after = independentAfter;
            borrowedAfter = false;
            return new InventorySummary[]{independentBefore, independentAfter};
        }

        public void commitArrivals() {
            if (convertedBefore) converted(previous, independentBefore);
            // From here, modifying an externally retained before is already observable.
            effects = true;
            close();
        }

        public InventorySummary arrivalsFinished(InventorySummary next, InvManipulationBehaviour current) {
            if (!effects) validate(current);
            close();
            return after == null ? next : after;
        }

        public InventorySummary expose(InventorySummary result, InvManipulationBehaviour inventory) {
            if (!request.deferAllowed && previous != null && result == previous.summary) {
                // The native hit already observed the exact source. Keep a fallback
                // for wrappers returning a baseline without reaching that hook.
                if (!bound) observe(inventory.getInventory(), true);
                if (!(source instanceof VaultInventoryWrapper vault)) throw new VaultSummarySnapshot.Changed();
                // The native local hit validates the inventory version; idle eviction may
                // have changed the revision since this historical snapshot was created.
                InventorySummary copy = previous.independent;
                VaultDiagnostics.CopyEvent beforeCopies = copies;
                if (copy == null) {
                    read = vault.beginSummaryRead(previous.inputSlots);
                    copy = previous.copy(CopyReason.PUBLIC_LOCAL, this);
                }
                validate(inventory);
                return canonical(previous, copy, beforeCopies);
            }
            return result;
        }

        public void commit(InventorySummary original, InventorySummary current) {
            if (previous != null && original == previous.summary && original != current) converted(previous, current);
            prune(current);
            if (borrowedAfter && candidate != null && current == candidate.summary) {
                if (pending != null && pending.summary == current) return;
                pending = candidate;
                VaultDiagnostics.deferred(candidate.origin, candidate.inputSlots, VaultDiagnostics.DeferredEvent.INSTALL);
            }
        }

        public void discarded() { VaultDiagnostics.CopyEvent.discardSince(copies, null); }

        private InventorySummary canonical(VaultSummarySnapshot snapshot, InventorySummary copy,
                                           VaultDiagnostics.CopyEvent firstCopy) {
            if (snapshot.independent == null || snapshot.independent == copy) return copy;
            // Even resolving the current capability during validation can run a
            // callback. Never replace an independent baseline it already exposed.
            VaultDiagnostics.CopyEvent.discardSince(copies, firstCopy);
            return snapshot.independent;
        }

        public boolean sameBaseline(InventorySummary original, InventorySummary current) {
            return PackagerSummaryState.sameBaseline(previous, original, current);
        }

        @Override
        public void close() {
            if (read != null) read.close();
            if (!attemptClosed && !classified && metrics != null) metrics.incompleteAttempt();
            attemptClosed = true;
        }
    }
}
