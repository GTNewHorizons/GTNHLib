package com.gtnewhorizon.gtnhlib.client.title;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
class TitleParticle {

    float x, y;
    float vx, vy;
    float r, g, b;
    float size;
    float rotation, rotationSpeed;
    int lifetime, maxLifetime;
    ItemStack itemIcon;

    float alpha() {
        float progress = (float) lifetime / maxLifetime;
        if (progress < 0.2F) return progress / 0.2F;
        if (progress > 0.8F) return (1.0F - progress) / 0.2F;
        return 1.0F;
    }
}
