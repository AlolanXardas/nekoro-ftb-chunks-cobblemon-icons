package dev.xardas.cobblemonmap;

import net.fabricmc.api.ClientModInitializer;

public class CobblemonMapFabric implements ClientModInitializer {

    /**
     * PokemonIconCache.init() is deliberately not called here.
     *
     * Fabric runs the 'client' entrypoint while Minecraft is still being constructed, before the
     * ResourceManager exists; getResourceManager() then returns null, and since 1.3 init() reads PNGs
     * (SpriteBounds.measure), so the game crashed on startup with an NPE. The fallback icon is loaded
     * lazily in PokemonIconCache.getFallback() on first use anyway.
     */
    @Override
    public void onInitializeClient() {
    }
}
