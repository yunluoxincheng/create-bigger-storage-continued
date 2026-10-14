package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import uwu.lopyluna.create_bs.diagnostics.VaultDiagnostics;

@Mixin(value = PackagerItemHandler.class, remap = false)
public abstract class PackagerItemHandlerMixin {
    @Shadow private PackagerBlockEntity blockEntity;

    @ModifyReturnValue(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"), require = 2, allow = 2)
    private ItemStack create_bs$countTakenPackage(ItemStack result, @Local(argsOnly = true) boolean simulate) {
        if (!simulate && !result.isEmpty()) VaultDiagnostics.packagesTaken(blockEntity, result.getCount());
        return result;
    }
}
