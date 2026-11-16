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
import java.util.Map;
import java.util.UUID;

/**
 * Every player's card, saved with the world (data/bingocraft_cards.dat in the overworld folder).
 * All changes go through this class so the save is always marked dirty.
 */
public class BingoState extends PersistentState {

    private static final String STATE_KEY = "bingocraft_cards";

    private final Map<UUID, BingoCard> cards = new HashMap<>();

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

    /** Marks a cell of a player's card as done. Returns false if it already was. */
    public boolean markDone(UUID player, int index) {
        BingoCard card = cards.get(player);
        if (card == null || !card.markDone(index)) return false;
        markDirty();
        return true;
    }

    // === Persistence ===

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, BingoCard> entry : cards.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("player", entry.getKey());
            tag.put("card", entry.getValue().toNbt());
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
            BingoCard card = BingoCard.fromNbt(tag.getCompound("card"));
            if (card != null) state.cards.put(tag.getUuid("player"), card);
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
