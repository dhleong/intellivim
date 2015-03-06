package org.intellivim.morph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate concrete implementations of abstract command
 *  with this, in case your command has different dependencies
 *  based on file type, for example
 *
 * @author dhleong
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandImpl {
    /**
     * @return The name of the command being implemented
     */
    String of();

    /**
     * @return Parameters to match with the Polymorpher
     *  for when this Impl should be used
     */
    String[] whenParams();

}
