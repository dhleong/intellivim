package org.intellivim;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate any required fields in your command
 *  with this to ensure they're non-null before
 *  execute() is called
 *
 * Created by dhleong on 11/9/14.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
}
