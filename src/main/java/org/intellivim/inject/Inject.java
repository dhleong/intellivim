package org.intellivim.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Certain things can be @Injected into your Command's fields
 *  for you, for example anything that is @ClientSpecific.
 *  If it is inflated by a specific field in the JSON, however
 *  (such as Project) it should simply have an adapter
 *  registered on the Gson instance rather than preparing
 *  an Injector.
 *
 * @author dhleong
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
}
