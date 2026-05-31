package com.gtnewhorizon.gtnhlib.integration.mui2;

import net.minecraft.entity.player.EntityPlayer;

public class GuiUtils {

    public static String clampString(String str, int length) {
        return str.length() <= length ? str : (str.substring(0, length) + "...");
    }

    public static boolean isOpServerSideOnly(EntityPlayer player) {
        return player.canCommandSenderUseCommand(2, "gtnhteam_admin");
    }
}
