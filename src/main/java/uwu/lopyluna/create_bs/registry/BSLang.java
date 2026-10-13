package uwu.lopyluna.create_bs.registry;

import uwu.lopyluna.create_bs.content.vault.UpgradeTier;

import static uwu.lopyluna.create_bs.CreateBS.REGISTRATE;

public final class BSLang {
    public static void register() {
        for (UpgradeTier tier : UpgradeTier.values())
            REGISTRATE.addRawLang("create_bs.tier." + tier.id, tier.englishName);
        REGISTRATE.addRawLang("create_bs.upgrade.tooltip", "%s → %s");
        REGISTRATE.addRawLang("create_bs.upgrade.whole_structure", "Right-click any member to upgrade the complete structure.");
        REGISTRATE.addRawLang("create_bs.upgrade.one_per_block", "Consumes one matching component per block from your inventory.");
        REGISTRATE.addRawLang("create_bs.upgrade.success", "Upgraded %s blocks; consumed %s components.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.unavailable", "This upgrade is unavailable right now.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.structure", "The source vault structure is incomplete or does not match this component.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.connected", "The standard vertical target requires Create: Connected.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.geometry", "The target tier cannot retain this structure's dimensions.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.access", "All members must be loaded and accessible.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.capacity", "The actual target capacity is too small; nothing was changed.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.components", "This complete structure requires %s matching components.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.prepare", "Could not prepare the upgrade; inventory was not changed.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.rollback", "The upgrade failed and the original vault was restored.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.recovery", "Rollback was incomplete. Original data was saved in the upgrade recovery record.");
        REGISTRATE.addRawLang("create_bs.upgrade.error.recovery_unsaved", "Rollback and recovery storage failed. Stop using this structure and check the server log.");
    }
}
