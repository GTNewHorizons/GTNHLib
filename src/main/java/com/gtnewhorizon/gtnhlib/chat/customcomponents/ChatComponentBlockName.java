package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

    protected ChatComponentBlockName(@NotNull String registryName) {
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
        final Block block = Block.getBlockFromName(registryName);
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
        final JsonElement registryNameElement = jsonElement.getAsJsonObject().get("registryName");
        if (registryNameElement == null) {
            throw new JsonParseException("Missing 'registryName' property in JSON for ChatComponentBlockName");
        }
        this.registryName = registryNameElement.getAsString();
        if (registryName.isEmpty() || Block.getBlockFromName(registryName) == null) {
            throw new JsonParseException("Failed to deserialize block from name: " + registryName + ".");
        }
    }

}
