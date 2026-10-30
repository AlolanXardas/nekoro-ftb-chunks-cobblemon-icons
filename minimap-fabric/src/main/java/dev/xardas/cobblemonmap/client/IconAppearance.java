package dev.xardas.cobblemonmap.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Species;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import net.minecraft.client.Minecraft;

import java.util.Set;
import java.util.UUID;

/**
 * How one Pokemon's icon looks in this frame: transparency, outline, caught badge.
 *
 * Computed on every draw, because it depends on the entity's state (owner, battle, species caught)
 * and on time (pulsing). That is a few map and set lookups per icon with no allocation: the result
 * goes into one reused object (we only draw on the render thread).
 *
 * Outline color, in order:
 * <ol>
 *     <li>a player's Pokemon with "Your Pokémon" / "Other players' Pokémon" set: this color
 *     beats everything else and does not pulse,</li>
 *     <li>the first rare category with a color set: shiny, mythical, legendary,
 *     Ultra Beast, paradox (shiny overrides the others),</li>
 *     <li>the Main color; Main = Off means "no outline".</li>
 * </ol>
 * Outline opacity (separate for the minimap and the large map) applies to every outline color;
 * the only exception is the pulse color of rare Pokemon, which is always fully opaque (applyPulse).
 */
public final class IconAppearance {

    private static final int BLACK = 0x000000;
    private static final int OPAQUE = 255;
    /** Dimming of a Pokemon in battle: vertex color and fraction of opacity. */
    private static final int BATTLE_TINT = 0x8C8C8C;
    private static final float BATTLE_ALPHA = 0.6F;
    private static final double MIN_SPECIES_SCALE = 0.5D;
    private static final double MAX_SPECIES_SCALE = 2D;

    private static final IconAppearance INSTANCE = new IconAppearance();

    public int alpha;
    public int spriteRgb;
    public OutlineStyle outline;
    public int outlineRgb;
    public int outlineAlpha;
    /** null = no badge, true = caught, false = not caught */
    public Boolean caughtBadge;
    /**
     * Inverse of the species height scale. The badge should always have the same size, but the area
     * FTB Chunks gives us is already enlarged by that scale (getIconScale).
     */
    public float badgeScale;

    private IconAppearance() {
    }

    public static IconAppearance plain(int alpha) {
        IconAppearance a = INSTANCE;
        a.alpha = alpha;
        a.spriteRgb = 0xFFFFFF;
        a.outline = OutlineStyle.NONE;
        a.outlineRgb = BLACK;
        a.outlineAlpha = 0;
        a.caughtBadge = null;
        a.badgeScale = 1F;
        return a;
    }

    /**
     * Icon size multiplier from the species' real height. Cobblemon stores height in decimeters.
     * Cube root, because linear differences would be far too large (Wailord is 145, Snom 3),
     * and limits so small ones do not vanish and big ones do not cover the map.
     */
    public static double speciesScale(Species species) {
        double meters = species.getHeight() / 10D;
        return Math.max(MIN_SPECIES_SCALE, Math.min(MAX_SPECIES_SCALE, Math.cbrt(meters)));
    }

    public static IconAppearance of(PokemonEntity pokemon, MapType mapType) {
        IconAppearance a = plain(PokemonIconConfig.alphaFor(mapType));

        if (PokemonIconConfig.dimInBattle() && pokemon.isBattling()) {
            a.spriteRgb = BATTLE_TINT;
            a.alpha = Math.round(a.alpha * BATTLE_ALPHA);
        }

        Species species = pokemon.getPokemon().getSpecies();
        UUID owner = pokemon.getOwnerUUID();
        OutlineStyle type = PokemonIconConfig.outlineType();
        if (type != OutlineStyle.NONE) {
            resolveOutline(a, pokemon, species, owner, type, mapType);
        }

        if (PokemonIconConfig.scaleBySpeciesSize()) {
            a.badgeScale = (float) (1D / speciesScale(species));
        }
        if (owner == null) {
            PokedexBadge indicator = PokemonIconConfig.caughtIndicator();
            if (indicator != PokedexBadge.OFF) {
                Boolean caught = PokedexStatus.isCaught(species.getResourceIdentifier());
                if (caught != null && (caught ? indicator.showCaught : indicator.showUncaught)) {
                    a.caughtBadge = caught;
                }
            }
        }
        return a;
    }

    private static void resolveOutline(IconAppearance a, PokemonEntity pokemon, Species species, UUID owner,
                                       OutlineStyle type, MapType mapType) {
        int outlineAlpha = a.alpha * PokemonIconConfig.outlineAlpha(mapType) / OPAQUE;
        if (owner != null) {
            OutlineColor playerColor = isLocalPlayer(owner)
                    ? PokemonIconConfig.ownColor()
                    : PokemonIconConfig.otherPlayersColor();
            if (playerColor != OutlineColor.OFF) {
                a.outline = type;
                a.outlineRgb = playerColor.rgb;
                a.outlineAlpha = outlineAlpha;
                return;
            }
        }

        boolean rare = PokemonVisibility.isRare(pokemon.getAspects(), species.getLabels());
        OutlineColor color = rare ? rareColor(pokemon.getAspects(), species.getLabels()) : OutlineColor.OFF;
        if (color == OutlineColor.OFF) {
            color = PokemonIconConfig.mainColor();
        }
        RarePulse pulse = rare ? PokemonIconConfig.rarePulse() : RarePulse.OFF;

        if (color != OutlineColor.OFF) {
            a.outline = type;
            a.outlineRgb = color.rgb;
            a.outlineAlpha = outlineAlpha;
        } else if (pulse.rgb() >= 0) {
            // A rare Pokemon with no outline but with a pulse color: only the pulse color shows,
            // from invisible to full, so a full pulse works even with outlines turned off.
            a.outline = type;
            a.outlineRgb = pulse.rgb();
            a.outlineAlpha = 0;
        } else {
            return;
        }
        if (pulse != RarePulse.OFF) {
            applyPulse(a, pulse);
        }
    }

    /** Color of the first rare category that has a color set, or OFF. */
    private static OutlineColor rareColor(Set<String> aspects, Set<String> labels) {
        OutlineColor color;
        if (aspects.contains(PokemonVisibility.SHINY_ASPECT) && (color = PokemonIconConfig.shinyColor()) != OutlineColor.OFF) {
            return color;
        }
        if (labels.contains("mythical") && (color = PokemonIconConfig.mythicalColor()) != OutlineColor.OFF) {
            return color;
        }
        if (labels.contains("legendary") && (color = PokemonIconConfig.legendaryColor()) != OutlineColor.OFF) {
            return color;
        }
        if (labels.contains("ultra_beast") && (color = PokemonIconConfig.ultraBeastColor()) != OutlineColor.OFF) {
            return color;
        }
        if (labels.contains("paradox") && (color = PokemonIconConfig.paradoxColor()) != OutlineColor.OFF) {
            return color;
        }
        return OutlineColor.OFF;
    }

    /**
     * The outline starts at Outline opacity (already set in a.outlineAlpha).
     * Fade: from there down to zero. Color: from the outline color at that opacity to the pulse color
     * at full opacity; Outline opacity does not apply to the pulse color.
     */
    private static void applyPulse(IconAppearance a, RarePulse pulse) {
        float phase = RarePulse.phase();
        int baseAlpha = a.outlineAlpha;
        if (pulse.fade()) {
            a.outlineAlpha = Math.round(baseAlpha * (1F - phase));
        } else {
            a.outlineRgb = RarePulse.lerpRgb(a.outlineRgb, pulse.rgb(), phase);
            a.outlineAlpha = Math.round(baseAlpha + (a.alpha - baseAlpha) * phase);
        }
    }

    static boolean isLocalPlayer(UUID owner) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && owner.equals(minecraft.player.getUUID());
    }
}
