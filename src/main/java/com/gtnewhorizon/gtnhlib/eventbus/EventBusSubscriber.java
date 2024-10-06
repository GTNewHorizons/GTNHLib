package com.gtnewhorizon.gtnhlib.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cpw.mods.fml.relauncher.Side;

/**
 * Annotation to mark a class as an EventBus subscriber. Classes annotated with this will automatically be registered to
 * listen for events. Registration will happen during the init phase.<br>
 * All methods annotated with {@link cpw.mods.fml.common.eventhandler.SubscribeEvent} are expected to be static.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {

    /**
     * The {@link cpw.mods.fml.relauncher.Side} that this subscriber should be registered on. Will default to both sides
     * if not specified.
     */
    Side[] side() default { Side.CLIENT, Side.SERVER };

    /**
     * Can be applied to a boolean field/method in the annotated class that provides a condition for registering the
     * subscriber. It is expected that the field/method is static, returns a boolean, and takes no parameters. <br>
     * There is expected to be at most one condition for a class. Config values can be used as the return value since
     * registration happens during init.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @interface Condition {}

}
