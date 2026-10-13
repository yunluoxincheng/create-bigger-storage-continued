package uwu.lopyluna.create_bs.content.vault;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VaultUpgradeItem extends Item {
    public final UpgradeTier from;
    public final UpgradeTier to;

    public VaultUpgradeItem(Properties properties, UpgradeTier from, UpgradeTier to) {
        super(properties);
        if (from.ordinal() >= to.ordinal()) throw new IllegalArgumentException("Upgrades must increase tier");
        this.from = from;
        this.to = to;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!from.matches(context.getLevel().getBlockState(context.getClickedPos())))
            return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        return VaultUpgradeService.upgrade(context, this);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("create_bs.upgrade.tooltip", from.displayName(), to.displayName())
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("create_bs.upgrade.whole_structure").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("create_bs.upgrade.one_per_block").withStyle(ChatFormatting.YELLOW));
    }
}
