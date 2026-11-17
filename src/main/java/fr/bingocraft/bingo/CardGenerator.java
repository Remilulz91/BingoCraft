package fr.bingocraft.bingo;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Deals a new card: size² distinct items drawn uniformly from {@link GoalFilter#candidates}.
 * Items in {@code exclude} (what the player is carrying when the card is dealt) are left out,
 * so a fresh card never starts with squares already ticked.
 */
public final class CardGenerator {

    /** Every card of both modes is 5x5. */
    public static final int SIZE = 5;

    /** Smaller cards only exist through /bingo debug new, for quick tests. */
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 5;

    private CardGenerator() { }

    /** Returns a new card, or null if fewer than size² items are eligible. */
    public static BingoCard generate(int size, MinecraftServer server, Random random, Set<Item> exclude) {
        int total = size * size;
        List<Item> pool = new ArrayList<>(GoalFilter.candidates(server));
        pool.removeAll(exclude);
        if (pool.size() < total) return null;
        Collections.shuffle(pool, random);

        List<Identifier> goals = new ArrayList<>(total);
        for (int i = 0; i < total; i++) goals.add(Registries.ITEM.getId(pool.get(i)));
        return new BingoCard(size, goals);
    }
}
