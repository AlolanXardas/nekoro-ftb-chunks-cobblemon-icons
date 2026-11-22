package dev.xardas.nekoroftbchunkscobblemonicons.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.xardas.nekoroftbchunkscobblemonicons.client.PokemonIconConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Order of our options on the settings screen.
 *
 * SNBTConfig sorts values by displayOrder, but EditConfigScreen sorts the finished ConfigValues
 * again, by their own order field and then by name, and displayOrder is not copied into it.
 * Without this the options are alphabetical. After the subgroup is built we set the order of our
 * values; options from other mods are not touched.
 */
@Mixin(value = SNBTConfig.class)
public class SNBTConfigMixin {

    @Inject(method = "createClientConfig(Ldev/ftb/mods/ftblibrary/config/ConfigGroup;)V", at = @At("RETURN"))
    private void nekoroftbchunkscobblemonicons$applyDisplayOrder(ConfigGroup group, CallbackInfo ci) {
        PokemonIconConfig.applyDisplayOrder((SNBTConfig) (Object) this, group);
    }
}
