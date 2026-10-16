package uwu.lopyluna.create_bs.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import uwu.lopyluna.create_bs.content.vault.SummaryNbtCopy;

@Mixin(value = ItemStack.class, remap = false)
public abstract class ItemStackSummaryCopyMixin {
    // Both names are pinned to MC 1.20.1 official/SRG mappings. No refmap is required.
    // Missing anchors retain native copying rather than replacing ItemStack.copy itself.
    @WrapOperation(method = {"copy()Lnet/minecraft/world/item/ItemStack;", "m_41777_()Lnet/minecraft/world/item/ItemStack;"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;copy()Lnet/minecraft/nbt/CompoundTag;"),
        require = 0, allow = 1)
    private CompoundTag create_bs$copySummaryTagDev(CompoundTag tag, Operation<CompoundTag> original) {
        return create_bs$copySummaryTag(tag, original);
    }

    @WrapOperation(method = {"copy()Lnet/minecraft/world/item/ItemStack;", "m_41777_()Lnet/minecraft/world/item/ItemStack;"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;m_6426_()Lnet/minecraft/nbt/CompoundTag;"),
        require = 0, allow = 1)
    private CompoundTag create_bs$copySummaryTagSrg(CompoundTag tag, Operation<CompoundTag> original) {
        return create_bs$copySummaryTag(tag, original);
    }

    @Unique
    private CompoundTag create_bs$copySummaryTag(CompoundTag tag, Operation<CompoundTag> original) {
        return SummaryNbtCopy.permits((ItemStack) (Object) this, tag)
            ? SummaryNbtCopy.copyAuthorized(tag) : original.call(tag);
    }
}
