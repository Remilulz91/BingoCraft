package fr.bingocraft.endless;

import fr.bingocraft.BingoCraft;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Rolls the card-completion reward from the loot table {@code bingocraft:rewards/card_complete}.
 * Data-driven: a datapack can override that file to change the rewards (an empty table gives none).
 * Items go into the inventory; whatever does not fit is dropped at the player's feet.
 */
public final class Rewards {

    public static final RegistryKey<LootTable> CARD_COMPLETE =
            RegistryKey.of(RegistryKeys.LOOT_TABLE, BingoCraft.id("rewards/card_complete"));

    private Rewards() { }

    public static void give(ServerPlayerEntity player) {
        LootTable table = player.server.getReloadableRegistries().getLootTable(CARD_COMPLETE);
        LootContextParameterSet params = new LootContextParameterSet.Builder(player.getServerWorld())
                .add(LootContextParameters.THIS_ENTITY, player)
                .add(LootContextParameters.ORIGIN, player.getPos())
                .luck(player.getLuck())
                .build(LootContextTypes.GIFT);

        List<ItemStack> loot = new ArrayList<>();
        table.generateLoot(params, loot::add);
        if (loot.isEmpty()) return;

        MutableText names = Text.empty();
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (i > 0) names.append(Text.literal(", ").formatted(Formatting.GRAY));
            names.append(Text.literal(stack.getCount() + "x ").formatted(Formatting.WHITE))
                    .append(stack.toHoverableText());
            player.getInventory().insertStack(stack);
            if (!stack.isEmpty()) player.dropItem(stack, false); // inventory full
        }
        player.sendMessage(Text.translatable("bingocraft.endless.reward", names).formatted(Formatting.GOLD), false);
    }
}
