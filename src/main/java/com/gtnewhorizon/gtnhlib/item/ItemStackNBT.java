package com.gtnewhorizon.gtnhlib.item;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for safe and convenient manipulation of {@link ItemStack} NBT data.
 *
 * <p>
 * This class provides two complementary APIs:
 * </p>
 *
 * <ul>
 * <li><b>Static methods</b> – allocation-free, ideal for singular operations or performance-critical code paths</li>
 * <li><b>Instance methods</b> – allocates one object, ideal for multiple chainable operations for more readable
 * code</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Automatically initializes {@link NBTTagCompound} only when needed (on write)</li>
 * <li>Prevents unnecessary empty NBT compounds on {@link ItemStack}s</li>
 * <li>Provides safe default values when reading from missing tags</li>
 * <li>Supports all common NBT types (primitives, arrays, compounds, lists)</li>
 * </ul>
 *
 * <h2>Static vs Instance Usage</h2>
 *
 * <h3>Static API</h3>
 * <p>
 * The static methods avoid any additional object allocation and should be preferred in performance-sensitive contexts
 * such as ticking logic, inventory iteration, or rendering.
 * </p>
 *
 * <pre>
 * {@code
 * ItemStack stack = ...;
 *
 * // Write values
 * ItemStackNBT.setInteger(stack, "energy", 1000);
 * ItemStackNBT.setBoolean(stack, "active", true);
 *
 * // Read values
 * int energy = ItemStackNBT.getInteger(stack, "energy");
 * boolean active = ItemStackNBT.getBoolean(stack, "active");
 * }
 * </pre>
 *
 * <h3>Instance API</h3>
 * <p>
 * The instance API wraps an {@link ItemStack} and allows method chaining for improved readability. This is slightly
 * less efficient due to object allocation, but often negligible outside hot loops.
 * </p>
 *
 * <pre>
 * {@code
 * ItemStack stack = ...;
 *
 * ItemStackNBT.of(stack)
 *     .setInteger("energy", 1000)
 *     .setInteger("bla", 87)
 *     .setInteger("ducks", 55)
 *     .setBoolean("active", true);
 *
 * int energy = ItemStackNBT.of(stack).getInteger("energy");
 * }
 * </pre>
 *
 * <h2>Behavior Notes</h2>
 * <ul>
 * <li>Getter methods return safe default values when the stack has no NBT data or the key is missing:
 * <ul>
 * <li>Numeric types → {@code 0}</li>
 * <li>Boolean → {@code false}</li>
 * <li>String → empty string</li>
 * <li>Arrays → empty arrays</li>
 * <li>Objects → {@code null} (e.g. {@link NBTBase})</li>
 * </ul>
 * </li>
 * <li>Setter methods automatically create the underlying {@link NBTTagCompound} if it does not exist.</li>
 * <li>{@link #removeTag(ItemStack, String)} and its instance counterpart will automatically remove the entire compound
 * from the stack if it becomes empty, preventing unnecessary NBT data.</li>
 * </ul>
 */
public final class ItemStackNBT {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    @NotNull
    private final ItemStack stack;

    private ItemStackNBT(@NotNull ItemStack stack) {
        this.stack = stack;
    }

    public static ItemStackNBT of(ItemStack stack) {
        return new ItemStackNBT(Objects.requireNonNull(stack));
    }

    // region instance setters methods

    public ItemStackNBT setTag(String key, NBTBase value) {
        Objects.requireNonNull(value);
        ensureInitialized();
        stack.getTagCompound().setTag(key, value);
        return this;
    }

    public ItemStackNBT setByte(String key, byte value) {
        ensureInitialized();
        stack.getTagCompound().setByte(key, value);
        return this;
    }

    public ItemStackNBT setShort(String key, short value) {
        ensureInitialized();
        stack.getTagCompound().setShort(key, value);
        return this;
    }

    public ItemStackNBT setInteger(String key, int value) {
        ensureInitialized();
        stack.getTagCompound().setInteger(key, value);
        return this;
    }

    public ItemStackNBT setLong(String key, long value) {
        ensureInitialized();
        stack.getTagCompound().setLong(key, value);
        return this;
    }

    public ItemStackNBT setFloat(String key, float value) {
        ensureInitialized();
        stack.getTagCompound().setFloat(key, value);
        return this;
    }

    public ItemStackNBT setDouble(String key, double value) {
        ensureInitialized();
        stack.getTagCompound().setDouble(key, value);
        return this;
    }

    public ItemStackNBT setString(String key, String value) {
        Objects.requireNonNull(value);
        ensureInitialized();
        stack.getTagCompound().setString(key, value);
        return this;
    }

    public ItemStackNBT setByteArray(String key, byte[] value) {
        Objects.requireNonNull(value);
        ensureInitialized();
        stack.getTagCompound().setByteArray(key, value);
        return this;
    }

    public ItemStackNBT setIntArray(String key, int[] value) {
        Objects.requireNonNull(value);
        ensureInitialized();
        stack.getTagCompound().setIntArray(key, value);
        return this;
    }

    public ItemStackNBT setCompoundTag(String key, NBTTagCompound value) {
        Objects.requireNonNull(value);
        ensureInitialized();
        stack.getTagCompound().setTag(key, value);
        return this;
    }

    public ItemStackNBT setTagList(String key, NBTTagList value) {
        Objects.requireNonNull(value);
        ensureInitialized();
        stack.getTagCompound().setTag(key, value);
        return this;
    }

    public ItemStackNBT setBoolean(String key, boolean value) {
        ensureInitialized();
        stack.getTagCompound().setBoolean(key, value);
        return this;
    }

    /**
     * Sets the display name for this stack, if the parameter is null or empty it resets the display name of this stack.
     */
    public ItemStackNBT setDisplayName(String name) {
        if (StringUtils.isBlank(name)) {
            stack.func_135074_t();
        } else {
            stack.setStackDisplayName(name);
        }
        return this;
    }

    // endregion
    // region instance getter methods

    public Set<String> getKeySet() {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().func_150296_c();
        }
        return Collections.emptySet();
    }

    public byte getTagId(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().func_150299_b(key);
        }
        return 0;
    }

    public NBTBase getTag(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getTag(key);
        }
        return null;
    }

    public byte getByte(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getByte(key);
        }
        return 0;
    }

    public short getShort(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getShort(key);
        }
        return 0;
    }

    public int getInteger(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getInteger(key);
        }
        return 0;
    }

    public long getLong(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getLong(key);
        }
        return 0L;
    }

    public float getFloat(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getFloat(key);
        }
        return 0.0F;
    }

    public double getDouble(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getDouble(key);
        }
        return 0.0D;
    }

    public String getString(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getString(key);
        }
        return "";
    }

    public byte[] getByteArray(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getByteArray(key);
        }
        return EMPTY_BYTE_ARRAY;
    }

    public int[] getIntArray(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getIntArray(key);
        }
        return EMPTY_INT_ARRAY;
    }

    @Nullable
    public NBTTagCompound getCompoundTag(String key) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey(key, NBT.TAG_COMPOUND)) {
                return nbt.getCompoundTag(key);
            }
        }
        return null;
    }

    @Nullable
    public NBTTagList getTagList(String key, int type) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey(key, NBT.TAG_LIST)) {
                return nbt.getTagList(key, type);
            }
        }
        return null;
    }

    public boolean getBoolean(String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getBoolean(key);
        }
        return false;
    }

    /**
     * Implementation is different from {@link ItemStack#getDisplayName()}, this method returns the display name embeded
     * in the attached NBTTagCompound if any, returns null otherwise.
     */
    @Nullable
    public String getDisplayName() {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey("display", NBT.TAG_COMPOUND)) {
                final NBTTagCompound display = nbt.getCompoundTag("display");
                if (display.hasKey("Name", NBT.TAG_STRING)) {
                    return display.getString("Name");
                }
            }
        }
        return null;
    }

    // endregion
    // region instance util methods

    public boolean hasKey(String key) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(key);
    }

    public boolean hasKey(String key, int type) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(key, type);
    }

    public ItemStackNBT removeTag(String key) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            nbt.removeTag(key);
            if (nbt.hasNoTags()) {
                stack.setTagCompound(null);
            }
        }
        return this;
    }

    public boolean hasNoTags() {
        return !stack.hasTagCompound() || stack.getTagCompound().hasNoTags();
    }

    @Nullable
    public NBTTagCompound copy() {
        if (stack.hasTagCompound()) {
            return (NBTTagCompound) stack.getTagCompound().copy();
        }
        return null;
    }

    /**
     * Inverts the boolean associated with the key and returns the new value
     */
    public boolean invertBoolean(String key) {
        ensureInitialized();
        final boolean b = !stack.getTagCompound().getBoolean(key);
        stack.getTagCompound().setBoolean(key, b);
        return b;
    }

    private void ensureInitialized() {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }

    // endregion
    // region static setter methods

    public static void setTag(ItemStack stack, String key, NBTBase value) {
        Objects.requireNonNull(value);
        ensureInitialized(stack);
        stack.getTagCompound().setTag(key, value);
    }

    public static void setByte(ItemStack stack, String key, byte value) {
        ensureInitialized(stack);
        stack.getTagCompound().setByte(key, value);
    }

    public static void setShort(ItemStack stack, String key, short value) {
        ensureInitialized(stack);
        stack.getTagCompound().setShort(key, value);
    }

    public static void setInteger(ItemStack stack, String key, int value) {
        ensureInitialized(stack);
        stack.getTagCompound().setInteger(key, value);
    }

    public static void setLong(ItemStack stack, String key, long value) {
        ensureInitialized(stack);
        stack.getTagCompound().setLong(key, value);
    }

    public static void setFloat(ItemStack stack, String key, float value) {
        ensureInitialized(stack);
        stack.getTagCompound().setFloat(key, value);
    }

    public static void setDouble(ItemStack stack, String key, double value) {
        ensureInitialized(stack);
        stack.getTagCompound().setDouble(key, value);
    }

    public static void setString(ItemStack stack, String key, String value) {
        Objects.requireNonNull(value);
        ensureInitialized(stack);
        stack.getTagCompound().setString(key, value);
    }

    public static void setByteArray(ItemStack stack, String key, byte[] value) {
        Objects.requireNonNull(value);
        ensureInitialized(stack);
        stack.getTagCompound().setByteArray(key, value);
    }

    public static void setIntArray(ItemStack stack, String key, int[] value) {
        Objects.requireNonNull(value);
        ensureInitialized(stack);
        stack.getTagCompound().setIntArray(key, value);
    }

    public static void setCompoundTag(ItemStack stack, String key, NBTTagCompound value) {
        Objects.requireNonNull(value);
        ensureInitialized(stack);
        stack.getTagCompound().setTag(key, value);
    }

    public static void setTagList(ItemStack stack, String key, NBTTagList value) {
        Objects.requireNonNull(value);
        ensureInitialized(stack);
        stack.getTagCompound().setTag(key, value);
    }

    public static void setBoolean(ItemStack stack, String key, boolean value) {
        ensureInitialized(stack);
        stack.getTagCompound().setBoolean(key, value);
    }

    /**
     * Sets the display name for this stack, if the parameter is null or empty it resets the display name of this stack.
     */
    public static void setDisplayName(ItemStack stack, String name) {
        if (StringUtils.isBlank(name)) {
            stack.func_135074_t();
        } else {
            stack.setStackDisplayName(name);
        }
    }

    // endregion
    // region static getter methods

    public static Set<String> getKeySet(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().func_150296_c();
        }
        return Collections.emptySet();
    }

    public static byte getTagId(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().func_150299_b(key);
        }
        return 0;
    }

    public static NBTBase getTag(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getTag(key);
        }
        return null;
    }

    public static byte getByte(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getByte(key);
        }
        return 0;
    }

    public static short getShort(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getShort(key);
        }
        return 0;
    }

    public static int getInteger(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getInteger(key);
        }
        return 0;
    }

    public static long getLong(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getLong(key);
        }
        return 0L;
    }

    public static float getFloat(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getFloat(key);
        }
        return 0.0F;
    }

    public static double getDouble(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getDouble(key);
        }
        return 0.0D;
    }

    public static String getString(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getString(key);
        }
        return "";
    }

    public static byte[] getByteArray(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getByteArray(key);
        }
        return EMPTY_BYTE_ARRAY;
    }

    public static int[] getIntArray(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getIntArray(key);
        }
        return EMPTY_INT_ARRAY;
    }

    @Nullable
    public static NBTTagCompound getCompoundTag(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey(key, NBT.TAG_COMPOUND)) {
                return nbt.getCompoundTag(key);
            }
        }
        return null;
    }

    @Nullable
    public static NBTTagList getTagList(ItemStack stack, String key, int type) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey(key, NBT.TAG_LIST)) {
                return nbt.getTagList(key, type);
            }
        }
        return null;
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().getBoolean(key);
        }
        return false;
    }

    /**
     * Implementation is different from {@link ItemStack#getDisplayName()}, this method returns the display name embeded
     * in the attached NBTTagCompound if any, returns null otherwise.
     */
    @Nullable
    public static String getDisplayName(ItemStack stack) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey("display", NBT.TAG_COMPOUND)) {
                final NBTTagCompound display = nbt.getCompoundTag("display");
                if (display.hasKey("Name", NBT.TAG_STRING)) {
                    return display.getString("Name");
                }
            }
        }
        return null;
    }

    // endregion
    // region static util methods

    public static boolean hasKey(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().hasKey(key);
        }
        return false;
    }

    public static boolean hasKey(ItemStack stack, String key, int type) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().hasKey(key, type);
        }
        return false;
    }

    public static void removeTag(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            final NBTTagCompound nbt = stack.getTagCompound();
            nbt.removeTag(key);
            if (nbt.hasNoTags()) {
                stack.setTagCompound(null);
            }
        }
    }

    public static boolean hasNoTags(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().hasNoTags();
        }
        return true;
    }

    @Nullable
    public static NBTTagCompound copy(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return (NBTTagCompound) stack.getTagCompound().copy();
        }
        return null;
    }

    /**
     * Inverts the boolean associated with the key and returns the new value
     */
    public static boolean invertBoolean(ItemStack stack, String key) {
        ensureInitialized(stack);
        final boolean b = !stack.getTagCompound().getBoolean(key);
        stack.getTagCompound().setBoolean(key, b);
        return b;
    }

    /**
     * This method returns the NBTTagCompound attached to the ItemStack, if it is not present it creates one and returns
     * it. This method defeats the whole point of the api, only use if you absolutely need the NBTTagCompound to exist.
     */
    @NotNull
    public static NBTTagCompound get(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static void ensureInitialized(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }
    // endregion
}
