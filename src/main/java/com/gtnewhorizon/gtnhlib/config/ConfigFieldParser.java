package com.gtnewhorizon.gtnhlib.config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.common.Loader;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.SneakyThrows;
import lombok.val;

public class ConfigFieldParser {

    private static final Map<Class<?>, Parser> PARSERS = new HashMap<>();
    private static final Object2BooleanMap<String> detectedMods = new Object2BooleanOpenHashMap<>();

    static {
        PARSERS.put(boolean.class, new BooleanParser());
        PARSERS.put(Boolean.class, new BooleanParser());
        PARSERS.put(int.class, new IntParser());
        PARSERS.put(Integer.class, new IntParser());
        PARSERS.put(float.class, new FloatParser());
        PARSERS.put(Float.class, new FloatParser());
        PARSERS.put(double.class, new DoubleParser());
        PARSERS.put(Double.class, new DoubleParser());
        PARSERS.put(String.class, new StringParser());
        PARSERS.put(String[].class, new StringArrayParser());
        PARSERS.put(double[].class, new DoubleArrayParser());
        PARSERS.put(int[].class, new IntArrayParser());
        PARSERS.put(Enum.class, new EnumParser());
    }

    public static void loadField(Object instance, Field field, Configuration config, String category)
            throws ConfigException {
        try {
            Parser parser = getParser(field);
            var comment = Optional.ofNullable(field.getAnnotation(Config.Comment.class)).map(Config.Comment::value)
                    .map((lines) -> String.join("\n", lines)).orElse("");
            val name = getFieldName(field);
            val langKey = Optional.ofNullable(field.getAnnotation(Config.LangKey.class)).map(Config.LangKey::value)
                    .orElse(name);
            val defValueString = getModDefault(field);

            parser.load(instance, defValueString, field, config, category, name, comment, langKey);
        } catch (Exception e) {
            throw new ConfigException(
                    "Failed to load field " + field.getName()
                            + " of type "
                            + field.getType().getSimpleName()
                            + " in class "
                            + field.getDeclaringClass().getName()
                            + ". Caused by: "
                            + e);
        }
    }

    public static void saveField(Object instance, Field field, Configuration config, String category)
            throws ConfigException {
        try {
            val name = getFieldName(field);
            Parser parser = getParser(field);
            parser.save(instance, field, config, category, name);
        } catch (Exception e) {
            throw new ConfigException(
                    "Failed to save field " + field.getName()
                            + " of type "
                            + field.getType().getSimpleName()
                            + " in class "
                            + field.getDeclaringClass().getName()
                            + ". Caused by: "
                            + e);
        }
    }

    private static Parser getParser(Field field) throws ConfigException {
        Class<?> fieldClass = field.getType();
        Parser parser = PARSERS.get(fieldClass);
        if (Enum.class.isAssignableFrom(fieldClass)) {
            parser = PARSERS.get(Enum.class);
        }

        if (parser == null) {
            throw new ConfigException("No parser found for field");
        }

        return parser;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canParse(Field field) {
        Class<?> fieldClass = field.getType();
        return PARSERS.containsKey(fieldClass) || Enum.class.isAssignableFrom(fieldClass);
    }

    public static String getFieldName(Field field) {
        if (field.isAnnotationPresent(Config.Name.class)) {
            return field.getAnnotation(Config.Name.class).value();
        }
        return field.getName();
    }

    private static @Nullable String getModDefault(Field field) {
        val modDefaultList = field.getAnnotation(Config.ModDetectedDefaultList.class);
        if (modDefaultList != null) {
            return Arrays.stream(modDefaultList.values()).filter(ConfigFieldParser::isModDetected).findFirst()
                    .map(
                            modDefault -> modDefault.values().length != 0 ? String.join(",", modDefault.values())
                                    : modDefault.value().trim())
                    .orElse(null);
        }

        val modDefault = field.getAnnotation(Config.ModDetectedDefault.class);
        if (isModDetected(modDefault)) {
            return modDefault.values().length != 0 ? String.join(",", modDefault.values()) : modDefault.value().trim();
        }

        return null;
    }

    private static boolean isModDetected(Config.ModDetectedDefault modDefault) {
        if (modDefault == null) return false;
        val modID = modDefault.modID();
        val coremod = modDefault.coremod();
        if (modID.isEmpty() && coremod.isEmpty()) return false;
        return detectedMods.computeIfAbsent(modID.isEmpty() ? coremod : modID, id -> {
            if (!coremod.isEmpty()) {
                try {
                    Class.forName(coremod);
                    return true;
                } catch (ClassNotFoundException ignored) {}
            }

            return !modID.isEmpty() && Loader.isModLoaded(modID);
        });
    }

    @SneakyThrows
    private static Field extractField(Class<?> clazz, String field) {
        return clazz.getDeclaredField(field);
    }

    @SneakyThrows
    private static Object extractValue(Field field) {
        return field.get(null);
    }

    public interface Parser {

        void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey);

        void save(@Nullable Object instance, Field field, Configuration config, String category, String name);
    }

    private static class BooleanParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val defaultValue = fromStringOrDefault(instance, defValueString, field);
            field.setBoolean(instance, config.getBoolean(name, category, defaultValue, comment, langKey));
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            boolean boxed = field.getType().equals(Boolean.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Boolean) field.get(instance) : field.getBoolean(instance));
        }

        @SneakyThrows
        private boolean fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            val boxed = field.getType().equals(Boolean.class);
            if (defValueString == null) {
                return Optional.ofNullable(field.getAnnotation(Config.DefaultBoolean.class))
                        .map(Config.DefaultBoolean::value)
                        .orElse(boxed ? (Boolean) field.get(instance) : field.getBoolean(instance));
            }

            // Boolean.parseBoolean returns false for any string that is not "true" which is probably not desired here
            if (!"true".equalsIgnoreCase(defValueString) && !"false".equalsIgnoreCase(defValueString)) {
                throw new ConfigException("Invalid boolean value: " + defValueString);
            }

            return Boolean.parseBoolean(defValueString);
        }
    }

    private static class IntParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val range = Optional.ofNullable(field.getAnnotation(Config.RangeInt.class));
            val min = range.map(Config.RangeInt::min).orElse(Integer.MIN_VALUE);
            val max = range.map(Config.RangeInt::max).orElse(Integer.MAX_VALUE);
            val defaultValue = fromStringOrDefault(instance, defValueString, field);

            field.setInt(instance, config.getInt(name, category, defaultValue, min, max, comment, langKey));
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            boolean boxed = field.getType().equals(Integer.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Integer) field.get(instance) : field.getInt(instance));
        }

        @SneakyThrows
        private int fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            val boxed = field.getType().equals(Integer.class);
            if (defValueString == null) {
                return Optional.ofNullable(field.getAnnotation(Config.DefaultInt.class)).map(Config.DefaultInt::value)
                        .orElse(boxed ? (Integer) field.get(instance) : field.getInt(instance));
            }

            return Integer.parseInt(defValueString);
        }
    }

    private static class FloatParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val range = Optional.ofNullable(field.getAnnotation(Config.RangeFloat.class));
            val min = range.map(Config.RangeFloat::min).orElse(Float.MIN_VALUE);
            val max = range.map(Config.RangeFloat::max).orElse(Float.MAX_VALUE);
            val defaultValue = fromStringOrDefault(instance, defValueString, field);
            field.setFloat(instance, config.getFloat(name, category, defaultValue, min, max, comment, langKey));
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            boolean boxed = field.getType().equals(Float.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Float) field.get(instance) : field.getFloat(instance));
        }

        @SneakyThrows
        private float fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            val boxed = field.getType().equals(Float.class);
            if (defValueString == null) {
                return Optional.ofNullable(field.getAnnotation(Config.DefaultFloat.class))
                        .map(Config.DefaultFloat::value)
                        .orElse(boxed ? (Float) field.get(instance) : field.getFloat(instance));
            }

            return Float.parseFloat(defValueString);
        }
    }

    private static class DoubleParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val range = Optional.ofNullable(field.getAnnotation(Config.RangeDouble.class));
            val min = range.map(Config.RangeDouble::min).orElse(Double.MIN_VALUE);
            val max = range.map(Config.RangeDouble::max).orElse(Double.MAX_VALUE);
            val defaultValue = fromStringOrDefault(instance, defValueString, field);

            val defaultValueComment = comment + " [range: " + min + " ~ " + max + ", default: " + defaultValue + "]";
            field.setDouble(
                    instance,
                    config.get(category, name, defaultValue, defaultValueComment, min, max).setLanguageKey(langKey)
                            .getDouble());
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            boolean boxed = field.getType().equals(Double.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Double) field.get(instance) : field.getDouble(instance));
        }

        @SneakyThrows
        private double fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            val boxed = field.getType().equals(Double.class);
            if (defValueString == null) {
                return Optional.ofNullable(field.getAnnotation(Config.DefaultDouble.class))
                        .map(Config.DefaultDouble::value)
                        .orElse(boxed ? (Double) field.get(instance) : field.getDouble(instance));
            }

            return Double.parseDouble(defValueString);
        }
    }

    private static class StringParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val defaultValue = fromStringOrDefault(instance, defValueString, field);
            val pattern = Optional.ofNullable(field.getAnnotation(Config.Pattern.class)).map(Config.Pattern::value)
                    .map(Pattern::compile).orElse(null);
            field.set(instance, config.getString(name, category, defaultValue, comment, langKey, pattern));
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            Property prop = config.getCategory(category).get(name);
            prop.set((String) field.get(instance));
        }

        @SneakyThrows
        private String fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            if (defValueString == null) {
                return Optional.ofNullable(field.getAnnotation(Config.DefaultString.class))
                        .map(Config.DefaultString::value).orElse((String) field.get(instance));
            }

            return defValueString;
        }
    }

    private static class EnumParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            Class<?> fieldClass = field.getType();
            val enumValues = Arrays.stream((Object[]) fieldClass.getDeclaredMethod("values").invoke(instance))
                    .map((obj) -> (Enum<?>) obj).collect(Collectors.toList());
            val defaultValue = fromStringOrDefault(instance, defValueString, field, enumValues);
            val possibleValues = enumValues.stream().map(Enum::name).toArray(String[]::new);
            String value = config.getString(
                    name,
                    category,
                    defaultValue.name(),
                    comment + "\nPossible values: " + Arrays.toString(possibleValues) + "\n",
                    possibleValues,
                    langKey);

            try {
                if (!Arrays.asList(possibleValues).contains(value)) {
                    throw new NoSuchFieldException();
                }
                Field enumField = fieldClass.getDeclaredField(value);
                if (!enumField.isEnumConstant()) {
                    throw new NoSuchFieldException();
                }
                field.set(instance, enumField.get(instance));
            } catch (NoSuchFieldException e) {
                ConfigurationManager.LOGGER.warn(
                        "Invalid value {} for enum configuration field {} of type {} in config class {}! Using default value of {}!",
                        value,
                        field.getName(),
                        fieldClass.getName(),
                        field.getDeclaringClass().getName(),
                        defaultValue);
                field.set(instance, defaultValue);
            }
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            Property prop = config.getCategory(category).get(name);
            prop.set(((Enum<?>) field.get(instance)).name());
        }

        @SneakyThrows
        private Enum<?> fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field,
                List<? extends Enum<?>> validValues) {
            Enum<?> value;
            if (defValueString == null) {
                value = (Enum<?>) Optional.ofNullable(field.getAnnotation(Config.DefaultEnum.class))
                        .map(Config.DefaultEnum::value).map((defName) -> extractField(field.getType(), defName))
                        .map(ConfigFieldParser::extractValue).orElse(field.get(instance));
            } else {
                val modDefaultField = extractField(field.getType(), defValueString);
                value = (Enum<?>) extractValue(modDefaultField);
            }

            if (value == null) {
                throw new ConfigException("Invalid default value for enum field! Valid values are " + validValues);
            }

            return value;
        }
    }

    private static class StringArrayParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val defaultValue = fromStringOrDefault(instance, defValueString, field);
            val value = config.getStringList(name, category, defaultValue, comment, null, langKey);
            field.set(instance, value);
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            val prop = config.getCategory(category).get(name);
            prop.set((String[]) field.get(instance));
        }

        @SneakyThrows
        private String[] fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            String[] value;
            if (defValueString == null) {
                value = Optional.ofNullable(field.getAnnotation(Config.DefaultStringList.class))
                        .map(Config.DefaultStringList::value).orElse((String[]) field.get(instance));
            } else {
                value = defValueString.split(",");
            }
            return value == null ? new String[0] : value;
        }
    }

    private static class DoubleArrayParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val defaultValue = fromStringOrDefault(instance, defValueString, field);

            String[] stringValues = new String[defaultValue.length];
            for (int i = 0; i < defaultValue.length; i++) {
                stringValues[i] = Double.toString(defaultValue[i]);
            }
            comment = comment + " [default: " + Arrays.toString(stringValues) + "]";
            double[] value = config.get(category, name, defaultValue, comment).getDoubleList();

            field.set(instance, value);
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            Property prop = config.getCategory(category).get(name);
            prop.set((double[]) field.get(instance));
        }

        @SneakyThrows
        private double[] fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            double[] value;
            if (defValueString == null) {
                value = Optional.ofNullable(field.getAnnotation(Config.DefaultDoubleList.class))
                        .map(Config.DefaultDoubleList::value).orElse((double[]) field.get(instance));
            } else {
                value = Arrays.stream(defValueString.split(",")).mapToDouble(s -> Double.parseDouble(s.trim()))
                        .toArray();
            }

            return value == null ? new double[0] : value;
        }
    }

    private static class IntArrayParser implements Parser {

        @Override
        @SneakyThrows
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) {
            val defaultValue = fromStringOrDefault(instance, defValueString, field);
            String[] stringValues = new String[defaultValue.length];
            for (int i = 0; i < defaultValue.length; i++) {
                stringValues[i] = Integer.toString(defaultValue[i]);
            }
            comment = comment + " [default: " + Arrays.toString(stringValues) + "]";
            int[] value = config.get(category, name, defaultValue, comment).getIntList();

            field.set(instance, value);
        }

        @Override
        @SneakyThrows
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name) {
            Property prop = config.getCategory(category).get(name);
            prop.set((int[]) field.get(instance));
        }

        @SneakyThrows
        private int[] fromStringOrDefault(@Nullable Object instance, @Nullable String defValueString, Field field) {
            int[] value;
            if (defValueString == null) {
                value = Optional.ofNullable(field.getAnnotation(Config.DefaultIntList.class))
                        .map(Config.DefaultIntList::value).orElse((int[]) field.get(instance));
            } else {
                value = Arrays.stream(defValueString.split(",")).mapToInt(s -> Integer.parseInt(s.trim())).toArray();
            }

            return value == null ? new int[0] : value;
        }
    }
}
