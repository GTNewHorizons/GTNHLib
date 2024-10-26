package com.gtnewhorizon.gtnhlib.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import net.minecraft.client.Minecraft;

public class TexturesDebug {

    private static boolean initSpriteLogger = false;
    private static PrintStream logAtlasSprite = null;
    private static boolean initDynamicLogger = false;
    private static PrintStream logDynamic = null;

    public static void logTextureAtlasSprite(String iconName, int width, int height, int frames, int sizeBytes) {
        logTextureAtlasSprite(iconName + "," + width + "," + height + "," + frames + "," + sizeBytes);
    }

    private static void logTextureAtlasSprite(String message) {
        if (!initSpriteLogger) {
            logAtlasSprite = initLogger("TexturesDebug.csv");
            initSpriteLogger = true;
            logTextureAtlasSprite("iconName,width,height,frames,sizeBytes");
        }
        if (logAtlasSprite != null) {
            logAtlasSprite.println(message);
        }
    }

    public static void logDynamicTexture(int width, int height) {
        if (!initDynamicLogger) {
            logDynamic = initLogger("DynamicTextures.txt");
            initDynamicLogger = true;
        }
        if (logDynamic != null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            logDynamic.println("Created texture of width " + width + " height " + height);
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement element = stackTrace[i];
                if (i == 4 || i == 5) {
                    logDynamic.println("at " + element);
                    break;
                }
            }
            logDynamic.println(" ");
        }
    }

    private static PrintStream initLogger(String file) {
        final File logFile = new File(Minecraft.getMinecraft().mcDataDir, file);
        if (logFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            logFile.delete();
        }
        if (!logFile.exists()) {
            try {
                // noinspection ResultOfMethodCallIgnored
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            return new PrintStream(new FileOutputStream(logFile, true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
