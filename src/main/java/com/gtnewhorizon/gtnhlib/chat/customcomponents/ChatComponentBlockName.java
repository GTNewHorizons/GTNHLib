package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;

/**
 * A chat component that displays the localized name of a block.
 */
public class ChatComponentBlockName extends AbstractChatComponentCustom {

    /**
     * Registry name of the block. Never {@code null}; an empty string means there is no block to display.
     */
    private String registryName = "";

    public ChatComponentBlockName() {}

    public ChatComponentBlockName(@NotNull Block block) {
        this(Block.blockRegistry.getNameForObject(Objects.requireNonNull(block, "block")));
    }

    public ChatComponentBlockName(@NotNull String registryName) {
        this.registryName = Objects.requireNonNull(registryName, "registryName");
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentBlockName";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        // The registry name is immutable, so the copy can safely share it.
        return new ChatComponentBlockName(registryName);
    }

    @Override
    public String getUnformattedTextForChat() {
        Block block = registryName.isEmpty() ? null : Block.getBlockFromName(registryName);
        return block == null ? StatCollector.translateToLocalFormatted("gtnhlib.chat.comp.block.error", registryName)
                : block.getLocalizedName();
    }

    @Override
    public @NotNull JsonElement serialize() {
        final JsonObject obj = new JsonObject();
        obj.addProperty("registryName", registryName);

        return obj;
    }

    @Override
    public void deserialize(@NotNull JsonElement jsonElement) {
        this.registryName = jsonElement.getAsJsonObject().get("registryName").getAsString();
        if (!registryName.isEmpty() && Block.getBlockFromName(registryName) == null) {
            GTNHLib.LOG.error("Failed to deserialize block from name: {}", registryName);
        }
    }

}
