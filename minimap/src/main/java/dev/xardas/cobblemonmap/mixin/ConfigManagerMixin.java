package dev.xardas.cobblemonmap.mixin;

import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftblibrary.util.BooleanConsumer;
import dev.xardas.cobblemonmap.client.PokemonIconConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Attaches the "cobblemon_icons" group to the FTB Chunks client config.
 *
 * FTBChunksClientConfig is an interface with constants, so its initialization cannot be injected
 * into. FTB Chunks registers it in the FTBChunks constructor through
 * ConfigManager.registerClientConfig(CONFIG, ...) instead; the two-argument version forwards to the
 * three-argument one, and the file is read only later.
 */
@Mixin(value = ConfigManager.class, remap = false)
public class ConfigManagerMixin {

    @Inject(
        method = "registerClientConfig(Ldev/ftb/mods/ftblibrary/snbt/config/SNBTConfig;Ljava/lang/String;Ldev/ftb/mods/ftblibrary/util/BooleanConsumer;)Ldev/ftb/mods/ftblibrary/snbt/config/SNBTConfig;",
        at = @At("HEAD"),
        remap = false
    )
    private void cobblemonmap$addPokemonIconGroup(SNBTConfig config, String subDirectory, BooleanConsumer onChanged,
                                                  CallbackInfoReturnable<SNBTConfig> cir) {
        PokemonIconConfig.onClientConfigRegistered(config);
    }
}
