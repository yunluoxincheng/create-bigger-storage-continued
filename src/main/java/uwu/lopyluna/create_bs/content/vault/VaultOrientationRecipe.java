package uwu.lopyluna.create_bs.content.vault;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import uwu.lopyluna.create_bs.registry.BSRecipes;

/** One-for-one orientation conversion, preserving the input item's complete NBT. */
public class VaultOrientationRecipe extends CustomRecipe {
    private final Item input;
    private final Item output;

    public VaultOrientationRecipe(ResourceLocation id, Item input, Item output) {
        super(id, CraftingBookCategory.MISC);
        this.input = input;
        this.output = output;
    }

    private ItemStack source(CraftingContainer container) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack item = container.getItem(slot);
            if (item.isEmpty()) continue;
            if (!item.is(input) || !found.isEmpty()) return ItemStack.EMPTY;
            found = item;
        }
        return found;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return !source(container).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack original = source(container);
        if (original.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(output);
        if (original.hasTag()) result.setTag(original.getTag().copy());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return new ItemStack(output);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BSRecipes.ORIENTATION.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(input));
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    public static class Serializer implements RecipeSerializer<VaultOrientationRecipe> {
        private static Item item(ResourceLocation id) {
            if (!ForgeRegistries.ITEMS.containsKey(id))
                throw new IllegalArgumentException("Unknown vault conversion item: " + id);
            return ForgeRegistries.ITEMS.getValue(id);
        }

        @Override
        public VaultOrientationRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new VaultOrientationRecipe(id, item(new ResourceLocation(GsonHelper.getAsString(json, "input"))),
                item(new ResourceLocation(GsonHelper.getAsString(json, "output"))));
        }

        @Override
        public VaultOrientationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new VaultOrientationRecipe(id, item(buffer.readResourceLocation()), item(buffer.readResourceLocation()));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, VaultOrientationRecipe recipe) {
            buffer.writeResourceLocation(ForgeRegistries.ITEMS.getKey(recipe.input));
            buffer.writeResourceLocation(ForgeRegistries.ITEMS.getKey(recipe.output));
        }
    }
}
