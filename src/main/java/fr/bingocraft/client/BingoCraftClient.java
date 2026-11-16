package fr.bingocraft.client;

import fr.bingocraft.BingoCraft;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-side entry point. The bingo card HUD and screen will be registered here.
 */
public class BingoCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BingoCraft.LOGGER.info("[BingoCraft] Client initialized.");
    }
}
