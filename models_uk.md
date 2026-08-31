# Model API

🌐 **Мови:** [English](models.md) | [Українська](models_uk.md)

Загалом цей API розроблений так, щоб імітувати поведінку новіших версій Minecraft.

### Свідомі відхилення
За замовчуванням передбачається, що посилання на текстури знаходяться в `assets/<domain>/blocks`,
відповідно до угод версії 1.7.10. Однак текстури, вказані як
`domain:block/whatever`, перескеровуються на `domain:whatever` для підтримки
імпорту моделей із сучасних версій.

Ресурспаки тепер можуть замінювати більшість блоків, аналогічно до сучасних версій.
*Зверніть увагу:* це працює насамперед для блоків із ISBRH — якщо для блоку
в коді зареєстровано TESR, це призведе до рендерингу *як* моделі,
*так і* TESR.

## Приклади
### BlockColor
Використовуйте для застосування кольору до `TintIndex`.
<p>
Приклад: Використання реєстрації BlockColor

<pre>
BlockColor.registerBlockColors(new IBlockColor() {

    &#64;Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        // Повертає червоний колір для основного шару, зелений — для додаткового
        return tintIndex == 0 ? 0xFF0000 : 0x00FF00;
    }

    &#64;Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Повертає синій колір для основного шару, жовтий — для додаткового
        return tintIndex == 0 ? 0x0000FF : 0xFFFF00;
    }
}, ModBlocks.MY_CUSTOM_BLOCK);
</pre>

Приклад: Реалізація `IBlockColor` безпосередньо у класі блоку
<pre>
public class BlockTestTint extends Block implements IBlockColor {

   public BlockTestTint() {
       super(Material.wood);
   }

   &#64;Override
   public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
       return switch(tintIndex) {
           case 0 -> 0xFF0000; // червоний
           case 1 -> 0x00FF00; // зелений
           case 2 -> 0x0000FF; // синій
           case 3 -> 0xFFFF00; // жовтий
           case 4 -> 0xFF00FF; // пурпуровий
           case 5 -> 0x00FFFF; // блакитний
           default -> 0xFFFFFF; // білий
       };
   }

   &#64;Override
   public int colorMultiplier(ItemStack stack, int tintIndex) {
       return colorMultiplier(null, 0, 0, 0, tintIndex);
   }
}
</pre>

### BlockState API

Замість використання 4-бітових метаданих, вибір моделі можна визначити на основі повних даних про стан блоку за допомогою зареєстрованого `BlockProperty`.

Це дозволяє:
- Необмежену кількість логічних станів (без обмеження в 16 значень метаданих);
- Варіанти моделей на основі даних `TileEntity`;
- Визначення стану з урахуванням `ItemStack`;
- Динамічну поведінку рендерингу.

Визначення BlockState працює у три кроки:
1. `BlockProperty` реєструється для блоку (і опціонально для його предмета).
2. Властивість надає значення з:
   - Світу + `TileEntity`
   - `ItemStack`
3. Завантажувач `JsonModel` визначає варіанти моделей, використовуючи ці значення властивостей.

#### 1. Створення BlockProperty

Приклад: Властивість напрямку, яка базується на `TileEntity`.

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

#### 2. Реєстрація властивості

<pre>
BlockPropertyRegistry.registerProperty(block, property);
BlockPropertyRegistry.registerProperty(Item.getItemFromBlock(block), property);
</pre>
Ви повинні зареєструвати властивість:
  - Для блоку (стан у світі);
  - Для предмета (відображення в інвентарі).

#### 3. Визначення JSON-файлу blockstate

Приклад:
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

Завантажувач виконає такі дії:
- Надішле запит до `property.getValue(...)`;
- Сформує рядок стану, наприклад: `facing=north`;
- Визначить відповідний варіант моделі.

#### Підтримувані атрибути
`BlockPropertyTrait` керує тим, де саме працює властивість:
- `SupportsWorld` → використовується під час рендерингу у світі;
- `SupportsStacks` → використовується під час рендерингу предмета в інвентарі;
- `WorldMutable` → значення можна змінювати у світі;
- `StackMutable` → значення можна змінювати в ItemStack.

Реалізовуйте лише ті атрибути, які вам необхідні.

### Заплановано
- Автоматичне завантаження текстур предметів.
- Додавання м’якого затінення до ModelISBRH.
- Додавання відсікання граней для моделей.
- Реалізація фіксації UV-координат.
