package fr.bingocraft;

import fr.bingocraft.bingo.CardTracker;
import fr.bingocraft.commands.BingoCommand;
import fr.bingocraft.race.RaceManager;
import fr.bingocraft.config.BingoCraftConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Main entry point of BingoCraft.
 * Initializes all server-side and common-side systems.
 */
public class BingoCraft implements ModInitializer {

    public static final String MOD_ID = "bingocraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("BingoCraft");

    // === Build type detection (set by Gradle's processResources) ===
    private static final boolean IS_DEBUG_BUILD;
    private static final String VERSION;
    static {
        boolean debug = false;
        String version = "unknown";
        try (InputStream is = BingoCraft.class.getResourceAsStream("/bingocraft.build.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                debug = "debug".equalsIgnoreCase(p.getProperty("build.type", "public").trim());
                version = p.getProperty("version", "unknown").trim();
            }
        } catch (IOException ignored) { }
        IS_DEBUG_BUILD = debug;
        VERSION = version;
    }

    /** Returns true if this JAR was built as the debug variant. */
    public static boolean isDebugBuild() {
        return IS_DEBUG_BUILD;
    }

    /** Returns the mod version baked in at build time. */
    public static String getVersion() {
        return VERSION;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("==============================================");
        LOGGER.info("    BingoCraft v{} - Starting up ({} build)",
                VERSION, IS_DEBUG_BUILD ? "DEBUG" : "PUBLIC");
        LOGGER.info("==============================================");

        // 1. Load configuration
        BingoCraftConfig.load();
        LOGGER.info("[BingoCraft] Configuration loaded");

        // 2. Card tracking: inventories are checked against the cards every few ticks
        CardTracker.register();
        RaceManager.register(); // race countdown

        // 3. Register commands (/bingo version, /bingo reload, /bingo debug ...)
        CommandRegistrationCallback.EVENT.register(BingoCommand::register);
        LOGGER.info("[BingoCraft] Commands registered");

        LOGGER.info("[BingoCraft] Mod loaded successfully!");
    }

    /** Creates an Identifier in the mod's namespace. */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
