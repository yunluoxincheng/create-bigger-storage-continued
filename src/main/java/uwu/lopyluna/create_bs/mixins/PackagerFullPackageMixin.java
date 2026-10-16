package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import uwu.lopyluna.create_bs.content.vault.PackagerScanControl;

import java.util.List;

@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerFullPackageMixin {
    @ModifyVariable(method = "attemptToSend(Ljava/util/List;)V", at = @At("STORE"), ordinal = 0,
        require = 1, allow = 1)
    private IItemHandler create_bs$selectFullPackageScan(
        IItemHandler inventory,
        @Local(argsOnly = true) List<PackagingRequest> requests,
        @Share("create_bs$fullPackageSource") LocalRef<IItemHandler> source
    ) {
        if (PackagerScanControl.permits((PackagerBlockEntity) (Object) this, requests, inventory)) source.set(inventory);
        return inventory;
    }

    @WrapOperation(method = "attemptToSend(Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItemStacked(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"),
        require = 1, allow = 1)
    private ItemStack create_bs$detectFullPackage(
        IItemHandler contents, ItemStack offered, boolean simulate, Operation<ItemStack> original,
        @Share("create_bs$fullPackageSource") LocalRef<IItemHandler> source,
        @Share("create_bs$packageInsertedUnits") LocalIntRef inserted,
        @Share("create_bs$packageFull") LocalBooleanRef full
    ) {
        int count = offered.getCount();
        ItemStack remainder = original.call(contents, offered, simulate);
        if (source.get() == null || simulate || full.get()) return remainder;
        inserted.set(inserted.get() + count - remainder.getCount());
        if (inserted.get() != PackageItem.SLOTS * 64 || contents.getClass() != ItemStackHandler.class
            || contents.getSlots() != PackageItem.SLOTS) return remainder;
        for (int slot = 0; slot < PackageItem.SLOTS; slot++) {
            ItemStack stack = contents.getStackInSlot(slot);
            if (contents.getSlotLimit(slot) != 64 || stack.getCount() != 64
                || !PackagerScanControl.isOrdinaryStack(stack)) return remainder;
        }
        full.set(true);
        return remainder;
    }

    @ModifyExpressionValue(method = "attemptToSend(Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;getSlots()I"),
        require = 1, allow = 1)
    private int create_bs$stopFullPackageScan(
        int slots, @Local IItemHandler inventory,
        @Share("create_bs$fullPackageSource") LocalRef<IItemHandler> source,
        @Share("create_bs$packageFull") LocalBooleanRef full
    ) {
        // The loop condition runs after upstream subtraction and request transitions.
        // Do not cancel the method: metadata, arrivals, animation and publication stay upstream.
        return full.get() && source.get() == inventory ? 0 : slots;
    }
}
