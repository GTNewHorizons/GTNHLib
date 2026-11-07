package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

/// Appends the name of the given fluid.
///
/// Usage Example:
/// ```
/// {var:amount} {mB} of {fluid:temporalfluid}
/// ```
public class FluidCommand extends TemplatedTextCommand {

    public FluidCommand() {
        super("FLUID");
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        Fluid fluid = FluidRegistry.getFluid(arg);

        if (fluid == null) {
            appendError(lineOutput, "INVALID FLUID: " + arg);
        } else {
            lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK).append(new FluidStack(fluid, 1).getLocalizedName())
                    .append(TextFormatUtil.FORMAT_POP_STACK);
        }
    }
}
