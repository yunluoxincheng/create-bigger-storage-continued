package uwu.lopyluna.create_bs.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uwu.lopyluna.create_bs.CreateBS;
import uwu.lopyluna.create_bs.content.vault.VaultOrientationRecipe;

public final class BSRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CreateBS.MOD_ID);
    public static final RegistryObject<VaultOrientationRecipe.Serializer> ORIENTATION =
        SERIALIZERS.register("vault_orientation", VaultOrientationRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
