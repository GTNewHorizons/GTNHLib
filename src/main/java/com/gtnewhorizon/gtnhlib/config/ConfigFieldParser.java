package com.gtnewhorizon.gtnhlib.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
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
            throws NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException,
            ConfigException {
        Parser parser = getParser(field);
        var comment = Optional.ofNullable(field.getAnnotation(Config.Comment.class)).map(Config.Comment::value)
                .map((lines) -> String.join("\n", lines)).orElse("");
        val name = getFieldName(field);
        val langKey = Optional.ofNullable(field.getAnnotation(Config.LangKey.class)).map(Config.LangKey::value)
                .orElse(name);

        val modDefault = getModDefault(field);
        String defValueString = null;
        if (modDefault != null) {
            if (modDefault.values().length != 0) {
                defValueString = String.join(",", modDefault.values());
            } else {
                defValueString = modDefault.value().replaceAll(" ", "");
            }
        }
        parser.load(instance, defValueString, field, config, category, name, comment, langKey);
    }

    public static void saveField(Object instance, Field field, Configuration config, String category)
            throws IllegalAccessException, ConfigException {
        val name = getFieldName(field);
        Parser parser = getParser(field);
        parser.save(instance, field, config, category, name);
    }

    private static Parser getParser(Field field) throws ConfigException {
        Class<?> fieldClass = field.getType();
        Parser parser = PARSERS.get(fieldClass);
        if (Enum.class.isAssignableFrom(fieldClass)) {
            parser = PARSERS.get(Enum.class);
        }

        if (parser == null) {
            throw new ConfigException(
                    "No parser found for field " + field.getName() + " of type " + fieldClass.getName() + "!");
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

    private static @Nullable Config.ModDetectedDefault getModDefault(Field field) {
        val modDefault = field.getAnnotation(Config.ModDetectedDefault.class);
        if (modDefault == null) return null;

        val coremod = modDefault.coremod();
        val modId = modDefault.modID();
        if (coremod.isEmpty() && modId.isEmpty()) return null;

        boolean isDetected = detectedMods.computeIfAbsent(modId.isEmpty() ? coremod : modId, id -> {
            if (!modId.isEmpty() && Loader.isModLoaded(modId)) return true;
            if (!coremod.isEmpty()) {
                try {
                    Class.forName(coremod);
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }
            return false;
        });

        return isDetected ? modDefault : null;
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
                String category, String name, String comment, String langKey) throws IllegalAccessException,
                NoSuchMethodException, InvocationTargetException, NoSuchFieldException, ConfigException;

        void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException;
    }

    private static class BooleanParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            boolean boxed = field.getType().equals(Boolean.class);
            boolean defaultValue;

            if (defValueString != null) {
                defaultValue = Boolean.parseBoolean(defValueString);
            } else {
                defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultBoolean.class))
                        .map(Config.DefaultBoolean::value)
                        .orElse(boxed ? (Boolean) field.get(instance) : field.getBoolean(instance));
            }
            field.setBoolean(instance, config.getBoolean(name, category, defaultValue, comment, langKey));
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            boolean boxed = field.getType().equals(Boolean.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Boolean) field.get(instance) : field.getBoolean(instance));
        }
    }

    private static class IntParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            boolean boxed = field.getType().equals(Integer.class);
            val range = Optional.ofNullable(field.getAnnotation(Config.RangeInt.class));
            val min = range.map(Config.RangeInt::min).orElse(Integer.MIN_VALUE);
            val max = range.map(Config.RangeInt::max).orElse(Integer.MAX_VALUE);
            int defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultInt.class))
                    .map(Config.DefaultInt::value)
                    .orElse(boxed ? (Integer) field.get(instance) : field.getInt(instance));

            if (defValueString != null) {
                defaultValue = Integer.parseInt(defValueString);
            }

            field.setInt(instance, config.getInt(name, category, defaultValue, min, max, comment, langKey));
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            boolean boxed = field.getType().equals(Integer.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Integer) field.get(instance) : field.getInt(instance));
        }
    }

    private static class FloatParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            boolean boxed = field.getType().equals(Float.class);
            val range = Optional.ofNullable(field.getAnnotation(Config.RangeFloat.class));
            val min = range.map(Config.RangeFloat::min).orElse(Float.MIN_VALUE);
            val max = range.map(Config.RangeFloat::max).orElse(Float.MAX_VALUE);
            float defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultFloat.class))
                    .map(Config.DefaultFloat::value)
                    .orElse(boxed ? (Float) field.get(instance) : field.getFloat(instance));

            if (defValueString != null) {
                defaultValue = Float.parseFloat(defValueString);
            }
            field.setFloat(instance, config.getFloat(name, category, defaultValue, min, max, comment, langKey));
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            boolean boxed = field.getType().equals(Float.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Float) field.get(instance) : field.getFloat(instance));
        }
    }

    private static class DoubleParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            boolean boxed = field.getType().equals(Double.class);
            val range = Optional.ofNullable(field.getAnnotation(Config.RangeDouble.class));
            val min = range.map(Config.RangeDouble::min).orElse(Double.MIN_VALUE);
            val max = range.map(Config.RangeDouble::max).orElse(Double.MAX_VALUE);
            double defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultDouble.class))
                    .map(Config.DefaultDouble::value)
                    .orElse(boxed ? (Double) field.get(instance) : field.getDouble(instance));

            if (defValueString != null) {
                defaultValue = Double.parseDouble(defValueString);
            }

            val defaultValueComment = comment + " [range: " + min + " ~ " + max + ", default: " + defaultValue + "]";
            field.setDouble(
                    instance,
                    config.get(category, name, defaultValue, defaultValueComment, min, max).setLanguageKey(langKey)
                            .getDouble());
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            boolean boxed = field.getType().equals(Double.class);
            Property prop = config.getCategory(category).get(name);
            prop.set(boxed ? (Double) field.get(instance) : field.getDouble(instance));
        }
    }

    private static class StringParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            String defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultString.class))
                    .map(Config.DefaultString::value).orElse((String) field.get(instance));

            if (defValueString != null) {
                defaultValue = defValueString;
            }

            val pattern = Optional.ofNullable(field.getAnnotation(Config.Pattern.class)).map(Config.Pattern::value)
                    .map(Pattern::compile).orElse(null);
            field.set(instance, config.getString(name, category, defaultValue, comment, langKey, pattern));
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            Property prop = config.getCategory(category).get(name);
            prop.set((String) field.get(instance));
        }
    }

    private static class EnumParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey)
                throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, ConfigException {
            Class<?> fieldClass = field.getType();
            val enumValues = Arrays.stream((Object[]) fieldClass.getDeclaredMethod("values").invoke(instance))
                    .map((obj) -> (Enum<?>) obj).collect(Collectors.toList());
            Enum<?> defaultValue = (Enum<?>) Optional.ofNullable(field.getAnnotation(Config.DefaultEnum.class))
                    .map(Config.DefaultEnum::value).map((defName) -> extractField(fieldClass, defName))
                    .map(ConfigFieldParser::extractValue).orElse(field.get(instance));

            if (defValueString != null) {
                val modDefaultField = extractField(fieldClass, defValueString);
                defaultValue = (Enum<?>) extractValue(modDefaultField);
            }

            if (defaultValue == null) {
                throw new ConfigException(
                        "Invalid default value for enum field " + field.getName()
                                + " of type "
                                + fieldClass.getName()
                                + " in config class "
                                + field.getDeclaringClass().getName()
                                + " Valid values are: "
                                + enumValues);
            }

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
                        "Invalid value " + value
                                + " for enum configuration field "
                                + field.getName()
                                + " of type "
                                + fieldClass.getName()
                                + " in config class "
                                + field.getDeclaringClass().getName()
                                + "! Using default value of "
                                + defaultValue
                                + "!");
                field.set(instance, defaultValue);
            }
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            Property prop = config.getCategory(category).get(name);
            prop.set(((Enum<?>) field.get(instance)).name());
        }
    }

    private static class StringArrayParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {

            String[] defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultStringList.class))
                    .map(Config.DefaultStringList::value).orElse((String[]) field.get(instance));

            if (defValueString != null) {
                defaultValue = defValueString.split(",");
            }

            if (defaultValue == null) defaultValue = new String[0];
            String[] value = config.getStringList(name, category, defaultValue, comment, null, langKey);
            field.set(instance, value);
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            Property prop = config.getCategory(category).get(name);
            prop.set((String[]) field.get(instance));
        }
    }

    private static class DoubleArrayParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            double[] defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultDoubleList.class))
                    .map(Config.DefaultDoubleList::value).orElse((double[]) field.get(instance));

            if (defValueString != null) {
                defaultValue = Arrays.stream(defValueString.split(",")).mapToDouble(Double::parseDouble).toArray();
            }

            if (defaultValue == null) defaultValue = new double[0];

            String[] stringValues = new String[defaultValue.length];
            for (int i = 0; i < defaultValue.length; i++) {
                stringValues[i] = Double.toString(defaultValue[i]);
            }
            comment = comment + " [default: " + Arrays.toString(stringValues) + "]";
            double[] value = config.get(category, name, defaultValue, comment).getDoubleList();

            field.set(instance, value);
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            Property prop = config.getCategory(category).get(name);
            prop.set((double[]) field.get(instance));
        }
    }

    private static class IntArrayParser implements Parser {

        @Override
        public void load(@Nullable Object instance, @Nullable String defValueString, Field field, Configuration config,
                String category, String name, String comment, String langKey) throws IllegalAccessException {
            int[] defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultIntList.class))
                    .map(Config.DefaultIntList::value).orElse((int[]) field.get(instance));

            if (defValueString != null) {
                defaultValue = Arrays.stream(defValueString.split(",")).mapToInt(Integer::parseInt).toArray();
            }

            if (defaultValue == null) defaultValue = new int[0];

            String[] stringValues = new String[defaultValue.length];
            for (int i = 0; i < defaultValue.length; i++) {
                stringValues[i] = Integer.toString(defaultValue[i]);
            }
            comment = comment + " [default: " + Arrays.toString(stringValues) + "]";
            int[] value = config.get(category, name, defaultValue, comment).getIntList();

            field.set(instance, value);
        }

        @Override
        public void save(@Nullable Object instance, Field field, Configuration config, String category, String name)
                throws IllegalAccessException {
            Property prop = config.getCategory(category).get(name);
            prop.set((int[]) field.get(instance));
        }
    }
}
