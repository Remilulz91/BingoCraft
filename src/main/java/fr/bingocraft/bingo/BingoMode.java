package fr.bingocraft.bingo;

import java.util.Locale;

/**
 * The game mode of the world, chosen by an OP with {@code /bingo mode}. A new world starts
 * with no mode: nothing is dealt and nothing is tracked until a mode is picked.
 */
public enum BingoMode {
    /** Bingo off. */
    NONE,
    /** Every player always has a card; completing it gives a reward and a new card. */
    ENDLESS,
    /** Players pick teams, an OP runs /bingo start, first team to a line or column wins. */
    RACE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BingoMode byId(String id) {
        for (BingoMode m : values()) if (m.id().equals(id)) return m;
        return NONE;
    }
}
