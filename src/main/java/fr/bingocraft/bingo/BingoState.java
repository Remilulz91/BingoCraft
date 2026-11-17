package fr.bingocraft.bingo;

import fr.bingocraft.race.Race;
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
 * Every player's card and number of completed cards, and the race in progress, saved with the world (data/bingocraft_cards.dat in the overworld folder).
 * All changes go through this class so the save is always marked dirty.
 */
public class BingoState extends PersistentState {

    private static final String STATE_KEY = "bingocraft_cards";

    private final Map<UUID, BingoCard> cards = new HashMap<>();
    private final Map<UUID, Integer> completedCards = new HashMap<>();

    private BingoMode mode = BingoMode.NONE;

    public BingoMode mode() {
        return mode;
    }

    public void setMode(BingoMode mode) {
        this.mode = mode;
        markDirty();
    }

    /** Players who chose to watch races (spectator item of the team menu). */
    private final Set<UUID> spectators = new HashSet<>();

    public boolean isSpectator(UUID player) {
        return spectators.contains(player);
    }

    public void setSpectator(UUID player, boolean spectator) {
        if (spectator ? spectators.add(player) : spectators.remove(player)) markDirty();
    }

    public void clearSpectators() {
        if (!spectators.isEmpty()) {
            spectators.clear();
            markDirty();
        }
    }

    /** The race in progress, or null. While a race runs, personal (endless) cards are paused. */
    private Race race;

    public Race race() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
        markDirty();
    }

    /** To call after changing the race in place (a cell ticked, a team added). */
    public void raceChanged() {
        markDirty();
    }

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
        nbt.putString("mode", mode.id());
        if (race != null) nbt.put("race", race.toNbt());
        NbtList watchers = new NbtList();
        for (UUID uuid : spectators) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("player", uuid);
            watchers.add(tag);
        }
        nbt.put("spectators", watchers);
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
        state.mode = BingoMode.byId(nbt.getString("mode"));
        if (nbt.contains("race")) state.race = Race.fromNbt(nbt.getCompound("race"));
        NbtList watchers = nbt.getList("spectators", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < watchers.size(); i++) {
            if (watchers.getCompound(i).containsUuid("player")) state.spectators.add(watchers.getCompound(i).getUuid("player"));
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
