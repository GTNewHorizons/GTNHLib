GTNHLib
=======

## About

Shared code library for GTNH mods


## Events

GTNHLib fires `InventoryChangedEvent` on the Forge event bus when a player's net item holdings change, on both client (local player only) and server. Subscribe to the concrete subclasses:

- `InventoryChangedEvent.ItemAdded` - net gain of an item
- `InventoryChangedEvent.ItemRemoved` - net loss of an item

Each event exposes the representative `ItemStack item`, `getCount()` (absolute amount changed), and `getDelta()` (signed: positive added, negative removed). Items are matched by type and subtype metadata, ignoring NBT, so rearranging or sorting an inventory fires nothing; only crossing an inventory boundary (pickup, drop, chest transfer, crafting) does. Scanned scope: main inventory, armor, the held cursor stack, the 2x2 crafting grid, and Baubles slots when Baubles is installed.

Inventories are scanned every `inventoryScanInterval` ticks (default 5, configurable 1-200 in the GTNHLib config).


## Credits

Configs: Originally by FalsePattern, licensed under LGPL-3.0 in [FalsePatternLib](https://github.com/FalsePattern/FalsePatternLib/tree/master/src/main/java/com/falsepattern/lib/config)
