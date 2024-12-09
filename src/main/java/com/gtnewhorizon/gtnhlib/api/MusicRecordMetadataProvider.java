package com.gtnewhorizon.gtnhlib.api;

import java.util.Collections;

import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Implement on classes extending {@link ItemRecord} to provide metadata and NBT-aware versions of the standard music
 * metadata for use by other mods that implement custom jukeboxes.
 */
public interface MusicRecordMetadataProvider {

    /**
     * @param stack The ItemStack to get the sound resource for.
     * @return The ResourceLocation of the sound this record plays, or {@code null} if none.
     */
    default ResourceLocation getMusicRecordResource(ItemStack stack) {
        if (stack == null || stack.getItem() != this) {
            return null;
        }
        final ItemRecord self = (ItemRecord) this;
        return self.getRecordResource(self.recordName);
    }

    /**
     * @return A list of all variants of this music record item that have playable music.
     */
    default Iterable<ItemStack> getMusicRecordVariants() {
        return Collections.singleton(new ItemStack((ItemRecord) this, 1, 0));
    }
}
