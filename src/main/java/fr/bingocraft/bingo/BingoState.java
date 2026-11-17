package fr.bingocraft.bingo;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every player's card and number of completed cards, saved with the world (data/bingocraft_cards.dat in the overworld folder).
 * All changes go through this class so the save is always marked dirty.
 */
public class BingoState extends PersistentState {

    private static final String STATE_KEY = "bingocraft_cards";

    private final Map<UUID, BingoCard> cards = new HashMap<>();
    private final Map<UUID, Integer> completedCards = new HashMap<>();

    public BingoCard get(UUID player) {
        return cards.get(player);
    }

    public void put(UUID player, BingoCard card) {
        cards.put(player, card);
        markDirty();
    }

    public boolean remove(UUID player) {
        boolean removed = cards.remove(player) != null;
        if (removed) markDirty();
        return removed;
    }

    /**
     * Marks a cell of a player's card as done. Returns false if it already was, or if
     * {@code card} is no longer that player's current card.
     */
    public boolean markDone(UUID player, BingoCard card, int index) {
        if (cards.get(player) != card || !card.markDone(index)) return false;
        markDirty();
        return true;
    }

    /** How many cards this player has completed in this world. */
    public int completedCards(UUID player) {
        return completedCards.getOrDefault(player, 0);
    }

    /** Counts one more completed card; returns the new total. */
    public int addCompletedCard(UUID player) {
        int total = completedCards(player) + 1;
        completedCards.put(player, total);
        markDirty();
        return total;
    }

    // === Persistence ===

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        Set<UUID> players = new HashSet<>(cards.keySet());
        players.addAll(completedCards.keySet());
        for (UUID player : players) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("player", player);
            BingoCard card = cards.get(player);
            if (card != null) tag.put("card", card.toNbt());
            tag.putInt("completed", completedCards(player));
            list.add(tag);
        }
        nbt.put("cards", list);
        return nbt;
    }

    public static BingoState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        BingoState state = new BingoState();
        NbtList list = nbt.getList("cards", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            if (!tag.containsUuid("player")) continue;
            UUID player = tag.getUuid("player");
            if (tag.contains("card")) {
                BingoCard card = BingoCard.fromNbt(tag.getCompound("card"));
                if (card != null) state.cards.put(player, card);
            }
            if (tag.getInt("completed") > 0) state.completedCards.put(player, tag.getInt("completed"));
        }
        return state;
    }

    private static final PersistentState.Type<BingoState> TYPE = new PersistentState.Type<>(
            BingoState::new,
            BingoState::fromNbt,
            DataFixTypes.LEVEL
    );

    public static BingoState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded yet");
        }
        PersistentStateManager psm = overworld.getPersistentStateManager();
        return psm.getOrCreate(TYPE, STATE_KEY);
    }
}
