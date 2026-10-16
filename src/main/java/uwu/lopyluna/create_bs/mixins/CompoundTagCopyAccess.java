package uwu.lopyluna.create_bs.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(value = CompoundTag.class, remap = false)
public interface CompoundTagCopyAccess {
    @Invoker("<init>")
    static CompoundTag create_bs$newCompound(Map<String, Tag> entries) {
        throw new AssertionError("Mixin constructor invoker not applied");
    }
}
