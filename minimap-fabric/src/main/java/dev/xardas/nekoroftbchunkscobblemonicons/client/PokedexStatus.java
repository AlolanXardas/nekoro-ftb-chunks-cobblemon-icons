package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Whether the player has caught a species, from the Pokedex Cobblemon keeps on the client.
 *
 * The state is compared by name, not by enum constants: Cobblemon 1.7 has NONE/ENCOUNTERED/CAUGHT
 * and 1.8 has UNREGISTERED/SEEN/OWNED, so referencing a constant from one version would crash the game
 * on the other. If reading the Pokedex fails altogether (e.g. an API change in a future version), badges
 * are turned off for the rest of the session instead of logging errors every frame.
 */
public final class PokedexStatus {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean broken = false;

    private PokedexStatus() {
    }

    /** @return true/false, or null when unknown (no Pokedex data) */
    @Nullable
    public static Boolean isCaught(ResourceLocation species) {
        if (broken) {
            return null;
        }
        try {
            ClientPokedexManager pokedex = CobblemonClient.INSTANCE.getClientPokedexData();
            if (pokedex == null) {
                return null;
            }
            Enum<?> knowledge = pokedex.getKnowledgeForSpecies(species);
            if (knowledge == null) {
                return false;
            }
            String name = knowledge.name();
            return name.equals("CAUGHT") || name.equals("OWNED");
        } catch (Throwable t) {
            broken = true;
            LOGGER.warn("Could not read the Cobblemon Pokedex, Pokedex badges are disabled", t);
            return null;
        }
    }
}
