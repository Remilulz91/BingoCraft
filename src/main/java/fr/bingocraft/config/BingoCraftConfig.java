package fr.bingocraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.bingocraft.BingoCraft;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central configuration for BingoCraft.
 * Saved to config/bingocraft.json.
 *
 * Options are added together with the feature that reads them: an option nothing reads
 * would only mislead whoever edits the file.
 */
public class BingoCraftConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("bingocraft.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static BingoCraftConfig INSTANCE = new BingoCraftConfig();

    // The game mode itself (off / endless / race) is not a config option: it is saved with the
    // world and picked in game with /bingo mode.

    /** Race: teams land at random within this many blocks of the world spawn. */
    public int raceSpawnRadius = 3000;

    /** Race: seconds of fall protection, regeneration and saturation after the drop. */
    public int raceProtectionSeconds = 60;

    // === Cards ===

    /** Ticks between two inventory checks (20 ticks = 1 second). */
    public int checkIntervalTicks = 10;

    // === Methods ===

    public static BingoCraftConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                BingoCraftConfig loaded = GSON.fromJson(json, BingoCraftConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
                save(); // rewrite so options added in newer versions appear in the file
                BingoCraft.LOGGER.info("[Config] Configuration loaded from {}", CONFIG_PATH);
            } else {
                save();
                BingoCraft.LOGGER.info("[Config] Default configuration created");
            }
        } catch (Exception e) {
            // Catches IOException and malformed JSON: keep defaults instead of crashing
            BingoCraft.LOGGER.error("[Config] Loading error, using defaults: {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            BingoCraft.LOGGER.error("[Config] Save error: {}", e.getMessage());
        }
    }
}
