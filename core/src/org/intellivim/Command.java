package org.intellivim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by dhleong on 11/9/14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    /**
     * Return the name of this command,
     *  to be passed in the "command" field
     *  of the json request
     *
     * @return
     */
    String value();
}
