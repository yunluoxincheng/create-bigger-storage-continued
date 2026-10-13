package uwu.lopyluna.create_bs.content.vault;

import com.simibubi.create.AllBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import uwu.lopyluna.create_bs.content.TierMaterials;
import uwu.lopyluna.create_bs.registry.BSBlocks;

/** Upgrade ordering follows the upstream per-block storage capacities. */
public enum UpgradeTier {
    WOODEN("wooden", "Wooden", TierMaterials.WOOD),
    STANDARD("standard", "Standard", null),
    COPPER("copper", "Copper", TierMaterials.COPPER),
    IRON("iron", "Iron", TierMaterials.IRON),
    EMERALD("emerald", "Emerald", TierMaterials.EMERALD),
    GOLD("gold", "Gold", TierMaterials.GOLD),
    CRYSTAL("crystal", "Crystal", TierMaterials.CRYSTAL),
    DIAMOND("diamond", "Diamond", TierMaterials.DIAMOND),
    OBSIDIAN("obsidian", "Obsidian", TierMaterials.OBSIDIAN),
    NETHERITE("netherite", "Netherite", TierMaterials.NETHERITE);

    public static final ResourceLocation SILO = new ResourceLocation("create_connected", "item_silo");
    public final String id;
    public final String englishName;
    public final TierMaterials material;

    UpgradeTier(String id, String englishName, TierMaterials material) {
        this.id = id;
        this.englishName = englishName;
        this.material = material;
    }

    public Component displayName() {
        return Component.translatable("create_bs.tier." + id);
    }

    public boolean matches(BlockState state) {
        if (material != null) return TieredVaultBlock.isVault(state, material);
        return AllBlocks.ITEM_VAULT.has(state) || SILO.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    public Block target(boolean vertical) {
        if (material != null) return (vertical ? BSBlocks.VERTICAL_VAULTS : BSBlocks.VAULTS).get(material).get();
        if (!vertical) return AllBlocks.ITEM_VAULT.get();
        return ForgeRegistries.BLOCKS.containsKey(SILO) ? ForgeRegistries.BLOCKS.getValue(SILO) : null;
    }
}
