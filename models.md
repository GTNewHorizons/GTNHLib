# Model API

In general, it is designed to mimic the latest version.

### Intentional deviations
Texture references are assumed to be in `assets/<domain>/blocks` by
default, to match 7.10 conventions. However, textures specified by
`domain:block/whatever` are remapped to `domain:whatever`, to support
importing models from modern.

Resource packs can now replace most blocks, similarly to modern.
Note that this primarily works on blocks with ISBRHS - if the block
has a TESR registered in code, that should result in *both* the model and
the TESR rendering.

## Examples
### BlockColor
Use to color to TintIndex
<p>
Example: using BlockColor register

<pre>
BlockColor.registerBlockColors(new IBlockColor() {

    &#64;Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        // Return red for main layer, green for secondary
        return tintIndex == 0 ? 0xFF0000 : 0x00FF00;
    }

    &#64;Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Return blue for main layer, yellow for secondary
        return tintIndex == 0 ? 0x0000FF : 0xFFFF00;
    }
}, ModBlocks.MY_CUSTOM_BLOCK);
</pre>

Example: implement IBlockColor directly in a block
<pre>
public class BlockTestTint extends Block implements IBlockColor {

   public BlockTestTint() {
       super(Material.wood);
   }

   &#64;Override
   public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
       return switch(tintIndex) {
           case 0 -> 0xFF0000; // red
           case 1 -> 0x00FF00; // green
           case 2 -> 0x0000FF; // blue
           case 3 -> 0xFFFF00; // yellow
           case 4 -> 0xFF00FF; // magenta
           case 5 -> 0x00FFFF; // cyan
           default -> 0xFFFFFF; // white
       };
   }

   &#64;Override
   public int colorMultiplier(ItemStack stack, int tintIndex) {
       return colorMultiplier(null, 0, 0, 0, tintIndex);
   }
}
</pre>

### BlockState Api

Instead of relying on 4-bit metadata, model selection can resolved from full block state data via registered BlockProperty.

This allows:
- Unlimited logical states (no 16-meta restriction)
- Model variants based on TileEntity data
- ItemStack-aware state resolution
- Dynamic rendering behavior

BlockState resolution works in three steps:
1. A `BlockProperty` is registered to a block (and optionally its item).
2. The property provides values from:
   - World + TileEntity
   - ItemStack
3. The JsonModel loader resolves model variants using those property values.

#### 1.Creating a BlockProperty

Example: Direction property backed by TileEntity.

<pre>
DirectionBlockProperty property = new DirectionBlockProperty() {

    @Override
    public String getName() {
        return "facing";
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        return switch (trait) {
            case SupportsWorld, WorldMutable, StackMutable, SupportsStacks -> true;
            default -> false;
        };
    }

    @Override
    public ForgeDirection getValue(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileTestTintMul tile) {
            return tile.getFacing();
        }
        return ForgeDirection.NORTH;
    }

    @Override
    public void setValue(World world, int x, int y, int z, ForgeDirection value) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileTestTintMul tile) {
            tile.setFacing(value);
        }
    }

    @Override
    public ForgeDirection getValue(ItemStack stack) {
        return ForgeDirection.NORTH;
    }
};
</pre>

#### 2.Registering the Property

<pre>
BlockPropertyRegistry.registerProperty(block, property);
BlockPropertyRegistry.registerProperty(Item.getItemFromBlock(block), property);
</pre>
You must register:
  - On the block (world state)
  - On the item (inventory rendering)

#### 3.Defining blockstate JSON

Example:
<pre>
{
  "variants": {
    "facing=north": { "model": "modid:block/machine_north" },
    "facing=south": { "model": "modid:block/machine_south" },
    "facing=west":  { "model": "modid:block/machine_west" },
    "facing=east":  { "model": "modid:block/machine_east" }
  }
}
</pre>

The loader will:
- Query property.getValue(...)
- Build a state string like: facing=north
- Resolve the correct model variant

#### Supported Traits
`BlockPropertyTrait` controls where the property works:
- `SupportsWorld` → usable in world rendering
- `SupportsStacks` → usable in item rendering
- `WorldMutable` → value can be changed in world
- `StackMutable` → value can be changed in ItemStack

Only implement the traits you need.

### TODO
- Autoload item textures too.
- Add smooth shading to ModelISBRH.
- Add face culling to models.
- Implement UV locking.
