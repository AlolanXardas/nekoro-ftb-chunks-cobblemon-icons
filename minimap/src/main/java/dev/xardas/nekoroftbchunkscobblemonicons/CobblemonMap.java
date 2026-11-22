package dev.xardas.nekoroftbchunkscobblemonicons;

import dev.xardas.nekoroftbchunkscobblemonicons.client.PokemonIconCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("nekoroftbchunkscobblemonicons")
public class CobblemonMap {

    public CobblemonMap(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(PokemonIconCache::init);
    }
}
