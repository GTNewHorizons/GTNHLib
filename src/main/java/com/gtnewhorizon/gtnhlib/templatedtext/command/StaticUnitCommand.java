package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Enforces formatting or naming conventions for a static unit.
///
/// This helps writers write what ever format they like and all the values get rendered the same to the player so
/// regardless of writer preference, the outcome is the same to the player.
///
/// Usage Examples:
/// ```text
/// {var:energy_amount} {EUt}
/// {var:fluid_amount}{L}
/// {var:heat_amount}{heat-unit}
/// ```
public class StaticUnitCommand implements ITemplatedTextCommand {

    private final @NotNull String unit;

    public StaticUnitCommand(@NotNull String unit) {
        this.unit = unit;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        lineOutput.append(unit);
    }

}
