package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Picks and caches Pokemon icons.
 *
 * Order of looking for a sprite (for shinies, every step tries the shiny/ version first):
 * <ol>
 *     <li>by species id: textures/pokemon/&lt;namespace&gt;/&lt;species&gt;[-&lt;form&gt;].png, a way in
 *     for resource packs, e.g. icons for species from addons like Pikimon,</li>
 *     <li>variant from aspect_sprites.json: Cobblemon species features stored in aspects,
 *     e.g. Deerling's season "autumn" -> 585-autumn,</li>
 *     <li>form from form_sprites.json: PokeAPI form sprites have their own numbers (10001+)
 *     or names (201-b), so Cobblemon forms have to be translated with a table,</li>
 *     <li>Pokedex number,</li>
 *     <li>question mark: shiny/0.png for shinies, 0.png for the rest.</li>
 * </ol>
 * For females, the female/ version is tried before the sprite from steps 2-4. PokeAPI has separate
 * sprites only for ~100 species with a visible gender difference; the rest share the male sprite.
 * A shiny without its own sprite gets the normal sprite of the same form, because showing the right
 * Pokemon in normal colors is better than an "unknown" mark.
 *
 * A variant without its own sprite but with an entry in sprite_tints.json (e.g. Torterra's trees)
 * gets the base form sprite passed through {@link SpriteTint}.
 *
 * All three tables live in assets/nekoroftbchunkscobblemonicons, so a resource pack can override them.
 */
@SuppressWarnings("rawtypes")
public class PokemonIconCache {

    private static final String NAMESPACE = "nekoroftbchunkscobblemonicons";
    private static final String BASE_FORM = "normal";
    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "textures/pokemon/0.png");
    private static final ResourceLocation SHINY_FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "textures/pokemon/shiny/0.png");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static Icon fallback = null;
    private static Icon shinyFallback = null;
    /** "dex/lowercase form name" -> sprite file name, without .png */
    private static Map<String, String> formSprites = null;
    /** "dex/aspect" -> sprite file name, without .png */
    private static Map<String, String> aspectSprites = null;
    /** "dex/aspect" or "dex/form" -> filter */
    private static Map<String, SpriteTint> tints = null;

    public static void init() {
        fallback = makeIcon(FALLBACK_TEXTURE, null);
    }

    /**
     * @param species Cobblemon species id, e.g. cobblemon:rattata
     * @param form    Cobblemon form name, e.g. "Alola"; the base form is "Normal"
     * @param aspects synced entity aspects (gender, shiny, features like "tree-cherry")
     */
    public static Icon getIcon(ResourceLocation species, int dexNumber, String form, Set<String> aspects,
                               boolean female, boolean shiny) {
        String formKey = form.toLowerCase(Locale.ROOT);
        String variant = findVariant(dexNumber, aspects);
        String key = species + "/" + formKey + "/" + variant + (female ? "/female" : "") + (shiny ? "/shiny" : "");
        return CACHE.computeIfAbsent(key, k -> {
            Icon icon = loadIcon(species, dexNumber, formKey, variant, female, shiny);
            // For checking variants (which sprite, size, filter); debug level, so it is not in the
            // normal latest.log. NeoForge writes it to logs/debug.log.
            if (!variant.isEmpty() || !formKey.equals(BASE_FORM)) {
                LOGGER.debug("Icon for {} form={} variant={} female={} shiny={} aspects={} -> {}",
                        species, formKey, variant, female, shiny, aspects, icon);
            }
            return icon;
        });
    }

    /** The first aspect that has a sprite or a filter; an empty string if none. */
    public static String findVariant(int dexNumber, Set<String> aspects) {
        String tinted = "";
        for (String aspect : aspects) {
            String key = dexNumber + "/" + aspect;
            if (aspectSprites().containsKey(key)) {
                return aspect;
            }
            if (tinted.isEmpty() && tints().containsKey(key)) {
                tinted = aspect;
            }
        }
        return tinted;
    }

    private static Icon loadIcon(ResourceLocation species, int dexNumber, String formKey, String variant,
                                 boolean female, boolean shiny) {
        boolean baseForm = formKey.equals(BASE_FORM);
        String speciesPath = species.getNamespace() + "/" + species.getPath();
        List<String> exact = new ArrayList<>(3);
        if (!baseForm) {
            exact.add(speciesPath + "-" + formKey.replace(' ', '-'));
        }
        exact.add(speciesPath);

        List<String> specific = new ArrayList<>(4);
        if (!variant.isEmpty()) {
            String aspectSprite = aspectSprites().get(dexNumber + "/" + variant);
            if (aspectSprite != null) {
                addWithFemale(specific, aspectSprite, female);
            }
        }
        if (!baseForm) {
            String formSprite = formSprites().get(dexNumber + "/" + formKey);
            if (formSprite != null) {
                addWithFemale(specific, formSprite, female);
            }
        }

        Icon icon = firstExisting(exact, shiny, null);
        if (icon == null) {
            icon = firstExisting(specific, shiny, null);
        }
        if (icon == null) {
            // A variant without its own sprite: the base sprite, possibly with a color filter.
            SpriteTint tint = tints().get(dexNumber + "/" + variant);
            if (tint == null) {
                tint = tints().get(dexNumber + "/" + formKey);
            }
            List<String> base = new ArrayList<>(2);
            addWithFemale(base, String.valueOf(dexNumber), female);
            icon = firstExisting(base, shiny, tint);
        }
        if (icon != null) {
            return icon;
        }
        // Species from addons (e.g. Pikimon, dex 2510+) and future generations have no sprite
        // at all; a shiny then gets a purple question mark so it can still be told apart.
        return shiny ? getShinyFallback() : getFallback();
    }

    @Nullable
    private static Icon firstExisting(List<String> candidates, boolean shiny, @Nullable SpriteTint tint) {
        for (String candidate : candidates) {
            if (shiny) {
                Icon icon = tryLoad("textures/pokemon/shiny/" + candidate + ".png", tint);
                if (icon != null) {
                    return icon;
                }
            }
            Icon icon = tryLoad("textures/pokemon/" + candidate + ".png", tint);
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    private static void addWithFemale(List<String> candidates, String sprite, boolean female) {
        if (female) {
            candidates.add("female/" + sprite);
        }
        candidates.add(sprite);
    }

    /** Paths with form names can contain characters not allowed in ResourceLocation (e.g. "pa'u"); then null. */
    @Nullable
    private static Icon tryLoad(String path, @Nullable SpriteTint tint) {
        ResourceLocation location = ResourceLocation.tryBuild(NAMESPACE, path);
        return location == null ? null : makeIcon(location, tint);
    }

    /** Returns null when the sprite does not exist; the caller then moves down the fallback chain. */
    @Nullable
    private static Icon makeIcon(ResourceLocation loc, @Nullable SpriteTint tint) {
        return Minecraft.getInstance().getResourceManager().getResource(loc)
                .map(resource -> (Icon) new MapSpriteIcon(loc, SpriteBounds.measure(loc, resource), tint))
                .orElse(null);
    }

    private static Map<String, String> formSprites() {
        if (formSprites == null) {
            formSprites = loadTable("form_sprites.json", JsonElement::getAsString);
        }
        return formSprites;
    }

    private static Map<String, String> aspectSprites() {
        if (aspectSprites == null) {
            aspectSprites = loadTable("aspect_sprites.json", JsonElement::getAsString);
        }
        return aspectSprites;
    }

    private static Map<String, SpriteTint> tints() {
        if (tints == null) {
            tints = loadTable("sprite_tints.json", json -> SpriteTint.fromJson(json.getAsJsonObject()));
        }
        return tints;
    }

    private static <T> Map<String, T> loadTable(String file, Function<JsonElement, T> parser) {
        Map<String, T> map = new HashMap<>();
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(NAMESPACE, file));
        if (resource.isEmpty()) {
            return map;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                try {
                    map.put(entry.getKey(), parser.apply(entry.getValue()));
                } catch (Exception e) {
                    // a single broken entry from a resource pack: skip only that one
                    LOGGER.warn("Skipping invalid entry {} in {}: {}", entry.getKey(), file, e.toString());
                }
            }
        } catch (Exception e) {
            // a broken file from a resource pack: variants will look like the base form
            LOGGER.warn("Could not read {}, variants will use the base icon", file, e);
        }
        return map;
    }

    public static Icon getFallback() {
        if (fallback == null) {
            fallback = makeIcon(FALLBACK_TEXTURE, null);
        }
        return fallback == null ? Icon.empty() : fallback;
    }

    private static Icon getShinyFallback() {
        if (shinyFallback == null) {
            shinyFallback = makeIcon(SHINY_FALLBACK_TEXTURE, null);
        }
        return shinyFallback == null ? getFallback() : shinyFallback;
    }
}
