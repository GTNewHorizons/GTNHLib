package com.gtnewhorizon.gtnhlib.config;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import lombok.val;

/**
 * Class for controlling the loading of configuration files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigurationManager {

    static final Logger LOGGER = LogManager.getLogger("GTNHLibConfig");
    private static final Map<String, Configuration> configs = new HashMap<>();
    private static final Map<Configuration, Map<String, Set<Class<?>>>> configToCategoryClassMap = new HashMap<>();
    private static final String[] langKeyPlaceholders = new String[] { "%mod", "%cat", "%file", "%field" };

    private static final ConfigurationManager instance = new ConfigurationManager();

    private static boolean initialized = false;

    private static Path configDir;

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

        Configuration rawConfig = configs.computeIfAbsent(getConfigKey(cfg), (ignored) -> {
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

    public static void save(Class<?>... configClasses) {
        if (configClasses.length == 0) return;

        Set<Configuration> savedConfigs = new HashSet<>();
        for (val clazz : configClasses) {
            try {
                val cfg = Optional.ofNullable(clazz.getAnnotation(Config.class)).orElseThrow(
                        () -> new ConfigException("Class " + clazz.getName() + " does not have a @Config annotation!"));
                val category = cfg.category().trim();
                Configuration rawConfig = configs.get(getConfigKey(cfg));
                save(clazz, null, rawConfig, category);
                savedConfigs.add(rawConfig);
            } catch (IllegalAccessException | ConfigException e) {
                LOGGER.error("Failed to save config for class {}", clazz.getName(), e);
            }
        }

        savedConfigs.forEach(Configuration::save);
    }

    private static void save(Class<?> configClass, Object instance, Configuration rawConfig, String category)
            throws IllegalAccessException, ConfigException {
        for (val field : configClass.getDeclaredFields()) {
            if (field.getAnnotation(Config.Ignore.class) != null) {
                continue;
            }

            field.setAccessible(true);

            if (!ConfigFieldParser.canParse(field)) {
                if (isFieldSubCategory(field)) {
                    val cat = (category.isEmpty() ? "" : category + Configuration.CATEGORY_SPLITTER)
                            + ConfigFieldParser.getFieldName(field).toLowerCase();
                    Object subInstance = field.get(instance);
                    save(field.getType(), subInstance, rawConfig, cat);
                }
                continue;
            }

            if (category.isEmpty()) continue;

            ConfigFieldParser.saveField(instance, field, rawConfig, category);
        }
    }

    private static void processSubCategory(Object instance, Configuration config, Field subCategoryField,
            String category) throws ConfigException {
        var comment = Optional.ofNullable(subCategoryField.getAnnotation(Config.Comment.class))
                .map(Config.Comment::value).map((lines) -> String.join("\n", lines)).orElse("");
        val name = ConfigFieldParser.getFieldName(subCategoryField);
        val cat = (category.isEmpty() ? "" : category + Configuration.CATEGORY_SPLITTER) + name.toLowerCase();
        ConfigCategory subCat = config.getCategory(cat);
        val langKey = getLangKey(
                subCategoryField.getType(),
                subCategoryField.getAnnotation(Config.LangKey.class),
                name,
                subCat.getName(),
                true);

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
        ConfigCategory cat = foundCategory ? rawConfig.getCategory(category) : null;
        boolean requiresMcRestart = getClassOrBaseAnnotation(configClass, Config.RequiresMcRestart.class) != null
                || foundCategory && cat.requiresMcRestart();
        boolean requiresWorldRestart = getClassOrBaseAnnotation(configClass, Config.RequiresWorldRestart.class) != null
                || foundCategory && cat.requiresWorldRestart();

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

            if (!ConfigFieldParser.canParse(field)) {
                if (isFieldSubCategory(field)) {
                    processSubCategory(instance, rawConfig, field, category);
                    foundCategory = true;
                    continue;
                }

                throw new ConfigException(
                        "Illegal config field: " + field.getName()
                                + " in "
                                + configClass.getName()
                                + ": Unsupported type "
                                + field.getType().getName()
                                + "! Did you forget an @Ignore annotation?");
            }

            if (category.isEmpty()) continue;

            val langKey = getLangKey(
                    configClass,
                    field.getAnnotation(Config.LangKey.class),
                    ConfigFieldParser.getFieldName(field),
                    category,
                    false);
            ConfigFieldParser.loadField(instance, field, rawConfig, category, langKey);

            if (!requiresMcRestart) {
                requiresMcRestart = field.isAnnotationPresent(Config.RequiresMcRestart.class);
            }

            if (!requiresWorldRestart) {
                requiresWorldRestart = field.isAnnotationPresent(Config.RequiresWorldRestart.class);
            }
        }

        if (!foundCategory) {
            throw new ConfigException("No category found for config class " + configClass.getName() + "!");
        }

        if (cat == null) cat = rawConfig.getCategory(category);
        val langKey = getLangKey(
                configClass,
                configClass.getAnnotation(Config.LangKey.class),
                null,
                cat.getName(),
                true);
        cat.setLanguageKey(langKey);
        cat.setRequiresMcRestart(requiresMcRestart);
        cat.setRequiresWorldRestart(requiresWorldRestart);
    }

    /**
     * Process the configuration into a list of config elements usable in config GUI code.
     *
     * @param configClass The class to process.
     *
     * @return The configuration elements.
     */
    @SuppressWarnings("rawtypes")
    public static List<IConfigElement> getConfigElements(Class<?> configClass) throws ConfigException {
        return getConfigElements(configClass, false);
    }

    /**
     * Process the configuration into a list of config elements usable in config GUI code.
     *
     * @param configClass The class to process.
     * @param categorized Whether to return the elements split by category.
     * @return The configuration elements.
     */
    @SuppressWarnings("rawtypes")
    public static List<IConfigElement> getConfigElements(Class<?> configClass, boolean categorized)
            throws ConfigException {
        init();
        val cfg = Optional.ofNullable(configClass.getAnnotation(Config.class)).orElseThrow(
                () -> new ConfigException("Class " + configClass.getName() + " does not have a @Config annotation!"));
        val rawConfig = Optional.ofNullable(configs.get(getConfigKey(cfg))).map(
                (conf) -> Optional.ofNullable(configToCategoryClassMap.get(conf))
                        .map((map) -> map.get(cfg.category().trim()).contains(configClass)).orElse(false) ? conf : null)
                .orElseThrow(
                        () -> new ConfigException("Tried to get config elements for non-registered config class!"));

        val category = cfg.category();
        List<IConfigElement> result = new ArrayList<>();
        if (categorized) {
            if (category.isEmpty()) return getSubcategoryElements(configClass, category, rawConfig, true);
            if (category.indexOf('.') != -1) return Collections.emptyList();
            result.add(
                    getProxyElement(
                            new ConfigElement<>(rawConfig.getCategory(category)),
                            configClass,
                            rawConfig,
                            category));
        } else {
            val elements = category.isEmpty() ? getSubcategoryElements(configClass, category, rawConfig, false)
                    : new ConfigElement<>(rawConfig.getCategory(category)).getChildElements();
            elements.stream().map((element) -> getProxyElement(element, configClass, rawConfig, category))
                    .forEach(result::add);
        }

        return result;
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
                return getConfigElements(configClasses[0], categorized);
            default:
                val result = new ArrayList<IConfigElement>();
                for (val configClass : configClasses) {
                    List<IConfigElement> elements = getConfigElements(configClass, categorized);
                    result.addAll(elements);
                }
                return result;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<IConfigElement> getSubcategoryElements(Class<?> configClass, String category,
            Configuration rawConfig, boolean categorized) {
        List<IConfigElement> elements = new ArrayList<>();
        for (val field : configClass.getDeclaredFields()) {
            if (isFieldSubCategory(field)) {
                val name = ConfigFieldParser.getFieldName(field).toLowerCase();
                elements.add(
                        getProxyElement(
                                new ConfigElement(rawConfig.getCategory(name)),
                                configClass,
                                rawConfig,
                                category));
            }
        }

        if (categorized) return elements;

        return (List<IConfigElement>) elements.stream().flatMap(element -> element.getChildElements().stream())
                .collect(Collectors.toList());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static IConfigElementProxy<?> getProxyElement(IConfigElement<?> element, Class<?> configClass,
            Configuration rawConfig, String category) {
        if (element instanceof IConfigElementProxy<?>proxy) {
            return proxy;
        }

        return new IConfigElementProxy(element, () -> {
            try {
                processConfigInternal(configClass, category, rawConfig, null);
                rawConfig.save();
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException
                    | ConfigException e) {
                e.printStackTrace();
            }
        });
    }

    private static String getLangKey(Class<?> configClass, @Nullable Config.LangKey langKey, @Nullable String fieldName,
            String categoryName, boolean isCategory) throws ConfigException {
        if (langKey != null) return langKey.value();

        Config.LangKeyPattern pattern = getClassOrBaseAnnotation(configClass, Config.LangKeyPattern.class);
        String name = Optional.ofNullable(fieldName).orElse(categoryName);
        if (pattern == null) return name;
        String patternStr = pattern.pattern();

        if (!patternStr.contains("%field") || !patternStr.contains(".")) {
            throw new ConfigException("Invalid pattern for class " + configClass.getName() + ": " + patternStr);
        }

        Config cfg = getClassOrBaseAnnotation(configClass, Config.class);
        // Config annotation can't be null at this point
        assert cfg != null;

        return buildKeyFromPattern(cfg, patternStr, name, isCategory);
    }

    private static String buildKeyFromPattern(Config cfg, String pattern, String fieldName, boolean isCategory) {
        StringBuilder s = new StringBuilder(pattern);
        String[] replacements = new String[] { cfg.modid(), cfg.category(), cfg.filename(), fieldName };
        for (int i = 0; i < langKeyPlaceholders.length; i++) {
            String placeholder = langKeyPlaceholders[i];
            int index = s.indexOf(placeholder);
            if (index == -1) continue;
            int nextIndex = index + placeholder.length();
            if (isCategory && "%field".equals(placeholder)) {
                if (s.charAt(index + 1) == '.') {
                    s.delete(index, nextIndex + 1);
                } else {
                    s.delete(index - 1, nextIndex);
                }
                continue;
            }
            s.replace(index, nextIndex, replacements[i].toLowerCase());
        }

        return s.toString();
    }

    private static @Nullable <A extends Annotation> A getClassOrBaseAnnotation(Class<?> clazz,
            Class<A> annotationClass) {
        A annotation = clazz.getAnnotation(annotationClass);
        if (annotation != null || !clazz.isMemberClass()) return annotation;

        while (clazz.isMemberClass()) {
            clazz = clazz.getDeclaringClass();
        }

        return clazz.getAnnotation(annotationClass);
    }

    private static boolean isFieldSubCategory(@Nullable Field field) {
        if (field == null) return false;

        Class<?> fieldClass = field.getType();
        return !ConfigFieldParser.canParse(field) && fieldClass.getSuperclass() != null
                && fieldClass.getSuperclass().equals(Object.class);
    }

    private static String getConfigKey(Config cfg) {
        return cfg.modid() + "|" + cfg.configSubDirectory() + "|" + cfg.filename();
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
