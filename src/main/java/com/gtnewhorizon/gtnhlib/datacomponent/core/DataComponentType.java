package com.gtnewhorizon.gtnhlib.datacomponent.core;

import java.lang.reflect.Type;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyJsonException;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;

/// A Component for a ItemStack.
///
/// The component's type can be any java type, but it must be convertible to json and vice versa.
/// For any values with special serialization requirements, the {@link #serialize(Object)} and
/// {@link #deserialize(JsonElement)} methods must be overridden, along with {@link #stringify(Object)} and
/// {@link #parse(String)}.
///
/// Consumers of this API must assume that the value is mutable. When mutating it in situations where the original
/// value must remain unmodified, the value must be first copied via {@link #copy(Object)}. If the value is immutable
/// (such as an enum variant or a property without an interface for modifying the value), implementations are free to
/// elide the copy.
///
/// [TValue] must implement [Object#hashCode()] and [Object#equals(Object)] properly.
public interface DataComponentType<TValue> {

    String getName();

    Type getType();

    default TValue copy(TValue value) {
        return value;
    }

    Gson GSON = new Gson();

    @OverrideOnly
    default Gson getGson() {
        return GSON;
    }

    /// Converts a json value into a java value.
    /// It cannot be assumed that the json value is compatible with the string serialization methods.
    /// @throws InvalidPropertyJsonException When the json is invalid
    default TValue deserialize(JsonElement element) {
        try {
            return getGson().fromJson(element, getType());
        } catch (JsonSyntaxException e) {
            throw new InvalidPropertyJsonException("Could not deserialize " + getType().getTypeName(), e);
        }
    }

    /// Converts a java value into a json value.
    /// It cannot be assumed that the json value is compatible with the string serialization methods.
    default JsonElement serialize(TValue value) {
        return getGson().toJsonTree(value);
    }

    /// Parses text into a java value.
    /// It cannot be assumed that the text value is compatible with the json serialization methods.
    /// @throws InvalidPropertyTextException When the text is invalid
    default TValue parse(String text) throws InvalidPropertyTextException {
        try {
            return getGson().fromJson(text, getType());
        } catch (JsonSyntaxException e) {
            throw new InvalidPropertyTextException("Could not parse " + getType().getTypeName(), e);
        }
    }

    /// Parses text into a java value.
    /// It cannot be assumed that the text value is compatible with the json serialization methods.
    default String stringify(TValue value) {
        return getGson().toJson(value);
    }

    default boolean appliesTo(ItemStack stack, Item item, int meta) {
        return true;
    }

    default TValue getValue(ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    default void setValue(ItemStack stack, TValue value) {
        throw new UnsupportedOperationException();
    }

    static <T> T getIndexSafe(T[] array, int index) {
        return array == null || index < 0 || index >= array.length ? null : array[index];
    }

    static <T> T getIndexSafe(List<T> list, int index) {
        return list == null || index < 0 || index >= list.size() ? null : list.get(index);
    }
}
