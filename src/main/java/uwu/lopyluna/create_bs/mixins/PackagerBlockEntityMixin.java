package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.minecraftforge.items.IItemHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uwu.lopyluna.create_bs.content.vault.VaultInventoryWrapper;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;

@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerBlockEntityMixin {

    @Shadow public InvManipulationBehaviour targetInventory;

    @ModifyExpressionValue(
        method = "getAvailableItems(Z)Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/inventory/VersionedInventoryTrackerBehaviour;stillWaiting(Lnet/minecraftforge/items/IItemHandler;)Z"),
        require = 1,
        allow = 1
    )
    private boolean create_bs$countLocalCache(
        boolean hit,
        @Local(argsOnly = true) boolean scanInputSlots,
        @Share("create_bs$diagnosticsQuery") LocalRef<VaultDiagnostics.ModeStats> countedQuery
    ) {
        if (VaultDiagnostics.isRecording()
            && targetInventory.getInventory() instanceof VaultInventoryWrapper vault)
            countedQuery.set(VaultDiagnostics.localCacheQuery(vault, scanInputSlots, hit));
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
        @Local(argsOnly = true) boolean scanInputSlots,
        @Local LocalRef<InventorySummary> summary,
        @Share("create_bs$summaryInventory") LocalRef<IItemHandler> summaryInventory,
        @Share("create_bs$diagnosticsQuery") LocalRef<VaultDiagnostics.ModeStats> countedQuery
    ) {
        if (inventory instanceof VaultInventoryWrapper vault) {
            VaultDiagnostics.queryAfterCacheMiss(vault, scanInputSlots, countedQuery.get());
            summary.set(vault.getPackagerSummary(scanInputSlots));
            summaryInventory.set(inventory);
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
        @Local IItemHandler inventory,
        @Share("create_bs$summaryInventory") LocalRef<IItemHandler> summaryInventory
    ) {
        // Only skip the loop for the exact handler whose independent summary was
        // installed above. Version tracking and submitNewArrivals stay upstream.
        return inventory == summaryInventory.get() ? 0 : slots;
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
