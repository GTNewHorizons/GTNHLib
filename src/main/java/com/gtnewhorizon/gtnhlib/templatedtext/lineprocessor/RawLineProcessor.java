package com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Prevents the evaluation of any commands or line processor on this line
///
/// Usage Example:
/// ```
/// #raw:{var:some_var} will not get parsed
/// #raw:{red}formatting commands are not parsed but §s§ccolor§t codes still function
/// ```
public class RawLineProcessor implements ITemplatedTextLineProcessor {

    @Override
    public void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> lineOutput, @NotNull String line,
            @NotNull TemplatedTextContext context) {
        lineOutput.add(line);
    }
}
