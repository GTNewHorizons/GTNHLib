GTNHLib
=======

## About

Shared code library for GTNH mods.

## Features

### Config

- Define configs with the `@Config` annotation on a class. Each field becomes a config option.
- Set defaults, ranges, comments, and lang keys with field annotations like `@DefaultInt`, `@RangeInt`, and `@Comment`.
- The library reads, writes, and validates the `.cfg` file for you. No manual `Configuration` calls.
- Mark fields `@Sync` to push server values to clients on connect.
- Generates a Forge config GUI straight from the annotations.

### Events

- Fires `InventoryChangedEvent` when a player's net item holdings change. Subscribe to `ItemAdded` or `ItemRemoved`, which carry the changed `ItemStack` and a signed delta.
- Items are matched by type and metadata ignoring NBT, so sorting an inventory fires nothing. Scans run every few ticks (configurable).
- Fires `PickBlockEvent` when a player middle-clicks a block.
- Mark a class `@EventBusSubscriber` to auto-register its `@SubscribeEvent` methods. Choose the side and load phase in the annotation. No manual registration needed.

### Items

- `ImmutableItemStack` wraps a stack as read-only to prevent accidental mutation.
- `ItemTransfer` moves items between an `ItemSource` and an `ItemSink` with predicate filters and count limits.
- Walk any `IInventory` with `InventoryIterator`, including sided slots.
- The source and sink interfaces abstract pulling and pushing so transfer logic works across any inventory type.

### Rendering

- Render items with multiple stacked texture layers by implementing `ItemWithTextures`. Register with `TexturedItemRenderer`.
- `CapturingTesselator` captures render quads off the main thread for batched or async rendering.
- Helpers for animated tooltips and an above-hotbar HUD message.

### Block State

- Register custom block properties: boolean, int, enum, direction, axis, and orientation.
- Pack and read property values from block metadata, so blocks behave like modern block states on 1.7.
- Includes a `blockstate` command to inspect state at a position.

### Commands

- Brigadier command API for modern command parsing, completion, and execution on 1.7.
- Register custom game rules with `GameRuleRegistry`. The library hooks the vanilla game rules system to notify your rules of changes.

### Teams

- Team management system with roles, membership, and admin commands.
- Register your own data on a team with `TeamDataRegistry`. The library persists it to disk and syncs it to clients.

### Networking

- Send titles, hotbar messages, and view distance to clients.
- Sync custom player data.

### Utilities

- LWJGL3-style `MemoryUtil` and `MemoryStack` backport in the `bytebuf` package.
- FNV-1a 32 and 64 bit hashing.
- Math, NBT, JSON, file, direction, and distance helpers.
- Color types for RGB and HSV.
- 3D geometry iterators and transforms.
- Capability provider helpers.
- Synced keybinds.
- ASM and bytecode helpers.
- Mod compatibility checks (Baubles, FalseTweaks, NEI).

## Credits

Configs: Originally by FalsePattern, licensed under LGPL-3.0 in [FalsePatternLib](https://github.com/FalsePattern/FalsePatternLib/tree/master/src/main/java/com/falsepattern/lib/config)
