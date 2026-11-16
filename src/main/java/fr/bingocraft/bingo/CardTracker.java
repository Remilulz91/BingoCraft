package fr.bingocraft.bingo;

import fr.bingocraft.config.BingoCraftConfig;
import fr.bingocraft.screen.CardScreenHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;

/**
 * Ticks the cards: every few ticks, each online player's inventory (main, armor, off-hand and
 * the stack held on the cursor) is compared with their card. A goal counts the moment its item
 * is in the inventory, however it got there (picked up, crafted, smelted, traded...). The item
 * is not consumed and the cell stays done even if the item is dropped later.
 */
public final class CardTracker {

    private static int tickCounter = 0;

    private CardTracker() { }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < Math.max(1, BingoCraftConfig.get().checkIntervalTicks)) return;
            tickCounter = 0;
            checkAll(server);
        });
    }

    private static void checkAll(MinecraftServer server) {
        BingoState state = BingoState.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BingoCard card = state.get(player.getUuid());
            if (card == null || card.isComplete()) continue;

            Set<Item> owned = ownedItems(player);
            for (int i = 0; i < card.cellCount(); i++) {
                Item item = card.item(i);
                if (!card.isDone(i) && item != null && owned.contains(item)) {
                    complete(player, state, card, i);
                }
            }
        }
    }

    private static Set<Item> ownedItems(ServerPlayerEntity player) {
        Set<Item> owned = new HashSet<>();
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty()) owned.add(stack.getItem());
        }
        ItemStack cursor = player.currentScreenHandler.getCursorStack();
        if (!cursor.isEmpty()) owned.add(cursor.getItem());
        return owned;
    }

    /**
     * Marks a cell done and tells the player. Also used by /bingo debug complete, so a forced
     * cell behaves exactly like a real one.
     */
    public static void complete(ServerPlayerEntity player, BingoState state, BingoCard card, int index) {
        if (!state.markDone(player.getUuid(), index)) return;

        int done = card.completedCount();
        int total = card.cellCount();
        player.sendMessage(Text.translatable("bingocraft.goal.done",
                        card.goalName(index).copy().formatted(Formatting.WHITE),
                        done, total)
                .formatted(Formatting.GREEN), false);

        CardScreenHandler.refresh(player, card);

        if (card.isComplete()) {
            player.server.getPlayerManager().broadcast(
                    Text.translatable("bingocraft.card.complete", player.getDisplayName())
                            .formatted(Formatting.GOLD, Formatting.BOLD), false);
            player.getWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            player.getWorld().playSound(null, player.getBlockPos(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
        }
    }
}
