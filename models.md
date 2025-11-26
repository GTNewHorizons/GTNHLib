# Model API

In general, it is designed to mimic the latest version.

### Intentional deviations
Texture references are assumed to be in `assets/<domain>/blocks` by
default, to match 7.10 conventions. However, textures specified by
`domain:block/whatever` are remapped to `domain:whatever`, to support
importing models from modern.

Resource packs can't replace any block. It has to return
`ModelISBRH.JSON_ISBRH_ID` as the render type. This is a fairly
trivial mixin, but the requirement is still there to avoid
spurious lookups on the vast majority of blocks that (currently)
don't have models. This may change in the future.

### Example: BlockColor registration
<p>
Example using BlockColor register:

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

### TODO
- Autoload item textures too.
- Add smooth shading to ModelISBRH.
- Add face culling to models.
- Implement UV locking.
- Integrate with a proper BlockState API.
- BlockItem json display.
