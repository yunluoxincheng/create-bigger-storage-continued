package uwu.lopyluna.create_bs.mixins;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(value = ListTag.class, remap = false)
public interface ListTagCopyAccess {
    @Invoker("<init>")
    static ListTag create_bs$newList(List<Tag> entries, byte type) {
        throw new AssertionError("Mixin constructor invoker not applied");
    }
}
