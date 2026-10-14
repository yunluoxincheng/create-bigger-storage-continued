package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;
import uwu.lopyluna.create_bs.mixins.InventorySummaryAccessor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class VaultInventoryWrapper extends VersionedInventoryWrapper {

    private static final long IDLE_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final int IDLE_CHECK_TICKS = 100;
    private static final int MAX_QUERY_ATTEMPTS = 3;

    private final TieredVaultBlockEntity owner;
    private final IItemHandlerModifiable inventory;
    private TieredVaultBlockEntity[] members;
    private LazyOptional<IItemHandler> capability;
    private final ModeState extractable = new ModeState();
    private final ModeState input = new ModeState();
    private long summaryRevision;
    private long memberChanges;
    private int idleCheckTicks = IDLE_CHECK_TICKS;
    private boolean retired;

    public VaultInventoryWrapper(IItemHandlerModifiable inventory, TieredVaultBlockEntity owner,
                                 TieredVaultBlockEntity[] members) {
        super(inventory);
        this.inventory = inventory;
        this.owner = owner;
        this.members = members.clone();
        int offset = 0;
        for (TieredVaultBlockEntity member : this.members) {
            member.trackInventory(this, offset);
            offset += member.getInventoryOfBlock().getSlots();
        }
    }

    public TieredVaultBlockEntity getOwner() { return owner; }

    void trackCapability(LazyOptional<IItemHandler> capability) {
        this.capability = capability;
        capability.addListener(ignored -> retire());
    }

    @Override
    public void incrementVersion() {
        advanceVersion();
        // An unattributed version signal cannot safely retain slot contributions.
        clearPackagerSummaries();
    }

    public void invalidatePackagerSummaries() {
        VaultDiagnostics.memberClearSignal(this);
        advanceVersion();
        clearPackagerSummaries();
    }

    private void advanceVersion() {
        super.incrementVersion();
        VaultDiagnostics.versionIncrement(this);
    }

    void memberChanged(int slot) {
        if (retired) return;
        if (slot < 0 || slot >= getSlots()) {
            incrementVersion();
            return;
        }
        memberChanges++;
        summaryRevision++;
        if (extractable.cache != null) extractable.cache.dirty.set(slot);
        if (input.cache != null) input.cache.dirty.set(slot);
        // Direct member writes must invalidate Create's local cache immediately too.
        advanceVersion();
        VaultDiagnostics.memberSlotChange(this);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        int count = stack.getCount();
        long changes = memberChanges;
        ItemStack remainder = inventory.insertItem(slot, stack, simulate);
        if (!simulate && count != remainder.getCount() && changes == memberChanges)
            incrementVersion();
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        long changes = memberChanges;
        ItemStack result = inventory.extractItem(slot, amount, simulate);
        if (!simulate && !result.isEmpty() && changes == memberChanges)
            incrementVersion();
        return result;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        ItemStack previous = inventory.getStackInSlot(slot);
        long changes = memberChanges;
        inventory.setStackInSlot(slot, stack);
        if (changes == memberChanges && (stack.isEmpty() != previous.isEmpty()
            || (!stack.isEmpty() && (!ItemHandlerHelper.canItemStacksStack(stack, previous)
                || stack.getCount() != previous.getCount()))))
            incrementVersion();
    }

    void retire() {
        if (retired) return;
        retired = true;
        advanceVersion();
        clearPackagerSummaries();
        for (TieredVaultBlockEntity member : members) member.untrackInventory(this);
        members = null;
        // Invalidate only this wrapper's optional, never a replacement capability.
        LazyOptional<IItemHandler> previous = capability;
        capability = null;
        if (previous != null) previous.invalidate();
        owner.markInventoryCapabilityDirty();
    }

    private void clearPackagerSummaries() {
        extractable.cache = null;
        input.cache = null;
        summaryRevision++;
    }

    void expireIdleSummaries() {
        if (retired || (extractable.cache == null && input.cache == null)) return;
        if (--idleCheckTicks > 0) return;
        idleCheckTicks = IDLE_CHECK_TICKS;
        long now = System.nanoTime();
        expire(extractable, now);
        expire(input, now);
    }

    private void expire(ModeState mode, long now) {
        if (mode.cache == null || mode.querying || now - mode.lastRequestNanos < IDLE_NANOS) return;
        mode.cache = null;
        summaryRevision++;
        VaultDiagnostics.idleRelease(this);
    }

    public InventorySummary getPackagerSummary(boolean scanInputSlots) {
        ModeState mode = scanInputSlots ? input : extractable;
        if (mode.querying) throw new IllegalStateException("Reentrant vault summary request");
        mode.lastRequestNanos = System.nanoTime();
        ContributionCache initial = mode.cache;
        VaultDiagnostics.ModeStats metrics = VaultDiagnostics.sharedRequest(this, scanInputSlots,
            initial != null && initial.dirty.isEmpty());
        mode.querying = true;
        try {
            for (int attempt = 0; attempt < MAX_QUERY_ATTEMPTS; attempt++) {
                int version = getVersion();
                long revision = summaryRevision;
                boolean wasRetired = retired;
                ContributionCache base = mode.cache;
                InventorySummary snapshot;
                if (base == null || !base.dirty.isEmpty()) {
                    int dirtyCount = base == null ? 0 : base.dirty.cardinality();
                    boolean incremental = base != null && dirtyCount < getSlots();
                    long started = metrics == null ? 0 : System.nanoTime();
                    BuildResult result = null;
                    boolean retained = false;
                    try {
                        result = incremental ? rebuildDirty(base, scanInputSlots, metrics != null)
                            : rebuildAll(scanInputSlots, !retired, metrics != null, dirtyCount);
                        if (version == getVersion() && revision == summaryRevision && !retired) {
                            result.commit();
                            mode.cache = result.cache;
                            retained = true;
                        }
                    } finally {
                        if (metrics != null)
                            metrics.rebuilt(System.nanoTime() - started, result == null ? 0 : result.scanned,
                                result != null, retained, incremental, result == null ? 0 : result.dirty,
                                result == null ? 0 : result.nonEmpty, result == null ? 0 : result.buckets,
                                result == null ? 0 : result.groups, getSlots());
                    }
                    if (version != getVersion() || revision != summaryRevision || retired != wasRetired)
                        continue;
                    snapshot = result.cache.summary;
                } else {
                    snapshot = base.summary;
                }

                long started = metrics == null ? 0 : System.nanoTime();
                boolean copied = false;
                InventorySummary result;
                try {
                    result = copySummary(snapshot);
                    copied = true;
                } finally {
                    if (metrics != null) metrics.copied(System.nanoTime() - started, copied);
                }
                // Capability initialization during copying can mutate inventory too.
                if (version == getVersion() && revision == summaryRevision && retired == wasRetired)
                    return result;
            }
            throw new IllegalStateException("Vault inventory changed during repeated summary requests");
        } finally {
            mode.querying = false;
        }
    }

    private ItemStack readContribution(int slot, boolean scanInputSlots) {
        int limit = getSlotLimit(slot);
        // Our members return independent stacks for simulated extraction.
        return scanInputSlots ? getStackInSlot(slot).copy() : extractItem(slot, limit, true);
    }

    private BuildResult rebuildAll(boolean inputSlots, boolean cacheContributions, boolean measureGroups,
                                   int dirtySlots) {
        int slots = getSlots();
        ItemStack[] contributions = cacheContributions ? new ItemStack[slots] : null;
        BitSet nonEmpty = new BitSet();
        InventorySummary summary = new InventorySummary();
        int count = 0;
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = readContribution(slot, inputSlots);
            summary.add(stack);
            if (stack.isEmpty()) continue;
            count++;
            if (contributions != null) {
                contributions[slot] = stack;
                nonEmpty.set(slot);
            }
        }
        return new BuildResult(new ContributionCache(contributions, nonEmpty, summary), slots, dirtySlots,
            count, summary.getItemMap().size(), measureGroups ? groupCount(summary) : 0, null, null);
    }

    private BuildResult rebuildDirty(ContributionCache base, boolean inputSlots, boolean measureGroups) {
        BitSet dirty = (BitSet) base.dirty.clone();
        int count = dirty.cardinality();
        int[] slots = new int[count];
        ItemStack[] updates = new ItemStack[count];
        Set<Item> affected = Collections.newSetFromMap(new IdentityHashMap<>());
        BitSet nonEmpty = (BitSet) base.nonEmpty.clone();
        int index = 0;
        for (int slot = dirty.nextSetBit(0); slot >= 0; slot = dirty.nextSetBit(slot + 1)) {
            ItemStack old = base.contributions[slot];
            ItemStack next = readContribution(slot, inputSlots);
            slots[index] = slot;
            updates[index++] = next.isEmpty() ? null : next;
            if (old != null) affected.add(old.getItem());
            if (!next.isEmpty()) affected.add(next.getItem());
            nonEmpty.set(slot, !next.isEmpty());
        }

        InventorySummary summary = new InventorySummary();
        int total = 0;
        int visited = 0;
        index = 0;
        for (int slot = nonEmpty.nextSetBit(0); slot >= 0; slot = nonEmpty.nextSetBit(slot + 1)) {
            while (index < slots.length && slots[index] < slot) index++;
            ItemStack stack = index < slots.length && slots[index] == slot ? updates[index]
                : base.contributions[slot];
            visited++;
            // Preserve Create's threshold-before-add rule and integer arithmetic.
            if (total < 1_000_000_000) total += stack.getCount();
            if (affected.contains(stack.getItem())) summary.add(stack);
        }
        int groups = measureGroups ? groupCount(summary) : 0;
        base.summary.getItemMap().forEach((item, entries) -> {
            if (!affected.contains(item)) summary.getItemMap().put(item, entries);
        });
        ((InventorySummaryAccessor) summary).create_bs$setTotalCount(total);
        summary.contributingLinks = base.summary.contributingLinks;
        // The old arrays and buckets remain untouched until version validation.
        return new BuildResult(new ContributionCache(base.contributions, nonEmpty, summary), count, count,
            visited, affected.size(), groups, slots, updates);
    }

    private static int groupCount(InventorySummary summary) {
        int count = 0;
        for (List<BigItemStack> entries : summary.getItemMap().values()) count += entries.size();
        return count;
    }

    private static final class ModeState {
        private ContributionCache cache;
        private long lastRequestNanos;
        private boolean querying;
    }

    private static final class ContributionCache {
        private final ItemStack[] contributions;
        private final BitSet nonEmpty;
        private final BitSet dirty = new BitSet();
        private final InventorySummary summary;

        private ContributionCache(ItemStack[] contributions, BitSet nonEmpty, InventorySummary summary) {
            this.contributions = contributions;
            this.nonEmpty = nonEmpty;
            this.summary = summary;
        }
    }

    private record BuildResult(ContributionCache cache, int scanned, int dirty, int nonEmpty,
                               int buckets, int groups, int[] changedSlots, ItemStack[] changes) {
        private void commit() {
            if (changedSlots == null) return;
            for (int i = 0; i < changedSlots.length; i++) cache.contributions[changedSlots[i]] = changes[i];
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
