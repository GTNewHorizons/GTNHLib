package com.gtnewhorizon.gtnhlib.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import net.minecraft.launchwrapper.Launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Based on EarlyConfig.java from Hodgepodge
public final class EarlyConfig {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLibEarly");

    public static final boolean enableFontRendererMixin;

    static {
        Properties config = new Properties();
        File configLocation = new File(Launch.minecraftHome, "config/gtnhlibEarly.properties");
        try (Reader r = new BufferedReader(new FileReader(configLocation))) {
            config.load(r);
        } catch (FileNotFoundException e) {
            LOGGER.debug("No existing configuration file. Will use defaults");
        } catch (IOException e) {
            LOGGER.error("Error reading configuration file. Will use defaults", e);
        }
        enableFontRendererMixin = Boolean.parseBoolean(config.getProperty("enableFontRendererMixin", "true"));
        config.setProperty("enableFontRendererMixin", String.valueOf(enableFontRendererMixin));
        try (Writer r = new BufferedWriter(new FileWriter(configLocation))) {
            config.store(r, "Config file for anything that's loaded before GTNHLibConfig");
        } catch (IOException e) {
            LOGGER.error("Error writing configuration file", e);
        }
    }
}
