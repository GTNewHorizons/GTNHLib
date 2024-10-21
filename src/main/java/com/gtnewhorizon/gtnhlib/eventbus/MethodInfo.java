package com.gtnewhorizon.gtnhlib.eventbus;

import cpw.mods.fml.common.eventhandler.EventPriority;
import lombok.Data;

@Data
public final class MethodInfo {

    public final String declaringClass;
    public final String name;
    public final String desc;
    public final boolean receiveCanceled;
    public final EventPriority priority;
    public String optionalMod;

    public String getKey() {
        return declaringClass + " " + name + desc;
    }
}
