package dev.xardas.cobblemonmap.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.xardas.cobblemonmap.client.PokemonIconCache;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@SuppressWarnings("rawtypes")
@Mixin(value = EntityIconLoader.class)
public class EntityIconLoaderMixin {

    /** The aspect Cobblemon uses to mark shinies; the same constant the mod itself uses. */
    private static final String SHINY_ASPECT = "shiny";
    /** Gender aspect (PokemonAspects.GENDER_ASPECT in Cobblemon); Cobblemon picks the female model from it. */
    private static final String FEMALE_ASPECT = "female";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loggedFailure = false;

    @Inject(
        method = "getIcon(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftblibrary/icon/Icon;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetIconForEntity(Entity entity, CallbackInfoReturnable<Icon> cir) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            try {
                Species species = pokemonEntity.getPokemon().getSpecies();
                // The form is taken from the synced aspects, for the same reason as shiny
                // (see isShiny); Cobblemon picks it the same way when rendering the model.
                Set<String> aspects = pokemonEntity.getAspects();
                FormData form = species.getForm(aspects);
                cir.setReturnValue(PokemonIconCache.getIcon(species.getResourceIdentifier(),
                        species.getNationalPokedexNumber(), form.getName(), aspects,
                        aspects.contains(FEMALE_ASPECT), isShiny(pokemonEntity)));
            } catch (Exception e) {
                if (!loggedFailure) {
                    loggedFailure = true;
                    LOGGER.warn("Could not pick an icon for {}, showing the fallback (logged once)", entity, e);
                }
                cir.setReturnValue(PokemonIconCache.getFallback());
            }
        }
    }

    /**
     * Shiny is read from the synced aspects, not from Pokemon.getShiny().
     *
     * PokemonEntity.getAspects() reads entityData.get(ASPECTS) directly, so it is always up to date
     * on the client; Cobblemon picks the shiny model from the same set of aspects when rendering
     * in the world.
     *
     * Pokemon.getShiny() on the client is only derived state: it is set by
     * PokemonClientDelegate.onSyncedDataUpdated(ASPECTS) through setShiny(aspects.contains("shiny")),
     * and the delegate call is guarded by a null check (the delegate does not exist yet while the
     * entity is being constructed). That is why the icon could stay the normal variant.
     */
    private static boolean isShiny(PokemonEntity pokemonEntity) {
        if (pokemonEntity.getAspects().contains(SHINY_ASPECT)) {
            return true;
        }
        return pokemonEntity.getPokemon().getShiny();
    }
}
