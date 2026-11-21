package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.List;

import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Appends the localised text associated with a given lang key and optionally processes the args and result for more
/// templated text commands.
///
/// Usage Examples:
/// ```text
/// # calls StatCollector.translateToLocal("lang.key")
/// {lang:lang.key}
/// # regular lang does not support params so this calls StatCollector.translateToLocal("lang.key:arg1:arg2")
/// {lang:lang.key:arg1:arg2}
/// # calls emit(StatCollector.translateToLocal("lang.key", "arg1", "arg2", ...))
/// {lang-veal:lang.key:arg1:arg2:...}
/// # calls StatCollector.translateToLocal("lang.key", "arg1", "arg2", ...)
/// {lang-eval:lang.key.that.contains.templated.text:{var:speed}}
/// ```
public class LangCommand implements ITemplatedTextCommand {

    private final boolean eval;

    public LangCommand(boolean eval) {
        this.eval = eval;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {

        String translated;
        if (arg.indexOf(TemplatedTextLoader.ARG_SPLITTER) >= 0) {
            List<String> paramList = TemplatedTextLoader.splitArgsWithCommands(loader, arg, -1, eval, context);
            String key = paramList.remove(0);

            // TODO: implement the arg processing of the gt5u localized class or just migrate it to GTNHlib entirely
            translated = StatCollector.translateToLocalFormatted(key, paramList.toArray());
        } else {
            translated = StatCollector.translateToLocal(arg);
        }

        if (eval) {
            loader.emit(translated, lineOutput, context);
        } else {
            lineOutput.append(translated);
        }
    }
}
