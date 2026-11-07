package com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

/// Used to debug parsing by showing both the unparsed line and it's evaluated equivalent.
///
/// The example line always starts with `Example:`
///
/// The output lines always starts with `Output:`
///
/// If there is no output the output line will say `[OUTPUT SKIPPED]`
///
/// Usage Example:
/// ```text
/// #example:this is how you use a var: {var:some_var}%
/// #example:#if:var_false:this won't appear
/// ```
///
/// Output Example
/// ```text
/// Example:this is how you use a var: {var:amount}%
/// Output:this is how you use a var: 200%
/// ```
/// ```text
/// Example:#if:var_false:this won't appear
/// [OUTPUT SKIPPED]
/// ```
@ApiStatus.Internal
public class ExampleLineProcessor implements ITemplatedTextLineProcessor {

    @Override
    public void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> lineOutput, @NotNull String line,
            @NotNull Map<String, Object> vars, Stack<ResourceLocation> includeHistory) {
        lineOutput.add(EnumChatFormatting.DARK_GRAY + "Example:" + line);
        final int oldSize = lineOutput.size();
        loader.evaluateLines(lineOutput, Collections.singletonList(line), vars, includeHistory);
        final int newSize = lineOutput.size();
        if (oldSize == newSize) {
            lineOutput.add(TextFormatUtil.wrapFormatting(EnumChatFormatting.DARK_GRAY.toString(), "[OUTPUT SKIPPED]"));
        } else if (newSize > oldSize) {
            for (int i = oldSize; i < newSize; i++) {
                lineOutput.set(
                        i,
                        TextFormatUtil.wrapFormatting(EnumChatFormatting.DARK_GRAY.toString(), "Output:")
                                + lineOutput.get(i));
            }
        }
    }
}
