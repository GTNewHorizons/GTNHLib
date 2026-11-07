package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Appends the localised text associated with a given lang key and optionally processes the args and result for more
/// templated text commands.
///
/// Usage Examples:
/// ```text
/// {lang:key}
/// {lang:lang.key:arg1:arg2:...}
/// {lang-eval:lang.key.that.contains.templated.text:{var:speed}}
/// ```
public class LangCommand implements ITemplatedTextCommand {

    private final boolean eval;

    public LangCommand(boolean eval) {
        this.eval = eval;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {

        String translated;
        if (arg.contains(TemplatedTextLoader.ARG_SPLITTER_PATTERN)) {
            List<Object> paramList = new ArrayList<>();
            StringBuilder paramBuffer = new StringBuilder();

            // We use the same parse for eval and no-eval to keep the syntax the same.
            // If the command 'quoting' isn't desirable, the end-user can escape braces to split on colons within
            // command-like structures.
            TemplatedTextLoader.parseCommands(arg, new TemplatedTextLoader.CommandVisitor() {

                @Override
                public void visitChar(char c) {
                    if (c == TemplatedTextLoader.ARG_SPLITTER) {
                        paramList.add(paramBuffer.toString());
                        paramBuffer.setLength(0);
                    } else {
                        paramBuffer.append(c);
                    }
                }

                @Override
                public void visitCommand(String command) {
                    if (eval) {
                        paramBuffer.append(loader.eval(command, vars));
                    } else {
                        paramBuffer.append(command);
                    }
                }
            });

            if (paramBuffer.length() <= 0) {
                paramList.add(paramBuffer.toString());
            }

            String key = paramList.remove(0).toString();

            // TODO: implement the arg processing of the gt5u localized class or just migrate it to GTNHlib entirely
            translated = StatCollector.translateToLocalFormatted(key, paramList);
        } else {
            translated = StatCollector.translateToLocal(arg);
        }

        if (eval) {
            loader.emit(translated, lineOutput, vars);
        } else {
            lineOutput.append(translated);
        }
    }
}
