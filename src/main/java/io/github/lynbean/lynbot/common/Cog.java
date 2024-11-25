package io.github.lynbean.lynbot.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a class as a cog.
 * A cog is a class that contains commands.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cog {
    /**
     * The name of the cog.
     */
    String value();
}
