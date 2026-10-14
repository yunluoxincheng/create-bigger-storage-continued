package uwu.lopyluna.create_bs.mixins;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = InventorySummary.class, remap = false)
public interface InventorySummaryAccessor {

    @Accessor("totalCount")
    void create_bs$setTotalCount(int count);
}
