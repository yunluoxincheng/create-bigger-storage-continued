package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.IItemHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uwu.lopyluna.create_bs.content.vault.VaultInventoryWrapper;
import uwu.lopyluna.create_bs.content.vault.PackagerSummaryState;
import uwu.lopyluna.create_bs.content.vault.PackagerSummaryState.Entry;
import uwu.lopyluna.create_bs.content.vault.PackagerSummaryState.Permit;
import uwu.lopyluna.create_bs.content.vault.PackagerSummaryState.Query;
import uwu.lopyluna.create_bs.content.vault.VaultSummarySnapshot;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;

import java.util.List;

@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerBlockEntityMixin {

    @Shadow public InvManipulationBehaviour targetInventory;
    @Shadow private InventorySummary availableItems;
    @Shadow private VersionedInventoryTrackerBehaviour invVersionTracker;
    @Unique private final PackagerSummaryState create_bs$summaryState = new PackagerSummaryState();
    @Unique private Query create_bs$query;
    @Unique private boolean create_bs$forwardStockCheck;
    @Unique private PackagerSummaryState.Request create_bs$request;
    @Unique private VaultSummarySnapshot create_bs$before;
    @Unique private int create_bs$queryFlags;
    // Self-owned immutable tokens add no world root; the thread value is cleared
    // before callbacks and restored to null when the outer invocation finishes.
    @Unique private final Permit create_bs$noArgsPermit = new Permit((PackagerBlockEntity) (Object) this, Entry.NO_ARGS);
    @Unique private final Permit create_bs$booleanPermit = new Permit((PackagerBlockEntity) (Object) this, Entry.BOOLEAN);
    @Unique private static final int create_bs$INPUT = 1, create_bs$INTERNAL = 2,
        create_bs$DEFER = 4, create_bs$PROTECTED = 8;

    @WrapOperation(method = "triggerStockCheck()V",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;getAvailableItems()Lcom/simibubi/create/content/logistics/packager/InventorySummary;"),
        require = 1, allow = 1)
    private InventorySummary create_bs$stockCheckScope(PackagerBlockEntity owner, Operation<InventorySummary> original) {
        Permit previous = PackagerSummaryState.enter(owner == (Object) this
            ? create_bs$noArgsPermit : new Permit(owner, Entry.NO_ARGS));
        try { return original.call(owner); }
        finally { PackagerSummaryState.restore(previous); }
    }

    @WrapMethod(method = "getAvailableItems()Lcom/simibubi/create/content/logistics/packager/InventorySummary;")
    private InventorySummary create_bs$consumeForwardScope(Operation<InventorySummary> original) {
        boolean previous = create_bs$forwardStockCheck;
        create_bs$forwardStockCheck = PackagerSummaryState.consume((PackagerBlockEntity) (Object) this, Entry.NO_ARGS);
        try { return original.call(); }
        finally { create_bs$forwardStockCheck = previous; }
    }

    @WrapOperation(method = "getAvailableItems()Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;"),
        require = 1, allow = 1)
    private InventorySummary create_bs$forwardScope(PackagerBlockEntity owner, boolean input,
                                                    Operation<InventorySummary> original) {
        if (!create_bs$forwardStockCheck) return original.call(owner, input);
        Permit previous = PackagerSummaryState.enter(owner == (Object) this
            ? create_bs$booleanPermit : new Permit(owner, Entry.BOOLEAN));
        try { return original.call(owner, input); }
        finally { PackagerSummaryState.restore(previous); }
    }

    @WrapMethod(method = "getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;")
    private InventorySummary create_bs$summaryQuery(boolean input, Operation<InventorySummary> original) {
        PackagerBlockEntity owner = (PackagerBlockEntity) (Object) this;
        boolean internal = PackagerSummaryState.consume(owner, Entry.BOOLEAN);
        Query previous = create_bs$query;
        if (previous != null ? previous.controlled() && previous.protectedMode(input)
            : (create_bs$queryFlags & create_bs$PROTECTED) != 0
                && ((create_bs$queryFlags & create_bs$INPUT) != 0) == input)
            throw new IllegalStateException("Reentrant vault summary request");
        PackagerSummaryState.Request previousRequest = create_bs$request;
        VaultSummarySnapshot previousBefore = create_bs$before;
        int previousFlags = create_bs$queryFlags;
        create_bs$request = null;
        create_bs$queryFlags = (input ? create_bs$INPUT : 0) | (internal ? create_bs$INTERNAL : 0)
            | (internal && PackagerSummaryState.nativePackager(owner) ? create_bs$DEFER : 0);
        try {
            return create_bs$runSummary(input, original);
        } finally {
            create_bs$query = previous;
            create_bs$request = previousRequest;
            create_bs$before = previousBefore;
            create_bs$queryFlags = previousFlags;
        }
    }

    @Unique
    private InventorySummary create_bs$runSummary(boolean input, Operation<InventorySummary> original) {
        for (int attempt = 0; attempt < 3; attempt++) {
            create_bs$query = null;
            create_bs$before = create_bs$summaryState.pendingFor(availableItems);
            create_bs$queryFlags = (create_bs$queryFlags & ~create_bs$PROTECTED)
                | (create_bs$before != null ? create_bs$PROTECTED : 0);
            try {
                return create_bs$attemptSummary(input, original);
            } catch (VaultSummarySnapshot.Changed changed) {
                if (create_bs$query != null && create_bs$query.effectsStarted())
                    throw new IllegalStateException("Cannot retry applied arrivals", changed);
            }
        }
        throw new IllegalStateException("Vault inventory changed during repeated summary requests");
    }

    @Unique
    private InventorySummary create_bs$attemptSummary(boolean input, Operation<InventorySummary> original) {
        try {
            InventorySummary result = original.call(input);
            Query query = create_bs$query;
            if (query == null) {
                if ((create_bs$queryFlags & create_bs$PROTECTED) != 0
                    && !PackagerSummaryState.sameBaseline(create_bs$before, result, availableItems))
                    throw new IllegalStateException("Packager summary changed during query");
                create_bs$summaryState.prune(availableItems);
                return result;
            }
            return create_bs$finishSummary(query, result);
        } catch (RuntimeException | Error failure) {
            Query query = create_bs$query;
            // A failed copy must not leave the uninstalled after recorded as cached.
            if (query != null && query.controlled() && !query.effectsStarted()) {
                query.discarded();
                invVersionTracker.reset();
            } else if (query == null && (create_bs$queryFlags & create_bs$PROTECTED) != 0) {
                invVersionTracker.reset();
            }
            throw failure;
        } finally {
            if (create_bs$query != null) create_bs$query.close();
        }
    }

    @Unique
    private InventorySummary create_bs$finishSummary(Query query, InventorySummary result) {
        if (!query.controlled()) {
            create_bs$summaryState.prune(availableItems);
            return result;
        }
        InventorySummary exposed = query.expose(result, targetInventory);
        if (!query.sameBaseline(result, availableItems))
            throw new IllegalStateException("Packager summary changed during query");
        availableItems = exposed;
        query.commit(result, exposed);
        return exposed;
    }

    @Unique
    private Query create_bs$requireQuery() {
        if (create_bs$query != null) return create_bs$query;
        int flags = create_bs$queryFlags;
        if (create_bs$request == null)
            create_bs$request = create_bs$summaryState.request((flags & create_bs$INPUT) != 0,
                (flags & create_bs$INTERNAL) != 0, (flags & create_bs$DEFER) != 0);
        return create_bs$query = create_bs$summaryState.attempt(create_bs$request, create_bs$before);
    }

    @WrapOperation(
        method = "getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/inventory/VersionedInventoryTrackerBehaviour;stillWaiting(Lnet/minecraftforge/items/IItemHandler;)Z"),
        require = 1,
        allow = 1
    )
    private boolean create_bs$countLocalCache(VersionedInventoryTrackerBehaviour tracker, IItemHandler inventory,
                                               Operation<Boolean> original) {
        boolean hit = original.call(tracker, inventory);
        if (hit && create_bs$request == null && ((create_bs$queryFlags & create_bs$DEFER) != 0
            || !PackagerSummaryState.borrowed(create_bs$before, availableItems))) {
            if (inventory instanceof VaultInventoryWrapper vault) {
                create_bs$queryFlags |= create_bs$PROTECTED;
                VaultDiagnostics.query(vault, (create_bs$queryFlags & create_bs$INPUT) != 0,
                    (create_bs$queryFlags & create_bs$INTERNAL) != 0, true, true);
            }
        } else if (inventory instanceof VaultInventoryWrapper || create_bs$before != null
            || create_bs$request != null) {
            create_bs$requireQuery().observe(inventory, hit);
        }
        return hit;
    }

    @ModifyVariable(
        method = "getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
        at = @At("STORE"),
        ordinal = 0,
        require = 1,
        allow = 1
    )
    private IItemHandler create_bs$prepareVaultSummary(
        IItemHandler inventory,
        @Local LocalRef<InventorySummary> summary
    ) {
        if (!(inventory instanceof VaultInventoryWrapper) && create_bs$before == null
            && create_bs$request == null) return inventory;
        InventorySummary prepared = create_bs$requireQuery().prepare(inventory);
        if (prepared != null) {
            summary.set(prepared);
        }
        return inventory;
    }

    @ModifyExpressionValue(
        method = "getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;getSlots()I"),
        require = 1,
        allow = 1
    )
    private int create_bs$skipSharedVaultScan(
        int slots,
        @Local IItemHandler inventory
    ) {
        // Only skip the loop for the exact handler whose controlled summary was
        // installed above. Version tracking and submitNewArrivals stay upstream.
        return create_bs$query != null && create_bs$query.preparedFor(inventory) ? 0 : slots;
    }

    @WrapOperation(method = "getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;submitNewArrivals(Lcom/simibubi/create/content/logistics/packager/InventorySummary;Lcom/simibubi/create/content/logistics/packager/InventorySummary;)V"),
        require = 1, allow = 1)
    private void create_bs$submitArrivals(PackagerBlockEntity owner, InventorySummary before, InventorySummary after,
                                          Operation<Void> original, @Local LocalRef<InventorySummary> summary) {
        Query query = create_bs$query;
        original.call(owner, before, after);
        if (query != null) summary.set(query.arrivalsFinished(after, targetInventory));
    }

    @WrapOperation(method = "submitNewArrivals(Lcom/simibubi/create/content/logistics/packager/InventorySummary;Lcom/simibubi/create/content/logistics/packager/InventorySummary;)V",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/InventorySummary;getStacks()Ljava/util/List;", ordinal = 0),
        require = 1, allow = 1)
    private List<BigItemStack> create_bs$separateArrivals(InventorySummary after, Operation<List<BigItemStack>> original,
                                                         @Local(argsOnly = true, ordinal = 0) LocalRef<InventorySummary> before) {
        Query query = create_bs$query;
        if (query == null || !query.controlled()) return original.call(after);
        InventorySummary old = before.get();
        InventorySummary[] prepared = query.prepareArrivals(old, after, targetInventory);
        if (!query.sameBaseline(old, availableItems))
            throw new IllegalStateException("Packager baseline changed during arrival preparation");
        availableItems = prepared[0];
        before.set(prepared[0]);
        query.commitArrivals();
        return original.call(prepared[1]);
    }

    @WrapOperation(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/InventorySummary;write()Lnet/minecraft/nbt/CompoundTag;"),
        require = 1, allow = 1)
    private CompoundTag create_bs$saveBaseline(InventorySummary summary, Operation<CompoundTag> original) {
        InventorySummary independent = create_bs$summaryState.forSave(summary);
        if (independent != summary) {
            if (availableItems != summary) throw new IllegalStateException("Packager baseline changed during save");
            availableItems = independent;
            create_bs$summaryState.saved(summary, independent);
        }
        return original.call(independent);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("RETURN"), require = 2, allow = 2)
    private void create_bs$readBaseline(CompoundTag tag, boolean clientPacket, CallbackInfo callback) {
        create_bs$summaryState.prune(availableItems);
    }

    @Inject(
        method = "attemptToSend(Ljava/util/List;)V",
        at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;heldBox:Lnet/minecraft/world/item/ItemStack;",
            opcode = Opcodes.GETFIELD, ordinal = 1),
        require = 1,
        allow = 1
    )
    private void create_bs$countPreparedPackage(CallbackInfo callback, @Local IItemHandler inventory) {
        if (inventory instanceof VaultInventoryWrapper vault) VaultDiagnostics.packagePrepared(vault);
    }

    @ModifyReturnValue(method = "unwrapBox(Lnet/minecraft/world/item/ItemStack;Z)Z",
        at = @At("RETURN"), require = 3, allow = 3)
    private boolean create_bs$countReceivedPackage(boolean result, @Local(argsOnly = true) boolean simulate) {
        if (result && !simulate) VaultDiagnostics.packageReceived((PackagerBlockEntity) (Object) this);
        return result;
    }
}
