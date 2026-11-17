package fr.bingocraft.endless;

import fr.bingocraft.bingo.BingoCard;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.bingo.CardGenerator;
import fr.bingocraft.bingo.CardTracker;
import fr.bingocraft.config.BingoCraftConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/**
 * Endless survival bingo: every player always has a card. Completing it gives a reward
 * (loot table {@code bingocraft:rewards/card_complete}) and deals a new card right away,
 * forever. Solo only: each player plays their own card.
 *
 * Players without a card (first join, or the mode just turned on) are dealt one by
 * {@link CardTracker} on its next check.
 */
public final class EndlessMode {

    private static final Random RANDOM = new Random();

    private EndlessMode() { }

    /**
     * Deals the player a new card, leaving out everything they are carrying so it starts empty.
     * Returns the card, or null if not enough items are eligible.
     */
    public static BingoCard deal(ServerPlayerEntity player, BingoState state) {
        BingoCard card = CardGenerator.generate(BingoCraftConfig.get().cardSize, player.server, RANDOM,
                CardTracker.ownedItems(player));
        if (card == null) return null;
        state.put(player.getUuid(), card);
        player.sendMessage(Text.translatable("bingocraft.endless.new_card")
                .formatted(Formatting.AQUA)
                .append(Text.literal(" "))
                .append(Text.translatable("bingocraft.endless.open").styled(style -> style
                        .withColor(Formatting.YELLOW)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo card"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("/bingo card"))))),
                false);
        return card;
    }

    /** The player's card is complete: reward first, then the next card (which skips the reward items). */
    public static void onCardComplete(ServerPlayerEntity player, BingoState state) {
        Rewards.give(player);
        deal(player, state);
    }
}
