package com.gtnewhorizon.gtnhlib.config;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.client.config.IConfigElement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Class for controlling the loading of configuration files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigurationManager {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLibConfig");
    private static final Map<String, Configuration> configs = new HashMap<>();
    private static final Map<Configuration, Map<String, Set<Class<?>>>> configToCategoryClassMap = new HashMap<>();

    private static final ConfigurationManager instance = new ConfigurationManager();
    private static final Set<Class<?>> configFields = new HashSet<>();

    private static boolean initialized = false;

    private static Path configDir;

    static {
        configFields.add(Boolean.class);
        configFields.add(boolean.class);
        configFields.add(Integer.class);
        configFields.add(int.class);
        configFields.add(Float.class);
        configFields.add(float.class);
        configFields.add(Double.class);
        configFields.add(double.class);
        configFields.add(String.class);
        configFields.add(Enum.class);
    }

    /**
     * Registers a configuration class to be loaded. This should be done in preInit.
     *
     * @param configClass The class to register.
     */
    public static void registerConfig(Class<?> configClass) throws ConfigException {
        init();
        val cfg = Optional.ofNullable(configClass.getAnnotation(Config.class)).orElseThrow(
                () -> new ConfigException("Class " + configClass.getName() + " does not have a @Config annotation!"));
        val category = cfg.category().trim();
        val modid = cfg.modid();
        val filename = Optional.of(cfg.filename().trim()).filter(s -> !s.isEmpty()).orElse(modid);

        Configuration rawConfig = configs.computeIfAbsent(modid + "|" + filename, (ignored) -> {
            Path newConfigDir = configDir;
            if (!cfg.configSubDirectory().trim().isEmpty()) {
                newConfigDir = newConfigDir.resolve(cfg.configSubDirectory().trim());
            }
            val configFile = newConfigDir.resolve(filename + ".cfg").toFile();
            val config = new Configuration(configFile);
            config.load();
            return config;
        });

        configToCategoryClassMap.computeIfAbsent(rawConfig, (ignored) -> new HashMap<>())
                .computeIfAbsent(category, (ignored) -> new HashSet<>()).add(configClass);

        try {
            processConfigInternal(configClass, category, rawConfig, null);
            rawConfig.save();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            throw new ConfigException(e);
        }
    }

    public static void processSubCategory(Object instance, Configuration config, Field subCategoryField,
            String category, String name, String comment, String langKey) throws ConfigException {
        val cat = (category.isEmpty() ? "" : category + Configuration.CATEGORY_SPLITTER) + name.toLowerCase();
        ConfigCategory subCat = config.getCategory(cat);

        subCat.setComment(comment);
        subCat.setLanguageKey(langKey);
        if (subCategoryField.isAnnotationPresent(Config.RequiresMcRestart.class)) {
            subCat.setRequiresMcRestart(true);
        }
        if (subCategoryField.isAnnotationPresent(Config.RequiresWorldRestart.class)) {
            subCat.setRequiresWorldRestart(true);
        }

        try {
            Object subInstance = subCategoryField.get(instance);
            processConfigInternal(subCategoryField.getType(), cat, config, subInstance);
            config.save();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            throw new ConfigException(e);
        }
    }

    private static void processConfigInternal(Class<?> configClass, String category, Configuration rawConfig,
            @Nullable Object instance) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException,
            NoSuchFieldException, ConfigException {
        boolean foundCategory = !category.isEmpty();
        for (val field : configClass.getDeclaredFields()) {
            if (instance != null && Modifier.isStatic(field.getModifiers())) {
                throw new ConfigException(
                        "Illegal config field: " + field.getName()
                                + " in "
                                + configClass.getName()
                                + ": Static field in instance context! Did you forget an @Config.Ignore annotation?");
            }

            if (field.getAnnotation(Config.Ignore.class) != null) {
                continue;
            }

            field.setAccessible(true);
            var comment = Optional.ofNullable(field.getAnnotation(Config.Comment.class)).map(Config.Comment::value)
                    .map((lines) -> String.join("\n", lines)).orElse("");
            val name = getFieldName(field);
            val langKey = Optional.ofNullable(field.getAnnotation(Config.LangKey.class)).map(Config.LangKey::value)
                    .orElse(name);
            val fieldClass = field.getType();

            if (isFieldSubCategory(field)) {
                processSubCategory(instance, rawConfig, field, category, name, comment, langKey);
                foundCategory = true;
                continue;
            }

            if (category.isEmpty()) continue;

            val cat = rawConfig.getCategory(category);
            boolean boxed;
            if ((boxed = fieldClass.equals(Boolean.class)) || fieldClass.equals(boolean.class)) {
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultBoolean.class))
                        .map(Config.DefaultBoolean::value)
                        .orElse(boxed ? (Boolean) field.get(instance) : field.getBoolean(instance));
                field.setBoolean(instance, rawConfig.getBoolean(name, category, defaultValue, comment, langKey));
            } else if ((boxed = fieldClass.equals(Integer.class)) || fieldClass.equals(int.class)) {
                val range = Optional.ofNullable(field.getAnnotation(Config.RangeInt.class));
                val min = range.map(Config.RangeInt::min).orElse(Integer.MIN_VALUE);
                val max = range.map(Config.RangeInt::max).orElse(Integer.MAX_VALUE);
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultInt.class))
                        .map(Config.DefaultInt::value)
                        .orElse(boxed ? (Integer) field.get(instance) : field.getInt(instance));
                field.setInt(instance, rawConfig.getInt(name, category, defaultValue, min, max, comment, langKey));
            } else if ((boxed = fieldClass.equals(Float.class)) || fieldClass.equals(float.class)) {
                val range = Optional.ofNullable(field.getAnnotation(Config.RangeFloat.class));
                val min = range.map(Config.RangeFloat::min).orElse(Float.MIN_VALUE);
                val max = range.map(Config.RangeFloat::max).orElse(Float.MAX_VALUE);
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultFloat.class))
                        .map(Config.DefaultFloat::value)
                        .orElse(boxed ? (Float) field.get(instance) : field.getFloat(instance));
                field.setFloat(instance, rawConfig.getFloat(name, category, defaultValue, min, max, comment, langKey));
            } else if ((boxed = fieldClass.equals(Double.class)) || fieldClass.equals(double.class)) {
                val range = Optional.ofNullable(field.getAnnotation(Config.RangeDouble.class));
                val min = range.map(Config.RangeDouble::min).orElse(Double.MIN_VALUE);
                val max = range.map(Config.RangeDouble::max).orElse(Double.MAX_VALUE);
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultDouble.class))
                        .map(Config.DefaultDouble::value)
                        .orElse(boxed ? (Double) field.get(instance) : field.getDouble(instance));
                val defaultValueComment = comment + " [range: "
                        + min
                        + " ~ "
                        + max
                        + ", default: "
                        + defaultValue
                        + "]";
                field.setDouble(
                        instance,
                        rawConfig.get(category, name, defaultValue, defaultValueComment, min, max)
                                .setLanguageKey(langKey).getDouble());
            } else if (fieldClass.equals(String.class)) {
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultString.class))
                        .map(Config.DefaultString::value).orElse((String) field.get(instance));
                val pattern = Optional.ofNullable(field.getAnnotation(Config.Pattern.class)).map(Config.Pattern::value)
                        .map(Pattern::compile).orElse(null);
                field.set(instance, rawConfig.getString(name, category, defaultValue, comment, langKey, pattern));
            } else if (fieldClass.isEnum()) {
                val enumValues = Arrays.stream((Object[]) fieldClass.getDeclaredMethod("values").invoke(instance))
                        .map((obj) -> (Enum<?>) obj).collect(Collectors.toList());
                val defaultValue = (Enum<?>) Optional.ofNullable(field.getAnnotation(Config.DefaultEnum.class))
                        .map(Config.DefaultEnum::value).map((defName) -> extractField(fieldClass, defName))
                        .map(ConfigurationManager::extractValue).orElse(field.get(instance));
                val possibleValues = enumValues.stream().map(Enum::name).toArray(String[]::new);
                String value = rawConfig.getString(
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
                    LOGGER.warn(
                            "Invalid value " + value
                                    + " for enum configuration field "
                                    + field.getName()
                                    + " of type "
                                    + fieldClass.getName()
                                    + " in config class "
                                    + configClass.getName()
                                    + "! Using default value of "
                                    + defaultValue
                                    + "!");
                    field.set(instance, defaultValue);
                }
            } else if (fieldClass.isArray() && fieldClass.getComponentType().equals(String.class)) {
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultStringList.class))
                        .map(Config.DefaultStringList::value).orElse((String[]) field.get(instance));
                String[] value = rawConfig.getStringList(name, category, defaultValue, comment, null, langKey);
                field.set(instance, value);
            } else if (fieldClass.isArray() && fieldClass.getComponentType().equals(double.class)) {
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultDoubleList.class))
                        .map(Config.DefaultDoubleList::value).orElse((double[]) field.get(instance));

                String[] stringValues = new String[defaultValue.length];
                for (int i = 0; i < defaultValue.length; i++) {
                    stringValues[i] = Double.toString(defaultValue[i]);
                }
                comment = comment + " [default: " + Arrays.toString(stringValues) + "]";
                double[] value = rawConfig.get(category, name, defaultValue, comment).getDoubleList();

                field.set(instance, value);
            } else if (fieldClass.isArray() && fieldClass.getComponentType().equals(int.class)) {
                val defaultValue = Optional.ofNullable(field.getAnnotation(Config.DefaultIntList.class))
                        .map(Config.DefaultIntList::value).orElse((int[]) field.get(instance));

                String[] stringValues = new String[defaultValue.length];
                for (int i = 0; i < defaultValue.length; i++) {
                    stringValues[i] = Integer.toString(defaultValue[i]);
                }
                comment = comment + " [default: " + Arrays.toString(stringValues) + "]";
                int[] value = rawConfig.get(category, name, defaultValue, comment).getIntList();

                field.set(instance, value);
            } else {
                throw new ConfigException(
                        "Illegal config field: " + field.getName()
                                + " in "
                                + configClass.getName()
                                + ": Unsupported type "
                                + fieldClass.getName()
                                + "! Did you forget an @Ignore annotation?");
            }
            if (field.isAnnotationPresent(Config.RequiresMcRestart.class)) {
                cat.setRequiresMcRestart(true);
            }
            if (field.isAnnotationPresent(Config.RequiresWorldRestart.class)) {
                cat.setRequiresWorldRestart(true);
            }
        }

        if (!foundCategory) {
            throw new ConfigException("No category found for config class " + configClass.getName() + "!");
        }
    }

    @SneakyThrows
    private static Field extractField(Class<?> clazz, String field) {
        return clazz.getDeclaredField(field);
    }

    @SneakyThrows
    private static Object extractValue(Field field) {
        return field.get(null);
    }

    /**
     * Process the configuration into a list of config elements usable in config GUI code.
     *
     * @param configClass The class to process.
     *
     * @return The configuration elements.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<IConfigElement> getConfigElements(Class<?> configClass) throws ConfigException {
        init();
        val cfg = Optional.ofNullable(configClass.getAnnotation(Config.class)).orElseThrow(
                () -> new ConfigException("Class " + configClass.getName() + " does not have a @Config annotation!"));
        val modid = cfg.modid();
        val filename = Optional.of(cfg.filename().trim()).filter(s -> !s.isEmpty()).orElse(modid);
        val rawConfig = Optional.ofNullable(configs.get(modid + "|" + filename)).map(
                (conf) -> Optional.ofNullable(configToCategoryClassMap.get(conf))
                        .map((map) -> map.get(cfg.category().trim()).contains(configClass)).orElse(false) ? conf : null)
                .orElseThrow(
                        () -> new ConfigException("Tried to get config elements for non-registered config class!"));
        val category = cfg.category();
        val elements = category.isEmpty() ? getSubcategoryElements(configClass, category, rawConfig, false)
                : new ConfigElement<>(rawConfig.getCategory(category)).getChildElements();
        return elements.stream().map((element) -> new IConfigElementProxy(element, () -> {
            try {
                processConfigInternal(configClass, category, rawConfig, null);
                rawConfig.save();
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException
                    | ConfigException e) {
                e.printStackTrace();
            }
        })).collect(Collectors.toList());
    }

    /**
     * Process the configuration into a list of config elements split by category usable in config GUI code.
     *
     * @param configClass The class to process.
     *
     * @return The configuration elements.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<IConfigElement> getCategorizedElements(Class<?> configClass) throws ConfigException {
        val cfg = Optional.ofNullable(configClass.getAnnotation(Config.class)).orElseThrow(
                () -> new ConfigException("Class " + configClass.getName() + " does not have a @Config annotation!"));;
        val modid = cfg.modid();
        val filename = Optional.of(cfg.filename().trim()).filter(s -> !s.isEmpty()).orElse(modid);
        val rawConfig = Optional.ofNullable(configs.get(modid + "|" + filename)).map(
                (conf) -> Optional.ofNullable(configToCategoryClassMap.get(conf))
                        .map((map) -> map.get(cfg.category().trim()).contains(configClass)).orElse(false) ? conf : null)
                .orElseThrow(
                        () -> new ConfigException("Tried to get config elements for non-registered config class!"));
        val category = cfg.category();

        if (category.isEmpty()) {
            return getSubcategoryElements(configClass, category, rawConfig, true);
        }

        if (category.indexOf('.') != -1) {
            return Collections.emptyList();
        }

        return Collections
                .singletonList(new IConfigElementProxy(new ConfigElement(rawConfig.getCategory(category)), () -> {
                    try {
                        processConfigInternal(configClass, category, rawConfig, null);
                        rawConfig.save();
                    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException
                            | NoSuchFieldException | ConfigException e) {
                        e.printStackTrace();
                    }
                }));
    }

    @SuppressWarnings({ "rawtypes" })
    public static List<IConfigElement> getConfigElementsMulti(Class<?>... configClasses) throws ConfigException {
        return getConfigElementsMulti(false, configClasses);
    }

    @SuppressWarnings({ "rawtypes" })
    public static List<IConfigElement> getConfigElementsMulti(boolean categorized, Class<?>... configClasses)
            throws ConfigException {
        switch (configClasses.length) {
            case 0:
                return Collections.emptyList();
            case 1:
                return categorized ? getCategorizedElements(configClasses[0]) : getConfigElements(configClasses[0]);
            default:
                val result = new ArrayList<IConfigElement>();
                for (val configClass : configClasses) {
                    List<IConfigElement> elements = categorized ? getCategorizedElements(configClass)
                            : getConfigElements(configClass);
                    result.addAll(elements);
                }
                return result;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<IConfigElement> getSubcategoryElements(Class<?> configClass, String category,
            Configuration rawConfig, boolean categorized) throws ConfigException {
        List<IConfigElement> elements = new ArrayList<>();
        for (val field : configClass.getDeclaredFields()) {
            if (isFieldSubCategory(field)) {
                val name = getFieldName(field).toLowerCase();

                IConfigElementProxy<?> element = new IConfigElementProxy(
                        new ConfigElement(rawConfig.getCategory(name)),
                        () -> {
                            try {
                                processConfigInternal(configClass, category, rawConfig, null);
                                rawConfig.save();
                            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException
                                    | NoSuchFieldException | ConfigException e) {
                                e.printStackTrace();
                            }
                        });

                elements.add(element);

            }
        }

        if (categorized) return elements;

        return (List<IConfigElement>) elements.stream().flatMap(element -> element.getChildElements().stream())
                .collect(Collectors.toList());
    }

    private static String getFieldName(Field f) {
        if (f.isAnnotationPresent(Config.Name.class)) {
            return f.getAnnotation(Config.Name.class).value();
        }
        return f.getName();
    }

    private static boolean isFieldSubCategory(Field field) {
        Class<?> fieldClass = field.getType();
        return !fieldClass.isArray() && !configFields.contains(fieldClass)
                && fieldClass.getSuperclass() != null
                && fieldClass.getSuperclass().equals(Object.class);
    }

    private static File minecraftHome() {
        return Launch.minecraftHome != null ? Launch.minecraftHome : new File(".");
    }

    private static void init() {
        if (initialized) {
            return;
        }
        configDir = minecraftHome().toPath().resolve("config");
        initialized = true;
    }
}
