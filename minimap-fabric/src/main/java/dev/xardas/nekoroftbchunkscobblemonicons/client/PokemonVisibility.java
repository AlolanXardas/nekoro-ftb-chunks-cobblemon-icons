package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Whether a Pokemon is shown on the map (the Visibility section). Rules in order:
 * <ol>
 *     <li>species in "Always shown species": always visible,</li>
 *     <li>rare with "Always show rare" on: visible,</li>
 *     <li>species in "Always hidden species": hidden,</li>
 *     <li>"Show only rare": hidden unless rare,</li>
 *     <li>a player's Pokemon: "Show my Pokémon" / "Show other players' Pokémon",</li>
 *     <li>wild: "Show caught species".</li>
 * </ol>
 * "Rare" means only the categories from Cobblemon's data (shiny, mythical, legendary, ultra_beast,
 * paradox); the spawn chance exists only on the server, so the client does not know it.
 */
public final class PokemonVisibility {

    static final String SHINY_ASPECT = "shiny";
    private static final Set<String> RARE_LABELS = Set.of("mythical", "legendary", "ultra_beast", "paradox");

    private static final SpeciesList ALWAYS_SHOWN = new SpeciesList();
    private static final SpeciesList ALWAYS_HIDDEN = new SpeciesList();

    private PokemonVisibility() {
    }

    public static boolean isRare(Set<String> aspects, Set<String> labels) {
        if (aspects.contains(SHINY_ASPECT)) {
            return true;
        }
        for (String label : RARE_LABELS) {
            if (labels.contains(label)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldShow(PokemonEntity pokemon) {
        Species species = pokemon.getPokemon().getSpecies();
        ResourceLocation id = species.getResourceIdentifier();
        if (ALWAYS_SHOWN.contains(PokemonIconConfig.alwaysShownSpecies(), id)) {
            return true;
        }
        boolean rare = isRare(pokemon.getAspects(), species.getLabels());
        if (rare && PokemonIconConfig.alwaysShowRare()) {
            return true;
        }
        if (ALWAYS_HIDDEN.contains(PokemonIconConfig.alwaysHiddenSpecies(), id)) {
            return false;
        }
        if (PokemonIconConfig.showOnlyRare() && !rare) {
            return false;
        }
        UUID owner = pokemon.getOwnerUUID();
        if (owner != null) {
            return IconAppearance.isLocalPlayer(owner) ? PokemonIconConfig.showMine() : PokemonIconConfig.showOtherPlayers();
        }
        if (!PokemonIconConfig.showCaught()) {
            return !Boolean.TRUE.equals(PokedexStatus.isCaught(id));
        }
        return true;
    }

    /**
     * The species list from the config as a set, rebuilt only when the list changes (edited in
     * game), not every frame. Contents are compared with a copy, not by reference, in case the list
     * is edited in place. An entry "ditto" matches any namespace, "cobblemon:ditto" only that one
     * (for addon species with the same name).
     */
    private static final class SpeciesList {
        private List<String> source = List.of();
        private final Set<String> entries = new HashSet<>();

        private boolean contains(List<String> current, ResourceLocation id) {
            if (current.isEmpty()) {
                return false;
            }
            if (!current.equals(source)) {
                source = List.copyOf(current);
                entries.clear();
                for (String entry : current) {
                    String normalized = entry.trim().toLowerCase(Locale.ROOT);
                    if (!normalized.isEmpty()) {
                        entries.add(normalized);
                    }
                }
            }
            return entries.contains(id.getPath()) || entries.contains(id.toString());
        }
    }
}
