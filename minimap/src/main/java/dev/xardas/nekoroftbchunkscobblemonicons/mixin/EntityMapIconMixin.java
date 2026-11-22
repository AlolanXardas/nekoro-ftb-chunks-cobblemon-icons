package dev.xardas.nekoroftbchunkscobblemonicons.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.xardas.nekoroftbchunkscobblemonicons.client.IconAppearance;
import dev.xardas.nekoroftbchunkscobblemonicons.client.MapSpriteIcon;
import dev.xardas.nekoroftbchunkscobblemonicons.client.PokemonIconConfig;
import dev.xardas.nekoroftbchunkscobblemonicons.client.PokemonTooltip;
import dev.xardas.nekoroftbchunkscobblemonicons.client.PokemonVisibility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws Pokemon icons our own way, skipping the dark FTB Chunks background.
 *
 * FTB Chunks 2101.1.22 draws every entity icon like this:
 * <pre>
 *     poseStack.translate(x, y, 0F);
 *     poseStack.scale(w / 18F, h / 18F, 1F);
 *     Color4I.BLACK.withAlpha(128).draw(graphics, 0, 0, 18, 18);
 *     icon.withColor(Color4I.BLACK).draw(graphics, 1, 1, widthHeight.width(), widthHeight.height());
 * </pre>
 * Hence three symptoms: a black square under the icon, a black tint on the sprite itself, and,
 * with a bigger "size" in the icon settings, the sprite drawn from (1,1) and drifting right and
 * down while the 18x18 background stays in place and looks like a location dot near the top
 * left corner.
 *
 * The area (x, y, w, h) we get is already centered on the entity: on the minimap
 * FTBChunksClient does translate(wx - ws, wy - ws) and draw(..., 0, 0, 1, 1), and on the large
 * map MapIconWidget.updatePosition() places the widget at (pos - width/2, pos - height/2).
 * So filling that area with the sprite is enough for the Pokemon to sit exactly on its
 * position, no matter how much the icon is enlarged with "scale".
 */
@Mixin(value = EntityMapIcon.class, remap = false)
public class EntityMapIconMixin {

    @Final
    @Shadow
    private Entity entity;

    @Final
    @Shadow
    private Icon icon;

    @Inject(
        method = "draw(Ldev/ftb/mods/ftbchunks/api/client/icon/MapType;Lnet/minecraft/client/gui/GuiGraphics;IIIIZI)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void nekoroftbchunkscobblemonicons$drawWithoutBackdrop(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h,
                                                  boolean outsideVisibleArea, int iconAlpha, CallbackInfo ci) {
        // Only our icons. If the icon swap in EntityIconLoaderMixin ever stops working,
        // FTB Chunks draws the entity its own way instead of it vanishing from the map.
        if (icon instanceof MapSpriteIcon spriteIcon) {
            IconAppearance look = entity instanceof PokemonEntity pokemon
                    ? IconAppearance.of(pokemon, mapType)
                    : IconAppearance.plain(PokemonIconConfig.alphaFor(mapType));
            spriteIcon.draw(graphics, x, y, w, h, look);
            ci.cancel();
        }
    }

    @Inject(
        method = "getIconScale(Ldev/ftb/mods/ftbchunks/api/client/icon/MapType;)D",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void nekoroftbchunkscobblemonicons$applyPokemonIconScale(MapType mapType, CallbackInfoReturnable<Double> cir) {
        if (icon instanceof MapSpriteIcon) {
            double scale = cir.getReturnValue() * PokemonIconConfig.scaleFor(mapType);
            if (PokemonIconConfig.scaleBySpeciesSize() && entity instanceof PokemonEntity pokemon) {
                scale *= IconAppearance.speciesScale(pokemon.getPokemon().getSpecies());
            }
            cir.setReturnValue(scale);
        }
    }

    @Inject(
        method = "isVisible(Ldev/ftb/mods/ftbchunks/api/client/icon/MapType;DZ)Z",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void nekoroftbchunkscobblemonicons$applyVisibilityFilters(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof PokemonEntity pokemon && !PokemonVisibility.shouldShow(pokemon)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "addTooltip(Ldev/ftb/mods/ftblibrary/util/TooltipList;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void nekoroftbchunkscobblemonicons$pokemonTooltip(TooltipList list, CallbackInfo ci) {
        if (entity instanceof PokemonEntity pokemon) {
            list.add(PokemonTooltip.build(pokemon));
            ci.cancel();
        }
    }
}
