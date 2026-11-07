package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

import cpw.mods.fml.common.registry.GameRegistry;

/// Appends the localized name of the given item id.
///
/// Usage Example:
/// ```text
/// Consumes {var:amount} {item:gregtech:gt.blockmachines:15757} per minute
/// ```
public class ItemCommand extends TemplatedTextCommand {

    public ItemCommand() {
        super("ITEM");
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        String[] chunks = arg.split(TemplatedTextLoader.ARG_SPLITTER_PATTERN);

        switch (chunks.length) {
            case 2 -> {
                Item item = GameRegistry.findItem(chunks[0].trim(), chunks[1].trim());

                if (item == null) {
                    appendError(lineOutput, "INVALID ITEM: " + chunks[0] + ":" + chunks[1]);
                } else {
                    lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK).append(new ItemStack(item, 1).getDisplayName())
                            .append(TextFormatUtil.FORMAT_POP_STACK);
                }
            }
            case 3 -> {
                Item item = GameRegistry.findItem(chunks[0].trim(), chunks[1].trim());

                if (item == null) {
                    appendError(lineOutput, "INVALID ITEM: " + chunks[0] + ":" + chunks[1]);
                    return;
                }

                int meta;

                try {
                    meta = Integer.parseInt(chunks[2].trim());
                } catch (NumberFormatException e) {
                    appendError(lineOutput, "INVALID META: " + chunks[2]);
                    return;
                }

                lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK)
                        .append(new ItemStack(item, 1, meta).getDisplayName()).append(TextFormatUtil.FORMAT_POP_STACK);
            }
        }
    }
}
