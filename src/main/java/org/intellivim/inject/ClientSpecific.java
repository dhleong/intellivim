package org.intellivim.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Client-specific dependencies can be injected
 *  by their interface type. Implementations of
 *  ClientSpecific types are inflated by parsing
 *  exactly the Command's original JSON using the
 *  global Gson instance, so any necessary args
 *  can be included right in there.
 *
 * The value used here should be a constant
 *  on Client. If Client.DEFAULT is used, then
 *  that implementation will be provided when
 *  no other implementation could be inflated.
 *  If no Client.DEFAULT implementation is provided
 *  in such a situation, then the field will simply
 *  be null. You may used @Required to ensure that
 *  this doesn't happen
 *
 * @author dhleong
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientSpecific {

    String value() /*default Client.SPEC*/;
}
