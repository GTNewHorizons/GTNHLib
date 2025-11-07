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

### TODO
- Autoload item textures too.
- Add smooth shading to ModelISBRH.
- Add face culling to models.
- Implement UV locking.
- Integrate with a proper BlockState API.
- BlockItem rendering.
