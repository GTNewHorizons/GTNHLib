package com.gtnewhorizon.gtnhlib.util;

import java.io.IOException;
import java.net.URI;

import org.lwjgl.Sys;

import com.gtnewhorizon.gtnhlib.GTNHLib;

public class FilesUtil {

    public static void openUri(URI uri) {
        switch (net.minecraft.util.Util.getOSType()) {
            case OSX -> {
                try {
                    Runtime.getRuntime().exec(new String[] { "/usr/bin/open", uri.toString() });
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case WINDOWS -> {
                try {
                    Runtime.getRuntime()
                            .exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", uri.toString() });
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case LINUX -> {
                try {
                    Runtime.getRuntime().exec(new String[] { "xdg-open", uri.toString() });
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            default -> {}
        }
        boolean openViaSystemClass = false;

        try {
            final Class<?> aClass = Class.forName("java.awt.Desktop");
            final Object getDesktop = aClass.getMethod("getDesktop").invoke(null);
            aClass.getMethod("browse", URI.class).invoke(getDesktop, uri);
        } catch (Exception e) {
            e.printStackTrace();
            openViaSystemClass = true;
        }

        if (openViaSystemClass) {
            GTNHLib.LOG.debug("Opening via system class!");
            Sys.openURL("file://" + uri);
        }
    }

}
