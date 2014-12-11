package com.intellij.execution.actions;

import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Just putting this in the package is not enough (we get an IllegalAccessError)
 *  so we're forced to reflect :(
 *
 * @author dhleong
 * @see com.intellij.execution.actions.PreferredProducerFind
 */
public class RuntimeConfigFinderDelegate {

    static Class<?> preferredProducerFind;
    static Method createConfiguration;
    static Method getConfigurationsFromContext;
    static {
        try {
            preferredProducerFind = Class.forName(
                    "com.intellij.execution.actions.PreferredProducerFind");
            createConfiguration =
                    preferredProducerFind.getDeclaredMethod("createConfiguration",
                        Location.class, ConfigurationContext.class);
            createConfiguration.setAccessible(true);

            getConfigurationsFromContext =
                    preferredProducerFind.getDeclaredMethod(
                            "getConfigurationsFromContext",
                            Location.class, ConfigurationContext.class, boolean.class);
            getConfigurationsFromContext.setAccessible(true);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static RunnerAndConfigurationSettings createConfiguration(Location location, final ConfigurationContext context) {
        try {
            return (RunnerAndConfigurationSettings)
                    createConfiguration.invoke(null, location, context);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<ConfigurationFromContext> getConfigurationsFromContext(final Location<?> location,
                       final ConfigurationContext context) {
 //        return preferredProducerFind.createConfiguration(location, context);
        try {
            return (List<ConfigurationFromContext>)
                    getConfigurationsFromContext.invoke(null, location, context, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
