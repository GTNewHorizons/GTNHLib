package com.gtnewhorizon.gtnhlib.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cpw.mods.fml.relauncher.Side;

/**
 * Annotation to mark a class as an EventBus subscriber. Classes annotated with this will automatically be registered to
 * listen for events. Registration will happen during the specified {@link EventBusSubscriber#phase()} or during
 * {@link Phase#INIT} if not specified. <br>
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
     * Which equivalent {@link cpw.mods.fml.common.LoaderState} this subscriber should be registered during.
     */
    Phase phase() default Phase.INIT;

    /**
     * Can be applied to a boolean method in the annotated class that provides a condition for registering the
     * subscriber. It is expected that the method is static, returns a boolean, and takes no parameters. <br>
     * There is expected to be at most one condition for a class.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Condition {}

}
