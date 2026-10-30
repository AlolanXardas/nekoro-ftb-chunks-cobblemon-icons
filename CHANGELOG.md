# Changelog

## 1.4

### New
- Shiny Pokémon without their own sprite (e.g. custom species from addons like Pikimon, future generations) now show a purple question mark instead of the regular one, so they can be told apart.
- New in-game settings: FTB Chunks settings (large map → settings) → "Cobblemon Icons", in four sections: Icons, Outline, Visibility, Tooltip.
- Icons: icon size and opacity, separately for minimap and large map; icons scaled by the Pokémon's real size (gently, 0.5×–2×, on by default); Pokémon in battle are dimmed.
- Outline: replaces the square backdrop FTB Chunks draws under mob icons with an outline that follows the Pokémon's shape (types: None / Shadow / Full). Other mobs keep FTB Chunks' square. Main color (default Black, or Off for no outline). Outline opacity for minimap and large map applies to all outline colors.
- Colored outlines for special Pokémon: Shiny (Gold), Mythical (Purple), Legendary (Red), Ultra Beast (Dark blue), Paradox (White), Your Pokémon and Other players' Pokémon — 12 colors each, Off falls back to the main color (or wild colors for players' Pokémon). Shiny overrides the other kinds.
- Rare pulse: outlines of shiny, mythical, legendary, ultra beast and paradox Pokémon pulse into a chosen color (default Black) or fade in and out.
- Visibility filters: show/hide caught species, your Pokémon and other players' Pokémon, always show rare, show only rare, plus lists of always shown and always hidden species.
- Caught indicator: badge next to wild Pokémon — gray circle for species you haven't caught yet, Poké Ball for caught ones.
- Large map tooltip shows level, gender, shiny star and variant, e.g. "Vulpix Lv. 12 ♀ ★ (Alola)" — each part can be toggled.
- Regional and alternate forms now have their own icons (Alolan, Galarian, Hisuian, Paldean, Unown letters, Vivillon patterns, female forms like Meowstic F, ...) — 360 forms mapped to the bundled sprites.
- Cobblemon feature variants with real sprites now use them: Deerling/Sawsbuck seasons, Shellos/Gastrodon east sea, Flabébé/Floette/Florges flowers, Burmy cloaks, Minior cores, Arceus plates, Silvally memories, Furfrou trims, Rotom appliances, Castform, Oricorio, Pumpkaboo sizes and more (266 variants).
- Gender differences: female Pokémon with a visibly different look (~100 species, e.g. Pikachu) use their female sprite, including shiny.
- Cobblemon-only variants without a sprite get a color filter so you can tell them apart: Torterra/Trevenant/Phantump/Komala tree types (e.g. cherry), dyed Wooloo/Dubwool (all 16 dye colors), Vivillon Inferno/Void/Forsaken, Cobblemon's extra Furfrou trims. Filters are computed once per sprite, no per-frame cost.
- Resource pack support: icons can be added or replaced per species, e.g. `assets/cobblemonmap/textures/pokemon/cobblemon/redpikmin.png` (and `shiny/...`). Such icons take priority over Pokédex numbers, so addon species with clashing numbers no longer steal each other's icons. Form/variant/filter tables (`form_sprites.json`, `aspect_sprites.json`, `sprite_tints.json`) can be overridden too.

### Compatibility
- Now requires Cobblemon 1.8+ and FTB Library 2101.1.37+ (FTB Chunks 2101.1.22+ as before).
- Tested with Cobblemon 1.8.3, FTB Chunks 2101.1.22, FTB Library 2101.1.37, on Fabric 0.19.5 and NeoForge 21.1.250 (Minecraft 1.21.1).
- Mod text is English only.
-
