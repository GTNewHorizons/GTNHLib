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

Known limitations: items moved into an external crafting-table grid or other open container slots are out of scope, so placing items there reads as a removal and retrieving them reads as an addition. On joining a world the inventory arrives over a few ticks, so a brief burst of add events is possible before the baseline settles.

## Credits

Configs: Originally by FalsePattern, licensed under LGPL-3.0 in [FalsePatternLib](https://github.com/FalsePattern/FalsePatternLib/tree/master/src/main/java/com/falsepattern/lib/config)
