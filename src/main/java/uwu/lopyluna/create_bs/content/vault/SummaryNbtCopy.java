package uwu.lopyluna.create_bs.content.vault;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import uwu.lopyluna.create_bs.mixins.CompoundTagCopyAccess;
import uwu.lopyluna.create_bs.mixins.ListTagCopyAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Invocation-local authorization for the ordinary tag inside native ItemStack.copy(). */
public final class SummaryNbtCopy {
    private static final ThreadLocal<ItemStack> SOURCE = new ThreadLocal<>();

    private SummaryNbtCopy() {}

    public static ItemStack copyStack(ItemStack source) {
        if (source.getClass() != ItemStack.class || source.getTag() == null) return source.copy();
        ItemStack previous = SOURCE.get();
        SOURCE.set(source);
        try {
            return source.copy();
        } finally {
            SOURCE.set(previous);
        }
    }

    public static boolean permits(ItemStack source, CompoundTag tag) {
        return SOURCE.get() == source && source.getTag() == tag;
    }

    public static CompoundTag copyAuthorized(CompoundTag tag) {
        ItemStack previous = SOURCE.get();
        // Custom Tag.copy callbacks must not inherit the outer source's authorization.
        SOURCE.set(null);
        try {
            return (CompoundTag) copyTree(tag);
        } finally {
            SOURCE.set(previous);
        }
    }

    private static Tag copyTree(Tag tag) {
        if (tag.getClass() == CompoundTag.class) {
            CompoundTag compound = (CompoundTag) tag;
            int size = compound.size();
            Map<String, Tag> entries = size == 0 ? new HashMap<>()
                : new HashMap<>((int) Math.min(size / 0.75f + 1, 1 << 30));
            for (String key : compound.getAllKeys()) entries.put(key, copyTree(compound.get(key)));
            return CompoundTagCopyAccess.create_bs$newCompound(entries);
        }
        if (tag.getClass() == ListTag.class) {
            ListTag list = (ListTag) tag;
            byte type = list.getElementType();
            ArrayList<Tag> entries = new ArrayList<>(list.size());
            if (TagTypes.getType(type).isValue()) entries.addAll(list);
            else for (Tag entry : list) entries.add(copyTree(entry));
            return ListTagCopyAccess.create_bs$newList(entries, type);
        }
        return tag.copy();
    }
}
