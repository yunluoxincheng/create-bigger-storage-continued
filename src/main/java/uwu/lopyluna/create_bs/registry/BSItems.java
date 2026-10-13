package uwu.lopyluna.create_bs.registry;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import uwu.lopyluna.create_bs.content.vault.UpgradeTier;
import uwu.lopyluna.create_bs.content.vault.VaultUpgradeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uwu.lopyluna.create_bs.CreateBS.REGISTRATE;

public final class BSItems {
    public static final ItemEntry<Item> BASE_COMPONENT = component("base_component", "Base Component");
    public static final List<ItemEntry<Item>> COMPONENTS;
    public static final List<ItemEntry<VaultUpgradeItem>> UPGRADES;
    static {
        List<ItemEntry<Item>> components = new ArrayList<>();
        components.add(BASE_COMPONENT);
        for (UpgradeTier tier : UpgradeTier.values())
            components.add(component(tier.id + "_component", tier.englishName + " Component"));
        COMPONENTS = Collections.unmodifiableList(components);

        List<ItemEntry<VaultUpgradeItem>> upgrades = new ArrayList<>();
        for (UpgradeTier from : UpgradeTier.values()) {
            for (UpgradeTier to : UpgradeTier.values()) {
                if (from.ordinal() >= to.ordinal()) continue;
                upgrades.add(REGISTRATE.item(from.id + "_to_" + to.id + "_upgrade",
                        p -> new VaultUpgradeItem(p, from, to))
                    .lang(from.englishName + " to " + to.englishName + " Upgrade")
                    .model((c, p) -> p.withExistingParent(c.getName(), "minecraft:item/generated")
                        .texture("layer0", "create_bs:item/" + c.getName()))
                    .register());
            }
        }
        UPGRADES = Collections.unmodifiableList(upgrades);
    }

    private static ItemEntry<Item> component(String id, String name) {
        return REGISTRATE.item(id, Item::new)
            .lang(name)
            .model((c, p) -> p.withExistingParent(c.getName(), "minecraft:item/generated")
                .texture("layer0", "create_bs:item/" + id))
            .register();
    }

    public static void register() {}
}
