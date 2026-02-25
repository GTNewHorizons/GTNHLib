package com.gtnewhorizon.gtnhlib.test.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.event.inventory.InventoryKey;

class InventoryKeyTest {

    private static final int DAMAGEABLE_ITEM_ID = 50000;
    private static final int META_ITEM_ID = 50001;
    private static final int FEATHER_ITEM_ID = 50002;

    private static Item damageableTestItem;
    private static Item metaTestItem;

    @BeforeAll
    static void registerTestItems() {
        ensureFeatherRegisteredForItemUtil();

        damageableTestItem = new Item().setUnlocalizedName("gtnhlib_test_damageable_item").setMaxDamage(128);
        metaTestItem = new Item().setUnlocalizedName("gtnhlib_test_meta_item").setHasSubtypes(true);

        registerItemId(damageableTestItem, DAMAGEABLE_ITEM_ID);
        registerItemId(metaTestItem, META_ITEM_ID);
    }

    private static void registerItemId(Item item, int id) {
        try {
            Field field = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
            field.setAccessible(true);
            ObjectIntIdentityMap idMap = (ObjectIntIdentityMap) field.get(Item.itemRegistry);
            idMap.func_148746_a(item, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject test item id", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void ensureFeatherRegisteredForItemUtil() {
        try {
            if (Item.itemRegistry.getObject("feather") != null) {
                return;
            }

            Item feather = new Item().setUnlocalizedName("gtnhlib_test_feather");

            Field mapField = RegistrySimple.class.getDeclaredField("registryObjects");
            mapField.setAccessible(true);
            Map<Object, Object> registryObjects = (Map<Object, Object>) mapField.get(Item.itemRegistry);
            registryObjects.put("minecraft:feather", feather);

            registerItemId(feather, FEATHER_ITEM_ID);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register a test feather item", e);
        }
    }

    @Test
    void damageableStacksIgnoreMeta() {
        ItemStack undamaged = new ItemStack(damageableTestItem, 1, 0);
        ItemStack damaged = new ItemStack(damageableTestItem, 1, 42);

        InventoryKey undamagedKey = InventoryKey.of(undamaged, false);
        InventoryKey damagedKey = InventoryKey.of(damaged, false);

        assertNotNull(undamagedKey);
        assertNotNull(damagedKey);
        assertEquals(undamagedKey, damagedKey);
        assertEquals(0, undamagedKey.getMeta());
        assertEquals(0, damagedKey.getMeta());
    }

    @Test
    void nonDamageableStacksStillRespectMeta() {
        ItemStack metaZero = new ItemStack(metaTestItem, 1, 0);
        ItemStack metaOne = new ItemStack(metaTestItem, 1, 1);

        InventoryKey metaZeroKey = InventoryKey.of(metaZero, false);
        InventoryKey metaOneKey = InventoryKey.of(metaOne, false);

        assertNotNull(metaZeroKey);
        assertNotNull(metaOneKey);
        assertNotEquals(metaZeroKey, metaOneKey);
        assertNotEquals(metaZeroKey.getMeta(), metaOneKey.getMeta());
    }

    @Test
    void damageableStacksStillRespectStrictNbt() {
        ItemStack first = new ItemStack(damageableTestItem, 1, 12);
        NBTTagCompound firstTag = new NBTTagCompound();
        firstTag.setString("owner", "alpha");
        first.setTagCompound(firstTag);

        ItemStack second = new ItemStack(damageableTestItem, 1, 99);
        NBTTagCompound secondTag = new NBTTagCompound();
        secondTag.setString("owner", "beta");
        second.setTagCompound(secondTag);

        InventoryKey nonStrictFirst = InventoryKey.of(first, false);
        InventoryKey nonStrictSecond = InventoryKey.of(second, false);
        InventoryKey strictFirst = InventoryKey.of(first, true);
        InventoryKey strictSecond = InventoryKey.of(second, true);

        assertNotNull(nonStrictFirst);
        assertNotNull(nonStrictSecond);
        assertNotNull(strictFirst);
        assertNotNull(strictSecond);
        assertEquals(nonStrictFirst, nonStrictSecond);
        assertNotEquals(strictFirst, strictSecond);
    }
}
