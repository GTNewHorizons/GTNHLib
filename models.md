# Model API

In general, it is designed to mimic the latest version.

### Intentional deviations
Texture references are assumed to be in `assets/<domain>/blocks` by
default, to match 7.10 conventions. However, textures specified by
`domain:block/whatever` are remapped to `domain:whatever`, to support
importing models from modern.

TODO: Autoload item textures too.
