package fr.bingocraft.bingo;

import java.util.Locale;

/** Difficulty tier of a bingo goal. Pool files declare which tier their items belong to. */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses "easy" / "medium" / "hard" (case-insensitive); null if unknown. */
    public static Difficulty byId(String id) {
        for (Difficulty d : values()) {
            if (d.id().equalsIgnoreCase(id)) return d;
        }
        return null;
    }
}
