package fr.bingocraft.bingo;

import fr.bingocraft.BingoCraft;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * Which items a card may ask for: every item of the game, vanilla and modded, except
 * <ul>
 *   <li>the item tag {@code #bingocraft:excluded} — items a survival player cannot obtain
 *       (bedrock, command blocks, spawn eggs...). Datapacks and modpacks add their own
 *       creative-only items to this tag;</li>
 *   <li>items of experimental features that are not enabled in this world.</li>
 * </ul>
 * Pure random: no difficulty tiers, a card can be trivial or brutal.
 */
public final class GoalFilter {

    public static final TagKey<Item> EXCLUDED = TagKey.of(RegistryKeys.ITEM, BingoCraft.id("excluded"));

    private GoalFilter() { }

    public static boolean isEligible(Item item, FeatureSet enabledFeatures) {
        return item != Items.AIR
                && item.isEnabled(enabledFeatures)
                && !item.getDefaultStack().isIn(EXCLUDED);
    }

    /** Every item a card can currently ask for, in registry order. */
    public static List<Item> candidates(MinecraftServer server) {
        FeatureSet features = server.getOverworld().getEnabledFeatures();
        return Registries.ITEM.stream()
                .filter(item -> isEligible(item, features))
                .toList();
    }
}
