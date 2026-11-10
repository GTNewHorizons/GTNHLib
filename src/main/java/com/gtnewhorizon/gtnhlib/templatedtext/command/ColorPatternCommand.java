package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

import it.unimi.dsi.fastutil.ints.IntObjectPair;

/// Repeats a color sequence over the given text.
///
/// Any existing color codes in the contained text will be ignored.
///
/// Usage Examples:
/// ```text
/// {color-pattern:dark_red,red,gold,yellow,green,aqua,light_purple,dark_purple:rainbow text is easy}
/// {color-pattern:green,2*dark_green|bold:RADIATION}
/// ```
public class ColorPatternCommand extends TemplatedTextCommand {

    private static final String REPEAT_SEPARATOR = "\\*";
    private static final String FORMAT_SEPARATOR = "\\|";
    private static final String PATTERN_SEPARATOR = ",";

    public ColorPatternCommand() {
        super("COLOR-PATTERN");
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        // format should be {command-name:tierVar:text to convert}
        String[] parts = TemplatedTextLoader.splitArgs(arg, 2);

        // must have 2 parts
        if (parts.length != 2) {
            appendArgCountError(lineOutput, 2, parts.length);
            return;
        }

        // ignore if text is empty
        if (parts[1].isEmpty()) return;

        // colors are is required
        if (parts[0].isEmpty()) {
            appendError(lineOutput, "PATTERN IS EMPTY");
            return;
        }

        // translate the codes
        StringBuilder sb = new StringBuilder();
        String[] unparsed = parts[0].split(PATTERN_SEPARATOR);
        List<IntObjectPair<String>> parsed = new ArrayList<>(unparsed.length);
        for (String s : unparsed) {
            String[] repetition = s.split(REPEAT_SEPARATOR, 2);
            int count;
            String params;
            if (repetition.length == 2) {
                String toParse = repetition[0];
                try {
                    count = Integer.parseInt(toParse);
                } catch (NumberFormatException nfe) {
                    appendError(lineOutput, "BAD REPETITION COUNT: " + toParse);
                    return;
                }
                params = repetition[1];
            } else {
                count = 1;
                params = repetition[0];
            }

            for (String format : params.split(FORMAT_SEPARATOR)) {
                var formatting = EnumChatFormatting.getValueByName(format);
                if (formatting == null) {
                    appendError(lineOutput, "UNKNOWN COLOR: " + format);
                    return;
                }
                sb.append(formatting);
            }
            parsed.add(IntObjectPair.of(count, sb.toString()));
            sb.delete(0, sb.length());
        }

        // evaluate the contents
        loader.emit(parts[1], sb, context);
        char[] arr = sb.toString().toCharArray();
        // render out the patterned text
        IntObjectPair<String> active = parsed.get(0);
        int remaining = active.keyInt();
        int pos = 0;
        for (int i = 0; i < arr.length; i++) {
            // skip and include passed tags
            if (arr[i] == TextFormatUtil.FORMAT_ESCAPE_PREFIX) {
                lineOutput.append(arr[i++]).append(arr[i]);
                continue;
            }
            lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK).append(active.value()).append(arr[i])
                    .append(TextFormatUtil.FORMAT_POP_STACK);
            if (--remaining <= 0) {
                active = parsed.get(++pos % parsed.size());
                remaining = active.keyInt();
            }
        }
    }
}
