package org.intellivim.inject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.intellivim.ICommand;

import java.lang.reflect.Field;

/**
 * Interface for classes that can handle field injection
 *  for commands
 *
 * @author dhleong
 */
public interface Injector<T> {

    /**
     * Lazy substitute for proper dependency graph.
     *  Lower is higher
     */
    public int getPriority();

    public boolean canInject(Field field, ICommand command);

    /** @returns True if it was able to inject */
    public boolean inject(Gson gson, Field field, ICommand command, JsonObject json) throws IllegalAccessException;
}
