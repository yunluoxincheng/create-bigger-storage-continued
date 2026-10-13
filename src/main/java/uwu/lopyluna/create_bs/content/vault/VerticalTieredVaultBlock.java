package uwu.lopyluna.create_bs.content.vault;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import uwu.lopyluna.create_bs.content.TierMaterials;

public class VerticalTieredVaultBlock extends TieredVaultBlock {
    public VerticalTieredVaultBlock(Properties properties, TierMaterials tier) {
        super(properties, tier);
    }

    @Override
    public boolean isVertical() {
        return true;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // This is a separate orientation variant; ordinary wrenching does not rotate its axis.
        return InteractionResult.PASS;
    }
}
