package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Icon tooltip on the large map: "Rattata Lv. 12 ♀ ★ (Alola)".
 *
 * Each part is shown only when Cobblemon actually sent it to the client: the level comes in a
 * separate entity field (labelLevel) and can be empty, gender is the male/female aspect
 * (genderless ones have neither), and the variant is added only for non-base forms and for
 * features that have a sprite or filter here, so there is no "(None)" on every Pokemon.
 */
public final class PokemonTooltip {

    private static final String BASE_FORM = "normal";
    /** Parts of Cobblemon aspect names that mean nothing to the player ("tree-cherry" -> "Cherry"). */
    private static final List<String> PREFIXES = List.of("tree-", "color-", "vivillon-wings-", "character-",
            "flower-", "pumpkin-size-", "tatsugiri-texture-");
    private static final List<String> SUFFIXES = List.of("-sea", "-cloak", "-core", "-plate", "-memory",
            "-forme", "-form", "-mode", "-style", "-appliance", "-trim", "-fusion", "-percent", "-cape");

    private PokemonTooltip() {
    }

    public static Component build(PokemonEntity pokemon) {
        MutableComponent line = pokemon.getName().copy();
        Set<String> aspects = pokemon.getAspects();

        if (PokemonIconConfig.tooltipLevel()) {
            Integer level = pokemon.labelLevel();
            if (level != null && level > 0) {
                line.append(Component.literal(" Lv. " + level).withStyle(ChatFormatting.GRAY));
            }
        }
        if (PokemonIconConfig.tooltipGender()) {
            if (aspects.contains("female")) {
                line.append(Component.literal(" ♀").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (aspects.contains("male")) {
                line.append(Component.literal(" ♂").withStyle(ChatFormatting.AQUA));
            }
        }
        if (PokemonIconConfig.tooltipShiny() && aspects.contains("shiny")) {
            line.append(Component.literal(" ★").withStyle(ChatFormatting.GOLD));
        }
        if (PokemonIconConfig.tooltipVariant()) {
            String variant = variantName(pokemon.getPokemon().getSpecies(), aspects);
            if (variant != null) {
                line.append(Component.literal(" (" + variant + ")").withStyle(ChatFormatting.GRAY));
            }
        }
        return line;
    }

    private static String variantName(Species species, Set<String> aspects) {
        FormData form = species.getForm(aspects);
        if (!form.getName().toLowerCase(Locale.ROOT).equals(BASE_FORM)) {
            return prettify(form.getName());
        }
        String aspect = PokemonIconCache.findVariant(species.getNationalPokedexNumber(), aspects);
        return aspect.isEmpty() ? null : prettify(strip(aspect));
    }

    private static String strip(String aspect) {
        String result = aspect;
        for (String prefix : PREFIXES) {
            if (result.startsWith(prefix)) {
                result = result.substring(prefix.length());
                break;
            }
        }
        for (String suffix : SUFFIXES) {
            if (result.endsWith(suffix) && result.length() > suffix.length()) {
                result = result.substring(0, result.length() - suffix.length());
                break;
            }
        }
        return result;
    }

    /** "light_blue" / "paldea-combat" -> "Light Blue" / "Paldea Combat" */
    private static String prettify(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (c == '-' || c == '_' || c == ' ') {
                out.append(' ');
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }
}
